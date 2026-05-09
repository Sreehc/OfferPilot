CREATE TABLE IF NOT EXISTS daily_memory_snapshot (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    today_card_total INT NOT NULL DEFAULT 0,
    today_completed_cards INT NOT NULL DEFAULT 0,
    today_completion_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    review_debt_count INT NOT NULL DEFAULT 0,
    mastered_card_count INT NOT NULL DEFAULT 0,
    due_today_count INT NOT NULL DEFAULT 0,
    reviewed_today_count INT NOT NULL DEFAULT 0,
    study_streak INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_daily_memory_snapshot (user_id, snapshot_date),
    KEY idx_daily_memory_snapshot_user_date (user_id, snapshot_date)
);
