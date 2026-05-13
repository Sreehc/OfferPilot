package com.offerpilot.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("chat_message")
@EqualsAndHashCode(callSuper = true)
public class ChatMessage extends BaseEntity {
    private Long sessionId;
    private Long userId;
    private String role;
    private String messageType;
    private String content;
    private String referenceJson;
}
