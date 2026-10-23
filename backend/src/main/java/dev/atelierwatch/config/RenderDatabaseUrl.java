package dev.atelierwatch.config;

import java.net.URI;
import java.util.Locale;

/** Converts Render's PostgreSQL URI into pgJDBC's URL and separate credentials. */
public final class RenderDatabaseUrl {

    private RenderDatabaseUrl() {
    }

    public static Connection parse(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw invalidDatabaseUrl();
        }

        final URI uri;
        try {
            uri = URI.create(databaseUrl);
        } catch (IllegalArgumentException exception) {
            throw invalidDatabaseUrl();
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.toLowerCase(Locale.ROOT).equals("postgres")
                || scheme.toLowerCase(Locale.ROOT).equals("postgresql"))) {
            throw invalidDatabaseUrl();
        }

        String userInfo = uri.getUserInfo();
        int credentialSeparator = userInfo == null ? -1 : userInfo.indexOf(':');
        String host = uri.getHost();
        String path = uri.getRawPath();
        if (credentialSeparator <= 0 || credentialSeparator == userInfo.length() - 1
                || host == null || path == null || path.length() < 2 || uri.getFragment() != null) {
            throw invalidDatabaseUrl();
        }

        String jdbcHost = host.indexOf(':') >= 0 && !host.startsWith("[") ? "[" + host + "]" : host;
        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(jdbcHost);
        if (uri.getPort() >= 0) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        jdbcUrl.append(path);
        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            jdbcUrl.append('?').append(uri.getRawQuery());
        }

        return new Connection(
                jdbcUrl.toString(),
                userInfo.substring(0, credentialSeparator),
                userInfo.substring(credentialSeparator + 1));
    }

    private static IllegalArgumentException invalidDatabaseUrl() {
        // Keep credentials out of startup errors and logs.
        return new IllegalArgumentException(
                "DATABASE_URL must be a PostgreSQL URL with a host, database, username, and password.");
    }

    public record Connection(String jdbcUrl, String username, String password) {
    }
}
