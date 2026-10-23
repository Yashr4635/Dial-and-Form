package dev.atelierwatch.wishlist.service;

import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.auth.repository.UserRepository;
import dev.atelierwatch.watches.dto.WatchSummaryResponse;
import dev.atelierwatch.watches.model.Watch;
import dev.atelierwatch.watches.repository.WatchRepository;
import dev.atelierwatch.wishlist.model.WishlistItem;
import dev.atelierwatch.wishlist.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final WatchRepository watchRepository;

    public WishlistService(WishlistRepository wishlistRepository, UserRepository userRepository, WatchRepository watchRepository) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.watchRepository = watchRepository;
    }

    @Transactional(readOnly = true)
    public List<WatchSummaryResponse> getUserWishlist(Long userId) {
        return wishlistRepository.findByUserIdWithWatch(userId)
                .stream()
                .map(item -> WatchSummaryResponse.from(item.getWatch()))
                .toList();
    }

    public void addToWishlist(Long userId, Long watchId) {
        if (wishlistRepository.existsByUserIdAndWatchId(userId, watchId)) {
            return; // Idempotent: already present
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));
        Watch watch = watchRepository.findById(watchId)
                .orElseThrow(() -> new NoSuchElementException("Watch not found with ID: " + watchId));

        if (!"Rolex".equals(watch.getBrand()) || !"PUBLISHED".equals(watch.getStatus())) {
            throw new IllegalArgumentException("Only published Rolex catalog entries can be added to a wishlist");
        }

        wishlistRepository.save(new WishlistItem(user, watch));
    }

    public void removeFromWishlist(Long userId, Long watchId) {
        wishlistRepository.deleteByUserIdAndWatchId(userId, watchId);
    }
}
