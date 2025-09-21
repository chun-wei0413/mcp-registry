"""
Unit tests for query execution functionality
"""

import pytest
from unittest.mock import AsyncMock, Mock, patch
from src.postgresql_mcp.infrastructure.database.query_executor import PostgreSQLQueryExecutor


class TestQueryTool:
    """Test query execution functionality."""

    @pytest.fixture
    def security_config(self):
        """Create security config for testing."""
        return SecurityConfig(
            readonly_mode=False,
            allowed_operations=["SELECT", "INSERT", "UPDATE", "DELETE"],
            blocked_keywords=["DROP", "TRUNCATE"],
            max_query_length=1000
        )

    @pytest.fixture
    def mock_connection_manager(self):
        """Mock connection manager."""
        manager = AsyncMock()
        mock_pool = AsyncMock()
        manager.get_pool.return_value = mock_pool
        return manager, mock_pool

    @pytest.fixture
    def query_tool(self, mock_connection_manager, security_config):
        """Create query tool with mocked dependencies."""
        connection_manager, _ = mock_connection_manager
        return QueryTool(connection_manager, security_config)

    @pytest.mark.asyncio
    async def test_execute_valid_select_query(self, query_tool, mock_connection_manager):
        """Test executing a valid SELECT query."""
        connection_manager, mock_pool = mock_connection_manager

        # Mock connection
        mock_conn = AsyncMock()
        mock_conn.fetch.return_value = [
            {"id": 1, "name": "test1"},
            {"id": 2, "name": "test2"}
        ]
        mock_pool.acquire.return_value.__aenter__.return_value = mock_conn

        result = await query_tool.execute_query(
            connection_id="test_conn",
            query="SELECT * FROM users WHERE id = $1",
            params=[1]
        )

        assert result.success
        assert len(result.rows) == 2
        assert result.rows[0]["id"] == 1
        assert result.row_count == 2
        assert result.columns == ["id", "name"]
        assert result.duration_ms >= 0

    @pytest.mark.asyncio
    async def test_execute_query_with_security_violation(self, query_tool, mock_connection_manager):
        """Test executing a query that violates security rules."""
        result = await query_tool.execute_query(
            connection_id="test_conn",
            query="DROP TABLE users"
        )

        assert not result.success
        assert "DROP" in result.error
        assert result.duration_ms == 0

    @pytest.mark.asyncio
    async def test_execute_query_connection_not_found(self, query_tool, mock_connection_manager):
        """Test executing query when connection is not found."""
        connection_manager, _ = mock_connection_manager
        connection_manager.get_pool.return_value = None

        result = await query_tool.execute_query(
            connection_id="nonexistent_conn",
            query="SELECT * FROM users"
        )

        assert not result.success
        assert "Connection not found" in result.error

    @pytest.mark.asyncio
    async def test_execute_query_non_select_rejected(self, query_tool, mock_connection_manager):
        """Test that non-SELECT queries are rejected in execute_query."""
        result = await query_tool.execute_query(
            connection_id="test_conn",
            query="INSERT INTO users (name) VALUES ('test')"
        )

        assert not result.success
        assert "only select" in result.error.lower()

    @pytest.mark.asyncio
    async def test_execute_transaction_valid(self, query_tool, mock_connection_manager):
        """Test executing a valid transaction."""
        connection_manager, mock_pool = mock_connection_manager

        # Mock connection and transaction
        mock_conn = AsyncMock()
        mock_transaction = AsyncMock()
        mock_conn.transaction.return_value = mock_transaction
        mock_conn.execute.return_value = "INSERT 0 1"
        mock_conn.fetch.return_value = [{"id": 1, "name": "test"}]
        mock_pool.acquire.return_value.__aenter__.return_value = mock_conn

        queries = [
            {"query": "INSERT INTO users (name) VALUES ($1)", "params": ["test"]},
            {"query": "SELECT * FROM users WHERE name = $1", "params": ["test"]}
        ]

        result = await query_tool.execute_transaction(
            connection_id="test_conn",
            queries=queries
        )

        assert result.success
        assert len(result.results) == 2
        assert not result.rolled_back
        assert result.duration_ms >= 0

    @pytest.mark.asyncio
    async def test_execute_transaction_with_security_violation(self, query_tool):
        """Test transaction with security violation."""
        queries = [
            {"query": "INSERT INTO users (name) VALUES ($1)", "params": ["test"]},
            {"query": "DROP TABLE users", "params": []}
        ]

        result = await query_tool.execute_transaction(
            connection_id="test_conn",
            queries=queries
        )

        assert not result.success
        assert "security validation failed" in result.error
        assert not result.rolled_back  # Didn't even start

    @pytest.mark.asyncio
    async def test_execute_transaction_with_query_failure(self, query_tool, mock_connection_manager):
        """Test transaction with query failure and rollback."""
        connection_manager, mock_pool = mock_connection_manager

        # Mock connection that fails on second query
        mock_conn = AsyncMock()
        mock_transaction = AsyncMock()
        mock_conn.transaction.return_value = mock_transaction
        mock_conn.execute.side_effect = [
            "INSERT 0 1",  # First query succeeds
            Exception("Query failed")  # Second query fails
        ]
        mock_pool.acquire.return_value.__aenter__.return_value = mock_conn

        queries = [
            {"query": "INSERT INTO users (name) VALUES ($1)", "params": ["test1"]},
            {"query": "INSERT INTO users (name) VALUES ($1)", "params": ["test2"]}
        ]

        result = await query_tool.execute_transaction(
            connection_id="test_conn",
            queries=queries
        )

        assert not result.success
        assert result.rolled_back
        assert "Query failed" in result.error

    @pytest.mark.asyncio
    async def test_batch_execute_valid(self, query_tool, mock_connection_manager):
        """Test batch execution of valid query."""
        connection_manager, mock_pool = mock_connection_manager

        mock_conn = AsyncMock()
        mock_conn.executemany.return_value = None
        mock_pool.acquire.return_value.__aenter__.return_value = mock_conn

        result = await query_tool.batch_execute(
            connection_id="test_conn",
            query="INSERT INTO users (name, email) VALUES ($1, $2)",
            params_list=[
                ["user1", "user1@example.com"],
                ["user2", "user2@example.com"],
                ["user3", "user3@example.com"]
            ]
        )

        assert result.success
        assert result.batch_size == 3
        assert result.total_affected_rows == 3
        assert result.duration_ms >= 0

    @pytest.mark.asyncio
    async def test_batch_execute_with_security_violation(self, query_tool):
        """Test batch execution with security violation."""
        result = await query_tool.batch_execute(
            connection_id="test_conn",
            query="DROP TABLE users",
            params_list=[[]]
        )

        assert not result.success
        assert "DROP" in result.error
        assert result.batch_size == 1

    @pytest.mark.asyncio
    async def test_batch_execute_connection_not_found(self, query_tool, mock_connection_manager):
        """Test batch execution when connection not found."""
        connection_manager, _ = mock_connection_manager
        connection_manager.get_pool.return_value = None

        result = await query_tool.batch_execute(
            connection_id="nonexistent_conn",
            query="INSERT INTO users (name) VALUES ($1)",
            params_list=[["test"]]
        )

        assert not result.success
        assert "Connection not found" in result.error

    @pytest.mark.asyncio
    async def test_batch_execute_database_error(self, query_tool, mock_connection_manager):
        """Test batch execution with database error."""
        connection_manager, mock_pool = mock_connection_manager

        mock_conn = AsyncMock()
        mock_conn.executemany.side_effect = Exception("Database error")
        mock_pool.acquire.return_value.__aenter__.return_value = mock_conn

        result = await query_tool.batch_execute(
            connection_id="test_conn",
            query="INSERT INTO users (name) VALUES ($1)",
            params_list=[["test1"], ["test2"]]
        )

        assert not result.success
        assert "Database error" in result.error
        assert result.batch_size == 2

    @pytest.mark.asyncio
    async def test_execute_query_with_fetch_size(self, query_tool, mock_connection_manager):
        """Test executing query with fetch size limitation."""
        connection_manager, mock_pool = mock_connection_manager

        # Mock large result set
        large_result = [{"id": i, "name": f"user{i}"} for i in range(1000)]
        mock_conn = AsyncMock()
        mock_conn.fetch.return_value = large_result[:50]  # Simulate fetch_size limit
        mock_pool.acquire.return_value.__aenter__.return_value = mock_conn

        result = await query_tool.execute_query(
            connection_id="test_conn",
            query="SELECT * FROM users",
            fetch_size=50
        )

        assert result.success
        assert len(result.rows) == 50
        assert result.row_count == 50

    @pytest.mark.asyncio
    async def test_execute_query_database_error(self, query_tool, mock_connection_manager):
        """Test executing query with database error."""
        connection_manager, mock_pool = mock_connection_manager

        mock_conn = AsyncMock()
        mock_conn.fetch.side_effect = Exception("Database connection error")
        mock_pool.acquire.return_value.__aenter__.return_value = mock_conn

        result = await query_tool.execute_query(
            connection_id="test_conn",
            query="SELECT * FROM users"
        )

        assert not result.success
        assert "Database connection error" in result.error
        assert result.duration_ms >= 0

    @pytest.mark.asyncio
    async def test_execute_explain_query(self, query_tool, mock_connection_manager):
        """Test executing EXPLAIN query."""
        connection_manager, mock_pool = mock_connection_manager

        mock_conn = AsyncMock()
        mock_conn.fetch.return_value = [{"QUERY PLAN": "Seq Scan on users"}]
        mock_pool.acquire.return_value.__aenter__.return_value = mock_conn

        result = await query_tool.execute_query(
            connection_id="test_conn",
            query="EXPLAIN SELECT * FROM users"
        )

        assert result.success
        assert len(result.rows) == 1
        assert "QUERY PLAN" in result.rows[0]

    @pytest.mark.asyncio
    async def test_execute_with_query(self, query_tool, mock_connection_manager):
        """Test executing WITH (CTE) query."""
        connection_manager, mock_pool = mock_connection_manager

        mock_conn = AsyncMock()
        mock_conn.fetch.return_value = [
            {"id": 1, "name": "user1", "rank": 1},
            {"id": 2, "name": "user2", "rank": 2}
        ]
        mock_pool.acquire.return_value.__aenter__.return_value = mock_conn

        result = await query_tool.execute_query(
            connection_id="test_conn",
            query="WITH ranked_users AS (SELECT *, ROW_NUMBER() OVER (ORDER BY id) as rank FROM users) SELECT * FROM ranked_users"
        )

        assert result.success
        assert len(result.rows) == 2
        assert "rank" in result.columns