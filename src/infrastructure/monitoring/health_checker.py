"""Health checking implementation."""

from typing import Dict, Any
import time
import structlog
import asyncio

from ...core.interfaces import IHealthChecker, IConnectionManager
from ...core.exceptions import MonitoringError
from ...domain.models import HealthStatus

logger = structlog.get_logger()


class PostgreSQLHealthChecker(IHealthChecker):
    """PostgreSQL health checker implementation."""

    def __init__(
        self,
        connection_manager: IConnectionManager,
        timeout_seconds: int = 30
    ):
        self._connection_manager = connection_manager
        self._timeout_seconds = timeout_seconds

    async def check_connection_health(self, connection_id: str) -> HealthStatus:
        """Check health of a specific connection."""
        start_time = time.time()

        try:
            # Test connection with timeout
            is_healthy = await asyncio.wait_for(
                self._connection_manager.test_connection(connection_id),
                timeout=self._timeout_seconds
            )

            response_time_ms = int((time.time() - start_time) * 1000)

            status = HealthStatus(
                is_healthy=is_healthy,
                response_time_ms=response_time_ms,
                last_check=int(time.time()),
                details={
                    "connection_id": connection_id,
                    "test_duration_ms": response_time_ms
                }
            )

            if is_healthy:
                logger.debug(
                    "connection_health_ok",
                    connection_id=connection_id,
                    response_time_ms=response_time_ms
                )
            else:
                logger.warning(
                    "connection_health_failed",
                    connection_id=connection_id,
                    response_time_ms=response_time_ms
                )

            return status

        except asyncio.TimeoutError:
            response_time_ms = int(self._timeout_seconds * 1000)
            logger.error(
                "connection_health_timeout",
                connection_id=connection_id,
                timeout_seconds=self._timeout_seconds
            )

            return HealthStatus(
                is_healthy=False,
                response_time_ms=response_time_ms,
                last_check=int(time.time()),
                details={
                    "connection_id": connection_id,
                    "error": f"Timeout after {self._timeout_seconds}s"
                }
            )

        except Exception as e:
            response_time_ms = int((time.time() - start_time) * 1000)
            logger.error(
                "connection_health_error",
                connection_id=connection_id,
                error=str(e)
            )

            return HealthStatus(
                is_healthy=False,
                response_time_ms=response_time_ms,
                last_check=int(time.time()),
                details={
                    "connection_id": connection_id,
                    "error": str(e)
                }
            )

    async def check_system_health(self) -> HealthStatus:
        """Check overall system health."""
        start_time = time.time()

        try:
            # Get all connections
            connections = await self._connection_manager.list_connections()

            if not connections:
                return HealthStatus(
                    is_healthy=True,
                    response_time_ms=0,
                    last_check=int(time.time()),
                    details={"connection_count": 0, "message": "No connections configured"}
                )

            # Check all connections
            health_checks = [
                self.check_connection_health(conn.connection_id)
                for conn in connections
            ]

            results = await asyncio.gather(*health_checks, return_exceptions=True)

            healthy_count = 0
            total_count = len(results)
            errors = []

            for i, result in enumerate(results):
                if isinstance(result, Exception):
                    errors.append(f"Connection {connections[i].connection_id}: {str(result)}")
                elif result.is_healthy:
                    healthy_count += 1
                else:
                    errors.append(f"Connection {connections[i].connection_id}: Unhealthy")

            is_healthy = healthy_count == total_count
            response_time_ms = int((time.time() - start_time) * 1000)

            details = {
                "total_connections": total_count,
                "healthy_connections": healthy_count,
                "check_duration_ms": response_time_ms
            }

            if errors:
                details["errors"] = errors

            logger.info(
                "system_health_check",
                is_healthy=is_healthy,
                healthy_connections=healthy_count,
                total_connections=total_count,
                response_time_ms=response_time_ms
            )

            return HealthStatus(
                is_healthy=is_healthy,
                response_time_ms=response_time_ms,
                last_check=int(time.time()),
                details=details
            )

        except Exception as e:
            response_time_ms = int((time.time() - start_time) * 1000)
            logger.error(
                "system_health_error",
                error=str(e),
                response_time_ms=response_time_ms
            )

            raise MonitoringError(f"System health check failed: {e}")