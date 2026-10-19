package dev.atelierwatch.ecommerce.controller;

import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.common.ApiResponse;
import dev.atelierwatch.common.PageResponse;
import dev.atelierwatch.ecommerce.dto.OrderResponse;
import dev.atelierwatch.ecommerce.service.OrderService;
import dev.atelierwatch.ecommerce.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Order Management", description = "Endpoints for customer order tracking and administrative fulfillment")
public class OrderController {

    private final OrderService orderService;
    private final ShippingService shippingService;

    public OrderController(OrderService orderService, ShippingService shippingService) {
        this.orderService = orderService;
        this.shippingService = shippingService;
    }

    @GetMapping("/me/orders")
    @Operation(summary = "Get historical orders placed by authenticated customer")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getUserOrders(user.getId())));
    }

    @GetMapping("/me/orders/{orderRef}")
    @Operation(summary = "Get detailed snapshot and tracking timeline for a specific order")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByRef(
            @AuthenticationPrincipal User user,
            @PathVariable String orderRef
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderByReference(user.getId(), orderRef)));
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: View and filter all customer orders across the platform")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        var result = orderService.getAllOrders(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @PatchMapping("/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Transition order lifecycle status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Order status updated", orderService.updateOrderStatus(id, status)));
    }

    @PatchMapping("/admin/orders/{id}/shipment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Update carrier tracking status (IN_TRANSIT, DELIVERED)")
    public ResponseEntity<ApiResponse<Void>> updateShipmentStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        shippingService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Shipment updated", null));
    }
}
