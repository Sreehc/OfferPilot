package com.offerpilot.wrong.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WrongMasteryUpdateRequest {
    @NotBlank(message = "cannot be blank")
    private String masteryLevel;
}

