package dev.atelierwatch.ecommerce.dto;

import dev.atelierwatch.ecommerce.model.ShippingAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(
    @NotBlank(message = "Recipient name is required")
    @Size(max = 150)
    String recipientName,

    @NotBlank(message = "Phone number is required")
    @Size(max = 30)
    @Pattern(regexp = "^\\+?[0-9][0-9 ()-]{6,28}$", message = "Enter a valid phone number")
    String phone,

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255)
    String addressLine1,

    String addressLine2,

    @NotBlank(message = "City is required")
    @Size(max = 100)
    String city,

    @NotBlank(message = "State is required")
    @Size(max = 100)
    String state,

    @NotBlank(message = "Postal code is required")
    @Size(max = 30)
    String postalCode,

    @NotBlank(message = "Country is required")
    @Size(max = 100)
    String country,

    boolean defaultAddress
) {}
