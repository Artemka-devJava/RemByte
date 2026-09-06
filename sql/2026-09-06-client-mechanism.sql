-- ============================================================
--  FixByte CRM — механизм ведения клиентов
--  Дата: 2026-09-06   |   Диалект: MariaDB / MySQL
--
--  Нужна только для prod (spring.jpa.hibernate.ddl-auto=validate).
--  На dev/docker/h2 (ddl-auto=update) всё создаётся автоматически.
--  Сделайте бэкап БД перед запуском.
-- ============================================================

-- 1. Новые поля карточки клиента
ALTER TABLE clients
    ADD COLUMN type               VARCHAR(20)  NOT NULL DEFAULT 'INDIVIDUAL',
    ADD COLUMN tags               VARCHAR(255) NULL,
    ADD COLUMN source             VARCHAR(60)  NULL,
    ADD COLUMN preferred_channel  VARCHAR(20)  NULL,
    ADD COLUMN company_details    TEXT         NULL,
    ADD COLUMN consent_pdn_at     DATETIME     NULL,
    ADD COLUMN consent_marketing_at DATETIME   NULL,
    ADD COLUMN archived_at        DATETIME     NULL;

-- 2. Журнал клиента (заметки, звонки, сообщения)
CREATE TABLE IF NOT EXISTS client_notes (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    client_id  BIGINT       NOT NULL,
    kind       VARCHAR(16)  NOT NULL DEFAULT 'NOTE',
    text       TEXT         NOT NULL,
    author     VARCHAR(120) NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_client_notes_client (client_id),
    CONSTRAINT fk_client_notes_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Устройства клиента
CREATE TABLE IF NOT EXISTS client_devices (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    client_id     BIGINT       NOT NULL,
    kind          VARCHAR(40)  NULL,
    model         VARCHAR(160) NOT NULL,
    serial_number VARCHAR(120) NULL,
    notes         TEXT         NULL,
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_client_devices_client (client_id),
    CONSTRAINT fk_client_devices_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Фото клиента / устройств (не привязаны к заказу)
CREATE TABLE IF NOT EXISTS client_photos (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    client_id     BIGINT       NOT NULL,
    device_id     BIGINT       NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(150) NOT NULL,
    content       LONGBLOB     NOT NULL,
    caption       VARCHAR(255) NULL,
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_client_photos_client (client_id),
    CONSTRAINT fk_client_photos_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE,
    CONSTRAINT fk_client_photos_device FOREIGN KEY (device_id) REFERENCES client_devices (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. (Рекомендуется) нормализовать существующие телефоны к виду +7XXXXXXXXXX
--    вручную/скриптом приложения — здесь не трогаем, чтобы не сломать уникальность.
