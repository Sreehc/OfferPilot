package com.bytecoach.chat.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageReferenceVO {
    private Long docId;
    private String docTitle;
    private Long chunkId;
    private String snippet;
    private Float score;
}

