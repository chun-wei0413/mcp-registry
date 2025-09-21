"""MySQL Query Executor Implementation"""

import time
from typing import Any, Dict, List, Optional
import structlog

from ...core.interfaces import IQueryExecutor, IConnectionPool, IMetricsCollector
from ...core.exceptions import QueryExecutionError, ConnectionError, TransactionError

logger = structlog.get_logger()


class MySQLQueryExecutor(IQueryExecutor):
    """MySQL query executor implementation"""

    def __init__(
        self,
        connection_pool: IConnectionPool,
        metrics_collector: Optional[IMetricsCollector] = None
    ):
        self._connection_pool = connection_pool
        self._metrics_collector = metrics_collector

    async def execute_query(
        self,
        connection_id: str,
        query: str,
        params: Optional[List[Any]] = None
    ) -> Dict[str, Any]:
        """Execute a single MySQL query"""
        start_time = time.time()

        try:
            async with await self._connection_pool.get_connection(connection_id) as conn:
                async with conn.cursor() as cursor:
                    # Execute query with parameters
                    if params:
                        await cursor.execute(query, params)
                    else:
                        await cursor.execute(query)

                    # Handle different query types
                    if query.strip().upper().startswith(('SELECT', 'SHOW', 'DESCRIBE', 'EXPLAIN')):
                        # For SELECT queries, fetch results
                        rows = await cursor.fetchall()
                        # Get column names
                        columns = [desc[0] for desc in cursor.description] if cursor.description else []

                        result = {
                            "success": True,
                            "rows": [dict(zip(columns, row)) for row in rows] if rows else [],
                            "row_count": len(rows) if rows else 0,
                            "columns": columns
                        }
                    else:
                        # For INSERT/UPDATE/DELETE queries
                        await conn.commit()
                        result = {
                            "success": True,
                            "rows_affected": cursor.rowcount,
                            "last_insert_id": cursor.lastrowid if hasattr(cursor, 'lastrowid') else None
                        }

            execution_time = (time.time() - start_time) * 1000

            # Record metrics
            if self._metrics_collector:
                await self._metrics_collector.record_query(
                    connection_id=connection_id,
                    query=query,
                    execution_time_ms=execution_time,
                    success=True
                )

            logger.info(
                "mysql_query_executed_successfully",
                connection_id=connection_id,
                query_preview=query[:100],
                execution_time_ms=execution_time,
                rows_affected=result.get("rows_affected", result.get("row_count", 0))
            )

            return result

        except Exception as e:
            execution_time = (time.time() - start_time) * 1000

            # Record failed metrics
            if self._metrics_collector:
                await self._metrics_collector.record_query(
                    connection_id=connection_id,
                    query=query,
                    execution_time_ms=execution_time,
                    success=False,
                    error_message=str(e)
                )

            logger.error(
                "mysql_query_execution_failed",
                connection_id=connection_id,
                query_preview=query[:100],
                execution_time_ms=execution_time,
                error=str(e)
            )

            return {
                "success": False,
                "error": str(e),
                "error_type": type(e).__name__
            }

    async def execute_transaction(
        self,
        connection_id: str,
        queries: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """Execute multiple queries in a MySQL transaction"""
        start_time = time.time()
        results = []

        try:
            async with await self._connection_pool.get_connection(connection_id) as conn:
                # Start transaction
                await conn.begin()

                try:
                    for i, query_info in enumerate(queries):
                        query = query_info["query"]
                        params = query_info.get("params")

                        async with conn.cursor() as cursor:
                            if params:
                                await cursor.execute(query, params)
                            else:
                                await cursor.execute(query)

                            # Collect result for each query
                            if query.strip().upper().startswith(('SELECT', 'SHOW', 'DESCRIBE')):
                                rows = await cursor.fetchall()
                                columns = [desc[0] for desc in cursor.description] if cursor.description else []
                                results.append({
                                    "query_index": i,
                                    "success": True,
                                    "rows": [dict(zip(columns, row)) for row in rows] if rows else [],
                                    "row_count": len(rows) if rows else 0
                                })
                            else:
                                results.append({
                                    "query_index": i,
                                    "success": True,
                                    "rows_affected": cursor.rowcount,
                                    "last_insert_id": cursor.lastrowid if hasattr(cursor, 'lastrowid') else None
                                })

                    # Commit transaction
                    await conn.commit()

                    execution_time = (time.time() - start_time) * 1000

                    # Record successful transaction
                    if self._metrics_collector:
                        for query_info in queries:
                            await self._metrics_collector.record_query(
                                connection_id=connection_id,
                                query=query_info["query"],
                                execution_time_ms=execution_time / len(queries),
                                success=True
                            )

                    logger.info(
                        "mysql_transaction_executed_successfully",
                        connection_id=connection_id,
                        query_count=len(queries),
                        execution_time_ms=execution_time
                    )

                    return {
                        "success": True,
                        "results": results,
                        "queries_executed": len(queries),
                        "execution_time_ms": execution_time
                    }

                except Exception as e:
                    # Rollback on error
                    await conn.rollback()
                    raise TransactionError(f"Transaction failed and rolled back: {str(e)}")

        except Exception as e:
            execution_time = (time.time() - start_time) * 1000

            # Record failed transaction
            if self._metrics_collector:
                for query_info in queries:
                    await self._metrics_collector.record_query(
                        connection_id=connection_id,
                        query=query_info["query"],
                        execution_time_ms=execution_time / len(queries),
                        success=False,
                        error_message=str(e)
                    )

            logger.error(
                "mysql_transaction_failed",
                connection_id=connection_id,
                query_count=len(queries),
                execution_time_ms=execution_time,
                error=str(e)
            )

            return {
                "success": False,
                "error": str(e),
                "error_type": type(e).__name__,
                "rolled_back": True
            }

    async def execute_batch(
        self,
        connection_id: str,
        query: str,
        params_list: List[List[Any]]
    ) -> Dict[str, Any]:
        """Execute batch operations with same query, different parameters"""
        start_time = time.time()

        try:
            async with await self._connection_pool.get_connection(connection_id) as conn:
                async with conn.cursor() as cursor:
                    # Execute batch
                    await cursor.executemany(query, params_list)
                    await conn.commit()

            execution_time = (time.time() - start_time) * 1000

            # Record metrics
            if self._metrics_collector:
                await self._metrics_collector.record_query(
                    connection_id=connection_id,
                    query=f"BATCH: {query}",
                    execution_time_ms=execution_time,
                    success=True
                )

            logger.info(
                "mysql_batch_executed_successfully",
                connection_id=connection_id,
                query_preview=query[:100],
                batch_size=len(params_list),
                execution_time_ms=execution_time,
                rows_affected=cursor.rowcount
            )

            return {
                "success": True,
                "batch_size": len(params_list),
                "rows_affected": cursor.rowcount,
                "execution_time_ms": execution_time
            }

        except Exception as e:
            execution_time = (time.time() - start_time) * 1000

            # Record failed metrics
            if self._metrics_collector:
                await self._metrics_collector.record_query(
                    connection_id=connection_id,
                    query=f"BATCH: {query}",
                    execution_time_ms=execution_time,
                    success=False,
                    error_message=str(e)
                )

            logger.error(
                "mysql_batch_execution_failed",
                connection_id=connection_id,
                query_preview=query[:100],
                batch_size=len(params_list),
                execution_time_ms=execution_time,
                error=str(e)
            )

            return {
                "success": False,
                "error": str(e),
                "error_type": type(e).__name__
            }