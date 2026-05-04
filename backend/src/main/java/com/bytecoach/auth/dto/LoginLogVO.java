package com.bytecoach.auth.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogVO {
    private Long id;
    private Long userId;
    private String username;
    private String ip;
    private String city;
    private String device;
    private Integer status;
    private String failReason;
    private LocalDateTime createTime;
}
