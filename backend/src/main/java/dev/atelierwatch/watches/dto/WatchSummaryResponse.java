package dev.atelierwatch.watches.dto;

import dev.atelierwatch.watches.model.Watch;
import java.math.BigDecimal;

public record WatchSummaryResponse(
    Long id,
    String name,
    String brand,
    String slug,
    String referenceNumber,
    String tagline,
    CategoryResponse category,
    BigDecimal price,
    BigDecimal retailPrice,
    String currency,
    Integer stockQuantity,
    String movementType,
    String caseMaterial,
    BigDecimal caseDiameterMm,
    String dialColor,
    String imageUrl,
    boolean featured,
    String collectionName,
    boolean purchasable
) {
    public static WatchSummaryResponse from(Watch watch) {
        return new WatchSummaryResponse(
            watch.getId(),
            watch.getName(),
            watch.getBrand(),
            watch.getSlug(),
            watch.getReferenceNumber(),
            watch.getTagline(),
            watch.getCategory() != null ? CategoryResponse.from(watch.getCategory()) : null,
            watch.getPrice(),
            watch.getRetailPrice(),
            watch.getCurrency(),
            watch.getStockQuantity(),
            watch.getMovementType(),
            watch.getCaseMaterial(),
            watch.getCaseDiameterMm(),
            watch.getDialColor(),
            watch.getImageUrl(),
            watch.isFeatured(),
            watch.getCollectionName(),
            isPurchasable(watch)
        );
    }

    private static boolean isPurchasable(Watch watch) {
        return "PUBLISHED".equals(watch.getStatus())
                && "Rolex".equals(watch.getBrand())
                && watch.getPrice() != null && watch.getPrice().signum() > 0
                && watch.getStockQuantity() != null && watch.getStockQuantity() > 0;
    }
}
