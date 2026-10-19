package dev.atelierwatch.auth.service;

import dev.atelierwatch.auth.dto.AuthRequest;
import dev.atelierwatch.auth.dto.AuthResponse;
import dev.atelierwatch.auth.dto.RegisterRequest;
import dev.atelierwatch.auth.dto.UserResponse;
import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.auth.repository.UserRepository;
import dev.atelierwatch.auth.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpiration;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail(), Map.of(
                "role", user.getRole(),
                "displayName", user.getDisplayName()
        ));

        return AuthResponse.of(token, jwtExpiration, UserResponse.from(user));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with email " + email + " already exists");
        }

        User user = new User(
                request.displayName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                "ROLE_USER" // Strictly enforced: standard users cannot grant themselves admin
        );

        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved.getEmail(), Map.of(
                "role", saved.getRole(),
                "displayName", saved.getDisplayName()
        ));

        return AuthResponse.of(token, jwtExpiration, UserResponse.from(saved));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + email));
    }
}
