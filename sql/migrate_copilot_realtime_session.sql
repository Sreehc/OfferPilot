CREATE TABLE IF NOT EXISTS copilot_realtime_session (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    copilot_prep_session_id BIGINT NOT NULL,
    application_id BIGINT DEFAULT NULL,
    resume_file_id BIGINT DEFAULT NULL,
    job_prep_session_id BIGINT DEFAULT NULL,
    company VARCHAR(128) DEFAULT NULL,
    job_title VARCHAR(128) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'awaiting_connection',
    provider_status VARCHAR(32) NOT NULL DEFAULT 'ready',
    prep_summary VARCHAR(1000) DEFAULT NULL,
    live_checklist_json TEXT DEFAULT NULL,
    provider_readiness_json TEXT DEFAULT NULL,
    latest_event_summary VARCHAR(500) DEFAULT NULL,
    connected_at DATETIME DEFAULT NULL,
    disconnected_at DATETIME DEFAULT NULL,
    ended_at DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_copilot_realtime_user_update (user_id, update_time),
    KEY idx_copilot_realtime_prep (copilot_prep_session_id),
    KEY idx_copilot_realtime_job_prep (job_prep_session_id)
);

CREATE TABLE IF NOT EXISTS copilot_event (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    source VARCHAR(32) NOT NULL,
    summary VARCHAR(500) DEFAULT NULL,
    payload_json TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_copilot_event_session_time (session_id, create_time),
    KEY idx_copilot_event_user_time (user_id, create_time)
);
