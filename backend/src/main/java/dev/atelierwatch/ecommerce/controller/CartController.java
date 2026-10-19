package dev.atelierwatch.ecommerce.controller;

import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.common.ApiResponse;
import dev.atelierwatch.ecommerce.dto.CartItemRequest;
import dev.atelierwatch.ecommerce.dto.CartResponse;
import dev.atelierwatch.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Shopping Cart", description = "Endpoints for managing client and persistent customer carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get current authenticated user cart with server-calculated totals")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(user.getId())));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a timepiece to the cart with stock verification")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CartItemRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart", cartService.addItem(user.getId(), request.watchId(), request.quantity())));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update item quantity in cart")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long itemId,
            @RequestParam int quantity
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Cart updated", cartService.updateItem(user.getId(), itemId, quantity)));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Item removed", cartService.removeItem(user.getId(), itemId)));
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge guest localStorage items upon authentication")
    public ResponseEntity<ApiResponse<CartResponse>> mergeCart(
            @AuthenticationPrincipal User user,
            @RequestBody List<CartItemRequest> items
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Cart synchronized", cartService.mergeGuestCart(user.getId(), items)));
    }
}
