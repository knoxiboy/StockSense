package com.stocksense.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    public EmailNotificationService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        boolean sent = false;
        if (mailSender != null) {
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
                sent = true;
                logger.info("Password reset OTP email sent successfully to {}", toEmail);
            } catch (Exception e) {
                logger.warn("Failed to send email via SMTP (will log OTP as fallback): {}", e.getMessage());
            }
        }

        if (!sent) {
            logger.info("================================================================================");
            logger.info("[STOCKSENSE SECURITY OTP FALLBACK] Destination: {}", toEmail);
            logger.info("[STOCKSENSE SECURITY OTP FALLBACK] Verification Code: {}", otp);
            logger.info("[STOCKSENSE SECURITY OTP FALLBACK] Expiry: 10 minutes");
            logger.info("================================================================================");
        }
    }
}
