package com.bytecoach.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bytecoach.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("notification")
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String link;
    private Boolean isRead;
}
