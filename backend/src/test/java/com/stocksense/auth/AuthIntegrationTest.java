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

    @Test
    void testAuthLifecycle_Register_Login_Profile() throws Exception {
        String email = "testuser_" + System.currentTimeMillis() + "@stocksense.io";
        RegisterRequest registerReq = new RegisterRequest(email, "SecurePassword123!", "Warehouse Manager");

        String registerRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn().getResponse().getContentAsString();

        // Verify password is NOT stored in plaintext in the database
        User savedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("SecurePassword123!");
        assertThat(new BCryptPasswordEncoder().matches("SecurePassword123!", savedUser.getPasswordHash())).isTrue();

        // Login with correct password
        LoginRequest loginReq = new LoginRequest(email, "SecurePassword123!");
        String loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginRes).get("token").asText();

        // Login with wrong password rejected
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "WrongPass"))))
                .andExpect(status().isConflict());

        // Access protected profile with token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Warehouse Manager"));

        // Access protected profile without token rejected
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isConflict());
    }

    @Test
    void testPasswordReset_OtpLifecycle() throws Exception {
        String email = "otpuser_" + System.currentTimeMillis() + "@stocksense.io";
        authService.register(new RegisterRequest(email, "InitialPass123", "Operator"));

        // Request OTP
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk());

        // Find the generated token in repo to inspect hash & simulate user receiving OTP
        PasswordResetToken tokenEntity = passwordResetTokenRepository
                .findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email).orElseThrow();

        assertThat(tokenEntity.isUsed()).isFalse();

        // Try resetting with wrong OTP
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(email, "000000", "NewPass123!"))))
                .andExpect(status().isConflict());

        // To test successful reset, let's create a known OTP
        String testOtp = "789123";
        String hashedOtp = new BCryptPasswordEncoder().encode(testOtp);
        tokenEntity.setOtpHash(hashedOtp);
        passwordResetTokenRepository.save(tokenEntity);

        // Reset with correct OTP
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(email, testOtp, "BrandNewPass456!"))))
                .andExpect(status().isOk());

        // Invariance: Token is now used and cannot be reused
        PasswordResetToken updatedToken = passwordResetTokenRepository.findById(tokenEntity.getId()).orElseThrow();
        assertThat(updatedToken.isUsed()).isTrue();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(email, testOtp, "AnotherPass789!"))))
                .andExpect(status().isConflict());

        // Login with new password
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "BrandNewPass456!"))))
                .andExpect(status().isOk());
    }
}
