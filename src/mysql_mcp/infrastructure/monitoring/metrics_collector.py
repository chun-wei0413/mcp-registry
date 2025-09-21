"""MySQL Metrics Collector Implementation"""

import time
from collections import defaultdict
from datetime import datetime
from typing import Dict, Any, Optional, List
import structlog

from ...core.interfaces import IMetricsCollector, ConnectionMetrics
from ...core.exceptions import MySQLMCPServerError

logger = structlog.get_logger()


class MySQLMetricsCollector(IMetricsCollector):
    """MySQL metrics collector implementation"""

    def __init__(self):
        self._connection_metrics: Dict[str, ConnectionMetrics] = {}
        self._global_metrics = {
            "total_connections": 0,
            "active_connections": 0,
            "total_queries": 0,
            "successful_queries": 0,
            "failed_queries": 0,
            "total_execution_time_ms": 0.0,
            "server_start_time": datetime.utcnow()
        }

    async def record_query(
        self,
        connection_id: str,
        query: str,
        execution_time_ms: float,
        success: bool,
        error_message: Optional[str] = None
    ) -> None:
        """Record query execution metrics"""
        try:
            # Initialize connection metrics if not exists
            if connection_id not in self._connection_metrics:
                self._connection_metrics[connection_id] = ConnectionMetrics(
                    connection_id=connection_id
                )

            conn_metrics = self._connection_metrics[connection_id]

            # Update connection metrics
            conn_metrics.total_queries += 1
            if success:
                conn_metrics.successful_queries += 1
                self._global_metrics["successful_queries"] += 1
            else:
                conn_metrics.failed_queries += 1
                self._global_metrics["failed_queries"] += 1

            # Update execution time
            total_time = (conn_metrics.average_execution_time_ms * (conn_metrics.total_queries - 1)) + execution_time_ms
            conn_metrics.average_execution_time_ms = total_time / conn_metrics.total_queries
            conn_metrics.last_query_time = datetime.utcnow()

            # Add to query history (keep last 100)
            query_record = {
                "timestamp": datetime.utcnow().isoformat(),
                "query": query[:200],  # Truncate long queries
                "execution_time_ms": execution_time_ms,
                "success": success,
                "error": error_message if not success else None
            }
            conn_metrics.query_history.append(query_record)
            if len(conn_metrics.query_history) > 100:
                conn_metrics.query_history.pop(0)

            # Update global metrics
            self._global_metrics["total_queries"] += 1
            self._global_metrics["total_execution_time_ms"] += execution_time_ms

            logger.debug(
                "mysql_query_metrics_recorded",
                connection_id=connection_id,
                execution_time_ms=execution_time_ms,
                success=success,
                total_queries=conn_metrics.total_queries
            )

        except Exception as e:
            logger.error(
                "mysql_metrics_recording_failed",
                connection_id=connection_id,
                error=str(e)
            )

    async def get_connection_metrics(self, connection_id: str) -> ConnectionMetrics:
        """Get metrics for specific connection"""
        if connection_id not in self._connection_metrics:
            # Return empty metrics for unknown connections
            return ConnectionMetrics(connection_id=connection_id)

        return self._connection_metrics[connection_id]

    async def get_global_metrics(self) -> Dict[str, Any]:
        """Get global server metrics"""
        try:
            # Calculate derived metrics
            total_queries = self._global_metrics["total_queries"]
            successful_queries = self._global_metrics["successful_queries"]
            total_execution_time = self._global_metrics["total_execution_time_ms"]

            success_rate = (successful_queries / total_queries * 100) if total_queries > 0 else 0.0
            average_execution_time = total_execution_time / total_queries if total_queries > 0 else 0.0

            # Calculate uptime
            uptime_seconds = (datetime.utcnow() - self._global_metrics["server_start_time"]).total_seconds()

            # Count active connections
            active_connections = len(self._connection_metrics)

            return {
                "total_connections": active_connections,
                "active_connections": active_connections,
                "total_queries": total_queries,
                "successful_queries": successful_queries,
                "failed_queries": self._global_metrics["failed_queries"],
                "success_rate_percent": round(success_rate, 2),
                "average_execution_time_ms": round(average_execution_time, 2),
                "total_execution_time_ms": round(total_execution_time, 2),
                "uptime_seconds": round(uptime_seconds, 2),
                "server_start_time": self._global_metrics["server_start_time"].isoformat(),
                "connections": {
                    conn_id: {
                        "total_queries": metrics.total_queries,
                        "successful_queries": metrics.successful_queries,
                        "failed_queries": metrics.failed_queries,
                        "average_execution_time_ms": round(metrics.average_execution_time_ms, 2),
                        "last_query_time": metrics.last_query_time.isoformat() if metrics.last_query_time else None
                    }
                    for conn_id, metrics in self._connection_metrics.items()
                }
            }

        except Exception as e:
            logger.error(
                "mysql_global_metrics_calculation_failed",
                error=str(e)
            )
            # Return minimal metrics on error
            return {
                "error": str(e),
                "total_queries": self._global_metrics.get("total_queries", 0),
                "successful_queries": self._global_metrics.get("successful_queries", 0),
                "failed_queries": self._global_metrics.get("failed_queries", 0)
            }

    async def reset_metrics(self, connection_id: Optional[str] = None) -> None:
        """Reset metrics"""
        try:
            if connection_id:
                # Reset specific connection metrics
                if connection_id in self._connection_metrics:
                    self._connection_metrics[connection_id] = ConnectionMetrics(
                        connection_id=connection_id
                    )
                    logger.info(
                        "mysql_connection_metrics_reset",
                        connection_id=connection_id
                    )
                else:
                    logger.warning(
                        "mysql_connection_not_found_for_reset",
                        connection_id=connection_id
                    )
            else:
                # Reset all metrics
                self._connection_metrics.clear()
                self._global_metrics = {
                    "total_connections": 0,
                    "active_connections": 0,
                    "total_queries": 0,
                    "successful_queries": 0,
                    "failed_queries": 0,
                    "total_execution_time_ms": 0.0,
                    "server_start_time": datetime.utcnow()
                }
                logger.info("mysql_all_metrics_reset")

        except Exception as e:
            logger.error(
                "mysql_metrics_reset_failed",
                connection_id=connection_id,
                error=str(e)
            )
            raise MySQLMCPServerError(f"Failed to reset metrics: {str(e)}")

    def increment_connection_count(self):
        """Increment total connection count"""
        self._global_metrics["total_connections"] += 1

    def decrement_connection_count(self):
        """Decrement active connection count when connection is removed"""
        if self._global_metrics["active_connections"] > 0:
            self._global_metrics["active_connections"] -= 1