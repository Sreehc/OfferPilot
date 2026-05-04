package com.bytecoach.notification.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationVO {
    private Long id;
    private String type;
    private String title;
    private String content;
    private String link;
    private Boolean isRead;
    private LocalDateTime createTime;
}
