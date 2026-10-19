package dev.atelierwatch.ecommerce.service;

import dev.atelierwatch.ecommerce.dto.PaymentVerificationRequest;
import dev.atelierwatch.ecommerce.dto.RazorpayOrderResponse;
import dev.atelierwatch.ecommerce.model.Order;
import dev.atelierwatch.ecommerce.model.Payment;
import dev.atelierwatch.ecommerce.repository.OrderRepository;
import dev.atelierwatch.ecommerce.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.NoSuchElementException;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ShippingService shippingService;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.razorpay.key-id:rzp_test_AtelierWatch2026}")
    private String keyId;

    @Value("${app.razorpay.key-secret:atelier_secret_key_luxury_horology_2026}")
    private String keySecret;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, ShippingService shippingService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.shippingService = shippingService;
    }

    public RazorpayOrderResponse createRazorpayOrder(Long userId, String orderReference) {
        Order order = orderRepository.findByOrderReferenceAndUserId(orderReference, userId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderReference));

        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not in pending payment state");
        }

        String razorpayOrderId = "order_" + Math.abs(random.nextLong() % 1000000000000L);

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseGet(() -> {
            Payment p = new Payment();
            p.setOrder(order);
            p.setGateway("RAZORPAY");
            p.setAmount(order.getTotalAmount());
            p.setCurrency(order.getCurrency());
            return p;
        });

        payment.setGatewayOrderId(razorpayOrderId);
        payment.setStatus("PENDING");
        paymentRepository.save(payment);

        return new RazorpayOrderResponse(
                razorpayOrderId,
                order.getTotalAmount(),
                order.getCurrency(),
                keyId,
                order.getOrderReference(),
                order.getShippingName(),
                order.getUser().getEmail(),
                order.getShippingPhone()
        );
    }

    public boolean verifyPayment(PaymentVerificationRequest req) {
        Order order = orderRepository.findByOrderReference(req.orderReference())
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + req.orderReference()));

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new NoSuchElementException("Payment record not found for order"));

        // Verify HMAC-SHA256 signature
        String payload = req.razorpayOrderId() + "|" + req.razorpayPaymentId();
        boolean isValid = verifySignature(payload, req.razorpaySignature(), keySecret);

        if (!isValid) {
            payment.setStatus("PAYMENT_FAILED");
            order.setStatus("PAYMENT_FAILED");
            paymentRepository.save(payment);
            orderRepository.save(order);
            throw new IllegalArgumentException("Invalid payment signature — potential tampering detected");
        }

        // Idempotent Transition
        if (!"CONFIRMED".equals(order.getStatus())) {
            payment.setGatewayPaymentId(req.razorpayPaymentId());
            payment.setGatewaySignature(req.razorpaySignature());
            payment.setStatus("PAID");
            payment.setPaidAt(Instant.now());
            order.setStatus("CONFIRMED");

            paymentRepository.save(payment);
            orderRepository.save(order);

            // Trigger automated shipment creation
            shippingService.createShipment(order);
        }

        return true;
    }

    public boolean handleWebhook(String payload, String signature) {
        if (!verifySignature(payload, signature, keySecret)) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }
        return true;
    }

    public String generateSimulatedSignature(String orderId, String paymentId) {
        String data = orderId + "|" + paymentId;
        return calculateHmacSha256(data, keySecret);
    }

    private boolean verifySignature(String data, String signature, String secret) {
        try {
            String expected = calculateHmacSha256(data, secret);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private String calculateHmacSha256(String data, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC calculation failed", e);
        }
    }
}
