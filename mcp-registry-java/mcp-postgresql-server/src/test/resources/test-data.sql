-- PostgreSQL 測試資料初始化腳本

-- 建立測試用戶表
CREATE TABLE IF NOT EXISTS test_users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255),
    age INTEGER CHECK (age >= 0 AND age <= 150),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 建立測試訂單表
CREATE TABLE IF NOT EXISTS test_orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES test_users(id) ON DELETE CASCADE,
    order_number VARCHAR(100) UNIQUE NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(50) DEFAULT 'pending',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 建立測試產品表
CREATE TABLE IF NOT EXISTS test_products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    category VARCHAR(100),
    stock_quantity INTEGER DEFAULT 0 CHECK (stock_quantity >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入測試用戶資料
INSERT INTO test_users (username, email, full_name, age) VALUES
('alice_test', 'alice.test@example.com', 'Alice Test User', 28),
('bob_test', 'bob.test@example.com', 'Bob Test User', 32),
('charlie_test', 'charlie.test@example.com', 'Charlie Test User', 25),
('diana_test', 'diana.test@example.com', 'Diana Test User', 35),
('eve_test', 'eve.test@example.com', 'Eve Test User', 29)
ON CONFLICT (username) DO NOTHING;

-- 插入測試訂單資料
INSERT INTO test_orders (user_id, order_number, amount, status) VALUES
(1, 'TEST-ORD-001', 99.99, 'completed'),
(1, 'TEST-ORD-002', 149.50, 'pending'),
(2, 'TEST-ORD-003', 75.25, 'completed'),
(3, 'TEST-ORD-004', 200.00, 'processing'),
(4, 'TEST-ORD-005', 50.75, 'completed'),
(5, 'TEST-ORD-006', 125.00, 'pending')
ON CONFLICT (order_number) DO NOTHING;

-- 插入測試產品資料
INSERT INTO test_products (name, description, price, category, stock_quantity) VALUES
('Test Laptop', 'High-performance test laptop', 999.99, 'Electronics', 10),
('Test Mouse', 'Wireless test mouse', 29.99, 'Electronics', 50),
('Test Book', 'Programming test book', 39.99, 'Books', 25),
('Test Chair', 'Ergonomic test chair', 199.99, 'Furniture', 8),
('Test Monitor', '4K test monitor', 399.99, 'Electronics', 15)
ON CONFLICT DO NOTHING;

-- 建立索引
CREATE INDEX IF NOT EXISTS idx_test_users_email ON test_users(email);
CREATE INDEX IF NOT EXISTS idx_test_users_age ON test_users(age);
CREATE INDEX IF NOT EXISTS idx_test_users_created_at ON test_users(created_at);
CREATE INDEX IF NOT EXISTS idx_test_orders_user_id ON test_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_test_orders_status ON test_orders(status);
CREATE INDEX IF NOT EXISTS idx_test_orders_order_date ON test_orders(order_date);
CREATE INDEX IF NOT EXISTS idx_test_products_category ON test_products(category);
CREATE INDEX IF NOT EXISTS idx_test_products_price ON test_products(price);

-- 建立測試視圖
CREATE OR REPLACE VIEW test_user_order_summary AS
SELECT
    u.id,
    u.username,
    u.email,
    u.full_name,
    COUNT(o.id) as total_orders,
    COALESCE(SUM(o.amount), 0) as total_spent,
    AVG(o.amount) as avg_order_value
FROM test_users u
LEFT JOIN test_orders o ON u.id = o.user_id
GROUP BY u.id, u.username, u.email, u.full_name;

-- 建立測試函數
CREATE OR REPLACE FUNCTION get_user_order_count(user_id_param INTEGER)
RETURNS INTEGER AS $$
BEGIN
    RETURN (SELECT COUNT(*) FROM test_orders WHERE user_id = user_id_param);
END;
$$ LANGUAGE plpgsql;

-- 建立測試觸發器函數
CREATE OR REPLACE FUNCTION update_user_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 建立測試觸發器
DROP TRIGGER IF EXISTS trigger_update_user_updated_at ON test_users;
CREATE TRIGGER trigger_update_user_updated_at
    BEFORE UPDATE ON test_users
    FOR EACH ROW
    EXECUTE FUNCTION update_user_updated_at();

-- 更新統計資訊
ANALYZE test_users;
ANALYZE test_orders;
ANALYZE test_products;