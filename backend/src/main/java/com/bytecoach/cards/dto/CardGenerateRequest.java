package com.bytecoach.cards.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CardGenerateRequest {
    @NotNull(message = "docId is required")
    private Long docId;

    @NotNull(message = "days is required")
    @Min(value = 1, message = "days must be between 1 and 30")
    @Max(value = 30, message = "days must be between 1 and 30")
    private Integer days;
}
