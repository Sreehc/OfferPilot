package com.bytecoach.cards.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class CardGenerateRequest {
    @NotNull(message = "docId is required")
    private Long docId;

    @Size(min = 1, max = 4, message = "cardTypes must contain 1-4 items")
    private List<String> cardTypes;

    @Min(value = 4, message = "cardCount must be between 4 and 30")
    @Max(value = 30, message = "cardCount must be between 4 and 30")
    private Integer cardCount;

    private String difficulty;

    @NotNull(message = "days is required")
    @Min(value = 1, message = "days must be between 1 and 30")
    @Max(value = 30, message = "days must be between 1 and 30")
    private Integer days;
}
