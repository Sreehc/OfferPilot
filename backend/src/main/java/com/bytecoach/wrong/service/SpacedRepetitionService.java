package com.bytecoach.wrong.service;

import com.bytecoach.wrong.dto.ReviewRateRequest;
import com.bytecoach.wrong.dto.ReviewScheduleResult;
import com.bytecoach.wrong.dto.ReviewStatsVO;
import com.bytecoach.wrong.dto.ReviewTodayVO;
import java.math.BigDecimal;

public interface SpacedRepetitionService {

    ReviewTodayVO getTodayReviews(Long userId, String contentType);

    ReviewTodayVO rate(Long userId, Long reviewItemId, ReviewRateRequest request);

    ReviewStatsVO getReviewStats(Long userId);

    ReviewScheduleResult schedule(BigDecimal easeFactor, Integer intervalDays, Integer streak, Integer rating);

    String computeMasteryLevel(BigDecimal easeFactor, Integer streak);
}
