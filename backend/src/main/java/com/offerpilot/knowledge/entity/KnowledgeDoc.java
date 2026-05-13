package com.offerpilot.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("knowledge_doc")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDoc extends BaseEntity {
    private String title;
    private Long categoryId;
    private Long userId;
    private String sourceType;
    private String fileUrl;
    private String summary;
    private String status;
}
