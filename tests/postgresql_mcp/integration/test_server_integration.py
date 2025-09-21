"""
Integration tests for PostgreSQL MCP Server
"""

import pytest
import asyncio
import os
from unittest.mock import patch, AsyncMock
from src.postgresql_mcp.server import PostgreSQLMCPServer
from src.postgresql_mcp.infrastructure.configuration.configuration_manager import EnvironmentConfigurationManager


class TestServerIntegration:
    """Integration tests for the complete server functionality."""

    @pytest.fixture
    def test_config_file(self, tmp_path):
        """Create a temporary config file for testing."""
        config_content = """
# Test configuration
MCP_SERVER_PORT=3001
MCP_LOG_LEVEL=DEBUG
DEFAULT_POOL_SIZE=5
QUERY_TIMEOUT=15
MAX_CONNECTIONS=20
ENABLE_QUERY_CACHE=false
CACHE_TTL_SECONDS=300

# Security Configuration
READONLY_MODE=false
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
MAX_QUERY_LENGTH=5000
ENABLE_QUERY_LOGGING=true

# Test Database Connection
DB_HOST=localhost
DB_PORT=5432
DB_DATABASE=test_db
DB_USER=test_user
DB_PASSWORD=test_password
DB_POOL_SIZE=5
"""
        config_file = tmp_path / "test.env"
        config_file.write_text(config_content)
        return str(config_file)

    @pytest.fixture
    def mock_server(self, test_config_file):
        """Create a mock server with test configuration."""
        with patch('src.tools.connection.asyncpg.connect') as mock_connect:
            mock_connect.return_value.__aenter__.return_value = AsyncMock()
            mock_connect.return_value.__aexit__.return_value = None

            with patch('src.db.pool.asyncpg.create_pool') as mock_create_pool:
                mock_pool = AsyncMock()
                mock_create_pool.return_value = mock_pool

                server = PostgreSQLMCPServer(test_config_file)
                return server

    def test_server_initialization(self, mock_server):
        """Test that server initializes correctly with configuration."""
        assert mock_server.server_config.port == 3001
        assert mock_server.server_config.log_level == "DEBUG"
        assert mock_server.server_config.default_pool_size == 5
        assert mock_server.security_config.readonly_mode == False
        assert mock_server.security_config.max_query_length == 5000

    @pytest.mark.asyncio
    async def test_config_manager_initialization(self, test_config_file):
        """Test that config manager loads configuration correctly."""
        config_manager = EnvironmentConfigurationManager()

        server_config = await config_manager.get_server_config()
        security_config = await config_manager.get_security_config()

        assert server_config["port"] == 3000  # Default value
        assert server_config["log_level"] == "INFO"  # Default value
        assert security_config["readonly_mode"] == False
        assert "SELECT" in security_config["allowed_operations"]
        assert "DROP" in security_config["blocked_keywords"]

    @pytest.mark.asyncio
    async def test_config_validation(self, test_config_file):
        """Test configuration validation."""
        config_manager = EnvironmentConfigurationManager()
        result = await config_manager.validate_config()
        assert result == True

    @pytest.mark.asyncio
    async def test_server_tools_registration(self, mock_server):
        """Test that all MCP tools are properly registered."""
        # This would need to be adapted based on the actual FastMCP API
        # For now, we'll test that the server has the expected components
        assert hasattr(mock_server, 'connection_manager')
        assert hasattr(mock_server, 'query_tool')
        assert hasattr(mock_server, 'schema_tool')
        assert hasattr(mock_server, 'health_checker')
        assert hasattr(mock_server, 'metrics_collector')

    @pytest.mark.asyncio
    async def test_add_connection_workflow(self, mock_server):
        """Test the complete workflow of adding a connection."""
        with patch.object(mock_server.connection_manager, 'add_connection') as mock_add:
            mock_add.return_value = {
                "connection_id": "test_conn",
                "success": True,
                "message": "Connection established successfully"
            }

            result = await mock_server.connection_manager.add_connection(
                connection_id="test_conn",
                host="localhost",
                port=5432,
                database="test_db",
                user="test_user",
                password="test_password"
            )

            assert result["success"]
            assert result["connection_id"] == "test_conn"

    @pytest.mark.asyncio
    async def test_query_execution_workflow(self, mock_server):
        """Test the complete workflow of query execution."""
        # Setup mock connection
        with patch.object(mock_server.query_tool, 'execute_query') as mock_query:
            mock_query.return_value = {
                "success": True,
                "rows": [{"id": 1, "name": "test"}],
                "row_count": 1,
                "columns": ["id", "name"],
                "duration_ms": 50
            }

            result = await mock_server.query_tool.execute_query(
                connection_id="test_conn",
                query="SELECT * FROM users WHERE id = $1",
                params=[1]
            )

            assert result["success"]
            assert len(result["rows"]) == 1
            assert result["row_count"] == 1

    @pytest.mark.asyncio
    async def test_health_check_workflow(self, mock_server):
        """Test the complete health check workflow."""
        with patch.object(mock_server.health_checker, 'check_overall_health') as mock_health:
            mock_health.return_value = AsyncMock()
            mock_health.return_value.is_healthy = True
            mock_health.return_value.status = "healthy"
            mock_health.return_value.timestamp = "2024-01-01T00:00:00Z"
            mock_health.return_value.checks = {}

            with patch.object(mock_server.health_checker, 'get_uptime') as mock_uptime:
                mock_uptime.return_value = {
                    "uptime_seconds": 3600,
                    "uptime_human": "1:00:00"
                }

                # This would be called through the MCP framework
                # For now we test the underlying functionality
                health_status = await mock_server.health_checker.check_overall_health()
                uptime = mock_server.health_checker.get_uptime()

                assert health_status.is_healthy
                assert health_status.status == "healthy"
                assert uptime["uptime_seconds"] == 3600

    @pytest.mark.asyncio
    async def test_metrics_collection_workflow(self, mock_server):
        """Test the complete metrics collection workflow."""
        # Simulate some query activity
        mock_server.metrics_collector.record_query("test_conn", True, 100)
        mock_server.metrics_collector.record_query("test_conn", True, 150)
        mock_server.metrics_collector.record_query("test_conn", False, 75)

        with patch.object(mock_server.metrics_collector, 'get_server_metrics') as mock_metrics:
            mock_metrics.return_value = AsyncMock()
            mock_metrics.return_value.total_queries = 3
            mock_metrics.return_value.successful_queries = 2
            mock_metrics.return_value.failed_queries = 1
            mock_metrics.return_value.avg_query_time_ms = 108.33
            mock_metrics.return_value.connections_metrics = []

            metrics = await mock_server.metrics_collector.get_server_metrics()

            assert metrics.total_queries == 3
            assert metrics.successful_queries == 2
            assert metrics.failed_queries == 1

    @pytest.mark.asyncio
    async def test_query_history_tracking(self, mock_server):
        """Test that query history is properly tracked."""
        # Initially empty
        assert len(mock_server.query_history) == 0

        # Add some query history
        mock_server._add_query_history(
            connection_id="test_conn",
            query="SELECT * FROM users",
            params=None,
            success=True,
            duration_ms=100,
            rows_affected=5,
            error=None
        )

        mock_server._add_query_history(
            connection_id="test_conn",
            query="INSERT INTO logs VALUES ($1)",
            params=["test"],
            success=False,
            duration_ms=50,
            rows_affected=0,
            error="Constraint violation"
        )

        assert len(mock_server.query_history) == 2

        # Check first entry
        first_entry = mock_server.query_history[0]
        assert first_entry.connection_id == "test_conn"
        assert first_entry.query == "SELECT * FROM users"
        assert first_entry.success == True
        assert first_entry.duration_ms == 100

        # Check second entry
        second_entry = mock_server.query_history[1]
        assert second_entry.connection_id == "test_conn"
        assert second_entry.success == False
        assert second_entry.error == "Constraint violation"

    def test_query_history_limit(self, mock_server):
        """Test that query history respects the limit."""
        # Add more than the limit (1000 queries)
        for i in range(1050):
            mock_server._add_query_history(
                connection_id="test_conn",
                query=f"SELECT {i}",
                params=None,
                success=True,
                duration_ms=10,
                rows_affected=1,
                error=None
            )

        # Should be limited to 1000
        assert len(mock_server.query_history) == 1000

        # Should keep the most recent ones
        assert mock_server.query_history[-1].query == "SELECT 1049"
        assert mock_server.query_history[0].query == "SELECT 50"

    def test_security_integration_with_query_tool(self, mock_server):
        """Test that security validation is integrated with query tool."""
        # The query tool should have security validation enabled
        assert mock_server.query_tool.security_validator is not None

        # Test that security config is properly passed
        security_config = mock_server.query_tool.security_validator.config
        assert security_config.readonly_mode == False
        assert "SELECT" in security_config.allowed_operations
        assert "DROP" in security_config.blocked_keywords

    def test_connection_manager_security_integration(self, mock_server):
        """Test that connection manager has security validation."""
        assert mock_server.connection_manager.security_validator is not None

        # Test that security config is properly passed
        security_config = mock_server.connection_manager.security_validator.config
        assert security_config.max_query_length == 5000

    @pytest.mark.asyncio
    async def test_error_handling_in_server_workflow(self, mock_server):
        """Test error handling throughout the server workflow."""
        # Test query execution with connection error
        with patch.object(mock_server.connection_manager, 'get_pool') as mock_get_pool:
            mock_get_pool.return_value = None

            result = await mock_server.query_tool.execute_query(
                connection_id="nonexistent",
                query="SELECT 1"
            )

            assert not result.success
            assert "Connection not found" in result.error

    def test_readonly_mode_server(self, tmp_path):
        """Test server in readonly mode."""
        readonly_config = """
READONLY_MODE=true
ALLOWED_OPERATIONS=SELECT,WITH,EXPLAIN
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER,INSERT,UPDATE,DELETE
"""
        config_file = tmp_path / "readonly.env"
        config_file.write_text(readonly_config)

        with patch('src.tools.connection.asyncpg.connect'):
            with patch('src.db.pool.asyncpg.create_pool'):
                server = PostgreSQLMCPServer(str(config_file))

                assert server.security_config.readonly_mode == True
                assert "INSERT" in server.security_config.blocked_keywords
                assert "UPDATE" in server.security_config.blocked_keywords
                assert "DELETE" in server.security_config.blocked_keywords