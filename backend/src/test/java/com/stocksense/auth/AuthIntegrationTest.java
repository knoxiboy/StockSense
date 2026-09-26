package com.stocksense.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksense.auth.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenService tokenService;

    @Test
    void testAuthLifecycle_Register_Login_Profile() throws Exception {
        String email = "testuser_" + System.currentTimeMillis() + "@stocksense.io";
        RegisterRequest registerReq = new RegisterRequest(email, "SecurePassword123!", "Warehouse Worker");

        // 1. Successful Signup
        String registerRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("WORKER"))
                .andReturn().getResponse().getContentAsString();

        // 2. Duplicate account handling rejected with Conflict 409
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict());

        // Verify password is NOT stored in plaintext
        User savedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("SecurePassword123!");
        assertThat(new BCryptPasswordEncoder().matches("SecurePassword123!", savedUser.getPasswordHash())).isTrue();

        // 3. Successful Login
        LoginRequest loginReq = new LoginRequest(email, "SecurePassword123!");
        String loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginRes).get("token").asText();

        // 4. Login with incorrect password returns 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "WrongPassword!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        // 5. Login with unknown user returns 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("nonexistent@stocksense.io", "Pass123456"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        // 6. Access protected endpoint with valid token returns 200 OK
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Warehouse Worker"));

        // 7. Access protected endpoint without token returns 401 Unauthorized
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // 8. Access protected inventory endpoint without token returns 401 Unauthorized
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());

        // 9. Access protected inventory endpoint with valid token returns 200 OK
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 10. Access with expired or invalid/tampered token returns 401 Unauthorized
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.tampered.token"))
                .andExpect(status().isUnauthorized());

        // 11. Logout endpoint
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void testRoleBasedAccess_WorkerVsManager() throws Exception {
        String workerToken = tokenService.generateToken(99L, "worker_rbac@stocksense.io", "WORKER");
        String managerToken = tokenService.generateToken(1L, "manager_rbac@stocksense.io", "MANAGER");

        // Worker can access GET endpoints (dashboard, products)
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk());

        // Worker attempting manager-only endpoint (e.g. POST /api/warehouses) is FORBIDDEN 403
        mockMvc.perform(post("/api/warehouses")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test WH\",\"code\":\"WH-RBAC-DENY\",\"address\":\"Zone 1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: Insufficient privileges. MANAGER role required."));

        // Manager accessing same endpoint is allowed past auth (proceeds to controller)
        mockMvc.perform(post("/api/warehouses")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Manager WH\",\"code\":\"WH-RBAC-ALLOW-" + System.nanoTime() + "\",\"address\":\"Zone 2\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void testPasswordReset_OtpLifecycle() throws Exception {
        String email = "otpuser_" + System.currentTimeMillis() + "@stocksense.io";
        authService.register(new RegisterRequest(email, "InitialPass123", "Operator"));

        // 1. Request OTP
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk());

        PasswordResetToken tokenEntity = passwordResetTokenRepository
                .findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email).orElseThrow();
        assertThat(tokenEntity.isUsed()).isFalse();

        // 2. Verify with wrong OTP fails (401)
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest(email, "000000"))))
                .andExpect(status().isUnauthorized());

        // Configure known OTP to test successful verification
        String testOtp = "654321";
        tokenEntity.setOtpHash(new BCryptPasswordEncoder().encode(testOtp));
        passwordResetTokenRepository.save(tokenEntity);

        // 3. Verify OTP with correct code succeeds
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest(email, testOtp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code verified successfully."));

        // 4. Reset password with new password
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(email, testOtp, "BrandNewPass456!"))))
                .andExpect(status().isOk());

        // 5. One-time use invariant: token is marked used and cannot be reused
        PasswordResetToken updatedToken = passwordResetTokenRepository.findById(tokenEntity.getId()).orElseThrow();
        assertThat(updatedToken.isUsed()).isTrue();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(email, testOtp, "AnotherPass789!"))))
                .andExpect(status().isUnauthorized());

        // 6. Login with new password succeeds
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "BrandNewPass456!"))))
                .andExpect(status().isOk());
    }

    @Test
    void testOtpExpiryHandling() throws Exception {
        String email = "expired_otp_" + System.currentTimeMillis() + "@stocksense.io";
        authService.register(new RegisterRequest(email, "Pass123456!", "Expiry Test User"));

        // Create expired OTP token
        PasswordResetToken expiredToken = new PasswordResetToken(
                email,
                new BCryptPasswordEncoder().encode("123456"),
                LocalDateTime.now().minusMinutes(5) // expired 5 minutes ago
        );
        passwordResetTokenRepository.save(expiredToken);

        // Verification of expired code returns 401
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest(email, "123456"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("The verification code has expired. Please request a new one."));
    }
}
