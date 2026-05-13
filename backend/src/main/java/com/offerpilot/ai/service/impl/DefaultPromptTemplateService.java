package com.offerpilot.ai.service.impl;

import com.offerpilot.ai.service.PromptTemplateService;
import com.offerpilot.ai.service.SystemConfigService;
import com.offerpilot.ai.support.AiSystemConfigKeys;
import com.offerpilot.ai.support.PromptTemplateDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultPromptTemplateService implements PromptTemplateService {

    private final SystemConfigService systemConfigService;

    @Override
    public String chatPrompt() {
        return systemConfigService.getString(AiSystemConfigKeys.PROMPT_CHAT, PromptTemplateDefaults.CHAT);
    }

    @Override
    public String knowledgeChatPrompt() {
        return systemConfigService.getString(AiSystemConfigKeys.PROMPT_KNOWLEDGE, PromptTemplateDefaults.KNOWLEDGE);
    }

    @Override
    public String referenceConstraintPrompt() {
        return systemConfigService.getString(
                AiSystemConfigKeys.PROMPT_REFERENCE_CONSTRAINT,
                PromptTemplateDefaults.REFERENCE_CONSTRAINT);
    }

    @Override
    public String interviewScorePrompt() {
        return systemConfigService.getString(
                AiSystemConfigKeys.PROMPT_INTERVIEW_SCORE,
                PromptTemplateDefaults.INTERVIEW_SCORE);
    }

    @Override
    public String followUpPrompt() {
        return systemConfigService.getString(AiSystemConfigKeys.PROMPT_FOLLOW_UP, PromptTemplateDefaults.FOLLOW_UP);
    }

    @Override
    public String knowledgeCardPrompt() {
        return systemConfigService.getString(
                AiSystemConfigKeys.PROMPT_KNOWLEDGE_CARD,
                PromptTemplateDefaults.KNOWLEDGE_CARD);
    }
}
