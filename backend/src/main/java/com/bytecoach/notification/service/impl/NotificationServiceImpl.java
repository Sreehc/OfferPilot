package com.bytecoach.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.notification.entity.Notification;
import com.bytecoach.notification.mapper.NotificationMapper;
import com.bytecoach.notification.service.NotificationService;
import com.bytecoach.notification.vo.NotificationVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public void send(Long userId, String type, String title, String content, String link) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setLink(link);
        n.setIsRead(false);
        notificationMapper.insert(n);
        log.debug("Notification sent to user {}: {}", userId, title);
    }

    @Override
    @Transactional
    public void sendBatch(List<Long> userIds, String type, String title, String content, String link) {
        for (Long userId : userIds) {
            send(userId, type, title, content, link);
        }
    }

    @Override
    public PageResult<NotificationVO> list(Long userId, int pageNum, int pageSize) {
        Page<Notification> page = notificationMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime));

        List<NotificationVO> records = page.getRecords().stream()
                .map(n -> NotificationVO.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .title(n.getTitle())
                        .content(n.getContent())
                        .link(n.getLink())
                        .isRead(n.getIsRead())
                        .createTime(n.getCreateTime())
                        .build())
                .toList();

        return PageResult.<NotificationVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                .build();
    }

    @Override
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, false));
    }

    @Override
    @Transactional
    public void markRead(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .in(Notification::getId, ids)
                .set(Notification::getIsRead, true));
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true));
    }
}
