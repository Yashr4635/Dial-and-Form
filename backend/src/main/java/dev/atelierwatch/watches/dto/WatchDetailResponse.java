package dev.atelierwatch.watches.dto;

import dev.atelierwatch.watches.model.Watch;
import java.math.BigDecimal;
import java.time.Instant;

public record WatchDetailResponse(
    Long id,
    String name,
    String brand,
    String slug,
    String referenceNumber,
    String tagline,
    String description,
    CategoryResponse category,
    BigDecimal price,
    BigDecimal retailPrice,
    String currency,
    Integer stockQuantity,
    Integer warrantyYears,
    String provenanceNotes,
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
    boolean featured,
    String status,
    Instant createdAt,
    Instant updatedAt
) {
    public static WatchDetailResponse from(Watch watch) {
        return new WatchDetailResponse(
            watch.getId(),
            watch.getName(),
            watch.getBrand(),
            watch.getSlug(),
            watch.getReferenceNumber(),
            watch.getTagline(),
            watch.getDescription(),
            watch.getCategory() != null ? CategoryResponse.from(watch.getCategory()) : null,
            watch.getPrice(),
            watch.getRetailPrice(),
            watch.getCurrency(),
            watch.getStockQuantity(),
            watch.getWarrantyYears(),
            watch.getProvenanceNotes(),
            watch.getMovementType(),
            watch.getCaliber(),
            watch.getCaseMaterial(),
            watch.getCaseDiameterMm(),
            watch.getCaseThicknessMm(),
            watch.getWaterResistanceM(),
            watch.getPowerReserveHours(),
            watch.getCrystalMaterial(),
            watch.getDialColor(),
            watch.getStrapMaterial(),
            watch.getFunctions(),
            watch.getImageUrl(),
            watch.isFeatured(),
            watch.getStatus(),
            watch.getCreatedAt(),
            watch.getUpdatedAt()
        );
    }
}
