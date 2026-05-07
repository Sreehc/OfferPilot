package com.bytecoach.cards.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CardRateRequest {
    @NotNull(message = "cardId is required")
    private Long cardId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be 1-4")
    @Max(value = 4, message = "rating must be 1-4")
    private Integer rating;

    private Integer responseTimeMs;
}
