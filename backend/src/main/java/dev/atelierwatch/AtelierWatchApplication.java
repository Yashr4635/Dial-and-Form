package dev.atelierwatch;

import dev.atelierwatch.config.RenderDatabaseUrl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AtelierWatchApplication {
    public static void main(String[] args) {
        configureRenderDatasource();
        SpringApplication.run(AtelierWatchApplication.class, args);
    }

    private static void configureRenderDatasource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        RenderDatabaseUrl.Connection connection = RenderDatabaseUrl.parse(databaseUrl);
        setIfMissing("spring.datasource.url", "SPRING_DATASOURCE_URL", connection.jdbcUrl());
        setIfMissing("spring.datasource.username", "SPRING_DATASOURCE_USERNAME", connection.username());
        setIfMissing("spring.datasource.password", "SPRING_DATASOURCE_PASSWORD", connection.password());
    }

    private static void setIfMissing(String propertyName, String environmentName, String value) {
        String configured = System.getenv(environmentName);
        if (configured == null || configured.isBlank()) {
            System.setProperty(propertyName, value);
        }
    }
}
