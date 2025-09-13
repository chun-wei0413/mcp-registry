-- Test Database Initialization Script
-- Minimal setup for automated testing

-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create basic test tables
CREATE TABLE IF NOT EXISTS test_users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS test_orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES test_users(id),
    total DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS test_logs (
    id BIGSERIAL PRIMARY KEY,
    level VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Create a table for testing different data types
CREATE TABLE IF NOT EXISTS test_data_types (
    id SERIAL PRIMARY KEY,
    text_col TEXT,
    int_col INTEGER,
    bool_col BOOLEAN,
    json_col JSONB,
    array_col INTEGER[],
    uuid_col UUID DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Insert minimal test data
INSERT INTO test_users (name, email, active) VALUES
    ('Test User 1', 'test1@example.com', true),
    ('Test User 2', 'test2@example.com', true),
    ('Inactive User', 'inactive@example.com', false);

INSERT INTO test_orders (user_id, total, status) VALUES
    (1, 100.50, 'completed'),
    (1, 75.25, 'pending'),
    (2, 200.00, 'shipped'),
    (2, 50.00, 'cancelled');

INSERT INTO test_data_types (text_col, int_col, bool_col, json_col, array_col) VALUES
    ('Sample text', 42, true, '{"key": "value"}', ARRAY[1, 2, 3]),
    ('Another sample', -10, false, '{"test": true, "count": 5}', ARRAY[10, 20]),
    (NULL, NULL, NULL, NULL, NULL);

-- Create indexes for test performance
CREATE INDEX idx_test_users_email ON test_users(email);
CREATE INDEX idx_test_orders_user_id ON test_orders(user_id);
CREATE INDEX idx_test_orders_status ON test_orders(status);
CREATE INDEX idx_test_logs_level ON test_logs(level);
CREATE INDEX idx_test_logs_created_at ON test_logs(created_at);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO mcp_test_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO mcp_test_user;

-- Create a simple view for testing
CREATE VIEW test_user_orders AS
SELECT
    u.id as user_id,
    u.name as user_name,
    u.email,
    COUNT(o.id) as order_count,
    COALESCE(SUM(o.total), 0) as total_spent
FROM test_users u
LEFT JOIN test_orders o ON u.id = o.user_id
WHERE u.active = true
GROUP BY u.id, u.name, u.email;

-- Create test functions
CREATE OR REPLACE FUNCTION test_function(input_text TEXT)
RETURNS TEXT AS $$
BEGIN
    RETURN 'Processed: ' || COALESCE(input_text, 'NULL');
END;
$$ LANGUAGE plpgsql;

-- Test trigger function
CREATE OR REPLACE FUNCTION test_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    NEW.created_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for testing
CREATE TRIGGER test_users_trigger
    BEFORE INSERT ON test_users
    FOR EACH ROW EXECUTE FUNCTION test_trigger_function();

-- Notify completion
DO $$
BEGIN
    RAISE NOTICE 'Test database setup completed!';
    RAISE NOTICE 'Test users: %', (SELECT COUNT(*) FROM test_users);
    RAISE NOTICE 'Test orders: %', (SELECT COUNT(*) FROM test_orders);
END $$;