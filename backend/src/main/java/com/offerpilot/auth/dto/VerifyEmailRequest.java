package com.offerpilot.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyEmailRequest {

    @NotBlank(message = "cannot be blank")
    @Size(min = 4, max = 12, message = "length must be between 4 and 12")
    private String code;
}
