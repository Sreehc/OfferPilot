package com.bytecoach.knowledge.dto;

import com.bytecoach.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeListQuery extends PageQuery {
    private Long categoryId;
    private String keyword;
    private String status;
}

