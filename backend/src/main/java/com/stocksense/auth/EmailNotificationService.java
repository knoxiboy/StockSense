package com.stocksense.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final String deliveryMode;
    private final String fromEmail;

    public EmailNotificationService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${stocksense.mail.delivery-mode:smtp}") String deliveryMode,
            @Value("${stocksense.mail.from:noreply@stocksense.io}") String fromEmail) {
        this.mailSender = mailSender;
        this.deliveryMode = deliveryMode != null ? deliveryMode.trim().toLowerCase() : "smtp";
        this.fromEmail = fromEmail != null ? fromEmail.trim() : "noreply@stocksense.io";
    }

    public void sendRegistrationOtp(String toEmail, String otp) {
        sendOtpEmail(toEmail, otp, "StockSense Account Verification Code",
                "Hello,\n\nWelcome to StockSense! Your 6-digit email verification code is: %s\n\n" +
                "This code will expire in 10 minutes.\n" +
                "Please enter this code on the registration verification screen to activate your account.\n\n" +
                "If you did not create a StockSense account, please ignore this email.\n\n" +
                "StockSense Security Team");
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        sendOtpEmail(toEmail, otp, "StockSense Password Reset Verification Code",
                "Hello,\n\nYour StockSense password reset verification code is: %s\n\n" +
                "This code will expire in 10 minutes.\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "StockSense Security Team");
    }

    private void sendOtpEmail(String toEmail, String otp, String subject, String bodyTemplate) {
        if ("smtp".equalsIgnoreCase(deliveryMode)) {
            if (mailSender == null) {
                logger.error("SMTP delivery mode is active, but JavaMailSender is not configured. Please check spring.mail.* and MAIL_* environment settings.");
                throw new IllegalStateException("SMTP delivery is enabled (STOCKSENSE_MAIL_DELIVERY_MODE=smtp), but SMTP server is not configured. Please set MAIL_HOST, MAIL_PORT, MAIL_USERNAME, and MAIL_PASSWORD.");
            }
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(String.format(bodyTemplate, otp));
                mailSender.send(message);
                logger.info("OTP email successfully submitted to SMTP server for recipient {}", maskEmail(toEmail));
            } catch (Exception e) {
                logger.error("Failed to submit OTP email to SMTP server for recipient {}: {}", maskEmail(toEmail), e.getMessage());
                throw new IllegalStateException("Failed to deliver OTP email via configured SMTP server: " + e.getMessage(), e);
            }
        } else if ("console".equalsIgnoreCase(deliveryMode)) {
            // Explicit local development / test console delivery mode
            logger.info("================================================================================");
            logger.info("[STOCKSENSE SECURITY OTP - DEV/CONSOLE MODE] Recipient: {}", toEmail);
            logger.info("[STOCKSENSE SECURITY OTP - DEV/CONSOLE MODE] Subject: {}", subject);
            logger.info("[STOCKSENSE SECURITY OTP - DEV/CONSOLE MODE] Verification Code: {}", otp);
            logger.info("[STOCKSENSE SECURITY OTP - DEV/CONSOLE MODE] Valid For: 10 minutes");
            logger.info("[STOCKSENSE SECURITY OTP - DEV/CONSOLE MODE] Configured Mode: {}", deliveryMode);
            logger.info("================================================================================");
        } else {
            logger.error("Invalid delivery mode '{}'. Must be 'smtp' or 'console'.", deliveryMode);
            throw new IllegalStateException("Invalid email delivery mode configured: " + deliveryMode);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "*@" + email.substring(atIndex + 1);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public String getFromEmail() {
        return fromEmail;
    }
}
