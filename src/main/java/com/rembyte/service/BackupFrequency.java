package com.rembyte.service;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Периодичность автоматического полного бэкапа в хранилище.
 * Проверка выполняется раз в сутки (ночью); {@link #isDue} решает, наступил ли срок,
 * с учётом момента последнего успешного бэкапа — пропущенное окно (сервер был
 * выключен) отработает при следующей проверке.
 */
public enum BackupFrequency {

    OFF("Выключен"),
    DAILY("Раз в день"),
    MONTHLY("Раз в месяц"),
    QUARTERLY("Раз в квартал"),
    YEARLY("Раз в год");

    private final String label;

    BackupFrequency(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Наступил ли срок очередного бэкапа. */
    public boolean isDue(LocalDateTime lastRun, LocalDateTime now) {
        if (this == OFF) {
            return false;
        }
        if (lastRun == null) {
            return true;
        }
        return switch (this) {
            case DAILY     -> lastRun.toLocalDate().isBefore(now.toLocalDate());
            case MONTHLY   -> YearMonth.from(lastRun).isBefore(YearMonth.from(now));
            case QUARTERLY -> quarterIndex(lastRun) < quarterIndex(now);
            case YEARLY    -> lastRun.getYear() < now.getYear();
            case OFF       -> false;
        };
    }

    /** Сквозной номер квартала: год*4 + (0..3). */
    private static long quarterIndex(LocalDateTime t) {
        return t.getYear() * 4L + (t.getMonthValue() - 1) / 3;
    }

    public static BackupFrequency parse(String value) {
        if (value == null) {
            return OFF;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OFF;
        }
    }
}
