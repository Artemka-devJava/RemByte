package com.rembyte.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Включает поддержку {@code @Scheduled} (используется плановым полным бэкапом).
 * Сам бэкап по расписанию выключен, пока не задан {@code fixbyte.backup.schedule.cron}
 * (значение по умолчанию '-' отключает триггер).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
