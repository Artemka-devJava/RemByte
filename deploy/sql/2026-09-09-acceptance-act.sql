-- ============================================================
--  FixByte CRM — акт приёма оборудования в ремонт (PDF)
--  Дата: 2026-09-09   |   Диалект: MariaDB / MySQL
--
--  Нужна только для prod (spring.jpa.hibernate.ddl-auto=validate).
--  На dev/docker/h2 (ddl-auto=update) ничего делать не нужно.
--  Сделайте бэкап БД перед запуском.
-- ============================================================

-- Акт хранится в существующей таблице order_attachments как вложение с
-- attachment_type = 'ACT' (по одной актуальной копии на заказ,
-- stored_name = 'acceptance-act.pdf'). Новых таблиц/колонок не добавляется.

-- 1. Снять CHECK-ограничение на attachment_type, если Hibernate его создавал
--    (attachment_type IN ('PHOTO','VIDEO','FILE')) — иначе вставка 'ACT' упадёт.
--    Имя ограничения зависит от версии; найдите его:
--
--      SELECT CONSTRAINT_NAME
--        FROM information_schema.CHECK_CONSTRAINTS
--       WHERE CONSTRAINT_SCHEMA = DATABASE()
--         AND CHECK_CLAUSE LIKE '%attachment_type%';
--
--    и выполните (подставив имя):
--
--      ALTER TABLE order_attachments DROP CONSTRAINT <имя>;
--
--    На многих версиях MariaDB такого ограничения нет — тогда пункт пропускается.
--    Приложение теперь хранит тип через AttributeConverter и новых CHECK не создаёт.

-- 2. Расширять сам столбец не требуется: attachment_type VARCHAR(12) вмещает 'ACT'.
