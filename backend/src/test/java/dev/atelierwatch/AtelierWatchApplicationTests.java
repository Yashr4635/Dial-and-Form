package dev.atelierwatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class AtelierWatchApplicationTests {

    @Test
    void contextLoads() {
        // Confirms Spring context initializes cleanly with H2 and Flyway migrations
    }
}
