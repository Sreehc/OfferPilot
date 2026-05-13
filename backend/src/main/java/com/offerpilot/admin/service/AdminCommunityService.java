package com.offerpilot.admin.service;

import com.offerpilot.admin.dto.AdminContentReviewVO;
import com.offerpilot.common.dto.PageResult;

public interface AdminCommunityService {
    PageResult<AdminContentReviewVO> listPending(int pageNum, int pageSize);
    void approve(Long id);
    void reject(Long id, String reason);
}
