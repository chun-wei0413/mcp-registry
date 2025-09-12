#!/usr/bin/env python3
"""
PostgreSQL MCP Server

A universal MCP server providing PostgreSQL database operations as tools.
This server acts as a pure tool layer without business logic.
"""

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
from .models.types import ConnectionInfo, QueryHistory

logger = structlog.get_logger()

class PostgreSQLMCPServer:
    """PostgreSQL MCP Server implementation"""
    
    def __init__(self):
        self.app = FastMCP("PostgreSQL MCP Server")
        self.connection_manager = ConnectionManager()
        self.query_tool = QueryTool(self.connection_manager)
        self.schema_tool = SchemaTool(self.connection_manager)
        self.query_history: List[QueryHistory] = []
        
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
            return await self.query_tool.execute_query(
                connection_id, query, params, fetch_size
            )
        
        @self.app.tool()
        async def execute_transaction(
            connection_id: str,
            queries: List[Dict[str, Any]]
        ):
            """在事務中執行多個查詢"""
            return await self.query_tool.execute_transaction(connection_id, queries)
        
        @self.app.tool()
        async def batch_execute(
            connection_id: str,
            query: str,
            params_list: List[List[Any]]
        ):
            """批次執行相同查詢，不同參數"""
            return await self.query_tool.batch_execute(connection_id, query, params_list)
        
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
    
    def _register_resources(self):
        """Register all MCP resources"""
        
        @self.app.resource("connections")
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
        
        @self.app.resource("query_history")
        async def get_query_history(connection_id: Optional[str] = None, limit: int = 100):
            """返回查詢歷史"""
            filtered_history = self.query_history
            if connection_id:
                filtered_history = [h for h in filtered_history if h.connection_id == connection_id]
            
            return [
                Resource(
                    uri=f"postgresql://query_history/{i}",
                    name=f"Query {i}: {history.query[:50]}...",
                    description=f"Executed at {history.executed_at}",
                    mimeType="application/json"
                )
                for i, history in enumerate(filtered_history[:limit])
            ]

def main():
    """Main entry point"""
    server = PostgreSQLMCPServer()
    server.app.run()

if __name__ == "__main__":
    main()