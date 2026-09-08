package com.rembyte.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class BackupStorageServiceTest {

    @TempDir
    Path storeDir;

    private BackupStorageService storage;

    /** Заглушка: пишет предсказуемое содержимое вместо реального дампа БД. */
    static class StubBackup extends DatabaseBackupService {
        StubBackup() { super(null); }
        @Override
        public void backupToStream(OutputStream out, BackupLevel level) throws Exception {
            out.write(("dummy-" + level).getBytes(StandardCharsets.UTF_8));
        }
    }

    @BeforeEach
    void setUp() {
        storage = new BackupStorageService(new StubBackup());
        ReflectionTestUtils.setField(storage, "backupDir", storeDir.toString());
        ReflectionTestUtils.setField(storage, "keep", 3);
    }

    @Test
    void notConfiguredWhenDirBlank() {
        var s = new BackupStorageService(new StubBackup());
        ReflectionTestUtils.setField(s, "backupDir", "");
        assertThat(s.isConfigured()).isFalse();
        assertThat(s.list()).isEmpty();
    }

    @Test
    void storesFullBackupAndListsIt() throws Exception {
        var b = storage.storeFullBackup();

        assertThat(b.name()).matches("rembyte_backup_\\d{8}_\\d{6}_full\\.zip");
        assertThat(b.size()).isGreaterThan(0);
        assertThat(storeDir.resolve(b.name())).exists();
        assertThat(Files.readString(storeDir.resolve(b.name()))).isEqualTo("dummy-FULL");

        List<BackupStorageService.StoredBackup> list = storage.list();
        assertThat(list).extracting(BackupStorageService.StoredBackup::name).contains(b.name());
    }

    @Test
    void rotationKeepsOnlyNewest() throws Exception {
        for (int i = 0; i < 6; i++) {
            // разные имена по секундам не гарантированы — кладём вручную
            Files.writeString(storeDir.resolve("rembyte_backup_20260101_00000" + i + "_full.zip"), "x");
        }
        // ещё один через сервис — триггерит prune
        storage.storeFullBackup();

        long count = Files.list(storeDir)
                .filter(p -> p.getFileName().toString().startsWith("rembyte_backup_"))
                .count();
        assertThat(count).isEqualTo(3); // keep = 3
    }

    @Test
    void rejectsUnsafeNames() {
        assertThatThrownBy(() -> storage.open("../secret.txt")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> storage.open("evil.zip")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> storage.delete("rembyte_backup_1_full.zip")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void openAndDeleteRoundTrip() throws Exception {
        var b = storage.storeFullBackup();
        try (var in = storage.open(b.name())) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("dummy-FULL");
        }
        storage.delete(b.name());
        assertThat(storeDir.resolve(b.name())).doesNotExist();
    }

    @Test
    void backupLevelParsing() {
        assertThat(BackupLevel.parse("light")).isEqualTo(BackupLevel.LIGHT);
        assertThat(BackupLevel.parse("db")).isEqualTo(BackupLevel.LIGHT);
        assertThat(BackupLevel.parse("FULL")).isEqualTo(BackupLevel.FULL);
        assertThat(BackupLevel.parse(null)).isEqualTo(BackupLevel.FULL);
        assertThat(BackupLevel.parse("nonsense")).isEqualTo(BackupLevel.FULL);
    }
}
