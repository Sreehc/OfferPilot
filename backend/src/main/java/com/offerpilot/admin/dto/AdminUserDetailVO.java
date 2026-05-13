package com.offerpilot.admin.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
    // Learning stats
    private Integer interviewCount;
    private Integer wrongCount;
    private Integer reviewCount;
    private Integer communityQuestions;
    private Integer communityAnswers;
}
