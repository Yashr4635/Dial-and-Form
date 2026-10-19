package dev.atelierwatch.ecommerce.repository;

import dev.atelierwatch.ecommerce.model.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {
    List<ShippingAddress> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ShippingAddress> findByIdAndUserId(Long id, Long userId);
    Optional<ShippingAddress> findByUserIdAndDefaultAddressTrue(Long userId);
}
