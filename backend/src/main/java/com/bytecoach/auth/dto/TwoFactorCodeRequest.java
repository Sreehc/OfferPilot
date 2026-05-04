package com.bytecoach.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorCodeRequest {
    @NotBlank(message = "code cannot be blank")
    private String code;
}
