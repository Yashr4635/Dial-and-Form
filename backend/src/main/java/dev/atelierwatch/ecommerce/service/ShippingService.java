package dev.atelierwatch.ecommerce.service;

import dev.atelierwatch.ecommerce.model.Order;
import dev.atelierwatch.ecommerce.model.Shipment;
import dev.atelierwatch.ecommerce.repository.OrderRepository;
import dev.atelierwatch.ecommerce.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final SecureRandom random = new SecureRandom();

    public ShippingService(ShipmentRepository shipmentRepository, OrderRepository orderRepository) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
    }

    public Shipment createShipment(Order order) {
        return shipmentRepository.findByOrderId(order.getId()).orElseGet(() -> {
            String trackingNum = "AW-EXP-" + (100000 + random.nextInt(900000));
            Shipment s = new Shipment();
            s.setOrder(order);
            s.setProvider("DEMO");
            s.setCourierName("Demonstration shipment — no live carrier");
            s.setTrackingNumber(trackingNum);
            s.setTrackingUrl(null);
            s.setStatus("ORDER_PROCESSED");
            s.setEstimatedDelivery(Instant.now().plus(3, ChronoUnit.DAYS));
            return shipmentRepository.save(s);
        });
    }

    public Shipment updateStatus(Long orderId, String status) {
        Shipment s = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found for order ID: " + orderId));

        s.setStatus(status);
        if ("IN_TRANSIT".equalsIgnoreCase(status) && s.getShippedAt() == null) {
            s.setShippedAt(Instant.now());
        }
        if ("DELIVERED".equalsIgnoreCase(status)) {
            s.setDeliveredAt(Instant.now());
            // also update order status
            Order order = s.getOrder();
            order.setStatus("DELIVERED");
            orderRepository.save(order);
        }
        return shipmentRepository.save(s);
    }
}
