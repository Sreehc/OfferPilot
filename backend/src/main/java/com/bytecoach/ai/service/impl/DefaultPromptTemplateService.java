package com.bytecoach.ai.service.impl;

import com.bytecoach.ai.service.PromptTemplateService;
import org.springframework.stereotype.Service;

@Service
public class DefaultPromptTemplateService implements PromptTemplateService {

    @Override
    public String chatPrompt() {
        return "You are ByteCoach, a Java backend interview tutor. Give concise, structured answers.";
    }

    @Override
    public String knowledgeChatPrompt() {
        return "You are ByteCoach, a Java backend interview tutor. Answer only with the provided knowledge context when possible, and stay honest when context is insufficient.";
    }

    @Override
    public String referenceConstraintPrompt() {
        return "When references are provided, ground the answer in them and keep the explanation compact and interview-oriented.";
    }

    @Override
    public String interviewScorePrompt() {
        return """
                You are a strict Java backend interview evaluator. Score the candidate's answer from 0 to 100.

                You MUST respond with a single JSON object (no markdown, no extra text) in this exact format:
                {
                  "score": <integer 0-100>,
                  "comment": "<brief evaluation of the answer's strengths and weaknesses in Chinese>",
                  "standardAnswer": "<a concise standard answer covering the key points in Chinese>",
                  "followUp": "<one follow-up question to probe deeper understanding>"
                }

                Scoring criteria:
                - 80-100: Comprehensive, accurate, well-structured answer
                - 60-79: Correct core concepts but missing some details
                - 40-59: Partially correct but significant gaps
                - 0-39: Largely incorrect or irrelevant
                """;
    }

    @Override
    public String followUpPrompt() {
        return "Generate one follow-up question to probe depth only once.";
    }
}
