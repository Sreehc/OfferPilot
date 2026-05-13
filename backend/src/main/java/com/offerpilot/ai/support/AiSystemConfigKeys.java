package com.offerpilot.ai.support;

public final class AiSystemConfigKeys {

    private AiSystemConfigKeys() {
    }

    public static final String GROUP_LLM = "llm";
    public static final String GROUP_EMBEDDING = "embedding";
    public static final String GROUP_PROMPT = "prompt";

    public static final String LLM_ENABLED = "ai.llm.enabled";
    public static final String LLM_BASE_URL = "ai.llm.base-url";
    public static final String LLM_MODEL = "ai.llm.model";
    public static final String LLM_TIMEOUT_SECONDS = "ai.llm.timeout-seconds";

    public static final String EMBEDDING_ENABLED = "ai.embedding.enabled";
    public static final String EMBEDDING_BASE_URL = "ai.embedding.base-url";
    public static final String EMBEDDING_MODEL = "ai.embedding.model";
    public static final String EMBEDDING_DIMENSIONS = "ai.embedding.dimensions";

    public static final String PROMPT_CHAT = "prompt.chat";
    public static final String PROMPT_KNOWLEDGE = "prompt.knowledge";
    public static final String PROMPT_REFERENCE_CONSTRAINT = "prompt.reference-constraint";
    public static final String PROMPT_INTERVIEW_SCORE = "prompt.interview-score";
    public static final String PROMPT_FOLLOW_UP = "prompt.follow-up";
    public static final String PROMPT_KNOWLEDGE_CARD = "prompt.knowledge-card";
}
