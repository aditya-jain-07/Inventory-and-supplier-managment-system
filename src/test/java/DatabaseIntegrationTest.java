import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("Verify MySQL Server connection, authentication, and SELECT 1")
    void testConnectionAndPing() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn, "Connection must not be null");
            assertFalse(conn.isClosed(), "Connection must be active");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("Verify target database dvms_db exists and schema tables are present")
    void testDatabaseSchemaTables() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String catalog = conn.getCatalog();
            assertEquals("dvms_db", catalog, "Connected catalog should be dvms_db");

            DatabaseMetaData md = conn.getMetaData();
            boolean hasInventory = false;
            boolean hasSuppliers = false;

            try (ResultSet rs = md.getTables(catalog, null, "inventory", null)) {
                if (rs.next()) hasInventory = true;
            }
            try (ResultSet rs = md.getTables(catalog, null, "suppliers", null)) {
                if (rs.next()) hasSuppliers = true;
            }

            assertTrue(hasInventory, "Table 'inventory' must exist in dvms_db");
            assertTrue(hasSuppliers, "Table 'suppliers' must exist in dvms_db");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Verify loading existing records via query helpers without errors")
    void testQueryExistingRecords() throws SQLException {
        List<Object[]> invRows = InventoryAndSupplierSystem.queryInventoryRows();
        assertNotNull(invRows);
        System.out.println("Current inventory row count: " + invRows.size());

        List<Object[]> supRows = InventoryAndSupplierSystem.querySupplierRows();
        assertNotNull(supRows);
        System.out.println("Current supplier row count: " + supRows.size());
    }

    @Test
    @Order(4)
    @DisplayName("Verify Rollback-Safe CRUD operations on 'inventory' table")
    void testInventoryCrudRollbackSafe() throws SQLException {
        final String testId = "TEST_ITM_" + System.currentTimeMillis();
        final String testName = "Temporary Test Widget";
        final String testCat = "Hardware";
        final int initialQty = 15;
        final BigDecimal initialPrice = new BigDecimal("49.99");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. CREATE (Insert)
                String insertSql = "INSERT INTO inventory (item_id, item_name, category, quantity, price) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, testId);
                    ps.setString(2, testName);
                    ps.setString(3, testCat);
                    ps.setInt(4, initialQty);
                    ps.setBigDecimal(5, initialPrice);
                    int inserted = ps.executeUpdate();
                    assertEquals(1, inserted, "One row should be inserted");
                }

                // 2. READ (Select)
                String selectSql = "SELECT item_name, category, quantity, price FROM inventory WHERE item_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, testId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next(), "Inserted row must be queryable");
                        assertEquals(testName, rs.getString("item_name"));
                        assertEquals(testCat, rs.getString("category"));
                        assertEquals(initialQty, rs.getInt("quantity"));
                        assertEquals(0, initialPrice.compareTo(rs.getBigDecimal("price")));
                    }
                }

                // 3. UPDATE
                String updateSql = "UPDATE inventory SET item_name=?, quantity=?, price=? WHERE item_id=?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, "Updated Widget");
                    ps.setInt(2, 25);
                    ps.setBigDecimal(3, new BigDecimal("59.99"));
                    ps.setString(4, testId);
                    int updated = ps.executeUpdate();
                    assertEquals(1, updated, "One row should be updated");
                }

                // Verify updated state
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, testId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals("Updated Widget", rs.getString("item_name"));
                        assertEquals(25, rs.getInt("quantity"));
                        assertEquals(0, new BigDecimal("59.99").compareTo(rs.getBigDecimal("price")));
                    }
                }

                // 4. DUPLICATE KEY VALIDATION
                assertThrows(SQLIntegrityConstraintViolationException.class, () -> {
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setString(1, testId); // duplicate ID
                        ps.setString(2, "Duplicate");
                        ps.setString(3, testCat);
                        ps.setInt(4, 5);
                        ps.setBigDecimal(5, BigDecimal.TEN);
                        ps.executeUpdate();
                    }
                }, "Inserting a duplicate primary key must throw SQLIntegrityConstraintViolationException");

                // 5. DELETE
                String deleteSql = "DELETE FROM inventory WHERE item_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setString(1, testId);
                    int deleted = ps.executeUpdate();
                    assertEquals(1, deleted, "One row should be deleted");
                }

                // Verify deletion
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, testId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertFalse(rs.next(), "Deleted item must no longer exist");
                    }
                }
            } finally {
                // Rollback transaction to ensure zero pollution of the database
                conn.rollback();
                conn.setAutoCommit(true);
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("Verify Rollback-Safe CRUD operations on 'suppliers' table")
    void testSuppliersCrudRollbackSafe() throws SQLException {
        final String testId = "TEST_SUP_" + System.currentTimeMillis();
        final String testName = "Apex Global Logistics";
        final String testType = "Distributor";
        final String testPhone = "+91-9876543210";
        final String testProduct = "Electronic Sensors";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. CREATE (Insert)
                String insertSql = "INSERT INTO suppliers (supplier_id, supplier_name, supplier_type, phone, product) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, testId);
                    ps.setString(2, testName);
                    ps.setString(3, testType);
                    ps.setString(4, testPhone);
                    ps.setString(5, testProduct);
                    int inserted = ps.executeUpdate();
                    assertEquals(1, inserted, "One row should be inserted");
                }

                // 2. READ (Select)
                String selectSql = "SELECT supplier_name, supplier_type, phone, product FROM suppliers WHERE supplier_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, testId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next(), "Inserted supplier must be queryable");
                        assertEquals(testName, rs.getString("supplier_name"));
                        assertEquals(testType, rs.getString("supplier_type"));
                        assertEquals(testPhone, rs.getString("phone"));
                        assertEquals(testProduct, rs.getString("product"));
                    }
                }

                // 3. UPDATE
                String updateSql = "UPDATE suppliers SET supplier_name=?, phone=?, product=? WHERE supplier_id=?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, "Apex Logistics International");
                    ps.setString(2, "+91-1122334455");
                    ps.setString(3, "Sensors & Controllers");
                    ps.setString(4, testId);
                    int updated = ps.executeUpdate();
                    assertEquals(1, updated, "One row should be updated");
                }

                // Verify updated state
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, testId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals("Apex Logistics International", rs.getString("supplier_name"));
                        assertEquals("+91-1122334455", rs.getString("phone"));
                        assertEquals("Sensors & Controllers", rs.getString("product"));
                    }
                }

                // 4. DUPLICATE KEY VALIDATION
                assertThrows(SQLIntegrityConstraintViolationException.class, () -> {
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setString(1, testId); // duplicate ID
                        ps.setString(2, "Duplicate Supplier");
                        ps.setString(3, testType);
                        ps.setString(4, testPhone);
                        ps.setString(5, testProduct);
                        ps.executeUpdate();
                    }
                }, "Inserting a duplicate supplier ID must throw SQLIntegrityConstraintViolationException");

                // 5. DELETE
                String deleteSql = "DELETE FROM suppliers WHERE supplier_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setString(1, testId);
                    int deleted = ps.executeUpdate();
                    assertEquals(1, deleted, "One supplier row should be deleted");
                }

                // Verify deletion
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, testId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertFalse(rs.next(), "Deleted supplier must no longer exist");
                    }
                }
            } finally {
                // Rollback transaction to ensure zero pollution of the database
                conn.rollback();
                conn.setAutoCommit(true);
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("Verify real auto-commit persistence across distinct connections with verified cleanup")
    void testInventoryCommittedPersistenceCycle() throws SQLException {
        final String tempId = "COMMITTED_ITM_" + System.currentTimeMillis();
        try (Connection conn1 = DBConnection.getConnection()) {
            assertTrue(conn1.getAutoCommit(), "Default connection should be auto-commit");
            String insertSql = "INSERT INTO inventory (item_id, item_name, category, quantity, price) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn1.prepareStatement(insertSql)) {
                ps.setString(1, tempId);
                ps.setString(2, "Committed Verification Widget");
                ps.setString(3, "Electronics");
                ps.setInt(4, 12);
                ps.setBigDecimal(5, new BigDecimal("89.99"));
                assertEquals(1, ps.executeUpdate());
            }
        }

        // Open distinct connection to verify data was persisted to disk/server
        try (Connection conn2 = DBConnection.getConnection()) {
            String selectSql = "SELECT item_name, quantity, price FROM inventory WHERE item_id = ?";
            try (PreparedStatement ps = conn2.prepareStatement(selectSql)) {
                ps.setString(1, tempId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "Committed record must be visible across distinct connections");
                    assertEquals("Committed Verification Widget", rs.getString("item_name"));
                    assertEquals(12, rs.getInt("quantity"));
                    assertEquals(0, new BigDecimal("89.99").compareTo(rs.getBigDecimal("price")));
                }
            }
        } finally {
            // Cleanup record
            try (Connection cleanConn = DBConnection.getConnection();
                 PreparedStatement ps = cleanConn.prepareStatement("DELETE FROM inventory WHERE item_id = ?")) {
                ps.setString(1, tempId);
                ps.executeUpdate();
            }
        }

        // Verify cleanup
        try (Connection verifyConn = DBConnection.getConnection();
             PreparedStatement ps = verifyConn.prepareStatement("SELECT item_id FROM inventory WHERE item_id = ?")) {
            ps.setString(1, tempId);
            try (ResultSet rs = ps.executeQuery()) {
                assertFalse(rs.next(), "Temporary record must be cleaned up");
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("Verify real auto-commit persistence across distinct connections for suppliers with verified cleanup")
    void testSuppliersCommittedPersistenceCycle() throws SQLException {
        final String tempId = "COMMITTED_SUP_" + System.currentTimeMillis();
        try (Connection conn1 = DBConnection.getConnection()) {
            assertTrue(conn1.getAutoCommit(), "Default connection should be auto-commit");
            String insertSql = "INSERT INTO suppliers (supplier_id, supplier_name, supplier_type, phone, product) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn1.prepareStatement(insertSql)) {
                ps.setString(1, tempId);
                ps.setString(2, "Committed Supplier Ltd");
                ps.setString(3, "Wholesaler");
                ps.setString(4, "+91-9988776655");
                ps.setString(5, "Hardware Components");
                assertEquals(1, ps.executeUpdate());
            }
        }

        // Open distinct connection to verify data was persisted to disk/server
        try (Connection conn2 = DBConnection.getConnection()) {
            String selectSql = "SELECT supplier_name, supplier_type FROM suppliers WHERE supplier_id = ?";
            try (PreparedStatement ps = conn2.prepareStatement(selectSql)) {
                ps.setString(1, tempId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "Committed supplier must be visible across distinct connections");
                    assertEquals("Committed Supplier Ltd", rs.getString("supplier_name"));
                    assertEquals("Wholesaler", rs.getString("supplier_type"));
                }
            }
        } finally {
            // Cleanup record
            try (Connection cleanConn = DBConnection.getConnection();
                 PreparedStatement ps = cleanConn.prepareStatement("DELETE FROM suppliers WHERE supplier_id = ?")) {
                ps.setString(1, tempId);
                ps.executeUpdate();
            }
        }

        // Verify cleanup
        try (Connection verifyConn = DBConnection.getConnection();
             PreparedStatement ps = verifyConn.prepareStatement("SELECT supplier_id FROM suppliers WHERE supplier_id = ?")) {
            ps.setString(1, tempId);
            try (ResultSet rs = ps.executeQuery()) {
                assertFalse(rs.next(), "Temporary supplier must be cleaned up");
            }
        }
    }
}
