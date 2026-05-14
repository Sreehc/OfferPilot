package com.offerpilot.chat.vo;

import com.offerpilot.common.vo.ContextSourceVO;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSessionVO {
    private Long id;
    private String title;
    private String mode;
    private String contextType;
    private String knowledgeScope;
    private ContextSourceVO contextSource;
    private LocalDateTime lastMessageTime;
    private LocalDateTime updateTime;
}
