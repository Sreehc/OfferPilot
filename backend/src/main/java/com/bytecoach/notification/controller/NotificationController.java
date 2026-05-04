package com.bytecoach.notification.controller;

import com.bytecoach.common.api.Result;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.notification.service.NotificationService;
import com.bytecoach.notification.vo.NotificationVO;
import com.bytecoach.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "消息通知", description = "用户通知管理")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "通知列表", description = "分页查询当前用户的通知")
    @GetMapping
    public Result<PageResult<NotificationVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(notificationService.list(currentUserId(), pageNum, pageSize));
    }

    @Operation(summary = "未读数量", description = "获取当前用户的未读通知数量")
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.success(Map.of("count", notificationService.unreadCount(currentUserId())));
    }

    @Operation(summary = "标记已读", description = "批量标记指定通知为已读")
    @PostMapping("/read")
    public Result<Void> markRead(@RequestBody List<Long> ids) {
        notificationService.markRead(currentUserId(), ids);
        return Result.success();
    }

    @Operation(summary = "全部已读", description = "标记所有通知为已读")
    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(currentUserId());
        return Result.success();
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
