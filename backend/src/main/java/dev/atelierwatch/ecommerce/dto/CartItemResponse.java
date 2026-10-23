package dev.atelierwatch.ecommerce.dto;

import dev.atelierwatch.ecommerce.model.CartItem;
import java.math.BigDecimal;

public record CartItemResponse(
    Long id,
    Long watchId,
    String name,
    String brand,
    String slug,
    String referenceNumber,
    String imageUrl,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineTotal,
    Integer availableStock,
    boolean purchasable
) {
    public static CartItemResponse from(CartItem item) {
        var watch = item.getWatch();
        boolean purchasable = "PUBLISHED".equals(watch.getStatus())
                && "Rolex".equals(watch.getBrand())
                && watch.getPrice() != null && watch.getPrice().signum() > 0
                && watch.getStockQuantity() != null && watch.getStockQuantity() > 0;
        BigDecimal unitPrice = purchasable ? watch.getPrice() : null;
        BigDecimal line = purchasable ? unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())) : null;
        return new CartItemResponse(
            item.getId(),
            watch.getId(),
            watch.getName(),
            watch.getBrand(),
            watch.getSlug(),
            watch.getReferenceNumber(),
            watch.getImageUrl(),
            unitPrice,
            item.getQuantity(),
            line,
            watch.getStockQuantity(),
            purchasable
        );
    }
}
