package dev.atelierwatch.ecommerce.service;

import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.auth.repository.UserRepository;
import dev.atelierwatch.ecommerce.dto.CartItemRequest;
import dev.atelierwatch.ecommerce.dto.CartItemResponse;
import dev.atelierwatch.ecommerce.dto.CartResponse;
import dev.atelierwatch.ecommerce.model.Cart;
import dev.atelierwatch.ecommerce.model.CartItem;
import dev.atelierwatch.ecommerce.repository.CartItemRepository;
import dev.atelierwatch.ecommerce.repository.CartRepository;
import dev.atelierwatch.watches.model.Watch;
import dev.atelierwatch.watches.repository.WatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final WatchRepository watchRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       WatchRepository watchRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.watchRepository = watchRepository;
        this.userRepository = userRepository;
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
            return cartRepository.save(new Cart(user));
        });
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return buildCartResponse(cart);
    }

    public CartResponse addItem(Long userId, Long watchId, int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1");
        Watch watch = watchRepository.findById(watchId)
                .orElseThrow(() -> new NoSuchElementException("Watch not found: " + watchId));

        if (!"PUBLISHED".equals(watch.getStatus())) {
            throw new IllegalArgumentException("This timepiece is currently unavailable for acquisition");
        }

        Cart cart = getOrCreateCart(userId);
        var existingOpt = cartItemRepository.findByCartIdAndWatchId(cart.getId(), watchId);

        int targetQuantity = quantity;
        CartItem item;
        if (existingOpt.isPresent()) {
            item = existingOpt.get();
            targetQuantity = item.getQuantity() + quantity;
        } else {
            item = new CartItem(cart, watch, 0);
            cart.getItems().add(item);
        }

        if (watch.getStockQuantity() < targetQuantity) {
            throw new IllegalArgumentException("Requested quantity (" + targetQuantity + ") exceeds available inventory (" + watch.getStockQuantity() + ")");
        }

        item.setQuantity(targetQuantity);
        cartItemRepository.save(item);
        return buildCartResponse(cart);
    }

    public CartResponse updateItem(Long userId, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Cart item not found: " + itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user");
        }

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            if (item.getWatch().getStockQuantity() < quantity) {
                throw new IllegalArgumentException("Requested quantity (" + quantity + ") exceeds available inventory (" + item.getWatch().getStockQuantity() + ")");
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return buildCartResponse(cart);
    }

    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Cart item not found: " + itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return buildCartResponse(cart);
    }

    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    public CartResponse mergeGuestCart(Long userId, List<CartItemRequest> items) {
        for (CartItemRequest req : items) {
            try {
                addItem(userId, req.watchId(), req.quantity());
            } catch (Exception ignored) {}
        }
        return getCart(userId);
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList();

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQty = itemResponses.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        return new CartResponse(cart.getId(), itemResponses, subtotal, totalQty);
    }
}
