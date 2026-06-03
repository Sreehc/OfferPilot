package com.offerpilot.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FavoriteUpsertRequest {
    @NotBlank
    private String targetType;
    @NotNull
    private Long targetId;
    private Long tagId;
}
