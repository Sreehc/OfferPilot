package com.bytecoach.dashboard.service;

import com.bytecoach.dashboard.dto.DashboardOverviewVO;

public interface DashboardService {
    DashboardOverviewVO overview();

    void evictCache(Long userId);
}

