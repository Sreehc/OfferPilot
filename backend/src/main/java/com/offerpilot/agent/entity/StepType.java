package com.offerpilot.agent.entity;

import java.util.Arrays;
import java.util.Locale;

public enum StepType {
    ANALYZE("analyze"),
    RETRIEVE("retrieve"),
    SCORE("score"),
    UPDATE_PROFILE("update_profile"),
    SCHEDULE_REVIEW("schedule_review"),
    PREPARE_REALTIME("prepare_realtime"),
    WAIT_TRANSCRIPTION("wait_transcription"),
    WAIT_APPROVAL("wait_approval");

    private final String value;

    StepType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static StepType fromValue(String raw) {
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
