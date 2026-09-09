-- ============================================================
--  FixByte CRM — расписание автоматического полного бэкапа
--  Дата: 2026-09-09   |   Диалект: MariaDB / MySQL
--
--  Нужна только для prod (spring.jpa.hibernate.ddl-auto=validate).
--  На dev/docker/h2 (ddl-auto=update) таблица создаётся автоматически.
--  Сделайте бэкап БД перед запуском.
-- ============================================================

-- Одна строка настроек (id = 1). frequency: OFF | DAILY | MONTHLY | QUARTERLY | YEARLY.
CREATE TABLE IF NOT EXISTS backup_schedule (
    id          BIGINT       NOT NULL,
    frequency   VARCHAR(20)  NOT NULL DEFAULT 'OFF',
    last_run_at DATETIME     NULL,
    last_status VARCHAR(500) NULL,
    updated_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO backup_schedule (id, frequency, updated_at)
VALUES (1, 'OFF', NOW())
ON DUPLICATE KEY UPDATE id = id;
