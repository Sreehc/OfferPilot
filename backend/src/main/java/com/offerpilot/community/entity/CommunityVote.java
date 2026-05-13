package com.offerpilot.community.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("community_vote")
@EqualsAndHashCode(callSuper = true)
public class CommunityVote extends BaseEntity {
    private Long userId;
    private String targetType;
    private Long targetId;
    private Integer value;
}
