package com.offerpilot.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("chat_session")
@EqualsAndHashCode(callSuper = true)
public class ChatSession extends BaseEntity {
    private Long userId;
    private String title;
    private String mode;
    private String contextType;
    private String knowledgeScope;
    private Long resumeFileId;
    private Long resumeProjectId;
    private LocalDateTime lastMessageTime;
}
