-- V4: Voice Interview Support
-- Adds mode field to interview_session and creates voice_record table.

-- ============================================================
-- Extend interview_session with mode field
-- ============================================================
ALTER TABLE interview_session
    ADD COLUMN mode VARCHAR(32) NOT NULL DEFAULT 'text' COMMENT 'Interview mode: text or voice';

-- ============================================================
-- Voice record table — stores audio transcription data
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
