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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Test
    void testRegistration_WorkerRole_And_EmailOtpFlow() throws Exception {
        String email = "worker_" + System.currentTimeMillis() + "@stocksense.io";
        RegisterRequest registerReq = new RegisterRequest(email, "SecurePassword123!", "Warehouse Worker", "WORKER");

        // 1. Worker Registration creates pending unverified account
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("WORKER"))
                .andExpect(jsonPath("$.requestedRole").value("WORKER"))
                .andExpect(jsonPath("$.requiresEmailVerification").value(true))
                .andExpect(jsonPath("$.otp").doesNotExist()) // Never exposed in API response
                .andExpect(jsonPath("$.token").doesNotExist()); // No token before verification

        // 2. Duplicate registration rejected with 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict());

        // 3. User in database has BCrypt hash and is unverified
        User savedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("SecurePassword123!");
        assertThat(new BCryptPasswordEncoder().matches("SecurePassword123!", savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.isEmailVerified()).isFalse();
        assertThat(savedUser.isEnabled()).isTrue();

        // 4. Unverified user cannot log in
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "SecurePassword123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your email address is not verified. Please verify your email with the 6-digit verification code sent during registration."));

        // 5. Verify email verification token exists and is hashed
        EmailVerificationToken verifToken = emailVerificationTokenRepository
                .findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email).orElseThrow();
        assertThat(verifToken.isUsed()).isFalse();
        assertThat(verifToken.getOtpHash()).isNotEmpty();

        // 6. Verification with wrong OTP fails (401)
        mockMvc.perform(post("/api/auth/verify-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest(email, "000000"))))
                .andExpect(status().isUnauthorized());

        // 7. Verify with correct OTP succeeds and activates account
        String knownOtp = "789123";
        verifToken.setOtpHash(new BCryptPasswordEncoder().encode(knownOtp));
        emailVerificationTokenRepository.save(verifToken);

        mockMvc.perform(post("/api/auth/verify-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest(email, knownOtp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("WORKER"));

        // 8. One-time use invariant: token cannot be reused
        mockMvc.perform(post("/api/auth/verify-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest(email, knownOtp))))
                .andExpect(status().isUnauthorized());

        // 9. Login now succeeds with genuine PostgreSQL user
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "SecurePassword123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void testRegistration_ManagerSelection_RequiresApproval() throws Exception {
        String email = "mgr_req_" + System.currentTimeMillis() + "@stocksense.io";
        RegisterRequest registerReq = new RegisterRequest(email, "ManagerPass123!", "Manager Candidate", "MANAGER");

        // 1. Public registration requesting MANAGER NEVER assigns MANAGER directly.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("WORKER"))
                .andExpect(jsonPath("$.requestedRole").value("MANAGER"))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING_APPROVAL"));

        User savedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(User.ROLE_WORKER);
        assertThat(savedUser.getApprovalStatus()).isEqualTo(User.STATUS_PENDING_APPROVAL);

        // 2. Verify email with OTP
        EmailVerificationToken verifToken = emailVerificationTokenRepository
                .findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email).orElseThrow();
        String testOtp = "112233";
        verifToken.setOtpHash(new BCryptPasswordEncoder().encode(testOtp));
        emailVerificationTokenRepository.save(verifToken);

        // Verification succeeds, returning 200 OK with null token and pending approval status
        mockMvc.perform(post("/api/auth/verify-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest(email, testOtp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.user.approvalStatus").value("PENDING_APPROVAL"));

        // 3. Login attempt while pending approval is rejected (401)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "ManagerPass123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your Manager account request is pending administrator approval. You will receive access once approved."));
    }

    @Test
    void testManagerApprovalWorkflow() throws Exception {
        // Create an existing Manager
        String mgrEmail = "admin_approver_" + System.currentTimeMillis() + "@stocksense.io";
        User existingManager = new User(mgrEmail, new BCryptPasswordEncoder().encode("Admin123!"), "Lead Manager", User.ROLE_MANAGER);
        existingManager.setEmailVerified(true);
        existingManager = userRepository.save(existingManager);
        String managerToken = tokenService.generateToken(existingManager.getId(), mgrEmail, User.ROLE_MANAGER);

        // Create a Candidate requesting Manager access
        String candidateEmail = "candidate_" + System.currentTimeMillis() + "@stocksense.io";
        User candidate = new User(candidateEmail, new BCryptPasswordEncoder().encode("Candidate123!"), "Applicant", User.ROLE_WORKER, "MANAGER", User.STATUS_PENDING_APPROVAL);
        candidate.setEmailVerified(true);
        candidate = userRepository.save(candidate);

        // 1. Candidate cannot approve themselves (Worker token)
        String candidateToken = tokenService.generateToken(candidate.getId(), candidateEmail, User.ROLE_WORKER);
        mockMvc.perform(post("/api/auth/manager-requests/" + candidate.getId() + "/approve")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());

        // 2. Existing Manager can view pending requests
        mockMvc.perform(get("/api/auth/manager-requests")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        // 3. Manager approves candidate
        mockMvc.perform(post("/api/auth/manager-requests/" + candidate.getId() + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

        // 4. Candidate can now successfully log in as MANAGER
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(candidateEmail, "Candidate123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("MANAGER"));
    }

    @Test
    void testLoginValidation_DisabledUser_UnknownUser_WrongPassword() throws Exception {
        String email = "valid_user_" + System.currentTimeMillis() + "@stocksense.io";
        User user = new User(email, new BCryptPasswordEncoder().encode("ValidPass123!"), "Valid User", User.ROLE_WORKER);
        user.setEmailVerified(true);
        userRepository.save(user);

        // 1. Wrong Password -> 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "WrongPass!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        // 2. Unknown Email -> 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ghost@stocksense.io", "Pass123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        // 3. Disabled Account -> 401
        user.setEnabled(false);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "ValidPass123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your account has been disabled. Please contact an administrator."));
    }

    @Test
    void testTokenRevocationOnLogout() throws Exception {
        String email = "logout_test_" + System.currentTimeMillis() + "@stocksense.io";
        User user = new User(email, new BCryptPasswordEncoder().encode("LogoutPass123!"), "Session Tester", User.ROLE_WORKER);
        user.setEmailVerified(true);
        user = userRepository.save(user);

        String token = tokenService.generateToken(user.getId(), email, user.getRole());

        // 1. Valid token grants access to protected profile endpoint
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // 2. Logout invalidates the token on the server
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // 3. Copied / reused token after logout is REJECTED by backend (401 Unauthorized)
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Session expired or token invalid. Please log in again."));
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
        User user = new User(email, new BCryptPasswordEncoder().encode("InitialPass123!"), "Operator", User.ROLE_WORKER);
        user.setEmailVerified(true);
        userRepository.save(user);

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

        // 6. Old password no longer works
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "InitialPass123!"))))
                .andExpect(status().isUnauthorized());

        // 7. Login with new password succeeds
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "BrandNewPass456!"))))
                .andExpect(status().isOk());
    }

    @Test
    void testOtpExpiryHandling() throws Exception {
        String email = "expired_otp_" + System.currentTimeMillis() + "@stocksense.io";
        User user = new User(email, new BCryptPasswordEncoder().encode("Pass123456!"), "Expiry Test User", User.ROLE_WORKER);
        user.setEmailVerified(true);
        userRepository.save(user);

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

    @Test
    void testOtpAttemptLimitExceeded() throws Exception {
        String email = "max_attempts_" + System.currentTimeMillis() + "@stocksense.io";
        RegisterRequest registerReq = new RegisterRequest(email, "Pass123456!", "Attempt Limit User", "WORKER");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Perform 5 wrong attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/verify-email-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyOtpRequest(email, "999999"))))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt is rejected because max attempts exceeded
        mockMvc.perform(post("/api/auth/verify-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest(email, "999999"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Maximum verification attempts exceeded. Please request a new code."));
    }

    @Test
    void testOtpResendCooldown() throws Exception {
        String email = "cooldown_" + System.currentTimeMillis() + "@stocksense.io";
        RegisterRequest registerReq = new RegisterRequest(email, "Pass123456!", "Cooldown User", "WORKER");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Immediately requesting resend violates the 60s cooldown -> 409 Conflict
        mockMvc.perform(post("/api/auth/resend-verification-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendOtpRequest(email))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("seconds before requesting a new verification code")));
    }

    @Test
    void testEmailNotificationService_SmtpFailureHonesty() {
        // When deliveryMode is smtp but mail sender or credentials are not configured,
        // it must throw IllegalStateException and NOT claim false delivery success.
        EmailNotificationService smtpService = new EmailNotificationService(null, "smtp", "noreply@stocksense.io");
        assertThatThrownBy(() -> smtpService.sendRegistrationOtp("test@example.com", "123456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMTP server is not configured");
    }
}
