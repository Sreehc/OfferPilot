-- V5: Learning Community
-- Adds community questions, answers, votes, and user stats tables.

-- ============================================================
-- Community question table — user-submitted questions
-- ============================================================
CREATE TABLE IF NOT EXISTS community_question (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    category_id BIGINT DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'published' COMMENT 'published / hidden / deleted',
    upvote_count INT NOT NULL DEFAULT 0,
    answer_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_cq_user (user_id),
    KEY idx_cq_category (category_id),
    KEY idx_cq_status (status)
);

-- ============================================================
-- Community answer table — answers to community questions
-- ============================================================
CREATE TABLE IF NOT EXISTS community_answer (
    id BIGINT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_accepted TINYINT NOT NULL DEFAULT 0,
    upvote_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ca_question (question_id),
    KEY idx_ca_user (user_id)
);

-- ============================================================
-- Community vote table — prevents duplicate votes
-- ============================================================
CREATE TABLE IF NOT EXISTS community_vote (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL COMMENT 'question / answer',
    target_id BIGINT NOT NULL,
    value TINYINT NOT NULL DEFAULT 1 COMMENT '1 = upvote',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vote (user_id, target_type, target_id),
    KEY idx_cv_user (user_id)
);

-- ============================================================
-- User stats table — aggregated learning stats and rank
-- ============================================================
CREATE TABLE IF NOT EXISTS user_stats (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    total_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    interview_count INT NOT NULL DEFAULT 0,
    avg_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    review_streak INT NOT NULL DEFAULT 0,
    total_reviews INT NOT NULL DEFAULT 0,
    community_score INT NOT NULL DEFAULT 0,
    community_questions INT NOT NULL DEFAULT 0,
    community_answers INT NOT NULL DEFAULT 0,
    community_accepted INT NOT NULL DEFAULT 0,
    rank_title VARCHAR(64) NOT NULL DEFAULT '见习生',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_us_user (user_id)
);
