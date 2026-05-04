package com.bytecoach.admin.service;

import com.bytecoach.admin.dto.AdminUserDetailVO;
import com.bytecoach.admin.dto.AdminUserUpdateRequest;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.user.vo.UserInfoVO;

public interface AdminUserService {
    PageResult<UserInfoVO> listUsers(String keyword, String role, int pageNum, int pageSize);
    void updateUser(Long userId, AdminUserUpdateRequest request);
    void banUser(Long userId);
    void unbanUser(Long userId);
    AdminUserDetailVO getUserDetail(Long userId);
}
