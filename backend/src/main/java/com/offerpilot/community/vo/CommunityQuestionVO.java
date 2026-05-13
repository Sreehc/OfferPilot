package com.offerpilot.community.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class CommunityQuestionVO {
    private Long id;
    private Long userId;
    private String authorName;
    private String authorRank;
    private String title;
    private String content;
    private Long categoryId;
    private String categoryName;
    private String status;
    private Integer upvoteCount;
    private Integer answerCount;
    private Boolean accepted;
    private Boolean hasVoted;
    private LocalDateTime createdAt;
    private List<CommunityAnswerVO> answers;
}
