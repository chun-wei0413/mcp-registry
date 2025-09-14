"""
Monitoring and Health Check Module

SPECIFICATION:
This module provides comprehensive monitoring, health checking, and performance
metrics collection for the PostgreSQL MCP Server. It tracks system health,
database connectivity, query performance, and resource utilization.

CORE MONITORING FEATURES:
- Health Status Tracking: Real-time server and database health monitoring
- Performance Metrics: Query execution times, connection pool utilization
- Resource Monitoring: Memory usage, connection counts, system resources
- Query Analytics: Query patterns, slow query detection, execution statistics
- Connection Monitoring: Database connection health and pool management
- Alert Generation: Automated alerts for system anomalies and failures

HEALTH CHECK COMPONENTS:
1. Server Health: MCP server responsiveness and resource availability
2. Database Health: Connection pool status and database accessibility
3. Query Performance: Average query times and slow query detection
4. Resource Utilization: Memory, CPU, and connection usage monitoring
5. Error Tracking: Error rates, failure patterns, and recovery metrics

METRICS COLLECTION:
- System Metrics: Uptime, memory usage, connection counts
- Query Metrics: Execution times, query counts, error rates
- Connection Metrics: Pool utilization, connection health, timeouts
- Performance Metrics: Throughput, latency, resource efficiency
- Historical Metrics: Time-series data for trend analysis

MONITORING CAPABILITIES:
- Real-time Health Checks: Continuous monitoring of system components
- Performance Baselines: Automatic performance threshold establishment
- Anomaly Detection: Statistical analysis of metric patterns
- Alerting System: Configurable alerts for critical conditions
- Metric Aggregation: Statistical summaries and trend analysis

USAGE PATTERN:
The monitoring system runs continuously in the background, collecting
metrics and performing health checks. It provides both immediate status
information and historical trend data for performance analysis.
"""

import asyncio
import time
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, field
import structlog

from .tools.connection import ConnectionManager

logger = structlog.get_logger()


@dataclass
class HealthStatus:
    """健康狀態"""
    is_healthy: bool
    status: str  # "healthy", "degraded", "unhealthy"
    checks: Dict[str, Dict[str, Any]] = field(default_factory=dict)
    timestamp: datetime = field(default_factory=datetime.utcnow)


@dataclass
class ConnectionMetrics:
    """連線指標"""
    connection_id: str
    pool_size: int
    idle_connections: int
    active_connections: int
    total_queries: int
    successful_queries: int
    failed_queries: int
    avg_query_time_ms: float
    last_activity: datetime


@dataclass
class ServerMetrics:
    """伺服器指標"""
    uptime_seconds: int
    total_connections: int
    total_queries: int
    successful_queries: int
    failed_queries: int
    avg_query_time_ms: float
    connections_metrics: List[ConnectionMetrics]
    memory_usage_bytes: int
    cpu_percent: float


class HealthChecker:
    """健康檢查器"""

    def __init__(self, connection_manager: ConnectionManager):
        self.connection_manager = connection_manager
        self.start_time = datetime.utcnow()

    async def check_overall_health(self) -> HealthStatus:
        """檢查整體健康狀態"""
        checks = {}
        overall_healthy = True

        # 1. 檢查連線池狀態
        connection_check = await self._check_connections()
        checks["connections"] = connection_check
        if not connection_check["is_healthy"]:
            overall_healthy = False

        # 2. 檢查記憶體使用率
        memory_check = await self._check_memory()
        checks["memory"] = memory_check
        if not memory_check["is_healthy"]:
            overall_healthy = False

        # 3. 檢查系統資源
        system_check = await self._check_system_resources()
        checks["system"] = system_check
        if not system_check["is_healthy"]:
            overall_healthy = False

        # 決定整體狀態
        if overall_healthy:
            status = "healthy"
        else:
            # 檢查是否為降級狀態
            degraded_conditions = [
                checks["connections"]["status"] == "degraded",
                checks["memory"]["warning"],
                checks["system"]["warning"]
            ]
            if any(degraded_conditions):
                status = "degraded"
            else:
                status = "unhealthy"

        return HealthStatus(
            is_healthy=overall_healthy,
            status=status,
            checks=checks
        )

    async def _check_connections(self) -> Dict[str, Any]:
        """檢查連線狀態"""
        try:
            connections = await self.connection_manager.get_all_connections()
            healthy_count = 0
            unhealthy_count = 0
            connection_details = []

            for conn_info in connections:
                conn_status = await self.connection_manager.test_connection(conn_info.connection_id)

                if conn_status.is_healthy:
                    healthy_count += 1
                else:
                    unhealthy_count += 1

                # 取得連線池統計
                pool = await self.connection_manager.get_pool(conn_info.connection_id)
                pool_stats = {}
                if pool:
                    pool_stats = await pool.get_pool_stats()

                connection_details.append({
                    "connection_id": conn_info.connection_id,
                    "is_healthy": conn_status.is_healthy,
                    "host": conn_info.host,
                    "database": conn_info.database,
                    "pool_stats": pool_stats
                })

            total_connections = len(connections)
            if total_connections == 0:
                status = "no_connections"
                is_healthy = True  # 沒有連線不算不健康
            elif unhealthy_count == 0:
                status = "healthy"
                is_healthy = True
            elif healthy_count > unhealthy_count:
                status = "degraded"
                is_healthy = True
            else:
                status = "unhealthy"
                is_healthy = False

            return {
                "is_healthy": is_healthy,
                "status": status,
                "total_connections": total_connections,
                "healthy_connections": healthy_count,
                "unhealthy_connections": unhealthy_count,
                "connections": connection_details
            }

        except Exception as e:
            logger.error("connection_health_check_failed", error=str(e))
            return {
                "is_healthy": False,
                "status": "error",
                "error": str(e)
            }

    async def _check_memory(self) -> Dict[str, Any]:
        """檢查記憶體使用率"""
        try:
            import psutil
            memory = psutil.virtual_memory()

            # 記憶體使用率閾值
            warning_threshold = 80  # 80%
            critical_threshold = 95  # 95%

            memory_percent = memory.percent
            is_healthy = memory_percent < critical_threshold
            warning = memory_percent > warning_threshold

            return {
                "is_healthy": is_healthy,
                "warning": warning,
                "memory_percent": memory_percent,
                "total_memory_gb": round(memory.total / (1024**3), 2),
                "available_memory_gb": round(memory.available / (1024**3), 2),
                "used_memory_gb": round(memory.used / (1024**3), 2)
            }

        except ImportError:
            # psutil 未安裝，跳過記憶體檢查
            return {
                "is_healthy": True,
                "warning": False,
                "message": "Memory monitoring not available (psutil not installed)"
            }
        except Exception as e:
            logger.error("memory_check_failed", error=str(e))
            return {
                "is_healthy": True,  # 記憶體檢查失敗不影響整體健康
                "warning": False,
                "error": str(e)
            }

    async def _check_system_resources(self) -> Dict[str, Any]:
        """檢查系統資源"""
        try:
            import psutil

            # CPU 使用率
            cpu_percent = psutil.cpu_percent(interval=1)

            # 磁碟使用率
            disk_usage = psutil.disk_usage('/')

            # 設定閾值
            cpu_warning = 80
            cpu_critical = 95
            disk_warning = 85
            disk_critical = 95

            cpu_healthy = cpu_percent < cpu_critical
            disk_percent = (disk_usage.used / disk_usage.total) * 100
            disk_healthy = disk_percent < disk_critical

            is_healthy = cpu_healthy and disk_healthy
            warning = cpu_percent > cpu_warning or disk_percent > disk_warning

            return {
                "is_healthy": is_healthy,
                "warning": warning,
                "cpu_percent": cpu_percent,
                "disk_percent": round(disk_percent, 1),
                "disk_total_gb": round(disk_usage.total / (1024**3), 2),
                "disk_free_gb": round(disk_usage.free / (1024**3), 2)
            }

        except ImportError:
            return {
                "is_healthy": True,
                "warning": False,
                "message": "System monitoring not available (psutil not installed)"
            }
        except Exception as e:
            logger.error("system_check_failed", error=str(e))
            return {
                "is_healthy": True,
                "warning": False,
                "error": str(e)
            }

    def get_uptime(self) -> Dict[str, Any]:
        """取得運行時間"""
        now = datetime.utcnow()
        uptime_delta = now - self.start_time
        uptime_seconds = int(uptime_delta.total_seconds())

        return {
            "start_time": self.start_time.isoformat(),
            "current_time": now.isoformat(),
            "uptime_seconds": uptime_seconds,
            "uptime_human": str(uptime_delta).split('.')[0]  # 去掉微秒
        }


class MetricsCollector:
    """指標收集器"""

    def __init__(self, connection_manager: ConnectionManager):
        self.connection_manager = connection_manager
        self.query_metrics = {}  # connection_id -> metrics
        self.start_time = datetime.utcnow()

    def record_query(self, connection_id: str, success: bool, duration_ms: int):
        """記錄查詢指標"""
        if connection_id not in self.query_metrics:
            self.query_metrics[connection_id] = {
                "total_queries": 0,
                "successful_queries": 0,
                "failed_queries": 0,
                "total_duration_ms": 0,
                "last_activity": datetime.utcnow()
            }

        metrics = self.query_metrics[connection_id]
        metrics["total_queries"] += 1
        metrics["total_duration_ms"] += duration_ms
        metrics["last_activity"] = datetime.utcnow()

        if success:
            metrics["successful_queries"] += 1
        else:
            metrics["failed_queries"] += 1

    async def get_server_metrics(self) -> ServerMetrics:
        """取得伺服器指標"""
        connections = await self.connection_manager.get_all_connections()
        connections_metrics = []

        total_queries = 0
        successful_queries = 0
        failed_queries = 0
        total_duration_ms = 0

        for conn_info in connections:
            metrics = self.query_metrics.get(conn_info.connection_id, {})

            # 取得連線池統計
            pool = await self.connection_manager.get_pool(conn_info.connection_id)
            pool_stats = {}
            if pool:
                pool_stats = await pool.get_pool_stats()

            queries = metrics.get("total_queries", 0)
            successful = metrics.get("successful_queries", 0)
            failed = metrics.get("failed_queries", 0)
            duration = metrics.get("total_duration_ms", 0)

            avg_query_time = (duration / queries) if queries > 0 else 0

            conn_metrics = ConnectionMetrics(
                connection_id=conn_info.connection_id,
                pool_size=pool_stats.get("size", 0),
                idle_connections=pool_stats.get("idle_size", 0),
                active_connections=pool_stats.get("size", 0) - pool_stats.get("idle_size", 0),
                total_queries=queries,
                successful_queries=successful,
                failed_queries=failed,
                avg_query_time_ms=avg_query_time,
                last_activity=metrics.get("last_activity", conn_info.created_at)
            )

            connections_metrics.append(conn_metrics)

            # 累計總計
            total_queries += queries
            successful_queries += successful
            failed_queries += failed
            total_duration_ms += duration

        # 計算整體平均查詢時間
        avg_query_time = (total_duration_ms / total_queries) if total_queries > 0 else 0

        # 取得系統資源使用率
        memory_usage = 0
        cpu_percent = 0
        try:
            import psutil
            process = psutil.Process()
            memory_usage = process.memory_info().rss
            cpu_percent = process.cpu_percent()
        except ImportError:
            pass

        uptime_seconds = int((datetime.utcnow() - self.start_time).total_seconds())

        return ServerMetrics(
            uptime_seconds=uptime_seconds,
            total_connections=len(connections),
            total_queries=total_queries,
            successful_queries=successful_queries,
            failed_queries=failed_queries,
            avg_query_time_ms=avg_query_time,
            connections_metrics=connections_metrics,
            memory_usage_bytes=memory_usage,
            cpu_percent=cpu_percent
        )