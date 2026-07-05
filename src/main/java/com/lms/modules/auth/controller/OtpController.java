package com.lms.modules.auth.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.auth.dto.AuthResponse;
import com.lms.modules.auth.service.OtpService;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import com.lms.security.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OTP Authentication Controller.
 *
 * Endpoints:
 * POST /api/auth/send-otp    — Sends OTP to registered email
 * POST /api/auth/verify-otp  — Verifies OTP and returns JWT token
 */
@RestController
@RequestMapping("/api/auth")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Request OTP for email-based login.
     * The email must be registered in the system.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<String>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        // Verify email is registered
        if (!otpService.isEmailRegistered(request.getEmail())) {
            // Return generic message to prevent email enumeration attacks
            return ResponseEntity.ok(ApiResponse.success(
                "If this email is registered, you will receive an OTP shortly.", null));
        }

        otpService.sendEmailOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
            "OTP sent successfully. Please check your email.", null));
    }

    /**
     * Verify OTP and receive JWT token.
     * Returns the same AuthResponse as the regular login endpoint.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP. Please request a new one.");
        }

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Your account is inactive. Please contact an Admin to activate it.");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        AuthResponse response = new AuthResponse(
                token, "Login successful via OTP",
                user.getName(), user.getRole().name(), user.getId()
        );

        return ResponseEntity.ok(ApiResponse.success("OTP verified. Login successful.", response));
    }

    // ── Request DTOs (inner classes for simplicity) ─────────────────
    @Data
    public static class SendOtpRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    public static class VerifyOtpRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "OTP is required")
        private String otp;
    }
}
