package com.stocksense.auth;

import com.stocksense.auth.dto.*;
import com.stocksense.common.exception.ConflictException;
import com.stocksense.common.exception.DuplicateResourceException;
import com.stocksense.common.exception.ResourceNotFoundException;
import com.stocksense.common.exception.UnauthorizedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenService tokenService;
    private final EmailNotificationService emailNotificationService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       TokenService tokenService,
                       EmailNotificationService emailNotificationService) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenService = tokenService;
        this.emailNotificationService = emailNotificationService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        // Normal public registrations are always WORKER role. Users cannot self-assign MANAGER.
        User user = new User(email, passwordHash, request.getFullName().trim(), User.ROLE_WORKER);
        User savedUser = userRepository.save(user);

        String token = tokenService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
        return new AuthResponse(token, toProfileResponse(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = tokenService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, toProfileResponse(user));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setFullName(request.getFullName().trim());
        User updated = userRepository.save(user);
        return toProfileResponse(updated);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            // Do not reveal email existence to prevent user enumeration attacks
            return;
        }

        // Check cooldown from latest unused token
        Optional<PasswordResetToken> latestTokenOpt = passwordResetTokenRepository
                .findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email);

        if (latestTokenOpt.isPresent()) {
            PasswordResetToken existing = latestTokenOpt.get();
            if (existing.getLastSentAt().plusSeconds(OTP_RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
                throw new ConflictException("Please wait " + OTP_RESEND_COOLDOWN_SECONDS + " seconds before requesting a new verification code");
            }
        }

        // Generate 6-digit OTP
        int code = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(code);
        String otpHash = passwordEncoder.encode(otp);

        PasswordResetToken resetToken = new PasswordResetToken(
                email,
                otpHash,
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
        );
        passwordResetTokenRepository.save(resetToken);

        // Send email with OTP (real SMTP or explicit console mode)
        emailNotificationService.sendPasswordResetOtp(email, otp);
    }

    @Transactional
    public void verifyOtp(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("No active password reset request found for this email"));

        // Check expiration
        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("The verification code has expired. Please request a new one.");
        }

        // Check attempt limit
        if (resetToken.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            throw new UnauthorizedException("Maximum verification attempts exceeded. Please request a new code.");
        }

        resetToken.setAttemptCount(resetToken.getAttemptCount() + 1);

        // Verify OTP
        if (!passwordEncoder.matches(otp.trim(), resetToken.getOtpHash())) {
            passwordResetTokenRepository.save(resetToken);
            int remaining = MAX_OTP_ATTEMPTS - resetToken.getAttemptCount();
            throw new UnauthorizedException("Invalid verification code. Attempts remaining: " + Math.max(0, remaining));
        }

        resetToken.setVerified(true);
        passwordResetTokenRepository.save(resetToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new UnauthorizedException("No active password reset request found for this email"));

        // Check expiration
        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("The password reset verification code has expired. Please request a new one.");
        }

        // If not already verified via /verify-otp, verify OTP now
        if (!resetToken.isVerified()) {
            if (resetToken.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
                throw new UnauthorizedException("Maximum verification attempts exceeded. Please request a new code.");
            }

            resetToken.setAttemptCount(resetToken.getAttemptCount() + 1);

            if (!passwordEncoder.matches(request.getOtp().trim(), resetToken.getOtpHash())) {
                passwordResetTokenRepository.save(resetToken);
                int remaining = MAX_OTP_ATTEMPTS - resetToken.getAttemptCount();
                throw new UnauthorizedException("Invalid verification code. Attempts remaining: " + Math.max(0, remaining));
            }
        }

        // Mark OTP token as used (one-time use invariant)
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Update password with BCrypt hash
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
