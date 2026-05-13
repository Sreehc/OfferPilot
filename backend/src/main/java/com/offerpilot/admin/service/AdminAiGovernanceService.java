package com.offerpilot.admin.service;

import com.offerpilot.admin.dto.AdminAiLogSummaryVO;
import com.offerpilot.admin.dto.AdminAiLogVO;
import com.offerpilot.admin.dto.AdminSystemConfigVO;
import com.offerpilot.admin.dto.AdminSystemConfigUpdateRequest;
import com.offerpilot.common.dto.PageResult;
import java.util.List;

public interface AdminAiGovernanceService {
    PageResult<AdminAiLogVO> listAiLogs(String scene, String callType, Integer success, String keyword, int pageNum, int pageSize);

    AdminAiLogSummaryVO aiLogSummary();

    List<AdminSystemConfigVO> listSystemConfigs();

    AdminSystemConfigVO updateSystemConfig(String configKey, AdminSystemConfigUpdateRequest request);
}
