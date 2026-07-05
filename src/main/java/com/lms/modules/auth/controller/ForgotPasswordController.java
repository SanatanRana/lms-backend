package com.lms.modules.auth.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.auth.service.ForgotPasswordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Forgot Password / Password Reset Controller.
 *
 * Endpoints (all public — no auth required):
 * POST /api/auth/forgot-password      — Send reset link to email
 * GET  /api/auth/reset-password       — Validate token (frontend uses this)
 * POST /api/auth/reset-password       — Set new password with token
 */
@RestController
@RequestMapping("/api/auth")
public class ForgotPasswordController {

    @Autowired
    private ForgotPasswordService forgotPasswordService;

    /**
     * Initiates password reset — sends email with reset link.
     * Always returns 200 with generic message (prevents email enumeration).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        try {
            forgotPasswordService.initiatePasswordReset(request.getEmail());
        } catch (RuntimeException e) {
            // Don't throw — return generic message regardless of outcome
            // This prevents attackers from knowing which emails are registered
        }
        return ResponseEntity.ok(ApiResponse.success(
            "If an account exists with this email, a password reset link has been sent.", null));
    }

    /**
     * Validates a reset token — used by frontend to show/hide the reset form.
     */
    @GetMapping("/reset-password")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestParam String token) {
        boolean isValid = forgotPasswordService.isTokenValid(token);
        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired reset token. Please request a new one."));
        }
        return ResponseEntity.ok(ApiResponse.success("Token is valid", true));
    }

    /**
     * Resets the password using a valid token.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        forgotPasswordService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(
            "Password has been reset successfully. You can now login with your new password.", null));
    }

    // ── Request DTOs ─────────────────────────────────────────────────
    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "Reset token is required")
        private String token;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String newPassword;
    }
}
