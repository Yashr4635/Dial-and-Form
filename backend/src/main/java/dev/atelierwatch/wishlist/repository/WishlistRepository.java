package dev.atelierwatch.wishlist.repository;

import dev.atelierwatch.wishlist.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    @Query("SELECT w FROM WishlistItem w JOIN FETCH w.watch WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<WishlistItem> findByUserIdWithWatch(@Param("userId") Long userId);

    Optional<WishlistItem> findByUserIdAndWatchId(Long userId, Long watchId);

    boolean existsByUserIdAndWatchId(Long userId, Long watchId);

    void deleteByUserIdAndWatchId(Long userId, Long watchId);
}
