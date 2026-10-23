package dev.atelierwatch.watches;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class WatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/watches returns only the 17 published Rolex manifest records")
    void testGetWatches() throws Exception {
        mockMvc.perform(get("/api/v1/watches").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(17)))
                .andExpect(jsonPath("$.data.content[*].brand", everyItem(is("Rolex"))))
                .andExpect(jsonPath("$.data.content[?(@.referenceNumber == '126334')]", hasSize(1)))
                .andExpect(jsonPath("$.data.content[?(@.referenceNumber == '336935')].slug", contains("rolex-336935")))
                .andExpect(jsonPath("$.data.content[?(@.referenceNumber == '336935')].price", contains(nullValue())))
                .andExpect(jsonPath("$.data.content[?(@.referenceNumber == '336935')].purchasable", contains(false)));
    }

    @Test
    @DisplayName("GET /api/v1/watches/{slug} returns source-backed Rolex details and provenance")
    void testGetWatchBySlug() throws Exception {
        mockMvc.perform(get("/api/v1/watches/rolex-126334"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Datejust 41"))
                .andExpect(jsonPath("$.data.referenceNumber").value("126334"))
                .andExpect(jsonPath("$.data.caseMaterial").value("White Rolesor"))
                .andExpect(jsonPath("$.data.waterResistanceText").value("Waterproof to 100 metres / 330 feet"))
                .andExpect(jsonPath("$.data.sourcePdf").value("m126334-0033.pdf"))
                .andExpect(jsonPath("$.data.sourceSpecificationPages").value("8-9"))
                .andExpect(jsonPath("$.data.imageSha256").value("3be4a6b26405a1aaa35e5524af531d186560c76dcf9e5083ef6922ca1274797b"))
                .andExpect(jsonPath("$.data.price").value(nullValue()))
                .andExpect(jsonPath("$.data.caseThicknessMm").value(nullValue()))
                .andExpect(jsonPath("$.data.purchasable").value(false));
    }

    @Test
    @DisplayName("Public details and search never expose archived non-Rolex demo watches")
    void testPublicCatalogIsRolexOnly() throws Exception {
        mockMvc.perform(get("/api/v1/watches/omega-speedmaster-professional-moonwatch"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/watches").param("brand", "OMEGA").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
        mockMvc.perform(get("/api/v1/watches/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", contains("Rolex")));
    }

    @Test
    @DisplayName("Reference search and collection filtering use verified manifest values")
    void testReferenceSearchAndCollectionFilter() throws Exception {
        mockMvc.perform(get("/api/v1/watches").param("search", "126613LB").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].referenceNumber").value("126613LB"));
        mockMvc.perform(get("/api/v1/watches").param("collection", "submariner").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].collectionName").value("Submariner"));
    }

    @Test
    @DisplayName("Flyway archives old rows in place and leaves seeded users and ecommerce tables intact")
    void testMigrationPreservesLegacyRowsAndUsers() {
        assertEquals(29, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM watches", Integer.class));
        assertEquals(12, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM watches WHERE status = 'ARCHIVED'", Integer.class));
        assertEquals(17, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM watches WHERE status = 'PUBLISHED' AND brand = 'Rolex'", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
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
    @DisplayName("GET /api/v1/watches/compare returns published Rolex records")
    void testCompareWatches() throws Exception {
        String page = mockMvc.perform(get("/api/v1/watches").param("size", "2").param("sort", "referenceNumber,asc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(page).path("data").path("content");
        String ids = content.get(0).path("id").asText() + "," + content.get(1).path("id").asText();
        mockMvc.perform(get("/api/v1/watches/compare").param("ids", ids))
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
