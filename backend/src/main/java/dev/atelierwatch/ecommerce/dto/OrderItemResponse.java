package dev.atelierwatch.ecommerce.dto;

import dev.atelierwatch.ecommerce.model.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(
    Long id,
    Long watchId,
    String name,
    String brand,
    String referenceNumber,
    String imageUrl,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal totalPrice
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
            item.getId(),
            item.getWatch().getId(),
            item.getSnapshotName(),
            item.getSnapshotBrand(),
            item.getSnapshotReference(),
            item.getSnapshotImageUrl(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getTotalPrice()
        );
    }
}
