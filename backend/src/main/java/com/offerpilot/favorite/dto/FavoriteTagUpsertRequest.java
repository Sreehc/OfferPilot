package com.offerpilot.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FavoriteTagUpsertRequest {
    @NotBlank
    private String name;
    private Integer sortOrder;
}
