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
import java.util.HexFormat;
import java.util.NoSuchElementException;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.razorpay.key-id:DEMO_ONLY}")
    private String keyId;

    @Value("${app.razorpay.key-secret:}")
    private String keySecret;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    public RazorpayOrderResponse createRazorpayOrder(Long userId, String orderReference) {
        Order order = orderRepository.findByOrderReferenceAndUserId(orderReference, userId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderReference));

        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not in pending payment state");
        }

        String razorpayOrderId = "demo_order_" + Math.abs(random.nextLong() % 1000000000000L);

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseGet(() -> {
            Payment p = new Payment();
            p.setOrder(order);
            p.setGateway("DEMO");
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

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public boolean verifyPayment(Long userId, PaymentVerificationRequest req) {
        Order order = orderRepository.findByOrderReferenceAndUserId(req.orderReference(), userId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + req.orderReference()));

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new NoSuchElementException("Payment record not found for order"));

        // Verify HMAC-SHA256 signature
        String payload = req.razorpayOrderId() + "|" + req.razorpayPaymentId();
        boolean isValid = req.razorpayOrderId().equals(payment.getGatewayOrderId())
                && verifySignature(payload, req.razorpaySignature(), keySecret);

        if (!isValid) {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            throw new IllegalArgumentException("Invalid payment signature — potential tampering detected");
        }

        if ("DEMO_COMPLETED".equals(order.getStatus())) {
            if (req.razorpayPaymentId().equals(payment.getGatewayPaymentId())
                    && req.razorpaySignature().equals(payment.getGatewaySignature())
                    && "SIMULATED".equals(payment.getStatus())) {
                return true;
            }
            throw new IllegalArgumentException("Demo order is already completed with another simulated result");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not awaiting payment");
        }

        payment.setGatewayPaymentId(req.razorpayPaymentId());
        payment.setGatewaySignature(req.razorpaySignature());
        payment.setStatus("SIMULATED");
        payment.setPaidAt(null);
        order.setStatus("DEMO_COMPLETED");

        paymentRepository.save(payment);
        orderRepository.save(order);

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

    public String generateSimulatedSignature(Long userId, String orderReference, String paymentId) {
        Order order = orderRepository.findByOrderReferenceAndUserId(orderReference, userId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderReference));
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not awaiting payment");
        }
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new NoSuchElementException("Payment record not found for order"));
        if (payment.getGatewayOrderId() == null) {
            throw new IllegalArgumentException("Payment order has not been initialized");
        }
        return generateSimulatedSignature(payment.getGatewayOrderId(), paymentId);
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
