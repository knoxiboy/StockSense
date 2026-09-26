package com.stocksense.auth.dto;

public class AuthResponse {
    private String token;
    private UserProfileResponse user;

    public AuthResponse() {
    }

    public AuthResponse(String token, UserProfileResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserProfileResponse getUser() {
        return user;
    }

    public void setUser(UserProfileResponse user) {
        this.user = user;
    }
}
