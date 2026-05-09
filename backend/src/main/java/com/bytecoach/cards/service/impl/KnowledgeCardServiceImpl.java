package com.bytecoach.cards.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.ai.dto.AiChatRequest;
import com.bytecoach.ai.dto.AiChatResponse;
import com.bytecoach.ai.service.LlmGateway;
import com.bytecoach.ai.service.PromptTemplateService;
import com.bytecoach.cards.dto.CardGenerateRequest;
import com.bytecoach.cards.dto.CardRateRequest;
import com.bytecoach.cards.dto.CardTaskCreateRequest;
import com.bytecoach.cards.entity.DailyCardTask;
import com.bytecoach.cards.entity.KnowledgeCard;
import com.bytecoach.cards.entity.KnowledgeCardLog;
import com.bytecoach.cards.entity.KnowledgeCardTask;
import com.bytecoach.cards.mapper.DailyCardTaskMapper;
import com.bytecoach.cards.mapper.KnowledgeCardLogMapper;
import com.bytecoach.cards.mapper.KnowledgeCardMapper;
import com.bytecoach.cards.mapper.KnowledgeCardTaskMapper;
import com.bytecoach.cards.service.KnowledgeCardService;
import com.bytecoach.cards.vo.CardDeckSummaryVO;
import com.bytecoach.cards.vo.CardStatsSummaryVO;
import com.bytecoach.cards.vo.KnowledgeCardItemVO;
import com.bytecoach.cards.vo.KnowledgeCardTaskVO;
import com.bytecoach.cards.vo.TodayCardsTaskVO;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.knowledge.entity.KnowledgeChunk;
import com.bytecoach.knowledge.entity.KnowledgeDoc;
import com.bytecoach.knowledge.mapper.KnowledgeChunkMapper;
import com.bytecoach.knowledge.mapper.KnowledgeDocMapper;
import com.bytecoach.question.entity.Question;
import com.bytecoach.question.mapper.QuestionMapper;
import com.bytecoach.wrong.entity.WrongQuestion;
import com.bytecoach.wrong.mapper.WrongQuestionMapper;
import com.bytecoach.wrong.service.impl.SpacedRepetitionServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private static final String SOURCE_KNOWLEDGE_DOC = "knowledge_doc";
    private static final String SOURCE_WRONG_AUTO = "wrong_auto";
    private static final String SOURCE_REF_KNOWLEDGE_CHUNK = "knowledge_chunk";
    private static final String SOURCE_REF_WRONG_QUESTION = "wrong_question";
    private static final String WRONG_DECK_TITLE = "错题复习";
    private static final long WRONG_DECK_DOC_ID = -1L;

    private static final class StudyQueue {
        private final int currentDay;
        private final List<KnowledgeCard> cards;
        private final int todayLearnCount;
        private final int todayReviewCount;
        private final int tomorrowDueCount;

        private StudyQueue(int currentDay, List<KnowledgeCard> cards, int todayLearnCount, int todayReviewCount, int tomorrowDueCount) {
            this.currentDay = currentDay;
            this.cards = cards;
            this.todayLearnCount = todayLearnCount;
            this.todayReviewCount = todayReviewCount;
            this.tomorrowDueCount = tomorrowDueCount;
        }
    }

    private final KnowledgeCardTaskMapper taskMapper;
    private final KnowledgeCardMapper cardMapper;
    private final KnowledgeCardLogMapper logMapper;
    private final DailyCardTaskMapper dailyCardTaskMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;
    private final PromptTemplateService promptTemplateService;
    private final LlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public KnowledgeCardTaskVO createTask(Long userId, CardTaskCreateRequest request) {
        return generateKnowledgeDeck(userId, request.getDocId(), request.getDays(), false);
    }

    @Override
    @Transactional
    public KnowledgeCardTaskVO generateDeck(Long userId, CardGenerateRequest request) {
        return generateKnowledgeDeck(userId, request.getDocId(), request.getDays(), true);
    }

    @Override
    public KnowledgeCardTaskVO getActiveTask(Long userId) {
        KnowledgeCardTask task = findCurrentDeck(userId);
        if (task == null) {
            task = findLatestActiveOrDraftDeck(userId);
        }
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
        activateCurrentDeck(userId, task.getId());
        if ("invalid".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "来源文档已删除，当前任务不可继续");
        }
        if (!"completed".equals(task.getStatus())) {
            task.setStatus("active");
        }
        if (task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }
        task.setLastStudiedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return buildTaskVO(task, loadCards(task.getId()));
    }

    @Override
    @Transactional
    public KnowledgeCardTaskVO rate(Long userId, Long taskId, CardRateRequest request) {
        KnowledgeCardTask task = getOwnedTask(userId, taskId);
        rateCardInternal(userId, task, request);
        return buildTaskVO(task, loadCards(taskId));
    }

    @Override
    @Transactional
    public KnowledgeCardTaskVO restart(Long userId, Long taskId) {
        KnowledgeCardTask task = getOwnedTask(userId, taskId);
        if ("invalid".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "来源文档已删除，当前任务不可继续");
        }
        if (SOURCE_WRONG_AUTO.equals(task.getSourceType())) {
            syncWrongDeck(userId);
            return buildTaskVO(getOwnedTask(userId, taskId), loadCards(taskId));
        }

        List<KnowledgeCard> cards = loadCards(taskId);
        for (KnowledgeCard card : cards) {
            resetKnowledgeCard(card);
            cardMapper.updateById(card);
        }

        task.setStatus("draft");
        task.setCurrentDay(1);
        task.setMasteredCards(0);
        task.setReviewCount(0);
        task.setEstimatedMinutes(estimateMinutes(cards.size()));
        task.setStartedAt(null);
        task.setCompletedAt(null);
        task.setLastStudiedAt(null);
        taskMapper.updateById(task);

        deleteTodaySnapshot(userId, taskId);
        return buildTaskVO(task, loadCards(taskId));
    }

    @Override
    public TodayCardsTaskVO getTodayTask(Long userId) {
        KnowledgeCardTask task = findCurrentDeck(userId);
        if (task == null) {
            return null;
        }
        return buildTodayTaskVO(task, loadCards(task.getId()));
    }

    @Override
    @Transactional
    public TodayCardsTaskVO reviewDeck(Long userId, Long deckId, CardRateRequest request) {
        KnowledgeCardTask task = getOwnedTask(userId, deckId);
        rateCardInternal(userId, task, request);
        return buildTodayTaskVO(task, loadCards(deckId));
    }

    @Override
    public List<CardDeckSummaryVO> listDecks(Long userId) {
        return taskMapper.selectList(new LambdaQueryWrapper<KnowledgeCardTask>()
                        .eq(KnowledgeCardTask::getUserId, userId)
                        .ne(KnowledgeCardTask::getStatus, "invalid")
                        .orderByDesc(KnowledgeCardTask::getIsCurrent)
                        .orderByDesc(KnowledgeCardTask::getUpdateTime))
                .stream()
                .map(task -> buildDeckSummary(task, loadCards(task.getId())))
                .toList();
    }

    @Override
    @Transactional
    public TodayCardsTaskVO activateDeck(Long userId, Long deckId) {
        KnowledgeCardTask task = getOwnedTask(userId, deckId);
        activateCurrentDeck(userId, deckId);
        if ("draft".equals(task.getStatus())) {
            task.setStatus("active");
            if (task.getStartedAt() == null) {
                task.setStartedAt(LocalDateTime.now());
            }
            taskMapper.updateById(task);
        }
        return buildTodayTaskVO(task, loadCards(deckId));
    }

    @Override
    public CardStatsSummaryVO getStats(Long userId) {
        List<KnowledgeCardTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<KnowledgeCardTask>()
                .eq(KnowledgeCardTask::getUserId, userId)
                .ne(KnowledgeCardTask::getStatus, "invalid"));
        KnowledgeCardTask current = tasks.stream().filter(task -> Integer.valueOf(1).equals(task.getIsCurrent())).findFirst().orElse(null);

        int deckCount = tasks.size();
        int totalCards = tasks.stream().map(KnowledgeCardTask::getTotalCards).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int masteredCards = tasks.stream().map(KnowledgeCardTask::getMasteredCards).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int dueTodayCount = tasks.stream()
                .mapToInt(task -> selectStudyQueue(task, loadCards(task.getId())).cards.size())
                .sum();
        TodayCardsTaskVO todayTask = current == null ? null : buildTodayTaskVO(current, loadCards(current.getId()));
        return CardStatsSummaryVO.builder()
                .currentDeckId(current == null ? null : String.valueOf(current.getId()))
                .deckCount(deckCount)
                .totalCards(totalCards)
                .masteredCards(masteredCards)
                .dueTodayCount(dueTodayCount)
                .streak(todayTask == null ? 0 : todayTask.getStreak())
                .completionRate(todayTask == null ? 0 : todayTask.getCompletionRate())
                .build();
    }

    @Override
    @Transactional
    public void syncWrongDeck(Long userId) {
        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .orderByAsc(WrongQuestion::getNextReviewDate)
                .orderByDesc(WrongQuestion::getUpdateTime));
        KnowledgeCardTask deck = findWrongDeck(userId);
        if (wrongs.isEmpty()) {
            if (deck != null) {
                deck.setStatus("completed");
                deck.setTotalCards(0);
                deck.setMasteredCards(0);
                deck.setDailyTarget(0);
                deck.setEstimatedMinutes(0);
                taskMapper.updateById(deck);
                cardMapper.delete(new LambdaQueryWrapper<KnowledgeCard>().eq(KnowledgeCard::getTaskId, deck.getId()));
                deleteTodaySnapshot(userId, deck.getId());
            }
            return;
        }

        if (deck == null) {
            deck = new KnowledgeCardTask();
            deck.setUserId(userId);
            deck.setDocId(WRONG_DECK_DOC_ID);
            deck.setDocTitle(WRONG_DECK_TITLE);
            deck.setDeckTitle(WRONG_DECK_TITLE);
            deck.setSourceType(SOURCE_WRONG_AUTO);
            deck.setStatus("active");
            deck.setIsCurrent(0);
            deck.setDays(1);
            deck.setCurrentDay(1);
            deck.setDailyTarget(0);
            deck.setTotalCards(0);
            deck.setMasteredCards(0);
            deck.setReviewCount(0);
            deck.setEstimatedMinutes(0);
            taskMapper.insert(deck);
        }

        Map<Long, Question> questionMap = questionMapper.selectBatchIds(
                        wrongs.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        List<KnowledgeCard> existingCards = loadCards(deck.getId());
        Map<Long, KnowledgeCard> existingByWrongId = existingCards.stream()
                .filter(card -> SOURCE_REF_WRONG_QUESTION.equals(card.getSourceRefType()) && card.getSourceRefId() != null)
                .collect(Collectors.toMap(KnowledgeCard::getSourceRefId, Function.identity(), (a, b) -> a));

        Set<Long> activeWrongIds = wrongs.stream().map(WrongQuestion::getId).collect(Collectors.toSet());
        for (KnowledgeCard existingCard : existingCards) {
            if (SOURCE_REF_WRONG_QUESTION.equals(existingCard.getSourceRefType())
                    && existingCard.getSourceRefId() != null
                    && !activeWrongIds.contains(existingCard.getSourceRefId())) {
                cardMapper.deleteById(existingCard.getId());
            }
        }

        int sortOrder = 1;
        for (WrongQuestion wrong : wrongs) {
            Question question = questionMap.get(wrong.getQuestionId());
            KnowledgeCard card = existingByWrongId.get(wrong.getId());
            if (card == null) {
                card = new KnowledgeCard();
                card.setTaskId(deck.getId());
                card.setReviewCount(wrong.getReviewCount() == null ? 0 : wrong.getReviewCount());
                card.setEaseFactor(wrong.getEaseFactor() == null ? DEFAULT_EASE_FACTOR : wrong.getEaseFactor());
                card.setIntervalDays(wrong.getIntervalDays() == null ? 0 : wrong.getIntervalDays());
                card.setStreak(wrong.getStreak() == null ? 0 : wrong.getStreak());
                card.setLastReviewTime(wrong.getLastReviewTime());
                card.setNextReviewAt(wrong.getNextReviewDate() == null ? null : wrong.getNextReviewDate().atStartOfDay());
                card.setLastRating(null);
            }
            card.setQuestion(question != null ? question.getTitle() : "Unknown");
            card.setAnswer(StringUtils.hasText(wrong.getStandardAnswer())
                    ? wrong.getStandardAnswer()
                    : (question != null ? question.getStandardAnswer() : "暂无标准答案"));
            card.setExplanation(wrong.getErrorReason());
            card.setSortOrder(sortOrder++);
            card.setScheduledDay(1);
            card.setSourceRefId(wrong.getId());
            card.setSourceRefType(SOURCE_REF_WRONG_QUESTION);
            card.setState(computeCardStateFromWrong(wrong));
            if (card.getId() == null) {
                cardMapper.insert(card);
            } else {
                cardMapper.updateById(card);
            }
        }

        List<KnowledgeCard> refreshed = loadCards(deck.getId());
        deck.setDeckTitle(WRONG_DECK_TITLE);
        deck.setDocTitle(WRONG_DECK_TITLE);
        deck.setSourceType(SOURCE_WRONG_AUTO);
        deck.setStatus(refreshed.isEmpty() ? "completed" : "active");
        deck.setCurrentDay(1);
        deck.setDays(1);
        deck.setTotalCards(refreshed.size());
        deck.setMasteredCards((int) refreshed.stream().filter(item -> "mastered".equals(item.getState())).count());
        deck.setDailyTarget(refreshed.size());
        deck.setEstimatedMinutes(estimateMinutes(Math.max(1, refreshed.size())));
        taskMapper.updateById(deck);

        updateTodaySnapshot(userId, deck, refreshed);
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

    private KnowledgeCardTaskVO generateKnowledgeDeck(Long userId, Long docId, Integer days, boolean setCurrent) {
        KnowledgeDoc doc = getAllowedDoc(userId, docId);
        List<KnowledgeChunk> chunks = loadChunks(doc.getId());
        if (chunks.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档还没有可用内容，暂时不能生成知识卡片");
        }

        KnowledgeCardTask existing = findLatestTask(userId, doc.getId());
        if (existing != null && !"invalid".equals(existing.getStatus())) {
            if (setCurrent) {
                activateCurrentDeck(userId, existing.getId());
            }
            return buildTaskVO(existing, loadCards(existing.getId()));
        }

        List<GeneratedCard> generatedCards = generateCards(doc, chunks);
        if (generatedCards.isEmpty()) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "知识卡片生成失败");
        }

        int totalCards = generatedCards.size();
        int safeDays = days == null ? 7 : days;
        int dailyTarget = Math.max(1, (int) Math.ceil((double) totalCards / safeDays));

        if (setCurrent) {
            clearCurrentDeck(userId);
        }

        KnowledgeCardTask task = new KnowledgeCardTask();
        task.setUserId(userId);
        task.setDocId(doc.getId());
        task.setDocTitle(doc.getTitle());
        task.setDeckTitle(doc.getTitle());
        task.setSourceType(SOURCE_KNOWLEDGE_DOC);
        task.setStatus("draft");
        task.setIsCurrent(setCurrent ? 1 : 0);
        task.setDays(safeDays);
        task.setCurrentDay(1);
        task.setDailyTarget(dailyTarget);
        task.setTotalCards(totalCards);
        task.setMasteredCards(0);
        task.setReviewCount(0);
        task.setEstimatedMinutes(estimateMinutes(dailyTarget));
        taskMapper.insert(task);

        for (int i = 0; i < generatedCards.size(); i++) {
            GeneratedCard generated = generatedCards.get(i);
            KnowledgeCard card = new KnowledgeCard();
            card.setTaskId(task.getId());
            card.setQuestion(generated.question());
            card.setAnswer(generated.answer());
            card.setExplanation(generated.explanation());
            card.setSortOrder(i + 1);
            card.setScheduledDay(Math.min(safeDays, i / dailyTarget + 1));
            card.setState("new");
            card.setReviewCount(0);
            card.setSourceRefType(SOURCE_REF_KNOWLEDGE_CHUNK);
            card.setEaseFactor(DEFAULT_EASE_FACTOR);
            card.setIntervalDays(0);
            card.setStreak(0);
            cardMapper.insert(card);
        }

        return getTask(userId, task.getId());
    }

    private void rateCardInternal(Long userId, KnowledgeCardTask task, CardRateRequest request) {
        activateCurrentDeck(userId, task.getId());
        if (!"active".equals(task.getStatus()) && !"completed".equals(task.getStatus())) {
            task.setStatus("active");
        }
        if ("invalid".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "来源文档已删除，当前任务不可继续");
        }
        if (task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
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
        logItem.setTaskId(task.getId());
        logItem.setCardId(card.getId());
        logItem.setRating(rating);
        logItem.setResponseTimeMs(request.getResponseTimeMs());
        logItem.setEaseFactorBefore(efBefore);
        logItem.setIntervalBefore(intervalBefore);
        logItem.setEaseFactorAfter(efAfter);
        logItem.setIntervalAfter(intervalAfter);
        logMapper.insert(logItem);

        List<KnowledgeCard> cards = loadCards(task.getId());
        task.setMasteredCards((int) cards.stream().filter(item -> "mastered".equals(item.getState())).count());
        task.setReviewCount((task.getReviewCount() != null ? task.getReviewCount() : 0) + 1);
        task.setCurrentDay(resolveCurrentDay(task, cards));
        task.setLastStudiedAt(LocalDateTime.now());
        task.setEstimatedMinutes(estimateMinutes(selectStudyQueue(task, cards).cards.size()));
        if (task.getMasteredCards() >= task.getTotalCards()) {
            task.setStatus("completed");
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
        } else if ("draft".equals(task.getStatus())) {
            task.setStatus("active");
        }
        taskMapper.updateById(task);
        updateTodaySnapshot(userId, task, cards);
    }

    private TodayCardsTaskVO buildTodayTaskVO(KnowledgeCardTask task, List<KnowledgeCard> cards) {
        StudyQueue queue = selectStudyQueue(task, cards);
        int todayCompletedCount = countReviewedToday(cards);
        DailyCardTask snapshot = updateTodaySnapshot(task.getUserId(), task, cards);
        int plannedCount = snapshot.getPlannedCount() == null ? 0 : snapshot.getPlannedCount();
        int completionRate = plannedCount <= 0 ? (task.getTotalCards() != null && task.getTotalCards() > 0 ? 100 : 0)
                : Math.min(100, (int) Math.round((todayCompletedCount * 100.0) / plannedCount));
        KnowledgeCard currentCard = queue.cards.stream().findFirst().orElse(null);

        return TodayCardsTaskVO.builder()
                .deckId(String.valueOf(task.getId()))
                .deckTitle(resolveDeckTitle(task))
                .sourceType(task.getSourceType())
                .status(task.getStatus())
                .todayLearnCount(snapshot.getLearnCount())
                .todayReviewCount(snapshot.getReviewCount())
                .todayCompletedCount(todayCompletedCount)
                .completionRate(completionRate)
                .estimatedMinutes(snapshot.getEstimatedMinutes())
                .streak(snapshot.getStreakSnapshot())
                .tomorrowDueCount(snapshot.getTomorrowDueCount())
                .totalCards(task.getTotalCards())
                .masteredCards(task.getMasteredCards())
                .dueCount(queue.cards.size())
                .reviewedTodayCount(todayCompletedCount)
                .currentCard(currentCard == null ? null : toItemVO(currentCard))
                .build();
    }

    private CardDeckSummaryVO buildDeckSummary(KnowledgeCardTask task, List<KnowledgeCard> cards) {
        StudyQueue queue = selectStudyQueue(task, cards);
        return CardDeckSummaryVO.builder()
                .deckId(String.valueOf(task.getId()))
                .deckTitle(resolveDeckTitle(task))
                .sourceType(task.getSourceType())
                .status(task.getStatus())
                .totalCards(task.getTotalCards())
                .masteredCards(task.getMasteredCards())
                .dueCount(queue.cards.size())
                .reviewedTodayCount(countReviewedToday(cards))
                .isCurrent(task.getIsCurrent() == null ? 0 : task.getIsCurrent())
                .lastStudiedAt(task.getLastStudiedAt())
                .build();
    }

    private KnowledgeCardTaskVO buildTaskVO(KnowledgeCardTask task, List<KnowledgeCard> cards) {
        List<KnowledgeCardItemVO> items = cards.stream().map(this::toItemVO).toList();
        int reviewedTodayCount = countReviewedToday(cards);
        StudyQueue studyQueue = selectStudyQueue(task, cards);
        KnowledgeCard currentCard = studyQueue.cards.stream().findFirst().orElse(null);

        return KnowledgeCardTaskVO.builder()
                .id(String.valueOf(task.getId()))
                .docId(String.valueOf(task.getDocId()))
                .docTitle(task.getDocTitle())
                .deckTitle(resolveDeckTitle(task))
                .sourceType(task.getSourceType())
                .status(task.getStatus())
                .isCurrent(task.getIsCurrent())
                .days(task.getDays())
                .currentDay(studyQueue.currentDay)
                .dailyTarget(task.getDailyTarget())
                .totalCards(task.getTotalCards())
                .masteredCards(task.getMasteredCards())
                .reviewCount(task.getReviewCount())
                .estimatedMinutes(task.getEstimatedMinutes())
                .invalidReason(task.getInvalidReason())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .lastStudiedAt(task.getLastStudiedAt())
                .dueCount(studyQueue.cards.size())
                .reviewedTodayCount(reviewedTodayCount)
                .currentCard(currentCard == null ? null : toItemVO(currentCard))
                .cards(items)
                .build();
    }

    private KnowledgeCardItemVO toItemVO(KnowledgeCard card) {
        return KnowledgeCardItemVO.builder()
                .id(String.valueOf(card.getId()))
                .question(card.getQuestion())
                .answer(card.getAnswer())
                .explanation(card.getExplanation())
                .sortOrder(card.getSortOrder())
                .scheduledDay(card.getScheduledDay())
                .state(card.getState())
                .reviewCount(card.getReviewCount())
                .sourceRefId(card.getSourceRefId() == null ? null : String.valueOf(card.getSourceRefId()))
                .sourceRefType(card.getSourceRefType())
                .lastRating(card.getLastRating())
                .easeFactor(card.getEaseFactor())
                .intervalDays(card.getIntervalDays())
                .streak(card.getStreak())
                .build();
    }

    private DailyCardTask updateTodaySnapshot(Long userId, KnowledgeCardTask task, List<KnowledgeCard> cards) {
        StudyQueue queue = selectStudyQueue(task, cards);
        int todayCompletedCount = countReviewedToday(cards);
        int plannedCount = queue.todayLearnCount + queue.todayReviewCount;
        LocalDate today = LocalDate.now();
        DailyCardTask snapshot = dailyCardTaskMapper.selectOne(new LambdaQueryWrapper<DailyCardTask>()
                .eq(DailyCardTask::getUserId, userId)
                .eq(DailyCardTask::getTaskId, task.getId())
                .eq(DailyCardTask::getTaskDate, today)
                .last("LIMIT 1"));
        if (snapshot == null) {
            snapshot = new DailyCardTask();
            snapshot.setUserId(userId);
            snapshot.setTaskId(task.getId());
            snapshot.setTaskDate(today);
            snapshot.setPlannedCount(plannedCount);
            snapshot.setLearnCount(queue.todayLearnCount);
            snapshot.setReviewCount(queue.todayReviewCount);
            snapshot.setEstimatedMinutes(estimateMinutes(Math.max(1, plannedCount)));
        } else if (!"completed".equals(snapshot.getStatus())) {
            // Keep the day's plan stable after the first load so completion rate does not jump as cards move out of today's queue.
            snapshot.setPlannedCount(Math.max(snapshot.getPlannedCount() == null ? 0 : snapshot.getPlannedCount(), plannedCount));
            snapshot.setLearnCount(Math.max(snapshot.getLearnCount() == null ? 0 : snapshot.getLearnCount(), queue.todayLearnCount));
            snapshot.setReviewCount(Math.max(snapshot.getReviewCount() == null ? 0 : snapshot.getReviewCount(), queue.todayReviewCount));
            snapshot.setEstimatedMinutes(estimateMinutes(Math.max(1, snapshot.getPlannedCount() == null ? plannedCount : snapshot.getPlannedCount())));
        }
        snapshot.setCompletedCount(todayCompletedCount);
        snapshot.setTomorrowDueCount(queue.tomorrowDueCount);
        snapshot.setStreakSnapshot(resolveStudyStreak(cards));
        int snapshotPlannedCount = snapshot.getPlannedCount() == null ? 0 : snapshot.getPlannedCount();
        snapshot.setStatus(snapshotPlannedCount > 0 && todayCompletedCount >= snapshotPlannedCount ? "completed" : "pending");
        if (snapshot.getId() == null) {
            dailyCardTaskMapper.insert(snapshot);
        } else {
            dailyCardTaskMapper.updateById(snapshot);
        }
        return snapshot;
    }

    private void deleteTodaySnapshot(Long userId, Long taskId) {
        dailyCardTaskMapper.delete(new LambdaQueryWrapper<DailyCardTask>()
                .eq(DailyCardTask::getUserId, userId)
                .eq(DailyCardTask::getTaskId, taskId));
    }

    private StudyQueue selectStudyQueue(KnowledgeCardTask task, List<KnowledgeCard> cards) {
        int totalDays = Math.max(1, task.getDays() == null ? 1 : task.getDays());
        int currentDay = resolveCalendarCurrentDay(task, totalDays);
        LocalDateTime now = LocalDateTime.now();

        int todayLearnCount = (int) cards.stream()
                .filter(card -> "new".equals(card.getState()) && normalizeScheduledDay(card.getScheduledDay()) <= currentDay)
                .count();
        int todayReviewCount = (int) cards.stream()
                .filter(card -> !"new".equals(card.getState()) && !"mastered".equals(card.getState()))
                .filter(card -> card.getNextReviewAt() == null || !card.getNextReviewAt().isAfter(now))
                .count();
        int tomorrowDueCount = (int) cards.stream()
                .filter(card -> !"mastered".equals(card.getState()))
                .filter(card -> {
                    if (card.getNextReviewAt() != null) {
                        LocalDate dueDate = card.getNextReviewAt().toLocalDate();
                        return dueDate.equals(LocalDate.now().plusDays(1));
                    }
                    return normalizeScheduledDay(card.getScheduledDay()) == Math.min(totalDays, currentDay + 1);
                })
                .count();

        List<KnowledgeCard> availableCards = cards.stream()
                .filter(card -> !"mastered".equals(card.getState()))
                .filter(card -> !"new".equals(card.getState()) || normalizeScheduledDay(card.getScheduledDay()) <= currentDay)
                .filter(card -> card.getNextReviewAt() == null || !card.getNextReviewAt().isAfter(now))
                .sorted(Comparator
                        .comparing((KnowledgeCard card) -> statePriority(card.getState()))
                        .thenComparing(KnowledgeCard::getSortOrder))
                .toList();

        return new StudyQueue(currentDay, availableCards, todayLearnCount, todayReviewCount, tomorrowDueCount);
    }

    private int resolveCurrentDay(KnowledgeCardTask task, List<KnowledgeCard> cards) {
        return selectStudyQueue(task, cards).currentDay;
    }

    private int resolveCalendarCurrentDay(KnowledgeCardTask task, int totalDays) {
        int storedDay = Math.max(1, Math.min(task.getCurrentDay() != null ? task.getCurrentDay() : 1, totalDays));
        if (task.getStartedAt() == null) {
            return storedDay;
        }
        long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(task.getStartedAt().toLocalDate(), LocalDate.now());
        int calendarDay = (int) Math.max(1, elapsedDays + 1);
        return Math.max(storedDay, Math.min(calendarDay, totalDays));
    }

    private int resolveStudyStreak(List<KnowledgeCard> cards) {
        Set<LocalDate> reviewedDates = cards.stream()
                .map(KnowledgeCard::getLastReviewTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());
        if (reviewedDates.isEmpty()) {
            return 0;
        }
        LocalDate cursor = LocalDate.now();
        int streak = 0;
        while (reviewedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int countReviewedToday(List<KnowledgeCard> cards) {
        return (int) cards.stream()
                .filter(card -> card.getLastReviewTime() != null
                        && card.getLastReviewTime().toLocalDate().equals(LocalDate.now()))
                .count();
    }

    private int estimateMinutes(int cardCount) {
        return Math.max(1, cardCount * 2);
    }

    private String resolveDeckTitle(KnowledgeCardTask task) {
        return StringUtils.hasText(task.getDeckTitle()) ? task.getDeckTitle() : task.getDocTitle();
    }

    private void activateCurrentDeck(Long userId, Long taskId) {
        clearCurrentDeck(userId);
        KnowledgeCardTask task = taskMapper.selectById(taskId);
        if (task != null) {
            task.setIsCurrent(1);
            taskMapper.updateById(task);
        }
    }

    private void clearCurrentDeck(Long userId) {
        List<KnowledgeCardTask> currentTasks = taskMapper.selectList(new LambdaQueryWrapper<KnowledgeCardTask>()
                .eq(KnowledgeCardTask::getUserId, userId)
                .eq(KnowledgeCardTask::getIsCurrent, 1));
        for (KnowledgeCardTask currentTask : currentTasks) {
            currentTask.setIsCurrent(0);
            taskMapper.updateById(currentTask);
        }
    }

    private KnowledgeCardTask findCurrentDeck(Long userId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<KnowledgeCardTask>()
                .eq(KnowledgeCardTask::getUserId, userId)
                .eq(KnowledgeCardTask::getIsCurrent, 1)
                .ne(KnowledgeCardTask::getStatus, "invalid")
                .last("LIMIT 1"));
    }

    private KnowledgeCardTask findLatestActiveOrDraftDeck(Long userId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<KnowledgeCardTask>()
                .eq(KnowledgeCardTask::getUserId, userId)
                .in(KnowledgeCardTask::getStatus, List.of("active", "draft"))
                .orderByDesc(KnowledgeCardTask::getUpdateTime)
                .last("LIMIT 1"));
    }

    private KnowledgeCardTask findWrongDeck(Long userId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<KnowledgeCardTask>()
                .eq(KnowledgeCardTask::getUserId, userId)
                .eq(KnowledgeCardTask::getSourceType, SOURCE_WRONG_AUTO)
                .last("LIMIT 1"));
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

    private void resetKnowledgeCard(KnowledgeCard card) {
        card.setState("new");
        card.setReviewCount(0);
        card.setLastRating(null);
        card.setLastReviewTime(null);
        card.setEaseFactor(DEFAULT_EASE_FACTOR);
        card.setIntervalDays(0);
        card.setStreak(0);
        card.setNextReviewAt(null);
    }

    private String computeCardStateFromWrong(WrongQuestion wrong) {
        if ("mastered".equals(wrong.getMasteryLevel())) {
            return "mastered";
        }
        if ("reviewing".equals(wrong.getMasteryLevel())) {
            return "learning";
        }
        return "weak";
    }

    private int normalizeScheduledDay(Integer scheduledDay) {
        return scheduledDay == null || scheduledDay < 1 ? 1 : scheduledDay;
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
                    result.add(new GeneratedCard(
                            cleanText(payload.getQuestion()),
                            cleanText(payload.getAnswer()),
                            cleanText(payload.getExplanation())));
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
                result.add(new GeneratedCard(question, answer, second));
            }
            if (result.size() >= 12) {
                break;
            }
        }
        return result;
    }

    private String buildFallbackQuestion(String title, String seed) {
        String normalized = seed
                .replaceAll("^[-*#\\d.\\s]+", "")
                .replace("：", "")
                .replace(":", "")
                .trim();
        if (normalized.length() > 26) {
            normalized = normalized.substring(0, 26).trim();
        }
        return "结合《" + title + "》，解释“" + normalized + "”的关键点。";
    }

    private String buildFallbackAnswer(String first, String second, List<String> lines) {
        if (lines.size() == 1) {
            return cleanText(first);
        }
        String joined = lines.stream().limit(3).map(this::cleanText).collect(Collectors.joining("；"));
        if (joined.length() > 280) {
            return cleanText(first) + "；" + cleanText(second);
        }
        return joined;
    }

    private String normalizeJson(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .trim();
        }
        return trimmed;
    }

    private String cleanText(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private record GeneratedCard(String question, String answer, String explanation) {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GeneratedCardPayload {
        private String question;
        private String answer;
        private String explanation;
    }
}
