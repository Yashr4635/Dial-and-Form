package dev.atelierwatch.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.atelierwatch.auth.dto.AuthRequest;
import dev.atelierwatch.ecommerce.dto.CartItemRequest;
import dev.atelierwatch.ecommerce.dto.CreateOrderRequest;
import dev.atelierwatch.ecommerce.dto.PaymentVerificationRequest;
import dev.atelierwatch.ecommerce.dto.ShippingAddressRequest;
import dev.atelierwatch.ecommerce.model.Order;
import dev.atelierwatch.ecommerce.model.Payment;
import dev.atelierwatch.ecommerce.repository.OrderRepository;
import dev.atelierwatch.ecommerce.repository.PaymentRepository;
import dev.atelierwatch.ecommerce.repository.ShipmentRepository;
import dev.atelierwatch.watches.model.Watch;
import dev.atelierwatch.watches.repository.WatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PaymentSimulationSafetyTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WatchRepository watchRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ShipmentRepository shipmentRepository;

    @Test
    @Transactional
    void demoVerificationNeverRecordsPaidOrCreatesShipmentAndOrderReferencesAreUnique() throws Exception {
        String token = getAuthToken();
        String auth = "Bearer " + token;
        Watch watch = watchRepository.findBySlug("rolex-126334").orElseThrow();
        watch.setPrice(new BigDecimal("1000.00"));
        watch.setCurrency("USD");
        watch.setStockQuantity(3);
        watchRepository.saveAndFlush(watch);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemRequest(watch.getId(), 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkoutAvailable").value(true));

        String addressResponse = mockMvc.perform(post("/api/v1/me/addresses")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShippingAddressRequest(
                                "Audit Demo", "+1-555-019-2834", "1 Test Street", "", "Test City", "CA",
                                "90001", "United States", true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long addressId = objectMapper.readTree(addressResponse).path("data").path("id").asLong();

        String firstOrderResponse = mockMvc.perform(post("/api/v1/checkout/orders")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(addressId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstReference = objectMapper.readTree(firstOrderResponse).path("data").path("orderReference").asText();

        String gatewayResponse = mockMvc.perform(post("/api/v1/payments/razorpay/create-order")
                        .header("Authorization", auth).param("orderReference", firstReference))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Demo payment record created; no gateway order or charge exists"))
                .andExpect(jsonPath("$.data.keyId").value("DEMO_ONLY"))
                .andReturn().getResponse().getContentAsString();
        String demoOrderId = objectMapper.readTree(gatewayResponse).path("data").path("razorpayOrderId").asText();
        assertTrue(demoOrderId.startsWith("demo_order_"));

        String paymentId = "pay_test_acceptance";
        String signatureResponse = mockMvc.perform(get("/api/v1/payments/razorpay/simulate-signature")
                        .header("Authorization", auth)
                        .param("orderReference", firstReference)
                        .param("razorpayPaymentId", paymentId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String signature = objectMapper.readTree(signatureResponse).path("data").path("signature").asText();

        mockMvc.perform(post("/api/v1/payments/razorpay/verify")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentVerificationRequest(
                                firstReference, demoOrderId, paymentId, signature))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Demo result recorded; no payment was processed"));

        Order order = orderRepository.findByOrderReference(firstReference).orElseThrow();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertEquals("DEMO_COMPLETED", order.getStatus());
        assertEquals("SIMULATED", payment.getStatus());
        assertEquals("DEMO", payment.getGateway());
        assertNull(payment.getPaidAt());
        assertTrue(shipmentRepository.findByOrderId(order.getId()).isEmpty());

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemRequest(watch.getId(), 1))))
                .andExpect(status().isOk());
        String secondOrderResponse = mockMvc.perform(post("/api/v1/checkout/orders")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(addressId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String secondReference = objectMapper.readTree(secondOrderResponse).path("data").path("orderReference").asText();
        assertNotEquals(firstReference, secondReference);
        assertEquals(2, orderRepository.findByUserIdOrderByCreatedAtDesc(order.getUser().getId()).size());
    }

    private String getAuthToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("collector@atelierwatch.dev", "Admin@123456"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }
}
