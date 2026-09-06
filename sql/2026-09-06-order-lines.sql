-- ============================================================
--  FixByte CRM — миграция состава заказов на строки (order_lines)
--  Дата: 2026-09-06
--  Диалект: MariaDB / MySQL
--
--  Нужна ТОЛЬКО для окружений с spring.jpa.hibernate.ddl-auto=validate
--  (обычно prod). На dev/docker (ddl-auto=update) таблица создаётся
--  автоматически, а перенос данных делает OrderLineMigrationService при старте.
--
--  Делайте бэкап перед запуском (см. OPERATIONS.md).
-- ============================================================

-- 1. Таблица строк заказа
CREATE TABLE IF NOT EXISTS order_lines (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    order_id   BIGINT       NOT NULL,
    service_id BIGINT       NULL,
    name       VARCHAR(255) NOT NULL,
    unit_price DOUBLE       NOT NULL DEFAULT 0,
    quantity   INT          NOT NULL DEFAULT 1,
    sort_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order_lines_order (order_id),
    KEY idx_order_lines_service (service_id),
    CONSTRAINT fk_order_lines_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_lines_service
        FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Перенос данных из старой связи many-to-many (только для заказов без строк)
INSERT INTO order_lines (order_id, service_id, name, unit_price, quantity, sort_order)
SELECT os.order_id,
       os.service_id,
       s.name,
       s.base_price,
       1,
       0
FROM order_services os
JOIN services s ON s.id = os.service_id
WHERE NOT EXISTS (
    SELECT 1 FROM order_lines ol WHERE ol.order_id = os.order_id
);

-- 3. Пересчитать сумму заказов по строкам
UPDATE orders o
SET o.total_price = COALESCE((
    SELECT SUM(ol.unit_price * ol.quantity)
    FROM order_lines ol
    WHERE ol.order_id = o.id
), 0);

-- 4. (Необязательно, после проверки) удалить старую таблицу связи:
-- DROP TABLE order_services;
