#!/usr/bin/env python3
"""PostgreSQL MCP Server - Main Entry Point with SOLID Architecture"""

import asyncio
import sys
from typing import Any, Dict, List, Optional

import structlog
from mcp.server.fastmcp import FastMCP
from mcp.server import NotificationOptions
from mcp.types import (
    Resource,
    Tool,
    TextContent,
    EmbeddedResource,
    LoggingLevel
)

from .presentation import get_dependency_factory, cleanup_dependency_factory
from .presentation.mcp_handlers import ConnectionHandler, QueryHandler
from .core.interfaces import IHealthChecker, IMetricsCollector, ISchemaInspector

logger = structlog.get_logger()


class PostgreSQLMCPServer:
    """PostgreSQL MCP Server implementation with SOLID architecture."""

    def __init__(self, config_file: Optional[str] = None):
        # Use stateless_http=True for better session management with Claude Code
        self.app = FastMCP("PostgreSQL MCP Server", stateless_http=True)
        self._dependency_factory = None
        self._connection_handler: ConnectionHandler = None
        self._query_handler: QueryHandler = None
        self._health_checker: IHealthChecker = None
        self._metrics_collector: IMetricsCollector = None
        self._schema_inspector: ISchemaInspector = None

    async def initialize(self):
        """Initialize the server with dependency injection."""
        try:
            # Get dependency factory
            self._dependency_factory = await get_dependency_factory()

            # Initialize handlers
            self._connection_handler = await self._dependency_factory.get_connection_handler()
            self._query_handler = await self._dependency_factory.get_query_handler()

            # Initialize monitoring components
            self._health_checker = await self._dependency_factory.get_health_checker()
            self._metrics_collector = await self._dependency_factory.get_metrics_collector()
            self._schema_inspector = await self._dependency_factory.get_schema_inspector()

            # Register tools and resources
            self._register_tools()
            self._register_resources()

            logger.info("postgresql_mcp_server_initialized")

        except Exception as e:
            logger.error("server_initialization_failed", error=str(e))
            raise

    def _register_tools(self):
        """Register all MCP tools using SOLID architecture handlers."""

        # Connection Management Tools
        @self.app.tool()
        async def add_connection(
            connection_id: str,
            host: str,
            port: int,
            database: str,
            user: str,
            password: str,
            pool_size: int = 10
        ):
            """建立資料庫連線"""
            return await self._connection_handler.add_connection(
                connection_id=connection_id,
                host=host,
                port=port,
                database=database,
                user=user,
                password=password,
                pool_size=pool_size
            )

        @self.app.tool()
        async def test_connection(connection_id: str):
            """測試連線狀態"""
            return await self._connection_handler.test_connection(connection_id)

        @self.app.tool()
        async def remove_connection(connection_id: str):
            """移除資料庫連線"""
            return await self._connection_handler.remove_connection(connection_id)

        @self.app.tool()
        async def list_connections():
            """列出所有連線"""
            return await self._connection_handler.list_connections()

        @self.app.tool()
        async def get_connection(connection_id: str):
            """取得連線資訊"""
            return await self._connection_handler.get_connection(connection_id)

        # Query Execution Tools
        @self.app.tool()
        async def execute_query(
            connection_id: str,
            query: str,
            params: Optional[List[Any]] = None
        ):
            """執行 SQL 查詢"""
            return await self._query_handler.execute_query(
                connection_id=connection_id,
                query=query,
                params=params
            )

        @self.app.tool()
        async def execute_transaction(
            connection_id: str,
            queries: List[Dict[str, Any]]
        ):
            """在事務中執行多個查詢"""
            return await self._query_handler.execute_transaction(
                connection_id=connection_id,
                queries=queries
            )

        @self.app.tool()
        async def execute_batch(
            connection_id: str,
            query: str,
            params_list: List[List[Any]]
        ):
            """批次執行相同查詢，不同參數"""
            return await self._query_handler.execute_batch(
                connection_id=connection_id,
                query=query,
                params_list=params_list
            )

        # Schema Inspection Tools
        @self.app.tool()
        async def get_table_schema(
            connection_id: str,
            table_name: str,
            schema_name: str = "public"
        ):
            """獲取表結構詳情"""
            try:
                schema = await self._schema_inspector.get_table_schema(
                    connection_id=connection_id,
                    table_name=table_name,
                    schema_name=schema_name
                )

                return {
                    "success": True,
                    "table_name": schema.table_name,
                    "schema_name": schema.schema_name,
                    "columns": [
                        {
                            "name": col.name,
                            "data_type": col.data_type,
                            "is_nullable": col.is_nullable,
                            "default_value": col.default_value,
                            "max_length": col.max_length,
                            "precision": col.precision,
                            "scale": col.scale,
                            "comment": col.comment
                        }
                        for col in schema.columns
                    ],
                    "indexes": [
                        {
                            "name": idx.name,
                            "definition": idx.definition,
                            "is_unique": idx.is_unique,
                            "is_primary": idx.is_primary
                        }
                        for idx in schema.indexes
                    ],
                    "constraints": [
                        {
                            "name": const.name,
                            "type": const.type,
                            "column_name": const.column_name,
                            "foreign_table": const.foreign_table,
                            "foreign_column": const.foreign_column
                        }
                        for const in schema.constraints
                    ],
                    "row_count": schema.row_count,
                    "table_size_bytes": schema.table_size_bytes
                }

            except Exception as e:
                logger.error(
                    "get_table_schema_tool_failed",
                    connection_id=connection_id,
                    table_name=f"{schema_name}.{table_name}",
                    error=str(e)
                )
                return {
                    "success": False,
                    "error": str(e),
                    "error_type": type(e).__name__
                }

        @self.app.tool()
        async def list_tables(
            connection_id: str,
            schema_name: str = "public"
        ):
            """列出所有表"""
            try:
                tables = await self._schema_inspector.list_tables(
                    connection_id=connection_id,
                    schema_name=schema_name
                )

                return {
                    "success": True,
                    "schema_name": schema_name,
                    "tables": tables,
                    "count": len(tables)
                }

            except Exception as e:
                logger.error(
                    "list_tables_tool_failed",
                    connection_id=connection_id,
                    schema_name=schema_name,
                    error=str(e)
                )
                return {
                    "success": False,
                    "error": str(e),
                    "error_type": type(e).__name__
                }

        @self.app.tool()
        async def list_schemas(connection_id: str):
            """列出所有 schemas"""
            try:
                schemas = await self._schema_inspector.list_schemas(connection_id)

                return {
                    "success": True,
                    "schemas": schemas,
                    "count": len(schemas)
                }

            except Exception as e:
                logger.error(
                    "list_schemas_tool_failed",
                    connection_id=connection_id,
                    error=str(e)
                )
                return {
                    "success": False,
                    "error": str(e),
                    "error_type": type(e).__name__
                }

        # Monitoring Tools
        @self.app.tool()
        async def health_check(connection_id: Optional[str] = None):
            """健康檢查"""
            try:
                if connection_id:
                    # Check specific connection
                    health_status = await self._health_checker.check_connection_health(connection_id)
                else:
                    # Check system health
                    health_status = await self._health_checker.check_system_health()

                return {
                    "is_healthy": health_status.is_healthy,
                    "response_time_ms": health_status.response_time_ms,
                    "last_check": health_status.last_check,
                    "details": health_status.details
                }

            except Exception as e:
                logger.error(
                    "health_check_tool_failed",
                    connection_id=connection_id,
                    error=str(e)
                )
                return {
                    "is_healthy": False,
                    "error": str(e),
                    "error_type": type(e).__name__
                }

        @self.app.tool()
        async def get_metrics(connection_id: Optional[str] = None):
            """取得伺服器指標"""
            try:
                if connection_id:
                    # Get connection-specific metrics
                    metrics = await self._metrics_collector.get_connection_metrics(connection_id)
                    return {
                        "connection_id": metrics.connection_id,
                        "total_queries": metrics.total_queries,
                        "successful_queries": metrics.successful_queries,
                        "failed_queries": metrics.failed_queries,
                        "average_execution_time_ms": metrics.average_execution_time_ms,
                        "last_query_time": metrics.last_query_time,
                        "query_history": metrics.query_history[-100:]  # Last 100 queries
                    }
                else:
                    # Get global metrics
                    global_metrics = await self._metrics_collector.get_global_metrics()
                    return global_metrics

            except Exception as e:
                logger.error(
                    "get_metrics_tool_failed",
                    connection_id=connection_id,
                    error=str(e)
                )
                return {
                    "error": str(e),
                    "error_type": type(e).__name__
                }

        @self.app.tool()
        async def reset_metrics(connection_id: Optional[str] = None):
            """重置指標"""
            try:
                await self._metrics_collector.reset_metrics(connection_id)
                return {
                    "success": True,
                    "message": f"Metrics reset for {'all connections' if not connection_id else connection_id}"
                }

            except Exception as e:
                logger.error(
                    "reset_metrics_tool_failed",
                    connection_id=connection_id,
                    error=str(e)
                )
                return {
                    "success": False,
                    "error": str(e),
                    "error_type": type(e).__name__
                }

    def _register_resources(self):
        """Register all MCP resources using SOLID architecture."""

        @self.app.resource("postgresql://connections")
        async def get_connections():
            """返回所有活躍連線"""
            try:
                connections_result = await self._connection_handler.list_connections()

                if connections_result.get("success"):
                    return [
                        Resource(
                            uri=f"postgresql://connection/{conn['connection_id']}",
                            name=f"Connection: {conn['connection_id']}",
                            description=f"PostgreSQL connection to {conn['host']}:{conn['port']}/{conn['database']}",
                            mimeType="application/json"
                        )
                        for conn in connections_result.get("connections", [])
                    ]
                else:
                    return []

            except Exception as e:
                logger.error("get_connections_resource_failed", error=str(e))
                return []

        @self.app.resource("postgresql://health")
        async def get_health_resource():
            """健康狀態資源"""
            try:
                health_status = await self._health_checker.check_system_health()
                status_text = "healthy" if health_status.is_healthy else "unhealthy"

                return [
                    Resource(
                        uri="postgresql://health/status",
                        name="Health Status",
                        description=f"Server health: {status_text}",
                        mimeType="application/json"
                    )
                ]

            except Exception as e:
                logger.error("get_health_resource_failed", error=str(e))
                return [
                    Resource(
                        uri="postgresql://health/status",
                        name="Health Status",
                        description="Health check failed",
                        mimeType="application/json"
                    )
                ]

        @self.app.resource("postgresql://metrics")
        async def get_metrics_resource():
            """指標資源"""
            try:
                metrics = await self._metrics_collector.get_global_metrics()

                return [
                    Resource(
                        uri="postgresql://metrics/global",
                        name="Global Metrics",
                        description=f"Total queries: {metrics.get('total_queries', 0)}, Success rate: {metrics.get('success_rate_percent', 0)}%",
                        mimeType="application/json"
                    )
                ]

            except Exception as e:
                logger.error("get_metrics_resource_failed", error=str(e))
                return []

    async def run(self):
        """Run the MCP server."""
        try:
            await self.initialize()
            self.app.run()

        except Exception as e:
            logger.error("server_run_failed", error=str(e))
            await self.cleanup()
            raise

    async def run_http(self, host="0.0.0.0", port=3000, path="/mcp"):
        """Run the MCP server with HTTP transport."""
        try:
            await self.initialize()
            logger.info(
                "starting_http_server",
                host=host,
                port=port,
                path=path
            )
            # Use streamable-http transport for Claude Code compatibility
            # FastMCP run() method only accepts transport parameter
            self.app.run(transport="streamable-http")

        except Exception as e:
            logger.error("server_http_run_failed", error=str(e))
            await self.cleanup()
            raise

    def run_sync(self):
        """Run the MCP server synchronously for Docker."""
        import asyncio
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            loop.run_until_complete(self.initialize())
            self.app.run()
        except Exception as e:
            logger.error("server_run_failed", error=str(e))
            loop.run_until_complete(self.cleanup())
            raise
        finally:
            loop.close()

    def run_sync_http(self, host="0.0.0.0", port=3000):
        """Run the MCP server with HTTP transport synchronously for Docker."""
        import asyncio
        import uvicorn

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            logger.info("initializing_server_for_http_transport")
            loop.run_until_complete(self.initialize())

            # Get the Starlette app directly instead of using self.app.run()
            starlette_app = self.app.streamable_http_app()

            logger.info(
                "starting_streamable_http_server",
                host=host,
                port=port,
                endpoint="/mcp"
            )

            # Use uvicorn to run the Starlette app with custom host/port
            uvicorn.run(
                starlette_app,
                host=host,
                port=port,
                log_level="info"
            )

        except Exception as e:
            logger.error("server_http_sync_run_failed", error=str(e))
            loop.run_until_complete(self.cleanup())
            raise
        finally:
            loop.close()

    async def cleanup(self):
        """Cleanup server resources."""
        try:
            if self._dependency_factory:
                await cleanup_dependency_factory()

            logger.info("postgresql_mcp_server_cleaned_up")

        except Exception as e:
            logger.error("server_cleanup_failed", error=str(e))


async def main():
    """Main entry point"""
    server = PostgreSQLMCPServer()
    try:
        await server.run()
    except KeyboardInterrupt:
        logger.info("server_interrupted")
        await server.cleanup()
    except Exception as e:
        logger.error("server_main_failed", error=str(e))
        await server.cleanup()
        sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())