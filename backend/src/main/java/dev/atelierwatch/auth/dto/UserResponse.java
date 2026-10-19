package dev.atelierwatch.auth.dto;

import dev.atelierwatch.auth.model.User;

public record UserResponse(
    Long id,
    String displayName,
    String email,
    String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getDisplayName(),
            user.getEmail(),
            user.getRole()
        );
    }
}
