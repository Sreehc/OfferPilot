ALTER TABLE recording_review_session
    ADD COLUMN IF NOT EXISTS status_message VARCHAR(500) DEFAULT NULL AFTER status;
