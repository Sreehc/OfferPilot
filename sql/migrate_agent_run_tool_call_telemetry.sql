ALTER TABLE agent_run
    ADD COLUMN IF NOT EXISTS tool_call_telemetry_json TEXT DEFAULT NULL AFTER execution_result_json;
