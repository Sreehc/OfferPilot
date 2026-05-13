package com.offerpilot.wrong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.cards.dto.CardRateRequest;
import com.offerpilot.cards.entity.KnowledgeCard;
import com.offerpilot.cards.entity.KnowledgeCardLog;
import com.offerpilot.cards.entity.KnowledgeCardTask;
import com.offerpilot.cards.mapper.KnowledgeCardLogMapper;
import com.offerpilot.cards.mapper.KnowledgeCardMapper;
import com.offerpilot.cards.mapper.KnowledgeCardTaskMapper;
import com.offerpilot.cards.service.KnowledgeCardService;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.wrong.dto.ReviewRateRequest;
import com.offerpilot.wrong.dto.ReviewScheduleResult;
import com.offerpilot.wrong.dto.ReviewStatsVO;
import com.offerpilot.wrong.dto.ReviewTodayItemVO;
import com.offerpilot.wrong.dto.ReviewTodayVO;
import com.offerpilot.wrong.entity.ReviewLog;
import com.offerpilot.wrong.entity.WrongQuestion;
import com.offerpilot.wrong.mapper.ReviewLogMapper;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import com.offerpilot.wrong.service.SpacedRepetitionService;
import com.offerpilot.wrong.support.ReviewSchedulingRules;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpacedRepetitionServiceImpl implements SpacedRepetitionService {

    private static final String CONTENT_ALL = "all";
    private static final String CONTENT_KNOWLEDGE_CARD = "knowledge_card";
    private static final String CONTENT_WRONG_CARD = "wrong_card";
    private static final String CONTENT_INTERVIEW_CARD = "interview_card";
    private static final String SOURCE_INTERVIEW_AUTO = "interview_auto";
    private static final String SOURCE_REF_WRONG_QUESTION = "wrong_question";
    private static final String SOURCE_REF_INTERVIEW_RECORD = "interview_record";
    private static final String SOURCE_WRONG_AUTO = "wrong_auto";

    private final WrongQuestionMapper wrongQuestionMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final QuestionMapper questionMapper;
    private final KnowledgeCardMapper knowledgeCardMapper;
    private final KnowledgeCardTaskMapper knowledgeCardTaskMapper;
    private final KnowledgeCardLogMapper knowledgeCardLogMapper;
    private final KnowledgeCardService knowledgeCardService;

    @Override
    public ReviewTodayVO getTodayReviews(Long userId, String contentType) {
        LocalDate today = LocalDate.now();
        String normalizedContentType = normalizeContentType(contentType);
        List<ReviewTodayItemVO> allItems = new ArrayList<>();
        allItems.addAll(loadWrongReviewItems(userId, today));
        allItems.addAll(loadKnowledgeReviewItems(userId, today));

        Map<String, Integer> countsByContentType = buildContentTypeDistribution(allItems);
        List<ReviewTodayItemVO> filteredItems = allItems.stream()
                .filter(item -> matchesContentType(item, normalizedContentType))
                .sorted(reviewComparator())
                .toList();

        int todayCompleted = countReviewsToday(userId);
        int currentStreak = computeReviewStreak(userId);
        int overdueCount = (int) filteredItems.stream().filter(item -> item.getOverdueDays() > 0).count();

        return ReviewTodayVO.builder()
                .selectedContentType(normalizedContentType)
                .totalPending(filteredItems.size())
                .overdueCount(overdueCount)
                .todayCompleted(todayCompleted)
                .currentStreak(currentStreak)
                .countsByContentType(countsByContentType)
                .items(filteredItems)
                .build();
    }

    @Override
    @Transactional
    public ReviewTodayVO rate(Long userId, Long reviewItemId, ReviewRateRequest request) {
        String normalizedContentType = normalizeContentType(request.getContentType());
        if (CONTENT_KNOWLEDGE_CARD.equals(normalizedContentType)) {
            rateKnowledgeCard(userId, reviewItemId, request);
            return getTodayReviews(userId, normalizedContentType);
        }
        if (CONTENT_INTERVIEW_CARD.equals(normalizedContentType)) {
            rateWrongQuestion(userId, reviewItemId, request);
            return getTodayReviews(userId, normalizedContentType);
        }
        if (CONTENT_WRONG_CARD.equals(normalizedContentType)) {
            rateWrongQuestion(userId, reviewItemId, request);
            return getTodayReviews(userId, normalizedContentType);
        }

        WrongQuestion wrongQuestion = wrongQuestionMapper.selectById(reviewItemId);
        if (wrongQuestion != null && userId.equals(wrongQuestion.getUserId())) {
            rateWrongQuestion(userId, reviewItemId, request);
            return getTodayReviews(userId, CONTENT_ALL);
        }

        KnowledgeCard card = knowledgeCardMapper.selectById(reviewItemId);
        if (card != null) {
            KnowledgeCardTask task = knowledgeCardTaskMapper.selectById(card.getTaskId());
            if (task != null && userId.equals(task.getUserId())) {
                rateKnowledgeCard(userId, reviewItemId, request);
                return getTodayReviews(userId, CONTENT_ALL);
            }
        }

        throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "review item not found");
    }

    @Override
    public ReviewStatsVO getReviewStats(Long userId) {
        ReviewTodayVO todayView = getTodayReviews(userId, CONTENT_ALL);
        return ReviewStatsVO.builder()
                .totalReviews(countTotalReviews(userId))
                .currentStreak(todayView.getCurrentStreak())
                .todayPending(todayView.getTotalPending())
                .todayCompleted(todayView.getTodayCompleted())
                .overdueCount(todayView.getOverdueCount())
                .contentTypeDistribution(todayView.getCountsByContentType())
                .heatmap(buildHeatmap(userId))
                .build();
    }

    @Override
    public ReviewScheduleResult schedule(BigDecimal easeFactor, Integer intervalDays, Integer streak, Integer rating) {
        return ReviewSchedulingRules.schedule(easeFactor, intervalDays, streak, rating);
    }

    @Override
    public String computeMasteryLevel(BigDecimal easeFactor, Integer streak) {
        return ReviewSchedulingRules.resolveMasteryLevel(easeFactor, streak);
    }

    private List<ReviewTodayItemVO> loadWrongReviewItems(Long userId, LocalDate today) {
        List<WrongQuestion> dueItems = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .le(WrongQuestion::getNextReviewDate, today)
                        .orderByAsc(WrongQuestion::getNextReviewDate));

        if (dueItems.isEmpty()) {
            return List.of();
        }

        Set<Long> questionIds = dueItems.stream()
                .map(WrongQuestion::getQuestionId)
                .collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        List<ReviewTodayItemVO> result = new ArrayList<>();
        for (WrongQuestion wq : dueItems) {
            Question q = questionMap.get(wq.getQuestionId());
            LocalDate dueDate = wq.getNextReviewDate() == null ? today : wq.getNextReviewDate();
            long overdueDays = ChronoUnit.DAYS.between(dueDate, today);
            String contentType = "interview".equalsIgnoreCase(wq.getSourceType())
                    ? CONTENT_INTERVIEW_CARD
                    : CONTENT_WRONG_CARD;

            result.add(ReviewTodayItemVO.builder()
                    .reviewItemId(String.valueOf(wq.getId()))
                    .contentType(contentType)
                    .sourceType(wq.getSourceType())
                    .title(q != null ? q.getTitle() : "Unknown")
                    .answer(wq.getStandardAnswer())
                    .explanation(wq.getErrorReason())
                    .easeFactor(wq.getEaseFactor())
                    .intervalDays(wq.getIntervalDays())
                    .streak(wq.getStreak())
                    .nextReviewDate(wq.getNextReviewDate())
                    .nextReviewAt(wq.getNextReviewDate() == null ? null : wq.getNextReviewDate().atStartOfDay())
                    .overdueDays(overdueDays)
                    .masteryLevel(ReviewSchedulingRules.resolveMasteryLevel(wq.getEaseFactor(), wq.getStreak()))
                    .wrongQuestionId(String.valueOf(wq.getId()))
                    .build());
        }
        return result;
    }

    private List<ReviewTodayItemVO> loadKnowledgeReviewItems(Long userId, LocalDate today) {
        List<KnowledgeCardTask> tasks = knowledgeCardTaskMapper.selectList(new LambdaQueryWrapper<KnowledgeCardTask>()
                .eq(KnowledgeCardTask::getUserId, userId)
                .ne(KnowledgeCardTask::getStatus, "invalid"));
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, KnowledgeCardTask> taskMap = tasks.stream()
                .collect(Collectors.toMap(KnowledgeCardTask::getId, Function.identity(), (a, b) -> a));
        List<KnowledgeCard> dueCards = knowledgeCardMapper.selectList(new LambdaQueryWrapper<KnowledgeCard>()
                .in(KnowledgeCard::getTaskId, taskMap.keySet()));

        LocalDateTime now = LocalDateTime.now();
        return dueCards.stream()
                .filter(card -> !"mastered".equals(card.getState()))
                .filter(card -> !"new".equals(card.getState()))
                .filter(card -> card.getNextReviewAt() == null || !card.getNextReviewAt().isAfter(now))
                .map(card -> {
                    KnowledgeCardTask task = taskMap.get(card.getTaskId());
                    LocalDateTime dueAt = card.getNextReviewAt();
                    LocalDate dueDate = dueAt == null ? today : dueAt.toLocalDate();
                    long overdueDays = ChronoUnit.DAYS.between(dueDate, today);
                    String contentType = resolveKnowledgeContentType(task, card);
                    return ReviewTodayItemVO.builder()
                            .reviewItemId(String.valueOf(card.getId()))
                            .contentType(contentType)
                            .sourceType(task == null ? null : task.getSourceType())
                            .title(card.getQuestion())
                            .answer(card.getAnswer())
                            .explanation(card.getExplanation())
                            .easeFactor(card.getEaseFactor())
                            .intervalDays(card.getIntervalDays())
                            .streak(card.getStreak())
                            .nextReviewDate(dueDate)
                            .nextReviewAt(dueAt)
                            .overdueDays(overdueDays)
                            .masteryLevel(resolveKnowledgeMastery(card))
                            .cardId(String.valueOf(card.getId()))
                            .deckId(task == null ? null : String.valueOf(task.getId()))
                            .deckTitle(task == null ? null : resolveDeckTitle(task))
                            .sourceQuote(card.getSourceQuote())
                            .build();
                })
                .toList();
    }

    private void rateWrongQuestion(Long userId, Long wrongQuestionId, ReviewRateRequest request) {
        WrongQuestion wq = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wq == null || !wq.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "wrong question not found");
        }

        BigDecimal efBefore = wq.getEaseFactor() != null ? wq.getEaseFactor() : ReviewSchedulingRules.DEFAULT_EASE_FACTOR;
        int intervalBefore = wq.getIntervalDays() != null ? wq.getIntervalDays() : 0;
        ReviewScheduleResult schedule = schedule(efBefore, intervalBefore, wq.getStreak(), request.getRating());

        wq.setEaseFactor(schedule.getEaseFactor());
        wq.setIntervalDays(schedule.getIntervalDays());
        wq.setNextReviewDate(schedule.getNextReviewDate());
        wq.setStreak(schedule.getStreak());
        wq.setReviewCount((wq.getReviewCount() != null ? wq.getReviewCount() : 0) + 1);
        wq.setLastReviewTime(java.time.LocalDateTime.now());
        wq.setMasteryLevel(schedule.getMasteryLevel());

        wrongQuestionMapper.updateById(wq);

        ReviewLog reviewLog = new ReviewLog();
        reviewLog.setUserId(userId);
        reviewLog.setWrongQuestionId(wrongQuestionId);
        reviewLog.setRating(request.getRating());
        reviewLog.setResponseTimeMs(request.getResponseTimeMs());
        reviewLog.setEaseFactorBefore(efBefore);
        reviewLog.setIntervalBefore(intervalBefore);
        reviewLog.setEaseFactorAfter(schedule.getEaseFactor());
        reviewLog.setIntervalAfter(schedule.getIntervalDays());
        reviewLogMapper.insert(reviewLog);

        log.info("Review rated: userId={}, wqId={}, rating={}, EF {}->{}, interval {}->{} days, next={}",
                userId, wrongQuestionId, request.getRating(), efBefore, schedule.getEaseFactor(), intervalBefore,
                schedule.getIntervalDays(), schedule.getNextReviewDate());
        knowledgeCardService.syncWrongDeck(userId);
    }

    private void rateKnowledgeCard(Long userId, Long cardId, ReviewRateRequest request) {
        KnowledgeCard card = knowledgeCardMapper.selectById(cardId);
        if (card == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "knowledge card not found");
        }
        KnowledgeCardTask task = knowledgeCardTaskMapper.selectById(card.getTaskId());
        if (task == null || !userId.equals(task.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "knowledge card not found");
        }
        CardRateRequest cardRateRequest = new CardRateRequest();
        cardRateRequest.setCardId(cardId);
        cardRateRequest.setRating(request.getRating());
        cardRateRequest.setResponseTimeMs(request.getResponseTimeMs());
        knowledgeCardService.reviewDeck(userId, task.getId(), cardRateRequest);
    }

    private int countTotalReviews(Long userId) {
        long wrongCount = reviewLogMapper.selectCount(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getUserId, userId));
        long knowledgeCount = knowledgeCardLogMapper.selectCount(new LambdaQueryWrapper<KnowledgeCardLog>()
                .eq(KnowledgeCardLog::getUserId, userId));
        return Math.toIntExact(wrongCount + knowledgeCount);
    }

    private int countReviewsToday(Long userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        long wrongCount = reviewLogMapper.selectCount(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getUserId, userId)
                .ge(ReviewLog::getCreateTime, start));
        long knowledgeCount = knowledgeCardLogMapper.selectCount(new LambdaQueryWrapper<KnowledgeCardLog>()
                .eq(KnowledgeCardLog::getUserId, userId)
                .ge(KnowledgeCardLog::getCreateTime, start));
        return Math.toIntExact(wrongCount + knowledgeCount);
    }

    private int computeReviewStreak(Long userId) {
        List<ReviewLog> recentLogs = reviewLogMapper.selectList(
                new LambdaQueryWrapper<ReviewLog>()
                        .eq(ReviewLog::getUserId, userId)
                        .select(ReviewLog::getCreateTime)
                        .orderByDesc(ReviewLog::getCreateTime)
                        .last("LIMIT 1000"));
        List<KnowledgeCardLog> recentCardLogs = knowledgeCardLogMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCardLog>()
                        .eq(KnowledgeCardLog::getUserId, userId)
                        .select(KnowledgeCardLog::getCreateTime)
                        .orderByDesc(KnowledgeCardLog::getCreateTime)
                        .last("LIMIT 1000"));

        if (recentLogs.isEmpty() && recentCardLogs.isEmpty()) {
            return 0;
        }

        Set<LocalDate> reviewDates = recentLogs.stream()
                .map(r -> r.getCreateTime().toLocalDate())
                .collect(Collectors.toSet());
        reviewDates.addAll(recentCardLogs.stream()
                .map(r -> r.getCreateTime().toLocalDate())
                .collect(Collectors.toSet()));

        int streak = 0;
        LocalDate checkDate = LocalDate.now();

        if (!reviewDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1);
            if (!reviewDates.contains(checkDate)) {
                return 0;
            }
        }

        while (reviewDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    private Map<String, Integer> buildContentTypeDistribution(List<ReviewTodayItemVO> items) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put(CONTENT_KNOWLEDGE_CARD, 0);
        distribution.put(CONTENT_WRONG_CARD, 0);
        distribution.put(CONTENT_INTERVIEW_CARD, 0);
        for (ReviewTodayItemVO item : items) {
            distribution.compute(item.getContentType(), (key, value) -> value == null ? 1 : value + 1);
        }
        return distribution;
    }

    private Map<String, Integer> buildHeatmap(Long userId) {
        Map<String, Integer> heatmap = new HashMap<>();
        List<ReviewLog> wrongLogs = reviewLogMapper.selectList(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getUserId, userId)
                .ge(ReviewLog::getCreateTime, LocalDate.now().minusDays(90).atStartOfDay()));
        for (ReviewLog logItem : wrongLogs) {
            String dateKey = logItem.getCreateTime().toLocalDate().toString();
            heatmap.merge(dateKey, 1, Integer::sum);
        }
        List<KnowledgeCardLog> cardLogs = knowledgeCardLogMapper.selectList(new LambdaQueryWrapper<KnowledgeCardLog>()
                .eq(KnowledgeCardLog::getUserId, userId)
                .ge(KnowledgeCardLog::getCreateTime, LocalDate.now().minusDays(90).atStartOfDay()));
        for (KnowledgeCardLog logItem : cardLogs) {
            String dateKey = logItem.getCreateTime().toLocalDate().toString();
            heatmap.merge(dateKey, 1, Integer::sum);
        }
        return heatmap;
    }

    private Comparator<ReviewTodayItemVO> reviewComparator() {
        return Comparator
                .comparingLong(ReviewTodayItemVO::getOverdueDays).reversed()
                .thenComparing(item -> item.getEaseFactor() == null ? ReviewSchedulingRules.DEFAULT_EASE_FACTOR : item.getEaseFactor())
                .thenComparing(ReviewTodayItemVO::getTitle, Comparator.nullsLast(String::compareTo));
    }

    private boolean matchesContentType(ReviewTodayItemVO item, String contentType) {
        return CONTENT_ALL.equals(contentType) || contentType.equals(item.getContentType());
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return CONTENT_ALL;
        }
        return switch (contentType) {
            case CONTENT_KNOWLEDGE_CARD, CONTENT_WRONG_CARD, CONTENT_INTERVIEW_CARD -> contentType;
            default -> CONTENT_ALL;
        };
    }

    private String resolveKnowledgeMastery(KnowledgeCard card) {
        if ("mastered".equals(card.getState())) {
            return "mastered";
        }
        if ("weak".equals(card.getState())) {
            return "not_started";
        }
        return "reviewing";
    }

    private String resolveDeckTitle(KnowledgeCardTask task) {
        if (task == null) {
            return null;
        }
        return task.getDeckTitle() != null && !task.getDeckTitle().isBlank() ? task.getDeckTitle() : task.getDocTitle();
    }

    private String resolveKnowledgeContentType(KnowledgeCardTask task, KnowledgeCard card) {
        if (task != null && SOURCE_INTERVIEW_AUTO.equals(task.getSourceType())) {
            return CONTENT_INTERVIEW_CARD;
        }
        if (SOURCE_REF_INTERVIEW_RECORD.equals(card.getSourceRefType())) {
            return CONTENT_INTERVIEW_CARD;
        }
        return CONTENT_KNOWLEDGE_CARD;
    }

    public static String resolveMasteryLevel(BigDecimal easeFactor, Integer streak) {
        return ReviewSchedulingRules.resolveMasteryLevel(easeFactor, streak);
    }

    public static String computeMasteryLevelStatic(BigDecimal easeFactor, Integer streak) {
        return ReviewSchedulingRules.resolveMasteryLevel(easeFactor, streak);
    }
}
