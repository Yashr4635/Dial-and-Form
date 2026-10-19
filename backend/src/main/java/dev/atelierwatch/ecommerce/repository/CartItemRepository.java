package dev.atelierwatch.ecommerce.repository;

import dev.atelierwatch.ecommerce.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndWatchId(Long cartId, Long watchId);
}
