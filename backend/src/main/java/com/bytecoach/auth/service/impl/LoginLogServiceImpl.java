package com.bytecoach.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bytecoach.auth.dto.LoginLogVO;
import com.bytecoach.auth.entity.LoginLog;
import com.bytecoach.auth.mapper.LoginLogMapper;
import com.bytecoach.auth.service.LoginLogService;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.config.ByteCoachProperties;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.notification.service.NotificationService;
import com.bytecoach.user.entity.User;
import com.bytecoach.user.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private static final String LOCK_PREFIX = "login:lock:";
    private static final String FAIL_COUNT_PREFIX = "login:fail:";

    private final LoginLogMapper loginLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ByteCoachProperties properties;

    @Override
    @Transactional
    public void recordLogin(Long userId, String ip, String device, boolean success, String failReason) {
        // Resolve city from IP (best effort)
        String city = resolveCity(ip);

        // Write login log
        LoginLog logEntry = new LoginLog();
        logEntry.setUserId(userId);
        logEntry.setIp(ip);
        logEntry.setCity(city);
        logEntry.setDevice(device);
        logEntry.setStatus(success ? 1 : 0);
        logEntry.setFailReason(failReason);
        loginLogMapper.insert(logEntry);

        ByteCoachProperties.LoginSecurity loginSecurity = properties.getLoginSecurity();

        if (success) {
            // Clear fail count on success
            redisTemplate.delete(FAIL_COUNT_PREFIX + userId);

            // Anomaly detection: compare with last successful login city
            try {
                checkAnomalyLogin(userId, city);
            } catch (Exception e) {
                log.warn("Anomaly detection failed for user {}: {}", userId, e.getMessage());
            }
        } else {
            // Increment fail count
            String failKey = FAIL_COUNT_PREFIX + userId;
            Long count = redisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1) {
                // Set TTL for the fail count window (5 minutes)
                redisTemplate.expire(failKey, loginSecurity.getFailWindowMinutes(), java.util.concurrent.TimeUnit.MINUTES);
            }

            // Check if we should lock the account
            if (count != null && count >= loginSecurity.getFailLimit()) {
                String lockKey = LOCK_PREFIX + userId;
                redisTemplate.opsForValue().set(lockKey, "1",
                        loginSecurity.getLockMinutes(), java.util.concurrent.TimeUnit.MINUTES);
                log.warn("Account locked for user {} due to {} consecutive failures", userId, count);
            }
        }
    }

    @Override
    public void checkAccountLock(String username) {
        ByteCoachProperties.LoginSecurity loginSecurity = properties.getLoginSecurity();
        // Look up user by username to get userId
        User user = userService.getByUsername(username);
        if (user == null) {
            return; // Let auth handle user-not-found
        }
        String lockKey = LOCK_PREFIX + user.getId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            Long ttl = redisTemplate.getExpire(lockKey, java.util.concurrent.TimeUnit.MINUTES);
            String msg = "账号已被锁定，请 " + (ttl != null ? ttl : loginSecurity.getLockMinutes()) + " 分钟后再试";
            throw new BusinessException(423, msg);
        }
    }

    @Override
    public PageResult<LoginLogVO> listByUser(Long userId, int pageNum, int pageSize) {
        Page<LoginLog> page = loginLogMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<LoginLog>()
                        .eq(LoginLog::getUserId, userId)
                        .orderByDesc(LoginLog::getCreateTime));

        List<LoginLogVO> records = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.<LoginLogVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                .build();
    }

    @Override
    public PageResult<LoginLogVO> listAll(String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();

        // If keyword provided, find matching user IDs first
        if (keyword != null && !keyword.isBlank()) {
            List<Long> userIds = userService.searchUserIds(keyword);
            if (userIds.isEmpty()) {
                return PageResult.<LoginLogVO>builder()
                        .records(List.of())
                        .total(0)
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .totalPages(0)
                        .build();
            }
            wrapper.in(LoginLog::getUserId, userIds);
        }

        wrapper.orderByDesc(LoginLog::getCreateTime);

        Page<LoginLog> page = loginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<LoginLogVO> records = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.<LoginLogVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                .build();
    }

    /**
     * Anomaly detection: if the city of the current successful login differs from
     * the last successful login, send a notification to the user.
     */
    private void checkAnomalyLogin(Long userId, String currentCity) {
        if (currentCity == null || currentCity.isBlank()) {
            return;
        }

        // Find the previous successful login (skip the one we just inserted)
        LoginLog lastLogin = loginLogMapper.selectOne(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, userId)
                .eq(LoginLog::getStatus, 1)
                .orderByDesc(LoginLog::getCreateTime)
                .last("LIMIT 1 OFFSET 1"));

        if (lastLogin == null || lastLogin.getCity() == null) {
            return;
        }

        if (!currentCity.equals(lastLogin.getCity())) {
            notificationService.send(userId, "login_anomaly",
                    "异地登录提醒",
                    "检测到您的账号在 " + currentCity + " 登录，上次登录城市为 " + lastLogin.getCity() + "。如非本人操作，请及时修改密码。",
                    "/settings/devices");
            log.info("Anomaly login detected for user {}: {} -> {}", userId, lastLogin.getCity(), currentCity);
        }
    }

    /**
     * Resolve city from IP address.
     * Uses a simple approach: calls a free IP geolocation API.
     * Falls back gracefully if the service is unavailable.
     */
    private String resolveCity(String ip) {
        if (ip == null || ip.isBlank() || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)
                || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return "本机";
        }
        try {
            // Use ip-api.com free tier (no key needed, 45 req/min limit)
            java.net.URL url = new java.net.URL("http://ip-api.com/json/" + ip + "?lang=zh-CN&fields=city");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            if (conn.getResponseCode() == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()))) {
                    String response = reader.readLine();
                    if (response != null && response.contains("\"city\"")) {
                        int start = response.indexOf("\"city\":\"") + 8;
                        int end = response.indexOf("\"", start);
                        if (start > 7 && end > start) {
                            return response.substring(start, end);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("IP geo resolution failed for {}: {}", ip, e.getMessage());
        }
        return null;
    }

    private LoginLogVO toVO(LoginLog logEntry) {
        // Try to resolve username
        String username = null;
        try {
            User user = userService.getById(logEntry.getUserId());
            if (user != null) {
                username = user.getUsername();
            }
        } catch (Exception ignored) {
            // ignore
        }

        return LoginLogVO.builder()
                .id(logEntry.getId())
                .userId(logEntry.getUserId())
                .username(username)
                .ip(logEntry.getIp())
                .city(logEntry.getCity())
                .device(logEntry.getDevice())
                .status(logEntry.getStatus())
                .failReason(logEntry.getFailReason())
                .createTime(logEntry.getCreateTime())
                .build();
    }
}
