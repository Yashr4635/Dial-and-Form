package dev.atelierwatch.ecommerce.controller;

import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.common.ApiResponse;
import dev.atelierwatch.ecommerce.dto.CheckoutQuoteResponse;
import dev.atelierwatch.ecommerce.dto.CreateOrderRequest;
import dev.atelierwatch.ecommerce.dto.OrderResponse;
import dev.atelierwatch.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/checkout")
@Tag(name = "Checkout", description = "Endpoints for generating checkout quotes and placing orders")
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/quote")
    @Operation(summary = "Calculate order subtotal, delivery charges, and final amount on server")
    public ResponseEntity<ApiResponse<CheckoutQuoteResponse>> getQuote(
            @AuthenticationPrincipal User user,
            @RequestParam Long addressId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getQuote(user.getId(), addressId)));
    }

    @PostMapping("/orders")
    @Operation(summary = "Place order, atomically deduct stock, and generate public reference")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        var order = orderService.createOrder(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order initialized. Awaiting payment authorization.", order));
    }
}
