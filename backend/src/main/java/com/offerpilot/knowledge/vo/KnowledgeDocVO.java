package com.offerpilot.knowledge.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeDocVO {
    private Long id;
    private String title;
    private Long categoryId;
    private String categoryName;
    private String libraryScope;
    private String businessType;
    private String sourceType;
    private String fileType;
    private String fileUrl;
    private String parseStatus;
    private String indexStatus;
    private String status;
    private String summary;
    private Integer chunkCount;
    private Long cardDeckId;
    private String cardDeckTitle;
    private Integer cardCount;
    private LocalDateTime cardGeneratedAt;
    private String cardTypes;
    private LocalDateTime updateTime;
}
