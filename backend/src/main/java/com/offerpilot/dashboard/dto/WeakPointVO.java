package com.offerpilot.dashboard.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class WeakPointVO {
    private String categoryName;
    private Integer wrongCount;
    private BigDecimal score;
}

