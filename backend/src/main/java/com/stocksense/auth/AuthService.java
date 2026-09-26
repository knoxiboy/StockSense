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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final TokenService tokenService;
    private final EmailNotificationService emailNotificationService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       TokenService tokenService,
                       EmailNotificationService emailNotificationService) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.tokenService = tokenService;
        this.emailNotificationService = emailNotificationService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        String requestedRole = "MANAGER".equalsIgnoreCase(request.getRequestedRole()) ? User.ROLE_MANAGER : User.ROLE_WORKER;

        // Security boundary: Public registration can NEVER directly assign MANAGER.
        // It always creates with assigned role = WORKER.
        // If Manager was requested, approvalStatus is PENDING_APPROVAL.
        String approvalStatus = User.ROLE_MANAGER.equals(requestedRole) ? User.STATUS_PENDING_APPROVAL : User.STATUS_APPROVED;

        User user = new User(
                email,
                passwordHash,
                request.getFullName().trim(),
                User.ROLE_WORKER,
                requestedRole,
                approvalStatus
        );
        user.setEmailVerified(false);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        // Invalidate any previous unused verification tokens for this email
        emailVerificationTokenRepository.findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email)
                .ifPresent(prev -> {
                    prev.setUsed(true);
                    emailVerificationTokenRepository.save(prev);
                });

        // Generate 6-digit cryptographically secure OTP
        int code = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(code);
        String otpHash = passwordEncoder.encode(otp);

        EmailVerificationToken verificationToken = new EmailVerificationToken(
                email,
                otpHash,
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
        );
        emailVerificationTokenRepository.save(verificationToken);

        // Send OTP via configured email service (SMTP or dev console)
        emailNotificationService.sendRegistrationOtp(email, otp);

        String message = User.ROLE_MANAGER.equals(requestedRole)
                ? "Registration successful. Please enter the 6-digit verification code sent to your email. Manager privileges will require administrator approval."
                : "Registration successful. Please enter the 6-digit verification code sent to your email to activate your account.";

        return new RegisterResponse(
                message,
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getRequestedRole(),
                savedUser.getApprovalStatus(),
                true
        );
    }

    @Transactional(noRollbackFor = { UnauthorizedException.class })
    public AuthResponse verifyEmailOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new UnauthorizedException("No active email verification request found for this email"));

        // Check expiration
        if (verificationToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("The verification code has expired. Please request a new one.");
        }

        // Check attempt limit
        if (verificationToken.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            throw new UnauthorizedException("Maximum verification attempts exceeded. Please request a new code.");
        }

        verificationToken.setAttemptCount(verificationToken.getAttemptCount() + 1);

        // Verify OTP against stored hash
        if (!passwordEncoder.matches(request.getOtp().trim(), verificationToken.getOtpHash())) {
            emailVerificationTokenRepository.save(verificationToken);
            int remaining = MAX_OTP_ATTEMPTS - verificationToken.getAttemptCount();
            throw new UnauthorizedException("Invalid verification code. Attempts remaining: " + Math.max(0, remaining));
        }

        // Mark OTP as verified and used (single-use invariant)
        verificationToken.setVerified(true);
        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        // Activate email verification on user account
        user.setEmailVerified(true);
        userRepository.save(user);

        // If user requested Manager and is pending approval, do not issue an active session token yet
        if (User.STATUS_PENDING_APPROVAL.equalsIgnoreCase(user.getApprovalStatus())) {
            return new AuthResponse(null, toProfileResponse(user));
        }

        // Issue real JWT token for verified worker
        String token = tokenService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, toProfileResponse(user));
    }

    @Transactional
    public void resendEmailVerificationOtp(ResendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.isEmailVerified()) {
            throw new ConflictException("Your email is already verified. Please sign in.");
        }

        // Check cooldown from latest unused token
        Optional<EmailVerificationToken> latestOpt = emailVerificationTokenRepository
                .findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email);

        if (latestOpt.isPresent()) {
            EmailVerificationToken existing = latestOpt.get();
            if (existing.getLastSentAt().plusSeconds(OTP_RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
                throw new ConflictException("Please wait " + OTP_RESEND_COOLDOWN_SECONDS + " seconds before requesting a new verification code.");
            }
            existing.setUsed(true);
            emailVerificationTokenRepository.save(existing);
        }

        // Generate new 6-digit OTP
        int code = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(code);
        String otpHash = passwordEncoder.encode(otp);

        EmailVerificationToken verificationToken = new EmailVerificationToken(
                email,
                otpHash,
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
        );
        emailVerificationTokenRepository.save(verificationToken);

        emailNotificationService.sendRegistrationOtp(email, otp);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Your account has been disabled. Please contact an administrator.");
        }

        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Your email address is not verified. Please verify your email with the 6-digit verification code sent during registration.");
        }

        if (User.STATUS_PENDING_APPROVAL.equalsIgnoreCase(user.getApprovalStatus())) {
            throw new UnauthorizedException("Your Manager account request is pending administrator approval. You will receive access once approved.");
        }

        if (User.STATUS_REJECTED.equalsIgnoreCase(user.getApprovalStatus())) {
            throw new UnauthorizedException("Your Manager account request was not approved. Please contact an administrator.");
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
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            tokenService.revokeToken(token);
        }
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            // Safe response: prevent user enumeration attacks
            return;
        }

        // Check cooldown from latest unused token
        Optional<PasswordResetToken> latestTokenOpt = passwordResetTokenRepository
                .findFirstByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email);

        if (latestTokenOpt.isPresent()) {
            PasswordResetToken existing = latestTokenOpt.get();
            if (existing.getLastSentAt().plusSeconds(OTP_RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
                throw new ConflictException("Please wait " + OTP_RESEND_COOLDOWN_SECONDS + " seconds before requesting a new verification code.");
            }
            // Invalidate previous token
            existing.setUsed(true);
            passwordResetTokenRepository.save(existing);
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

    @Transactional(noRollbackFor = { UnauthorizedException.class })
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

    @Transactional(noRollbackFor = { UnauthorizedException.class })
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

        // Update password with BCrypt hash and record timestamp
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // Manager Provisioning & Approvals (Phase 5 & Phase 10)
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getPendingManagerRequests() {
        return userRepository.findByRequestedRoleAndApprovalStatus(User.ROLE_MANAGER, User.STATUS_PENDING_APPROVAL)
                .stream()
                .map(this::toProfileResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProfileResponse approveManagerRequest(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new ConflictException("You cannot approve your own Manager access request.");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        target.setRole(User.ROLE_MANAGER);
        target.setApprovalStatus(User.STATUS_APPROVED);
        User saved = userRepository.save(target);
        return toProfileResponse(saved);
    }

    @Transactional
    public UserProfileResponse rejectManagerRequest(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new ConflictException("You cannot reject your own Manager access request.");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        target.setRole(User.ROLE_WORKER);
        target.setApprovalStatus(User.STATUS_REJECTED);
        User saved = userRepository.save(target);
        return toProfileResponse(saved);
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getRequestedRole(),
                user.getApprovalStatus(),
                user.isEmailVerified(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
