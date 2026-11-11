-- DVMS Inventory and Supplier Management System
-- Run this script in MySQL Workbench.
CREATE DATABASE IF NOT EXISTS dvms_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE dvms_db;

CREATE TABLE IF NOT EXISTS inventory (
    item_id VARCHAR(50) PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_inventory_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_price CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id VARCHAR(50) PRIMARY KEY,
    supplier_name VARCHAR(100) NOT NULL,
    supplier_type VARCHAR(50) NOT NULL,
    phone VARCHAR(30),
    product VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Optional sample rows (uncomment to insert examples):
-- INSERT INTO inventory (item_id, item_name, category, quantity, price)
-- VALUES ('ITM001', 'USB Keyboard', 'Electronics', 10, 15.99);
-- INSERT INTO suppliers (supplier_id, supplier_name, supplier_type, phone, product)
-- VALUES ('SUP001', 'ABC Traders', 'Wholesaler', '9876543210', 'USB Keyboard');
