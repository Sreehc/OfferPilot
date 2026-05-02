package com.bytecoach.ai.service;

import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Per-user daily LLM call quota management using Redis.
 * Default limit: 100 calls per user per day.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmQuotaService {

    private final StringRedisTemplate redisTemplate;

    private static final int DAILY_LIMIT = 100;
    private static final String QUOTA_KEY_PREFIX = "llm_quota:";

    /**
     * Check and consume one LLM call quota for the user.
     * Throws BusinessException if quota is exceeded.
     */
    public void checkAndConsume(Long userId) {
        String key = QUOTA_KEY_PREFIX + userId + ":" + java.time.LocalDate.now();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // Set TTL to end of day
            redisTemplate.expire(key, Duration.ofHours(24));
        }

        if (count != null && count > DAILY_LIMIT) {
            log.warn("LLM quota exceeded for user {}: {}/{}", userId, count, DAILY_LIMIT);
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(),
                    "今日 AI 调用次数已达上限（" + DAILY_LIMIT + " 次），请明天再试");
        }

        if (log.isDebugEnabled()) {
            log.debug("LLM quota for user {}: {}/{}", userId, count, DAILY_LIMIT);
        }
    }

    /**
     * Get remaining quota for the user today.
     */
    public int getRemainingQuota(Long userId) {
        String key = QUOTA_KEY_PREFIX + userId + ":" + java.time.LocalDate.now();
        String val = redisTemplate.opsForValue().get(key);
        int used = val != null ? Integer.parseInt(val) : 0;
        return Math.max(0, DAILY_LIMIT - used);
    }
}
