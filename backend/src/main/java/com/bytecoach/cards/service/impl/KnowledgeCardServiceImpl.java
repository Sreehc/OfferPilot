package com.bytecoach.cards.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.ai.dto.AiChatRequest;
import com.bytecoach.ai.dto.AiChatResponse;
import com.bytecoach.ai.service.LlmGateway;
import com.bytecoach.ai.service.PromptTemplateService;
import com.bytecoach.cards.dto.CardRateRequest;
import com.bytecoach.cards.dto.CardTaskCreateRequest;
import com.bytecoach.cards.entity.KnowledgeCard;
import com.bytecoach.cards.entity.KnowledgeCardLog;
import com.bytecoach.cards.entity.KnowledgeCardTask;
import com.bytecoach.cards.mapper.KnowledgeCardLogMapper;
import com.bytecoach.cards.mapper.KnowledgeCardMapper;
import com.bytecoach.cards.mapper.KnowledgeCardTaskMapper;
import com.bytecoach.cards.service.KnowledgeCardService;
import com.bytecoach.cards.vo.KnowledgeCardItemVO;
import com.bytecoach.cards.vo.KnowledgeCardTaskVO;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.knowledge.entity.KnowledgeChunk;
import com.bytecoach.knowledge.entity.KnowledgeDoc;
import com.bytecoach.knowledge.mapper.KnowledgeChunkMapper;
import com.bytecoach.knowledge.mapper.KnowledgeDocMapper;
import com.bytecoach.wrong.service.impl.SpacedRepetitionServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeCardServiceImpl implements KnowledgeCardService {

    private static final BigDecimal MIN_EASE_FACTOR = new BigDecimal("1.30");
    private static final BigDecimal DEFAULT_EASE_FACTOR = new BigDecimal("2.50");

    private final KnowledgeCardTaskMapper taskMapper;
    private final KnowledgeCardMapper cardMapper;
    private final KnowledgeCardLogMapper logMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final PromptTemplateService promptTemplateService;
    private final LlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public KnowledgeCardTaskVO createTask(Long userId, CardTaskCreateRequest request) {
        KnowledgeDoc doc = getAllowedDoc(userId, request.getDocId());
        List<KnowledgeChunk> chunks = loadChunks(doc.getId());
        if (chunks.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档还没有可用内容，暂时不能生成知识卡片");
        }

        KnowledgeCardTask existing = findLatestTask(userId, doc.getId());
        if (existing != null && !"invalid".equals(existing.getStatus())) {
            return buildTaskVO(existing, loadCards(existing.getId()));
        }

        List<GeneratedCard> generatedCards = generateCards(doc, chunks);
        if (generatedCards.isEmpty()) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "知识卡片生成失败");
        }

        int totalCards = generatedCards.size();
        int days = request.getDays();
        int dailyTarget = Math.max(1, (int) Math.ceil((double) totalCards / days));

        KnowledgeCardTask task = new KnowledgeCardTask();
        task.setUserId(userId);
        task.setDocId(doc.getId());
        task.setDocTitle(doc.getTitle());
        task.setStatus("draft");
        task.setDays(days);
        task.setCurrentDay(1);
        task.setDailyTarget(dailyTarget);
        task.setTotalCards(totalCards);
        task.setMasteredCards(0);
        task.setReviewCount(0);
        taskMapper.insert(task);

        for (int i = 0; i < generatedCards.size(); i++) {
            GeneratedCard generated = generatedCards.get(i);
            KnowledgeCard card = new KnowledgeCard();
            card.setTaskId(task.getId());
            card.setQuestion(generated.question());
            card.setAnswer(generated.answer());
            card.setSortOrder(i + 1);
            card.setScheduledDay(Math.min(days, i / dailyTarget + 1));
            card.setState("new");
            card.setReviewCount(0);
            card.setEaseFactor(DEFAULT_EASE_FACTOR);
            card.setIntervalDays(0);
            card.setStreak(0);
            cardMapper.insert(card);
        }

        return getTask(userId, task.getId());
    }

    @Override
    public KnowledgeCardTaskVO getActiveTask(Long userId) {
        KnowledgeCardTask task = taskMapper.selectOne(new LambdaQueryWrapper<KnowledgeCardTask>()
                .eq(KnowledgeCardTask::getUserId, userId)
                .in(KnowledgeCardTask::getStatus, List.of("active", "draft"))
                .orderByDesc(KnowledgeCardTask::getUpdateTime)
                .last("LIMIT 1"));
        return task == null ? null : buildTaskVO(task, loadCards(task.getId()));
    }

    @Override
    public KnowledgeCardTaskVO getTask(Long userId, Long taskId) {
        KnowledgeCardTask task = getOwnedTask(userId, taskId);
        return buildTaskVO(task, loadCards(task.getId()));
    }

    @Override
    @Transactional
    public KnowledgeCardTaskVO startTask(Long userId, Long taskId) {
        KnowledgeCardTask task = getOwnedTask(userId, taskId);
        if ("invalid".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "来源文档已删除，当前任务不可继续");
        }
        if (!"completed".equals(task.getStatus())) {
            task.setStatus("active");
        }
        if (task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }
        taskMapper.updateById(task);
        return buildTaskVO(task, loadCards(task.getId()));
    }

    @Override
    @Transactional
    public KnowledgeCardTaskVO rate(Long userId, Long taskId, CardRateRequest request) {
        KnowledgeCardTask task = getOwnedTask(userId, taskId);
        if (!"active".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前任务尚未开始");
        }
        if ("invalid".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "来源文档已删除，当前任务不可继续");
        }

        KnowledgeCard card = cardMapper.selectById(request.getCardId());
        if (card == null || !task.getId().equals(card.getTaskId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "knowledge card not found");
        }

        BigDecimal efBefore = card.getEaseFactor() != null ? card.getEaseFactor() : DEFAULT_EASE_FACTOR;
        int intervalBefore = card.getIntervalDays() != null ? card.getIntervalDays() : 0;
        int rating = request.getRating();

        BigDecimal efAfter;
        int intervalAfter;
        int newStreak;
        String nextState;

        if (rating < 3) {
            intervalAfter = 1;
            newStreak = 0;
            BigDecimal penalty = rating == 1 ? new BigDecimal("0.20") : new BigDecimal("0.15");
            efAfter = efBefore.subtract(penalty).max(MIN_EASE_FACTOR);
            nextState = rating == 1 ? "weak" : "learning";
        } else {
            newStreak = (card.getStreak() != null ? card.getStreak() : 0) + 1;
            if (newStreak == 1) {
                intervalAfter = 1;
            } else if (newStreak == 2) {
                intervalAfter = 3;
            } else {
                int baseInterval = Math.max(1, intervalBefore);
                intervalAfter = BigDecimal.valueOf(baseInterval)
                        .multiply(efBefore)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
            }
            int quality = rating == 4 ? 5 : 3;
            BigDecimal efDelta = new BigDecimal("0.1")
                    .subtract(BigDecimal.valueOf(5 - quality)
                            .multiply(new BigDecimal("0.08")
                                    .add(BigDecimal.valueOf(5 - quality).multiply(new BigDecimal("0.02")))));
            efAfter = efBefore.add(efDelta).max(MIN_EASE_FACTOR);
            nextState = "mastered".equals(SpacedRepetitionServiceImpl.computeMasteryLevel(efAfter, newStreak))
                    ? "mastered"
                    : "learning";
        }

        card.setEaseFactor(efAfter);
        card.setIntervalDays(intervalAfter);
        card.setStreak(newStreak);
        card.setState(nextState);
        card.setReviewCount((card.getReviewCount() != null ? card.getReviewCount() : 0) + 1);
        card.setLastRating(rating);
        card.setLastReviewTime(LocalDateTime.now());
        card.setNextReviewAt(LocalDateTime.now().plusDays(intervalAfter));
        cardMapper.updateById(card);

        KnowledgeCardLog logItem = new KnowledgeCardLog();
        logItem.setUserId(userId);
        logItem.setTaskId(taskId);
        logItem.setCardId(card.getId());
        logItem.setRating(rating);
        logItem.setResponseTimeMs(request.getResponseTimeMs());
        logItem.setEaseFactorBefore(efBefore);
        logItem.setIntervalBefore(intervalBefore);
        logItem.setEaseFactorAfter(efAfter);
        logItem.setIntervalAfter(intervalAfter);
        logMapper.insert(logItem);

        List<KnowledgeCard> cards = loadCards(taskId);
        task.setMasteredCards((int) cards.stream().filter(item -> "mastered".equals(item.getState())).count());
        task.setReviewCount((task.getReviewCount() != null ? task.getReviewCount() : 0) + 1);
        task.setCurrentDay(computeCurrentDay(task, cards));
        if (task.getMasteredCards() >= task.getTotalCards()) {
            task.setStatus("completed");
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
        }
        taskMapper.updateById(task);

        return buildTaskVO(task, cards);
    }

    @Override
    @Transactional
    public KnowledgeCardTaskVO restart(Long userId, Long taskId) {
        KnowledgeCardTask task = getOwnedTask(userId, taskId);
        if ("invalid".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "来源文档已删除，当前任务不可继续");
        }

        List<KnowledgeCard> cards = loadCards(taskId);
        for (KnowledgeCard card : cards) {
            card.setState("new");
            card.setReviewCount(0);
            card.setLastRating(null);
            card.setLastReviewTime(null);
            card.setEaseFactor(DEFAULT_EASE_FACTOR);
            card.setIntervalDays(0);
            card.setStreak(0);
            card.setNextReviewAt(null);
            cardMapper.updateById(card);
        }

        task.setStatus("draft");
        task.setCurrentDay(1);
        task.setMasteredCards(0);
        task.setReviewCount(0);
        task.setStartedAt(null);
        task.setCompletedAt(null);
        taskMapper.updateById(task);

        return buildTaskVO(task, loadCards(taskId));
    }

    @Override
    @Transactional
    public void invalidateByDocId(Long docId, String reason) {
        List<KnowledgeCardTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<KnowledgeCardTask>()
                .eq(KnowledgeCardTask::getDocId, docId)
                .in(KnowledgeCardTask::getStatus, List.of("draft", "active", "completed")));
        for (KnowledgeCardTask task : tasks) {
            task.setStatus("invalid");
            task.setInvalidReason(StringUtils.hasText(reason) ? reason : "来源文档已删除");
            taskMapper.updateById(task);
        }
    }

    private KnowledgeDoc getAllowedDoc(Long userId, Long docId) {
        KnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
        if (doc == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "knowledge doc not found");
        }
        boolean isOwner = doc.getUserId() != null && doc.getUserId().equals(userId);
        boolean isSystemDoc = doc.getUserId() == null;
        if (!isOwner && !isSystemDoc) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能为自己的资料或系统资料生成卡片");
        }
        if (!List.of("parsed", "indexed").contains(doc.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档尚未处理完成，暂时不能生成知识卡片");
        }
        return doc;
    }

    private List<KnowledgeChunk> loadChunks(Long docId) {
        return knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocId, docId)
                .orderByAsc(KnowledgeChunk::getChunkIndex));
    }

    private KnowledgeCardTask getOwnedTask(Long userId, Long taskId) {
        KnowledgeCardTask task = taskMapper.selectById(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "knowledge card task not found");
        }
        return task;
    }

    private KnowledgeCardTask findLatestTask(Long userId, Long docId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<KnowledgeCardTask>()
                .eq(KnowledgeCardTask::getUserId, userId)
                .eq(KnowledgeCardTask::getDocId, docId)
                .orderByDesc(KnowledgeCardTask::getUpdateTime)
                .last("LIMIT 1"));
    }

    private List<KnowledgeCard> loadCards(Long taskId) {
        return cardMapper.selectList(new LambdaQueryWrapper<KnowledgeCard>()
                .eq(KnowledgeCard::getTaskId, taskId)
                .orderByAsc(KnowledgeCard::getSortOrder));
    }

    private KnowledgeCardTaskVO buildTaskVO(KnowledgeCardTask task, List<KnowledgeCard> cards) {
        List<KnowledgeCardItemVO> items = cards.stream()
                .map(card -> KnowledgeCardItemVO.builder()
                        .id(card.getId())
                        .question(card.getQuestion())
                        .answer(card.getAnswer())
                        .sortOrder(card.getSortOrder())
                        .scheduledDay(card.getScheduledDay())
                        .state(card.getState())
                        .reviewCount(card.getReviewCount())
                        .lastRating(card.getLastRating())
                        .easeFactor(card.getEaseFactor())
                        .intervalDays(card.getIntervalDays())
                        .streak(card.getStreak())
                        .build())
                .toList();

        int reviewedTodayCount = (int) cards.stream()
                .filter(card -> card.getLastReviewTime() != null
                        && card.getLastReviewTime().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count();
        List<KnowledgeCard> dueCards = selectDueCards(task, cards);
        KnowledgeCard currentCard = dueCards.stream().findFirst().orElse(null);

        return KnowledgeCardTaskVO.builder()
                .id(task.getId())
                .docId(task.getDocId())
                .docTitle(task.getDocTitle())
                .status(task.getStatus())
                .days(task.getDays())
                .currentDay(task.getCurrentDay())
                .dailyTarget(task.getDailyTarget())
                .totalCards(task.getTotalCards())
                .masteredCards(task.getMasteredCards())
                .reviewCount(task.getReviewCount())
                .invalidReason(task.getInvalidReason())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .dueCount(dueCards.size())
                .reviewedTodayCount(reviewedTodayCount)
                .currentCard(currentCard == null ? null : items.stream()
                        .filter(item -> item.getId().equals(currentCard.getId()))
                        .findFirst()
                        .orElse(null))
                .cards(items)
                .build();
    }

    private int computeCurrentDay(KnowledgeCardTask task, List<KnowledgeCard> cards) {
        for (int day = 1; day <= task.getDays(); day++) {
            final int targetDay = day;
            boolean unfinished = cards.stream()
                    .anyMatch(card -> card.getScheduledDay() <= targetDay && !"mastered".equals(card.getState()));
            if (unfinished) {
                return day;
            }
        }
        return task.getDays();
    }

    private List<KnowledgeCard> selectDueCards(KnowledgeCardTask task, List<KnowledgeCard> cards) {
        int currentDay = task.getCurrentDay() != null ? task.getCurrentDay() : 1;
        List<KnowledgeCard> due = cards.stream()
                .filter(card -> !"mastered".equals(card.getState()) && card.getScheduledDay() <= currentDay)
                .sorted(Comparator
                        .comparing((KnowledgeCard card) -> statePriority(card.getState()))
                        .thenComparing(KnowledgeCard::getSortOrder))
                .toList();
        if (!due.isEmpty()) {
            return due;
        }
        return cards.stream()
                .filter(card -> !"mastered".equals(card.getState()))
                .sorted(Comparator.comparing(KnowledgeCard::getSortOrder))
                .toList();
    }

    private int statePriority(String state) {
        return switch (state == null ? "new" : state) {
            case "weak" -> 0;
            case "learning" -> 1;
            case "new" -> 2;
            default -> 3;
        };
    }

    private List<GeneratedCard> generateCards(KnowledgeDoc doc, List<KnowledgeChunk> chunks) {
        List<GeneratedCard> cards = generateByLlm(doc, chunks);
        if (!cards.isEmpty()) {
            return cards;
        }
        return generateFallbackCards(doc, chunks);
    }

    private List<GeneratedCard> generateByLlm(KnowledgeDoc doc, List<KnowledgeChunk> chunks) {
        try {
            String excerpt = chunks.stream()
                    .limit(8)
                    .map(KnowledgeChunk::getContent)
                    .reduce((left, right) -> left + "\n\n---\n\n" + right)
                    .orElse("");
            AiChatResponse response = llmGateway.chatCompletion(AiChatRequest.builder()
                    .systemPrompt(promptTemplateService.knowledgeCardPrompt())
                    .userPrompt("文档标题：" + doc.getTitle() + "\n\n文档内容：\n" + excerpt)
                    .references(List.of())
                    .build());
            String content = normalizeJson(response.getContent());
            List<GeneratedCardPayload> payloads =
                    objectMapper.readValue(content, new TypeReference<List<GeneratedCardPayload>>() {});
            List<GeneratedCard> result = new ArrayList<>();
            Set<String> dedupe = new LinkedHashSet<>();
            for (GeneratedCardPayload payload : payloads) {
                if (payload == null || !StringUtils.hasText(payload.getQuestion()) || !StringUtils.hasText(payload.getAnswer())) {
                    continue;
                }
                String key = payload.getQuestion().trim().toLowerCase(Locale.ROOT);
                if (dedupe.add(key)) {
                    result.add(new GeneratedCard(cleanText(payload.getQuestion()), cleanText(payload.getAnswer())));
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Knowledge card LLM generation failed for doc {}: {}", doc.getId(), e.getMessage());
            return List.of();
        }
    }

    private List<GeneratedCard> generateFallbackCards(KnowledgeDoc doc, List<KnowledgeChunk> chunks) {
        List<GeneratedCard> result = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();
        for (KnowledgeChunk chunk : chunks) {
            List<String> lines = chunk.getContent().lines()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .filter(line -> line.length() > 12)
                    .limit(4)
                    .toList();
            if (lines.isEmpty()) {
                continue;
            }
            String first = lines.get(0);
            String second = lines.size() > 1 ? lines.get(1) : first;
            String question = buildFallbackQuestion(doc.getTitle(), first);
            String answer = buildFallbackAnswer(first, second, lines);
            String key = question.toLowerCase(Locale.ROOT);
            if (dedupe.add(key)) {
                result.add(new GeneratedCard(question, answer));
            }
            if (result.size() >= 12) {
                break;
            }
        }
        return result;
    }

    private String buildFallbackQuestion(String title, String seed) {
        String normalized = seed
                .replaceFirst("^#+\\s*", "")
                .replaceFirst("^[\\-\\*\\d\\.\\)\\s]+", "")
                .trim();
        if (normalized.length() > 40) {
            normalized = normalized.substring(0, 40) + "...";
        }
        return "结合《" + title + "》，说明：" + normalized;
    }

    private String buildFallbackAnswer(String first, String second, List<String> lines) {
        String merged = first.equals(second) ? first : first + "\n" + second;
        if (merged.length() < 28 && lines.size() > 2) {
            merged = merged + "\n" + lines.get(2);
        }
        return cleanText(merged);
    }

    private String cleanText(String text) {
        return text == null ? "" : text.trim().replaceAll("\\n{3,}", "\n\n");
    }

    private String normalizeJson(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private record GeneratedCard(String question, String answer) {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GeneratedCardPayload {
        private String question;
        private String answer;
    }
}
