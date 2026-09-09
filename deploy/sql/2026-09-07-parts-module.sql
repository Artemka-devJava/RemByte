-- ============================================================
--  FixByte CRM — модуль «Комплектующие» (закупка/продажа деталей ПК)
--  Дата: 2026-09-07
--  Диалект: MariaDB / MySQL
--
--  Нужна ТОЛЬКО для окружений с spring.jpa.hibernate.ddl-auto=validate
--  (обычно prod). На dev/docker/h2 (ddl-auto=update) таблицы создаются
--  автоматически при старте приложения.
-- ============================================================

CREATE TABLE IF NOT EXISTS part_lots (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(255) NOT NULL,
    sale_price  DOUBLE       NULL,
    sale_date   DATETIME     NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'IN_STOCK',
    notes       TEXT         NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS part_items (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(255) NOT NULL,
    category        VARCHAR(255) NULL,
    source          VARCHAR(255) NULL,
    purchase_price  DOUBLE       NOT NULL DEFAULT 0,
    purchase_date   DATETIME     NOT NULL,
    sale_price      DOUBLE       NULL,
    sale_date       DATETIME     NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'IN_STOCK',
    lot_id          BIGINT       NULL,
    notes           TEXT         NULL,
    created_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_part_items_lot (lot_id),
    CONSTRAINT fk_part_items_lot
        FOREIGN KEY (lot_id) REFERENCES part_lots (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS part_item_photos (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    item_id       BIGINT       NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(150) NOT NULL,
    content       LONGBLOB     NOT NULL,
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_part_item_photos_item (item_id),
    CONSTRAINT fk_part_item_photos_item
        FOREIGN KEY (item_id) REFERENCES part_items (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS parts_budget (
    id              BIGINT   NOT NULL,
    starting_amount DOUBLE   NOT NULL DEFAULT 0,
    updated_at      DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
