package com.bytecoach.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bytecoach.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("user")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String role;
    private Integer status;
    private String source;
    private String remark;
    private java.time.LocalDateTime lastLoginTime;
    private String totpSecret;
    private Boolean totpEnabled;
    private String recoveryCodes;
}
