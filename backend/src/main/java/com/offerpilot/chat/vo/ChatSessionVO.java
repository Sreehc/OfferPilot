package com.offerpilot.chat.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSessionVO {
    private Long id;
    private String title;
    private String mode;
    private LocalDateTime lastMessageTime;
    private LocalDateTime updateTime;
}
