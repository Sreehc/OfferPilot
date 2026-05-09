-- ByteCoach Database Schema
-- Run this file to create the database and all tables.

CREATE DATABASE IF NOT EXISTS bytecoach DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bytecoach;

-- ============================================================
-- 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar VARCHAR(255) DEFAULT NULL,
    email VARCHAR(128) DEFAULT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    status TINYINT NOT NULL DEFAULT 1,
    source VARCHAR(32) DEFAULT 'system',
    remark VARCHAR(255) DEFAULT NULL,
    last_login_time DATETIME DEFAULT NULL,
    totp_secret VARCHAR(64) DEFAULT NULL COMMENT 'TOTP 密钥（AES 加密存储）',
    totp_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用两步验证',
    recovery_codes TEXT DEFAULT NULL COMMENT '恢复码 JSON 数组（AES 加密存储）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- 分类表
-- ============================================================
CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_category_type CHECK (type IN ('question', 'knowledge', 'interview'))
);

-- ============================================================
-- 聊天会话表
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    last_message_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_chat_session_user_id (user_id),
    CONSTRAINT ck_chat_session_mode CHECK (mode IN ('chat', 'rag'))
);

-- ============================================================
-- 聊天消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    reference_json JSON DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_chat_message_session_id (session_id),
    KEY idx_chat_message_user_id (user_id)
);

-- ============================================================
-- 知识文档表
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_doc (
    id BIGINT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    category_id BIGINT DEFAULT NULL,
    user_id BIGINT DEFAULT NULL COMMENT '上传用户ID，NULL表示系统内置',
    source_type VARCHAR(32) NOT NULL DEFAULT 'system' COMMENT 'system=系统内置, user_upload=用户上传',
    file_url VARCHAR(255) DEFAULT NULL,
    summary VARCHAR(500) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_knowledge_doc_user_id (user_id)
);

-- ============================================================
-- 知识分片表
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    token_count INT DEFAULT 0,
    vector_id VARCHAR(128) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_knowledge_chunk_doc_id (doc_id)
);

-- ============================================================
-- 知识卡片任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_card_task (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    doc_id BIGINT NOT NULL,
    doc_title VARCHAR(128) NOT NULL,
    deck_title VARCHAR(128) DEFAULT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'knowledge_doc' COMMENT 'knowledge_doc / wrong_auto',
    status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT 'draft / active / completed / invalid',
    is_current TINYINT NOT NULL DEFAULT 0,
    days INT NOT NULL DEFAULT 7,
    current_day INT NOT NULL DEFAULT 1,
    daily_target INT NOT NULL DEFAULT 0,
    total_cards INT NOT NULL DEFAULT 0,
    mastered_cards INT NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    estimated_minutes INT NOT NULL DEFAULT 0,
    invalid_reason VARCHAR(255) DEFAULT NULL,
    started_at DATETIME DEFAULT NULL,
    completed_at DATETIME DEFAULT NULL,
    last_studied_at DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_card_task_user (user_id),
    KEY idx_card_task_doc (doc_id),
    KEY idx_card_task_status (user_id, status),
    KEY idx_card_task_current (user_id, is_current),
    KEY idx_card_task_source (user_id, source_type)
);

-- ============================================================
-- 知识卡片表
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_card (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    explanation TEXT DEFAULT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    scheduled_day INT NOT NULL DEFAULT 1,
    state VARCHAR(32) NOT NULL DEFAULT 'new' COMMENT 'new / learning / weak / mastered',
    review_count INT NOT NULL DEFAULT 0,
    source_ref_id BIGINT DEFAULT NULL,
    source_ref_type VARCHAR(32) DEFAULT NULL COMMENT 'knowledge_chunk / wrong_question',
    last_rating INT DEFAULT NULL COMMENT '1=Again, 2=Hard, 3=Good, 4=Easy',
    last_review_time DATETIME DEFAULT NULL,
    ease_factor DECIMAL(4,2) NOT NULL DEFAULT 2.50,
    interval_days INT NOT NULL DEFAULT 0,
    streak INT NOT NULL DEFAULT 0,
    next_review_at DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_card_task_id (task_id),
    KEY idx_card_task_state (task_id, state),
    KEY idx_card_task_day (task_id, scheduled_day),
    KEY idx_card_source_ref (task_id, source_ref_type, source_ref_id)
);

-- ============================================================
-- 知识卡片评分记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_card_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    rating INT NOT NULL COMMENT '1=Again, 2=Hard, 3=Good, 4=Easy',
    response_time_ms INT DEFAULT NULL,
    ease_factor_before DECIMAL(4,2) NOT NULL,
    interval_before INT NOT NULL,
    ease_factor_after DECIMAL(4,2) NOT NULL,
    interval_after INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_card_log_user (user_id),
    KEY idx_card_log_task (task_id),
    KEY idx_card_log_card (card_id)
);

-- ============================================================
-- 每日卡片任务快照表
-- ============================================================
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

-- ============================================================
-- 题目表
-- ============================================================
CREATE TABLE IF NOT EXISTS question (
    id BIGINT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    type VARCHAR(32) DEFAULT NULL,
    difficulty VARCHAR(32) NOT NULL,
    frequency INT DEFAULT 0,
    tags VARCHAR(255) DEFAULT NULL,
    standard_answer TEXT DEFAULT NULL,
    score_standard TEXT DEFAULT NULL,
    source VARCHAR(32) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- 面试会话表
-- ============================================================
CREATE TABLE IF NOT EXISTS interview_session (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    direction VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_score DECIMAL(5, 2) DEFAULT NULL,
    question_count INT NOT NULL,
    current_index INT NOT NULL DEFAULT 1,
    start_time DATETIME DEFAULT NULL,
    end_time DATETIME DEFAULT NULL,
    mode VARCHAR(32) NOT NULL DEFAULT 'text' COMMENT 'Interview mode: text or voice',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_interview_session_user_id (user_id)
);

-- ============================================================
-- 面试记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS interview_record (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    user_answer TEXT DEFAULT NULL,
    score DECIMAL(5, 2) DEFAULT NULL,
    comment TEXT DEFAULT NULL,
    follow_up TEXT DEFAULT NULL,
    is_wrong TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_interview_record_session_id (session_id),
    KEY idx_interview_record_user_id (user_id)
);

-- ============================================================
-- 语音记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS voice_record (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    record_id BIGINT NOT NULL COMMENT 'Links to interview_record.id',
    user_id BIGINT NOT NULL,
    audio_url VARCHAR(512) DEFAULT NULL COMMENT 'Path to stored audio file',
    transcript TEXT DEFAULT NULL COMMENT 'STT transcription result',
    transcript_confidence DECIMAL(4,3) DEFAULT NULL COMMENT 'STT confidence score 0.000-1.000',
    transcript_time_ms INT DEFAULT NULL COMMENT 'STT processing time in ms',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_voice_record_session (session_id),
    KEY idx_voice_record_record (record_id),
    KEY idx_voice_record_user (user_id)
);

-- ============================================================
-- 错题表（含 SM-2 间隔复习字段）
-- ============================================================
CREATE TABLE IF NOT EXISTS wrong_question (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    user_answer TEXT DEFAULT NULL,
    standard_answer TEXT DEFAULT NULL,
    error_reason TEXT DEFAULT NULL,
    mastery_level VARCHAR(32) NOT NULL DEFAULT 'not_started',
    review_count INT NOT NULL DEFAULT 0,
    last_review_time DATETIME DEFAULT NULL,
    ease_factor DECIMAL(4,2) NOT NULL DEFAULT 2.50 COMMENT 'SM-2 easiness factor (1.30 min)',
    interval_days INT NOT NULL DEFAULT 0 COMMENT 'Current review interval in days',
    next_review_date DATE DEFAULT NULL COMMENT 'Next scheduled review date',
    streak INT NOT NULL DEFAULT 0 COMMENT 'Consecutive successful reviews (rating >= 3)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_wrong_question_user_id (user_id),
    KEY idx_wrong_next_review (user_id, next_review_date),
    UNIQUE KEY uk_user_question (user_id, question_id),
    CONSTRAINT ck_wrong_source_type CHECK (source_type IN ('interview', 'chat'))
);

-- ============================================================
-- 复习记录表
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

-- ============================================================
-- 社区问题表
-- ============================================================
CREATE TABLE IF NOT EXISTS community_question (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    category_id BIGINT DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending / approved / rejected / hidden / deleted',
    upvote_count INT NOT NULL DEFAULT 0,
    answer_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_cq_user (user_id),
    KEY idx_cq_category (category_id),
    KEY idx_cq_status (status)
);

-- ============================================================
-- 社区回答表
-- ============================================================
CREATE TABLE IF NOT EXISTS community_answer (
    id BIGINT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending / approved / rejected',
    is_accepted TINYINT NOT NULL DEFAULT 0,
    upvote_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ca_question (question_id),
    KEY idx_ca_user (user_id)
);

-- ============================================================
-- 社区投票表
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
-- 用户统计表
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

-- ============================================================
-- 通知表
-- ============================================================
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(64) NOT NULL COMMENT 'review_remind / interview_feedback / community_accept / community_vote / rank_upgrade',
    title VARCHAR(200) NOT NULL,
    content TEXT,
    link VARCHAR(500),
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notification_user_read (user_id, is_read),
    INDEX idx_notification_user_time (user_id, create_time DESC)
);

-- ============================================================
-- 登录设备表
-- ============================================================
CREATE TABLE IF NOT EXISTS login_device (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_fingerprint VARCHAR(128) NOT NULL COMMENT '浏览器/设备指纹',
    device_name VARCHAR(128) DEFAULT NULL COMMENT '设备名称，如 Chrome on Windows',
    ip VARCHAR(64) DEFAULT NULL,
    city VARCHAR(64) DEFAULT NULL COMMENT 'IP 归属城市（可选解析）',
    last_active_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=活跃, 0=已撤销',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_login_device_user (user_id),
    KEY idx_login_device_fp (user_id, device_fingerprint)
);

-- ============================================================
-- 登录日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS login_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ip VARCHAR(64) DEFAULT NULL,
    city VARCHAR(64) DEFAULT NULL COMMENT 'IP 归属城市',
    device VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
    status TINYINT NOT NULL COMMENT '1=成功, 0=失败',
    fail_reason VARCHAR(128) DEFAULT NULL COMMENT '失败原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_login_log_user (user_id),
    KEY idx_login_log_time (create_time)
);
