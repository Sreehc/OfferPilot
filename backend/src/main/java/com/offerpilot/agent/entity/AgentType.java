package com.offerpilot.agent.entity;

import java.util.Arrays;
import java.util.Locale;

public enum AgentType {
    COORDINATOR("coordinator"),
    STUDY_PLANNER("study_planner"),
    INTERVIEW_REVIEW("interview_review"),
    RESUME_COACH("resume_coach"),
    APPLICATION_STRATEGIST("application_strategist"),
    JOB_PREP("job_prep"),
    RECORDING_REVIEW("recording_review"),
    REALTIME_COPILOT("realtime_copilot");

    private final String value;

    AgentType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static AgentType fromValue(String raw) {
        String normalized = normalize(raw);
        return Arrays.stream(values())
                .filter(item -> item.value.equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
