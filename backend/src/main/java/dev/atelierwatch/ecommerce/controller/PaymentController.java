package dev.atelierwatch.ecommerce.controller;

import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.common.ApiResponse;
import dev.atelierwatch.ecommerce.dto.PaymentVerificationRequest;
import dev.atelierwatch.ecommerce.dto.RazorpayOrderResponse;
import dev.atelierwatch.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Gateway", description = "Endpoints for Razorpay order generation, signature verification, and webhooks")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/razorpay/create-order")
    @Operation(summary = "Initialize a server-side payment order with Razorpay")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createOrder(
            @AuthenticationPrincipal User user,
            @RequestParam String orderReference
    ) {
        var response = paymentService.createRazorpayOrder(user.getId(), orderReference);
        return ResponseEntity.ok(ApiResponse.ok("Payment order generated", response));
    }

    @PostMapping("/razorpay/verify")
    @Operation(summary = "Verify HMAC-SHA256 signature returned by payment gateway and confirm order")
    public ResponseEntity<ApiResponse<Void>> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request
    ) {
        paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.ok("Payment verified and order confirmed", null));
    }

    @GetMapping("/razorpay/simulate-signature")
    @Operation(summary = "Sandbox utility: generate valid HMAC signature for automated testing")
    public ResponseEntity<ApiResponse<Map<String, String>>> simulateSignature(
            @RequestParam String razorpayOrderId,
            @RequestParam String razorpayPaymentId
    ) {
        String signature = paymentService.generateSimulatedSignature(razorpayOrderId, razorpayPaymentId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "razorpayOrderId", razorpayOrderId,
                "razorpayPaymentId", razorpayPaymentId,
                "signature", signature
        )));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Idempotent server-to-server payment gateway webhook receiver")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        if (signature != null) {
            paymentService.handleWebhook(rawPayload, signature);
        }
        return ResponseEntity.ok("OK");
    }
}
