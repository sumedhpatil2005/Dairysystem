package com.dairy.dairy_management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
