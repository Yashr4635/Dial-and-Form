package dev.atelierwatch.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.atelierwatch.auth.dto.AuthRequest;
import dev.atelierwatch.ecommerce.dto.CreateOrderRequest;
import dev.atelierwatch.ecommerce.dto.PaymentVerificationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.razorpay.simulation-enabled=false")
class PaymentDisabledSafetyTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void paymentEndpointsStayUnavailableWhenSimulationIsDisabled() throws Exception {
        String login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("collector@atelierwatch.dev", "Admin@123456"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String auth = "Bearer " + objectMapper.readTree(login).path("data").path("token").asText();

        mockMvc.perform(post("/api/v1/payments/razorpay/create-order")
                        .header("Authorization", auth).param("orderReference", "AW-2026-12345"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(post("/api/v1/payments/razorpay/verify")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentVerificationRequest(
                                "AW-2026-12345", "demo_order_1", "pay_test_1", "signature"))))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/v1/payments/razorpay/simulate-signature")
                        .header("Authorization", auth)
                        .param("orderReference", "AW-2026-12345")
                        .param("razorpayPaymentId", "pay_test_1"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/v1/checkout/quote")
                        .header("Authorization", auth).param("addressId", "1"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(post("/api/v1/checkout/orders")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(1L))))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(post("/api/v1/payments/webhook").content("{}"))
                .andExpect(status().isServiceUnavailable());
    }
}
