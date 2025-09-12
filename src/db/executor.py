"""
SQL execution utilities for PostgreSQL MCP Server
"""

import asyncio
import time
from typing import Any, Dict, List, Optional, Union
import asyncpg
import structlog

logger = structlog.get_logger()

class SQLExecutor:
    """SQL 執行器"""
    
    def __init__(self, pool):
        self.pool = pool
    
    async def execute_with_timeout(
        self,
        query: str,
        params: Optional[List[Any]] = None,
        timeout: float = 30.0
    ) -> Any:
        """執行查詢並設定超時"""
        try:
            async with asyncio.timeout(timeout):
                async with self.pool.acquire() as conn:
                    if params:
                        return await conn.execute(query, *params)
                    else:
                        return await conn.execute(query)
        except asyncio.TimeoutError:
            logger.error(
                "query_timeout",
                query=query[:100],
                timeout=timeout
            )
            raise
    
    async def fetch_with_timeout(
        self,
        query: str,
        params: Optional[List[Any]] = None,
        timeout: float = 30.0,
        fetch_size: Optional[int] = None
    ) -> List[asyncpg.Record]:
        """執行 SELECT 查詢並設定超時"""
        try:
            async with asyncio.timeout(timeout):
                async with self.pool.acquire() as conn:
                    if fetch_size:
                        # 使用游標進行分批讀取
                        results = []
                        async with conn.transaction():
                            if params:
                                cursor = await conn.cursor(query, *params)
                            else:
                                cursor = await conn.cursor(query)
                            
                            async for record in cursor:
                                results.append(record)
                                if len(results) >= fetch_size:
                                    break
                        
                        return results
                    else:
                        if params:
                            return await conn.fetch(query, *params)
                        else:
                            return await conn.fetch(query)
                            
        except asyncio.TimeoutError:
            logger.error(
                "query_timeout",
                query=query[:100],
                timeout=timeout,
                fetch_size=fetch_size
            )
            raise
    
    async def execute_transaction_with_savepoints(
        self,
        queries: List[Dict[str, Any]],
        timeout: float = 30.0
    ) -> Dict[str, Any]:
        """使用儲存點執行事務"""
        results = []
        savepoints = {}
        
        try:
            async with asyncio.timeout(timeout):
                async with self.pool.acquire() as conn:
                    async with conn.transaction():
                        for i, query_info in enumerate(queries):
                            query = query_info.get('query', '')
                            params = query_info.get('params', [])
                            savepoint_name = f'sp_{i}'
                            
                            try:
                                # 建立儲存點
                                await conn.execute(f'SAVEPOINT {savepoint_name}')
                                savepoints[i] = savepoint_name
                                
                                # 執行查詢
                                if query.strip().upper().startswith('SELECT'):
                                    result = await conn.fetch(query, *params)
                                    rows = [dict(row) for row in result]
                                    results.append({
                                        'query_index': i,
                                        'success': True,
                                        'rows': rows,
                                        'row_count': len(rows)
                                    })
                                else:
                                    status = await conn.execute(query, *params)
                                    affected_rows = self._parse_affected_rows(status)
                                    results.append({
                                        'query_index': i,
                                        'success': True,
                                        'rows_affected': affected_rows,
                                        'status': status
                                    })
                                
                                # 釋放儲存點
                                await conn.execute(f'RELEASE SAVEPOINT {savepoint_name}')
                                
                            except Exception as query_error:
                                # 回滾到儲存點
                                await conn.execute(f'ROLLBACK TO SAVEPOINT {savepoint_name}')
                                
                                results.append({
                                    'query_index': i,
                                    'success': False,
                                    'error': str(query_error),
                                    'savepoint_rollback': True
                                })
                                
                                # 根據設定決定是否繼續
                                if query_info.get('fail_on_error', True):
                                    raise query_error
                
                return {
                    'success': True,
                    'results': results,
                    'rolled_back': False
                }
                
        except asyncio.TimeoutError:
            logger.error(
                "transaction_timeout",
                query_count=len(queries),
                timeout=timeout
            )
            raise
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'results': results,
                'rolled_back': True
            }
    
    def _parse_affected_rows(self, status: str) -> int:
        """解析影響的行數"""
        try:
            if status.startswith(('INSERT', 'UPDATE', 'DELETE')):
                return int(status.split()[-1])
            return 0
        except (ValueError, IndexError):
            return 0
    
    async def explain_query_advanced(
        self,
        query: str,
        params: Optional[List[Any]] = None,
        analyze: bool = False,
        verbose: bool = False,
        buffers: bool = False,
        settings: bool = False
    ) -> Dict[str, Any]:
        """進階查詢計畫分析"""
        explain_options = ['FORMAT JSON']
        if analyze:
            explain_options.append('ANALYZE')
        if verbose:
            explain_options.append('VERBOSE')
        if buffers:
            explain_options.append('BUFFERS')
        if settings:
            explain_options.append('SETTINGS')
        
        explain_query = f"EXPLAIN ({', '.join(explain_options)}) {query}"
        
        async with self.pool.acquire() as conn:
            if params:
                result = await conn.fetchval(explain_query, *params)
            else:
                result = await conn.fetchval(explain_query)
            
            return {
                'plan': result,
                'options': {
                    'analyze': analyze,
                    'verbose': verbose,
                    'buffers': buffers,
                    'settings': settings
                }
            }
    
    async def get_query_locks(self, connection_id: str) -> List[Dict[str, Any]]:
        """取得查詢相關的鎖定資訊"""
        query = """
        SELECT 
            locktype,
            database,
            relation::regclass as table_name,
            page,
            tuple,
            virtualxid,
            transactionid,
            mode,
            granted,
            fastpath
        FROM pg_locks
        WHERE pid = pg_backend_pid()
        """
        
        async with self.pool.acquire() as conn:
            rows = await conn.fetch(query)
            return [dict(row) for row in rows]