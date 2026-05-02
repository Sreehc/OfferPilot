-- ByteCoach Database Schema
-- Run this file to create the database and tables.

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
-- 错题表
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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_wrong_question_user_id (user_id),
    UNIQUE KEY uk_user_question (user_id, question_id),
    CONSTRAINT ck_wrong_source_type CHECK (source_type IN ('interview', 'chat'))
);

-- ============================================================
-- 学习计划表
-- ============================================================
CREATE TABLE IF NOT EXISTS study_plan (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    goal VARCHAR(255) DEFAULT NULL,
    content TEXT DEFAULT NULL,
    days INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    version INT NOT NULL DEFAULT 1 COMMENT '计划版本号',
    parent_plan_id BIGINT DEFAULT NULL COMMENT '前一版本计划ID，用于追踪调整历史',
    start_date DATE DEFAULT NULL,
    end_date DATE DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_study_plan_user_id (user_id)
);

-- ============================================================
-- 学习计划任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS study_plan_task (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    task_date DATE NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    content VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'todo',
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_study_plan_task_plan_id (plan_id),
    KEY idx_study_plan_task_user_id (user_id)
);
