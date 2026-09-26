package com.stocksense.auth;

public final class UserContext {

    private static final ThreadLocal<TokenService.TokenPayload> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setCurrentUser(TokenService.TokenPayload payload) {
        CURRENT_USER.set(payload);
    }

    public static TokenService.TokenPayload getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
