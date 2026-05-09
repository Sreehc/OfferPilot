package com.bytecoach.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.analytics.entity.DailyMemorySnapshot;
import com.bytecoach.analytics.mapper.DailyMemorySnapshotMapper;
import com.bytecoach.analytics.service.AnalyticsService;
import com.bytecoach.analytics.vo.EfficiencyVO;
import com.bytecoach.analytics.vo.LearningInsightsVO;
import com.bytecoach.analytics.vo.TrendVO;
import com.bytecoach.cards.entity.DailyCardTask;
import com.bytecoach.cards.entity.KnowledgeCard;
import com.bytecoach.cards.entity.KnowledgeCardLog;
import com.bytecoach.cards.mapper.DailyCardTaskMapper;
import com.bytecoach.cards.mapper.KnowledgeCardLogMapper;
import com.bytecoach.cards.mapper.KnowledgeCardMapper;
import com.bytecoach.category.entity.Category;
import com.bytecoach.category.service.CategoryService;
import com.bytecoach.interview.entity.InterviewRecord;
import com.bytecoach.interview.entity.InterviewSession;
import com.bytecoach.interview.mapper.InterviewRecordMapper;
import com.bytecoach.interview.mapper.InterviewSessionMapper;
import com.bytecoach.knowledge.entity.KnowledgeChunk;
import com.bytecoach.knowledge.entity.KnowledgeDoc;
import com.bytecoach.knowledge.mapper.KnowledgeChunkMapper;
import com.bytecoach.knowledge.mapper.KnowledgeDocMapper;
import com.bytecoach.question.entity.Question;
import com.bytecoach.question.mapper.QuestionMapper;
import com.bytecoach.wrong.entity.ReviewLog;
import com.bytecoach.wrong.entity.WrongQuestion;
import com.bytecoach.wrong.mapper.ReviewLogMapper;
import com.bytecoach.wrong.mapper.WrongQuestionMapper;
import com.bytecoach.wrong.support.ReviewSchedulingRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String SOURCE_REF_KNOWLEDGE_CHUNK = "knowledge_chunk";
    private static final String SOURCE_REF_WRONG_QUESTION = "wrong_question";
    private static final String SOURCE_REF_INTERVIEW_RECORD = "interview_record";
    private static final BigDecimal DEFAULT_EASE_FACTOR = new BigDecimal("2.50");

    private final InterviewRecordMapper recordMapper;
    private final InterviewSessionMapper sessionMapper;
    private final QuestionMapper questionMapper;
    private final CategoryService categoryService;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final KnowledgeCardMapper knowledgeCardMapper;
    private final KnowledgeCardLogMapper knowledgeCardLogMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final DailyCardTaskMapper dailyCardTaskMapper;
    private final DailyMemorySnapshotMapper dailyMemorySnapshotMapper;

    @Override
    @Transactional
    public TrendVO getAbilityTrend(Long userId, int weeks, List<Long> categoryIds) {
        int safeWeeks = weeks <= 0 ? 12 : weeks;
        ensureSnapshots(userId, safeWeeks * 7 + 7);

        List<DailyMemorySnapshot> snapshots = dailyMemorySnapshotMapper.selectList(new LambdaQueryWrapper<DailyMemorySnapshot>()
                .eq(DailyMemorySnapshot::getUserId, userId)
                .ge(DailyMemorySnapshot::getSnapshotDate, LocalDate.now().minusWeeks(safeWeeks).with(DayOfWeek.MONDAY))
                .orderByAsc(DailyMemorySnapshot::getSnapshotDate));

        List<TrendVO.MemoryTrendPoint> completionRateTrend = aggregateSnapshotTrend(
                snapshots,
                DailyMemorySnapshot::getTodayCompletionRate,
                snapshot -> snapshot.getTodayCardTotal() == null ? 0 : snapshot.getTodayCardTotal());
        List<TrendVO.MemoryTrendPoint> reviewDebtTrend = aggregateSnapshotTrend(
                snapshots,
                snapshot -> BigDecimal.valueOf(snapshot.getReviewDebtCount() == null ? 0 : snapshot.getReviewDebtCount()),
                snapshot -> snapshot.getReviewDebtCount() == null ? 0 : snapshot.getReviewDebtCount());
        List<TrendVO.MemoryTrendPoint> masteredGrowthTrend = aggregateSnapshotTrend(
                snapshots,
                snapshot -> BigDecimal.valueOf(snapshot.getMasteredCardCount() == null ? 0 : snapshot.getMasteredCardCount()),
                snapshot -> snapshot.getMasteredCardCount() == null ? 0 : snapshot.getMasteredCardCount());

        TrendData interviewTrend = buildInterviewTrend(userId, safeWeeks, categoryIds);
        List<String> weeksAxis = completionRateTrend.stream().map(TrendVO.MemoryTrendPoint::getWeek).toList();
        if (weeksAxis.isEmpty()) {
            weeksAxis = interviewTrend.weeks();
        }

        return TrendVO.builder()
                .weeks(weeksAxis)
                .completionRateTrend(completionRateTrend)
                .reviewDebtTrend(reviewDebtTrend)
                .masteredGrowthTrend(masteredGrowthTrend)
                .overallTrend(interviewTrend.overallTrend())
                .categoryTrends(interviewTrend.categoryTrends())
                .build();
    }

    @Override
    @Transactional
    public EfficiencyVO getEfficiencyData(Long userId) {
        ensureSnapshots(userId, 90);

        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));
        List<KnowledgeCard> cards = knowledgeCardMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCard>()
                        .inSql(KnowledgeCard::getTaskId,
                                "SELECT id FROM knowledge_card_task WHERE user_id = " + userId + " AND status <> 'invalid'"));
        List<ReviewLog> allLogs = reviewLogMapper.selectList(
                new LambdaQueryWrapper<ReviewLog>()
                        .eq(ReviewLog::getUserId, userId)
                        .orderByAsc(ReviewLog::getCreateTime));
        List<KnowledgeCardLog> allCardLogs = knowledgeCardLogMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCardLog>()
                        .eq(KnowledgeCardLog::getUserId, userId)
                        .orderByAsc(KnowledgeCardLog::getCreateTime));
        List<DailyMemorySnapshot> snapshots = dailyMemorySnapshotMapper.selectList(new LambdaQueryWrapper<DailyMemorySnapshot>()
                .eq(DailyMemorySnapshot::getUserId, userId)
                .ge(DailyMemorySnapshot::getSnapshotDate, LocalDate.now().minusDays(90))
                .orderByAsc(DailyMemorySnapshot::getSnapshotDate));

        Map<String, Long> masteryDist = buildMasteryDistribution(wrongs, cards);
        BigDecimal avgEaseFactor = resolveAvgEaseFactor(wrongs, cards);
        Map<Integer, Long> ratingDist = buildRatingDistribution(allLogs, allCardLogs);
        MergedLogs mergedLogs = mergeLogs(userId, allLogs, allCardLogs);

        List<EfficiencyVO.WeeklyEF> efTrend = buildEfTrend(mergedLogs.logs());
        List<EfficiencyVO.WeeklyForgettingRate> frTrend = buildForgettingRateTrend(mergedLogs.logs());
        List<EfficiencyVO.CompletionRatePoint> completionRateTrend = snapshots.stream()
                .map(snapshot -> EfficiencyVO.CompletionRatePoint.builder()
                        .label(snapshot.getSnapshotDate().toString())
                        .completionRate(defaultDecimal(snapshot.getTodayCompletionRate()))
                        .plannedCount(snapshot.getTodayCardTotal() == null ? 0 : snapshot.getTodayCardTotal())
                        .completedCount(snapshot.getTodayCompletedCards() == null ? 0 : snapshot.getTodayCompletedCards())
                        .build())
                .toList();
        List<EfficiencyVO.DebtTrendPoint> reviewDebtTrend = snapshots.stream()
                .map(snapshot -> EfficiencyVO.DebtTrendPoint.builder()
                        .label(snapshot.getSnapshotDate().toString())
                        .reviewDebtCount(snapshot.getReviewDebtCount() == null ? 0 : snapshot.getReviewDebtCount())
                        .build())
                .toList();
        List<EfficiencyVO.MasteredGrowthPoint> masteredGrowthTrend = snapshots.stream()
                .map(snapshot -> EfficiencyVO.MasteredGrowthPoint.builder()
                        .label(snapshot.getSnapshotDate().toString())
                        .masteredCardCount(snapshot.getMasteredCardCount() == null ? 0 : snapshot.getMasteredCardCount())
                        .build())
                .toList();
        Map<String, Long> contentTypeDistribution = mergedLogs.logs().stream()
                .collect(Collectors.groupingBy(MergedLogPoint::contentType, Collectors.counting()));
        List<EfficiencyVO.CategoryMastery> categoryMastery = buildCategoryMastery(userId, cards);

        BigDecimal reviewCompletionRate = snapshots.isEmpty()
                ? BigDecimal.ZERO
                : defaultDecimal(snapshots.get(snapshots.size() - 1).getTodayCompletionRate());
        BigDecimal forgettingRate = frTrend.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(frTrend.get(frTrend.size() - 1).getForgettingRate() * 100).setScale(2, RoundingMode.HALF_UP);

        return EfficiencyVO.builder()
                .avgEaseFactor(avgEaseFactor)
                .efTrend(efTrend)
                .ratingDistribution(ratingDist)
                .forgettingRateTrend(frTrend)
                .completionRateTrend(completionRateTrend)
                .reviewDebtTrend(reviewDebtTrend)
                .masteredGrowthTrend(masteredGrowthTrend)
                .masteryDistribution(masteryDist)
                .contentTypeDistribution(contentTypeDistribution)
                .categoryMastery(categoryMastery)
                .totalReviews(mergedLogs.logs().size())
                .currentStreak(mergedLogs.currentStreak())
                .reviewCompletionRate(reviewCompletionRate)
                .forgettingRate(forgettingRate)
                .build();
    }

    @Override
    @Transactional
    public LearningInsightsVO getLearningInsights(Long userId) {
        ensureSnapshots(userId, 14);

        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(DayOfWeek.MONDAY);
        LocalDate lastMonday = thisMonday.minusWeeks(1);

        List<InterviewSession> recentSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "finished")
                        .isNotNull(InterviewSession::getTotalScore)
                        .ge(InterviewSession::getCreateTime, lastMonday.atStartOfDay())
                        .orderByAsc(InterviewSession::getCreateTime));

        BigDecimal thisWeekAvg = BigDecimal.ZERO;
        BigDecimal lastWeekAvg = BigDecimal.ZERO;
        int thisWeekCount = 0;
        int lastWeekCount = 0;

        for (InterviewSession session : recentSessions) {
            if (session.getStartTime() == null || session.getTotalScore() == null) continue;
            LocalDate sessionDate = session.getStartTime().toLocalDate();
            if (!sessionDate.isBefore(thisMonday)) {
                thisWeekAvg = thisWeekAvg.add(session.getTotalScore());
                thisWeekCount++;
            } else if (!sessionDate.isBefore(lastMonday)) {
                lastWeekAvg = lastWeekAvg.add(session.getTotalScore());
                lastWeekCount++;
            }
        }

        BigDecimal thisAvg = thisWeekCount > 0
                ? thisWeekAvg.divide(BigDecimal.valueOf(thisWeekCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal lastAvg = lastWeekCount > 0
                ? lastWeekAvg.divide(BigDecimal.valueOf(lastWeekCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<LearningInsightsVO.CategoryChange> categoryChanges = buildCategoryChanges(recentSessions, thisMonday);
        List<LearningInsightsVO.HourDistribution> bestStudyHours = analyzeBestHours(userId);
        List<DailyMemorySnapshot> snapshots = dailyMemorySnapshotMapper.selectList(new LambdaQueryWrapper<DailyMemorySnapshot>()
                .eq(DailyMemorySnapshot::getUserId, userId)
                .ge(DailyMemorySnapshot::getSnapshotDate, today.minusDays(7))
                .orderByAsc(DailyMemorySnapshot::getSnapshotDate));

        DailyMemorySnapshot latest = snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1);
        DailyMemorySnapshot previous = snapshots.size() >= 2 ? snapshots.get(snapshots.size() - 2) : null;

        return LearningInsightsVO.builder()
                .thisWeekAvgScore(thisAvg)
                .lastWeekAvgScore(lastAvg)
                .thisWeekInterviewCount(thisWeekCount)
                .lastWeekInterviewCount(lastWeekCount)
                .todayCompletionStatus(resolveTodayCompletionStatus(latest))
                .reviewDebtStatus(resolveReviewDebtStatus(latest, previous))
                .masteryGrowthStatus(resolveMasteryGrowthStatus(latest, previous))
                .categoryChanges(categoryChanges)
                .bestStudyHours(bestStudyHours)
                .build();
    }

    private TrendData buildInterviewTrend(Long userId, int weeks, List<Long> categoryIds) {
        LocalDate startDate = LocalDate.now().minusWeeks(weeks).with(DayOfWeek.MONDAY);
        List<InterviewRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<InterviewRecord>()
                        .eq(InterviewRecord::getUserId, userId)
                        .isNotNull(InterviewRecord::getScore)
                        .ge(InterviewRecord::getCreateTime, startDate.atStartOfDay()));
        if (records.isEmpty()) {
            return new TrendData(List.of(), List.of(), List.of());
        }

        Set<Long> sessionIds = records.stream().map(InterviewRecord::getSessionId).collect(Collectors.toSet());
        Map<Long, InterviewSession> sessionMap = sessionMapper.selectBatchIds(sessionIds)
                .stream()
                .collect(Collectors.toMap(InterviewSession::getId, Function.identity(), (a, b) -> a));
        Set<Long> questionIds = records.stream().map(InterviewRecord::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        Set<Long> filterCategoryIds = categoryIds == null || categoryIds.isEmpty() ? null : Set.copyOf(categoryIds);

        Map<String, List<InterviewRecord>> byWeek = new LinkedHashMap<>();
        for (InterviewRecord record : records) {
            InterviewSession session = sessionMap.get(record.getSessionId());
            if (session == null || session.getStartTime() == null) continue;
            String weekKey = formatWeek(session.getStartTime().toLocalDate());
            byWeek.computeIfAbsent(weekKey, key -> new ArrayList<>()).add(record);
        }

        List<TrendVO.WeeklyPoint> overallTrend = new ArrayList<>();
        for (Map.Entry<String, List<InterviewRecord>> entry : byWeek.entrySet()) {
            List<BigDecimal> scores = entry.getValue().stream()
                    .map(InterviewRecord::getScore)
                    .filter(Objects::nonNull)
                    .toList();
            overallTrend.add(TrendVO.WeeklyPoint.builder()
                    .week(entry.getKey())
                    .score(avg(scores))
                    .count(scores.size())
                    .build());
        }

        Map<Long, String> categoryNameMap = new HashMap<>();
        Set<Long> allCategoryIds = records.stream()
                .map(record -> questionMap.get(record.getQuestionId()))
                .filter(Objects::nonNull)
                .map(Question::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!allCategoryIds.isEmpty()) {
            categoryService.listByIds(allCategoryIds).forEach(category -> categoryNameMap.put(category.getId(), category.getName()));
        }

        Map<Long, Map<String, List<BigDecimal>>> categoryWeekScores = new HashMap<>();
        for (InterviewRecord record : records) {
            InterviewSession session = sessionMap.get(record.getSessionId());
            Question question = questionMap.get(record.getQuestionId());
            if (session == null || question == null || question.getCategoryId() == null) continue;
            if (filterCategoryIds != null && !filterCategoryIds.contains(question.getCategoryId())) continue;
            String weekKey = formatWeek(session.getStartTime().toLocalDate());
            categoryWeekScores
                    .computeIfAbsent(question.getCategoryId(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(weekKey, key -> new ArrayList<>())
                    .add(record.getScore());
        }

        List<TrendVO.CategoryTrend> categoryTrends = new ArrayList<>();
        for (Map.Entry<Long, Map<String, List<BigDecimal>>> entry : categoryWeekScores.entrySet()) {
            List<TrendVO.WeeklyPoint> points = entry.getValue().entrySet().stream()
                    .map(weekEntry -> TrendVO.WeeklyPoint.builder()
                            .week(weekEntry.getKey())
                            .score(avg(weekEntry.getValue()))
                            .count(weekEntry.getValue().size())
                            .build())
                    .toList();
            categoryTrends.add(TrendVO.CategoryTrend.builder()
                    .categoryId(entry.getKey())
                    .categoryName(categoryNameMap.getOrDefault(entry.getKey(), "未分类"))
                    .points(points)
                    .build());
        }

        List<String> allWeeks = new ArrayList<>(byWeek.keySet());
        allWeeks.sort(String::compareTo);
        return new TrendData(allWeeks, overallTrend, categoryTrends);
    }

    private Map<String, Long> buildMasteryDistribution(List<WrongQuestion> wrongs, List<KnowledgeCard> cards) {
        Map<String, Long> masteryDistribution = new HashMap<>();
        wrongs.forEach(wrong -> masteryDistribution.merge(
                wrong.getMasteryLevel() != null ? wrong.getMasteryLevel() : "not_started",
                1L,
                Long::sum));
        cards.forEach(card -> masteryDistribution.merge(normalizeKnowledgeMastery(card), 1L, Long::sum));
        return masteryDistribution;
    }

    private BigDecimal resolveAvgEaseFactor(List<WrongQuestion> wrongs, List<KnowledgeCard> cards) {
        BigDecimal wrongEfSum = wrongs.stream()
                .map(WrongQuestion::getEaseFactor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cardEfSum = cards.stream()
                .map(KnowledgeCard::getEaseFactor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long efCount = wrongs.stream().filter(wrong -> wrong.getEaseFactor() != null).count()
                + cards.stream().filter(card -> card.getEaseFactor() != null).count();
        return efCount > 0
                ? wrongEfSum.add(cardEfSum).divide(BigDecimal.valueOf(efCount), 2, RoundingMode.HALF_UP)
                : DEFAULT_EASE_FACTOR;
    }

    private Map<Integer, Long> buildRatingDistribution(List<ReviewLog> reviewLogs, List<KnowledgeCardLog> cardLogs) {
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        reviewLogs.forEach(logItem -> ratingDistribution.merge(logItem.getRating(), 1L, Long::sum));
        cardLogs.forEach(logItem -> ratingDistribution.merge(logItem.getRating(), 1L, Long::sum));
        return ratingDistribution;
    }

    private MergedLogs mergeLogs(Long userId, List<ReviewLog> reviewLogs, List<KnowledgeCardLog> cardLogs) {
        List<MergedLogPoint> mergedLogs = new ArrayList<>();
        reviewLogs.forEach(logItem -> mergedLogs.add(new MergedLogPoint(
                formatWeek(logItem.getCreateTime().toLocalDate()),
                logItem.getEaseFactorAfter(),
                logItem.getRating(),
                resolveWrongContentType(userId, logItem.getWrongQuestionId()),
                logItem.getCreateTime())));
        cardLogs.forEach(logItem -> mergedLogs.add(new MergedLogPoint(
                formatWeek(logItem.getCreateTime().toLocalDate()),
                logItem.getEaseFactorAfter(),
                logItem.getRating(),
                "knowledge_card",
                logItem.getCreateTime())));
        mergedLogs.sort(Comparator.comparing(MergedLogPoint::createTime));

        int streak = 0;
        LocalDate cursor = LocalDate.now();
        Set<LocalDate> reviewDays = mergedLogs.stream().map(log -> log.createTime().toLocalDate()).collect(Collectors.toSet());
        if (!reviewDays.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        while (reviewDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return new MergedLogs(mergedLogs, streak);
    }

    private List<EfficiencyVO.WeeklyEF> buildEfTrend(List<MergedLogPoint> mergedLogs) {
        Map<String, List<MergedLogPoint>> logsByWeek = mergedLogs.stream()
                .collect(Collectors.groupingBy(MergedLogPoint::week, LinkedHashMap::new, Collectors.toList()));
        List<EfficiencyVO.WeeklyEF> result = new ArrayList<>();
        for (Map.Entry<String, List<MergedLogPoint>> entry : logsByWeek.entrySet()) {
            BigDecimal lastEf = entry.getValue().stream()
                    .map(MergedLogPoint::easeFactorAfter)
                    .filter(Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse(DEFAULT_EASE_FACTOR);
            result.add(EfficiencyVO.WeeklyEF.builder()
                    .week(entry.getKey())
                    .avgEF(lastEf)
                    .reviewCount(entry.getValue().size())
                    .build());
        }
        return result;
    }

    private List<EfficiencyVO.WeeklyForgettingRate> buildForgettingRateTrend(List<MergedLogPoint> mergedLogs) {
        Map<String, List<MergedLogPoint>> logsByWeek = mergedLogs.stream()
                .collect(Collectors.groupingBy(MergedLogPoint::week, LinkedHashMap::new, Collectors.toList()));
        List<EfficiencyVO.WeeklyForgettingRate> result = new ArrayList<>();
        for (Map.Entry<String, List<MergedLogPoint>> entry : logsByWeek.entrySet()) {
            long againCount = entry.getValue().stream().filter(log -> log.rating() == 1).count();
            double forgettingRate = entry.getValue().isEmpty() ? 0.0 : (double) againCount / entry.getValue().size();
            result.add(EfficiencyVO.WeeklyForgettingRate.builder()
                    .week(entry.getKey())
                    .forgettingRate(forgettingRate)
                    .totalRatings(entry.getValue().size())
                    .againCount((int) againCount)
                    .build());
        }
        return result;
    }

    private List<EfficiencyVO.CategoryMastery> buildCategoryMastery(Long userId, List<KnowledgeCard> cards) {
        if (cards.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> categoryByCardId = resolveCardCategories(userId, cards);
        Set<Long> categoryIds = categoryByCardId.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categoryNames = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryService.listByIds(categoryIds).forEach(category -> categoryNames.put(category.getId(), category.getName()));
        }

        Map<Long, List<KnowledgeCard>> cardsByCategory = new LinkedHashMap<>();
        for (KnowledgeCard card : cards) {
            Long categoryId = categoryByCardId.get(card.getId());
            cardsByCategory.computeIfAbsent(categoryId == null ? -1L : categoryId, key -> new ArrayList<>()).add(card);
        }

        return cardsByCategory.entrySet().stream()
                .map(entry -> {
                    List<KnowledgeCard> items = entry.getValue();
                    int totalCards = items.size();
                    int masteredCards = (int) items.stream().filter(card -> "mastered".equals(card.getState())).count();
                    int dueCount = (int) items.stream()
                            .filter(card -> !"mastered".equals(card.getState()))
                            .filter(card -> card.getNextReviewAt() == null || !card.getNextReviewAt().isAfter(LocalDateTime.now()))
                            .count();
                    BigDecimal masteryRate = totalCards <= 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(masteredCards * 100.0 / totalCards).setScale(2, RoundingMode.HALF_UP);
                    return EfficiencyVO.CategoryMastery.builder()
                            .categoryId(entry.getKey() < 0 ? null : entry.getKey())
                            .categoryName(entry.getKey() < 0 ? "未分类" : categoryNames.getOrDefault(entry.getKey(), "未分类"))
                            .totalCards(totalCards)
                            .masteredCards(masteredCards)
                            .dueCount(dueCount)
                            .masteryRate(masteryRate)
                            .build();
                })
                .sorted(Comparator.comparing(EfficiencyVO.CategoryMastery::getDueCount).reversed()
                        .thenComparing(EfficiencyVO.CategoryMastery::getMasteryRate))
                .toList();
    }

    private Map<Long, Long> resolveCardCategories(Long userId, List<KnowledgeCard> cards) {
        Map<Long, Long> result = new HashMap<>();
        Set<Long> chunkIds = cards.stream()
                .filter(card -> SOURCE_REF_KNOWLEDGE_CHUNK.equals(card.getSourceRefType()) && card.getSourceRefId() != null)
                .map(KnowledgeCard::getSourceRefId)
                .collect(Collectors.toSet());
        Set<Long> wrongIds = cards.stream()
                .filter(card -> SOURCE_REF_WRONG_QUESTION.equals(card.getSourceRefType()) && card.getSourceRefId() != null)
                .map(KnowledgeCard::getSourceRefId)
                .collect(Collectors.toSet());
        Set<Long> interviewIds = cards.stream()
                .filter(card -> SOURCE_REF_INTERVIEW_RECORD.equals(card.getSourceRefType()) && card.getSourceRefId() != null)
                .map(KnowledgeCard::getSourceRefId)
                .collect(Collectors.toSet());

        Map<Long, KnowledgeChunk> chunkMap = chunkIds.isEmpty() ? Map.of() : knowledgeChunkMapper.selectBatchIds(chunkIds).stream()
                .collect(Collectors.toMap(KnowledgeChunk::getId, Function.identity(), (a, b) -> a));
        Set<Long> docIds = chunkMap.values().stream().map(KnowledgeChunk::getDocId).collect(Collectors.toSet());
        Map<Long, KnowledgeDoc> docMap = docIds.isEmpty() ? Map.of() : knowledgeDocMapper.selectBatchIds(docIds).stream()
                .collect(Collectors.toMap(KnowledgeDoc::getId, Function.identity(), (a, b) -> a));
        Map<Long, WrongQuestion> wrongMap = wrongIds.isEmpty() ? Map.of() : wrongQuestionMapper.selectBatchIds(wrongIds).stream()
                .filter(wrong -> userId.equals(wrong.getUserId()))
                .collect(Collectors.toMap(WrongQuestion::getId, Function.identity(), (a, b) -> a));
        Map<Long, InterviewRecord> interviewMap = interviewIds.isEmpty() ? Map.of() : recordMapper.selectBatchIds(interviewIds).stream()
                .filter(record -> userId.equals(record.getUserId()))
                .collect(Collectors.toMap(InterviewRecord::getId, Function.identity(), (a, b) -> a));

        Set<Long> questionIds = new java.util.HashSet<>();
        wrongMap.values().forEach(wrong -> questionIds.add(wrong.getQuestionId()));
        interviewMap.values().forEach(record -> questionIds.add(record.getQuestionId()));
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Map.of() : questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        for (KnowledgeCard card : cards) {
            Long categoryId = null;
            if (SOURCE_REF_KNOWLEDGE_CHUNK.equals(card.getSourceRefType()) && card.getSourceRefId() != null) {
                KnowledgeChunk chunk = chunkMap.get(card.getSourceRefId());
                KnowledgeDoc doc = chunk == null ? null : docMap.get(chunk.getDocId());
                categoryId = doc == null ? null : doc.getCategoryId();
            } else if (SOURCE_REF_WRONG_QUESTION.equals(card.getSourceRefType()) && card.getSourceRefId() != null) {
                WrongQuestion wrong = wrongMap.get(card.getSourceRefId());
                Question question = wrong == null ? null : questionMap.get(wrong.getQuestionId());
                categoryId = question == null ? null : question.getCategoryId();
            } else if (SOURCE_REF_INTERVIEW_RECORD.equals(card.getSourceRefType()) && card.getSourceRefId() != null) {
                InterviewRecord record = interviewMap.get(card.getSourceRefId());
                Question question = record == null ? null : questionMap.get(record.getQuestionId());
                categoryId = question == null ? null : question.getCategoryId();
            }
            result.put(card.getId(), categoryId);
        }
        return result;
    }

    private List<LearningInsightsVO.CategoryChange> buildCategoryChanges(List<InterviewSession> recentSessions, LocalDate thisMonday) {
        Set<Long> sessionIds = recentSessions.stream().map(InterviewSession::getId).collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            return List.of();
        }

        List<InterviewRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<InterviewRecord>()
                        .in(InterviewRecord::getSessionId, sessionIds)
                        .isNotNull(InterviewRecord::getScore));
        Map<Long, InterviewSession> sessionMap = recentSessions.stream()
                .collect(Collectors.toMap(InterviewSession::getId, Function.identity(), (a, b) -> a));
        Set<Long> questionIds = records.stream().map(InterviewRecord::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Map.of() : questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        Set<Long> categoryIds = questionMap.values().stream().map(Question::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categoryNames = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryService.listByIds(categoryIds).forEach(category -> categoryNames.put(category.getId(), category.getName()));
        }

        Map<Long, Map<String, BigDecimal>> scoresByCategory = new HashMap<>();
        for (InterviewRecord record : records) {
            InterviewSession session = sessionMap.get(record.getSessionId());
            Question question = questionMap.get(record.getQuestionId());
            if (session == null || question == null || question.getCategoryId() == null || session.getStartTime() == null) continue;
            String period = session.getStartTime().toLocalDate().isBefore(thisMonday) ? "last" : "this";
            scoresByCategory.computeIfAbsent(question.getCategoryId(), key -> new HashMap<>()).merge(period, record.getScore(), BigDecimal::add);
            scoresByCategory.computeIfAbsent(question.getCategoryId(), key -> new HashMap<>()).merge(period + "_count", BigDecimal.ONE, BigDecimal::add);
        }

        return scoresByCategory.entrySet().stream()
                .map(entry -> {
                    BigDecimal thisScore = entry.getValue().getOrDefault("this", BigDecimal.ZERO);
                    BigDecimal thisCount = entry.getValue().getOrDefault("this_count", BigDecimal.ZERO);
                    BigDecimal lastScore = entry.getValue().getOrDefault("last", BigDecimal.ZERO);
                    BigDecimal lastCount = entry.getValue().getOrDefault("last_count", BigDecimal.ZERO);
                    BigDecimal thisWeekScore = thisCount.compareTo(BigDecimal.ZERO) > 0
                            ? thisScore.divide(thisCount, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal lastWeekScore = lastCount.compareTo(BigDecimal.ZERO) > 0
                            ? lastScore.divide(lastCount, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return LearningInsightsVO.CategoryChange.builder()
                            .categoryId(entry.getKey())
                            .categoryName(categoryNames.getOrDefault(entry.getKey(), "未分类"))
                            .thisWeekScore(thisWeekScore)
                            .lastWeekScore(lastWeekScore)
                            .change(thisWeekScore.subtract(lastWeekScore))
                            .build();
                })
                .sorted(Comparator.comparing((LearningInsightsVO.CategoryChange change) -> change.getChange().abs()).reversed())
                .toList();
    }

    private List<LearningInsightsVO.HourDistribution> analyzeBestHours(Long userId) {
        List<InterviewSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "finished")
                        .select(InterviewSession::getStartTime, InterviewSession::getTotalScore)
                        .isNotNull(InterviewSession::getStartTime)
                        .isNotNull(InterviewSession::getTotalScore)
                        .last("LIMIT 200"));
        if (sessions.isEmpty()) {
            return List.of();
        }

        Map<String, List<BigDecimal>> buckets = new LinkedHashMap<>();
        buckets.put("上午 (6-12)", new ArrayList<>());
        buckets.put("下午 (12-18)", new ArrayList<>());
        buckets.put("晚上 (18-24)", new ArrayList<>());
        buckets.put("凌晨 (0-6)", new ArrayList<>());
        for (InterviewSession session : sessions) {
            int hour = session.getStartTime().getHour();
            String bucket = hour >= 6 && hour < 12 ? "上午 (6-12)"
                    : hour < 18 ? "下午 (12-18)"
                    : hour < 24 ? "晚上 (18-24)"
                    : "凌晨 (0-6)";
            buckets.get(bucket).add(session.getTotalScore());
        }

        return buckets.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> LearningInsightsVO.HourDistribution.builder()
                        .timeSlot(entry.getKey())
                        .sessionCount(entry.getValue().size())
                        .avgScore(avg(entry.getValue()))
                        .build())
                .sorted(Comparator.comparing(LearningInsightsVO.HourDistribution::getAvgScore).reversed())
                .toList();
    }

    private List<TrendVO.MemoryTrendPoint> aggregateSnapshotTrend(
            List<DailyMemorySnapshot> snapshots,
            Function<DailyMemorySnapshot, BigDecimal> valueExtractor,
            Function<DailyMemorySnapshot, Integer> countExtractor) {
        Map<String, List<DailyMemorySnapshot>> snapshotsByWeek = snapshots.stream()
                .collect(Collectors.groupingBy(snapshot -> formatWeek(snapshot.getSnapshotDate()), LinkedHashMap::new, Collectors.toList()));
        List<TrendVO.MemoryTrendPoint> result = new ArrayList<>();
        for (Map.Entry<String, List<DailyMemorySnapshot>> entry : snapshotsByWeek.entrySet()) {
            List<DailyMemorySnapshot> weekSnapshots = entry.getValue();
            BigDecimal avgValue = avg(weekSnapshots.stream().map(valueExtractor).filter(Objects::nonNull).toList());
            int count = weekSnapshots.stream().map(countExtractor).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            result.add(TrendVO.MemoryTrendPoint.builder()
                    .week(entry.getKey())
                    .value(avgValue)
                    .count(count)
                    .build());
        }
        return result;
    }

    private String resolveTodayCompletionStatus(DailyMemorySnapshot latest) {
        if (latest == null || latest.getTodayCardTotal() == null || latest.getTodayCardTotal() <= 0) {
            return "今天还没有卡片任务";
        }
        if (latest.getTodayCompletedCards() != null && latest.getTodayCompletedCards() >= latest.getTodayCardTotal()) {
            return "今日记忆任务已完成";
        }
        return "今天还有卡片待完成";
    }

    private String resolveReviewDebtStatus(DailyMemorySnapshot latest, DailyMemorySnapshot previous) {
        int current = latest == null || latest.getReviewDebtCount() == null ? 0 : latest.getReviewDebtCount();
        int previousValue = previous == null || previous.getReviewDebtCount() == null ? current : previous.getReviewDebtCount();
        if (current <= 0) {
            return "当前没有复习积压";
        }
        if (current > previousValue) {
            return "复习负债正在上升";
        }
        if (current < previousValue) {
            return "复习负债正在下降";
        }
        return "复习负债保持稳定";
    }

    private String resolveMasteryGrowthStatus(DailyMemorySnapshot latest, DailyMemorySnapshot previous) {
        int current = latest == null || latest.getMasteredCardCount() == null ? 0 : latest.getMasteredCardCount();
        int previousValue = previous == null || previous.getMasteredCardCount() == null ? current : previous.getMasteredCardCount();
        if (current > previousValue) {
            return "掌握卡片正在增长";
        }
        if (current < previousValue) {
            return "掌握卡片出现回退";
        }
        return current > 0 ? "掌握卡片保持稳定" : "还没有形成掌握增长";
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatWeek(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    private BigDecimal avg(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private String normalizeKnowledgeMastery(KnowledgeCard card) {
        if ("mastered".equals(card.getState())) return "mastered";
        if ("learning".equals(card.getState())) return "reviewing";
        if ("weak".equals(card.getState())) return "not_started";
        return ReviewSchedulingRules.resolveMasteryLevel(card.getEaseFactor(), card.getStreak());
    }

    private String resolveWrongContentType(Long userId, Long wrongQuestionId) {
        WrongQuestion wrongQuestion = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wrongQuestion == null || !userId.equals(wrongQuestion.getUserId())) {
            return "wrong_card";
        }
        return "interview".equalsIgnoreCase(wrongQuestion.getSourceType()) ? "interview_card" : "wrong_card";
    }

    private void ensureSnapshots(Long userId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(Math.max(days, 1) - 1L);
        List<DailyMemorySnapshot> existing = dailyMemorySnapshotMapper.selectList(new LambdaQueryWrapper<DailyMemorySnapshot>()
                .eq(DailyMemorySnapshot::getUserId, userId)
                .ge(DailyMemorySnapshot::getSnapshotDate, startDate)
                .orderByAsc(DailyMemorySnapshot::getSnapshotDate));
        Map<LocalDate, DailyMemorySnapshot> existingByDate = existing.stream()
                .collect(Collectors.toMap(DailyMemorySnapshot::getSnapshotDate, Function.identity(), (a, b) -> a));

        List<DailyCardTask> cardTasks = dailyCardTaskMapper.selectList(new LambdaQueryWrapper<DailyCardTask>()
                .eq(DailyCardTask::getUserId, userId)
                .ge(DailyCardTask::getTaskDate, startDate)
                .orderByAsc(DailyCardTask::getTaskDate));
        Map<LocalDate, List<DailyCardTask>> cardTasksByDate = cardTasks.stream()
                .collect(Collectors.groupingBy(DailyCardTask::getTaskDate, LinkedHashMap::new, Collectors.toList()));

        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId));
        List<KnowledgeCard> cards = knowledgeCardMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCard>()
                        .inSql(KnowledgeCard::getTaskId,
                                "SELECT id FROM knowledge_card_task WHERE user_id = " + userId + " AND status <> 'invalid'"));
        List<ReviewLog> reviewLogs = reviewLogMapper.selectList(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getUserId, userId)
                .ge(ReviewLog::getCreateTime, startDate.atStartOfDay()));
        List<KnowledgeCardLog> cardLogs = knowledgeCardLogMapper.selectList(new LambdaQueryWrapper<KnowledgeCardLog>()
                .eq(KnowledgeCardLog::getUserId, userId)
                .ge(KnowledgeCardLog::getCreateTime, startDate.atStartOfDay()));

        Map<LocalDate, Integer> reviewsPerDate = new HashMap<>();
        reviewLogs.forEach(log -> reviewsPerDate.merge(log.getCreateTime().toLocalDate(), 1, Integer::sum));
        cardLogs.forEach(log -> reviewsPerDate.merge(log.getCreateTime().toLocalDate(), 1, Integer::sum));

        int baseMastered = (int) cards.stream().filter(card -> "mastered".equals(card.getState())).count();
        for (int offset = 0; offset < days; offset++) {
            LocalDate date = startDate.plusDays(offset);
            DailyMemorySnapshot snapshot = existingByDate.get(date);
            List<DailyCardTask> taskSnapshots = cardTasksByDate.getOrDefault(date, List.of());
            int todayCardTotal = taskSnapshots.stream().map(DailyCardTask::getPlannedCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            int todayCompletedCards = taskSnapshots.stream().map(DailyCardTask::getCompletedCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            int dueTodayCount = taskSnapshots.stream().map(DailyCardTask::getReviewCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            int reviewedTodayCount = reviewsPerDate.getOrDefault(date, 0);
            BigDecimal todayCompletionRate = todayCardTotal <= 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(todayCompletedCards * 100.0 / todayCardTotal).setScale(2, RoundingMode.HALF_UP);
            int reviewDebtCount = countDebtAtDate(wrongs, cards, date);
            int masteredCount = countMasteredByDate(cards, date, baseMastered);
            int studyStreak = resolveStudyStreakToDate(reviewLogs, cardLogs, date);

            if (snapshot == null) {
                snapshot = new DailyMemorySnapshot();
                snapshot.setUserId(userId);
                snapshot.setSnapshotDate(date);
            }
            snapshot.setTodayCardTotal(todayCardTotal);
            snapshot.setTodayCompletedCards(todayCompletedCards);
            snapshot.setTodayCompletionRate(todayCompletionRate);
            snapshot.setReviewDebtCount(reviewDebtCount);
            snapshot.setMasteredCardCount(masteredCount);
            snapshot.setDueTodayCount(dueTodayCount);
            snapshot.setReviewedTodayCount(reviewedTodayCount);
            snapshot.setStudyStreak(studyStreak);
            if (snapshot.getId() == null) {
                dailyMemorySnapshotMapper.insert(snapshot);
            } else {
                dailyMemorySnapshotMapper.updateById(snapshot);
            }
        }
    }

    private int countDebtAtDate(List<WrongQuestion> wrongs, List<KnowledgeCard> cards, LocalDate date) {
        int wrongDebt = (int) wrongs.stream()
                .filter(wrong -> wrong.getNextReviewDate() != null && wrong.getNextReviewDate().isBefore(date))
                .count();
        int cardDebt = (int) cards.stream()
                .filter(card -> !"mastered".equals(card.getState()))
                .filter(card -> card.getNextReviewAt() != null && card.getNextReviewAt().toLocalDate().isBefore(date))
                .count();
        return wrongDebt + cardDebt;
    }

    private int countMasteredByDate(List<KnowledgeCard> cards, LocalDate date, int fallbackCurrent) {
        int derived = (int) cards.stream()
                .filter(card -> "mastered".equals(card.getState()))
                .filter(card -> card.getLastReviewTime() == null || !card.getLastReviewTime().toLocalDate().isAfter(date))
                .count();
        return Math.max(derived, fallbackCurrent == 0 ? derived : Math.min(derived == 0 ? fallbackCurrent : derived, fallbackCurrent));
    }

    private int resolveStudyStreakToDate(List<ReviewLog> reviewLogs, List<KnowledgeCardLog> cardLogs, LocalDate date) {
        Set<LocalDate> reviewDates = new java.util.HashSet<>();
        reviewLogs.stream()
                .map(ReviewLog::getCreateTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .filter(logDate -> !logDate.isAfter(date))
                .forEach(reviewDates::add);
        cardLogs.stream()
                .map(KnowledgeCardLog::getCreateTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .filter(logDate -> !logDate.isAfter(date))
                .forEach(reviewDates::add);

        int streak = 0;
        LocalDate cursor = date;
        while (reviewDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private record TrendData(
            List<String> weeks,
            List<TrendVO.WeeklyPoint> overallTrend,
            List<TrendVO.CategoryTrend> categoryTrends) {
    }

    private record MergedLogPoint(
            String week,
            BigDecimal easeFactorAfter,
            Integer rating,
            String contentType,
            LocalDateTime createTime) {
    }

    private record MergedLogs(
            List<MergedLogPoint> logs,
            int currentStreak) {
    }
}
