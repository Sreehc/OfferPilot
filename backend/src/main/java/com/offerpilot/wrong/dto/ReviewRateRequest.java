package com.offerpilot.wrong.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRateRequest {

    /**
     * Review item content type: knowledge_card | wrong_card | interview_card.
     * Old wrong-question callers may omit this field.
     */
    private String contentType;

    /**
     * User's self-rating: 1=Again, 2=Hard, 3=Good, 4=Easy.
     */
    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be 1-4")
    @Max(value = 4, message = "rating must be 1-4")
    private Integer rating;

    /**
     * Optional: time spent on this review in milliseconds.
     */
    private Integer responseTimeMs;
}
