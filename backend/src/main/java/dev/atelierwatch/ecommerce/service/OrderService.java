package dev.atelierwatch.ecommerce.service;

import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.auth.repository.UserRepository;
import dev.atelierwatch.ecommerce.dto.*;
import dev.atelierwatch.ecommerce.model.*;
import dev.atelierwatch.ecommerce.repository.*;
import dev.atelierwatch.watches.model.Watch;
import dev.atelierwatch.watches.repository.WatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Year;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ShippingAddressRepository addressRepository;
    private final WatchRepository watchRepository;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.razorpay.simulation-enabled:false}")
    private boolean simulationEnabled;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                        ShippingAddressRepository addressRepository, WatchRepository watchRepository,
                        UserRepository userRepository, ShipmentRepository shipmentRepository,
                        PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.watchRepository = watchRepository;
        this.userRepository = userRepository;
        this.shipmentRepository = shipmentRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public CheckoutQuoteResponse getQuote(Long userId, Long addressId) {
        requireCheckoutEnabled();
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot generate checkout quote for an empty cart");
        }

        ShippingAddress addr = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NoSuchElementException("Shipping address not found"));

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItems = 0;
        for (CartItem item : cart.getItems()) {
            Watch watch = item.getWatch();
            requirePurchasable(watch, item.getQuantity());
            subtotal = subtotal.add(watch.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalItems += item.getQuantity();
        }

        BigDecimal shippingFee = BigDecimal.ZERO; // Demo-only until shipping rates are configured.
        BigDecimal total = subtotal.add(shippingFee);

        return new CheckoutQuoteResponse(
                subtotal, shippingFee, total, "USD", totalItems, ShippingAddressResponse.from(addr)
        );
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrderResponse createOrder(Long userId, CreateOrderRequest req) {
        requireCheckoutEnabled();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot create an order from an empty cart");
        }

        ShippingAddress addr = addressRepository.findByIdAndUserId(req.shippingAddressId(), userId)
                .orElseThrow(() -> new NoSuchElementException("Valid shipping address is required"));

        // 1. Concurrency-safe Stock Verification & Decrement
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            Watch watch = watchRepository.findById(item.getWatch().getId())
                    .orElseThrow(() -> new NoSuchElementException("Watch not found: " + item.getWatch().getId()));

            requirePurchasable(watch, item.getQuantity());
            if (watch.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient inventory for '" + watch.getName() + "'. Available: " + watch.getStockQuantity() + ", requested: " + item.getQuantity());
            }

            // Atomic decrement
            watch.setStockQuantity(watch.getStockQuantity() - item.getQuantity());
            watchRepository.save(watch);

            BigDecimal line = watch.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(line);
        }

        // 2. Create Order Entity
        String orderRef = generateOrderReference();
        Order order = new Order();
        order.setOrderReference(orderRef);
        order.setUser(user);
        order.setShippingName(addr.getRecipientName());
        order.setShippingPhone(addr.getPhone());
        order.setShippingAddressLine1(addr.getAddressLine1());
        order.setShippingAddressLine2(addr.getAddressLine2());
        order.setShippingCity(addr.getCity());
        order.setShippingState(addr.getState());
        order.setShippingPostalCode(addr.getPostalCode());
        order.setShippingCountry(addr.getCountry());
        order.setSubtotal(subtotal);
        order.setShippingFee(BigDecimal.ZERO);
        order.setTotalAmount(subtotal);
        order.setCurrency("USD");
        order.setStatus("PENDING_PAYMENT");

        // 3. Snapshot Order Items
        for (CartItem item : cart.getItems()) {
            Watch watch = item.getWatch();
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setWatch(watch);
            oi.setSnapshotName(watch.getName());
            oi.setSnapshotBrand(watch.getBrand());
            oi.setSnapshotReference(watch.getReferenceNumber());
            oi.setSnapshotImageUrl(watch.getImageUrl());
            oi.setUnitPrice(watch.getPrice());
            oi.setQuantity(item.getQuantity());
            oi.setTotalPrice(watch.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            order.getItems().add(oi);
        }

        Order saved = orderRepository.save(order);

        // 4. Clear Cart
        cart.getItems().clear();
        cartRepository.save(cart);

        return enrichWithShipment(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::enrichWithShipment)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByReference(Long userId, String orderRef) {
        Order order = orderRepository.findByOrderReferenceAndUserId(orderRef, userId)
                .orElseThrow(() -> new NoSuchElementException("Order not found or access denied: " + orderRef));
        return enrichWithShipment(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(String status, Pageable pageable) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::enrichWithShipment);
    }

    public OrderResponse updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with ID: " + orderId));
        order.setStatus(newStatus);
        return enrichWithShipment(orderRepository.save(order));
    }

    private OrderResponse enrichWithShipment(Order order) {
        var base = OrderResponse.from(order);
        var shipmentOpt = shipmentRepository.findByOrderId(order.getId());
        String paymentStatus = paymentRepository.findByOrderId(order.getId())
                .map(payment -> payment.getStatus())
                .orElse("NOT_STARTED");
        Shipment shipment = shipmentOpt.orElse(null);
        return new OrderResponse(
                base.id(), base.orderReference(), base.status(), paymentStatus,
                base.subtotal(), base.shippingFee(), base.totalAmount(), base.currency(),
                base.shippingName(), base.shippingPhone(), base.shippingAddressLine1(), base.shippingAddressLine2(),
                base.shippingCity(), base.shippingState(), base.shippingPostalCode(), base.shippingCountry(),
                base.items(), shipment != null ? shipment.getTrackingNumber() : null,
                shipment != null ? shipment.getTrackingUrl() : null,
                shipment != null ? shipment.getStatus() : null, base.createdAt()
        );
    }

    private String generateOrderReference() {
        int year = Year.now().getValue();
        for (int attempt = 0; attempt < 10; attempt++) {
            int code = 10000 + random.nextInt(90000);
            String reference = "AW-" + year + "-" + code;
            if (!orderRepository.existsByOrderReference(reference)) return reference;
        }
        throw new IllegalStateException("Could not allocate a unique order reference");
    }

    private void requirePurchasable(Watch watch, int quantity) {
        if (!"Rolex".equals(watch.getBrand()) || !"PUBLISHED".equals(watch.getStatus())) {
            throw new IllegalArgumentException("This catalog item is no longer available for checkout");
        }
        if (watch.getPrice() == null || watch.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("Checkout is unavailable until a verified selling price is configured");
        }
        if (watch.getStockQuantity() == null || watch.getStockQuantity() <= 0) {
            throw new IllegalArgumentException("Checkout is unavailable until inventory is configured");
        }
        if (watch.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient inventory for '" + watch.getName() + "'. Available: " + watch.getStockQuantity() + ", requested: " + quantity);
        }
    }

    private void requireCheckoutEnabled() {
        if (!simulationEnabled) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Checkout is unavailable because no live payment gateway is configured"
            );
        }
    }
}
