package com.offerpilot.cards.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.ai.dto.AiChatRequest;
import com.offerpilot.ai.dto.AiChatResponse;
import com.offerpilot.ai.service.LlmGateway;
import com.offerpilot.ai.service.PromptTemplateService;
import com.offerpilot.cards.dto.CardGenerateRequest;
import com.offerpilot.cards.dto.CardRateRequest;
import com.offerpilot.cards.dto.CardTaskCreateRequest;
import com.offerpilot.cards.entity.DailyCardTask;
import com.offerpilot.cards.entity.KnowledgeCard;
import com.offerpilot.cards.entity.KnowledgeCardLog;
import com.offerpilot.cards.entity.KnowledgeCardTask;
import com.offerpilot.cards.mapper.DailyCardTaskMapper;
import com.offerpilot.cards.mapper.KnowledgeCardLogMapper;
import com.offerpilot.cards.mapper.KnowledgeCardMapper;
import com.offerpilot.cards.mapper.KnowledgeCardTaskMapper;
import com.offerpilot.cards.service.KnowledgeCardService;
import com.offerpilot.cards.vo.CardDeckSummaryVO;
import com.offerpilot.cards.vo.CardStatsSummaryVO;
import com.offerpilot.cards.vo.KnowledgeCardItemVO;
import com.offerpilot.cards.vo.KnowledgeCardTaskVO;
import com.offerpilot.cards.vo.TodayCardsTaskVO;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.knowledge.entity.KnowledgeChunk;
import com.offerpilot.knowledge.entity.KnowledgeDoc;
import com.offerpilot.knowledge.mapper.KnowledgeChunkMapper;
import com.offerpilot.knowledge.mapper.KnowledgeDocMapper;
import com.offerpilot.interview.entity.InterviewRecord;
import com.offerpilot.interview.entity.InterviewSession;
import com.offerpilot.interview.mapper.InterviewRecordMapper;
import com.offerpilot.interview.mapper.InterviewSessionMapper;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.wrong.entity.WrongQuestion;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import com.offerpilot.wrong.dto.ReviewScheduleResult;
import com.offerpilot.wrong.support.ReviewSchedulingRules;
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
    private static final String SOURCE_INTERVIEW_AUTO = "interview_auto";
    private static final String SOURCE_REF_KNOWLEDGE_CHUNK = "knowledge_chunk";
    private static final String SOURCE_REF_WRONG_QUESTION = "wrong_question";
    private static final String SOURCE_REF_INTERVIEW_RECORD = "interview_record";
    private static final String WRONG_DECK_TITLE = "错题复习";
    private static final String INTERVIEW_DECK_TITLE = "面试诊断卡片";
    private static final long WRONG_DECK_DOC_ID = -1L;
    private static final long INTERVIEW_DECK_DOC_ID = -2L;
    private static final List<String> DEFAULT_CARD_TYPES = List.of("concept", "qa", "scenario", "compare");
    private static final Set<String> ALLOWED_CARD_TYPES = Set.copyOf(DEFAULT_CARD_TYPES);
    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of("auto", "easy", "medium", "hard");

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
    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewRecordMapper interviewRecordMapper;
    private final QuestionMapper questionMapper;
    private final PromptTemplateService promptTemplateService;
    private final LlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public KnowledgeCardTaskVO createTask(Long userId, CardTaskCreateRequest request) {
        CardGenerateRequest generateRequest = new CardGenerateRequest();
        generateRequest.setDocId(request.getDocId());
        generateRequest.setDays(request.getDays());
        return generateKnowledgeDeck(userId, generateRequest, false);
    }

    @Override
    @Transactional
    public KnowledgeCardTaskVO generateDeck(Long userId, CardGenerateRequest request) {
        return generateKnowledgeDeck(userId, request, true);
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
    public KnowledgeCardTaskVO getInterviewDeck(Long userId) {
        KnowledgeCardTask deck = findInterviewDeck(userId);
        return deck == null ? null : buildTaskVO(deck, loadCards(deck.getId()));
    }

    @Override
    @Transactional
    public KnowledgeCardTaskVO syncInterviewDeckBySession(Long userId, Long sessionId) {
        InterviewSession session = interviewSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "interview session not found");
        }
        List<InterviewRecord> records = interviewRecordMapper.selectList(new LambdaQueryWrapper<InterviewRecord>()
                .eq(InterviewRecord::getSessionId, sessionId)
                .eq(InterviewRecord::getUserId, userId)
                .orderByAsc(InterviewRecord::getId));
        syncInterviewDeck(userId, records);
        KnowledgeCardTask deck = findInterviewDeck(userId);
        return deck == null ? null : buildTaskVO(deck, loadCards(deck.getId()));
    }

    @Override
    @Transactional
    public TodayCardsTaskVO activateInterviewDeck(Long userId) {
        KnowledgeCardTask deck = findInterviewDeck(userId);
        if (deck == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "interview deck not found");
        }
        return activateDeck(userId, deck.getId());
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

    @Transactional
    protected void syncInterviewDeck(Long userId, List<InterviewRecord> records) {
        if (records.isEmpty()) {
            return;
        }
        List<InterviewRecord> lowScoreRecords = records.stream()
                .filter(record -> Integer.valueOf(1).equals(record.getIsWrong()))
                .toList();
        Set<Long> sessionRecordIds = records.stream().map(InterviewRecord::getId).collect(Collectors.toSet());

        KnowledgeCardTask deck = findInterviewDeck(userId);
        if (lowScoreRecords.isEmpty()) {
            if (deck != null) {
                List<KnowledgeCard> existingCards = loadCards(deck.getId());
                for (KnowledgeCard existingCard : existingCards) {
                    if (SOURCE_REF_INTERVIEW_RECORD.equals(existingCard.getSourceRefType())
                            && existingCard.getSourceRefId() != null
                            && sessionRecordIds.contains(existingCard.getSourceRefId())) {
                        cardMapper.deleteById(existingCard.getId());
                    }
                }
                refreshInterviewDeck(deck, userId);
            }
            return;
        }

        if (deck == null) {
            deck = new KnowledgeCardTask();
            deck.setUserId(userId);
            deck.setDocId(INTERVIEW_DECK_DOC_ID);
            deck.setDocTitle(INTERVIEW_DECK_TITLE);
            deck.setDeckTitle(INTERVIEW_DECK_TITLE);
            deck.setSourceType(SOURCE_INTERVIEW_AUTO);
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

        Map<Long, Question> questionMap = questionMapper.selectBatchIds(lowScoreRecords.stream()
                        .map(InterviewRecord::getQuestionId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        List<KnowledgeCard> existingCards = loadCards(deck.getId());
        Map<Long, KnowledgeCard> existingByRecordId = existingCards.stream()
                .filter(card -> SOURCE_REF_INTERVIEW_RECORD.equals(card.getSourceRefType()) && card.getSourceRefId() != null)
                .collect(Collectors.toMap(KnowledgeCard::getSourceRefId, Function.identity(), (a, b) -> a));

        Set<Long> activeRecordIds = lowScoreRecords.stream().map(InterviewRecord::getId).collect(Collectors.toSet());
        for (KnowledgeCard existingCard : existingCards) {
            if (SOURCE_REF_INTERVIEW_RECORD.equals(existingCard.getSourceRefType())
                    && existingCard.getSourceRefId() != null
                    && sessionRecordIds.contains(existingCard.getSourceRefId())
                    && !activeRecordIds.contains(existingCard.getSourceRefId())) {
                cardMapper.deleteById(existingCard.getId());
            }
        }

        int sortOrder = 1;
        for (InterviewRecord record : lowScoreRecords) {
            Question question = questionMap.get(record.getQuestionId());
            KnowledgeCard card = existingByRecordId.get(record.getId());
            if (card == null) {
                card = new KnowledgeCard();
                card.setTaskId(deck.getId());
                card.setReviewCount(0);
                card.setEaseFactor(DEFAULT_EASE_FACTOR);
                card.setIntervalDays(0);
                card.setStreak(0);
                card.setState("new");
            }
            card.setQuestion(question != null ? question.getTitle() : "Unknown");
            card.setAnswer(question != null ? question.getStandardAnswer() : "暂无标准答案");
            card.setExplanation(buildInterviewExplanation(record));
            card.setCardType("qa");
            card.setDifficulty(resolveInterviewDifficulty(record));
            card.setTags("interview,diagnosis");
            card.setSourceQuote(record.getFollowUp());
            card.setSortOrder(sortOrder++);
            card.setScheduledDay(1);
            card.setSourceRefId(record.getId());
            card.setSourceRefType(SOURCE_REF_INTERVIEW_RECORD);
            if (card.getId() == null) {
                cardMapper.insert(card);
            } else {
                cardMapper.updateById(card);
            }
        }

        refreshInterviewDeck(deck, userId);
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

    private KnowledgeCardTaskVO generateKnowledgeDeck(Long userId, CardGenerateRequest request, boolean setCurrent) {
        validateGenerateRequest(request);
        Long docId = request.getDocId();
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

        List<GeneratedCard> generatedCards = generateCards(doc, chunks, request);
        if (generatedCards.isEmpty()) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "知识卡片生成失败");
        }

        int totalCards = generatedCards.size();
        int safeDays = request.getDays() == null ? 7 : request.getDays();
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
            card.setCardType(generated.cardType());
            card.setDifficulty(generated.difficulty());
            card.setTags(generated.tags());
            card.setSourceQuote(generated.sourceQuote());
            card.setSortOrder(i + 1);
            card.setScheduledDay(Math.min(safeDays, i / dailyTarget + 1));
            card.setState("new");
            card.setReviewCount(0);
            card.setSourceRefType(SOURCE_REF_KNOWLEDGE_CHUNK);
            card.setSourceRefId(resolveChunkId(generated.sourceChunkId(), generated.sourceQuote(), chunks));
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

        ReviewScheduleResult schedule = ReviewSchedulingRules.schedule(efBefore, intervalBefore, card.getStreak(), rating);

        card.setEaseFactor(schedule.getEaseFactor());
        card.setIntervalDays(schedule.getIntervalDays());
        card.setStreak(schedule.getStreak());
        card.setState(resolveCardState(schedule.getMasteryLevel(), rating));
        card.setReviewCount((card.getReviewCount() != null ? card.getReviewCount() : 0) + 1);
        card.setLastRating(rating);
        card.setLastReviewTime(LocalDateTime.now());
        card.setNextReviewAt(schedule.getNextReviewDate().atStartOfDay());
        cardMapper.updateById(card);

        KnowledgeCardLog logItem = new KnowledgeCardLog();
        logItem.setUserId(userId);
        logItem.setTaskId(task.getId());
        logItem.setCardId(card.getId());
        logItem.setRating(rating);
        logItem.setResponseTimeMs(request.getResponseTimeMs());
        logItem.setEaseFactorBefore(efBefore);
        logItem.setIntervalBefore(intervalBefore);
        logItem.setEaseFactorAfter(schedule.getEaseFactor());
        logItem.setIntervalAfter(schedule.getIntervalDays());
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
                .cardType(card.getCardType())
                .difficulty(card.getDifficulty())
                .tags(card.getTags())
                .sourceQuote(card.getSourceQuote())
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

    private KnowledgeCardTask findInterviewDeck(Long userId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<KnowledgeCardTask>()
                .eq(KnowledgeCardTask::getUserId, userId)
                .eq(KnowledgeCardTask::getSourceType, SOURCE_INTERVIEW_AUTO)
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

    private String buildInterviewExplanation(InterviewRecord record) {
        String comment = StringUtils.hasText(record.getComment()) ? record.getComment() : "本题建议回到标准答案，重点补齐薄弱点。";
        String userAnswer = StringUtils.hasText(record.getUserAnswer()) ? record.getUserAnswer() : "未作答";
        return comment + "\n\n回答问题点：\n" + userAnswer;
    }

    private String resolveInterviewDifficulty(InterviewRecord record) {
        if (record.getScore() == null) {
            return "medium";
        }
        if (record.getScore().compareTo(new BigDecimal("40")) < 0) {
            return "hard";
        }
        if (record.getScore().compareTo(new BigDecimal("70")) < 0) {
            return "medium";
        }
        return "easy";
    }

    private void refreshInterviewDeck(KnowledgeCardTask deck, Long userId) {
        List<KnowledgeCard> refreshed = loadCards(deck.getId());
        deck.setDeckTitle(INTERVIEW_DECK_TITLE);
        deck.setDocTitle(INTERVIEW_DECK_TITLE);
        deck.setSourceType(SOURCE_INTERVIEW_AUTO);
        deck.setStatus(refreshed.isEmpty() ? "completed" : "active");
        deck.setCurrentDay(1);
        deck.setDays(1);
        deck.setTotalCards(refreshed.size());
        deck.setMasteredCards((int) refreshed.stream().filter(item -> "mastered".equals(item.getState())).count());
        deck.setDailyTarget(refreshed.size());
        deck.setEstimatedMinutes(estimateMinutes(Math.max(1, refreshed.size())));
        taskMapper.updateById(deck);
        if (refreshed.isEmpty()) {
            deleteTodaySnapshot(userId, deck.getId());
        } else {
            updateTodaySnapshot(userId, deck, refreshed);
        }
    }

    private String resolveCardState(String masteryLevel, Integer rating) {
        if ("mastered".equals(masteryLevel)) {
            return "mastered";
        }
        return rating != null && rating <= 1 ? "weak" : "learning";
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

    private List<GeneratedCard> generateCards(KnowledgeDoc doc, List<KnowledgeChunk> chunks, CardGenerateRequest request) {
        List<GeneratedCard> cards = generateByLlm(doc, chunks, request);
        if (!cards.isEmpty()) {
            return cards;
        }
        return generateFallbackCards(doc, chunks, request);
    }

    private List<GeneratedCard> generateByLlm(KnowledgeDoc doc, List<KnowledgeChunk> chunks, CardGenerateRequest request) {
        try {
            String excerpt = chunks.stream()
                    .limit(8)
                    .map(chunk -> "[chunkId=" + chunk.getId() + "]\n" + chunk.getContent())
                    .reduce((left, right) -> left + "\n\n---\n\n" + right)
                    .orElse("");
            String prompt = """
                    文档标题：%s

                    生成要求：
                    - 卡片类型：%s
                    - 期望数量：%d
                    - 难度：%s

                    文档内容：
                    %s
                    """.formatted(
                    doc.getTitle(),
                    String.join(", ", normalizeCardTypes(request.getCardTypes())),
                    resolveCardCount(request.getCardCount()),
                    normalizeDifficulty(request.getDifficulty()),
                    excerpt);
            AiChatResponse response = llmGateway.chatCompletion(AiChatRequest.builder()
                    .systemPrompt(promptTemplateService.knowledgeCardPrompt())
                    .userPrompt(prompt)
                    .references(List.of())
                    .build());
            String content = normalizeJson(response.getContent());
            List<GeneratedCardPayload> payloads =
                    objectMapper.readValue(content, new TypeReference<List<GeneratedCardPayload>>() {});
            List<GeneratedCard> result = new ArrayList<>();
            Set<String> dedupe = new LinkedHashSet<>();
            for (GeneratedCardPayload payload : payloads) {
                if (payload == null || !StringUtils.hasText(payload.getFront()) || !StringUtils.hasText(payload.getBack())) {
                    continue;
                }
                String key = payload.getFront().trim().toLowerCase(Locale.ROOT);
                if (dedupe.add(key)) {
                    result.add(new GeneratedCard(
                            cleanText(payload.getFront()),
                            cleanText(payload.getBack()),
                            cleanText(payload.getExplanation()),
                            normalizeCardType(payload.getCardType(), request.getCardTypes()),
                            normalizeGeneratedDifficulty(payload.getDifficulty(), request.getDifficulty()),
                            joinTags(payload.getTags()),
                            cleanText(payload.getSourceQuote()),
                            payload.getSourceChunkId()));
                }
                if (result.size() >= resolveCardCount(request.getCardCount())) {
                    break;
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Knowledge card LLM generation failed for doc {}: {}", doc.getId(), e.getMessage());
            return List.of();
        }
    }

    private List<GeneratedCard> generateFallbackCards(KnowledgeDoc doc, List<KnowledgeChunk> chunks, CardGenerateRequest request) {
        List<GeneratedCard> result = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();
        String fallbackType = normalizeCardTypes(request.getCardTypes()).get(0);
        String fallbackDifficulty = normalizeDifficulty(request.getDifficulty());
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
                result.add(new GeneratedCard(
                        question,
                        answer,
                        second,
                        fallbackType,
                        "auto".equals(fallbackDifficulty) ? "medium" : fallbackDifficulty,
                        deriveFallbackTags(doc.getTitle(), first),
                        cleanText(first),
                        chunk.getId()));
            }
            if (result.size() >= resolveCardCount(request.getCardCount())) {
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

    private void validateGenerateRequest(CardGenerateRequest request) {
        if (!ALLOWED_DIFFICULTIES.contains(normalizeDifficulty(request.getDifficulty()))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "difficulty must be auto, easy, medium or hard");
        }
        if (normalizeCardTypes(request.getCardTypes()).isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "cardTypes must contain at least one valid type");
        }
    }

    private List<String> normalizeCardTypes(List<String> cardTypes) {
        if (cardTypes == null || cardTypes.isEmpty()) {
            return DEFAULT_CARD_TYPES;
        }
        return cardTypes.stream()
                .filter(StringUtils::hasText)
                .map(type -> type.trim().toLowerCase(Locale.ROOT))
                .filter(ALLOWED_CARD_TYPES::contains)
                .distinct()
                .toList();
    }

    private String normalizeDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return "auto";
        }
        return difficulty.trim().toLowerCase(Locale.ROOT);
    }

    private int resolveCardCount(Integer cardCount) {
        if (cardCount == null) {
            return 12;
        }
        return Math.max(4, Math.min(30, cardCount));
    }

    private String normalizeCardType(String value, List<String> requestedTypes) {
        String normalized = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
        if (ALLOWED_CARD_TYPES.contains(normalized)) {
            return normalized;
        }
        return normalizeCardTypes(requestedTypes).get(0);
    }

    private String normalizeGeneratedDifficulty(String value, String requestedDifficulty) {
        String normalized = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
        if (Set.of("easy", "medium", "hard").contains(normalized)) {
            return normalized;
        }
        String fallback = normalizeDifficulty(requestedDifficulty);
        return "auto".equals(fallback) ? "medium" : fallback;
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream()
                .filter(StringUtils::hasText)
                .map(this::cleanText)
                .distinct()
                .limit(3)
                .collect(Collectors.joining(","));
    }

    private String deriveFallbackTags(String title, String firstLine) {
        List<String> tags = new ArrayList<>();
        if (StringUtils.hasText(title)) {
            tags.add(cleanText(title));
        }
        if (StringUtils.hasText(firstLine)) {
            String seed = cleanText(firstLine);
            if (seed.length() > 18) {
                seed = seed.substring(0, 18).trim();
            }
            tags.add(seed);
        }
        return String.join(",", tags.stream().distinct().limit(2).toList());
    }

    private Long resolveChunkId(Long sourceChunkId, String sourceQuote, List<KnowledgeChunk> chunks) {
        if (sourceChunkId != null && chunks.stream().anyMatch(chunk -> chunk.getId().equals(sourceChunkId))) {
            return sourceChunkId;
        }
        if (StringUtils.hasText(sourceQuote)) {
            for (KnowledgeChunk chunk : chunks) {
                if (chunk.getContent() != null && chunk.getContent().contains(sourceQuote)) {
                    return chunk.getId();
                }
            }
        }
        return chunks.isEmpty() ? null : chunks.get(0).getId();
    }

    private record GeneratedCard(
            String question,
            String answer,
            String explanation,
            String cardType,
            String difficulty,
            String tags,
            String sourceQuote,
            Long sourceChunkId) {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GeneratedCardPayload {
        private String front;
        private String back;
        private String explanation;
        private List<String> tags;
        private String difficulty;
        private String cardType;
        private String sourceQuote;
        private Long sourceChunkId;
    }
}
