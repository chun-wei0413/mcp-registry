"""SQL query execution implementation."""

import time
from typing import List, Any, Dict
import structlog

from ...core.interfaces import IQueryExecutor
from ...core.exceptions import QueryError, TimeoutError
from ...domain.models import Query, QueryResult
from .connection_pool import PostgreSQLConnectionPool

logger = structlog.get_logger()


class PostgreSQLQueryExecutor(IQueryExecutor):
    """PostgreSQL query executor implementation."""

    def __init__(self, connection_manager: PostgreSQLConnectionPool, query_timeout: int = 30):
        self._connection_manager = connection_manager
        self._query_timeout = query_timeout

    async def execute_query(self, connection_id: str, query: Query) -> QueryResult:
        """Execute a single SQL query."""
        start_time = time.time()

        try:
            pool = await self._connection_manager.get_pool(connection_id)
            if not pool:
                raise QueryError(f"Connection not found: {connection_id}")

            async with pool.acquire() as conn:
                if query.params:
                    result = await conn.fetch(query.sql, *query.params)
                else:
                    result = await conn.fetch(query.sql)

                rows = [dict(row) for row in result]
                execution_time = int((time.time() - start_time) * 1000)

                logger.info(
                    "query_executed",
                    connection_id=connection_id,
                    query_id=query.query_id,
                    execution_time_ms=execution_time,
                    row_count=len(rows)
                )

                return QueryResult(
                    success=True,
                    rows=rows,
                    row_count=len(rows),
                    execution_time_ms=execution_time,
                    query_id=query.query_id
                )

        except Exception as e:
            execution_time = int((time.time() - start_time) * 1000)

            logger.error(
                "query_failed",
                connection_id=connection_id,
                query_id=query.query_id,
                error=str(e),
                execution_time_ms=execution_time
            )

            return QueryResult(
                success=False,
                execution_time_ms=execution_time,
                query_id=query.query_id,
                message=str(e)
            )

    async def execute_transaction(self, connection_id: str, queries: List[Query]) -> QueryResult:
        """Execute multiple queries in a transaction."""
        start_time = time.time()
        total_affected = 0

        try:
            pool = await self._connection_manager.get_pool(connection_id)
            if not pool:
                raise QueryError(f"Connection not found: {connection_id}")

            async with pool.acquire() as conn:
                async with conn.transaction():
                    for query in queries:
                        if query.params:
                            await conn.execute(query.sql, *query.params)
                        else:
                            await conn.execute(query.sql)
                        total_affected += 1

                execution_time = int((time.time() - start_time) * 1000)

                logger.info(
                    "transaction_executed",
                    connection_id=connection_id,
                    query_count=len(queries),
                    execution_time_ms=execution_time,
                    affected_rows=total_affected
                )

                return QueryResult(
                    success=True,
                    row_count=total_affected,
                    execution_time_ms=execution_time
                )

        except Exception as e:
            execution_time = int((time.time() - start_time) * 1000)

            logger.error(
                "transaction_failed",
                connection_id=connection_id,
                query_count=len(queries),
                error=str(e),
                execution_time_ms=execution_time
            )

            return QueryResult(
                success=False,
                execution_time_ms=execution_time,
                message=str(e)
            )

    async def execute_batch(self, connection_id: str, query: Query, params_list: List[List[Any]]) -> QueryResult:
        """Execute a query with multiple parameter sets."""
        start_time = time.time()

        try:
            pool = await self._connection_manager.get_pool(connection_id)
            if not pool:
                raise QueryError(f"Connection not found: {connection_id}")

            async with pool.acquire() as conn:
                await conn.executemany(query.sql, params_list)

                execution_time = int((time.time() - start_time) * 1000)

                logger.info(
                    "batch_executed",
                    connection_id=connection_id,
                    batch_size=len(params_list),
                    execution_time_ms=execution_time
                )

                return QueryResult(
                    success=True,
                    row_count=len(params_list),
                    execution_time_ms=execution_time,
                    query_id=query.query_id
                )

        except Exception as e:
            execution_time = int((time.time() - start_time) * 1000)

            logger.error(
                "batch_failed",
                connection_id=connection_id,
                batch_size=len(params_list),
                error=str(e),
                execution_time_ms=execution_time
            )

            return QueryResult(
                success=False,
                execution_time_ms=execution_time,
                query_id=query.query_id,
                message=str(e)
            )