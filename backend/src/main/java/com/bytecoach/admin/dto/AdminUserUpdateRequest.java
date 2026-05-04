package com.bytecoach.admin.dto;

import lombok.Data;

@Data
public class AdminUserUpdateRequest {
    private String nickname;
    private String role;
    private Integer status;
}
