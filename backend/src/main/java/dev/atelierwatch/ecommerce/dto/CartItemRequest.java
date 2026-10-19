package dev.atelierwatch.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
    @NotNull(message = "Watch ID is required")
    Long watchId,

    @Min(value = 1, message = "Quantity must be at least 1")
    int quantity
) {}
