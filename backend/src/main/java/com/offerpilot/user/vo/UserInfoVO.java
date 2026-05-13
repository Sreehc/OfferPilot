package com.offerpilot.user.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private Boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private Boolean githubLinked;
    private String githubUsername;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
}
