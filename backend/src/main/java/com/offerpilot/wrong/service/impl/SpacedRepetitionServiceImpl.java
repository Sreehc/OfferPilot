package com.offerpilot.wrong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.dashboard.service.DashboardService;
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
    private static final String CONTENT_WRONG_CARD = "wrong_card";

    private final WrongQuestionMapper wrongQuestionMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final QuestionMapper questionMapper;
    private final DashboardService dashboardService;

    @Override
    public ReviewTodayVO getTodayReviews(Long userId, String contentType) {
        LocalDate today = LocalDate.now();
        String normalizedContentType = normalizeContentType(contentType);
        List<ReviewTodayItemVO> items = loadWrongReviewItems(userId, today);
        List<ReviewTodayItemVO> filteredItems = items.stream()
                .filter(item -> matchesContentType(item, normalizedContentType))
                .sorted(reviewComparator())
                .toList();

        Map<String, Integer> countsByContentType = Map.of(CONTENT_WRONG_CARD, items.size());
        int overdueCount = (int) filteredItems.stream().filter(item -> item.getOverdueDays() > 0).count();

        return ReviewTodayVO.builder()
                .selectedContentType(normalizedContentType)
                .totalPending(filteredItems.size())
                .overdueCount(overdueCount)
                .todayCompleted(countReviewsToday(userId))
                .currentStreak(computeReviewStreak(userId))
                .countsByContentType(countsByContentType)
                .items(filteredItems)
                .build();
    }

    @Override
    @Transactional
    public ReviewTodayVO rate(Long userId, Long reviewItemId, ReviewRateRequest request) {
        rateWrongQuestion(userId, reviewItemId, request);
        dashboardService.evictCache(userId);
        return getTodayReviews(userId, normalizeContentType(request.getContentType()));
    }

    @Override
    public ReviewStatsVO getReviewStats(Long userId) {
        ReviewTodayVO todayView = getTodayReviews(userId, CONTENT_WRONG_CARD);
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

        return dueItems.stream()
                .map(wq -> {
                    Question question = questionMap.get(wq.getQuestionId());
                    LocalDate dueDate = wq.getNextReviewDate() == null ? today : wq.getNextReviewDate();
                    long overdueDays = ChronoUnit.DAYS.between(dueDate, today);
                    return ReviewTodayItemVO.builder()
                            .reviewItemId(String.valueOf(wq.getId()))
                            .contentType(CONTENT_WRONG_CARD)
                            .sourceType(wq.getSourceType())
                            .title(question != null ? question.getTitle() : "Unknown")
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
                            .build();
                })
                .toList();
    }

    private void rateWrongQuestion(Long userId, Long wrongQuestionId, ReviewRateRequest request) {
        WrongQuestion wrongQuestion = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wrongQuestion == null || !wrongQuestion.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "wrong question not found");
        }

        BigDecimal efBefore = wrongQuestion.getEaseFactor() != null
                ? wrongQuestion.getEaseFactor()
                : ReviewSchedulingRules.DEFAULT_EASE_FACTOR;
        int intervalBefore = wrongQuestion.getIntervalDays() != null ? wrongQuestion.getIntervalDays() : 0;
        ReviewScheduleResult schedule = schedule(efBefore, intervalBefore, wrongQuestion.getStreak(), request.getRating());

        wrongQuestion.setEaseFactor(schedule.getEaseFactor());
        wrongQuestion.setIntervalDays(schedule.getIntervalDays());
        wrongQuestion.setNextReviewDate(schedule.getNextReviewDate());
        wrongQuestion.setStreak(schedule.getStreak());
        wrongQuestion.setReviewCount((wrongQuestion.getReviewCount() != null ? wrongQuestion.getReviewCount() : 0) + 1);
        wrongQuestion.setLastReviewTime(LocalDateTime.now());
        wrongQuestion.setMasteryLevel(schedule.getMasteryLevel());
        wrongQuestionMapper.updateById(wrongQuestion);

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

        log.info("Review rated: userId={}, wrongId={}, rating={}, EF {}->{}, interval {}->{} days, next={}",
                userId, wrongQuestionId, request.getRating(), efBefore, schedule.getEaseFactor(), intervalBefore,
                schedule.getIntervalDays(), schedule.getNextReviewDate());
    }

    private int countTotalReviews(Long userId) {
        long wrongCount = reviewLogMapper.selectCount(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getUserId, userId));
        return Math.toIntExact(wrongCount);
    }

    private int countReviewsToday(Long userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        long wrongCount = reviewLogMapper.selectCount(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getUserId, userId)
                .ge(ReviewLog::getCreateTime, start));
        return Math.toIntExact(wrongCount);
    }

    private int computeReviewStreak(Long userId) {
        List<ReviewLog> recentLogs = reviewLogMapper.selectList(
                new LambdaQueryWrapper<ReviewLog>()
                        .eq(ReviewLog::getUserId, userId)
                        .select(ReviewLog::getCreateTime)
                        .orderByDesc(ReviewLog::getCreateTime)
                        .last("LIMIT 1000"));

        if (recentLogs.isEmpty()) {
            return 0;
        }

        Set<LocalDate> reviewDates = recentLogs.stream()
                .map(log -> log.getCreateTime().toLocalDate())
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate cursor = LocalDate.now();
        if (!reviewDates.contains(cursor)) {
            cursor = cursor.minusDays(1);
            if (!reviewDates.contains(cursor)) {
                return 0;
            }
        }

        while (reviewDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private Map<String, Integer> buildHeatmap(Long userId) {
        Map<String, Integer> heatmap = new HashMap<>();
        List<ReviewLog> logs = reviewLogMapper.selectList(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getUserId, userId)
                .ge(ReviewLog::getCreateTime, LocalDate.now().minusDays(90).atStartOfDay()));
        for (ReviewLog logItem : logs) {
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
            return CONTENT_WRONG_CARD;
        }
        return CONTENT_WRONG_CARD.equals(contentType) ? contentType : CONTENT_ALL;
    }

    public static String resolveMasteryLevel(BigDecimal easeFactor, Integer streak) {
        return ReviewSchedulingRules.resolveMasteryLevel(easeFactor, streak);
    }

    public static String computeMasteryLevelStatic(BigDecimal easeFactor, Integer streak) {
        return ReviewSchedulingRules.resolveMasteryLevel(easeFactor, streak);
    }
}
