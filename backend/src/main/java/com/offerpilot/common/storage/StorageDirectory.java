package com.offerpilot.common.storage;

public enum StorageDirectory {
    AVATAR("avatars", true),
    KNOWLEDGE("knowledge", false),
    INTERVIEW_AUDIO("interview-audio", false),
    RESUME("resumes", false),
    ATTACHMENT("attachments", false);

    private final String code;
    private final boolean publicReadable;

    StorageDirectory(String code, boolean publicReadable) {
        this.code = code;
        this.publicReadable = publicReadable;
    }

    public String getCode() {
        return code;
    }

    public boolean isPublicReadable() {
        return publicReadable;
    }
}
