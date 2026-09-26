package com.stocksense.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class TokenService {

    private final String secretKey;
    private static final long EXPIRATION_SECONDS = 7 * 24 * 3600; // 7 days

    public TokenService(@Value("${stocksense.jwt.secret:StockSenseSuperSecretSigningKeyForJwtTokens2026!#}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String generateToken(Long userId, String email, String role) {
        long expiry = Instant.now().getEpochSecond() + EXPIRATION_SECONDS;
        String payload = userId + ":" + email + ":" + role + ":" + expiry;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    public TokenPayload parseAndVerifyToken(String token) {
        if (token == null || !token.contains(".")) {
            return null;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return null;
        }

        String encodedPayload = parts[0];
        String signature = parts[1];

        String expectedSignature = sign(encodedPayload);
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            String[] segments = payload.split(":");
            if (segments.length != 4) {
                return null;
            }

            Long userId = Long.parseLong(segments[0]);
            String email = segments[1];
            String role = segments[2];
            long expiry = Long.parseLong(segments[3]);

            if (Instant.now().getEpochSecond() > expiry) {
                return null; // Expired
            }

            return new TokenPayload(userId, email, role);
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);
            byte[] signedBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error signing auth token", e);
        }
    }

    public static class TokenPayload {
        private final Long userId;
        private final String email;
        private final String role;

        public TokenPayload(Long userId, String email, String role) {
            this.userId = userId;
            this.email = email;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }
    }
}
