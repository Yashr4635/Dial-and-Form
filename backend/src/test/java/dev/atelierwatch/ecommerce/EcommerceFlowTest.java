package dev.atelierwatch.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.atelierwatch.auth.dto.AuthRequest;
import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.auth.repository.UserRepository;
import dev.atelierwatch.ecommerce.dto.CreateOrderRequest;
import dev.atelierwatch.ecommerce.model.Cart;
import dev.atelierwatch.ecommerce.model.CartItem;
import dev.atelierwatch.ecommerce.repository.CartRepository;
import dev.atelierwatch.ecommerce.repository.OrderRepository;
import dev.atelierwatch.ecommerce.dto.PaymentVerificationRequest;
import dev.atelierwatch.ecommerce.dto.ShippingAddressRequest;
import dev.atelierwatch.watches.model.Watch;
import dev.atelierwatch.watches.repository.WatchRepository;
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
    private UserRepository userRepository;

    @Autowired
    private WatchRepository watchRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

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
    @DisplayName("Unpriced catalog records cannot enter cart, checkout quote, order, or payment flow")
    void testUnpricedCatalogItemCannotBePurchased() throws Exception {
        String token = getAuthToken("collector@atelierwatch.dev", "Admin@123456");
        Watch watch = watchRepository.findBySlug("rolex-126334").orElseThrow();
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new dev.atelierwatch.ecommerce.dto.CartItemRequest(watch.getId(), 1))))
                .andExpect(status().isBadRequest());

        User user = userRepository.findByEmail("collector@atelierwatch.dev").orElseThrow();
        Cart cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> cartRepository.save(new Cart(user)));
        cart.getItems().add(new CartItem(cart, watch, 1));
        cartRepository.saveAndFlush(cart);
        mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkoutAvailable").value(false))
                .andExpect(jsonPath("$.data.items[0].purchasable").value(false))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(nullValue()));

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

        mockMvc.perform(get("/api/v1/checkout/quote?addressId=" + addressId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        CreateOrderRequest orderReq = new CreateOrderRequest(addressId);
        mockMvc.perform(post("/api/v1/checkout/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/me/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
        org.junit.jupiter.api.Assertions.assertEquals(0, orderRepository.count());
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
