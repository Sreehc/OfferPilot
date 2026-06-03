package com.offerpilot.agent.entity;

import java.util.Arrays;
import java.util.Locale;

public enum TriggerSource {
    MANUAL("manual"),
    DASHBOARD("dashboard"),
    ANALYTICS("analytics"),
    INTERVIEW("interview"),
    APPLICATIONS("applications"),
    RECORDING_REVIEW("recording_review"),
    INTERVIEW_LIVE("interview_live"),
    SETTINGS("settings");

    private final String value;

    TriggerSource(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static TriggerSource fromValue(String raw) {
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
