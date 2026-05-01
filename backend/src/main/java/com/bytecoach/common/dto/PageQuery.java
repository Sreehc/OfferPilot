package com.bytecoach.common.dto;

import lombok.Data;

@Data
public class PageQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 20;

    public int getOffset() {
        return (Math.max(pageNum, 1) - 1) * Math.max(pageSize, 1);
    }

    public int getLimit() {
        return Math.max(pageSize, 1);
    }
}
