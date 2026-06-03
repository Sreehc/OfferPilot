package com.offerpilot.favorite.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FavoriteVO {
    private Long id;
    private String targetType;
    private Long targetId;
    private String title;
    private String summary;
    private String categoryName;
    private Long tagId;
    private String tagName;
    private LocalDateTime createTime;
}
