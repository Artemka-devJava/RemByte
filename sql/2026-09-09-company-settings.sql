-- ============================================================
--  FixByte CRM — реквизиты компании (шапка чека и акта приёмки)
--  Дата: 2026-09-09   |   Диалект: MariaDB / MySQL
--
--  Нужна только для prod (spring.jpa.hibernate.ddl-auto=validate).
--  На dev/docker/h2 (ddl-auto=update) таблица создаётся автоматически.
--  Сделайте бэкап БД перед запуском.
-- ============================================================

CREATE TABLE IF NOT EXISTS company_settings (
    id         BIGINT       NOT NULL,
    name       VARCHAR(200) NULL,
    subtitle   VARCHAR(300) NULL,
    address    VARCHAR(300) NULL,
    phone      VARCHAR(120) NULL,
    email      VARCHAR(160) NULL,
    employee   VARCHAR(160) NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Одна строка настроек (id = 1). Значения дальше правятся в панели
-- администратора → «Настройки чека». При первом старте приложение
-- заполнит её из FIXBYTE_COMPANY_* (если строки ещё нет).
INSERT INTO company_settings (id, name, subtitle, updated_at)
VALUES (1, 'FixByte', 'Сервисный центр · ремонт техники', NOW())
ON DUPLICATE KEY UPDATE id = id;
