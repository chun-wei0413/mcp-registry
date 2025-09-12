"""
Connection pool management for PostgreSQL MCP Server
"""

import asyncio
from typing import Optional
import asyncpg
import structlog

logger = structlog.get_logger()

class ConnectionPool:
    """PostgreSQL 連線池管理"""
    
    def __init__(
        self,
        host: str,
        port: int,
        database: str,
        user: str,
        password: str,
        min_size: int = 2,
        max_size: int = 20,
        command_timeout: float = 30.0
    ):
        self.host = host
        self.port = port
        self.database = database
        self.user = user
        self.password = password
        self.min_size = min_size
        self.max_size = max_size
        self.command_timeout = command_timeout
        self._pool: Optional[asyncpg.Pool] = None
    
    async def initialize(self):
        """初始化連線池"""
        try:
            self._pool = await asyncpg.create_pool(
                host=self.host,
                port=self.port,
                database=self.database,
                user=self.user,
                password=self.password,
                min_size=self.min_size,
                max_size=self.max_size,
                command_timeout=self.command_timeout,
                server_settings={
                    'application_name': 'postgresql-mcp-server',
                }
            )
            
            logger.info(
                "connection_pool_created",
                host=self.host,
                port=self.port,
                database=self.database,
                min_size=self.min_size,
                max_size=self.max_size
            )
            
        except Exception as e:
            logger.error(
                "connection_pool_creation_failed",
                host=self.host,
                port=self.port,
                database=self.database,
                error=str(e)
            )
            raise
    
    def acquire(self):
        """取得連線"""
        if not self._pool:
            raise RuntimeError("Connection pool not initialized")
        return self._pool.acquire()
    
    async def execute(self, query: str, *args):
        """執行查詢"""
        async with self.acquire() as conn:
            return await conn.execute(query, *args)
    
    async def fetch(self, query: str, *args):
        """執行 SELECT 查詢"""
        async with self.acquire() as conn:
            return await conn.fetch(query, *args)
    
    async def fetchrow(self, query: str, *args):
        """執行查詢並返回單行"""
        async with self.acquire() as conn:
            return await conn.fetchrow(query, *args)
    
    async def fetchval(self, query: str, *args):
        """執行查詢並返回單值"""
        async with self.acquire() as conn:
            return await conn.fetchval(query, *args)
    
    async def close(self):
        """關閉連線池"""
        if self._pool:
            await self._pool.close()
            self._pool = None
            
            logger.info(
                "connection_pool_closed",
                host=self.host,
                port=self.port,
                database=self.database
            )
    
    @property
    def is_closed(self) -> bool:
        """檢查連線池是否已關閉"""
        return self._pool is None or self._pool._closed
    
    async def get_pool_stats(self) -> dict:
        """取得連線池統計資訊"""
        if not self._pool:
            return {}
        
        return {
            'size': self._pool.get_size(),
            'min_size': self._pool.get_min_size(),
            'max_size': self._pool.get_max_size(),
            'idle_size': self._pool.get_idle_size(),
            'is_closed': self._pool._closed
        }