#!/usr/bin/env python3
"""MySQL MCP Server - Main Entry Point for MySQL Database Operations"""

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

from .core.interfaces import (
    IConnectionPool,
    IQueryExecutor,
    ISchemaInspector,
    IHealthChecker,
    IMetricsCollector
)
from .infrastructure.database.connection_pool import MySQLConnectionPool
from .infrastructure.database.query_executor import MySQLQueryExecutor
from .infrastructure.schema.schema_inspector import MySQLSchemaInspector
from .infrastructure.monitoring.health_checker import MySQLHealthChecker
from .infrastructure.monitoring.metrics_collector import MySQLMetricsCollector

logger = structlog.get_logger()


class MySQLMCPServer:
    """MySQL MCP Server implementation for MySQL database operations"""

    def __init__(self, config_file: Optional[str] = None):
        self.app = FastMCP("MySQL MCP Server", stateless_http=True)

        # Initialize dependencies
        self._connection_pool: IConnectionPool = MySQLConnectionPool()
        self._metrics_collector: IMetricsCollector = MySQLMetricsCollector()
        self._query_executor: IQueryExecutor = MySQLQueryExecutor(
            self._connection_pool, self._metrics_collector
        )
        self._schema_inspector: ISchemaInspector = MySQLSchemaInspector(self._connection_pool)
        self._health_checker: IHealthChecker = MySQLHealthChecker(self._connection_pool)

    async def initialize(self):
        """Initialize the MySQL MCP server"""
        try:
            # Register tools and resources
            self._register_tools()
            self._register_resources()

            logger.info("mysql_mcp_server_initialized")

        except Exception as e:
            logger.error("mysql_server_initialization_failed", error=str(e))
            raise

    def _register_tools(self):
        """Register all MCP tools for MySQL operations"""

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
            """建立 MySQL 資料庫連線"""
            try:
                await self._connection_pool.create_connection(
                    connection_id=connection_id,
                    host=host,
                    port=port,
                    user=user,
                    password=password,
                    database=database,
                    pool_size=pool_size
                )

                self._metrics_collector.increment_connection_count()

                return {
                    "success": True,
                    "connection_id": connection_id,
                    "host": host,
                    "port": port,
                    "database": database,
                    "pool_size": pool_size
                }

            except Exception as e:
                logger.error(
                    "mysql_add_connection_failed",
                    connection_id=connection_id,
                    host=host,
                    port=port,
                    database=database,
                    error=str(e)
                )
                return {
                    "success": False,
                    "error": str(e),
                    "error_type": type(e).__name__
                }

        @self.app.tool()
        async def test_connection(connection_id: str):
            """測試 MySQL 連線狀態"""
            try:
                is_healthy = await self._connection_pool.test_connection(connection_id)
                return {
                    "success": True,
                    "connection_id": connection_id,
                    "is_healthy": is_healthy
                }

            except Exception as e:
                return {
                    "success": False,
                    "connection_id": connection_id,
                    "error": str(e),
                    "error_type": type(e).__name__
                }

        @self.app.tool()
        async def remove_connection(connection_id: str):
            """移除 MySQL 資料庫連線"""
            try:
                removed = await self._connection_pool.remove_connection(connection_id)
                if removed:
                    self._metrics_collector.decrement_connection_count()

                return {
                    "success": removed,
                    "connection_id": connection_id
                }

            except Exception as e:
                return {
                    "success": False,
                    "connection_id": connection_id,
                    "error": str(e)
                }

        @self.app.tool()
        async def list_connections():
            """列出所有 MySQL 連線"""
            try:
                connections = await self._connection_pool.list_connections()
                return {
                    "success": True,
                    "connections": connections,
                    "count": len(connections)
                }

            except Exception as e:
                return {
                    "success": False,
                    "error": str(e)
                }

        # Query Execution Tools
        @self.app.tool()
        async def execute_query(
            connection_id: str,
            query: str,
            params: Optional[List[Any]] = None
        ):
            """執行 MySQL 查詢"""
            return await self._query_executor.execute_query(
                connection_id=connection_id,
                query=query,
                params=params
            )

        @self.app.tool()
        async def execute_transaction(
            connection_id: str,
            queries: List[Dict[str, Any]]
        ):
            """在事務中執行多個 MySQL 查詢"""
            return await self._query_executor.execute_transaction(
                connection_id=connection_id,
                queries=queries
            )

        @self.app.tool()
        async def execute_batch(
            connection_id: str,
            query: str,
            params_list: List[List[Any]]
        ):
            """批次執行相同 MySQL 查詢，不同參數"""
            return await self._query_executor.execute_batch(
                connection_id=connection_id,
                query=query,
                params_list=params_list
            )

        # Schema Inspection Tools
        @self.app.tool()
        async def get_table_schema(
            connection_id: str,
            table_name: str,
            schema_name: Optional[str] = None
        ):
            """獲取 MySQL 表結構詳情"""
            try:
                # Use current database if no schema specified
                if not schema_name:
                    async with await self._connection_pool.get_connection(connection_id) as conn:
                        async with conn.cursor() as cursor:
                            await cursor.execute("SELECT DATABASE()")
                            result = await cursor.fetchone()
                            schema_name = result[0] if result and result[0] else "mysql"

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
                    "mysql_get_table_schema_failed",
                    connection_id=connection_id,
                    table_name=table_name,
                    schema_name=schema_name,
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
            schema_name: Optional[str] = None
        ):
            """列出 MySQL 資料庫中的所有表"""
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
                    "mysql_list_tables_failed",
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
            """列出所有 MySQL schemas/databases"""
            try:
                schemas = await self._schema_inspector.list_schemas(connection_id)

                return {
                    "success": True,
                    "schemas": schemas,
                    "count": len(schemas)
                }

            except Exception as e:
                logger.error(
                    "mysql_list_schemas_failed",
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
            """MySQL 健康檢查"""
            try:
                if connection_id:
                    health_status = await self._health_checker.check_connection_health(connection_id)
                else:
                    health_status = await self._health_checker.check_system_health()

                return {
                    "is_healthy": health_status.is_healthy,
                    "response_time_ms": health_status.response_time_ms,
                    "last_check": health_status.last_check.isoformat(),
                    "details": health_status.details
                }

            except Exception as e:
                logger.error(
                    "mysql_health_check_failed",
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
            """取得 MySQL 伺服器指標"""
            try:
                if connection_id:
                    metrics = await self._metrics_collector.get_connection_metrics(connection_id)
                    return {
                        "connection_id": metrics.connection_id,
                        "total_queries": metrics.total_queries,
                        "successful_queries": metrics.successful_queries,
                        "failed_queries": metrics.failed_queries,
                        "average_execution_time_ms": metrics.average_execution_time_ms,
                        "last_query_time": metrics.last_query_time.isoformat() if metrics.last_query_time else None,
                        "query_history": metrics.query_history[-50:]  # Last 50 queries
                    }
                else:
                    return await self._metrics_collector.get_global_metrics()

            except Exception as e:
                logger.error(
                    "mysql_get_metrics_failed",
                    connection_id=connection_id,
                    error=str(e)
                )
                return {
                    "error": str(e),
                    "error_type": type(e).__name__
                }

        @self.app.tool()
        async def reset_metrics(connection_id: Optional[str] = None):
            """重置 MySQL 指標"""
            try:
                await self._metrics_collector.reset_metrics(connection_id)
                return {
                    "success": True,
                    "message": f"Metrics reset for {'all connections' if not connection_id else connection_id}"
                }

            except Exception as e:
                logger.error(
                    "mysql_reset_metrics_failed",
                    connection_id=connection_id,
                    error=str(e)
                )
                return {
                    "success": False,
                    "error": str(e),
                    "error_type": type(e).__name__
                }

    def _register_resources(self):
        """Register all MCP resources for MySQL"""

        @self.app.resource("mysql://connections")
        async def get_connections():
            """返回所有活躍的 MySQL 連線"""
            try:
                connections = await self._connection_pool.list_connections()
                return [
                    Resource(
                        uri=f"mysql://connection/{conn_id}",
                        name=f"MySQL Connection: {conn_id}",
                        description=f"MySQL connection {conn_id}",
                        mimeType="application/json"
                    )
                    for conn_id in connections
                ]

            except Exception as e:
                logger.error("mysql_get_connections_resource_failed", error=str(e))
                return []

        @self.app.resource("mysql://health")
        async def get_health_resource():
            """MySQL 健康狀態資源"""
            try:
                health_status = await self._health_checker.check_system_health()
                status_text = "healthy" if health_status.is_healthy else "unhealthy"

                return [
                    Resource(
                        uri="mysql://health/status",
                        name="MySQL Health Status",
                        description=f"MySQL server health: {status_text}",
                        mimeType="application/json"
                    )
                ]

            except Exception as e:
                logger.error("mysql_get_health_resource_failed", error=str(e))
                return [
                    Resource(
                        uri="mysql://health/status",
                        name="MySQL Health Status",
                        description="Health check failed",
                        mimeType="application/json"
                    )
                ]

    async def run(self):
        """Run the MySQL MCP server"""
        try:
            await self.initialize()
            self.app.run()

        except Exception as e:
            logger.error("mysql_server_run_failed", error=str(e))
            await self.cleanup()
            raise

    async def run_http(self, host="0.0.0.0", port=3001, path="/mcp"):
        """Run the MySQL MCP server with HTTP transport"""
        try:
            await self.initialize()
            logger.info(
                "starting_mysql_http_server",
                host=host,
                port=port,
                path=path
            )
            self.app.run(transport="streamable-http")

        except Exception as e:
            logger.error("mysql_server_http_run_failed", error=str(e))
            await self.cleanup()
            raise

    def run_sync_http(self, host="0.0.0.0", port=3001):
        """Run the MySQL MCP server with HTTP transport synchronously"""
        import asyncio
        import uvicorn

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            logger.info("initializing_mysql_server_for_http_transport")
            loop.run_until_complete(self.initialize())

            starlette_app = self.app.streamable_http_app()

            logger.info(
                "starting_mysql_streamable_http_server",
                host=host,
                port=port,
                endpoint="/mcp"
            )

            uvicorn.run(
                starlette_app,
                host=host,
                port=port,
                log_level="info"
            )

        except Exception as e:
            logger.error("mysql_server_http_sync_run_failed", error=str(e))
            loop.run_until_complete(self.cleanup())
            raise
        finally:
            loop.close()

    async def cleanup(self):
        """Cleanup MySQL server resources"""
        try:
            await self._connection_pool.cleanup()
            logger.info("mysql_mcp_server_cleaned_up")

        except Exception as e:
            logger.error("mysql_server_cleanup_failed", error=str(e))


async def main():
    """Main entry point for MySQL MCP Server"""
    server = MySQLMCPServer()
    try:
        await server.run()
    except KeyboardInterrupt:
        logger.info("mysql_server_interrupted")
        await server.cleanup()
    except Exception as e:
        logger.error("mysql_server_main_failed", error=str(e))
        await server.cleanup()
        sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())