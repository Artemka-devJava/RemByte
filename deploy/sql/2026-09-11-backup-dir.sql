-- ============================================================
--  FixByte CRM — путь хранения бэкапов, выбираемый в админке
--  Дата: 2026-09-11   |   Диалект: MariaDB / MySQL
--
--  Нужна только для prod (spring.jpa.hibernate.ddl-auto=validate) и только
--  если таблица backup_schedule уже существует (создана миграцией
--  2026-09-09-backup-schedule.sql). На dev/docker/h2 (ddl-auto=update)
--  колонка добавляется автоматически.
--  Сделайте бэкап БД перед запуском.
-- ============================================================

ALTER TABLE backup_schedule
    ADD COLUMN IF NOT EXISTS backup_dir VARCHAR(500) NULL;
