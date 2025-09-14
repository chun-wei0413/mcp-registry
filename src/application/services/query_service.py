"""Query execution application service."""

from typing import List, Any
import structlog

from ...core.interfaces import IQueryExecutor, ISecurityValidator, IMetricsCollector
from ...core.exceptions import QueryError, SecurityError
from ...domain.models import Query, QueryResult
from ...domain.services import QueryBuilder

logger = structlog.get_logger()


class QueryService:
    """Application service for query execution operations."""

    def __init__(
        self,
        query_executor: IQueryExecutor,
        security_validator: ISecurityValidator,
        metrics_collector: IMetricsCollector,
        query_builder: QueryBuilder
    ):
        self._query_executor = query_executor
        self._security_validator = security_validator
        self._metrics_collector = metrics_collector
        self._query_builder = query_builder

    async def execute_query(
        self,
        connection_id: str,
        sql: str,
        params: List[Any] = None
    ) -> QueryResult:
        """Execute a single query with validation."""

        # Build query object
        query = self._query_builder.build_parameterized_query(sql, params or [])
        query.connection_id = connection_id

        # Domain validation
        if not self._query_builder.validate_query_structure(query):
            logger.warning(
                "query_structure_invalid",
                connection_id=connection_id,
                query_id=query.query_id
            )
            raise QueryError("Invalid query structure")

        # Security validation
        if not await self._security_validator.validate_query(query):
            logger.warning(
                "query_security_failed",
                connection_id=connection_id,
                query_id=query.query_id
            )
            raise SecurityError("Query failed security validation")

        # Execute query
        try:
            result = await self._query_executor.execute_query(connection_id, query)

            # Record metrics
            await self._metrics_collector.record_query_execution(
                connection_id,
                result.execution_time_ms,
                result.success
            )

            logger.info(
                "query_completed",
                connection_id=connection_id,
                query_id=query.query_id,
                success=result.success,
                execution_time_ms=result.execution_time_ms
            )

            return result

        except Exception as e:
            logger.error(
                "query_execution_failed",
                connection_id=connection_id,
                query_id=query.query_id,
                error=str(e)
            )
            # Record failed execution
            await self._metrics_collector.record_query_execution(
                connection_id,
                0,
                False
            )
            raise QueryError(f"Query execution failed: {e}")

    async def execute_transaction(
        self,
        connection_id: str,
        queries: List[dict]
    ) -> QueryResult:
        """Execute multiple queries in a transaction."""

        # Build query objects
        query_objects = []
        for query_data in queries:
            sql = query_data.get("query", "")
            params = query_data.get("params", [])
            query = self._query_builder.build_parameterized_query(sql, params)
            query.connection_id = connection_id

            # Validate each query
            if not self._query_builder.validate_query_structure(query):
                raise QueryError(f"Invalid query structure in transaction")

            if not await self._security_validator.validate_query(query):
                raise SecurityError(f"Query failed security validation in transaction")

            query_objects.append(query)

        # Execute transaction
        try:
            result = await self._query_executor.execute_transaction(connection_id, query_objects)

            # Record metrics
            await self._metrics_collector.record_query_execution(
                connection_id,
                result.execution_time_ms,
                result.success
            )

            logger.info(
                "transaction_completed",
                connection_id=connection_id,
                query_count=len(query_objects),
                success=result.success,
                execution_time_ms=result.execution_time_ms
            )

            return result

        except Exception as e:
            logger.error(
                "transaction_failed",
                connection_id=connection_id,
                query_count=len(query_objects),
                error=str(e)
            )
            # Record failed execution
            await self._metrics_collector.record_query_execution(
                connection_id,
                0,
                False
            )
            raise QueryError(f"Transaction execution failed: {e}")

    async def execute_batch(
        self,
        connection_id: str,
        sql: str,
        params_list: List[List[Any]]
    ) -> QueryResult:
        """Execute a query with multiple parameter sets."""

        # Build query object
        query = self._query_builder.build_parameterized_query(sql, [])
        query.connection_id = connection_id

        # Validate query structure
        if not query.sql.strip():
            raise QueryError("Empty query not allowed")

        # Security validation
        if not await self._security_validator.validate_query(query):
            raise SecurityError("Query failed security validation")

        # Execute batch
        try:
            result = await self._query_executor.execute_batch(connection_id, query, params_list)

            # Record metrics
            await self._metrics_collector.record_query_execution(
                connection_id,
                result.execution_time_ms,
                result.success
            )

            logger.info(
                "batch_completed",
                connection_id=connection_id,
                batch_size=len(params_list),
                success=result.success,
                execution_time_ms=result.execution_time_ms
            )

            return result

        except Exception as e:
            logger.error(
                "batch_execution_failed",
                connection_id=connection_id,
                batch_size=len(params_list),
                error=str(e)
            )
            # Record failed execution
            await self._metrics_collector.record_query_execution(
                connection_id,
                0,
                False
            )
            raise QueryError(f"Batch execution failed: {e}")