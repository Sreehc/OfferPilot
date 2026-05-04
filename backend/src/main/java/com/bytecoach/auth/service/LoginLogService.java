package com.bytecoach.auth.service;

import com.bytecoach.auth.dto.LoginLogVO;
import com.bytecoach.common.dto.PageResult;

public interface LoginLogService {

    /**
     * Record a login attempt (success or failure).
     * Also handles: IP geo resolution, anomaly detection (different city notification),
     * and consecutive failure locking.
     */
    void recordLogin(Long userId, String ip, String device, boolean success, String failReason);

    /**
     * Check if the account is currently locked due to consecutive failures.
     * Throws BusinessException if locked.
     */
    void checkAccountLock(String username);

    /**
     * Query login logs for a specific user (paginated).
     */
    PageResult<LoginLogVO> listByUser(Long userId, int pageNum, int pageSize);

    /**
     * Query all login logs (admin, paginated, with optional username filter).
     */
    PageResult<LoginLogVO> listAll(String keyword, int pageNum, int pageSize);
}
