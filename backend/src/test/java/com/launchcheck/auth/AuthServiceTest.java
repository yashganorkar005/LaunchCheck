package com.launchcheck.auth;

import com.launchcheck.auth.entity.User;
import com.launchcheck.auth.repository.UserRepository;
import com.launchcheck.auth.security.JwtService;
import com.launchcheck.auth.service.AuthService;
import com.launchcheck.auth.dto.AuthResponse;
import com.launchcheck.auth.dto.LoginRequest;
import com.launchcheck.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService("0123456789012345678901234567890123456789012345678901234567890123", 86400000L);
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerNewUserSuccessfully() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return new User(UUID.randomUUID(), u.getEmail(), u.getPasswordHash(), Instant.now(), Instant.now());
        });

        AuthResponse response = authService.register(new RegisterRequest("test@example.com", "SecureP@ssword123"));

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("test@example.com", response.user().email());
        assertTrue(jwtService.validateToken(response.token()));
        assertEquals(response.user().id(), jwtService.extractUserId(response.token()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.register(new RegisterRequest("duplicate@example.com", "SecureP@ssword123"))
        );
        assertEquals("Email is already registered", ex.getMessage());
    }

    @Test
    void registerRejectsWeakPassword() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.register(new RegisterRequest("user@example.com", "short"))
        );
        assertTrue(ex.getMessage().contains("Password must be at least 8 characters"));
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        UUID userId = UUID.randomUUID();
        String hashedPassword = passwordEncoder.encode("SecureP@ssword123");
        User user = new User(userId, "test@example.com", hashedPassword, Instant.now(), Instant.now());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("test@example.com", "SecureP@ssword123"));

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals(userId, response.user().id());
        assertEquals("test@example.com", response.user().email());
    }

    @Test
    void loginWithInvalidPasswordFails() {
        String hashedPassword = passwordEncoder.encode("SecureP@ssword123");
        User user = new User(UUID.randomUUID(), "test@example.com", hashedPassword, Instant.now(), Instant.now());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.login(new LoginRequest("test@example.com", "WrongPassword"))
        );
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void loginWithNonExistentEmailFails() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.login(new LoginRequest("unknown@example.com", "SecureP@ssword123"))
        );
        assertEquals("Invalid email or password", ex.getMessage());
    }
}
