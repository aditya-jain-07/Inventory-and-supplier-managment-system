import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class DBConnectionTest {

    @Test
    @DisplayName("Verify MySQL Connector/J driver class is available from local JAR")
    void testDriverClassAvailability() {
        assertTrue(DBConnection.isDriverAvailable(),
                "Driver class com.mysql.cj.jdbc.Driver must be loaded from local lib JAR");
        assertDoesNotThrow(() -> Class.forName("com.mysql.cj.jdbc.Driver"),
                "Class.forName(com.mysql.cj.jdbc.Driver) should succeed with local JAR");
    }

    @Test
    @DisplayName("Verify DBConnection configuration and safe connection string")
    void testConfigurationDefaults() {
        assertNotNull(DBConnection.getUrl());
        assertTrue(DBConnection.getUrl().contains("dvms_db"));
        assertEquals("root", DBConnection.getUser());
        assertEquals("MySql@2008", DBConnection.getPassword());

        String safeInfo = DBConnection.getSafeConnectionInfo();
        assertTrue(safeInfo.contains("dvms_db"));
        assertTrue(safeInfo.contains("root"));
        assertFalse(safeInfo.contains("MySql@2008"), "Password must never be logged or exposed in safe info");
    }

    @Test
    @DisplayName("Verify System property override capability")
    void testSystemPropertyOverride() {
        String originalUrl = System.getProperty("db.url");
        try {
            System.setProperty("db.url", "jdbc:mysql://localhost:3306/test_override_db");
            assertEquals("jdbc:mysql://localhost:3306/test_override_db", DBConnection.getUrl());
        } finally {
            if (originalUrl != null) {
                System.setProperty("db.url", originalUrl);
            } else {
                System.clearProperty("db.url");
            }
        }
    }

    @Test
    @DisplayName("Verify Database wrapper delegates properly to DBConnection")
    void testDatabaseDelegation() throws Exception {
        try (Connection c = Database.getConnection()) {
            assertNotNull(c, "Database.getConnection() must return an active connection");
            assertFalse(c.isClosed(), "Connection must not be closed");
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    @Test
    @DisplayName("Test live MySQL connectivity, dvms_db schema, and table assertions")
    void testLiveMySQLConnection() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");

            // Verify basic query
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }

            // Verify database metadata and schema tables existence
            DatabaseMetaData metaData = conn.getMetaData();
            assertEquals("dvms_db", conn.getCatalog(), "Active catalog must be dvms_db");

            boolean hasInventory = false;
            boolean hasSuppliers = false;

            try (ResultSet rs = metaData.getTables(conn.getCatalog(), null, "inventory", null)) {
                hasInventory = rs.next();
            }
            try (ResultSet rs = metaData.getTables(conn.getCatalog(), null, "suppliers", null)) {
                hasSuppliers = rs.next();
            }

            assertTrue(hasInventory, "Table 'inventory' must exist in dvms_db");
            assertTrue(hasSuppliers, "Table 'suppliers' must exist in dvms_db");
        }
    }
}
