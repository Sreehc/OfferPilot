package com.offerpilot.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeSearchRequest {
    @NotBlank(message = "cannot be blank")
    private String query;
}

