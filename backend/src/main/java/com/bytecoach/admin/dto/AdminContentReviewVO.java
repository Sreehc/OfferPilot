package com.bytecoach.admin.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContentReviewVO {
    private Long id;
    private String type; // "question" or "answer"
    private Long userId;
    private String username;
    private String title;
    private String content;
    private LocalDateTime createTime;
}
