package com.offerpilot.favorite.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FavoriteTagVO {
    private Long id;
    private String name;
    private Integer count;
    private Integer sortOrder;
}
