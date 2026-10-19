package dev.atelierwatch.ecommerce.controller;

import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.common.ApiResponse;
import dev.atelierwatch.ecommerce.dto.ShippingAddressRequest;
import dev.atelierwatch.ecommerce.dto.ShippingAddressResponse;
import dev.atelierwatch.ecommerce.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/addresses")
@Tag(name = "Customer Addresses", description = "Endpoints for managing customer delivery addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @Operation(summary = "Get all saved shipping addresses for current customer")
    public ResponseEntity<ApiResponse<List<ShippingAddressResponse>>> getAddresses(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.getUserAddresses(user.getId())));
    }

    @PostMapping
    @Operation(summary = "Add a new verified delivery address")
    public ResponseEntity<ApiResponse<ShippingAddressResponse>> addAddress(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ShippingAddressRequest request
    ) {
        var created = addressService.addAddress(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Address saved successfully", created));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a shipping address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        addressService.deleteAddress(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted", null));
    }
}
