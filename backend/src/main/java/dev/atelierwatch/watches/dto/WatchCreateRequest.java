package dev.atelierwatch.watches.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record WatchCreateRequest(
    @NotBlank(message = "Watch name is required")
    @Size(max = 150)
    String name,

    @NotBlank(message = "Brand is required")
    @Size(max = 100)
    String brand,

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
    String slug,

    String tagline,

    @NotBlank(message = "Description is required")
    String description,

    @NotNull(message = "Category ID is required")
    Long categoryId,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    BigDecimal price,

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    String currency,

    @NotBlank(message = "Movement type is required")
    String movementType,

    String caliber,

    @NotBlank(message = "Case material is required")
    String caseMaterial,

    @NotNull(message = "Case diameter is required")
    @DecimalMin(value = "10.0")
    @DecimalMax(value = "65.0")
    BigDecimal caseDiameterMm,

    BigDecimal caseThicknessMm,

    @NotNull(message = "Water resistance is required")
    @Min(0)
    Integer waterResistanceM,

    @NotNull(message = "Power reserve is required")
    @Min(0)
    Integer powerReserveHours,

    @NotBlank(message = "Crystal material is required")
    String crystalMaterial,

    @NotBlank(message = "Dial color is required")
    String dialColor,

    String strapMaterial,

    String functions,

    @NotBlank(message = "Image URL is required")
    String imageUrl,

    boolean featured,

    String status
) {}
