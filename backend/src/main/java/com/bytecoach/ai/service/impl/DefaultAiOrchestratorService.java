package com.bytecoach.ai.service.impl;

import com.bytecoach.ai.dto.AiChatRequest;
import com.bytecoach.ai.dto.AiChatResponse;
import com.bytecoach.ai.service.AiOrchestratorService;
import com.bytecoach.ai.service.LlmGateway;
import com.bytecoach.ai.service.PromptTemplateService;
import com.bytecoach.chat.dto.ChatSendRequest;
import com.bytecoach.chat.vo.ChatMessageReferenceVO;
import com.bytecoach.chat.vo.ChatSendVO;
import com.bytecoach.interview.dto.InterviewAnswerRequest;
import com.bytecoach.interview.vo.InterviewAnswerVO;
import com.bytecoach.knowledge.service.KnowledgeRetrievalService;
import com.bytecoach.knowledge.vo.KnowledgeSearchVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAiOrchestratorService implements AiOrchestratorService {

    private final LlmGateway llmGateway;
    private final PromptTemplateService promptTemplateService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final ObjectMapper objectMapper;

    @Override
    public ChatSendVO answerChat(ChatSendRequest request) {
        List<KnowledgeSearchVO.Reference> references = "rag".equalsIgnoreCase(request.getMode())
                ? knowledgeRetrievalService.searchReferences(request.getMessage(), 5)
                : List.of();
        String systemPrompt = references.isEmpty()
                ? promptTemplateService.chatPrompt()
                : promptTemplateService.knowledgeChatPrompt() + "\n" + promptTemplateService.referenceConstraintPrompt();
        AiChatResponse response = callModel(systemPrompt, request.getMessage(), references);
        String answer = response.getContent().isBlank() ? fallbackAnswer(request, references) : response.getContent();
        return ChatSendVO.builder()
                .sessionId(request.getSessionId())
                .answer(answer)
                .references(references.stream().map(reference -> ChatMessageReferenceVO.builder()
                        .docId(reference.getDocId())
                        .docTitle(reference.getDocTitle())
                        .chunkId(reference.getChunkId())
                        .snippet(reference.getSnippet())
                        .build()).toList())
                .build();
    }

    @Override
    public InterviewAnswerVO scoreInterviewAnswer(InterviewAnswerRequest request) {
        String userPrompt = buildScoreUserPrompt(request);
        String rawContent;

        log.info("LLM scoring request: questionId={}, questionTitle={}", request.getQuestionId(), request.getQuestionTitle());
        try {
            AiChatResponse scoreResponse = llmGateway.chatCompletion(AiChatRequest.builder()
                    .systemPrompt(promptTemplateService.interviewScorePrompt())
                    .userPrompt(userPrompt)
                    .references(List.of())
                    .build());
            rawContent = scoreResponse.getContent();
            log.debug("LLM scoring raw response length: {}", rawContent.length());
        } catch (RuntimeException exception) {
            log.warn("LLM call failed for interview scoring, using fallback: {}", exception.getMessage());
            rawContent = "";
        }

        InterviewAnswerVO result = parseScoreResponse(rawContent, request);
        log.info("Interview score result: questionId={}, score={}, addedToWrongBook={}", request.getQuestionId(), result.getScore(), result.getAddedToWrongBook());
        return result;
    }

    private String buildScoreUserPrompt(InterviewAnswerRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Question\n");
        if (request.getQuestionTitle() != null) {
            sb.append(request.getQuestionTitle());
        } else {
            sb.append("Question ID: ").append(request.getQuestionId());
        }
        sb.append("\n\n## Candidate's Answer\n").append(request.getAnswer());

        if (request.getStandardAnswer() != null && !request.getStandardAnswer().isBlank()) {
            sb.append("\n\n## Reference Standard Answer\n").append(request.getStandardAnswer());
        }
        if (request.getScoreStandard() != null && !request.getScoreStandard().isBlank()) {
            sb.append("\n\n## Scoring Criteria\n").append(request.getScoreStandard());
        }
        return sb.toString();
    }

    private InterviewAnswerVO parseScoreResponse(String rawContent, InterviewAnswerRequest request) {
        if (rawContent == null || rawContent.isBlank()) {
            return fallbackScoreResult(request);
        }

        try {
            // Try to extract JSON from the response (LLM may wrap it in markdown code blocks)
            String jsonContent = extractJson(rawContent);
            JsonNode node = objectMapper.readTree(jsonContent);

            BigDecimal score = node.has("score")
                    ? new BigDecimal(node.get("score").asInt())
                    : new BigDecimal("60");
            String comment = node.has("comment") ? node.get("comment").asText() : "评分解析完成。";
            String standardAnswer = node.has("standardAnswer") ? node.get("standardAnswer").asText()
                    : (request.getStandardAnswer() != null ? request.getStandardAnswer() : "暂无标准答案。");
            String followUp = node.has("followUp") ? node.get("followUp").asText() : "能否进一步展开说明？";

            // Clamp score to 0-100
            if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;
            if (score.compareTo(new BigDecimal("100")) > 0) score = new BigDecimal("100");

            return InterviewAnswerVO.builder()
                    .score(score)
                    .comment(comment)
                    .standardAnswer(standardAnswer)
                    .followUp(followUp)
                    .addedToWrongBook(false) // will be set by InterviewServiceImpl
                    .hasNextQuestion(true)   // will be set by InterviewServiceImpl
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse LLM score response as JSON, using fallback: {}", e.getMessage());
            return fallbackScoreResult(request);
        }
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        // Strip markdown code fences if present
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private InterviewAnswerVO fallbackScoreResult(InterviewAnswerRequest request) {
        String standardAnswer = request.getStandardAnswer() != null
                ? request.getStandardAnswer()
                : "暂无标准答案，请参考题目相关知识点。";
        return InterviewAnswerVO.builder()
                .score(new BigDecimal("60"))
                .comment("AI 评分暂时不可用，已给出默认分数。请稍后重试。")
                .standardAnswer(standardAnswer)
                .followUp("能否从另一个角度谈谈你的理解？")
                .addedToWrongBook(false)
                .hasNextQuestion(true)
                .build();
    }

    private AiChatResponse callModel(String systemPrompt, String userPrompt, List<KnowledgeSearchVO.Reference> references) {
        log.info("LLM chat request: mode={}, refCount={}, promptLength={}", references.isEmpty() ? "chat" : "rag", references.size(), userPrompt.length());
        try {
            AiChatResponse response = llmGateway.chatCompletion(AiChatRequest.builder()
                    .systemPrompt(systemPrompt)
                    .userPrompt(userPrompt)
                    .references(references.stream()
                            .map(reference -> reference.getDocTitle() + ": " + reference.getSnippet())
                            .toList())
                    .build());
            log.debug("LLM chat response length: {}", response.getContent().length());
            return response;
        } catch (RuntimeException exception) {
            log.warn("LLM chat call failed, using fallback: {}", exception.getMessage());
            return AiChatResponse.builder().content("").raw("fallback").build();
        }
    }

    private String fallbackAnswer(ChatSendRequest request, List<KnowledgeSearchVO.Reference> references) {
        if (!references.isEmpty()) {
            String snippets = references.stream()
                    .map(KnowledgeSearchVO.Reference::getSnippet)
                    .limit(2)
                    .reduce((left, right) -> left + "\n\n" + right)
                    .orElse("");
            return "根据当前知识库命中的片段，先给出一个可复述版本：\n\n" + snippets;
        }
        return "这是普通问答模式的兜底回答：当前未命中知识库上下文，你可以继续追问更具体的 Java 后端问题。";
    }
}
