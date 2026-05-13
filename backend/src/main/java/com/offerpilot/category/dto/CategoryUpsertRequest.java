package com.offerpilot.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryUpsertRequest {
    private Long id;

    @NotBlank(message = "cannot be blank")
    private String name;

    @NotBlank(message = "cannot be blank")
    private String type;

    private Integer sortOrder;

    private Integer status;
}
