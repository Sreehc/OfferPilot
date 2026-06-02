ALTER TABLE agent_run
    ADD COLUMN IF NOT EXISTS approval_action_type VARCHAR(64) DEFAULT NULL AFTER requires_approval,
    ADD COLUMN IF NOT EXISTS approval_summary VARCHAR(500) DEFAULT NULL AFTER approval_action_type,
    ADD COLUMN IF NOT EXISTS approval_payload_json TEXT DEFAULT NULL AFTER approval_summary,
    ADD COLUMN IF NOT EXISTS execution_result_json TEXT DEFAULT NULL AFTER approval_payload_json,
    ADD COLUMN IF NOT EXISTS decision_note VARCHAR(500) DEFAULT NULL AFTER execution_result_json;
