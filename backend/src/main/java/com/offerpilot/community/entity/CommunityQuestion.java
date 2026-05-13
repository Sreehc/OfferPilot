package com.offerpilot.community.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("community_question")
@EqualsAndHashCode(callSuper = true)
public class CommunityQuestion extends BaseEntity {
    private Long userId;
    private String title;
    private String content;
    private Long categoryId;
    private String status;
    private Integer upvoteCount;
    private Integer answerCount;
}
