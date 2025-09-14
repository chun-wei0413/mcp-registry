"""Metrics collection implementation."""

from typing import Dict, List, Any, Optional
import time
from collections import defaultdict, deque
import structlog

from ...core.interfaces import IMetricsCollector
from ...core.exceptions import MonitoringError
from ...domain.models import MetricsData

logger = structlog.get_logger()


class InMemoryMetricsCollector(IMetricsCollector):
    """In-memory metrics collector implementation."""

    def __init__(self, max_history: int = 1000):
        self._max_history = max_history
        self._query_metrics = defaultdict(lambda: deque(maxlen=max_history))
        self._connection_metrics = defaultdict(lambda: {
            "total_queries": 0,
            "successful_queries": 0,
            "failed_queries": 0,
            "total_execution_time_ms": 0,
            "last_query_time": None
        })
        self._global_metrics = {
            "total_queries": 0,
            "successful_queries": 0,
            "failed_queries": 0,
            "total_execution_time_ms": 0,
            "start_time": int(time.time())
        }

    async def record_query_execution(
        self,
        connection_id: str,
        execution_time_ms: int,
        success: bool,
        query_type: Optional[str] = None
    ) -> None:
        """Record query execution metrics."""
        try:
            timestamp = int(time.time())

            # Record individual query
            query_record = {
                "timestamp": timestamp,
                "execution_time_ms": execution_time_ms,
                "success": success,
                "query_type": query_type
            }
            self._query_metrics[connection_id].append(query_record)

            # Update connection metrics
            conn_metrics = self._connection_metrics[connection_id]
            conn_metrics["total_queries"] += 1
            conn_metrics["total_execution_time_ms"] += execution_time_ms
            conn_metrics["last_query_time"] = timestamp

            if success:
                conn_metrics["successful_queries"] += 1
                self._global_metrics["successful_queries"] += 1
            else:
                conn_metrics["failed_queries"] += 1
                self._global_metrics["failed_queries"] += 1

            # Update global metrics
            self._global_metrics["total_queries"] += 1
            self._global_metrics["total_execution_time_ms"] += execution_time_ms

            logger.debug(
                "query_metrics_recorded",
                connection_id=connection_id,
                execution_time_ms=execution_time_ms,
                success=success,
                query_type=query_type
            )

        except Exception as e:
            logger.error(
                "metrics_recording_failed",
                connection_id=connection_id,
                error=str(e)
            )
            raise MonitoringError(f"Failed to record query metrics: {e}")

    async def get_connection_metrics(self, connection_id: str) -> MetricsData:
        """Get metrics for a specific connection."""
        try:
            if connection_id not in self._connection_metrics:
                return MetricsData(
                    connection_id=connection_id,
                    total_queries=0,
                    successful_queries=0,
                    failed_queries=0,
                    average_execution_time_ms=0,
                    last_query_time=None,
                    query_history=[]
                )

            conn_metrics = self._connection_metrics[connection_id]
            query_history = list(self._query_metrics[connection_id])

            # Calculate average execution time
            avg_time = 0
            if conn_metrics["total_queries"] > 0:
                avg_time = conn_metrics["total_execution_time_ms"] / conn_metrics["total_queries"]

            return MetricsData(
                connection_id=connection_id,
                total_queries=conn_metrics["total_queries"],
                successful_queries=conn_metrics["successful_queries"],
                failed_queries=conn_metrics["failed_queries"],
                average_execution_time_ms=int(avg_time),
                last_query_time=conn_metrics["last_query_time"],
                query_history=query_history
            )

        except Exception as e:
            logger.error(
                "get_connection_metrics_failed",
                connection_id=connection_id,
                error=str(e)
            )
            raise MonitoringError(f"Failed to get connection metrics: {e}")

    async def get_global_metrics(self) -> Dict[str, Any]:
        """Get global system metrics."""
        try:
            current_time = int(time.time())
            uptime_seconds = current_time - self._global_metrics["start_time"]

            # Calculate averages
            avg_execution_time = 0
            if self._global_metrics["total_queries"] > 0:
                avg_execution_time = (
                    self._global_metrics["total_execution_time_ms"] /
                    self._global_metrics["total_queries"]
                )

            success_rate = 0
            if self._global_metrics["total_queries"] > 0:
                success_rate = (
                    self._global_metrics["successful_queries"] /
                    self._global_metrics["total_queries"] * 100
                )

            # Calculate queries per second
            qps = 0
            if uptime_seconds > 0:
                qps = self._global_metrics["total_queries"] / uptime_seconds

            return {
                "uptime_seconds": uptime_seconds,
                "total_queries": self._global_metrics["total_queries"],
                "successful_queries": self._global_metrics["successful_queries"],
                "failed_queries": self._global_metrics["failed_queries"],
                "success_rate_percent": round(success_rate, 2),
                "average_execution_time_ms": round(avg_execution_time, 2),
                "queries_per_second": round(qps, 2),
                "active_connections": len(self._connection_metrics),
                "total_execution_time_ms": self._global_metrics["total_execution_time_ms"]
            }

        except Exception as e:
            logger.error("get_global_metrics_failed", error=str(e))
            raise MonitoringError(f"Failed to get global metrics: {e}")

    async def reset_metrics(self, connection_id: Optional[str] = None) -> None:
        """Reset metrics for connection or globally."""
        try:
            if connection_id:
                # Reset specific connection
                if connection_id in self._connection_metrics:
                    del self._connection_metrics[connection_id]
                if connection_id in self._query_metrics:
                    del self._query_metrics[connection_id]

                logger.info("connection_metrics_reset", connection_id=connection_id)
            else:
                # Reset all metrics
                self._query_metrics.clear()
                self._connection_metrics.clear()
                self._global_metrics = {
                    "total_queries": 0,
                    "successful_queries": 0,
                    "failed_queries": 0,
                    "total_execution_time_ms": 0,
                    "start_time": int(time.time())
                }

                logger.info("all_metrics_reset")

        except Exception as e:
            logger.error(
                "metrics_reset_failed",
                connection_id=connection_id,
                error=str(e)
            )
            raise MonitoringError(f"Failed to reset metrics: {e}")

    async def collect_metrics(self) -> MetricsData:
        """Collect system metrics."""
        try:
            # Return system-wide metrics as MetricsData for the first connection
            # or create a synthetic one if no connections exist
            if self._connection_metrics:
                first_connection_id = list(self._connection_metrics.keys())[0]
                return await self.get_connection_metrics(first_connection_id)
            else:
                # Return empty metrics if no connections
                return MetricsData(
                    connection_id="system",
                    total_queries=self._global_metrics["total_queries"],
                    successful_queries=self._global_metrics["successful_queries"],
                    failed_queries=self._global_metrics["failed_queries"],
                    average_execution_time_ms=0,
                    last_query_time=None,
                    query_history=[]
                )

        except Exception as e:
            logger.error("collect_metrics_failed", error=str(e))
            raise MonitoringError(f"Failed to collect metrics: {e}")

    async def export_metrics(self) -> Dict[str, Any]:
        """Export all metrics for external systems."""
        try:
            global_metrics = await self.get_global_metrics()
            connection_metrics = {}

            for connection_id in self._connection_metrics.keys():
                metrics_data = await self.get_connection_metrics(connection_id)
                connection_metrics[connection_id] = {
                    "total_queries": metrics_data.total_queries,
                    "successful_queries": metrics_data.successful_queries,
                    "failed_queries": metrics_data.failed_queries,
                    "average_execution_time_ms": metrics_data.average_execution_time_ms,
                    "last_query_time": metrics_data.last_query_time,
                    "query_history_count": len(metrics_data.query_history)
                }

            return {
                "global": global_metrics,
                "connections": connection_metrics,
                "export_timestamp": int(time.time())
            }

        except Exception as e:
            logger.error("metrics_export_failed", error=str(e))
            raise MonitoringError(f"Failed to export metrics: {e}")