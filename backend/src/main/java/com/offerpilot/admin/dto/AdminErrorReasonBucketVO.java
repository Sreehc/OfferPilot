package com.offerpilot.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminErrorReasonBucketVO {
    private String reason;
    private long count;
}
