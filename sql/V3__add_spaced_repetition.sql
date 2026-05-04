-- V3: Spaced Repetition Engine
-- Adds SM-2 fields to wrong_question and creates review_log table.

-- ============================================================
-- Extend wrong_question with SM-2 fields
-- ============================================================
ALTER TABLE wrong_question
    ADD COLUMN ease_factor DECIMAL(4,2) NOT NULL DEFAULT 2.50 COMMENT 'SM-2 easiness factor (1.30 min)',
    ADD COLUMN interval_days INT NOT NULL DEFAULT 0 COMMENT 'Current review interval in days',
    ADD COLUMN next_review_date DATE DEFAULT NULL COMMENT 'Next scheduled review date',
    ADD COLUMN streak INT NOT NULL DEFAULT 0 COMMENT 'Consecutive successful reviews (rating >= 3)';

-- Index for fetching today's reviews efficiently
ALTER TABLE wrong_question
    ADD INDEX idx_wrong_next_review (user_id, next_review_date);

-- Initialize existing records: schedule all for today
UPDATE wrong_question
SET next_review_date = CURDATE(),
    ease_factor = 2.50,
    interval_days = 1
WHERE next_review_date IS NULL;

-- ============================================================
-- Review log table — records every review attempt
-- ============================================================
CREATE TABLE IF NOT EXISTS review_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wrong_question_id BIGINT NOT NULL,
    rating INT NOT NULL COMMENT 'User rating 1-4 (1=Again, 2=Hard, 3=Good, 4=Easy)',
    response_time_ms INT DEFAULT NULL COMMENT 'Time spent reviewing in ms',
    ease_factor_before DECIMAL(4,2) NOT NULL COMMENT 'EF before this review',
    interval_before INT NOT NULL COMMENT 'Interval before this review',
    ease_factor_after DECIMAL(4,2) NOT NULL COMMENT 'EF after this review',
    interval_after INT NOT NULL COMMENT 'Interval after this review',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_review_log_user (user_id),
    KEY idx_review_log_wq (wrong_question_id)
);
