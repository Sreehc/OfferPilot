package com.offerpilot.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeImportRequest {
    @NotBlank(message = "cannot be blank")
    private String seedKey;

    private Long categoryId;

    private Boolean forceRebuild;
}
