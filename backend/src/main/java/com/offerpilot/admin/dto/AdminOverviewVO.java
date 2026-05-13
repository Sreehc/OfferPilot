package com.offerpilot.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewVO {
    private long totalUsers;
    private long todayActive;
    private long todayNew;
    private long totalInterviews;
    private long totalReviews;
}
