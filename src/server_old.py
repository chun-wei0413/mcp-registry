#!/usr/bin/env python3
"""PostgreSQL MCP Server - Main Entry Point"""

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

from .tools.connection import ConnectionManager
from .tools.query import QueryTool
from .tools.schema import SchemaTool
from .models.types import ConnectionInfo, QueryHistory, SecurityConfig
from .config import ConfigManager
from .monitoring import HealthChecker, MetricsCollector

logger = structlog.get_logger()

class PostgreSQLMCPServer:
    """PostgreSQL MCP Server implementation"""

    def __init__(self, config_file: Optional[str] = None):
        # 載入配置
        self.config_manager = ConfigManager(config_file)
        self.config_manager.setup_logging()

        # 驗證配置
        if not self.config_manager.validate_config():
            raise ValueError("Invalid configuration")

        self.config_manager.print_config_summary()

        self.app = FastMCP("PostgreSQL MCP Server")
        self.server_config = self.config_manager.get_server_config()
        self.security_config = self.config_manager.get_security_config()
        self.connection_manager = ConnectionManager(self.security_config)
        self.query_tool = QueryTool(self.connection_manager, self.security_config)
        self.schema_tool = SchemaTool(self.connection_manager)
        self.query_history: List[QueryHistory] = []

        # 初始化監控
        self.health_checker = HealthChecker(self.connection_manager)
        self.metrics_collector = MetricsCollector(self.connection_manager)

        # Register tools and resources
        self._register_tools()
        self._register_resources()
        
    def _register_tools(self):
        """Register all MCP tools"""
        
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
            return await self.connection_manager.add_connection(
                connection_id, host, port, database, user, password, pool_size
            )
        
        @self.app.tool()
        async def test_connection(connection_id: str):
            """測試連線狀態"""
            return await self.connection_manager.test_connection(connection_id)
        
        @self.app.tool()
        async def execute_query(
            connection_id: str,
            query: str,
            params: Optional[List[Any]] = None,
            fetch_size: Optional[int] = None
        ):
            """執行 SELECT 查詢"""
            result = await self.query_tool.execute_query(
                connection_id, query, params, fetch_size
            )

            # 記錄查詢歷史
            self._add_query_history(
                connection_id, query, params, result.success,
                result.duration_ms, result.row_count if result.success else 0,
                result.error
            )

            return result
        
        @self.app.tool()
        async def execute_transaction(
            connection_id: str,
            queries: List[Dict[str, Any]]
        ):
            """在事務中執行多個查詢"""
            result = await self.query_tool.execute_transaction(connection_id, queries)

            # 記錄事務歷史
            total_affected = sum(r.get('rows_affected', 0) for r in result.results if r.get('success'))
            self._add_query_history(
                connection_id, f"TRANSACTION ({len(queries)} queries)", None,
                result.success, result.duration_ms, total_affected, result.error
            )

            return result
        
        @self.app.tool()
        async def batch_execute(
            connection_id: str,
            query: str,
            params_list: List[List[Any]]
        ):
            """批次執行相同查詢，不同參數"""
            result = await self.query_tool.batch_execute(connection_id, query, params_list)

            # 記錄批次歷史
            self._add_query_history(
                connection_id, f"BATCH: {query[:50]}...", None,
                result.success, result.duration_ms, result.total_affected_rows, result.error
            )

            return result
        
        @self.app.tool()
        async def get_table_schema(
            connection_id: str,
            table_name: str,
            schema: str = "public"
        ):
            """獲取表結構詳情"""
            return await self.schema_tool.get_table_schema(connection_id, table_name, schema)
        
        @self.app.tool()
        async def list_tables(
            connection_id: str,
            schema: str = "public"
        ):
            """列出所有表"""
            return await self.schema_tool.list_tables(connection_id, schema)
        
        @self.app.tool()
        async def explain_query(
            connection_id: str,
            query: str,
            analyze: bool = False
        ):
            """分析查詢執行計畫"""
            return await self.schema_tool.explain_query(connection_id, query, analyze)

        @self.app.tool()
        async def health_check():
            """健康檢查"""
            health_status = await self.health_checker.check_overall_health()
            uptime = self.health_checker.get_uptime()

            return {
                "status": health_status.status,
                "is_healthy": health_status.is_healthy,
                "timestamp": health_status.timestamp.isoformat(),
                "uptime": uptime,
                "checks": health_status.checks
            }

        @self.app.tool()
        async def get_metrics():
            """取得伺服器指標"""
            metrics = await self.metrics_collector.get_server_metrics()

            return {
                "uptime_seconds": metrics.uptime_seconds,
                "total_connections": metrics.total_connections,
                "total_queries": metrics.total_queries,
                "successful_queries": metrics.successful_queries,
                "failed_queries": metrics.failed_queries,
                "success_rate": (metrics.successful_queries / metrics.total_queries * 100) if metrics.total_queries > 0 else 0,
                "avg_query_time_ms": metrics.avg_query_time_ms,
                "memory_usage_mb": round(metrics.memory_usage_bytes / (1024 * 1024), 2),
                "cpu_percent": metrics.cpu_percent,
                "connections": [
                    {
                        "connection_id": conn.connection_id,
                        "pool_size": conn.pool_size,
                        "idle_connections": conn.idle_connections,
                        "active_connections": conn.active_connections,
                        "total_queries": conn.total_queries,
                        "successful_queries": conn.successful_queries,
                        "failed_queries": conn.failed_queries,
                        "avg_query_time_ms": conn.avg_query_time_ms,
                        "last_activity": conn.last_activity.isoformat()
                    }
                    for conn in metrics.connections_metrics
                ]
            }

    def _add_query_history(
        self,
        connection_id: str,
        query: str,
        params: Optional[List[Any]],
        success: bool,
        duration_ms: int,
        rows_affected: int,
        error: Optional[str]
    ):
        """添加查詢歷史記錄"""
        history_entry = QueryHistory(
            connection_id=connection_id,
            query=query,
            params=params,
            success=success,
            duration_ms=duration_ms,
            rows_affected=rows_affected,
            error=error
        )

        self.query_history.append(history_entry)

        # 限制歷史記錄數量（最多保留 1000 條）
        if len(self.query_history) > 1000:
            self.query_history = self.query_history[-1000:]

        # 記錄到指標收集器
        self.metrics_collector.record_query(connection_id, success, duration_ms)

        logger.info(
            "query_history_recorded",
            connection_id=connection_id,
            query_type=query.split()[0] if query else "UNKNOWN",
            success=success,
            duration_ms=duration_ms,
            total_history_count=len(self.query_history)
        )

    def _register_resources(self):
        """Register all MCP resources"""

        @self.app.resource("postgresql://connections")
        async def get_connections():
            """返回所有活躍連線"""
            connections = await self.connection_manager.get_all_connections()
            return [
                Resource(
                    uri=f"postgresql://connection/{conn.connection_id}",
                    name=f"Connection: {conn.connection_id}",
                    description=f"PostgreSQL connection to {conn.host}:{conn.port}/{conn.database}",
                    mimeType="application/json"
                )
                for conn in connections
            ]

        @self.app.resource("postgresql://health")
        async def get_health_resource():
            """健康狀態資源"""
            health_status = await self.health_checker.check_overall_health()
            return [
                Resource(
                    uri="postgresql://health/status",
                    name="Health Status",
                    description=f"Server health: {health_status.status}",
                    mimeType="application/json"
                )
            ]

def main():
    """Main entry point"""
    server = PostgreSQLMCPServer()
    server.app.run()

if __name__ == "__main__":
    main()