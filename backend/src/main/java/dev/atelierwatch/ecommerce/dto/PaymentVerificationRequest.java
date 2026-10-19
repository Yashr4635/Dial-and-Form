package dev.atelierwatch.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentVerificationRequest(
    @NotBlank(message = "Order reference is required")
    String orderReference,

    @NotBlank(message = "Razorpay order ID is required")
    String razorpayOrderId,

    @NotBlank(message = "Razorpay payment ID is required")
    String razorpayPaymentId,

    @NotBlank(message = "Razorpay signature is required")
    String razorpaySignature
) {}
