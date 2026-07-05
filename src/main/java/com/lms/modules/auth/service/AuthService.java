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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public String registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("An account with this email address already exists.");
        }

        UserEntity user = new UserEntity();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : "");
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Public registration always creates STUDENT accounts.
        // Admin creates TEACHER accounts via /api/admin/users/register-teacher
        user.setRole(Role.STUDENT);
        user.setActive(true);

        UserEntity saved = userRepository.save(user);

        eventPublisher.publishEvent(new DomainEvents.UserRegisteredEvent(
                saved.getId(), saved.getEmail(), saved.getName(), saved.getRole().name()
        ));

        return "Registration successful! Welcome to LearnGen. Please login to continue.";
    }

    @Transactional(readOnly = true)
    public AuthResponse loginUser(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException e) {
            throw new RuntimeException("Your account has been deactivated. Please contact an administrator.");
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password. Please try again.");
        }

        UserEntity user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("User not found."));

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, "Login successful", user.getName(), user.getRole().name(), user.getId());
    }
}