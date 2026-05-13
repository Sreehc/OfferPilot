package com.offerpilot.admin.service;

import com.offerpilot.admin.dto.AdminUserDetailVO;
import com.offerpilot.admin.dto.AdminUserUpdateRequest;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.user.vo.UserInfoVO;

public interface AdminUserService {
    PageResult<UserInfoVO> listUsers(String keyword, String role, int pageNum, int pageSize);
    void updateUser(Long userId, AdminUserUpdateRequest request);
    void banUser(Long userId);
    void unbanUser(Long userId);
    AdminUserDetailVO getUserDetail(Long userId);
}
