-- PostgreSQL MCP Server Database Initialization Script
-- This script sets up the basic database structure for production

-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create sample tables for testing MCP server functionality
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bio TEXT,
    avatar_url VARCHAR(512),
    location VARCHAR(255),
    website VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    shipped_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    category VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    stock_quantity INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    table_name VARCHAR(100),
    record_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_table_name ON audit_logs(table_name);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);

-- Insert sample data
INSERT INTO users (username, email, full_name, is_active) VALUES
    ('admin', 'admin@example.com', 'System Administrator', true),
    ('alice', 'alice@example.com', 'Alice Johnson', true),
    ('bob', 'bob@example.com', 'Bob Smith', true),
    ('charlie', 'charlie@example.com', 'Charlie Brown', false)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_profiles (user_id, bio, location)
SELECT
    u.id,
    'Sample user profile for ' || u.full_name,
    CASE
        WHEN u.username = 'admin' THEN 'System'
        WHEN u.username = 'alice' THEN 'New York, USA'
        WHEN u.username = 'bob' THEN 'London, UK'
        WHEN u.username = 'charlie' THEN 'Tokyo, Japan'
    END
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id);

INSERT INTO products (name, description, price, category, stock_quantity) VALUES
    ('Laptop Pro', 'High-performance laptop for professionals', 1299.99, 'Electronics', 50),
    ('Wireless Mouse', 'Ergonomic wireless mouse', 29.99, 'Electronics', 200),
    ('Office Chair', 'Comfortable ergonomic office chair', 299.99, 'Furniture', 25),
    ('Coffee Mug', 'Ceramic coffee mug with logo', 12.99, 'Accessories', 100),
    ('Notebook', 'Premium leather-bound notebook', 24.99, 'Accessories', 75)
ON CONFLICT DO NOTHING;

-- Insert sample orders
INSERT INTO orders (user_id, order_number, status, total_amount)
SELECT
    u.id,
    'ORD-' || LPAD((ROW_NUMBER() OVER())::TEXT, 6, '0'),
    CASE
        WHEN RANDOM() < 0.3 THEN 'pending'
        WHEN RANDOM() < 0.6 THEN 'shipped'
        ELSE 'delivered'
    END,
    (50 + RANDOM() * 1000)::DECIMAL(10,2)
FROM users u
WHERE u.username != 'admin'
ON CONFLICT (order_number) DO NOTHING;

-- Insert sample order items
INSERT INTO order_items (order_id, product_name, quantity, unit_price, total_price)
SELECT
    o.id,
    p.name,
    (1 + RANDOM() * 3)::INTEGER,
    p.price,
    ((1 + RANDOM() * 3)::INTEGER * p.price)::DECIMAL(10,2)
FROM orders o
CROSS JOIN products p
WHERE RANDOM() < 0.3  -- Only some combinations
ON CONFLICT DO NOTHING;

-- Create a function to update the updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers to automatically update the updated_at column
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_profiles_updated_at BEFORE UPDATE ON user_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create a view for user statistics
CREATE OR REPLACE VIEW user_stats AS
SELECT
    u.id,
    u.username,
    u.email,
    u.full_name,
    u.is_active,
    u.created_at,
    u.last_login,
    COUNT(o.id) as total_orders,
    COALESCE(SUM(o.total_amount), 0) as total_spent,
    MAX(o.created_at) as last_order_date
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.username, u.email, u.full_name, u.is_active, u.created_at, u.last_login;

-- Grant appropriate permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO mcp_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO mcp_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO mcp_user;