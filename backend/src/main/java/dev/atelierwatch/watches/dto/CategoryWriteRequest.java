package dev.atelierwatch.watches.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryWriteRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 100)
        String name,

        @NotBlank(message = "Category slug is required")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
        @Size(max = 100)
        String slug,

        String description
) {}
