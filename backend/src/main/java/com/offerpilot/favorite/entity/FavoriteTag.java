package com.offerpilot.favorite.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("favorite_tag")
@EqualsAndHashCode(callSuper = true)
public class FavoriteTag extends BaseEntity {
    private Long userId;
    private String name;
    private Integer sortOrder;
}
