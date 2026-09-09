package com.rembyte.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Включает поддержку {@code @Scheduled}. Используется
 * {@link com.rembyte.service.BackupScheduleService}: каждую ночь в 03:30 он
 * проверяет, наступил ли срок автоматического полного бэкапа (периодичность
 * задаётся в панели администратора).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
