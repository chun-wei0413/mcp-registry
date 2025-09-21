"""MySQL Connection Pool Implementation"""

import asyncio
from typing import Any, Dict, List, Optional
import aiomysql
import structlog

from ...core.interfaces import IConnectionPool
from ...core.exceptions import ConnectionError

logger = structlog.get_logger()


class MySQLConnectionPool(IConnectionPool):
    """MySQL connection pool implementation using aiomysql"""

    def __init__(self):
        self._pools: Dict[str, aiomysql.Pool] = {}
        self._connection_configs: Dict[str, Dict[str, Any]] = {}
        self._lock = asyncio.Lock()

    async def create_connection(
        self,
        connection_id: str,
        host: str,
        port: int,
        user: str,
        password: str,
        database: str,
        pool_size: int = 10
    ) -> None:
        """Create a new MySQL connection pool"""
        async with self._lock:
            try:
                # Store connection config
                self._connection_configs[connection_id] = {
                    "host": host,
                    "port": port,
                    "user": user,
                    "password": password,
                    "db": database,
                    "minsize": 1,
                    "maxsize": pool_size,
                    "autocommit": False,
                    "charset": "utf8mb4"
                }

                # Create connection pool
                pool = await aiomysql.create_pool(**self._connection_configs[connection_id])
                self._pools[connection_id] = pool

                logger.info(
                    "mysql_connection_pool_created",
                    connection_id=connection_id,
                    host=host,
                    port=port,
                    database=database,
                    pool_size=pool_size
                )

            except Exception as e:
                logger.error(
                    "mysql_connection_pool_creation_failed",
                    connection_id=connection_id,
                    host=host,
                    port=port,
                    database=database,
                    error=str(e)
                )
                raise ConnectionError(f"Failed to create MySQL connection pool: {str(e)}")

    async def get_connection(self, connection_id: str):
        """Get a connection from pool"""
        if connection_id not in self._pools:
            raise ConnectionError(f"Connection pool '{connection_id}' not found")

        try:
            return self._pools[connection_id].acquire()
        except Exception as e:
            logger.error(
                "mysql_connection_acquire_failed",
                connection_id=connection_id,
                error=str(e)
            )
            raise ConnectionError(f"Failed to acquire MySQL connection: {str(e)}")

    async def remove_connection(self, connection_id: str) -> bool:
        """Remove a connection pool"""
        async with self._lock:
            if connection_id not in self._pools:
                return False

            try:
                pool = self._pools[connection_id]
                pool.close()
                await pool.wait_closed()

                del self._pools[connection_id]
                del self._connection_configs[connection_id]

                logger.info(
                    "mysql_connection_pool_removed",
                    connection_id=connection_id
                )
                return True

            except Exception as e:
                logger.error(
                    "mysql_connection_pool_removal_failed",
                    connection_id=connection_id,
                    error=str(e)
                )
                return False

    async def test_connection(self, connection_id: str) -> bool:
        """Test if connection is alive"""
        if connection_id not in self._pools:
            return False

        try:
            async with self._pools[connection_id].acquire() as conn:
                async with conn.cursor() as cursor:
                    await cursor.execute("SELECT 1")
                    result = await cursor.fetchone()
                    return result[0] == 1

        except Exception as e:
            logger.error(
                "mysql_connection_test_failed",
                connection_id=connection_id,
                error=str(e)
            )
            return False

    async def list_connections(self) -> List[str]:
        """List all connection IDs"""
        return list(self._pools.keys())

    async def cleanup(self) -> None:
        """Cleanup all connections"""
        async with self._lock:
            for connection_id in list(self._pools.keys()):
                try:
                    pool = self._pools[connection_id]
                    pool.close()
                    await pool.wait_closed()
                    logger.info(
                        "mysql_connection_pool_cleaned_up",
                        connection_id=connection_id
                    )
                except Exception as e:
                    logger.error(
                        "mysql_connection_pool_cleanup_failed",
                        connection_id=connection_id,
                        error=str(e)
                    )

            self._pools.clear()
            self._connection_configs.clear()
            logger.info("mysql_connection_pools_cleanup_completed")