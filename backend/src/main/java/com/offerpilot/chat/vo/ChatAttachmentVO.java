package com.offerpilot.chat.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatAttachmentVO {
    private String id;
    private String fileId;
    private String filename;
    private String contentType;
    private long size;
}
