package com.offerpilot.ai.service.impl;

import com.offerpilot.ai.dto.AiChatRequest;
import com.offerpilot.ai.dto.AiChatResponse;
import com.offerpilot.ai.service.AiOrchestratorService;
import com.offerpilot.ai.service.LlmGateway;
import com.offerpilot.ai.service.PromptTemplateService;
import com.offerpilot.chat.dto.ChatSendRequest;
import com.offerpilot.chat.vo.ChatMessageReferenceVO;
import com.offerpilot.chat.vo.ChatSendVO;
import com.offerpilot.interview.dto.InterviewAnswerRequest;
import com.offerpilot.interview.vo.InterviewAnswerVO;
import com.offerpilot.knowledge.service.KnowledgeRetrievalService;
import com.offerpilot.knowledge.vo.KnowledgeSearchVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
                ? knowledgeRetrievalService.searchReferences(
                        request.getMessage(), 5, request.getKnowledgeScope(), request.getUserId())
                : List.of();
        String systemPrompt = buildChatPrompt(request, references);
        AiChatResponse response = callModel(systemPrompt, request.getMessage(), references);
        String answer = response.getContent().isBlank() ? fallbackAnswer(request, references) : response.getContent();
        return ChatSendVO.builder()
                .sessionId(request.getSessionId())
                .answer(answer)
                .answerMode(request.getAnswerMode())
                .knowledgeScope(request.getKnowledgeScope())
                .references(references.stream().map(reference -> ChatMessageReferenceVO.builder()
                        .docId(reference.getDocId())
                        .docTitle(reference.getDocTitle())
                        .chunkId(reference.getChunkId())
                        .snippet(reference.getSnippet())
                        .score(reference.getScore())
                        .libraryScope(reference.getLibraryScope())
                        .businessType(reference.getBusinessType())
                        .fileType(reference.getFileType())
                        .build()).toList())
                .suggestedQuestions(buildSuggestedQuestions(request))
                .build();
    }

    @Override
    public List<ChatMessageReferenceVO> streamChat(ChatSendRequest request, Consumer<String> onToken) {
        List<KnowledgeSearchVO.Reference> references = "rag".equalsIgnoreCase(request.getMode())
                ? knowledgeRetrievalService.searchReferences(
                        request.getMessage(), 5, request.getKnowledgeScope(), request.getUserId())
                : List.of();
        String systemPrompt = buildChatPrompt(request, references);

        try {
            AiChatResponse response = llmGateway.streamCompletion(
                    AiChatRequest.builder()
                            .systemPrompt(systemPrompt)
                            .userPrompt(request.getMessage())
                            .references(references.stream()
                                    .map(r -> r.getDocTitle() + ": " + r.getSnippet())
                                    .toList())
                            .scene("chat.stream." + (request.getMode() == null ? "chat" : request.getMode().toLowerCase()))
                            .build(),
                    onToken);

            // If streaming returned empty content, send fallback as tokens
            if (response.getContent().isBlank()) {
                String fallback = fallbackAnswer(request, references);
                onToken.accept(fallback);
            }
        } catch (Exception e) {
            log.warn("Streaming chat failed: {}", e.getMessage());
            String fallback = fallbackAnswer(request, references);
            onToken.accept(fallback);
        }

        return references.stream().map(reference -> ChatMessageReferenceVO.builder()
                .docId(reference.getDocId())
                .docTitle(reference.getDocTitle())
                .chunkId(reference.getChunkId())
                .snippet(reference.getSnippet())
                .score(reference.getScore())
                .libraryScope(reference.getLibraryScope())
                .businessType(reference.getBusinessType())
                .fileType(reference.getFileType())
                .build()).toList();
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
                    .scene("interview.score")
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
        if (request.getDirection() != null && !request.getDirection().isBlank()) {
            sb.append("\n\n## Interview Direction\n").append(request.getDirection());
        }
        if (request.getJobRole() != null && !request.getJobRole().isBlank()) {
            sb.append("\n\n## Target Role\n").append(request.getJobRole());
        }
        if (request.getExperienceLevel() != null && !request.getExperienceLevel().isBlank()) {
            sb.append("\n\n## Experience Level\n").append(request.getExperienceLevel());
        }
        if (request.getTechStack() != null && !request.getTechStack().isBlank()) {
            sb.append("\n\n## Tech Stack Focus\n").append(request.getTechStack());
        }
        if (Boolean.TRUE.equals(request.getIncludeResumeProject())) {
            sb.append("\n\n## Resume Context\nPlease evaluate whether the answer can connect to real project experience.");
        }
        if (request.getContextSummary() != null && !request.getContextSummary().isBlank()) {
            sb.append("\n\n## Bound Context\n").append(request.getContextSummary());
            if (request.getContextType() != null && !request.getContextType().isBlank()) {
                sb.append("\nContext type: ").append(request.getContextType());
            }
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
            List<InterviewAnswerVO.ScoreDimensionVO> scoreBreakdown = parseScoreBreakdown(node.get("scoreBreakdown"), score.intValue());
            List<String> weakPointTags = parseWeakPointTags(node.get("weakPointTags"));
            String reviewSummary = node.has("reviewSummary")
                    ? node.get("reviewSummary").asText()
                    : "建议围绕关键原理、表达结构和工程落地补齐这道题。";

            // Clamp score to 0-100
            if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;
            if (score.compareTo(new BigDecimal("100")) > 0) score = new BigDecimal("100");

            return InterviewAnswerVO.builder()
                    .score(score)
                    .comment(comment)
                    .standardAnswer(standardAnswer)
                    .followUp(followUp)
                    .scoreBreakdown(scoreBreakdown)
                    .weakPointTags(weakPointTags)
                    .reviewSummary(reviewSummary)
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
                .scoreBreakdown(List.of(
                        InterviewAnswerVO.ScoreDimensionVO.builder().dimension("概念准确性").score(60).summary("基础概念待进一步核对").build(),
                        InterviewAnswerVO.ScoreDimensionVO.builder().dimension("结构表达").score(60).summary("建议先结论后展开").build(),
                        InterviewAnswerVO.ScoreDimensionVO.builder().dimension("工程落地").score(60).summary("可以补充真实项目场景").build()))
                .weakPointTags(List.of("表达结构", "原理细节"))
                .reviewSummary("建议回到标准答案，先补齐核心知识点，再练习 1 分钟表达。")
                .addedToWrongBook(false)
                .hasNextQuestion(true)
                .build();
    }

    private List<InterviewAnswerVO.ScoreDimensionVO> parseScoreBreakdown(JsonNode node, int fallbackScore) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return List.of(
                    InterviewAnswerVO.ScoreDimensionVO.builder().dimension("概念准确性").score(fallbackScore).summary("请继续核对关键概念").build(),
                    InterviewAnswerVO.ScoreDimensionVO.builder().dimension("结构表达").score(fallbackScore).summary("建议先给结论再展开").build(),
                    InterviewAnswerVO.ScoreDimensionVO.builder().dimension("工程落地").score(fallbackScore).summary("可以补充项目场景与权衡").build());
        }
        List<InterviewAnswerVO.ScoreDimensionVO> dimensions = new ArrayList<>();
        for (JsonNode item : node) {
            dimensions.add(InterviewAnswerVO.ScoreDimensionVO.builder()
                    .dimension(item.has("dimension") ? item.get("dimension").asText() : "维度")
                    .score(item.has("score") ? item.get("score").asInt(fallbackScore) : fallbackScore)
                    .summary(item.has("summary") ? item.get("summary").asText() : "建议继续补充")
                    .build());
        }
        return dimensions;
    }

    private List<String> parseWeakPointTags(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return List.of("原理细节", "表达结构");
        }
        List<String> tags = new ArrayList<>();
        for (JsonNode item : node) {
            if (item != null && !item.asText().isBlank()) {
                tags.add(item.asText());
            }
        }
        return tags.isEmpty() ? List.of("原理细节", "表达结构") : tags.stream().distinct().limit(3).toList();
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
                    .scene("chat.answer." + (references.isEmpty() ? "chat" : "rag"))
                    .build());
            log.debug("LLM chat response length: {}", response.getContent().length());
            return response;
        } catch (RuntimeException exception) {
            log.warn("LLM chat call failed, using fallback: {}", exception.getMessage());
            return AiChatResponse.builder().content("").raw("fallback").build();
        }
    }

    private String fallbackAnswer(ChatSendRequest request, List<KnowledgeSearchVO.Reference> references) {
        String contextLead = request.getContextSummary() == null || request.getContextSummary().isBlank()
                ? ""
                : "已结合当前绑定上下文：" + request.getContextSummary() + "\n\n";
        if (!references.isEmpty()) {
            String snippets = references.stream()
                    .map(KnowledgeSearchVO.Reference::getSnippet)
                    .limit(2)
                    .reduce((left, right) -> left + "\n\n" + right)
                    .orElse("");
            return switch ((request.getAnswerMode() == null ? "learning" : request.getAnswerMode()).toLowerCase()) {
                case "interview" -> contextLead + "根据当前知识库命中的片段，先给你一个更适合面试表达的回答骨架：\n\n" + snippets;
                case "concise" -> contextLead + "根据当前知识库命中的片段，先给你一个简明版答案：\n\n" + snippets;
                case "project" -> contextLead + "根据当前知识库命中的片段，先给你一个偏项目结合的回答版本：\n\n" + snippets;
                default -> contextLead + "根据当前知识库命中的片段，先给出一个便于学习理解的版本：\n\n" + snippets;
            };
        }
        return contextLead + "当前没有命中足够的知识库上下文。你可以补充更具体的 Java 后端场景、技术关键词，或切换到自由提问模式继续追问。";
    }

    private String buildChatPrompt(ChatSendRequest request, List<KnowledgeSearchVO.Reference> references) {
        String basePrompt = references.isEmpty()
                ? promptTemplateService.chatPrompt()
                : promptTemplateService.knowledgeChatPrompt() + "\n" + promptTemplateService.referenceConstraintPrompt();
        if (request.getContextSummary() != null && !request.getContextSummary().isBlank()) {
            basePrompt += "\n\n## Bound Context\n" + request.getContextSummary()
                    + "\nUse this context when it is relevant, and make the answer explicit about whether it is drawing from the user's own resume/project or from general资料。";
        }
        String answerMode = request.getAnswerMode() == null ? "learning" : request.getAnswerMode().toLowerCase();
        return switch (answerMode) {
            case "interview" -> basePrompt + "\nUse an interview-ready structure: conclusion first, then key points, trade-offs, and pitfalls.";
            case "concise" -> basePrompt + "\nKeep the answer terse, high-signal, and within 4 short bullets or a short paragraph.";
            case "project" -> basePrompt + "\nConnect the answer to realistic Java backend project scenarios, engineering trade-offs, and production usage.";
            default -> basePrompt + "\nOptimize for teaching clarity: explain concepts, mechanisms, and when to use them.";
        };
    }

    private List<String> buildSuggestedQuestions(ChatSendRequest request) {
        String topic = request.getMessage() == null ? "这个主题" : request.getMessage().trim();
        String normalized = topic.length() > 22 ? topic.substring(0, 22) + "..." : topic;
        String answerMode = request.getAnswerMode() == null ? "learning" : request.getAnswerMode().toLowerCase();
        boolean projectContext = "project".equalsIgnoreCase(request.getContextType()) || "project".equals(answerMode);
        boolean resumeContext = "resume".equalsIgnoreCase(request.getContextType());
        if (projectContext) {
            return List.of(
                    "把 `" + normalized + "` 换成我项目里的表达方式",
                    "围绕当前项目，继续追问 `" + normalized + "` 的取舍和风险",
                    "如果面试官追问这个项目的结果，你会怎么回答？");
        }
        if (resumeContext) {
            return List.of(
                    "把 `" + normalized + "` 放进我的简历表达里",
                    "如果面试官从简历里追问 `" + normalized + "`，我该怎么答？",
                    "帮我把这段回答压成 1 分钟简历面试版本");
        }
        return switch (answerMode) {
            case "interview" -> List.of(
                    "如果面试官继续追问 `" + normalized + "` 的底层原理，我该怎么答？",
                    "围绕 `" + normalized + "`，最容易答错的点有哪些？",
                    "把 `" + normalized + "` 压缩成 1 分钟面试回答");
            case "concise" -> List.of(
                    "把 `" + normalized + "` 再压缩成三句话",
                    "只保留 `" + normalized + "` 的核心结论",
                    "给我 `" + normalized + "` 的速记版要点");
            case "project" -> List.of(
                    "把 `" + normalized + "` 换成项目实战里的表达方式",
                    "如果在真实项目里用到 `" + normalized + "`，有哪些权衡？",
                    "给我一个 `" + normalized + "` 的后端排障场景");
            default -> List.of(
                    "继续展开 `" + normalized + "` 的底层机制",
                    "给我 `" + normalized + "` 的对比理解方式",
                    "围绕 `" + normalized + "` 出 3 个高频追问");
        };
    }
}
