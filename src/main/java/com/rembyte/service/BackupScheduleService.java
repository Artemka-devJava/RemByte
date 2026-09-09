package com.rembyte.service;

import com.rembyte.model.BackupSchedule;
import com.rembyte.repository.BackupScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Автоматический полный бэкап в хранилище по расписанию.
 * <p>
 * Периодичность выбирает администратор (панель → «База данных»): выключено,
 * раз в день / месяц / квартал / год. Каждую ночь в 03:30 сервис проверяет,
 * наступил ли срок, и, если хранилище настроено, кладёт в него полный бэкап.
 * Ориентир — момент последнего успешного бэкапа, поэтому пропущенное окно
 * (сервер был выключен) отработает при следующей ночной проверке.
 */
@Service
public class BackupScheduleService {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduleService.class);

    private final BackupScheduleRepository repo;
    private final BackupStorageService storage;

    public BackupScheduleService(BackupScheduleRepository repo, BackupStorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    // ── Настройки ─────────────────────────────────────────────

    @Transactional
    public BackupSchedule current() {
        return repo.findById(BackupSchedule.SINGLETON_ID).orElseGet(() -> {
            BackupSchedule s = new BackupSchedule();
            s.setId(BackupSchedule.SINGLETON_ID);
            s.setFrequency(BackupFrequency.OFF);
            s.setUpdatedAt(LocalDateTime.now());
            return repo.save(s);
        });
    }

    @Transactional
    public BackupSchedule setFrequency(BackupFrequency frequency) {
        BackupSchedule s = current();
        s.setFrequency(frequency);
        s.setUpdatedAt(LocalDateTime.now());
        return repo.save(s);
    }

    // ── Расписание ───────────────────────────────────────────

    /** Ночная проверка: если подошёл срок и хранилище настроено — полный бэкап. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void nightlyCheck() {
        BackupSchedule s = current();
        BackupFrequency freq = s.getFrequency();
        if (freq == null || freq == BackupFrequency.OFF) {
            return;
        }
        if (!freq.isDue(s.getLastRunAt(), LocalDateTime.now())) {
            return;
        }
        if (!storage.isConfigured()) {
            log.warn("Плановый бэкап пропущен: не задан FIXBYTE_BACKUP_DIR");
            s.setLastStatus("пропущено " + LocalDateTime.now().withNano(0)
                    + ": не настроено хранилище (FIXBYTE_BACKUP_DIR)");
            repo.save(s);
            return;
        }
        runFullBackup(s);
    }

    private void runFullBackup(BackupSchedule s) {
        try {
            BackupStorageService.StoredBackup b = storage.storeFullBackup();
            s.setLastRunAt(LocalDateTime.now());
            s.setLastStatus("успех: " + b.name());
            repo.save(s);
            log.info("Плановый полный бэкап готов: {}", b.name());
        } catch (Exception e) {
            // lastRunAt НЕ трогаем — попытка повторится следующей ночью.
            s.setLastStatus("ошибка " + LocalDateTime.now().withNano(0) + ": " + e.getMessage());
            repo.save(s);
            log.error("Плановый бэкап не выполнен: {}", e.getMessage(), e);
        }
    }
}
