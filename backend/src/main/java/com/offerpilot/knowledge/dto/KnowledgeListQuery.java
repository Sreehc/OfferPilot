package com.offerpilot.knowledge.dto;

import com.offerpilot.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeListQuery extends PageQuery {
    private Long categoryId;
    private String keyword;
    private String status;
}

