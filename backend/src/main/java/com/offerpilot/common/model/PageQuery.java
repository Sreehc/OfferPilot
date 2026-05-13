package com.offerpilot.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {

    @Min(value = 1, message = "must be greater than 0")
    private long current = 1;

    @Min(value = 1, message = "must be greater than 0")
    @Max(value = 100, message = "must be less than or equal to 100")
    private long size = 10;
}

