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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Demo Payments", description = "Development-only payment simulation; no live gateway is configured")
public class PaymentController {

    @Value("${app.razorpay.simulation-enabled:false}")
    private boolean simulationEnabled;

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/razorpay/create-order")
    @Operation(summary = "Create a development-only demo payment record")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createOrder(
            @AuthenticationPrincipal User user,
            @RequestParam String orderReference
    ) {
        requireSimulationEnabled();
        var response = paymentService.createRazorpayOrder(user.getId(), orderReference);
        return ResponseEntity.ok(ApiResponse.ok("Demo payment record created; no gateway order or charge exists", response));
    }

    @PostMapping("/razorpay/verify")
    @Operation(summary = "Record a development-only simulated payment result")
    public ResponseEntity<ApiResponse<Void>> verifyPayment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PaymentVerificationRequest request
    ) {
        requireSimulationEnabled();
        paymentService.verifyPayment(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Demo result recorded; no payment was processed", null));
    }

    @GetMapping("/razorpay/simulate-signature")
    @Operation(summary = "Development-only helper for the simulated payment flow")
    public ResponseEntity<ApiResponse<Map<String, String>>> simulateSignature(
            @AuthenticationPrincipal User user,
            @RequestParam String orderReference,
            @RequestParam String razorpayPaymentId
    ) {
        requireSimulationEnabled();
        String signature = paymentService.generateSimulatedSignature(user.getId(), orderReference, razorpayPaymentId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "orderReference", orderReference,
                "razorpayPaymentId", razorpayPaymentId,
                "signature", signature
        )));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Live gateway webhooks are not configured")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "No live payment gateway is configured; webhooks are disabled"
        );
    }

    private void requireSimulationEnabled() {
        if (!simulationEnabled) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "No live payment gateway is configured; demo payment processing is disabled"
            );
        }
    }
}
