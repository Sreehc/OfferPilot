package com.offerpilot.knowledge.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class AdminKnowledgeBatchActionRequest {
    @NotEmpty(message = "docIds is required")
    private List<Long> docIds;
}
