package com.bytecoach.ai.service;

public interface PromptTemplateService {
    String chatPrompt();

    String knowledgeChatPrompt();

    String referenceConstraintPrompt();

    String interviewScorePrompt();

    String followUpPrompt();
}
