"""
Unit tests for monitoring and health check functionality
"""

import pytest
from unittest.mock import AsyncMock, Mock, patch
from datetime import datetime, timedelta
from src.postgresql_mcp.infrastructure.monitoring.health_checker import PostgreSQLHealthChecker
from src.postgresql_mcp.infrastructure.monitoring.metrics_collector import InMemoryMetricsCollector
from src.postgresql_mcp.domain.models import Connection


class TestHealthChecker:
    """Test health checking functionality."""

    @pytest.fixture
    def mock_connection_manager(self):
        """Mock connection manager for testing."""
        manager = AsyncMock()
        return manager

    @pytest.fixture
    def health_checker(self, mock_connection_manager):
        """Create health checker with mocked dependencies."""
        return HealthChecker(mock_connection_manager)

    @pytest.mark.asyncio
    async def test_check_overall_health_all_healthy(self, health_checker, mock_connection_manager):
        """Test overall health check when all systems are healthy."""
        # Mock all connections as healthy
        mock_connection_manager.get_all_connections.return_value = [
            ConnectionInfo(
                connection_id="test_conn",
                host="localhost",
                port=5432,
                database="test_db",
                user="test_user",
                encrypted_password="encrypted",
                is_active=True
            )
        ]

        mock_connection_manager.test_connection.return_value = Mock(is_healthy=True)
        mock_connection_manager.get_pool.return_value = AsyncMock()

        with patch('src.monitoring.psutil') as mock_psutil:
            # Mock memory and CPU stats
            mock_memory = Mock()
            mock_memory.percent = 45.0
            mock_memory.total = 16 * 1024**3  # 16GB
            mock_memory.available = 8 * 1024**3  # 8GB available
            mock_memory.used = 8 * 1024**3  # 8GB used
            mock_psutil.virtual_memory.return_value = mock_memory
            mock_psutil.cpu_percent.return_value = 25.0

            mock_disk = Mock()
            mock_disk.total = 1000 * 1024**3  # 1TB
            mock_disk.used = 300 * 1024**3   # 300GB used
            mock_disk.free = 700 * 1024**3   # 700GB free
            mock_psutil.disk_usage.return_value = mock_disk

            health_status = await health_checker.check_overall_health()

        assert health_status.is_healthy
        assert health_status.status == "healthy"
        assert "connections" in health_status.checks
        assert "memory" in health_status.checks
        assert "system" in health_status.checks

    @pytest.mark.asyncio
    async def test_check_overall_health_connection_unhealthy(self, health_checker, mock_connection_manager):
        """Test overall health check when connections are unhealthy."""
        # Mock connections with one unhealthy
        mock_connection_manager.get_all_connections.return_value = [
            ConnectionInfo(
                connection_id="healthy_conn",
                host="localhost",
                port=5432,
                database="test_db",
                user="test_user",
                encrypted_password="encrypted",
                is_active=True
            ),
            ConnectionInfo(
                connection_id="unhealthy_conn",
                host="remote",
                port=5432,
                database="test_db",
                user="test_user",
                encrypted_password="encrypted",
                is_active=True
            )
        ]

        # First connection healthy, second unhealthy
        mock_connection_manager.test_connection.side_effect = [
            Mock(is_healthy=True),
            Mock(is_healthy=False)
        ]
        mock_connection_manager.get_pool.return_value = AsyncMock()

        with patch('src.monitoring.psutil') as mock_psutil:
            # Mock healthy memory and CPU
            mock_memory = Mock()
            mock_memory.percent = 45.0
            mock_memory.total = 16 * 1024**3
            mock_memory.available = 8 * 1024**3
            mock_memory.used = 8 * 1024**3
            mock_psutil.virtual_memory.return_value = mock_memory
            mock_psutil.cpu_percent.return_value = 25.0

            mock_disk = Mock()
            mock_disk.total = 1000 * 1024**3
            mock_disk.used = 300 * 1024**3
            mock_disk.free = 700 * 1024**3
            mock_psutil.disk_usage.return_value = mock_disk

            health_status = await health_checker.check_overall_health()

        assert health_status.is_healthy  # Still healthy because more connections are healthy
        assert health_status.status == "degraded"
        assert health_status.checks["connections"]["status"] == "degraded"

    @pytest.mark.asyncio
    async def test_check_connections_no_connections(self, health_checker, mock_connection_manager):
        """Test connection check when no connections exist."""
        mock_connection_manager.get_all_connections.return_value = []

        result = await health_checker._check_connections()

        assert result["is_healthy"]
        assert result["status"] == "no_connections"
        assert result["total_connections"] == 0

    @pytest.mark.asyncio
    async def test_check_connections_all_healthy(self, health_checker, mock_connection_manager):
        """Test connection check when all connections are healthy."""
        mock_connection_manager.get_all_connections.return_value = [
            ConnectionInfo(
                connection_id="conn1",
                host="localhost",
                port=5432,
                database="db1",
                user="user",
                encrypted_password="encrypted",
                is_active=True
            ),
            ConnectionInfo(
                connection_id="conn2",
                host="localhost",
                port=5432,
                database="db2",
                user="user",
                encrypted_password="encrypted",
                is_active=True
            )
        ]

        mock_connection_manager.test_connection.return_value = Mock(is_healthy=True)
        mock_pool = AsyncMock()
        mock_pool.get_pool_stats.return_value = {
            "size": 5,
            "idle_size": 3
        }
        mock_connection_manager.get_pool.return_value = mock_pool

        result = await health_checker._check_connections()

        assert result["is_healthy"]
        assert result["status"] == "healthy"
        assert result["total_connections"] == 2
        assert result["healthy_connections"] == 2
        assert result["unhealthy_connections"] == 0

    @pytest.mark.asyncio
    async def test_check_memory_normal(self, health_checker):
        """Test memory check with normal usage."""
        with patch('src.monitoring.psutil') as mock_psutil:
            mock_memory = Mock()
            mock_memory.percent = 60.0  # Normal usage
            mock_memory.total = 16 * 1024**3
            mock_memory.available = 6.4 * 1024**3
            mock_memory.used = 9.6 * 1024**3
            mock_psutil.virtual_memory.return_value = mock_memory

            result = await health_checker._check_memory()

        assert result["is_healthy"]
        assert not result["warning"]
        assert result["memory_percent"] == 60.0

    @pytest.mark.asyncio
    async def test_check_memory_warning(self, health_checker):
        """Test memory check with warning level usage."""
        with patch('src.monitoring.psutil') as mock_psutil:
            mock_memory = Mock()
            mock_memory.percent = 85.0  # Warning level
            mock_memory.total = 16 * 1024**3
            mock_memory.available = 2.4 * 1024**3
            mock_memory.used = 13.6 * 1024**3
            mock_psutil.virtual_memory.return_value = mock_memory

            result = await health_checker._check_memory()

        assert result["is_healthy"]  # Still healthy but warning
        assert result["warning"]
        assert result["memory_percent"] == 85.0

    @pytest.mark.asyncio
    async def test_check_memory_critical(self, health_checker):
        """Test memory check with critical level usage."""
        with patch('src.monitoring.psutil') as mock_psutil:
            mock_memory = Mock()
            mock_memory.percent = 97.0  # Critical level
            mock_memory.total = 16 * 1024**3
            mock_memory.available = 0.48 * 1024**3
            mock_memory.used = 15.52 * 1024**3
            mock_psutil.virtual_memory.return_value = mock_memory

            result = await health_checker._check_memory()

        assert not result["is_healthy"]
        assert result["warning"]
        assert result["memory_percent"] == 97.0

    @pytest.mark.asyncio
    async def test_check_memory_psutil_not_available(self, health_checker):
        """Test memory check when psutil is not available."""
        with patch('src.monitoring.psutil', side_effect=ImportError):
            result = await health_checker._check_memory()

        assert result["is_healthy"]
        assert not result["warning"]
        assert "not available" in result["message"]

    @pytest.mark.asyncio
    async def test_check_system_resources_normal(self, health_checker):
        """Test system resources check with normal usage."""
        with patch('src.monitoring.psutil') as mock_psutil:
            mock_psutil.cpu_percent.return_value = 45.0

            mock_disk = Mock()
            mock_disk.total = 1000 * 1024**3  # 1TB
            mock_disk.used = 500 * 1024**3   # 500GB used (50%)
            mock_disk.free = 500 * 1024**3   # 500GB free
            mock_psutil.disk_usage.return_value = mock_disk

            result = await health_checker._check_system_resources()

        assert result["is_healthy"]
        assert not result["warning"]
        assert result["cpu_percent"] == 45.0
        assert result["disk_percent"] == 50.0

    @pytest.mark.asyncio
    async def test_check_system_resources_warning(self, health_checker):
        """Test system resources check with warning levels."""
        with patch('src.monitoring.psutil') as mock_psutil:
            mock_psutil.cpu_percent.return_value = 85.0  # Warning level

            mock_disk = Mock()
            mock_disk.total = 1000 * 1024**3
            mock_disk.used = 870 * 1024**3   # 87% used (warning level)
            mock_disk.free = 130 * 1024**3
            mock_psutil.disk_usage.return_value = mock_disk

            result = await health_checker._check_system_resources()

        assert result["is_healthy"]
        assert result["warning"]
        assert result["cpu_percent"] == 85.0
        assert result["disk_percent"] == 87.0

    def test_get_uptime(self, health_checker):
        """Test uptime calculation."""
        # Set start time to 1 hour ago
        health_checker.start_time = datetime.utcnow() - timedelta(hours=1)

        uptime = health_checker.get_uptime()

        assert uptime["uptime_seconds"] >= 3600  # At least 1 hour
        assert uptime["uptime_seconds"] < 3700   # Less than 1 hour 2 minutes
        assert "start_time" in uptime
        assert "current_time" in uptime
        assert "uptime_human" in uptime


class TestMetricsCollector:
    """Test metrics collection functionality."""

    @pytest.fixture
    def mock_connection_manager(self):
        """Mock connection manager for testing."""
        manager = AsyncMock()
        return manager

    @pytest.fixture
    def metrics_collector(self, mock_connection_manager):
        """Create metrics collector with mocked dependencies."""
        return MetricsCollector(mock_connection_manager)

    def test_record_query_success(self, metrics_collector):
        """Test recording successful query metrics."""
        metrics_collector.record_query("test_conn", True, 150)

        assert "test_conn" in metrics_collector.query_metrics
        metrics = metrics_collector.query_metrics["test_conn"]
        assert metrics["total_queries"] == 1
        assert metrics["successful_queries"] == 1
        assert metrics["failed_queries"] == 0
        assert metrics["total_duration_ms"] == 150

    def test_record_query_failure(self, metrics_collector):
        """Test recording failed query metrics."""
        metrics_collector.record_query("test_conn", False, 75)

        assert "test_conn" in metrics_collector.query_metrics
        metrics = metrics_collector.query_metrics["test_conn"]
        assert metrics["total_queries"] == 1
        assert metrics["successful_queries"] == 0
        assert metrics["failed_queries"] == 1
        assert metrics["total_duration_ms"] == 75

    def test_record_multiple_queries(self, metrics_collector):
        """Test recording multiple queries for the same connection."""
        metrics_collector.record_query("test_conn", True, 100)
        metrics_collector.record_query("test_conn", True, 200)
        metrics_collector.record_query("test_conn", False, 50)

        metrics = metrics_collector.query_metrics["test_conn"]
        assert metrics["total_queries"] == 3
        assert metrics["successful_queries"] == 2
        assert metrics["failed_queries"] == 1
        assert metrics["total_duration_ms"] == 350

    @pytest.mark.asyncio
    async def test_get_server_metrics_no_connections(self, metrics_collector, mock_connection_manager):
        """Test getting server metrics with no connections."""
        mock_connection_manager.get_all_connections.return_value = []

        with patch('src.monitoring.psutil') as mock_psutil:
            mock_process = Mock()
            mock_process.memory_info.return_value.rss = 128 * 1024 * 1024  # 128MB
            mock_process.cpu_percent.return_value = 15.5
            mock_psutil.Process.return_value = mock_process

            metrics = await metrics_collector.get_server_metrics()

        assert metrics.total_connections == 0
        assert metrics.total_queries == 0
        assert metrics.successful_queries == 0
        assert metrics.failed_queries == 0
        assert metrics.avg_query_time_ms == 0
        assert len(metrics.connections_metrics) == 0
        assert metrics.memory_usage_bytes == 128 * 1024 * 1024
        assert metrics.cpu_percent == 15.5

    @pytest.mark.asyncio
    async def test_get_server_metrics_with_connections(self, metrics_collector, mock_connection_manager):
        """Test getting server metrics with active connections."""
        # Setup mock connections
        connections = [
            ConnectionInfo(
                connection_id="conn1",
                host="localhost",
                port=5432,
                database="db1",
                user="user",
                encrypted_password="encrypted",
                is_active=True,
                created_at=datetime.utcnow() - timedelta(hours=1)
            ),
            ConnectionInfo(
                connection_id="conn2",
                host="localhost",
                port=5432,
                database="db2",
                user="user",
                encrypted_password="encrypted",
                is_active=True,
                created_at=datetime.utcnow() - timedelta(minutes=30)
            )
        ]
        mock_connection_manager.get_all_connections.return_value = connections

        # Setup mock pools
        mock_pool1 = AsyncMock()
        mock_pool1.get_pool_stats.return_value = {
            "size": 8, "idle_size": 5
        }
        mock_pool2 = AsyncMock()
        mock_pool2.get_pool_stats.return_value = {
            "size": 6, "idle_size": 2
        }
        mock_connection_manager.get_pool.side_effect = [mock_pool1, mock_pool2]

        # Record some query metrics
        metrics_collector.record_query("conn1", True, 100)
        metrics_collector.record_query("conn1", True, 200)
        metrics_collector.record_query("conn1", False, 50)
        metrics_collector.record_query("conn2", True, 75)
        metrics_collector.record_query("conn2", True, 125)

        with patch('src.monitoring.psutil') as mock_psutil:
            mock_process = Mock()
            mock_process.memory_info.return_value.rss = 256 * 1024 * 1024  # 256MB
            mock_process.cpu_percent.return_value = 25.0
            mock_psutil.Process.return_value = mock_process

            metrics = await metrics_collector.get_server_metrics()

        assert metrics.total_connections == 2
        assert metrics.total_queries == 5
        assert metrics.successful_queries == 4
        assert metrics.failed_queries == 1
        assert metrics.avg_query_time_ms == (100 + 200 + 50 + 75 + 125) / 5  # 110

        # Check individual connection metrics
        assert len(metrics.connections_metrics) == 2

        conn1_metrics = next(m for m in metrics.connections_metrics if m.connection_id == "conn1")
        assert conn1_metrics.total_queries == 3
        assert conn1_metrics.successful_queries == 2
        assert conn1_metrics.failed_queries == 1
        assert conn1_metrics.avg_query_time_ms == (100 + 200 + 50) / 3
        assert conn1_metrics.pool_size == 8
        assert conn1_metrics.idle_connections == 5
        assert conn1_metrics.active_connections == 3

        conn2_metrics = next(m for m in metrics.connections_metrics if m.connection_id == "conn2")
        assert conn2_metrics.total_queries == 2
        assert conn2_metrics.successful_queries == 2
        assert conn2_metrics.failed_queries == 0
        assert conn2_metrics.avg_query_time_ms == (75 + 125) / 2
        assert conn2_metrics.pool_size == 6
        assert conn2_metrics.idle_connections == 2
        assert conn2_metrics.active_connections == 4

    @pytest.mark.asyncio
    async def test_get_server_metrics_psutil_not_available(self, metrics_collector, mock_connection_manager):
        """Test getting server metrics when psutil is not available."""
        mock_connection_manager.get_all_connections.return_value = []

        with patch('src.monitoring.psutil', side_effect=ImportError):
            metrics = await metrics_collector.get_server_metrics()

        assert metrics.memory_usage_bytes == 0
        assert metrics.cpu_percent == 0

    def test_metrics_collector_start_time(self, metrics_collector):
        """Test that metrics collector tracks start time."""
        now = datetime.utcnow()
        time_diff = abs((metrics_collector.start_time - now).total_seconds())
        assert time_diff < 1  # Should be very recent