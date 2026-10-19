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
    int availableStock
) {
    public static CartItemResponse from(CartItem item) {
        var watch = item.getWatch();
        BigDecimal line = watch.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
            item.getId(),
            watch.getId(),
            watch.getName(),
            watch.getBrand(),
            watch.getSlug(),
            watch.getReferenceNumber(),
            watch.getImageUrl(),
            watch.getPrice(),
            item.getQuantity(),
            line,
            watch.getStockQuantity()
        );
    }
}
