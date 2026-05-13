package com.offerpilot.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("knowledge_chunk")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeChunk extends BaseEntity {
    private Long docId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private String vectorId;
}
