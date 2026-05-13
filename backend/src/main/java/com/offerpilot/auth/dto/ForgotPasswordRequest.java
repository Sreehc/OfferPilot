package com.offerpilot.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "cannot be blank")
    @Email(message = "invalid email")
    @Size(max = 128, message = "length must be less than or equal to 128")
    private String email;
}
