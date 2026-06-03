package com.offerpilot.dashboard.service;

import com.offerpilot.dashboard.dto.DashboardOverviewVO;

public interface DashboardService {
    DashboardOverviewVO overview();

    DashboardOverviewVO overview(Long userId);

    void evictCache(Long userId);
}
