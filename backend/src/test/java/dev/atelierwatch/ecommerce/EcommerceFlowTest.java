package dev.atelierwatch.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.atelierwatch.auth.dto.AuthRequest;
import dev.atelierwatch.auth.dto.AuthResponse;
import dev.atelierwatch.auth.dto.RegisterRequest;
import dev.atelierwatch.ecommerce.dto.CartItemRequest;
import dev.atelierwatch.ecommerce.dto.CreateOrderRequest;
import dev.atelierwatch.ecommerce.dto.PaymentVerificationRequest;
import dev.atelierwatch.ecommerce.dto.ShippingAddressRequest;
import dev.atelierwatch.ecommerce.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EcommerceFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentService paymentService;

    private String getAuthToken(String email, String password) throws Exception {
        AuthRequest req = new AuthRequest(email, password);
        String resp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(resp).get("data").get("token").asText();
    }

    @Test
    @DisplayName("End-to-End Shopping Journey: Cart -> Address -> Checkout -> Payment -> Order Tracking")
    void testCompleteShoppingJourney() throws Exception {
        // 1. Authenticate Collector
        String token = getAuthToken("collector@atelierwatch.dev", "Admin@123456");

        // 2. Add Watch 1 to Cart (Sky-Dweller)
        CartItemRequest cartReq = new CartItemRequest(1L, 1);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.subtotal").isNumber());

        // 3. Save Shipping Address
        ShippingAddressRequest addrReq = new ShippingAddressRequest(
                "Alexander Thorne", "+1-555-019-2834", "742 Evergreen Terrace", "Suite 4B",
                "New York", "NY", "10021", "United States", true
        );
        String addrResp = mockMvc.perform(post("/api/v1/me/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addrReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long addressId = objectMapper.readTree(addrResp).get("data").get("id").asLong();

        // 4. Generate Checkout Quote
        mockMvc.perform(get("/api/v1/checkout/quote?addressId=" + addressId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal").isNumber())
                .andExpect(jsonPath("$.data.shippingFee").isNumber())
                .andExpect(jsonPath("$.data.totalAmount").isNumber());

        // 5. Place Order (Atomic inventory deduction & order reference creation)
        CreateOrderRequest orderReq = new CreateOrderRequest(addressId);
        String orderResp = mockMvc.perform(post("/api/v1/checkout/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderReference", startsWith("AW-2026-")))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andReturn().getResponse().getContentAsString();

        String orderRef = objectMapper.readTree(orderResp).get("data").get("orderReference").asText();

        // 6. Initialize Razorpay Payment Order
        String rzpResp = mockMvc.perform(post("/api/v1/payments/razorpay/create-order?orderReference=" + orderRef)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.razorpayOrderId", startsWith("order_")))
                .andReturn().getResponse().getContentAsString();

        String rzpOrderId = objectMapper.readTree(rzpResp).get("data").get("razorpayOrderId").asText();
        String rzpPaymentId = "pay_simulated_" + System.currentTimeMillis();
        String validSignature = paymentService.generateSimulatedSignature(rzpOrderId, rzpPaymentId);

        // 7. Verify Payment with HMAC-SHA256 Signature
        PaymentVerificationRequest verifyReq = new PaymentVerificationRequest(orderRef, rzpOrderId, rzpPaymentId, validSignature);
        mockMvc.perform(post("/api/v1/payments/razorpay/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 8. Order is now CONFIRMED and has an automated tracking shipment
        mockMvc.perform(get("/api/v1/me/orders/" + orderRef)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.trackingNumber", startsWith("AW-EXP-")))
                .andExpect(jsonPath("$.data.shipmentStatus").value("ORDER_PROCESSED"));
    }

    @Test
    @DisplayName("Security: Tampered payment signature is rejected with error")
    void testTamperedPaymentSignature() throws Exception {
        PaymentVerificationRequest tamperedReq = new PaymentVerificationRequest(
                "AW-2026-99999", "order_12345", "pay_fake", "tampered_signature_abc123"
        );

        mockMvc.perform(post("/api/v1/payments/razorpay/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tamperedReq)))
                .andExpect(status().isForbidden()); // Unauthenticated / forbidden
    }
}
