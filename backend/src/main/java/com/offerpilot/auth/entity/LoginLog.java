package com.offerpilot.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("login_log")
@EqualsAndHashCode(callSuper = true)
public class LoginLog extends BaseEntity {
    private Long userId;
    private String ip;
    private String city;
    private String device;
    /** 1=成功, 0=失败 */
    private Integer status;
    private String failReason;
}
