package com.dairy.dairy_management.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String refreshToken;
    private long expiresIn;
    private String role;
    private String username;

    public AuthResponse(String token, String refreshToken, long expiresIn, String role, String username) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.role = role;
        this.username = username;
    }
}
