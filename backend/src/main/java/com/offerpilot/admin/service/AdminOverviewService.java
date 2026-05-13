package com.offerpilot.admin.service;

import com.offerpilot.admin.dto.AdminOverviewVO;
import com.offerpilot.admin.dto.AdminTrendVO;
import java.util.List;

public interface AdminOverviewService {
    AdminOverviewVO getOverview();
    List<AdminTrendVO> getTrend();
}
