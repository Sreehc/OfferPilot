package com.bytecoach.community.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bytecoach.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("community_answer")
@EqualsAndHashCode(callSuper = true)
public class CommunityAnswer extends BaseEntity {
    private Long questionId;
    private Long userId;
    private String content;
    private Integer isAccepted;
    private Integer upvoteCount;
}
