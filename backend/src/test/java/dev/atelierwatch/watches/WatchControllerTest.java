package dev.atelierwatch.watches;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class WatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/watches returns paginated seed watches")
    void testGetWatches() throws Exception {
        mockMvc.perform(get("/api/v1/watches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(8))))
                .andExpect(jsonPath("$.data.content[0].name").exists());
    }

    @Test
    @DisplayName("GET /api/v1/watches/{slug} returns the Sky-Dweller details")
    void testGetWatchBySlug() throws Exception {
        mockMvc.perform(get("/api/v1/watches/rolex-sky-dweller-everose-green"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sky-Dweller Annual Calendar"))
                .andExpect(jsonPath("$.data.caseMaterial").value("18 ct Everose Gold"))
                .andExpect(jsonPath("$.data.waterResistanceM").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/categories returns luxury watch categories")
    void testGetCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(5))));
    }

    @Test
    @DisplayName("GET /api/v1/watches/compare returns comparison matrix")
    void testCompareWatches() throws Exception {
        mockMvc.perform(get("/api/v1/watches/compare?ids=1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.watches", hasSize(2)));
    }

    @Test
    @DisplayName("GET /actuator/health returns UP status")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
