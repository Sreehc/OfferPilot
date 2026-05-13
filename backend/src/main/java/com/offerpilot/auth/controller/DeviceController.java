package com.offerpilot.auth.controller;

import com.offerpilot.auth.dto.DeviceVO;
import com.offerpilot.auth.service.DeviceService;
import com.offerpilot.common.api.Result;
import com.offerpilot.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "设备管理", description = "登录设备查看、撤销")
@RestController
@RequestMapping("/api/auth/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "获取已登录设备列表")
    @GetMapping
    public Result<List<DeviceVO>> listDevices(
            @RequestHeader(value = "X-Device-Id", required = false) Long currentDeviceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(deviceService.listDevices(userId, currentDeviceId));
    }

    @Operation(summary = "撤销指定设备")
    @PostMapping("/{id}/revoke")
    public Result<Void> revokeDevice(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        deviceService.revokeDevice(userId, id);
        return Result.success();
    }

    @Operation(summary = "撤销除当前设备外的所有设备")
    @PostMapping("/revoke-all")
    public Result<Void> revokeAllOtherDevices(
            @RequestHeader(value = "X-Device-Id", required = false) Long currentDeviceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        deviceService.revokeAllOtherDevices(userId, currentDeviceId);
        return Result.success();
    }
}
