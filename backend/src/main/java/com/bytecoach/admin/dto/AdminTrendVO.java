package com.bytecoach.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTrendVO {
    private String date;
    private long newUsers;
    private long activeUsers;
}
