package dev.atelierwatch.ecommerce.service;

import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.auth.repository.UserRepository;
import dev.atelierwatch.ecommerce.dto.ShippingAddressRequest;
import dev.atelierwatch.ecommerce.dto.ShippingAddressResponse;
import dev.atelierwatch.ecommerce.model.ShippingAddress;
import dev.atelierwatch.ecommerce.repository.ShippingAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class AddressService {

    private final ShippingAddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(ShippingAddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ShippingAddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ShippingAddressResponse::from)
                .toList();
    }

    public ShippingAddressResponse addAddress(Long userId, ShippingAddressRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        if (req.defaultAddress()) {
            addressRepository.findByUserIdAndDefaultAddressTrue(userId)
                    .ifPresent(existingDefault -> {
                        existingDefault.setDefaultAddress(false);
                        addressRepository.save(existingDefault);
                    });
        }

        ShippingAddress addr = new ShippingAddress();
        addr.setUser(user);
        addr.setRecipientName(req.recipientName().trim());
        addr.setPhone(req.phone().trim());
        addr.setAddressLine1(req.addressLine1().trim());
        addr.setAddressLine2(req.addressLine2() != null ? req.addressLine2().trim() : null);
        addr.setCity(req.city().trim());
        addr.setState(req.state().trim());
        addr.setPostalCode(req.postalCode().trim());
        addr.setCountry(req.country().trim());
        addr.setDefaultAddress(req.defaultAddress());

        return ShippingAddressResponse.from(addressRepository.save(addr));
    }

    public void deleteAddress(Long userId, Long addressId) {
        ShippingAddress addr = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NoSuchElementException("Address not found or does not belong to user"));
        addressRepository.delete(addr);
    }
}
