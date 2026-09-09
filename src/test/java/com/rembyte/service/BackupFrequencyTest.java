package com.rembyte.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BackupFrequencyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 9, 3, 30);

    @Test
    void offIsNeverDue() {
        assertThat(BackupFrequency.OFF.isDue(null, NOW)).isFalse();
        assertThat(BackupFrequency.OFF.isDue(NOW.minusYears(5), NOW)).isFalse();
    }

    @Test
    void everyFrequencyIsDueWhenNeverRun() {
        for (BackupFrequency f : BackupFrequency.values()) {
            if (f == BackupFrequency.OFF) continue;
            assertThat(f.isDue(null, NOW)).as(f.name()).isTrue();
        }
    }

    @Test
    void dailyDueOncePerCalendarDay() {
        assertThat(BackupFrequency.DAILY.isDue(NOW.minusHours(2), NOW)).isFalse();
        assertThat(BackupFrequency.DAILY.isDue(NOW.minusDays(1), NOW)).isTrue();
        assertThat(BackupFrequency.DAILY.isDue(NOW.toLocalDate().atStartOfDay(), NOW)).isFalse();
    }

    @Test
    void monthlyDueOncePerCalendarMonth() {
        assertThat(BackupFrequency.MONTHLY.isDue(NOW.minusDays(3), NOW)).isFalse();
        assertThat(BackupFrequency.MONTHLY.isDue(NOW.withDayOfMonth(1), NOW)).isFalse();
        assertThat(BackupFrequency.MONTHLY.isDue(NOW.minusMonths(1), NOW)).isTrue();
        // сервер молчал полгода — при следующей проверке всё равно сработает
        assertThat(BackupFrequency.MONTHLY.isDue(NOW.minusMonths(6), NOW)).isTrue();
    }

    @Test
    void quarterlyDueOncePerCalendarQuarter() {
        // 9 сентября — Q3 (июль-сентябрь)
        assertThat(BackupFrequency.QUARTERLY.isDue(LocalDateTime.of(2026, 7, 1, 0, 0), NOW)).isFalse();
        assertThat(BackupFrequency.QUARTERLY.isDue(LocalDateTime.of(2026, 6, 30, 23, 0), NOW)).isTrue();
        assertThat(BackupFrequency.QUARTERLY.isDue(LocalDateTime.of(2025, 9, 9, 3, 30), NOW)).isTrue();
    }

    @Test
    void yearlyDueOncePerCalendarYear() {
        assertThat(BackupFrequency.YEARLY.isDue(LocalDateTime.of(2026, 1, 1, 0, 0), NOW)).isFalse();
        assertThat(BackupFrequency.YEARLY.isDue(LocalDateTime.of(2025, 12, 31, 23, 59), NOW)).isTrue();
    }

    @Test
    void parseFallsBackToOff() {
        assertThat(BackupFrequency.parse("monthly")).isEqualTo(BackupFrequency.MONTHLY);
        assertThat(BackupFrequency.parse("  YEARLY ")).isEqualTo(BackupFrequency.YEARLY);
        assertThat(BackupFrequency.parse(null)).isEqualTo(BackupFrequency.OFF);
        assertThat(BackupFrequency.parse("nonsense")).isEqualTo(BackupFrequency.OFF);
    }
}
