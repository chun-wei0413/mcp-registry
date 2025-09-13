"""
Connection management tools for PostgreSQL MCP Server
"""

import asyncio
from typing import Dict, List, Optional
import asyncpg
import structlog
from cryptography.fernet import Fernet
import os

from ..models.types import ConnectionInfo, ConnectionResult, ConnectionStatus, SecurityConfig
from ..db.pool import ConnectionPool
from ..security import SecurityValidator

logger = structlog.get_logger()

class ConnectionManager:
    """管理資料庫連線"""

    def __init__(self, security_config: Optional[SecurityConfig] = None):
        self.pools: Dict[str, ConnectionPool] = {}
        self.connections: Dict[str, ConnectionInfo] = {}
        self._cipher_key = self._get_or_create_key()
        self.security_validator = SecurityValidator(security_config or SecurityConfig())
        
    def _get_or_create_key(self) -> bytes:
        """取得或建立加密金鑰"""
        key = os.environ.get("POSTGRES_MCP_ENCRYPTION_KEY")
        if key:
            return key.encode()
        
        # 為開發環境生成暫時金鑰
        return Fernet.generate_key()
    
    def _encrypt_password(self, password: str) -> str:
        """加密密碼"""
        fernet = Fernet(self._cipher_key)
        return fernet.encrypt(password.encode()).decode()
    
    def _decrypt_password(self, encrypted_password: str) -> str:
        """解密密碼"""
        fernet = Fernet(self._cipher_key)
        return fernet.decrypt(encrypted_password.encode()).decode()
    
    async def add_connection(
        self,
        connection_id: str,
        host: str,
        port: int,
        database: str,
        user: str,
        password: str,
        pool_size: int = 10
    ) -> ConnectionResult:
        """建立新的資料庫連線"""
        try:
            # 安全性驗證連線參數
            security_result = self.security_validator.validate_connection_params(
                host, port, database, user
            )
            if not security_result.is_valid:
                return ConnectionResult(
                    connection_id=connection_id,
                    success=False,
                    message=f"Connection security validation failed: {security_result.error_message}"
                )

            # 測試連線
            test_conn = await asyncpg.connect(
                host=host,
                port=port,
                database=database,
                user=user,
                password=password,
                command_timeout=30
            )
            await test_conn.close()
            
            # 建立連線池
            pool = ConnectionPool(
                host=host,
                port=port,
                database=database,
                user=user,
                password=password,
                min_size=2,
                max_size=pool_size,
                command_timeout=30
            )
            
            await pool.initialize()
            
            # 儲存連線資訊
            encrypted_password = self._encrypt_password(password)
            self.connections[connection_id] = ConnectionInfo(
                connection_id=connection_id,
                host=host,
                port=port,
                database=database,
                user=user,
                encrypted_password=encrypted_password,
                pool_size=pool_size,
                is_active=True
            )
            
            self.pools[connection_id] = pool
            
            logger.info(
                "connection_added",
                connection_id=connection_id,
                host=host,
                port=port,
                database=database,
                user=user
            )
            
            return ConnectionResult(
                connection_id=connection_id,
                success=True,
                message="Connection established successfully"
            )
            
        except Exception as e:
            logger.error(
                "connection_failed",
                connection_id=connection_id,
                error=str(e),
                host=host,
                port=port,
                database=database
            )
            
            return ConnectionResult(
                connection_id=connection_id,
                success=False,
                message=f"Connection failed: {str(e)}"
            )
    
    async def test_connection(self, connection_id: str) -> ConnectionStatus:
        """測試連線狀態"""
        if connection_id not in self.pools:
            return ConnectionStatus(
                connection_id=connection_id,
                is_healthy=False,
                message="Connection not found"
            )
        
        try:
            pool = self.pools[connection_id]
            async with pool.acquire() as conn:
                await conn.execute("SELECT 1")
            
            return ConnectionStatus(
                connection_id=connection_id,
                is_healthy=True,
                message="Connection is healthy"
            )
            
        except Exception as e:
            logger.error(
                "connection_test_failed",
                connection_id=connection_id,
                error=str(e)
            )
            
            return ConnectionStatus(
                connection_id=connection_id,
                is_healthy=False,
                message=f"Connection test failed: {str(e)}"
            )
    
    async def get_pool(self, connection_id: str) -> Optional[ConnectionPool]:
        """取得連線池"""
        return self.pools.get(connection_id)
    
    async def get_all_connections(self) -> List[ConnectionInfo]:
        """取得所有連線資訊"""
        return list(self.connections.values())
    
    async def close_connection(self, connection_id: str) -> bool:
        """關閉連線"""
        if connection_id in self.pools:
            try:
                await self.pools[connection_id].close()
                del self.pools[connection_id]
                
                if connection_id in self.connections:
                    self.connections[connection_id].is_active = False
                
                logger.info("connection_closed", connection_id=connection_id)
                return True
                
            except Exception as e:
                logger.error(
                    "connection_close_failed",
                    connection_id=connection_id,
                    error=str(e)
                )
                return False
        
        return False
    
    async def close_all_connections(self):
        """關閉所有連線"""
        for connection_id in list(self.pools.keys()):
            await self.close_connection(connection_id)