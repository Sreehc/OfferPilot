package com.offerpilot.community.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CommunityAnswerVO {
    private Long id;
    private Long questionId;
    private Long userId;
    private String authorName;
    private String authorRank;
    private String content;
    private Boolean isAccepted;
    private Integer upvoteCount;
    private Boolean hasVoted;
    private LocalDateTime createdAt;
}
