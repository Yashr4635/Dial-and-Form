package dev.atelierwatch.watches.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record WatchUpdateRequest(
    @Size(max = 150) String name,
    @Size(max = 100) String brand,
    String tagline,
    String description,
    Long categoryId,
    @DecimalMin(value = "0.01") BigDecimal price,
    String currency,
    String movementType,
    String caliber,
    String caseMaterial,
    BigDecimal caseDiameterMm,
    BigDecimal caseThicknessMm,
    Integer waterResistanceM,
    Integer powerReserveHours,
    String crystalMaterial,
    String dialColor,
    String strapMaterial,
    String functions,
    String imageUrl,
    Boolean featured,
    String status
) {}
