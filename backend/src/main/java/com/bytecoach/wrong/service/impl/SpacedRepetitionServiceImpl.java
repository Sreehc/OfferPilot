package com.bytecoach.wrong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.question.entity.Question;
import com.bytecoach.question.mapper.QuestionMapper;
import com.bytecoach.wrong.dto.ReviewRateRequest;
import com.bytecoach.wrong.dto.ReviewStatsVO;
import com.bytecoach.wrong.dto.ReviewTodayVO;
import com.bytecoach.wrong.entity.ReviewLog;
import com.bytecoach.wrong.entity.WrongQuestion;
import com.bytecoach.wrong.mapper.ReviewLogMapper;
import com.bytecoach.wrong.mapper.WrongQuestionMapper;
import com.bytecoach.wrong.service.SpacedRepetitionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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

    private static final BigDecimal MIN_EASE_FACTOR = new BigDecimal("1.30");
    private static final BigDecimal DEFAULT_EASE_FACTOR = new BigDecimal("2.50");

    private final WrongQuestionMapper wrongQuestionMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final QuestionMapper questionMapper;

    @Override
    public List<ReviewTodayVO> getTodayReviews(Long userId) {
        LocalDate today = LocalDate.now();

        // Fetch all wrong questions with a review date <= today
        List<WrongQuestion> dueItems = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .le(WrongQuestion::getNextReviewDate, today)
                        .orderByAsc(WrongQuestion::getNextReviewDate));

        if (dueItems.isEmpty()) {
            return List.of();
        }

        // Batch load question titles
        Set<Long> questionIds = dueItems.stream()
                .map(WrongQuestion::getQuestionId)
                .collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        List<ReviewTodayVO> result = new ArrayList<>();
        for (WrongQuestion wq : dueItems) {
            Question q = questionMap.get(wq.getQuestionId());
            long overdueDays = ChronoUnit.DAYS.between(wq.getNextReviewDate(), today);

            result.add(ReviewTodayVO.builder()
                    .wrongQuestionId(wq.getId())
                    .questionId(wq.getQuestionId())
                    .title(q != null ? q.getTitle() : "Unknown")
                    .standardAnswer(wq.getStandardAnswer())
                    .errorReason(wq.getErrorReason())
                    .easeFactor(wq.getEaseFactor())
                    .intervalDays(wq.getIntervalDays())
                    .streak(wq.getStreak())
                    .nextReviewDate(wq.getNextReviewDate())
                    .overdueDays(overdueDays)
                    .masteryLevel(computeMasteryLevel(wq.getEaseFactor(), wq.getStreak()))
                    .build());
        }

        // Sort: overdue first (most overdue first), then by ease factor ascending (harder items first)
        result.sort(Comparator
                .comparingLong(ReviewTodayVO::getOverdueDays).reversed()
                .thenComparing(ReviewTodayVO::getEaseFactor));

        return result;
    }

    @Override
    public int getTodayReviewCount(Long userId) {
        LocalDate today = LocalDate.now();
        return Math.toIntExact(wrongQuestionMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .le(WrongQuestion::getNextReviewDate, today)));
    }

    @Override
    @Transactional
    public void rate(Long userId, Long wrongQuestionId, ReviewRateRequest request) {
        WrongQuestion wq = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wq == null || !wq.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "wrong question not found");
        }

        int rating = request.getRating();

        // Capture before-state
        BigDecimal efBefore = wq.getEaseFactor() != null ? wq.getEaseFactor() : DEFAULT_EASE_FACTOR;
        int intervalBefore = wq.getIntervalDays() != null ? wq.getIntervalDays() : 0;

        // Apply SM-2 algorithm
        BigDecimal efAfter;
        int intervalAfter;
        int newStreak;

        if (rating < 3) {
            // Again or Hard: reset interval, keep EF (with penalty), reset streak
            intervalAfter = 1;
            newStreak = 0;
            // Penalize EF for "Again" (1) more than "Hard" (2)
            BigDecimal penalty = rating == 1
                    ? new BigDecimal("0.20")
                    : new BigDecimal("0.15");
            efAfter = efBefore.subtract(penalty).max(MIN_EASE_FACTOR);
        } else {
            // Good (3) or Easy (4): increase interval
            newStreak = (wq.getStreak() != null ? wq.getStreak() : 0) + 1;

            if (newStreak == 1) {
                intervalAfter = 1;
            } else if (newStreak == 2) {
                intervalAfter = 6;
            } else {
                // interval = round(previous_interval * EF)
                intervalAfter = BigDecimal.valueOf(intervalBefore)
                        .multiply(efBefore)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
            }

            // Update EF: EF' = EF + (0.1 - (4 - rating) * (0.08 + (4 - rating) * 0.02))
            // SM-2 uses 0-5 scale; we use 1-4, so map: rating 3 -> quality 3, rating 4 -> quality 5
            int quality = rating == 4 ? 5 : 3;
            BigDecimal efDelta = new BigDecimal("0.1")
                    .subtract(BigDecimal.valueOf(5 - quality)
                            .multiply(new BigDecimal("0.08")
                                    .add(BigDecimal.valueOf(5 - quality)
                                            .multiply(new BigDecimal("0.02")))));
            efAfter = efBefore.add(efDelta).max(MIN_EASE_FACTOR);
        }

        LocalDate nextReviewDate = LocalDate.now().plusDays(intervalAfter);

        // Update wrong question
        wq.setEaseFactor(efAfter);
        wq.setIntervalDays(intervalAfter);
        wq.setNextReviewDate(nextReviewDate);
        wq.setStreak(newStreak);
        wq.setReviewCount((wq.getReviewCount() != null ? wq.getReviewCount() : 0) + 1);
        wq.setLastReviewTime(java.time.LocalDateTime.now());

        // Auto-compute mastery level
        wq.setMasteryLevel(computeMasteryLevel(efAfter, newStreak));

        wrongQuestionMapper.updateById(wq);

        // Insert review log
        ReviewLog reviewLog = new ReviewLog();
        reviewLog.setUserId(userId);
        reviewLog.setWrongQuestionId(wrongQuestionId);
        reviewLog.setRating(rating);
        reviewLog.setResponseTimeMs(request.getResponseTimeMs());
        reviewLog.setEaseFactorBefore(efBefore);
        reviewLog.setIntervalBefore(intervalBefore);
        reviewLog.setEaseFactorAfter(efAfter);
        reviewLog.setIntervalAfter(intervalAfter);
        reviewLogMapper.insert(reviewLog);

        log.info("Review rated: userId={}, wqId={}, rating={}, EF {}->{}, interval {}->{} days, next={}",
                userId, wrongQuestionId, rating, efBefore, efAfter, intervalBefore, intervalAfter, nextReviewDate);
    }

    @Override
    public int getTotalReviewCount(Long userId) {
        return Math.toIntExact(reviewLogMapper.selectCount(
                new LambdaQueryWrapper<ReviewLog>()
                        .eq(ReviewLog::getUserId, userId)));
    }

    @Override
    public int getReviewStreak(Long userId) {
        // Get distinct review dates in descending order
        List<ReviewLog> recentLogs = reviewLogMapper.selectList(
                new LambdaQueryWrapper<ReviewLog>()
                        .eq(ReviewLog::getUserId, userId)
                        .select(ReviewLog::getCreateTime)
                        .orderByDesc(ReviewLog::getCreateTime)
                        .last("LIMIT 1000"));

        if (recentLogs.isEmpty()) {
            return 0;
        }

        // Count consecutive days from today backwards
        Set<LocalDate> reviewDates = recentLogs.stream()
                .map(r -> r.getCreateTime().toLocalDate())
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate checkDate = LocalDate.now();

        // If no review today, check if there was one yesterday (streak may still be active)
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

    /**
     * Compute mastery level from ease factor and streak.
     * - mastered: EF >= 2.3 AND streak >= 3
     * - reviewing: EF >= 1.8 OR streak >= 1
     * - not_started: everything else
     */
    public static String computeMasteryLevel(BigDecimal easeFactor, Integer streak) {
        BigDecimal ef = easeFactor != null ? easeFactor : DEFAULT_EASE_FACTOR;
        int s = streak != null ? streak : 0;

        if (ef.compareTo(new BigDecimal("2.3")) >= 0 && s >= 3) {
            return "mastered";
        }
        if (ef.compareTo(new BigDecimal("1.8")) >= 0 || s >= 1) {
            return "reviewing";
        }
        return "not_started";
    }
}
