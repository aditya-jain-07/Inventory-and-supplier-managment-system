import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * DVMS Inventory and Supplier Management System
 * Java Swing GUI connected to MySQL through JDBC.
 *
 * Utilizes SwingWorker for non-blocking database operations, ensures
 * UI responsiveness, and prevents duplicate submissions while background
 * queries run.
 */
public class InventoryAndSupplierSystem extends JFrame implements ActionListener {

    // --- Inventory Components (Left Side) ---
    private JTextField txtItemId, txtItemName, txtQuantity, txtPrice;
    private JComboBox<String> comboCategory;
    private JButton btnAddInventory, btnUpdateInventory, btnDeleteInventory, btnClearInventory;
    private JTable inventoryTable;
    private DefaultTableModel inventoryModel;

    // --- Supplier Components (Right Side) ---
    private JTextField txtSupplierId, txtSupplierName, txtPhone, txtProduct;
    private JComboBox<String> comboSupplierType;
    private JButton btnAddSupplier, btnUpdateSupplier, btnDeleteSupplier, btnClearSupplier;
    private JTable supplierTable;
    private DefaultTableModel supplierModel;

    // --- System Control ---
    private JButton btnExit;

    public InventoryAndSupplierSystem() {
        setTitle("Inventory and Supplier Management System");
        setSize(1000, 600);
        setMinimumSize(new Dimension(900, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.add(createInventoryPanel());
        mainPanel.add(createSupplierPanel());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnExit = new JButton("Exit");
        btnExit.setPreferredSize(new Dimension(100, 30));
        btnExit.addActionListener(this);
        bottomPanel.add(btnExit);

        add(mainPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        loadInventory();
        loadSuppliers();
    }

    private JPanel createInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 10));
        JLabel lblTitle = new JLabel("INVENTORY MANAGEMENT", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(lblTitle, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 8));
        inputPanel.add(new JLabel("Item ID"));
        txtItemId = new JTextField();
        inputPanel.add(txtItemId);

        inputPanel.add(new JLabel("Item Name"));
        txtItemName = new JTextField();
        inputPanel.add(txtItemName);

        inputPanel.add(new JLabel("Category"));
        comboCategory = new JComboBox<>(new String[]{"Electronics", "Hardware", "Raw Material", "Software"});
        inputPanel.add(comboCategory);

        inputPanel.add(new JLabel("Quantity"));
        txtQuantity = new JTextField();
        inputPanel.add(txtQuantity);

        inputPanel.add(new JLabel("Price ($)"));
        txtPrice = new JTextField();
        inputPanel.add(txtPrice);

        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnAddInventory = new JButton("Add Item");
        btnUpdateInventory = new JButton("Update Selected");
        btnDeleteInventory = new JButton("Delete Selected");
        btnClearInventory = new JButton("Clear");
        btnAddInventory.addActionListener(this);
        btnUpdateInventory.addActionListener(this);
        btnDeleteInventory.addActionListener(this);
        btnClearInventory.addActionListener(this);
        btnPanel.add(btnAddInventory);
        btnPanel.add(btnUpdateInventory);
        btnPanel.add(btnDeleteInventory);
        btnPanel.add(btnClearInventory);

        JPanel topContainer = new JPanel(new BorderLayout(5, 10));
        topContainer.add(inputPanel, BorderLayout.CENTER);
        topContainer.add(btnPanel, BorderLayout.SOUTH);

        String[] columns = {"ID", "Name", "Category", "Qty", "Price"};
        inventoryModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        inventoryTable = new JTable(inventoryModel);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventoryTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = inventoryTable.getSelectedRow();
                if (row >= 0) {
                    txtItemId.setText(value(inventoryModel, row, 0));
                    txtItemName.setText(value(inventoryModel, row, 1));
                    comboCategory.setSelectedItem(value(inventoryModel, row, 2));
                    txtQuantity.setText(value(inventoryModel, row, 3));
                    txtPrice.setText(value(inventoryModel, row, 4));
                }
            }
        });

        panel.add(topContainer, BorderLayout.NORTH);
        panel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSupplierPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 10));
        JLabel lblTitle = new JLabel("SUPPLIER MANAGEMENT", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(lblTitle, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 8));
        inputPanel.add(new JLabel("Supplier ID"));
        txtSupplierId = new JTextField();
        inputPanel.add(txtSupplierId);

        inputPanel.add(new JLabel("Supplier Name"));
        txtSupplierName = new JTextField();
        inputPanel.add(txtSupplierName);

        inputPanel.add(new JLabel("Supplier Type"));
        comboSupplierType = new JComboBox<>(new String[]{"Local Supplier", "Wholesaler", "Manufacturer", "Distributor"});
        inputPanel.add(comboSupplierType);

        inputPanel.add(new JLabel("Phone Number"));
        txtPhone = new JTextField();
        inputPanel.add(txtPhone);

        inputPanel.add(new JLabel("Supplied Product"));
        txtProduct = new JTextField();
        inputPanel.add(txtProduct);

        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnAddSupplier = new JButton("Add Supplier");
        btnUpdateSupplier = new JButton("Update Selected");
        btnDeleteSupplier = new JButton("Delete Selected");
        btnClearSupplier = new JButton("Clear");
        btnAddSupplier.addActionListener(this);
        btnUpdateSupplier.addActionListener(this);
        btnDeleteSupplier.addActionListener(this);
        btnClearSupplier.addActionListener(this);
        btnPanel.add(btnAddSupplier);
        btnPanel.add(btnUpdateSupplier);
        btnPanel.add(btnDeleteSupplier);
        btnPanel.add(btnClearSupplier);

        JPanel topContainer = new JPanel(new BorderLayout(5, 10));
        topContainer.add(inputPanel, BorderLayout.CENTER);
        topContainer.add(btnPanel, BorderLayout.SOUTH);

        String[] columns = {"ID", "Name", "Type", "Phone", "Product"};
        supplierModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        supplierTable = new JTable(supplierModel);
        supplierTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        supplierTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = supplierTable.getSelectedRow();
                if (row >= 0) {
                    txtSupplierId.setText(value(supplierModel, row, 0));
                    txtSupplierName.setText(value(supplierModel, row, 1));
                    comboSupplierType.setSelectedItem(value(supplierModel, row, 2));
                    txtPhone.setText(value(supplierModel, row, 3));
                    txtProduct.setText(value(supplierModel, row, 4));
                }
            }
        });

        panel.add(topContainer, BorderLayout.NORTH);
        panel.add(new JScrollPane(supplierTable), BorderLayout.CENTER);
        return panel;
    }

    private String value(DefaultTableModel model, int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == btnExit) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?",
                    "Exit System", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) dispose();
        } else if (source == btnAddInventory) {
            addInventory();
        } else if (source == btnUpdateInventory) {
            updateInventory();
        } else if (source == btnDeleteInventory) {
            deleteInventory();
        } else if (source == btnClearInventory) {
            clearInventoryFields();
        } else if (source == btnAddSupplier) {
            addSupplier();
        } else if (source == btnUpdateSupplier) {
            updateSupplier();
        } else if (source == btnDeleteSupplier) {
            deleteSupplier();
        } else if (source == btnClearSupplier) {
            clearSupplierFields();
        }
    }

    // --- Inventory Operations with SwingWorker ---

    private void setInventoryButtonsEnabled(boolean enabled) {
        btnAddInventory.setEnabled(enabled);
        btnUpdateInventory.setEnabled(enabled);
        btnDeleteInventory.setEnabled(enabled);
        btnClearInventory.setEnabled(enabled);
    }

    private void setSupplierButtonsEnabled(boolean enabled) {
        btnAddSupplier.setEnabled(enabled);
        btnUpdateSupplier.setEnabled(enabled);
        btnDeleteSupplier.setEnabled(enabled);
        btnClearSupplier.setEnabled(enabled);
    }

    public void addInventory() {
        final InventoryData d;
        try {
            d = readInventoryForm();
        } catch (NumberFormatException ex) {
            showError("Quantity must be a whole number and price must be a valid number.");
            return;
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        setInventoryButtonsEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String sql = "INSERT INTO inventory (item_id, item_name, category, quantity, price) VALUES (?, ?, ?, ?, ?)";
                try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, d.id);
                    ps.setString(2, d.name);
                    ps.setString(3, d.category);
                    ps.setInt(4, d.quantity);
                    ps.setBigDecimal(5, d.price);
                    ps.executeUpdate();
                }
                return null;
            }

            @Override
            protected void done() {
                setInventoryButtonsEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                    loadInventory();
                    clearInventoryFields();
                    JOptionPane.showMessageDialog(InventoryAndSupplierSystem.this, "Inventory item added successfully.");
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        }.execute();
    }

    public void updateInventory() {
        int row = inventoryTable.getSelectedRow();
        if (row < 0) {
            showWarning("Please select a row from the Inventory table to update.");
            return;
        }
        final String originalId = value(inventoryModel, row, 0);

        final InventoryData d;
        try {
            d = readInventoryForm();
        } catch (NumberFormatException ex) {
            showError("Quantity must be a whole number and price must be a valid number.");
            return;
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        setInventoryButtonsEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                String sql = "UPDATE inventory SET item_id=?, item_name=?, category=?, quantity=?, price=? WHERE item_id=?";
                try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, d.id);
                    ps.setString(2, d.name);
                    ps.setString(3, d.category);
                    ps.setInt(4, d.quantity);
                    ps.setBigDecimal(5, d.price);
                    ps.setString(6, originalId);
                    int updated = ps.executeUpdate();
                    return updated > 0;
                }
            }

            @Override
            protected void done() {
                setInventoryButtonsEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                try {
                    boolean updated = get();
                    if (!updated) {
                        showWarning("The inventory record was not found.");
                        return;
                    }
                    loadInventory();
                    clearInventoryFields();
                    JOptionPane.showMessageDialog(InventoryAndSupplierSystem.this, "Inventory row updated successfully.");
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        }.execute();
    }

    public void deleteInventory() {
        int row = inventoryTable.getSelectedRow();
        if (row < 0) {
            showWarning("Please select a row from the Inventory table to delete.");
            return;
        }
        final String id = value(inventoryModel, row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete inventory item " + id + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        setInventoryButtonsEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement("DELETE FROM inventory WHERE item_id=?")) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                }
                return null;
            }

            @Override
            protected void done() {
                setInventoryButtonsEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                    loadInventory();
                    clearInventoryFields();
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        }.execute();
    }

    // --- Supplier Operations with SwingWorker ---

    public void addSupplier() {
        final SupplierData d;
        try {
            d = readSupplierForm();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        setSupplierButtonsEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String sql = "INSERT INTO suppliers (supplier_id, supplier_name, supplier_type, phone, product) VALUES (?, ?, ?, ?, ?)";
                try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, d.id);
                    ps.setString(2, d.name);
                    ps.setString(3, d.type);
                    ps.setString(4, d.phone);
                    ps.setString(5, d.product);
                    ps.executeUpdate();
                }
                return null;
            }

            @Override
            protected void done() {
                setSupplierButtonsEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                    loadSuppliers();
                    clearSupplierFields();
                    JOptionPane.showMessageDialog(InventoryAndSupplierSystem.this, "Supplier added successfully.");
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        }.execute();
    }

    public void updateSupplier() {
        int row = supplierTable.getSelectedRow();
        if (row < 0) {
            showWarning("Please select a row from the Supplier table to update.");
            return;
        }
        final String originalId = value(supplierModel, row, 0);

        final SupplierData d;
        try {
            d = readSupplierForm();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        setSupplierButtonsEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                String sql = "UPDATE suppliers SET supplier_id=?, supplier_name=?, supplier_type=?, phone=?, product=? WHERE supplier_id=?";
                try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, d.id);
                    ps.setString(2, d.name);
                    ps.setString(3, d.type);
                    ps.setString(4, d.phone);
                    ps.setString(5, d.product);
                    ps.setString(6, originalId);
                    int updated = ps.executeUpdate();
                    return updated > 0;
                }
            }

            @Override
            protected void done() {
                setSupplierButtonsEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                try {
                    boolean updated = get();
                    if (!updated) {
                        showWarning("The supplier record was not found.");
                        return;
                    }
                    loadSuppliers();
                    clearSupplierFields();
                    JOptionPane.showMessageDialog(InventoryAndSupplierSystem.this, "Supplier row updated successfully.");
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        }.execute();
    }

    public void deleteSupplier() {
        int row = supplierTable.getSelectedRow();
        if (row < 0) {
            showWarning("Please select a row from the Supplier table to delete.");
            return;
        }
        final String id = value(supplierModel, row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete supplier " + id + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        setSupplierButtonsEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement("DELETE FROM suppliers WHERE supplier_id=?")) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                }
                return null;
            }

            @Override
            protected void done() {
                setSupplierButtonsEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                    loadSuppliers();
                    clearSupplierFields();
                } catch (Exception ex) {
                    handleException(ex);
                }
            }
        }.execute();
    }

    // --- Form Parsing & Validation ---

    private InventoryData readInventoryForm() {
        String id = txtItemId.getText().trim();
        String name = txtItemName.getText().trim();
        String quantityText = txtQuantity.getText().trim();
        String priceText = txtPrice.getText().trim();
        if (id.isEmpty() || name.isEmpty()) throw new IllegalArgumentException("Please fill in Item ID and Name.");
        if (quantityText.isEmpty() || priceText.isEmpty()) throw new IllegalArgumentException("Please fill in Quantity and Price.");
        int quantity = Integer.parseInt(quantityText);
        BigDecimal price = new BigDecimal(priceText);
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        if (price.signum() < 0) throw new IllegalArgumentException("Price cannot be negative.");
        return new InventoryData(id, name, Objects.toString(comboCategory.getSelectedItem(), ""),
                quantity, price);
    }

    private SupplierData readSupplierForm() {
        String id = txtSupplierId.getText().trim();
        String name = txtSupplierName.getText().trim();
        if (id.isEmpty() || name.isEmpty()) throw new IllegalArgumentException("Please fill in Supplier ID and Name.");
        return new SupplierData(id, name, Objects.toString(comboSupplierType.getSelectedItem(), ""),
                txtPhone.getText().trim(), txtProduct.getText().trim());
    }

    // --- Loading Tables Asynchronously via SwingWorker ---

    public void loadInventory() {
        if (inventoryModel == null) return;
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                return queryInventoryRows();
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    inventoryModel.setRowCount(0);
                    for (Object[] row : rows) {
                        inventoryModel.addRow(row);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
                    showError("Could not load inventory:\n" + cause.getMessage()
                            + "\n\nRun database/dvms_db.sql and check Database.java.");
                }
            }
        }.execute();
    }

    public void loadSuppliers() {
        if (supplierModel == null) return;
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                return querySupplierRows();
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    supplierModel.setRowCount(0);
                    for (Object[] row : rows) {
                        supplierModel.addRow(row);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
                    showError("Could not load suppliers:\n" + cause.getMessage()
                            + "\n\nRun database/dvms_db.sql and check Database.java.");
                }
            }
        }.execute();
    }

    // --- Direct Database Query Helpers ---

    public static List<Object[]> queryInventoryRows() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT item_id, item_name, category, quantity, price FROM inventory ORDER BY item_id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getString("item_id"), rs.getString("item_name"), rs.getString("category"),
                        rs.getInt("quantity"), rs.getBigDecimal("price")
                });
            }
        }
        return rows;
    }

    public static List<Object[]> querySupplierRows() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT supplier_id, supplier_name, supplier_type, phone, product FROM suppliers ORDER BY supplier_id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getString("supplier_id"), rs.getString("supplier_name"), rs.getString("supplier_type"),
                        rs.getString("phone"), rs.getString("product")
                });
            }
        }
        return rows;
    }

    private void clearInventoryFields() {
        txtItemId.setText(""); txtItemName.setText(""); comboCategory.setSelectedIndex(0);
        txtQuantity.setText(""); txtPrice.setText(""); inventoryTable.clearSelection();
    }

    private void clearSupplierFields() {
        txtSupplierId.setText(""); txtSupplierName.setText(""); comboSupplierType.setSelectedIndex(0);
        txtPhone.setText(""); txtProduct.setText(""); supplierTable.clearSelection();
    }

    private void handleException(Exception ex) {
        Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
        if (cause instanceof SQLIntegrityConstraintViolationException) {
            showError("That ID already exists. Please use a unique ID.");
        } else if (cause instanceof NumberFormatException) {
            showError("Quantity must be a whole number and price must be a valid number.");
        } else if (cause instanceof SQLException) {
            showError("Database operation failed:\n" + cause.getMessage()
                    + "\n\nCheck that MySQL is running and Database.java credentials are correct.");
        } else if (cause instanceof IllegalArgumentException) {
            showError(cause.getMessage());
        } else {
            showError("Unexpected error: " + cause.getMessage());
        }
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public DefaultTableModel getInventoryModel() {
        return inventoryModel;
    }

    public DefaultTableModel getSupplierModel() {
        return supplierModel;
    }

    private static class InventoryData {
        final String id, name, category;
        final int quantity;
        final BigDecimal price;
        InventoryData(String id, String name, String category, int quantity, BigDecimal price) {
            this.id = id; this.name = name; this.category = category; this.quantity = quantity; this.price = price;
        }
    }

    private static class SupplierData {
        final String id, name, type, phone, product;
        SupplierData(String id, String name, String type, String phone, String product) {
            this.id = id; this.name = name; this.type = type; this.phone = phone; this.product = product;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventoryAndSupplierSystem().setVisible(true));
    }
}
