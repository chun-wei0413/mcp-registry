"""PostgreSQL connection pool implementation."""

import asyncio
from typing import Optional, Dict, Any
import asyncpg
import structlog

from ...core.interfaces import IConnectionManager
from ...core.exceptions import ConnectionError
from ...domain.models import Connection

logger = structlog.get_logger()


class PostgreSQLConnectionPool(IConnectionManager):
    """PostgreSQL connection pool implementation using asyncpg."""

    def __init__(self):
        self._pools: Dict[str, asyncpg.Pool] = {}
        self._connections: Dict[str, Connection] = {}

    async def add_connection(self, connection: Connection) -> bool:
        """Add a new database connection with pool."""
        try:
            pool = await asyncpg.create_pool(
                host=connection.host,
                port=connection.port,
                database=connection.database,
                user=connection.user,
                password=connection.encrypted_password,  # Assume decrypted
                min_size=1,
                max_size=connection.pool_size,
                server_settings={
                    'application_name': 'PostgreSQL MCP Server',
                    'timezone': 'UTC'
                }
            )

            self._pools[connection.connection_id] = pool
            self._connections[connection.connection_id] = connection

            logger.info(
                "connection_added",
                connection_id=connection.connection_id,
                host=connection.host,
                database=connection.database
            )

            return True

        except Exception as e:
            logger.error(
                "connection_failed",
                connection_id=connection.connection_id,
                error=str(e)
            )
            raise ConnectionError(f"Failed to create connection: {e}")

    async def remove_connection(self, connection_id: str) -> bool:
        """Remove and close a database connection."""
        if connection_id not in self._pools:
            return False

        try:
            pool = self._pools[connection_id]
            await pool.close()

            del self._pools[connection_id]
            del self._connections[connection_id]

            logger.info("connection_removed", connection_id=connection_id)
            return True

        except Exception as e:
            logger.error("connection_removal_failed", connection_id=connection_id, error=str(e))
            return False

    async def test_connection(self, connection_id: str) -> bool:
        """Test if a connection is healthy."""
        if connection_id not in self._pools:
            return False

        try:
            pool = self._pools[connection_id]
            async with pool.acquire() as conn:
                await conn.fetchval("SELECT 1")
            return True

        except Exception:
            return False

    async def get_connection(self, connection_id: str) -> Optional[Connection]:
        """Get connection information."""
        return self._connections.get(connection_id)

    async def list_connections(self) -> list[Connection]:
        """List all active connections."""
        return list(self._connections.values())

    async def get_pool(self, connection_id: str) -> Optional[asyncpg.Pool]:
        """Get the asyncpg pool for a connection."""
        return self._pools.get(connection_id)

    async def cleanup(self):
        """Clean up all connections."""
        for connection_id in list(self._pools.keys()):
            await self.remove_connection(connection_id)