"""
Pytest configuration and fixtures for PostgreSQL MCP Server tests
"""

import pytest
import asyncio
import os
from typing import Dict, Any, AsyncGenerator
from unittest.mock import Mock, AsyncMock
import asyncpg

from src.server import PostgreSQLMCPServer
from src.tools.connection import ConnectionManager
from src.models.types import SecurityConfig, ServerConfig


@pytest.fixture(scope="session")
def event_loop():
    """Create an instance of the default event loop for the test session."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture
def mock_security_config():
    """Create a mock security configuration for testing."""
    return SecurityConfig(
        readonly_mode=False,
        allowed_operations=["SELECT", "INSERT", "UPDATE", "DELETE"],
        blocked_keywords=["DROP", "TRUNCATE", "ALTER"],
        max_query_length=10000,
        enable_query_logging=True
    )


@pytest.fixture
def mock_server_config():
    """Create a mock server configuration for testing."""
    return ServerConfig(
        port=3000,
        log_level="INFO",
        default_pool_size=5,
        query_timeout=30,
        max_connections=10,
        enable_query_cache=False,
        cache_ttl_seconds=300
    )


@pytest.fixture
async def mock_connection_manager(mock_security_config):
    """Create a mock connection manager."""
    manager = ConnectionManager(mock_security_config)

    # Mock the connection pool
    mock_pool = AsyncMock()
    mock_pool.acquire.return_value.__aenter__.return_value = Mock()
    mock_pool.acquire.return_value.__aexit__.return_value = None
    mock_pool.get_pool_stats.return_value = {
        "size": 5,
        "min_size": 2,
        "max_size": 10,
        "idle_size": 3,
        "is_closed": False
    }

    manager.pools["test_connection"] = mock_pool

    yield manager

    # Cleanup
    await manager.close_all_connections()


@pytest.fixture
def sample_query_result():
    """Sample query result for testing."""
    return {
        "success": True,
        "rows": [
            {"id": 1, "name": "test1", "value": 100},
            {"id": 2, "name": "test2", "value": 200}
        ],
        "row_count": 2,
        "columns": ["id", "name", "value"],
        "duration_ms": 50
    }


@pytest.fixture
def sample_table_schema():
    """Sample table schema for testing."""
    return {
        "success": True,
        "table_name": "test_table",
        "schema": "public",
        "columns": [
            {
                "name": "id",
                "data_type": "integer",
                "is_nullable": False,
                "default_value": None,
                "ordinal_position": 1
            },
            {
                "name": "name",
                "data_type": "character varying",
                "max_length": 255,
                "is_nullable": True,
                "default_value": None,
                "ordinal_position": 2
            }
        ],
        "primary_keys": ["id"],
        "indexes": [
            {
                "name": "test_table_pkey",
                "columns": ["id"],
                "is_unique": True,
                "is_primary": True
            }
        ],
        "foreign_keys": []
    }


@pytest.fixture
def dangerous_queries():
    """List of dangerous queries for security testing."""
    return [
        "DROP TABLE users",
        "TRUNCATE TABLE orders",
        "ALTER TABLE products ADD COLUMN secret TEXT",
        "DELETE FROM users",  # without WHERE
        "UPDATE users SET password = 'hacked'",  # without WHERE
        "SELECT pg_read_file('/etc/passwd')",
        "COPY users TO PROGRAM 'cat > /tmp/dump.sql'",
        "GRANT ALL PRIVILEGES ON ALL TABLES TO hacker",
        "CREATE USER evil_user WITH PASSWORD 'evil'"
    ]


@pytest.fixture
def safe_queries():
    """List of safe queries for testing."""
    return [
        "SELECT * FROM users WHERE id = $1",
        "SELECT COUNT(*) FROM orders",
        "WITH ranked_products AS (SELECT *, ROW_NUMBER() OVER (ORDER BY price) FROM products) SELECT * FROM ranked_products",
        "INSERT INTO logs (message, created_at) VALUES ($1, NOW())",
        "UPDATE users SET last_login = NOW() WHERE id = $1",
        "DELETE FROM temp_data WHERE created_at < NOW() - INTERVAL '1 day'",
        "EXPLAIN SELECT * FROM users WHERE email = $1"
    ]


class MockAsyncConnection:
    """Mock asyncpg connection for testing."""

    def __init__(self):
        self.closed = False
        self.transaction_active = False

    async def execute(self, query: str, *args):
        """Mock execute method."""
        if query.upper().startswith("INSERT"):
            return "INSERT 0 1"
        elif query.upper().startswith("UPDATE"):
            return "UPDATE 1"
        elif query.upper().startswith("DELETE"):
            return "DELETE 1"
        else:
            return "OK"

    async def fetch(self, query: str, *args):
        """Mock fetch method."""
        if "users" in query.lower():
            return [
                {"id": 1, "name": "user1", "email": "user1@example.com"},
                {"id": 2, "name": "user2", "email": "user2@example.com"}
            ]
        elif "orders" in query.lower():
            return [
                {"id": 1, "user_id": 1, "total": 100.0},
                {"id": 2, "user_id": 2, "total": 200.0}
            ]
        else:
            return []

    async def fetchval(self, query: str, *args):
        """Mock fetchval method."""
        if "count" in query.lower():
            return 2
        elif "explain" in query.lower():
            return [{"Plan": {"Node Type": "Seq Scan", "Total Cost": 1.0}}]
        else:
            return "test_value"

    async def fetchrow(self, query: str, *args):
        """Mock fetchrow method."""
        return {"id": 1, "name": "test", "value": 100}

    async def executemany(self, query: str, args_list):
        """Mock executemany method."""
        return f"EXECUTED {len(args_list)} queries"

    async def close(self):
        """Mock close method."""
        self.closed = True

    def transaction(self):
        """Mock transaction context manager."""
        return MockTransaction(self)


class MockTransaction:
    """Mock transaction context manager."""

    def __init__(self, connection):
        self.connection = connection

    async def __aenter__(self):
        self.connection.transaction_active = True
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        self.connection.transaction_active = False
        # Simulate rollback on exception
        if exc_type is not None:
            return False


@pytest.fixture
def mock_asyncpg_connection():
    """Create a mock asyncpg connection."""
    return MockAsyncConnection()


@pytest.fixture
async def integration_db_config():
    """Database configuration for integration tests."""
    # Check if we have a test database configured
    test_db_url = os.getenv("TEST_DATABASE_URL")
    if not test_db_url:
        pytest.skip("TEST_DATABASE_URL not configured")

    return {
        "host": os.getenv("TEST_DB_HOST", "localhost"),
        "port": int(os.getenv("TEST_DB_PORT", "5432")),
        "database": os.getenv("TEST_DB_NAME", "test_mcp"),
        "user": os.getenv("TEST_DB_USER", "test_user"),
        "password": os.getenv("TEST_DB_PASSWORD", "test_password")
    }


@pytest.fixture
async def setup_test_database(integration_db_config):
    """Setup test database with sample tables."""
    conn = await asyncpg.connect(**integration_db_config)

    try:
        # Create test tables
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS test_users (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) UNIQUE,
                created_at TIMESTAMP DEFAULT NOW()
            )
        """)

        await conn.execute("""
            CREATE TABLE IF NOT EXISTS test_orders (
                id SERIAL PRIMARY KEY,
                user_id INTEGER REFERENCES test_users(id),
                total DECIMAL(10,2) NOT NULL,
                status VARCHAR(50) DEFAULT 'pending',
                created_at TIMESTAMP DEFAULT NOW()
            )
        """)

        # Insert sample data
        await conn.execute("""
            INSERT INTO test_users (name, email) VALUES
            ('Alice', 'alice@example.com'),
            ('Bob', 'bob@example.com')
            ON CONFLICT (email) DO NOTHING
        """)

        await conn.execute("""
            INSERT INTO test_orders (user_id, total, status) VALUES
            (1, 100.50, 'completed'),
            (2, 75.25, 'pending')
        """)

        yield integration_db_config

    finally:
        # Cleanup
        try:
            await conn.execute("DROP TABLE IF EXISTS test_orders")
            await conn.execute("DROP TABLE IF EXISTS test_users")
        except:
            pass
        await conn.close()


@pytest.fixture
def mock_health_status():
    """Mock health status for testing."""
    return {
        "is_healthy": True,
        "status": "healthy",
        "checks": {
            "connections": {
                "is_healthy": True,
                "status": "healthy",
                "total_connections": 1,
                "healthy_connections": 1,
                "unhealthy_connections": 0
            },
            "memory": {
                "is_healthy": True,
                "warning": False,
                "memory_percent": 45.2
            },
            "system": {
                "is_healthy": True,
                "warning": False,
                "cpu_percent": 15.5,
                "disk_percent": 30.1
            }
        }
    }