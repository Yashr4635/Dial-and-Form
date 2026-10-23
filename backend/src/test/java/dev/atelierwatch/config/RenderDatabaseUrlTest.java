package dev.atelierwatch.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RenderDatabaseUrlTest {

    @Test
    void convertsRenderUriToJdbcUrlAndDecodesSeparateCredentials() {
        RenderDatabaseUrl.Connection connection = RenderDatabaseUrl.parse(
                "postgresql://catalog_user:p%40ss%3Aword@db.internal:5432/dial_and_form_staging?sslmode=require");

        assertEquals("jdbc:postgresql://db.internal:5432/dial_and_form_staging?sslmode=require",
                connection.jdbcUrl());
        assertEquals("catalog_user", connection.username());
        assertEquals("p@ss:word", connection.password());
    }

    @Test
    void supportsThePostgresSchemeAndDoesNotPutCredentialsInJdbcUrl() {
        RenderDatabaseUrl.Connection connection = RenderDatabaseUrl.parse(
                "postgres://catalog_user:secret@db.internal/dial_and_form_staging");

        assertEquals("jdbc:postgresql://db.internal/dial_and_form_staging", connection.jdbcUrl());
        assertFalse(connection.jdbcUrl().contains("catalog_user"));
        assertFalse(connection.jdbcUrl().contains("secret"));
    }

    @Test
    void reportsInvalidUrlsWithoutEchoingTheInput() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> RenderDatabaseUrl.parse("postgresql://catalog_user:secret@/broken"));

        assertFalse(error.getMessage().contains("secret"));
    }
}
