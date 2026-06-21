ALTER TABLE chat_message
    ADD COLUMN feedback VARCHAR(16) DEFAULT NULL COMMENT 'positive / negative' AFTER reference_json;
