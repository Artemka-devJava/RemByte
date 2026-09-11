package com.rembyte.model;

import com.rembyte.service.BackupFrequency;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Настройки автоматического полного бэкапа. Хранится ровно одна строка (id = 1).
 */
@Entity
@Table(name = "backup_schedule")
public class BackupSchedule {

    /** Единственная строка настроек. */
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BackupFrequency frequency = BackupFrequency.OFF;

    /** Момент последнего УСПЕШНОГО планового бэкапа (null — ещё не было). */
    private LocalDateTime lastRunAt;

    /** Итог последней попытки: «успех: …» или «ошибка: …». */
    @Column(length = 500)
    private String lastStatus;

    /**
     * Каталог для полных бэкапов, выбранный администратором в панели
     * (абсолютный путь, уже проверенный на попадание внутрь
     * {@code fixbyte.backup.base-dir}). {@code null} — используется
     * значение из переменной окружения {@code FIXBYTE_BACKUP_DIR}.
     */
    @Column(name = "backup_dir", length = 500)
    private String backupDir;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BackupFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(BackupFrequency frequency) {
        this.frequency = frequency == null ? BackupFrequency.OFF : frequency;
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(LocalDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getBackupDir() {
        return backupDir;
    }

    public void setBackupDir(String backupDir) {
        this.backupDir = backupDir;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
