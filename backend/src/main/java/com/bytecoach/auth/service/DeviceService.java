package com.bytecoach.auth.service;

import com.bytecoach.auth.dto.DeviceVO;
import java.util.List;

public interface DeviceService {

    /**
     * Record or update device info on login.
     * Returns the device ID (existing or newly created).
     */
    Long recordDevice(Long userId, String deviceFingerprint, String deviceName, String ip, String city);

    /**
     * List all active devices for the user, sorted by last active time desc.
     */
    List<DeviceVO> listDevices(Long userId, Long currentDeviceId);

    /**
     * Revoke a single device (mark inactive + blacklist its token).
     */
    void revokeDevice(Long userId, Long deviceId);

    /**
     * Revoke all devices except the given current device.
     */
    void revokeAllOtherDevices(Long userId, Long currentDeviceId);
}
