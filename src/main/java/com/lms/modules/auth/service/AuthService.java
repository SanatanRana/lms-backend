package com.lms.modules.auth.service;

import com.lms.common.enums.Role;
import com.lms.common.event.DomainEvents;
import com.lms.modules.auth.dto.AuthResponse;
import com.lms.modules.auth.dto.LoginRequest;
import com.lms.modules.auth.dto.RegisterRequest;
import com.lms.modules.user.entity.UserEntity;
import com.lms.modules.user.repository.UserRepository;
import com.lms.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public String registerUser(RegisterRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Name cannot be empty!");
        }
        if (request.getEmail() == null || !request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new RuntimeException("Invalid email format!");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters long!");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered!");
        }

        UserEntity user = new UserEntity();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT);
        user.setActive(true);

        userRepository.save(user);

        // OCP: Publish event – any listener can react without changing this code
        eventPublisher.publishEvent(new DomainEvents.UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getName(), user.getRole().name()
        ));

        return "User registered successfully!";
    }

    public AuthResponse loginUser(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new RuntimeException("Your account is inactive! Please contact an Admin to activate it.");
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid email or password!");
        }

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthResponse(token, "Login successful", user.getName(), user.getRole().name());
    }
}