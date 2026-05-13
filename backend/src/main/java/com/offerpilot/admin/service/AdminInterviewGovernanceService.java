package com.offerpilot.admin.service;

import com.offerpilot.admin.dto.AdminInterviewGovernanceSummaryVO;
import com.offerpilot.admin.dto.AdminInterviewGovernanceVO;
import com.offerpilot.common.dto.PageResult;

public interface AdminInterviewGovernanceService {
    PageResult<AdminInterviewGovernanceVO> list(String status, String mode, String keyword, int pageNum, int pageSize);

    AdminInterviewGovernanceSummaryVO summary();
}
