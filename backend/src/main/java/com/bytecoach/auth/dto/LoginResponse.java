package com.bytecoach.auth.dto;

import com.bytecoach.user.vo.UserInfoVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private UserInfoVO userInfo;
    private Long deviceId;
}

