package com.homemakers.homemakers.dto;

public class LoginResponse {

    private String accessToken;
    private String refreshToken; // internal only
    private String email;
    private String role;

    public LoginResponse(String accessToken, String refreshToken, String email, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
        this.role = role;
    }

    // ✅ getters
    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    // ✅ setter (needed to null it before response)
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
