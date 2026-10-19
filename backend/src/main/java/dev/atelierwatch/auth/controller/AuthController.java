package dev.atelierwatch.auth.controller;

import dev.atelierwatch.auth.dto.AuthRequest;
import dev.atelierwatch.auth.dto.AuthResponse;
import dev.atelierwatch.auth.dto.RegisterRequest;
import dev.atelierwatch.auth.dto.UserResponse;
import dev.atelierwatch.auth.service.AuthService;
import dev.atelierwatch.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Authentication", description = "Endpoints for user login, registration, and identity")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Authenticate user and receive JWT bearer token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/auth/register")
    @Operation(summary = "Register a new collector account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "Client session logout notification")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @GetMapping("/users/me")
    @Operation(summary = "Get current authenticated user details")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal dev.atelierwatch.auth.model.User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.ok(authService.getCurrentUser(user.getEmail())));
    }
}
