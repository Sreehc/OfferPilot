CREATE DATABASE IF NOT EXISTS bytecoach DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bytecoach;

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

CREATE TABLE IF NOT EXISTS knowledge_doc (
    id BIGINT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    category_id BIGINT DEFAULT NULL,
    source_type VARCHAR(32) NOT NULL,
    file_url VARCHAR(255) DEFAULT NULL,
    summary VARCHAR(500) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

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

CREATE TABLE IF NOT EXISTS study_plan (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    goal VARCHAR(255) DEFAULT NULL,
    content TEXT DEFAULT NULL,
    days INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    start_date DATE DEFAULT NULL,
    end_date DATE DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_study_plan_user_id (user_id)
);

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

INSERT IGNORE INTO user (id, username, password, nickname, role, status, source, create_time, update_time)
VALUES
    (1, 'demo', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', 'ByteCoach', 'ADMIN', 1, 'seed', NOW(), NOW());

INSERT IGNORE INTO category (id, name, type, sort_order, status, create_time, update_time)
VALUES
    (1, 'Spring', 'question', 1, 1, NOW(), NOW()),
    (2, 'JVM', 'knowledge', 2, 1, NOW(), NOW()),
    (3, 'MySQL', 'interview', 3, 1, NOW(), NOW()),
    (4, 'JVM', 'question', 4, 1, NOW(), NOW()),
    (5, 'MySQL', 'question', 5, 1, NOW(), NOW());

INSERT IGNORE INTO question (
    id, title, category_id, type, difficulty, frequency, tags, standard_answer, score_standard, source, create_time, update_time
)
VALUES
    (
        1001,
        '解释 Spring AOP 的动态代理实现',
        1,
        'short_answer',
        'medium',
        8,
        'Spring,AOP,代理',
        'Spring AOP 常见实现方式是 JDK 动态代理和 CGLIB。接口代理优先使用 JDK 动态代理，目标类没有接口时通常使用 CGLIB。',
        '至少提到 JDK 动态代理、CGLIB 以及两者适用边界。',
        'seed',
        NOW(),
        NOW()
    ),
    (
        1002,
        'JVM CMS 和 G1 的主要差异是什么？',
        4,
        'short_answer',
        'hard',
        7,
        'JVM,GC,CMS,G1',
        'CMS 以最短停顿为目标但会产生内存碎片，G1 面向服务端大堆内存，按 Region 回收并兼顾吞吐与停顿。',
        '至少说明目标、内存管理方式和典型问题。',
        'seed',
        NOW(),
        NOW()
    ),
    (
        1003,
        'MySQL 为什么会出现索引失效？',
        5,
        'short_answer',
        'medium',
        6,
        'MySQL,索引,执行计划',
        '常见原因包括最左前缀被破坏、列上使用函数或表达式、隐式类型转换、范围查询后继续使用联合索引列等。',
        '至少给出 3 个典型场景。',
        'seed',
        NOW(),
        NOW()
    );
