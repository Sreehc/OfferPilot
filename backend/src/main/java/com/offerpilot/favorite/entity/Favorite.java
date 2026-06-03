package com.offerpilot.favorite.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("favorite")
@EqualsAndHashCode(callSuper = true)
public class Favorite extends BaseEntity {
    private Long userId;
    private String targetType;
    private Long targetId;
    private Long tagId;
}
