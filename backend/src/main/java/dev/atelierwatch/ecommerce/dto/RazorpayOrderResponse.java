package dev.atelierwatch.ecommerce.dto;

import java.math.BigDecimal;

public record RazorpayOrderResponse(
    String razorpayOrderId,
    BigDecimal amount,
    String currency,
    String keyId,
    String orderReference,
    String customerName,
    String customerEmail,
    String customerPhone
) {}
