ALTER TABLE chat_message
    ADD COLUMN client_message_id VARCHAR(128) DEFAULT NULL COMMENT 'frontend idempotency key' AFTER feedback;

CREATE UNIQUE INDEX uk_chat_message_client_message_role ON chat_message (user_id, role, client_message_id);
