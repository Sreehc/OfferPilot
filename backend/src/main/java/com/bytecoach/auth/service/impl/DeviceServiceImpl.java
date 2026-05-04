package com.bytecoach.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.auth.dto.DeviceVO;
import com.bytecoach.auth.entity.LoginDevice;
import com.bytecoach.auth.mapper.LoginDeviceMapper;
import com.bytecoach.auth.service.DeviceService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private static final String DEVICE_TOKEN_PREFIX = "device:token:";

    private final LoginDeviceMapper deviceMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public Long recordDevice(Long userId, String deviceFingerprint, String deviceName, String ip, String city) {
        // Look for existing active device with same fingerprint
        LoginDevice existing = deviceMapper.selectOne(new LambdaQueryWrapper<LoginDevice>()
                .eq(LoginDevice::getUserId, userId)
                .eq(LoginDevice::getDeviceFingerprint, deviceFingerprint)
                .eq(LoginDevice::getStatus, 1)
                .last("LIMIT 1"));

        if (existing != null) {
            existing.setLastActiveTime(LocalDateTime.now());
            existing.setDeviceName(deviceName);
            existing.setIp(ip);
            if (city != null) {
                existing.setCity(city);
            }
            deviceMapper.updateById(existing);
            return existing.getId();
        }

        LoginDevice device = new LoginDevice();
        device.setUserId(userId);
        device.setDeviceFingerprint(deviceFingerprint);
        device.setDeviceName(deviceName);
        device.setIp(ip);
        device.setCity(city);
        device.setLastActiveTime(LocalDateTime.now());
        device.setStatus(1);
        deviceMapper.insert(device);
        return device.getId();
    }

    @Override
    public List<DeviceVO> listDevices(Long userId, Long currentDeviceId) {
        List<LoginDevice> devices = deviceMapper.selectList(new LambdaQueryWrapper<LoginDevice>()
                .eq(LoginDevice::getUserId, userId)
                .eq(LoginDevice::getStatus, 1)
                .orderByDesc(LoginDevice::getLastActiveTime));

        List<DeviceVO> result = new ArrayList<>();
        for (LoginDevice d : devices) {
            result.add(DeviceVO.builder()
                    .id(d.getId())
                    .deviceFingerprint(d.getDeviceFingerprint())
                    .deviceName(d.getDeviceName())
                    .ip(d.getIp())
                    .city(d.getCity())
                    .lastActiveTime(d.getLastActiveTime())
                    .createTime(d.getCreateTime())
                    .current(d.getId().equals(currentDeviceId))
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public void revokeDevice(Long userId, Long deviceId) {
        LoginDevice device = deviceMapper.selectOne(new LambdaQueryWrapper<LoginDevice>()
                .eq(LoginDevice::getId, deviceId)
                .eq(LoginDevice::getUserId, userId)
                .eq(LoginDevice::getStatus, 1));
        if (device == null) {
            return;
        }
        device.setStatus(0);
        deviceMapper.updateById(device);

        // Store revoked fingerprint in Redis so the JWT filter can reject requests from this device
        blacklistDeviceFingerprint(userId, device.getDeviceFingerprint());
        log.info("Revoked device {} for user {}", deviceId, userId);
    }

    @Override
    @Transactional
    public void revokeAllOtherDevices(Long userId, Long currentDeviceId) {
        // Get the current device fingerprint to preserve it
        String currentFingerprint = null;
        if (currentDeviceId != null) {
            LoginDevice current = deviceMapper.selectById(currentDeviceId);
            if (current != null) {
                currentFingerprint = current.getDeviceFingerprint();
            }
        }

        List<LoginDevice> devices = deviceMapper.selectList(new LambdaQueryWrapper<LoginDevice>()
                .eq(LoginDevice::getUserId, userId)
                .eq(LoginDevice::getStatus, 1));

        for (LoginDevice d : devices) {
            if (currentDeviceId != null && d.getId().equals(currentDeviceId)) {
                continue;
            }
            d.setStatus(0);
            deviceMapper.updateById(d);
            blacklistDeviceFingerprint(userId, d.getDeviceFingerprint());
        }
        log.info("Revoked all other devices for user {}, kept device {}", userId, currentDeviceId);
    }

    private void blacklistDeviceFingerprint(Long userId, String fingerprint) {
        try {
            String key = DEVICE_TOKEN_PREFIX + userId + ":" + fingerprint;
            // Store with 7-day TTL (max token lifetime)
            redisTemplate.opsForValue().set(key, "revoked", 7 * 24 * 3600, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to blacklist device fingerprint in Redis: {}", e.getMessage());
        }
    }
}
