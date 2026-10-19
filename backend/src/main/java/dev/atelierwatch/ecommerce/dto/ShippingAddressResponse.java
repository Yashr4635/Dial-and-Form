package dev.atelierwatch.ecommerce.dto;

import dev.atelierwatch.ecommerce.model.ShippingAddress;

public record ShippingAddressResponse(
    Long id,
    String recipientName,
    String phone,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String postalCode,
    String country,
    boolean defaultAddress
) {
    public static ShippingAddressResponse from(ShippingAddress addr) {
        return new ShippingAddressResponse(
            addr.getId(),
            addr.getRecipientName(),
            addr.getPhone(),
            addr.getAddressLine1(),
            addr.getAddressLine2(),
            addr.getCity(),
            addr.getState(),
            addr.getPostalCode(),
            addr.getCountry(),
            addr.isDefaultAddress()
        );
    }
}
