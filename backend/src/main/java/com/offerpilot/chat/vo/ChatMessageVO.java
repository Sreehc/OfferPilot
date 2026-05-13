package com.offerpilot.chat.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageVO {
    private Long id;
    private String role;
    private String messageType;
    private String content;
    private LocalDateTime createTime;
    private List<ChatMessageReferenceVO> references;
}
