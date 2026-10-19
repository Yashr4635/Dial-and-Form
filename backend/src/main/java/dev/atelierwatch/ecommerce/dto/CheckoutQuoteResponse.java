package dev.atelierwatch.ecommerce.dto;

import java.math.BigDecimal;

public record CheckoutQuoteResponse(
    BigDecimal subtotal,
    BigDecimal shippingFee,
    BigDecimal totalAmount,
    String currency,
    int totalItems,
    ShippingAddressResponse shippingAddress
) {}
