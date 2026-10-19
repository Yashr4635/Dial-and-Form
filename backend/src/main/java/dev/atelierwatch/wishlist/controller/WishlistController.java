package dev.atelierwatch.wishlist.controller;

import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.common.ApiResponse;
import dev.atelierwatch.watches.dto.WatchSummaryResponse;
import dev.atelierwatch.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/wishlist")
@Tag(name = "Wishlist", description = "Endpoints for saved watches and personalized wishlists")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    @Operation(summary = "Get current authenticated user's wishlist")
    public ResponseEntity<ApiResponse<List<WatchSummaryResponse>>> getWishlist(
            @AuthenticationPrincipal User user
    ) {
        var items = wishlistService.getUserWishlist(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @PutMapping("/{watchId}")
    @Operation(summary = "Add a watch to current user's wishlist")
    public ResponseEntity<ApiResponse<Void>> addToWishlist(
            @AuthenticationPrincipal User user,
            @PathVariable Long watchId
    ) {
        wishlistService.addToWishlist(user.getId(), watchId);
        return ResponseEntity.ok(ApiResponse.ok("Watch added to wishlist", null));
    }

    @DeleteMapping("/{watchId}")
    @Operation(summary = "Remove a watch from current user's wishlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @AuthenticationPrincipal User user,
            @PathVariable Long watchId
    ) {
        wishlistService.removeFromWishlist(user.getId(), watchId);
        return ResponseEntity.ok(ApiResponse.ok("Watch removed from wishlist", null));
    }
}
