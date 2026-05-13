package com.offerpilot.ai.service.impl;

import com.offerpilot.ai.service.PromptTemplateService;
import org.springframework.stereotype.Service;

@Service
public class DefaultPromptTemplateService implements PromptTemplateService {

    @Override
    public String chatPrompt() {
        return "You are OfferPilot, a Java backend interview tutor. Give concise, structured answers.";
    }

    @Override
    public String knowledgeChatPrompt() {
        return "You are OfferPilot, a Java backend interview tutor. Answer only with the provided knowledge context when possible, and stay honest when context is insufficient.";
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

    @Override
    public String knowledgeCardPrompt() {
        return """
                你是 OfferPilot 的知识卡片生成器。请根据给定文档内容，输出适合记忆复习的结构化卡片。

                输出要求：
                1. 只返回 JSON 数组，不要输出 markdown，不要解释。
                2. 每个元素格式必须为：
                   {"front":"...","back":"...","explanation":"...","tags":["..."],"difficulty":"easy|medium|hard","cardType":"concept|qa|scenario|compare","sourceQuote":"...","sourceChunkId":123}
                3. front 必须清晰、可直接回忆；back 必须简洁、准确、适合翻牌后快速核对。
                4. explanation 用于补充理解；tags 只保留 1-3 个高价值标签；sourceQuote 必须摘自原文且不宜过长。
                5. 优先提炼 Java、后端、系统设计、工程实践中的核心概念、区别、流程、注意点。
                6. 不要生成重复问题，不要输出空字段；difficulty 和 cardType 必须使用枚举值。
                """;
    }
}
