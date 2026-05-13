package com.offerpilot.wrong.service;

import com.offerpilot.wrong.dto.ReviewRateRequest;
import com.offerpilot.wrong.dto.ReviewScheduleResult;
import com.offerpilot.wrong.dto.ReviewStatsVO;
import com.offerpilot.wrong.dto.ReviewTodayVO;
import java.math.BigDecimal;

public interface SpacedRepetitionService {

    ReviewTodayVO getTodayReviews(Long userId, String contentType);

    ReviewTodayVO rate(Long userId, Long reviewItemId, ReviewRateRequest request);

    ReviewStatsVO getReviewStats(Long userId);

    ReviewScheduleResult schedule(BigDecimal easeFactor, Integer intervalDays, Integer streak, Integer rating);

    String computeMasteryLevel(BigDecimal easeFactor, Integer streak);
}
