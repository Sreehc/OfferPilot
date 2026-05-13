package com.offerpilot.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("login_device")
@EqualsAndHashCode(callSuper = true)
public class LoginDevice extends BaseEntity {
    private Long userId;
    private String deviceFingerprint;
    private String deviceName;
    private String ip;
    private String city;
    private LocalDateTime lastActiveTime;
    private Integer status;
}
