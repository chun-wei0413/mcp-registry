"""MCP query execution handlers."""

from typing import List, Dict, Any, Optional
import structlog

from ...application.services import QueryService
from ...core.exceptions import QueryError, SecurityError

logger = structlog.get_logger()


class QueryHandler:
    """MCP handler for query execution operations."""

    def __init__(self, query_service: QueryService):
        self._query_service = query_service

    async def execute_query(
        self,
        connection_id: str,
        query: str,
        params: Optional[List[Any]] = None
    ) -> Dict[str, Any]:
        """Handle execute_query MCP tool call."""
        try:
            result = await self._query_service.execute_query(
                connection_id=connection_id,
                sql=query,
                params=params or []
            )

            return {
                "success": result.success,
                "connection_id": connection_id,
                "query_id": result.query_id,
                "columns": result.columns,
                "rows": result.rows,
                "row_count": result.row_count,
                "execution_time_ms": result.execution_time_ms,
                "message": result.message
            }

        except (QueryError, SecurityError) as e:
            logger.error(
                "mcp_execute_query_failed",
                connection_id=connection_id,
                error=str(e),
                error_type=type(e).__name__
            )
            return {
                "success": False,
                "connection_id": connection_id,
                "error": str(e),
                "error_type": type(e).__name__,
                "columns": [],
                "rows": [],
                "row_count": 0,
                "execution_time_ms": 0
            }

        except Exception as e:
            logger.error(
                "mcp_execute_query_unexpected_error",
                connection_id=connection_id,
                error=str(e)
            )
            return {
                "success": False,
                "connection_id": connection_id,
                "error": f"Unexpected error: {e}",
                "error_type": "UnexpectedError",
                "columns": [],
                "rows": [],
                "row_count": 0,
                "execution_time_ms": 0
            }

    async def execute_transaction(
        self,
        connection_id: str,
        queries: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """Handle execute_transaction MCP tool call."""
        try:
            result = await self._query_service.execute_transaction(
                connection_id=connection_id,
                queries=queries
            )

            return {
                "success": result.success,
                "connection_id": connection_id,
                "query_id": result.query_id,
                "transaction_results": result.rows if isinstance(result.rows, list) else [],
                "total_rows_affected": result.row_count,
                "execution_time_ms": result.execution_time_ms,
                "message": result.message
            }

        except (QueryError, SecurityError) as e:
            logger.error(
                "mcp_execute_transaction_failed",
                connection_id=connection_id,
                query_count=len(queries),
                error=str(e),
                error_type=type(e).__name__
            )
            return {
                "success": False,
                "connection_id": connection_id,
                "error": str(e),
                "error_type": type(e).__name__,
                "transaction_results": [],
                "total_rows_affected": 0,
                "execution_time_ms": 0
            }

        except Exception as e:
            logger.error(
                "mcp_execute_transaction_unexpected_error",
                connection_id=connection_id,
                query_count=len(queries),
                error=str(e)
            )
            return {
                "success": False,
                "connection_id": connection_id,
                "error": f"Unexpected error: {e}",
                "error_type": "UnexpectedError",
                "transaction_results": [],
                "total_rows_affected": 0,
                "execution_time_ms": 0
            }

    async def execute_batch(
        self,
        connection_id: str,
        query: str,
        params_list: List[List[Any]]
    ) -> Dict[str, Any]:
        """Handle execute_batch MCP tool call."""
        try:
            result = await self._query_service.execute_batch(
                connection_id=connection_id,
                sql=query,
                params_list=params_list
            )

            return {
                "success": result.success,
                "connection_id": connection_id,
                "query_id": result.query_id,
                "batch_size": len(params_list),
                "rows_affected": result.row_count,
                "execution_time_ms": result.execution_time_ms,
                "message": result.message
            }

        except (QueryError, SecurityError) as e:
            logger.error(
                "mcp_execute_batch_failed",
                connection_id=connection_id,
                batch_size=len(params_list),
                error=str(e),
                error_type=type(e).__name__
            )
            return {
                "success": False,
                "connection_id": connection_id,
                "error": str(e),
                "error_type": type(e).__name__,
                "batch_size": len(params_list),
                "rows_affected": 0,
                "execution_time_ms": 0
            }

        except Exception as e:
            logger.error(
                "mcp_execute_batch_unexpected_error",
                connection_id=connection_id,
                batch_size=len(params_list),
                error=str(e)
            )
            return {
                "success": False,
                "connection_id": connection_id,
                "error": f"Unexpected error: {e}",
                "error_type": "UnexpectedError",
                "batch_size": len(params_list),
                "rows_affected": 0,
                "execution_time_ms": 0
            }