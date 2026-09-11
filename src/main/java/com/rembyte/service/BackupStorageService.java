package com.rembyte.service;

import com.rembyte.model.BackupSchedule;
import com.rembyte.repository.BackupScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Хранилище ПОЛНЫХ резервных копий.
 * <p>
 * Каталог задаётся одним из двух способов (в таком порядке приоритета):
 * <ol>
 *   <li>выбран администратором в панели («База данных» → «Хранилище
 *       полных бэкапов») — хранится в {@link BackupSchedule#getBackupDir()},
 *       обязан лежать внутри {@link #baseDir()} ({@code fixbyte.backup.base-dir},
 *       по умолчанию {@code /mnt} — это то, что смонтировано в docker-compose);</li>
 *   <li>иначе — переменная окружения {@code FIXBYTE_BACKUP_DIR} (старый способ,
 *       путь не ограничен {@link #baseDir()}, для обратной совместимости).</li>
 * </ol>
 * Каталог может быть смонтированной сетевой шарой Samba/CIFS — тогда полные
 * бэкапы «сохраняются через samba» без дополнительного кода:
 * <pre>
 *   # Linux-хост
 *   mount -t cifs //NAS/backups /mnt/crm-backups -o user=…,pass=…,uid=1000
 *   # выбрать в админке путь "/mnt/crm-backups" (или переменной FIXBYTE_BACKUP_DIR)
 * </pre>
 */
@Service
public class BackupStorageService {

    private static final Logger log = LoggerFactory.getLogger(BackupStorageService.class);

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Pattern SAFE_NAME =
            Pattern.compile("^rembyte_backup_\\d{8}_\\d{6}_(full|light)\\.zip$");
    private static final String PREFIX = "rembyte_backup_";

    private final DatabaseBackupService backupService;
    private final BackupScheduleRepository scheduleRepository;

    /** Старый способ задать каталог — переменной окружения (без ограничения на baseDir). */
    @Value("${fixbyte.backup.dir:}")
    private String legacyBackupDir;

    /** Каталог, смонтированный в docker-compose — единственное разрешённое место для пути из админки. */
    @Value("${fixbyte.backup.base-dir:/mnt}")
    private String baseDirProperty;

    @Value("${fixbyte.backup.keep:14}")
    private int keep;

    public BackupStorageService(DatabaseBackupService backupService, BackupScheduleRepository scheduleRepository) {
        this.backupService = backupService;
        this.scheduleRepository = scheduleRepository;
    }

    // ── Состояние ─────────────────────────────────────────────

    /** Каталог, смонтированный в docker-compose (напр. {@code /mnt}) — граница для выбора в админке. */
    public String baseDir() {
        return Paths.get(baseDirProperty).toAbsolutePath().normalize().toString();
    }

    /** Путь, выбранный администратором в панели (как хранится в БД), либо {@code null}. */
    public String directoryOverride() {
        BackupSchedule s = scheduleRepository.findById(BackupSchedule.SINGLETON_ID).orElse(null);
        return s == null ? null : s.getBackupDir();
    }

    private String configuredDir() {
        String override = directoryOverride();
        if (override != null && !override.isBlank()) {
            return override;
        }
        return legacyBackupDir;
    }

    public boolean isConfigured() {
        String dir = configuredDir();
        return dir != null && !dir.isBlank();
    }

    public String targetDescription() {
        return isConfigured()
                ? Paths.get(configuredDir()).toAbsolutePath().normalize().toString()
                : "не настроено (выберите путь в панели или задайте FIXBYTE_BACKUP_DIR)";
    }

    public int keepCount() {
        return keep;
    }

    /**
     * Сохранить выбранный администратором каталог. {@code null}/пусто — сбросить
     * на переменную окружения {@code FIXBYTE_BACKUP_DIR}. Иначе путь обязан
     * лежать внутри {@link #baseDir()} — только он гарантированно примонтирован
     * в docker-compose, произвольный путь внутри контейнера писать некуда.
     */
    @Transactional
    public String setDirectory(String raw) {
        BackupSchedule s = scheduleRepository.findById(BackupSchedule.SINGLETON_ID)
                .orElseGet(() -> {
                    BackupSchedule n = new BackupSchedule();
                    n.setId(BackupSchedule.SINGLETON_ID);
                    return n;
                });
        s.setBackupDir(raw == null || raw.isBlank() ? null : validateUnderBase(raw));
        s.setUpdatedAt(LocalDateTime.now());
        scheduleRepository.save(s);
        return targetDescription();
    }

    private String validateUnderBase(String raw) {
        Path base = Paths.get(baseDirProperty).toAbsolutePath().normalize();
        Path candidate = Paths.get(raw.trim());
        Path resolved = (candidate.isAbsolute() ? candidate : base.resolve(candidate)).normalize();
        if (!resolved.equals(base) && !resolved.startsWith(base)) {
            throw new IllegalArgumentException(
                    "Путь должен быть внутри " + base + " — это каталог, примонтированный в docker-compose");
        }
        return resolved.toString();
    }

    // ── Запись ────────────────────────────────────────────────

    /** Сделать полный бэкап и положить его в хранилище. Возвращает метаданные файла. */
    public synchronized StoredBackup storeFullBackup() throws Exception {
        Path dir = requireDir();
        String name = PREFIX + LocalDateTime.now().format(TS) + "_full.zip";
        Path target = dir.resolve(name);
        Path part = dir.resolve(name + ".part");

        try {
            try (OutputStream os = Files.newOutputStream(part, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                backupService.backupToStream(os, BackupLevel.FULL);
            }
            try {
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // некоторые CIFS-моунты не умеют rename с заменой — копируем напрямую
                Files.copy(part, target, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(part);
            }
        } catch (Exception e) {
            try { Files.deleteIfExists(part); } catch (IOException ignored) { /* no-op */ }
            throw e;
        }

        prune(dir);
        log.info("Полный бэкап сохранён в хранилище: {} ({} байт)", name, sizeOf(target));
        return describe(target);
    }

    // ── Чтение / список / удаление ────────────────────────────

    public List<StoredBackup> list() {
        if (!isConfigured()) return List.of();
        Path dir = Paths.get(configuredDir()).toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) return List.of();

        List<StoredBackup> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(PREFIX)
                            && p.getFileName().toString().endsWith(".zip"))
                    .forEach(p -> out.add(describe(p)));
        } catch (IOException e) {
            log.warn("Не удалось прочитать каталог бэкапов {}: {}", dir, e.getMessage());
        }
        out.sort(Comparator.comparing(StoredBackup::modified).reversed());
        return out;
    }

    public InputStream open(String name) throws IOException {
        return Files.newInputStream(resolveExisting(name));
    }

    public long size(String name) throws IOException {
        return Files.size(resolveExisting(name));
    }

    public void delete(String name) throws IOException {
        Files.deleteIfExists(resolveExisting(name));
    }

    // ── Внутреннее ───────────────────────────────────────────

    private Path requireDir() throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Хранилище бэкапов не настроено (FIXBYTE_BACKUP_DIR)");
        }
        Path dir = Paths.get(configuredDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return dir;
    }

    private Path resolveExisting(String name) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Хранилище бэкапов не настроено");
        }
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Недопустимое имя файла бэкапа");
        }
        Path dir = Paths.get(configuredDir()).toAbsolutePath().normalize();
        Path p = dir.resolve(name).normalize();
        if (!p.startsWith(dir) || !Files.isRegularFile(p)) {
            throw new NoSuchFileException(name);
        }
        return p;
    }

    private void prune(Path dir) {
        if (keep <= 0) return;
        try (Stream<Path> s = Files.list(dir)) {
            List<Path> zips = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(PREFIX)
                            && p.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparing(this::lastModified).reversed())
                    .toList();
            for (int i = keep; i < zips.size(); i++) {
                Files.deleteIfExists(zips.get(i));
                log.info("Старый бэкап удалён по ротации: {}", zips.get(i).getFileName());
            }
        } catch (IOException e) {
            log.warn("Ротация бэкапов не выполнена: {}", e.getMessage());
        }
    }

    private StoredBackup describe(Path p) {
        return new StoredBackup(p.getFileName().toString(), sizeOf(p), lastModified(p));
    }

    private long sizeOf(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return -1;
        }
    }

    private Instant lastModified(Path p) {
        try {
            BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
            return a.lastModifiedTime().toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    public record StoredBackup(String name, long size, Instant modified) {}
}
