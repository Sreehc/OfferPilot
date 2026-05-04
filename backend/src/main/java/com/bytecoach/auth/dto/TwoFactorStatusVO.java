package com.bytecoach.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorStatusVO {
    private Boolean enabled;
    private Integer recoveryCodesRemaining;
}
