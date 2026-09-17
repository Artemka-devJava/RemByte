-- ============================================================
--  FixByte CRM — себестоимость расходных материалов по заказу
--  Дата: 2026-09-17   |   Диалект: MariaDB / MySQL
--
--  Нужна только для prod (spring.jpa.hibernate.ddl-auto=validate).
--  На dev/docker/h2 (ddl-auto=update) колонка добавляется автоматически.
--  Сделайте бэкап БД перед запуском.
-- ============================================================

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS material_cost DOUBLE NOT NULL DEFAULT 0;
