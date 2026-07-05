package com.lms.modules.auth.service;

import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forgot Password / Password Reset Service.
 *
 * Flow:
 * 1. User submits email → generateResetToken() creates a token and emails it
 * 2. Frontend shows "reset link sent" and gives user a form with token field
 * 3. User submits new password + token → resetPassword() validates and updates
 *
 * Tokens are stored in-memory. For production scale, use Redis or DB table.
 */
@Service
public class ForgotPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordService.class);
    private static final int TOKEN_EXPIRY_MINUTES = 30;

    // key = token, value = {email, expiry}
    private final Map<String, ResetRecord> tokenStore = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.mail.from:noreply@learngen.app}")
    private String fromEmail;

    @Value("${app.name:LearnGen}")
    private String appName;

    /**
     * Initiates password reset — sends a reset link to the user's email.
     * Always returns success message (even if email not found) to prevent email enumeration.
     */
    public void initiatePasswordReset(String email) {
        if (mailSender == null) {
            throw new RuntimeException("Email service is not configured. Please contact support.");
        }

        // Only send if user exists — but don't reveal this to the caller
        userRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            String token = generateSecureToken();
            tokenStore.put(token, new ResetRecord(email.toLowerCase(),
                    LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES)));

            String resetLink = frontendUrl + "/reset-password?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject(appName + " — Reset Your Password");
            message.setText(
                "Hello " + user.getName() + ",\n\n" +
                "We received a request to reset your " + appName + " password.\n\n" +
                "Click the link below to reset it (valid for " + TOKEN_EXPIRY_MINUTES + " minutes):\n\n" +
                resetLink + "\n\n" +
                "If you did not request this, please ignore this email. Your password will remain unchanged.\n\n" +
                "— " + appName + " Team"
            );

            try {
                mailSender.send(message);
                logger.info("[ForgotPasswordService] Reset link sent to: {}", maskEmail(email));
            } catch (Exception e) {
                logger.error("[ForgotPasswordService] Failed to send reset email: {}", e.getMessage());
                // Remove token if email failed to send
                tokenStore.remove(token);
                throw new RuntimeException("Failed to send password reset email. Please try again.");
            }
        });
    }

    /**
     * Validates the reset token and updates the user's password.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters long.");
        }

        ResetRecord record = tokenStore.get(token);
        if (record == null) {
            throw new RuntimeException("Invalid or expired reset token. Please request a new one.");
        }
        if (LocalDateTime.now().isAfter(record.expiry())) {
            tokenStore.remove(token);
            throw new RuntimeException("Reset token has expired. Please request a new one.");
        }

        UserEntity user = userRepository.findByEmail(record.email())
                .orElseThrow(() -> new RuntimeException("User not found."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenStore.remove(token); // One-time use
        logger.info("[ForgotPasswordService] Password reset successful for user: {}", maskEmail(record.email()));
    }

    /**
     * Validates if a token is still valid (frontend uses this to show/hide the form).
     */
    public boolean isTokenValid(String token) {
        ResetRecord record = tokenStore.get(token);
        if (record == null) return false;
        if (LocalDateTime.now().isAfter(record.expiry())) {
            tokenStore.remove(token);
            return false;
        }
        return true;
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String visible = local.length() > 2 ? local.substring(0, 2) : local;
        return visible + "***@" + parts[1];
    }

    private record ResetRecord(String email, LocalDateTime expiry) {}
}
