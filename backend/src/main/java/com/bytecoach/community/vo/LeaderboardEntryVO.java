package com.bytecoach.community.vo;

import lombok.Data;

@Data
public class LeaderboardEntryVO {
    private Long userId;
    private String username;
    private String rankTitle;
    private Integer communityScore;
    private Integer communityQuestions;
    private Integer communityAnswers;
    private Integer communityAccepted;
    private Integer position;
}
