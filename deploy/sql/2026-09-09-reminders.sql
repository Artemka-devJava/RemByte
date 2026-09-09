-- ============================================================
--  FixByte CRM — напоминания оператору
--  Дата: 2026-09-09   |   Диалект: MariaDB / MySQL
--
--  Нужна только для prod (spring.jpa.hibernate.ddl-auto=validate).
--  На dev/docker/h2 (ddl-auto=update) таблица создаётся автоматически.
--  Сделайте бэкап БД перед запуском.
-- ============================================================

CREATE TABLE IF NOT EXISTS reminders (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    text       VARCHAR(500) NOT NULL,
    due_at     DATETIME     NULL,
    done       TINYINT(1)   NOT NULL DEFAULT 0,
    done_at    DATETIME     NULL,
    client_id  BIGINT       NULL,
    order_id   BIGINT       NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_reminders_open (done, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- «Готовый, но не забранный» заказ определяется на лету запросом
-- (status IN ('READY','COMPLETED') и давность > FIXBYTE_STALE_ORDER_DAYS,
--  по умолчанию 30) — отдельного хранения не требует.
