package com.offerpilot.auth.dto;

import com.offerpilot.user.vo.UserInfoVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private UserInfoVO userInfo;
    private Long deviceId;

    /** If true, the user must complete 2FA verification before getting the real token */
    private Boolean requires2fa;
    /** Temporary token for 2FA verification (only set when requires2fa=true) */
    private String tempToken;
}
