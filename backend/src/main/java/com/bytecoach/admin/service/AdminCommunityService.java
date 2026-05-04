package com.bytecoach.admin.service;

import com.bytecoach.admin.dto.AdminContentReviewVO;
import com.bytecoach.common.dto.PageResult;

public interface AdminCommunityService {
    PageResult<AdminContentReviewVO> listPending(int pageNum, int pageSize);
    void approve(Long id);
    void reject(Long id, String reason);
}
