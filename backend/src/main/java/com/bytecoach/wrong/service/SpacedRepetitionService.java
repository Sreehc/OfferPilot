package com.bytecoach.wrong.service;

import com.bytecoach.wrong.dto.ReviewRateRequest;
import com.bytecoach.wrong.dto.ReviewTodayVO;
import java.util.List;

public interface SpacedRepetitionService {

    /**
     * Get today's due review items for the user, sorted by priority.
     * Priority: overdue first (by days overdue desc), then new items.
     */
    List<ReviewTodayVO> getTodayReviews(Long userId);

    /**
     * Get the count of today's pending reviews.
     */
    int getTodayReviewCount(Long userId);

    /**
     * Submit a review rating for a wrong question.
     * Applies SM-2 algorithm to calculate next review date.
     */
    void rate(Long userId, Long wrongQuestionId, ReviewRateRequest request);

    /**
     * Get total review count for the user.
     */
    int getTotalReviewCount(Long userId);

    /**
     * Get current streak (consecutive days with at least one review).
     */
    int getReviewStreak(Long userId);
}
