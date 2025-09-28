-- MySQL 測試資料初始化腳本

-- 建立測試用戶表
CREATE TABLE IF NOT EXISTS test_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255),
    age INT CHECK (age >= 0 AND age <= 150),
    is_active BOOLEAN DEFAULT true,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_age (age),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 建立測試訂單表
CREATE TABLE IF NOT EXISTS test_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    order_number VARCHAR(100) UNIQUE NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    status ENUM('pending', 'processing', 'completed', 'cancelled') DEFAULT 'pending',
    order_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES test_users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_order_number (order_number),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 建立測試產品表
CREATE TABLE IF NOT EXISTS test_products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    category VARCHAR(100),
    stock_quantity INT DEFAULT 0 CHECK (stock_quantity >= 0),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_price (price),
    INDEX idx_stock_quantity (stock_quantity),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 建立測試訂單項目表
CREATE TABLE IF NOT EXISTS test_order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    total_price DECIMAL(10,2) NOT NULL CHECK (total_price >= 0),
    FOREIGN KEY (order_id) REFERENCES test_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES test_products(id) ON DELETE RESTRICT,
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入測試用戶資料
INSERT IGNORE INTO test_users (username, email, full_name, age) VALUES
('alice_mysql', 'alice.mysql@example.com', 'Alice MySQL User', 28),
('bob_mysql', 'bob.mysql@example.com', 'Bob MySQL User', 32),
('charlie_mysql', 'charlie.mysql@example.com', 'Charlie MySQL User', 25),
('diana_mysql', 'diana.mysql@example.com', 'Diana MySQL User', 35),
('eve_mysql', 'eve.mysql@example.com', 'Eve MySQL User', 29),
('frank_mysql', 'frank.mysql@example.com', 'Frank MySQL User', 41),
('grace_mysql', 'grace.mysql@example.com', 'Grace MySQL User', 27);

-- 插入測試產品資料
INSERT IGNORE INTO test_products (name, description, price, category, stock_quantity) VALUES
('MySQL Laptop', 'High-performance MySQL laptop', 1299.99, 'Electronics', 15),
('MySQL Mouse', 'Wireless MySQL mouse', 39.99, 'Electronics', 75),
('MySQL Book', 'Advanced MySQL programming book', 49.99, 'Books', 30),
('MySQL Chair', 'Ergonomic MySQL chair', 249.99, 'Furniture', 12),
('MySQL Monitor', '4K MySQL monitor', 499.99, 'Electronics', 20),
('MySQL Keyboard', 'Mechanical MySQL keyboard', 89.99, 'Electronics', 40),
('MySQL Desk', 'Standing MySQL desk', 399.99, 'Furniture', 8),
('MySQL Headphones', 'Noise-cancelling MySQL headphones', 199.99, 'Electronics', 25);

-- 插入測試訂單資料
INSERT IGNORE INTO test_orders (user_id, order_number, amount, status) VALUES
(1, 'MYSQL-ORD-001', 1299.99, 'completed'),
(1, 'MYSQL-ORD-002', 149.98, 'pending'),
(2, 'MYSQL-ORD-003', 549.98, 'completed'),
(3, 'MYSQL-ORD-004', 249.99, 'processing'),
(4, 'MYSQL-ORD-005', 89.99, 'completed'),
(5, 'MYSQL-ORD-006', 199.99, 'pending'),
(6, 'MYSQL-ORD-007', 749.98, 'completed'),
(7, 'MYSQL-ORD-008', 449.99, 'processing');

-- 插入測試訂單項目資料
INSERT IGNORE INTO test_order_items (order_id, product_id, quantity, unit_price, total_price) VALUES
-- Order 1: MySQL Laptop
(1, 1, 1, 1299.99, 1299.99),
-- Order 2: Mouse + Keyboard
(2, 2, 1, 39.99, 39.99),
(2, 6, 1, 89.99, 89.99),
(2, 3, 1, 49.99, 49.99),
-- Order 3: Monitor + Headphones
(3, 5, 1, 499.99, 499.99),
(3, 8, 1, 199.99, 199.99),
-- Order 4: Chair
(4, 4, 1, 249.99, 249.99),
-- Order 5: Keyboard
(5, 6, 1, 89.99, 89.99),
-- Order 6: Headphones
(6, 8, 1, 199.99, 199.99),
-- Order 7: Desk + Chair
(7, 7, 1, 399.99, 399.99),
(7, 4, 1, 249.99, 249.99),
(8, 3, 2, 49.99, 99.98),
-- Order 8: Books + Monitor
(8, 5, 1, 499.99, 499.99);

-- 建立測試視圖
CREATE OR REPLACE VIEW test_user_order_summary AS
SELECT
    u.id,
    u.username,
    u.email,
    u.full_name,
    COUNT(DISTINCT o.id) as total_orders,
    COALESCE(SUM(o.amount), 0) as total_spent,
    COALESCE(AVG(o.amount), 0) as avg_order_value,
    COUNT(DISTINCT oi.product_id) as unique_products_purchased
FROM test_users u
LEFT JOIN test_orders o ON u.id = o.user_id
LEFT JOIN test_order_items oi ON o.id = oi.order_id
GROUP BY u.id, u.username, u.email, u.full_name;

-- 建立產品銷售統計視圖
CREATE OR REPLACE VIEW test_product_sales_summary AS
SELECT
    p.id,
    p.name,
    p.category,
    p.price,
    p.stock_quantity,
    COUNT(DISTINCT oi.order_id) as times_ordered,
    COALESCE(SUM(oi.quantity), 0) as total_quantity_sold,
    COALESCE(SUM(oi.total_price), 0) as total_revenue
FROM test_products p
LEFT JOIN test_order_items oi ON p.id = oi.product_id
GROUP BY p.id, p.name, p.category, p.price, p.stock_quantity;

-- 建立測試預存程序
DELIMITER //

CREATE PROCEDURE GetUserOrderStats(IN user_id_param INT)
BEGIN
    SELECT
        u.username,
        u.email,
        COUNT(o.id) as order_count,
        COALESCE(SUM(o.amount), 0) as total_spent,
        COALESCE(AVG(o.amount), 0) as avg_order_value,
        MIN(o.order_date) as first_order_date,
        MAX(o.order_date) as last_order_date
    FROM test_users u
    LEFT JOIN test_orders o ON u.id = o.user_id
    WHERE u.id = user_id_param
    GROUP BY u.id, u.username, u.email;
END //

CREATE PROCEDURE GetTopProducts(IN limit_param INT)
BEGIN
    SELECT
        p.id,
        p.name,
        p.category,
        p.price,
        COUNT(DISTINCT oi.order_id) as times_ordered,
        SUM(oi.quantity) as total_sold,
        SUM(oi.total_price) as total_revenue
    FROM test_products p
    INNER JOIN test_order_items oi ON p.id = oi.product_id
    GROUP BY p.id, p.name, p.category, p.price
    ORDER BY total_revenue DESC
    LIMIT limit_param;
END //

DELIMITER ;

-- 建立測試函數
DELIMITER //

CREATE FUNCTION CalculateOrderTotal(order_id_param INT)
RETURNS DECIMAL(10,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE total_amount DECIMAL(10,2) DEFAULT 0;

    SELECT COALESCE(SUM(total_price), 0) INTO total_amount
    FROM test_order_items
    WHERE order_id = order_id_param;

    RETURN total_amount;
END //

DELIMITER ;

-- 更新統計資訊
ANALYZE TABLE test_users;
ANALYZE TABLE test_orders;
ANALYZE TABLE test_products;
ANALYZE TABLE test_order_items;