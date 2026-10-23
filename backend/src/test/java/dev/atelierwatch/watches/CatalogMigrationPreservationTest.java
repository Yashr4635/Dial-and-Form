package dev.atelierwatch.watches;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CatalogMigrationPreservationTest {

    @Test
    void forwardMigrationPreservesHistoricalCommerceSnapshotsAndReferences() throws Exception {
        String url = "jdbc:h2:mem:catalog_preservation_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("4"))
                .load()
                .migrate();

        long userId;
        long legacyWatchId;
        long cartId;
        long orderId;
        String userEmail;
        String userPasswordHash;
        String userRole;
        String legacyName;
        String legacyBrand;
        String legacyReference;
        String legacyImage;
        double legacyUnitPrice;
        double legacyTotal;
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            userId = firstLong(connection, "SELECT id FROM users WHERE email = 'collector@atelierwatch.dev'");
            userEmail = firstString(connection, "SELECT email FROM users WHERE id = " + userId);
            userPasswordHash = firstString(connection, "SELECT password_hash FROM users WHERE id = " + userId);
            userRole = firstString(connection, "SELECT role FROM users WHERE id = " + userId);
            legacyWatchId = firstLong(connection, "SELECT id FROM watches WHERE brand <> 'Rolex' ORDER BY id LIMIT 1");
            legacyName = firstString(connection, "SELECT name FROM watches WHERE id = " + legacyWatchId);
            legacyBrand = firstString(connection, "SELECT brand FROM watches WHERE id = " + legacyWatchId);
            legacyReference = firstString(connection, "SELECT reference_number FROM watches WHERE id = " + legacyWatchId);
            legacyImage = firstString(connection, "SELECT image_url FROM watches WHERE id = " + legacyWatchId);
            legacyUnitPrice = scalarDouble(connection, "SELECT price FROM watches WHERE id = " + legacyWatchId);
            legacyTotal = legacyUnitPrice * 2;

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO carts (user_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, userId);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    cartId = keys.getLong(1);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO cart_items (cart_id, watch_id, quantity) VALUES (?, ?, 2)")) {
                statement.setLong(1, cartId);
                statement.setLong(2, legacyWatchId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO wishlist_items (user_id, watch_id) VALUES (?, ?)")) {
                statement.setLong(1, userId);
                statement.setLong(2, legacyWatchId);
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO orders (order_reference, user_id, shipping_name, shipping_phone,
                        shipping_address_line1, shipping_city, shipping_state, shipping_postal_code,
                        shipping_country, subtotal, shipping_fee, total_amount, currency, status)
                    VALUES ('AW-HISTORY-0001', ?, 'Historical Buyer', '+1-555-0100', '1 Legacy Lane',
                        'New York', 'NY', '10001', 'United States', ?, 25.00, ?,
                        'USD', 'CANCELLED')
                    """, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, userId);
                statement.setDouble(2, legacyTotal);
                statement.setDouble(3, legacyTotal + 25.00);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    orderId = keys.getLong(1);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO order_items (order_id, watch_id, snapshot_name, snapshot_brand,
                        snapshot_reference, snapshot_image_url, unit_price, quantity, total_price)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 2, ?)
                    """)) {
                statement.setLong(1, orderId);
                statement.setLong(2, legacyWatchId);
                statement.setString(3, legacyName);
                statement.setString(4, legacyBrand);
                statement.setString(5, legacyReference);
                statement.setString(6, legacyImage);
                statement.setDouble(7, legacyUnitPrice);
                statement.setDouble(8, legacyTotal);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO payments (order_id, gateway, amount, currency, status)
                    VALUES (?, 'LEGACY_GATEWAY', ?, 'USD', 'FAILED')
                    """)) {
                statement.setLong(1, orderId);
                statement.setDouble(2, legacyTotal + 25.00);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO shipments (order_id, provider, courier_name, tracking_number, status)
                    VALUES (?, 'LEGACY_CARRIER', 'Legacy Courier', 'LEGACY-TRACK-0001', 'CANCELLED')
                    """)) {
                statement.setLong(1, orderId);
                statement.executeUpdate();
            }
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            assertEquals(userEmail, firstString(connection, "SELECT email FROM users WHERE id = " + userId));
            assertEquals(userPasswordHash, firstString(connection, "SELECT password_hash FROM users WHERE id = " + userId));
            assertEquals(userRole, firstString(connection, "SELECT role FROM users WHERE id = " + userId));
            assertEquals(1, scalarLong(connection, "SELECT COUNT(*) FROM orders WHERE order_reference = 'AW-HISTORY-0001'"));
            assertEquals(1, scalarLong(connection, "SELECT COUNT(*) FROM order_items WHERE order_id = " + orderId));
            assertEquals(1, scalarLong(connection, "SELECT COUNT(*) FROM payments WHERE order_id = " + orderId + " AND status = 'FAILED'"));
            assertEquals(1, scalarLong(connection, "SELECT COUNT(*) FROM shipments WHERE order_id = " + orderId + " AND status = 'CANCELLED'"));
            assertEquals(1, scalarLong(connection, "SELECT COUNT(*) FROM wishlist_items WHERE user_id = " + userId + " AND watch_id = " + legacyWatchId));
            assertEquals(1, scalarLong(connection, "SELECT COUNT(*) FROM cart_items WHERE cart_id = " + cartId + " AND watch_id = " + legacyWatchId + " AND quantity = 2"));
            assertEquals("ARCHIVED", firstString(connection, "SELECT status FROM watches WHERE id = " + legacyWatchId));
            assertEquals(legacyName, firstString(connection, "SELECT snapshot_name FROM order_items WHERE order_id = " + orderId));
            assertEquals(legacyBrand, firstString(connection, "SELECT snapshot_brand FROM order_items WHERE order_id = " + orderId));
            assertEquals(legacyReference, firstString(connection, "SELECT snapshot_reference FROM order_items WHERE order_id = " + orderId));
            assertEquals(legacyImage, firstString(connection, "SELECT snapshot_image_url FROM order_items WHERE order_id = " + orderId));
            assertEquals(legacyUnitPrice, scalarDouble(connection, "SELECT unit_price FROM order_items WHERE order_id = " + orderId));
            assertNotNull(firstString(connection, "SELECT order_reference FROM orders WHERE id = " + orderId));
        }
    }

    private static long firstLong(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static String firstString(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private static long scalarLong(Connection connection, String sql) throws Exception {
        return firstLong(connection, sql);
    }

    private static double scalarDouble(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getDouble(1);
        }
    }
}
