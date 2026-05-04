package com.bytecoach.notification.service;

import com.bytecoach.common.dto.PageResult;
import com.bytecoach.notification.vo.NotificationVO;
import java.util.List;

public interface NotificationService {
    void send(Long userId, String type, String title, String content, String link);
    void sendBatch(List<Long> userIds, String type, String title, String content, String link);
    PageResult<NotificationVO> list(Long userId, int pageNum, int pageSize);
    long unreadCount(Long userId);
    void markRead(Long userId, List<Long> ids);
    void markAllRead(Long userId);
}
