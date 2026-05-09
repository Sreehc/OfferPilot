ALTER TABLE knowledge_card_task
    ADD COLUMN IF NOT EXISTS deck_title VARCHAR(128) DEFAULT NULL AFTER doc_title,
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) NOT NULL DEFAULT 'knowledge_doc' COMMENT 'knowledge_doc / wrong_auto' AFTER deck_title,
    ADD COLUMN IF NOT EXISTS is_current TINYINT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN IF NOT EXISTS estimated_minutes INT NOT NULL DEFAULT 0 AFTER review_count,
    ADD COLUMN IF NOT EXISTS last_studied_at DATETIME DEFAULT NULL AFTER completed_at;

ALTER TABLE knowledge_card
    ADD COLUMN IF NOT EXISTS explanation TEXT DEFAULT NULL AFTER answer,
    ADD COLUMN IF NOT EXISTS source_ref_id BIGINT DEFAULT NULL AFTER review_count,
    ADD COLUMN IF NOT EXISTS source_ref_type VARCHAR(32) DEFAULT NULL COMMENT 'knowledge_chunk / wrong_question' AFTER source_ref_id;

CREATE INDEX idx_card_task_current ON knowledge_card_task (user_id, is_current);
CREATE INDEX idx_card_task_source ON knowledge_card_task (user_id, source_type);
CREATE INDEX idx_card_source_ref ON knowledge_card (task_id, source_ref_type, source_ref_id);

CREATE TABLE IF NOT EXISTS daily_card_task (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    task_date DATE NOT NULL,
    planned_count INT NOT NULL DEFAULT 0,
    learn_count INT NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    completed_count INT NOT NULL DEFAULT 0,
    streak_snapshot INT NOT NULL DEFAULT 0,
    estimated_minutes INT NOT NULL DEFAULT 0,
    tomorrow_due_count INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending / completed',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_daily_card_task (user_id, task_id, task_date),
    KEY idx_daily_card_user_date (user_id, task_date),
    KEY idx_daily_card_task (task_id)
);
