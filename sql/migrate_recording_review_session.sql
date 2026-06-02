CREATE TABLE IF NOT EXISTS recording_review_session (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    direction VARCHAR(64) DEFAULT NULL,
    job_role VARCHAR(128) DEFAULT NULL,
    notes VARCHAR(500) DEFAULT NULL,
    audio_url VARCHAR(255) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ready',
    transcript MEDIUMTEXT DEFAULT NULL,
    transcript_confidence DECIMAL(5,4) DEFAULT NULL,
    transcript_time_ms INT DEFAULT NULL,
    overall_score DECIMAL(5,2) DEFAULT NULL,
    summary VARCHAR(1000) DEFAULT NULL,
    strengths_json TEXT DEFAULT NULL,
    weak_points_json TEXT DEFAULT NULL,
    suggested_actions_json TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_recording_review_user_update (user_id, update_time)
);

CREATE TABLE IF NOT EXISTS recording_transcript_segment (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    segment_index INT NOT NULL,
    transcript_text TEXT NOT NULL,
    start_offset_ms INT DEFAULT NULL,
    end_offset_ms INT DEFAULT NULL,
    signal_type VARCHAR(32) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_recording_segment_session (session_id, segment_index),
    KEY idx_recording_segment_user (user_id, create_time)
);
