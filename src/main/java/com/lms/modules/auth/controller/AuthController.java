package com.lms.modules.auth.controller;

import com.lms.common.dto.ApiResponse;
import com.lms.modules.auth.dto.AuthResponse;
import com.lms.modules.auth.dto.LoginRequest;
import com.lms.modules.auth.dto.RegisterRequest;
import com.lms.modules.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private com.lms.modules.user.repository.UserRepository userRepository;

    @GetMapping("/debug-users")
    public ResponseEntity<?> debugUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream()
            .map(u -> u.getEmail() + " (role: " + u.getRole() + ", active: " + u.isActive() + ")")
            .collect(java.util.stream.Collectors.toList()));
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