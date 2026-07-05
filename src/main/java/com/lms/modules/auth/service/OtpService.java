package com.lms.modules.auth.service;

import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OTP Service — Generates and verifies 6-digit OTPs for phone/email authentication.
 *
 * OTPs are stored in-memory with a 10-minute expiry.
 * For production scale, replace with Redis-backed store.
 *
 * ACTIVATION:
 * - Email OTP works when spring.mail.username is configured.
 * - For SMS OTP, integrate Twilio or MSG91 SDK and uncomment the sendSms method.
 */
@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_LENGTH = 6;

    // In-memory OTP store: key = email/phone, value = {otp, expiry}
    // In production, use Redis with TTL for auto-expiry
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.mail.from:noreply@learngen.app}")
    private String fromEmail;

    @Value("${app.name:LearnGen}")
    private String appName;

    /**
     * Sends an OTP to the given email address.
     * OTP is valid for 10 minutes.
     */
    public void sendEmailOtp(String email) {
        if (mailSender == null) {
            throw new RuntimeException("Email service is not configured. Please contact support.");
        }

        String otp = generateOtp();
        otpStore.put(email.toLowerCase(), new OtpRecord(otp, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject(appName + " — Your OTP Code");
        message.setText(
            "Hello,\n\n" +
            "Your OTP for " + appName + " login is:\n\n" +
            "  " + otp + "\n\n" +
            "This OTP is valid for " + OTP_EXPIRY_MINUTES + " minutes.\n" +
            "Do NOT share this OTP with anyone.\n\n" +
            "If you did not request this OTP, please ignore this email.\n\n" +
            "— " + appName + " Team"
        );

        try {
            mailSender.send(message);
            logger.info("[OtpService] Email OTP sent to: {}", maskEmail(email));
        } catch (Exception e) {
            logger.error("[OtpService] Failed to send OTP email: {}", e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }

    /**
     * Verifies an OTP for the given identifier (email or phone).
     * Returns true if valid and not expired. Deletes OTP after successful verification.
     */
    public boolean verifyOtp(String identifier, String otp) {
        OtpRecord record = otpStore.get(identifier.toLowerCase());
        if (record == null) return false;
        if (LocalDateTime.now().isAfter(record.expiry())) {
            otpStore.remove(identifier.toLowerCase());
            return false;
        }
        if (!record.otp().equals(otp)) return false;

        otpStore.remove(identifier.toLowerCase()); // One-time use
        return true;
    }

    /**
     * Checks if the given email is registered in the system.
     */
    public boolean isEmailRegistered(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String visible = local.length() > 2 ? local.substring(0, 2) : local;
        return visible + "***@" + parts[1];
    }

    // Immutable OTP record
    private record OtpRecord(String otp, LocalDateTime expiry) {}
}
