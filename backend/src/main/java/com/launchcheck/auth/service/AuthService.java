package com.launchcheck.auth.service;

import com.launchcheck.auth.dto.AuthResponse;
import com.launchcheck.auth.dto.LoginRequest;
import com.launchcheck.auth.dto.RegisterRequest;
import com.launchcheck.auth.dto.UserSummary;
import com.launchcheck.auth.entity.User;
import com.launchcheck.auth.repository.UserRepository;
import com.launchcheck.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        Instant now = Instant.now();
        User user = new User(UUID.randomUUID(), normalizedEmail, passwordHash, now, now);
        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail());
        UserSummary summary = new UserSummary(savedUser.getId(), savedUser.getEmail(), savedUser.getCreatedAt());

        return new AuthResponse(token, summary);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request.email() == null || request.password() == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        UserSummary summary = new UserSummary(user.getId(), user.getEmail(), user.getCreatedAt());

        return new AuthResponse(token, summary);
    }

    @Transactional(readOnly = true)
    public UserSummary getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserSummary(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}
