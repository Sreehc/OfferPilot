package com.offerpilot.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "cannot be blank")
    @Email(message = "invalid email")
    @Size(max = 128, message = "length must be less than or equal to 128")
    private String email;

    @NotBlank(message = "cannot be blank")
    @Size(min = 4, max = 12, message = "length must be between 4 and 12")
    private String code;

    @NotBlank(message = "cannot be blank")
    @Size(min = 6, max = 128, message = "length must be between 6 and 128")
    private String newPassword;
}
