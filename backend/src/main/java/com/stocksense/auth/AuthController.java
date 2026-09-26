package com.stocksense.auth;

import com.stocksense.auth.dto.*;
import com.stocksense.common.exception.ForbiddenException;
import com.stocksense.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<AuthResponse> verifyEmailOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyEmailOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification-otp")
    public ResponseEntity<Map<String, String>> resendVerificationOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendEmailVerificationOtp(request);
        return ResponseEntity.ok(Map.of("message", "A new 6-digit verification code has been sent to your email."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenPayload payload = extractAndVerify(authHeader);
        return ResponseEntity.ok(authService.getProfile(payload.getUserId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        TokenService.TokenPayload payload = extractAndVerify(authHeader);
        return ResponseEntity.ok(authService.updateProfile(payload.getUserId(), request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "If an account exists with this email, a verification code has been sent."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(Map.of("message", "Verification code verified successfully."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully. You can now login with your new password."));
    }

    // Manager Provisioning & Approvals
    @GetMapping("/manager-requests")
    public ResponseEntity<List<UserProfileResponse>> getPendingManagerRequests(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenService.TokenPayload payload = extractAndVerify(authHeader);
        if (!User.ROLE_MANAGER.equalsIgnoreCase(payload.getRole())) {
            throw new ForbiddenException("Access denied: Insufficient privileges. MANAGER role required.");
        }
        return ResponseEntity.ok(authService.getPendingManagerRequests());
    }

    @PostMapping("/manager-requests/{userId}/approve")
    public ResponseEntity<UserProfileResponse> approveManagerRequest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userId) {
        TokenService.TokenPayload payload = extractAndVerify(authHeader);
        if (!User.ROLE_MANAGER.equalsIgnoreCase(payload.getRole())) {
            throw new ForbiddenException("Access denied: Insufficient privileges. MANAGER role required.");
        }
        return ResponseEntity.ok(authService.approveManagerRequest(payload.getUserId(), userId));
    }

    @PostMapping("/manager-requests/{userId}/reject")
    public ResponseEntity<UserProfileResponse> rejectManagerRequest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userId) {
        TokenService.TokenPayload payload = extractAndVerify(authHeader);
        if (!User.ROLE_MANAGER.equalsIgnoreCase(payload.getRole())) {
            throw new ForbiddenException("Access denied: Insufficient privileges. MANAGER role required.");
        }
        return ResponseEntity.ok(authService.rejectManagerRequest(payload.getUserId(), userId));
    }

    private TokenService.TokenPayload extractAndVerify(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7).trim();
        TokenService.TokenPayload payload = tokenService.parseAndVerifyToken(token);
        if (payload == null) {
            throw new UnauthorizedException("Session expired or token invalid. Please log in again.");
        }
        return payload;
    }
}
