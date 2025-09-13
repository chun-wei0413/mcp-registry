-- Development Database Initialization Script
-- This script includes additional test data and development-friendly settings

-- Include all production setup
\i /docker-entrypoint-initdb.d/init-db.sql

-- Additional development tables for testing
CREATE TABLE IF NOT EXISTS test_data_types (
    id SERIAL PRIMARY KEY,
    text_field TEXT,
    varchar_field VARCHAR(255),
    integer_field INTEGER,
    bigint_field BIGINT,
    decimal_field DECIMAL(10,2),
    boolean_field BOOLEAN,
    date_field DATE,
    timestamp_field TIMESTAMP,
    timestamptz_field TIMESTAMP WITH TIME ZONE,
    json_field JSON,
    jsonb_field JSONB,
    uuid_field UUID DEFAULT uuid_generate_v4(),
    array_field INTEGER[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS performance_test (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    value DECIMAL(15,6) NOT NULL,
    metadata JSONB,
    tags TEXT[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert test data for data types
INSERT INTO test_data_types (
    text_field, varchar_field, integer_field, bigint_field, decimal_field,
    boolean_field, date_field, timestamp_field, timestamptz_field,
    json_field, jsonb_field, array_field
) VALUES
    ('Sample text data', 'Varchar sample', 42, 9223372036854775807, 123.45,
     true, '2024-01-15', '2024-01-15 10:30:00', '2024-01-15 10:30:00+00',
     '{"key": "value", "number": 123}', '{"key": "value", "number": 123, "nested": {"key": "nested_value"}}',
     ARRAY[1, 2, 3, 4, 5]),
    ('Another text sample', 'Another varchar', -100, 1000000000000, 999.99,
     false, '2024-02-20', '2024-02-20 15:45:30', '2024-02-20 15:45:30+00',
     '{"array": [1, 2, 3], "object": {"nested": true}}', '{"status": "active", "config": {"debug": true}}',
     ARRAY[10, 20, 30]),
    (NULL, NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, NULL,
     NULL, NULL, NULL);

-- Insert performance test data
INSERT INTO performance_test (category, value, metadata, tags)
SELECT
    CASE (i % 5)
        WHEN 0 THEN 'cpu_usage'
        WHEN 1 THEN 'memory_usage'
        WHEN 2 THEN 'disk_io'
        WHEN 3 THEN 'network_io'
        ELSE 'response_time'
    END,
    RANDOM() * 100,
    jsonb_build_object(
        'server_id', 'srv-' || LPAD((i % 10 + 1)::TEXT, 3, '0'),
        'region', CASE (i % 3) WHEN 0 THEN 'us-east' WHEN 1 THEN 'eu-west' ELSE 'asia-pacific' END,
        'timestamp', NOW() - (i || ' minutes')::INTERVAL
    ),
    ARRAY['monitoring', CASE WHEN i % 2 = 0 THEN 'production' ELSE 'staging' END]
FROM generate_series(1, 1000) AS i;

-- Insert more sample users for development
INSERT INTO users (username, email, full_name, is_active)
SELECT
    'testuser' || i,
    'testuser' || i || '@example.com',
    'Test User ' || i,
    RANDOM() < 0.8  -- 80% active users
FROM generate_series(1, 50) AS i
ON CONFLICT (username) DO NOTHING;

-- Insert more sample orders for testing pagination and performance
INSERT INTO orders (user_id, order_number, status, total_amount)
SELECT
    u.id,
    'DEV-' || LPAD(i::TEXT, 8, '0'),
    CASE (i % 4)
        WHEN 0 THEN 'pending'
        WHEN 1 THEN 'processing'
        WHEN 2 THEN 'shipped'
        ELSE 'delivered'
    END,
    (10 + RANDOM() * 500)::DECIMAL(10,2)
FROM users u
CROSS JOIN generate_series(1, 20) AS i
WHERE u.username LIKE 'testuser%'
ON CONFLICT (order_number) DO NOTHING;

-- Create additional indexes for development testing
CREATE INDEX IF NOT EXISTS idx_test_data_types_category ON test_data_types(varchar_field);
CREATE INDEX IF NOT EXISTS idx_test_data_types_created_at ON test_data_types(created_at);
CREATE INDEX IF NOT EXISTS idx_performance_test_category ON performance_test(category);
CREATE INDEX IF NOT EXISTS idx_performance_test_value ON performance_test(value);
CREATE INDEX IF NOT EXISTS idx_performance_test_created_at ON performance_test(created_at);
CREATE INDEX IF NOT EXISTS idx_performance_test_metadata ON performance_test USING GIN (metadata);
CREATE INDEX IF NOT EXISTS idx_performance_test_tags ON performance_test USING GIN (tags);

-- Create a view for development statistics
CREATE OR REPLACE VIEW dev_table_stats AS
SELECT
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation,
    most_common_vals,
    most_common_freqs
FROM pg_stats
WHERE schemaname = 'public'
ORDER BY schemaname, tablename, attname;

-- Create a function for generating test data
CREATE OR REPLACE FUNCTION generate_test_orders(user_count INTEGER DEFAULT 10, orders_per_user INTEGER DEFAULT 5)
RETURNS INTEGER AS $$
DECLARE
    inserted_count INTEGER := 0;
    user_record RECORD;
    i INTEGER;
BEGIN
    FOR user_record IN
        SELECT id FROM users WHERE username LIKE 'testuser%' LIMIT user_count
    LOOP
        FOR i IN 1..orders_per_user LOOP
            INSERT INTO orders (user_id, order_number, status, total_amount)
            VALUES (
                user_record.id,
                'GEN-' || EXTRACT(EPOCH FROM NOW())::BIGINT || '-' || i,
                CASE (RANDOM() * 4)::INTEGER
                    WHEN 0 THEN 'pending'
                    WHEN 1 THEN 'processing'
                    WHEN 2 THEN 'shipped'
                    ELSE 'delivered'
                END,
                (20 + RANDOM() * 200)::DECIMAL(10,2)
            );
            inserted_count := inserted_count + 1;
        END LOOP;
    END LOOP;

    RETURN inserted_count;
END;
$$ LANGUAGE plpgsql;

-- Grant permissions for development
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO mcp_dev_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO mcp_dev_user;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO mcp_dev_user;

-- Enable query logging for development
ALTER DATABASE mcp_dev SET log_statement = 'all';
ALTER DATABASE mcp_dev SET log_min_duration_statement = 0;

-- Create development-specific configuration
CREATE TABLE IF NOT EXISTS dev_config (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

INSERT INTO dev_config (key, value, description) VALUES
    ('debug_mode', 'true', 'Enable debug mode for development'),
    ('query_logging', 'true', 'Enable comprehensive query logging'),
    ('performance_monitoring', 'true', 'Enable performance monitoring features'),
    ('test_data_refresh_interval', '1h', 'Interval for refreshing test data')
ON CONFLICT (key) DO NOTHING;

-- Notify that development setup is complete
DO $$
BEGIN
    RAISE NOTICE 'Development database setup completed successfully!';
    RAISE NOTICE 'Total users: %', (SELECT COUNT(*) FROM users);
    RAISE NOTICE 'Total orders: %', (SELECT COUNT(*) FROM orders);
    RAISE NOTICE 'Total products: %', (SELECT COUNT(*) FROM products);
    RAISE NOTICE 'Performance test records: %', (SELECT COUNT(*) FROM performance_test);
END $$;