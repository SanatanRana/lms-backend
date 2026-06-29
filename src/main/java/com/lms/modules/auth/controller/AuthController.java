package com.lms.modules.auth.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.auth.dto.AuthResponse;
import com.lms.modules.auth.dto.LoginRequest;
import com.lms.modules.auth.dto.RegisterRequest;
import com.lms.modules.auth.service.AuthService;
import com.lms.security.JwtUtil;
import com.lms.modules.user.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private com.lms.modules.user.repository.UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/debug-users")
    public ResponseEntity<?> debugUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream()
            .map(u -> u.getEmail() + " (role: " + u.getRole() + ", active: " + u.isActive() + ")")
            .collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String email = jwtUtil.extractUsername(token);
                UserEntity user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    return ResponseEntity.ok(ApiResponse.success("User profile fetched", Map.of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "role", user.getRole().name(),
                        "email", user.getEmail()
                    )));
                }
            } catch (Exception e) {
                // Token parsing failed
            }
        }
        return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody RegisterRequest request) {
        String result = authService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.loginUser(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}