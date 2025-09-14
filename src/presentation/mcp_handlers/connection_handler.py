"""MCP connection management handlers."""

from typing import List, Dict, Any, Optional
import structlog

from ...application.services import ConnectionService
from ...core.exceptions import ConnectionError, SecurityError

logger = structlog.get_logger()


class ConnectionHandler:
    """MCP handler for connection management operations."""

    def __init__(self, connection_service: ConnectionService):
        self._connection_service = connection_service

    async def add_connection(
        self,
        connection_id: str,
        host: str,
        port: int,
        database: str,
        user: str,
        password: str,
        pool_size: int = 10
    ) -> Dict[str, Any]:
        """Handle add_connection MCP tool call."""
        try:
            success = await self._connection_service.create_connection(
                connection_id=connection_id,
                host=host,
                port=port,
                database=database,
                user=user,
                password=password,
                pool_size=pool_size
            )

            return {
                "success": success,
                "connection_id": connection_id,
                "message": f"Connection {connection_id} created successfully" if success else "Failed to create connection"
            }

        except (ConnectionError, SecurityError) as e:
            logger.error(
                "mcp_add_connection_failed",
                connection_id=connection_id,
                error=str(e),
                error_type=type(e).__name__
            )
            return {
                "success": False,
                "connection_id": connection_id,
                "error": str(e),
                "error_type": type(e).__name__
            }

        except Exception as e:
            logger.error(
                "mcp_add_connection_unexpected_error",
                connection_id=connection_id,
                error=str(e)
            )
            return {
                "success": False,
                "connection_id": connection_id,
                "error": f"Unexpected error: {e}",
                "error_type": "UnexpectedError"
            }

    async def test_connection(self, connection_id: str) -> Dict[str, Any]:
        """Handle test_connection MCP tool call."""
        try:
            is_healthy = await self._connection_service.test_connection(connection_id)

            return {
                "connection_id": connection_id,
                "is_healthy": is_healthy,
                "message": "Connection is healthy" if is_healthy else "Connection is unhealthy"
            }

        except Exception as e:
            logger.error(
                "mcp_test_connection_failed",
                connection_id=connection_id,
                error=str(e)
            )
            return {
                "connection_id": connection_id,
                "is_healthy": False,
                "error": str(e),
                "error_type": type(e).__name__
            }

    async def remove_connection(self, connection_id: str) -> Dict[str, Any]:
        """Handle remove_connection MCP tool call."""
        try:
            success = await self._connection_service.remove_connection(connection_id)

            return {
                "success": success,
                "connection_id": connection_id,
                "message": f"Connection {connection_id} removed successfully" if success else "Failed to remove connection"
            }

        except Exception as e:
            logger.error(
                "mcp_remove_connection_failed",
                connection_id=connection_id,
                error=str(e)
            )
            return {
                "success": False,
                "connection_id": connection_id,
                "error": str(e),
                "error_type": type(e).__name__
            }

    async def list_connections(self) -> Dict[str, Any]:
        """Handle list_connections MCP tool call."""
        try:
            connections = await self._connection_service.list_connections()

            connection_list = []
            for conn in connections:
                connection_list.append({
                    "connection_id": conn.connection_id,
                    "host": conn.host,
                    "port": conn.port,
                    "database": conn.database,
                    "user": conn.user,
                    "pool_size": conn.pool_size,
                    "created_at": conn.created_at.isoformat() if conn.created_at else None
                })

            return {
                "success": True,
                "connections": connection_list,
                "count": len(connection_list)
            }

        except Exception as e:
            logger.error("mcp_list_connections_failed", error=str(e))
            return {
                "success": False,
                "connections": [],
                "error": str(e),
                "error_type": type(e).__name__
            }

    async def get_connection(self, connection_id: str) -> Dict[str, Any]:
        """Handle get_connection MCP tool call."""
        try:
            connection = await self._connection_service.get_connection(connection_id)

            if not connection:
                return {
                    "success": False,
                    "connection_id": connection_id,
                    "error": "Connection not found"
                }

            return {
                "success": True,
                "connection": {
                    "connection_id": connection.connection_id,
                    "host": connection.host,
                    "port": connection.port,
                    "database": connection.database,
                    "user": connection.user,
                    "pool_size": connection.pool_size,
                    "created_at": connection.created_at.isoformat() if connection.created_at else None
                }
            }

        except Exception as e:
            logger.error(
                "mcp_get_connection_failed",
                connection_id=connection_id,
                error=str(e)
            )
            return {
                "success": False,
                "connection_id": connection_id,
                "error": str(e),
                "error_type": type(e).__name__
            }