package com.offerpilot.category.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryVO {
    private Long id;
    private String name;
    private String type;
    private Integer sortOrder;
    private Integer status;
}
