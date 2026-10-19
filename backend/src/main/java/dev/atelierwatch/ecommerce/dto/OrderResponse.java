package dev.atelierwatch.ecommerce.dto;

import dev.atelierwatch.ecommerce.model.Order;
import dev.atelierwatch.ecommerce.model.OrderItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderReference,
    String status,
    BigDecimal subtotal,
    BigDecimal shippingFee,
    BigDecimal totalAmount,
    String currency,
    String shippingName,
    String shippingPhone,
    String shippingAddressLine1,
    String shippingAddressLine2,
    String shippingCity,
    String shippingState,
    String shippingPostalCode,
    String shippingCountry,
    List<OrderItemResponse> items,
    String trackingNumber,
    String trackingUrl,
    String shipmentStatus,
    Instant createdAt
) {
    public static OrderResponse from(Order order) {
        var itemResponses = order.getItems().stream().map(OrderItemResponse::from).toList();
        return new OrderResponse(
            order.getId(),
            order.getOrderReference(),
            order.getStatus(),
            order.getSubtotal(),
            order.getShippingFee(),
            order.getTotalAmount(),
            order.getCurrency(),
            order.getShippingName(),
            order.getShippingPhone(),
            order.getShippingAddressLine1(),
            order.getShippingAddressLine2(),
            order.getShippingCity(),
            order.getShippingState(),
            order.getShippingPostalCode(),
            order.getShippingCountry(),
            itemResponses,
            null, // populated if shipment exists
            null,
            null,
            order.getCreatedAt()
        );
    }
}
