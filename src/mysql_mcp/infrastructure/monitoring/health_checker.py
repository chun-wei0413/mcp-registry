"""MySQL Health Checker Implementation"""

import time
from datetime import datetime
from typing import Dict, Any
import structlog

from ...core.interfaces import IHealthChecker, IConnectionPool, HealthStatus
from ...core.exceptions import ConnectionError

logger = structlog.get_logger()


class MySQLHealthChecker(IHealthChecker):
    """MySQL health checker implementation"""

    def __init__(self, connection_pool: IConnectionPool):
        self._connection_pool = connection_pool

    async def check_connection_health(self, connection_id: str) -> HealthStatus:
        """Check specific MySQL connection health"""
        start_time = time.time()
        details = {}

        try:
            # Test basic connectivity
            is_alive = await self._connection_pool.test_connection(connection_id)
            if not is_alive:
                return HealthStatus(
                    is_healthy=False,
                    response_time_ms=(time.time() - start_time) * 1000,
                    last_check=datetime.utcnow(),
                    details={"error": "Connection test failed"}
                )

            # Get detailed connection info
            async with await self._connection_pool.get_connection(connection_id) as conn:
                async with conn.cursor() as cursor:
                    # Check MySQL version
                    await cursor.execute("SELECT VERSION()")
                    version_result = await cursor.fetchone()
                    details["mysql_version"] = version_result[0] if version_result else "unknown"

                    # Check current database
                    await cursor.execute("SELECT DATABASE()")
                    db_result = await cursor.fetchone()
                    details["current_database"] = db_result[0] if db_result and db_result[0] else "none"

                    # Check connection count
                    await cursor.execute("SHOW STATUS LIKE 'Threads_connected'")
                    conn_result = await cursor.fetchone()
                    details["active_connections"] = int(conn_result[1]) if conn_result else 0

                    # Check uptime
                    await cursor.execute("SHOW STATUS LIKE 'Uptime'")
                    uptime_result = await cursor.fetchone()
                    details["server_uptime_seconds"] = int(uptime_result[1]) if uptime_result else 0

            response_time_ms = (time.time() - start_time) * 1000

            logger.info(
                "mysql_connection_health_check_passed",
                connection_id=connection_id,
                response_time_ms=response_time_ms,
                details=details
            )

            return HealthStatus(
                is_healthy=True,
                response_time_ms=response_time_ms,
                last_check=datetime.utcnow(),
                details=details
            )

        except Exception as e:
            response_time_ms = (time.time() - start_time) * 1000

            logger.error(
                "mysql_connection_health_check_failed",
                connection_id=connection_id,
                response_time_ms=response_time_ms,
                error=str(e)
            )

            return HealthStatus(
                is_healthy=False,
                response_time_ms=response_time_ms,
                last_check=datetime.utcnow(),
                details={"error": str(e), "error_type": type(e).__name__}
            )

    async def check_system_health(self) -> HealthStatus:
        """Check overall MySQL MCP Server system health"""
        start_time = time.time()
        details = {}

        try:
            # Get all connections
            connection_ids = await self._connection_pool.list_connections()
            details["total_connections"] = len(connection_ids)

            # Check each connection
            healthy_connections = 0
            unhealthy_connections = 0
            connection_details = {}

            for connection_id in connection_ids:
                try:
                    conn_health = await self.check_connection_health(connection_id)
                    if conn_health.is_healthy:
                        healthy_connections += 1
                        connection_details[connection_id] = {
                            "status": "healthy",
                            "response_time_ms": conn_health.response_time_ms
                        }
                    else:
                        unhealthy_connections += 1
                        connection_details[connection_id] = {
                            "status": "unhealthy",
                            "error": conn_health.details.get("error", "unknown")
                        }
                except Exception as e:
                    unhealthy_connections += 1
                    connection_details[connection_id] = {
                        "status": "error",
                        "error": str(e)
                    }

            details["healthy_connections"] = healthy_connections
            details["unhealthy_connections"] = unhealthy_connections
            details["connections"] = connection_details

            # System is healthy if at least one connection is healthy
            is_healthy = healthy_connections > 0 or len(connection_ids) == 0

            response_time_ms = (time.time() - start_time) * 1000

            logger.info(
                "mysql_system_health_check_completed",
                is_healthy=is_healthy,
                response_time_ms=response_time_ms,
                healthy_connections=healthy_connections,
                unhealthy_connections=unhealthy_connections
            )

            return HealthStatus(
                is_healthy=is_healthy,
                response_time_ms=response_time_ms,
                last_check=datetime.utcnow(),
                details=details
            )

        except Exception as e:
            response_time_ms = (time.time() - start_time) * 1000

            logger.error(
                "mysql_system_health_check_failed",
                response_time_ms=response_time_ms,
                error=str(e)
            )

            return HealthStatus(
                is_healthy=False,
                response_time_ms=response_time_ms,
                last_check=datetime.utcnow(),
                details={"error": str(e), "error_type": type(e).__name__}
            )