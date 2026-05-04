package com.bytecoach.admin.service;

import com.bytecoach.admin.dto.AdminOverviewVO;
import com.bytecoach.admin.dto.AdminTrendVO;
import java.util.List;

public interface AdminOverviewService {
    AdminOverviewVO getOverview();
    List<AdminTrendVO> getTrend();
}
