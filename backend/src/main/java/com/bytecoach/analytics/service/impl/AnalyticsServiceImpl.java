package com.bytecoach.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.cards.entity.KnowledgeCard;
import com.bytecoach.cards.entity.KnowledgeCardLog;
import com.bytecoach.cards.mapper.KnowledgeCardLogMapper;
import com.bytecoach.cards.mapper.KnowledgeCardMapper;
import com.bytecoach.analytics.service.AnalyticsService;
import com.bytecoach.analytics.vo.EfficiencyVO;
import com.bytecoach.analytics.vo.LearningInsightsVO;
import com.bytecoach.analytics.vo.TrendVO;
import com.bytecoach.category.entity.Category;
import com.bytecoach.category.service.CategoryService;
import com.bytecoach.interview.entity.InterviewRecord;
import com.bytecoach.interview.entity.InterviewSession;
import com.bytecoach.interview.mapper.InterviewRecordMapper;
import com.bytecoach.interview.mapper.InterviewSessionMapper;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final InterviewRecordMapper recordMapper;
    private final InterviewSessionMapper sessionMapper;
    private final QuestionMapper questionMapper;
    private final CategoryService categoryService;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final KnowledgeCardMapper knowledgeCardMapper;
    private final KnowledgeCardLogMapper knowledgeCardLogMapper;

    @Override
    public TrendVO getAbilityTrend(Long userId, int weeks, List<Long> categoryIds) {
        if (weeks <= 0) weeks = 12;
        LocalDate startDate = LocalDate.now().minusWeeks(weeks).with(DayOfWeek.MONDAY);

        // Load scored interview records with session dates
        List<InterviewRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<InterviewRecord>()
                        .eq(InterviewRecord::getUserId, userId)
                        .isNotNull(InterviewRecord::getScore)
                        .ge(InterviewRecord::getCreateTime, startDate.atStartOfDay()));

        if (records.isEmpty()) {
            return TrendVO.builder()
                    .weeks(List.of())
                    .overallTrend(List.of())
                    .categoryTrends(List.of())
                    .build();
        }

        // Load sessions for date mapping
        Set<Long> sessionIds = records.stream()
                .map(InterviewRecord::getSessionId)
                .collect(Collectors.toSet());
        Map<Long, InterviewSession> sessionMap = sessionMapper.selectBatchIds(sessionIds)
                .stream()
                .collect(Collectors.toMap(InterviewSession::getId, Function.identity(), (a, b) -> a));

        // Load question → category mapping
        Set<Long> questionIds = records.stream()
                .map(InterviewRecord::getQuestionId)
                .collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        // Determine category filter
        Set<Long> filterCategoryIds = (categoryIds != null && !categoryIds.isEmpty())
                ? Set.copyOf(categoryIds) : null;

        // Group records by week
        Map<String, List<InterviewRecord>> byWeek = new LinkedHashMap<>();
        for (InterviewRecord r : records) {
            InterviewSession s = sessionMap.get(r.getSessionId());
            if (s == null || s.getStartTime() == null) continue;
            String weekKey = formatWeek(s.getStartTime().toLocalDate());
            byWeek.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(r);
        }

        // Compute overall trend
        List<TrendVO.WeeklyPoint> overallTrend = new ArrayList<>();
        for (var entry : byWeek.entrySet()) {
            List<BigDecimal> scores = entry.getValue().stream()
                    .map(InterviewRecord::getScore)
                    .filter(s -> s != null)
                    .toList();
            BigDecimal avg = avg(scores);
            overallTrend.add(TrendVO.WeeklyPoint.builder()
                    .week(entry.getKey())
                    .score(avg)
                    .count(scores.size())
                    .build());
        }

        // Compute category trends
        // Collect all category IDs that appear
        Map<Long, String> categoryNameMap = new HashMap<>();
        Set<Long> allCatIds = records.stream()
                .map(r -> questionMap.get(r.getQuestionId()))
                .filter(q -> q != null && q.getCategoryId() != null)
                .map(Question::getCategoryId)
                .collect(Collectors.toSet());
        if (!allCatIds.isEmpty()) {
            categoryService.listByIds(allCatIds).forEach(c -> categoryNameMap.put(c.getId(), c.getName()));
        }

        // Build per-category weekly scores
        Map<Long, Map<String, List<BigDecimal>>> catWeekScores = new HashMap<>();
        for (InterviewRecord r : records) {
            InterviewSession s = sessionMap.get(r.getSessionId());
            Question q = questionMap.get(r.getQuestionId());
            if (s == null || q == null || q.getCategoryId() == null) continue;
            if (filterCategoryIds != null && !filterCategoryIds.contains(q.getCategoryId())) continue;

            String weekKey = formatWeek(s.getStartTime().toLocalDate());
            catWeekScores
                    .computeIfAbsent(q.getCategoryId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(weekKey, k -> new ArrayList<>())
                    .add(r.getScore());
        }

        List<TrendVO.CategoryTrend> categoryTrends = new ArrayList<>();
        for (var catEntry : catWeekScores.entrySet()) {
            Long catId = catEntry.getKey();
            String catName = categoryNameMap.getOrDefault(catId, "Unknown");
            List<TrendVO.WeeklyPoint> catPoints = new ArrayList<>();
            for (var weekEntry : catEntry.getValue().entrySet()) {
                catPoints.add(TrendVO.WeeklyPoint.builder()
                        .week(weekEntry.getKey())
                        .score(avg(weekEntry.getValue()))
                        .count(weekEntry.getValue().size())
                        .build());
            }
            categoryTrends.add(TrendVO.CategoryTrend.builder()
                    .categoryId(catId)
                    .categoryName(catName)
                    .points(catPoints)
                    .build());
        }

        // Collect all week keys in order
        List<String> allWeeks = new ArrayList<>(byWeek.keySet());
        allWeeks.sort(String::compareTo);

        return TrendVO.builder()
                .weeks(allWeeks)
                .overallTrend(overallTrend)
                .categoryTrends(categoryTrends)
                .build();
    }

    @Override
    public EfficiencyVO getEfficiencyData(Long userId) {
        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId));
        List<KnowledgeCard> cards = knowledgeCardMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCard>()
                        .inSql(KnowledgeCard::getTaskId,
                                "SELECT id FROM knowledge_card_task WHERE user_id = " + userId + " AND status <> 'invalid'"));

        Map<String, Long> masteryDist = new HashMap<>();
        wrongs.forEach(w -> masteryDist.merge(
                w.getMasteryLevel() != null ? w.getMasteryLevel() : "not_started",
                1L,
                Long::sum));
        cards.forEach(card -> masteryDist.merge(normalizeKnowledgeMastery(card), 1L, Long::sum));

        BigDecimal wrongEfSum = wrongs.stream()
                .map(WrongQuestion::getEaseFactor)
                .filter(ef -> ef != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cardEfSum = cards.stream()
                .map(KnowledgeCard::getEaseFactor)
                .filter(ef -> ef != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long efCount = wrongs.stream()
                .filter(w -> w.getEaseFactor() != null)
                .count()
                + cards.stream()
                .filter(card -> card.getEaseFactor() != null)
                .count();
        BigDecimal avgEaseFactor = efCount > 0
                ? wrongEfSum.add(cardEfSum).divide(BigDecimal.valueOf(efCount), 2, RoundingMode.HALF_UP)
                : new BigDecimal("2.50");

        List<ReviewLog> allLogs = reviewLogMapper.selectList(
                new LambdaQueryWrapper<ReviewLog>()
                        .eq(ReviewLog::getUserId, userId)
                        .orderByAsc(ReviewLog::getCreateTime));
        List<KnowledgeCardLog> allCardLogs = knowledgeCardLogMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCardLog>()
                        .eq(KnowledgeCardLog::getUserId, userId)
                        .orderByAsc(KnowledgeCardLog::getCreateTime));

        Map<Integer, Long> ratingDist = new HashMap<>();
        allLogs.forEach(logItem -> ratingDist.merge(logItem.getRating(), 1L, Long::sum));
        allCardLogs.forEach(logItem -> ratingDist.merge(logItem.getRating(), 1L, Long::sum));

        record WeeklyLogPoint(String week, BigDecimal easeFactorAfter, Integer rating, String contentType, LocalDateTime createTime) {}
        List<WeeklyLogPoint> mergedLogs = new ArrayList<>();
        allLogs.forEach(logItem -> mergedLogs.add(new WeeklyLogPoint(
                formatWeek(logItem.getCreateTime().toLocalDate()),
                logItem.getEaseFactorAfter(),
                logItem.getRating(),
                resolveWrongContentType(userId, logItem.getWrongQuestionId()),
                logItem.getCreateTime())));
        allCardLogs.forEach(logItem -> mergedLogs.add(new WeeklyLogPoint(
                formatWeek(logItem.getCreateTime().toLocalDate()),
                logItem.getEaseFactorAfter(),
                logItem.getRating(),
                "knowledge_card",
                logItem.getCreateTime())));
        mergedLogs.sort(Comparator.comparing(WeeklyLogPoint::createTime));

        Map<String, List<WeeklyLogPoint>> logsByWeek = mergedLogs.stream()
                .collect(Collectors.groupingBy(
                        WeeklyLogPoint::week,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<EfficiencyVO.WeeklyEF> efTrend = new ArrayList<>();
        List<EfficiencyVO.WeeklyForgettingRate> frTrend = new ArrayList<>();

        for (var entry : logsByWeek.entrySet()) {
            List<WeeklyLogPoint> weekLogs = entry.getValue();

            BigDecimal lastEF = weekLogs.stream()
                    .map(WeeklyLogPoint::easeFactorAfter)
                    .filter(ef -> ef != null)
                    .reduce((a, b) -> b)
                    .orElse(new BigDecimal("2.50"));
            efTrend.add(EfficiencyVO.WeeklyEF.builder()
                    .week(entry.getKey())
                    .avgEF(lastEF)
                    .reviewCount(weekLogs.size())
                    .build());

            long againCount = weekLogs.stream()
                    .filter(l -> l.rating() == 1)
                    .count();
            double fr = weekLogs.isEmpty() ? 0.0
                    : (double) againCount / weekLogs.size();
            frTrend.add(EfficiencyVO.WeeklyForgettingRate.builder()
                    .week(entry.getKey())
                    .forgettingRate(fr)
                    .totalRatings(weekLogs.size())
                    .againCount((int) againCount)
                    .build());
        }

        int streak = 0;
        LocalDate check = LocalDate.now();
        Set<LocalDate> reviewDays = mergedLogs.stream()
                .map(l -> l.createTime().toLocalDate())
                .collect(Collectors.toSet());
        if (!reviewDays.contains(check)) {
            check = check.minusDays(1);
        }
        while (reviewDays.contains(check)) {
            streak++;
            check = check.minusDays(1);
        }

        Map<String, Long> contentTypeDistribution = mergedLogs.stream()
                .collect(Collectors.groupingBy(WeeklyLogPoint::contentType, Collectors.counting()));

        return EfficiencyVO.builder()
                .avgEaseFactor(avgEaseFactor)
                .efTrend(efTrend)
                .ratingDistribution(ratingDist)
                .forgettingRateTrend(frTrend)
                .masteryDistribution(masteryDist)
                .contentTypeDistribution(contentTypeDistribution)
                .totalReviews(mergedLogs.size())
                .currentStreak(streak)
                .build();
    }

    @Override
    public LearningInsightsVO getLearningInsights(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(DayOfWeek.MONDAY);
        LocalDate lastMonday = thisMonday.minusWeeks(1);

        // Load recent sessions for comparison
        List<InterviewSession> recentSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "finished")
                        .isNotNull(InterviewSession::getTotalScore)
                        .ge(InterviewSession::getCreateTime, lastMonday.atStartOfDay())
                        .orderByAsc(InterviewSession::getCreateTime));

        // Split into this week and last week
        BigDecimal thisWeekAvg = BigDecimal.ZERO;
        BigDecimal lastWeekAvg = BigDecimal.ZERO;
        int thisWeekCount = 0;
        int lastWeekCount = 0;

        for (InterviewSession s : recentSessions) {
            if (s.getStartTime() == null || s.getTotalScore() == null) continue;
            LocalDate sessionDate = s.getStartTime().toLocalDate();
            if (!sessionDate.isBefore(thisMonday)) {
                thisWeekAvg = thisWeekAvg.add(s.getTotalScore());
                thisWeekCount++;
            } else if (!sessionDate.isBefore(lastMonday)) {
                lastWeekAvg = lastWeekAvg.add(s.getTotalScore());
                lastWeekCount++;
            }
        }

        BigDecimal thisAvg = thisWeekCount > 0
                ? thisWeekAvg.divide(BigDecimal.valueOf(thisWeekCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal lastAvg = lastWeekCount > 0
                ? lastWeekAvg.divide(BigDecimal.valueOf(lastWeekCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Category-level comparison (this week vs last week)
        // Load all records for recent sessions
        Set<Long> sessionIds = recentSessions.stream()
                .map(InterviewSession::getId)
                .collect(Collectors.toSet());

        Map<Long, Map<String, BigDecimal>> catWeekScores = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            List<InterviewRecord> records = recordMapper.selectList(
                    new LambdaQueryWrapper<InterviewRecord>()
                            .in(InterviewRecord::getSessionId, sessionIds)
                            .isNotNull(InterviewRecord::getScore));

            Map<Long, InterviewSession> sessionMap = recentSessions.stream()
                    .collect(Collectors.toMap(InterviewSession::getId, Function.identity(), (a, b) -> a));

            Set<Long> qIds = records.stream()
                    .map(InterviewRecord::getQuestionId)
                    .collect(Collectors.toSet());
            Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds)
                    .stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

            Set<Long> catIds = qMap.values().stream()
                    .map(Question::getCategoryId)
                    .filter(c -> c != null)
                    .collect(Collectors.toSet());
            Map<Long, String> catNames = new HashMap<>();
            if (!catIds.isEmpty()) {
                categoryService.listByIds(catIds)
                        .forEach(c -> catNames.put(c.getId(), c.getName()));
            }

            for (InterviewRecord r : records) {
                InterviewSession s = sessionMap.get(r.getSessionId());
                Question q = qMap.get(r.getQuestionId());
                if (s == null || q == null || q.getCategoryId() == null) continue;
                if (s.getStartTime() == null) continue;

                String period = s.getStartTime().toLocalDate().isBefore(thisMonday) ? "last" : "this";
                catWeekScores
                        .computeIfAbsent(q.getCategoryId(), k -> new HashMap<>())
                        .merge(period, r.getScore(), BigDecimal::add);

                // Also track count for averaging
                catWeekScores
                        .computeIfAbsent(q.getCategoryId(), k -> new HashMap<>())
                        .merge(period + "_count", BigDecimal.ONE, BigDecimal::add);
            }

            // Build category changes
            List<LearningInsightsVO.CategoryChange> changes = new ArrayList<>();
            for (var entry : catWeekScores.entrySet()) {
                Long catId = entry.getKey();
                Map<String, BigDecimal> scores = entry.getValue();
                String catName = catNames.getOrDefault(catId, "Unknown");

                BigDecimal thisScore = scores.getOrDefault("this", BigDecimal.ZERO);
                BigDecimal thisCnt = scores.getOrDefault("this_count", BigDecimal.ZERO);
                BigDecimal lastScore = scores.getOrDefault("last", BigDecimal.ZERO);
                BigDecimal lastCnt = scores.getOrDefault("last_count", BigDecimal.ZERO);

                BigDecimal thisCatAvg = thisCnt.compareTo(BigDecimal.ZERO) > 0
                        ? thisScore.divide(thisCnt, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                BigDecimal lastCatAvg = lastCnt.compareTo(BigDecimal.ZERO) > 0
                        ? lastScore.divide(lastCnt, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

                if (thisCnt.intValue() > 0 || lastCnt.intValue() > 0) {
                    changes.add(LearningInsightsVO.CategoryChange.builder()
                            .categoryId(catId)
                            .categoryName(catName)
                            .thisWeekScore(thisCatAvg)
                            .lastWeekScore(lastCatAvg)
                            .change(thisCatAvg.subtract(lastCatAvg))
                            .build());
                }
            }

            // Sort by change magnitude
            changes.sort(Comparator.comparing(
                    (LearningInsightsVO.CategoryChange c) -> c.getChange().abs()).reversed());

            return LearningInsightsVO.builder()
                    .thisWeekAvgScore(thisAvg)
                    .lastWeekAvgScore(lastAvg)
                    .thisWeekInterviewCount(thisWeekCount)
                    .lastWeekInterviewCount(lastWeekCount)
                    .categoryChanges(changes)
                    .bestStudyHours(analyzeBestHours(userId))
                    .build();
        }

        return LearningInsightsVO.builder()
                .thisWeekAvgScore(thisAvg)
                .lastWeekAvgScore(lastAvg)
                .thisWeekInterviewCount(thisWeekCount)
                .lastWeekInterviewCount(lastWeekCount)
                .categoryChanges(List.of())
                .bestStudyHours(List.of())
                .build();
    }

    // ──────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────

    /**
     * Analyze historical interview session start times to find best study hours.
     */
    private List<LearningInsightsVO.HourDistribution> analyzeBestHours(Long userId) {
        List<InterviewSession> allSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "finished")
                        .select(InterviewSession::getStartTime, InterviewSession::getTotalScore)
                        .isNotNull(InterviewSession::getStartTime)
                        .isNotNull(InterviewSession::getTotalScore)
                        .last("LIMIT 200"));

        if (allSessions.isEmpty()) {
            return List.of();
        }

        // Group by hour bucket (morning/afternoon/evening/night)
        Map<String, List<BigDecimal>> hourBuckets = new LinkedHashMap<>();
        hourBuckets.put("上午 (6-12)", new ArrayList<>());
        hourBuckets.put("下午 (12-18)", new ArrayList<>());
        hourBuckets.put("晚上 (18-24)", new ArrayList<>());
        hourBuckets.put("凌晨 (0-6)", new ArrayList<>());

        for (InterviewSession s : allSessions) {
            int hour = s.getStartTime().getHour();
            String bucket;
            if (hour >= 6 && hour < 12) bucket = "上午 (6-12)";
            else if (hour >= 12 && hour < 18) bucket = "下午 (12-18)";
            else if (hour >= 18 && hour < 24) bucket = "晚上 (18-24)";
            else bucket = "凌晨 (0-6)";
            hourBuckets.get(bucket).add(s.getTotalScore());
        }

        List<LearningInsightsVO.HourDistribution> result = new ArrayList<>();
        for (var entry : hourBuckets.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            BigDecimal avg = entry.getValue().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP);
            result.add(LearningInsightsVO.HourDistribution.builder()
                    .timeSlot(entry.getKey())
                    .sessionCount(entry.getValue().size())
                    .avgScore(avg)
                    .build());
        }

        result.sort(Comparator.comparing(LearningInsightsVO.HourDistribution::getAvgScore).reversed());
        return result;
    }

    private String formatWeek(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    private BigDecimal avg(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private String normalizeKnowledgeMastery(KnowledgeCard card) {
        if ("mastered".equals(card.getState())) {
            return "mastered";
        }
        if ("learning".equals(card.getState())) {
            return "reviewing";
        }
        if ("weak".equals(card.getState())) {
            return "not_started";
        }
        return ReviewSchedulingRules.resolveMasteryLevel(card.getEaseFactor(), card.getStreak());
    }

    private String resolveWrongContentType(Long userId, Long wrongQuestionId) {
        WrongQuestion wrongQuestion = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wrongQuestion == null || !userId.equals(wrongQuestion.getUserId())) {
            return "wrong_card";
        }
        return "interview".equalsIgnoreCase(wrongQuestion.getSourceType()) ? "interview_card" : "wrong_card";
    }
}
