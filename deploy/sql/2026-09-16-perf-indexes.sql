-- ============================================================
--  FixByte CRM — индексы на горячих столбцах (orders/clients/chat/payments/notes)
--  Дата: 2026-09-16   |   Диалект: MariaDB / MySQL
--
--  Нужна только для prod (spring.jpa.hibernate.ddl-auto=validate) — Hibernate
--  в этом режиме не создаёт и не проверяет индексы сам. На dev/docker/h2
--  (ddl-auto=update) они появляются автоматически из @Table(indexes=...) в
--  моделях (Order, Client, ChatMessage, ChatConversation, Payment, NoteItem).
--
--  До этой миграции списки заказов/клиентов и особенно чат (опрос раз в
--  4-5 секунд оператором и публичным виджетом) шли full scan-ом по мере
--  роста таблиц. Сделайте бэкап БД перед запуском.
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at);
CREATE INDEX IF NOT EXISTS idx_orders_client ON orders (client_id);

CREATE INDEX IF NOT EXISTS idx_clients_archived_at ON clients (archived_at);
CREATE INDEX IF NOT EXISTS idx_clients_is_active ON clients (is_active);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation ON chat_messages (conversation_id);
CREATE INDEX IF NOT EXISTS idx_chat_conversations_status_last_message
    ON chat_conversations (status, last_message_at);

CREATE INDEX IF NOT EXISTS idx_payments_order ON payments (order_id);
CREATE INDEX IF NOT EXISTS idx_note_items_folder ON note_items (folder_id);
