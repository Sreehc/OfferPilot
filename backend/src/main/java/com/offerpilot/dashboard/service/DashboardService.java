package com.offerpilot.dashboard.service;

import com.offerpilot.dashboard.dto.DashboardOverviewVO;

public interface DashboardService {
    DashboardOverviewVO overview();

    void evictCache(Long userId);
}

