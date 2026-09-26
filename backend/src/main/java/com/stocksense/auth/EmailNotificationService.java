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

    public EmailNotificationService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${stocksense.mail.delivery-mode:console}") String deliveryMode) {
        this.mailSender = mailSender;
        this.deliveryMode = deliveryMode != null ? deliveryMode.trim().toLowerCase() : "console";
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        if ("smtp".equalsIgnoreCase(deliveryMode)) {
            if (mailSender == null) {
                logger.error("SMTP delivery mode is active, but JavaMailSender is not configured.");
                throw new IllegalStateException("SMTP delivery is enabled (STOCKSENSE_MAIL_DELIVERY_MODE=smtp), but SMTP server is not configured. Please check spring.mail.* settings.");
            }
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject("StockSense Password Reset Verification Code");
                message.setText(String.format(
                        "Hello,\n\nYour StockSense password reset verification code is: %s\n\n" +
                        "This code will expire in 10 minutes.\n" +
                        "If you did not request a password reset, please ignore this email.\n\n" +
                        "StockSense Security Team",
                        otp
                ));
                mailSender.send(message);
                logger.info("Password reset OTP email successfully delivered via SMTP to {}", toEmail);
            } catch (Exception e) {
                logger.error("Failed to send password reset OTP to {} via SMTP: {}", toEmail, e.getMessage());
                throw new IllegalStateException("Failed to deliver OTP email via configured SMTP server: " + e.getMessage(), e);
            }
        } else {
            // Explicit local development / test console delivery mode
            logger.info("================================================================================");
            logger.info("[STOCKSENSE SECURITY OTP - DEV/CONSOLE MODE] Recipient: {}", toEmail);
            logger.info("[STOCKSENSE SECURITY OTP - DEV/CONSOLE MODE] Verification Code: {}", otp);
            logger.info("[STOCKSENSE SECURITY OTP - DEV/CONSOLE MODE] Valid For: 10 minutes");
            logger.info("[STOCKSENSE SECURITY OTP - DEV/CONSOLE MODE] Configured Mode: {}", deliveryMode);
            logger.info("================================================================================");
        }
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }
}
