package com.offerpilot.wrong.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.wrong.entity.WrongQuestion;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for the spaced repetition engine.
 * - Initializes SM-2 fields for newly added wrong questions.
 * - Caches daily review counts per user in Redis for fast dashboard display.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewScheduler {

    private static final String REVIEW_COUNT_PREFIX = "review:today_count:";
    private static final BigDecimal DEFAULT_EASE_FACTOR = new BigDecimal("2.50");

    private final WrongQuestionMapper wrongQuestionMapper;
    private final StringRedisTemplate redisTemplate;
    private final OfferPilotProperties props;

    /**
     * Every 10 minutes: find wrong questions that have no next_review_date
     * (newly inserted from interviews/chats) and initialize SM-2 fields.
     */
    @Scheduled(fixedRateString = "${offerpilot.review.init-fixed-rate-ms:600000}")
    public void initializeNewWrongQuestions() {
        List<WrongQuestion> uninitialized = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .isNull(WrongQuestion::getNextReviewDate)
                        .last("LIMIT " + props.getReview().getInitBatchLimit()));

        if (uninitialized.isEmpty()) {
            return;
        }

        int updated = 0;
        for (WrongQuestion wq : uninitialized) {
            wq.setEaseFactor(DEFAULT_EASE_FACTOR);
            wq.setIntervalDays(1);
            wq.setNextReviewDate(LocalDate.now());
            wq.setStreak(0);
            wrongQuestionMapper.updateById(wq);
            updated++;
        }

        log.info("Initialized SM-2 fields for {} new wrong questions", updated);
    }

    /**
     * Every day at 00:05: cache each user's today review count in Redis.
     * This avoids expensive COUNT queries on the dashboard.
     */
    @Scheduled(cron = "${offerpilot.review.cron-daily-cache:0 5 0 * * ?}")
    public void cacheDailyReviewCounts() {
        log.info("Caching daily review counts...");
        LocalDate today = LocalDate.now();

        // Get distinct user IDs with due reviews
        List<WrongQuestion> dueItems = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .le(WrongQuestion::getNextReviewDate, today)
                        .select(WrongQuestion::getUserId)
                        .groupBy(WrongQuestion::getUserId));

        for (WrongQuestion item : dueItems) {
            Long userId = item.getUserId();
            long count = wrongQuestionMapper.selectCount(
                    new LambdaQueryWrapper<WrongQuestion>()
                            .eq(WrongQuestion::getUserId, userId)
                            .le(WrongQuestion::getNextReviewDate, today));
            String key = REVIEW_COUNT_PREFIX + userId;
            redisTemplate.opsForValue().set(key, String.valueOf(count));
            // TTL until end of day
            redisTemplate.expire(key, java.time.Duration.ofHours(props.getReview().getCacheTtlHours()));
        }

        log.info("Cached review counts for {} users", dueItems.size());
    }
}
