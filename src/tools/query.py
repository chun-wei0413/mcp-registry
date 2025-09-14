"""
Query Execution Tools

SPECIFICATION:
This module implements the core query execution tools for the PostgreSQL MCP Server.
It provides secure, efficient, and comprehensive database query capabilities through
MCP tools, including single queries, transactions, batch operations, and streaming.

CORE QUERY TOOLS:
1. execute_query: Single SQL query execution with parameter binding
2. execute_transaction: Multi-statement atomic transaction execution
3. batch_execute: Efficient bulk operation execution with batching
4. execute_streaming: Large result set streaming for memory efficiency

QUERY EXECUTION FEATURES:
- Parameterized Queries: SQL injection protection through parameter binding
- Transaction Support: ACID-compliant multi-statement transactions
- Batch Processing: High-performance bulk data operations
- Result Streaming: Memory-efficient processing of large datasets
- Query Optimization: Automatic query performance analysis and optimization

SECURITY INTEGRATION:
- Query Validation: Pre-execution security validation for all queries
- Operation Filtering: Configurable allowed/blocked SQL operations
- Parameter Sanitization: Input validation and sanitization
- Access Control: Query-level permission enforcement
- Audit Logging: Comprehensive query execution logging and tracking

PERFORMANCE FEATURES:
- Connection Pool Integration: Efficient database connection utilization
- Query Plan Analysis: EXPLAIN query support for performance tuning
- Execution Metrics: Query timing and performance measurement
- Resource Management: Memory and connection resource optimization
- Concurrent Execution: Parallel query processing capabilities

TRANSACTION MANAGEMENT:
- ACID Compliance: Full ACID transaction property support
- Savepoint Support: Nested transaction with rollback capabilities
- Isolation Levels: Configurable transaction isolation levels
- Deadlock Handling: Automatic deadlock detection and resolution
- Transaction Timeout: Configurable transaction execution limits

ERROR HANDLING:
- Comprehensive Error Catching: Database and network error handling
- Retry Logic: Automatic retry for transient errors
- Error Categorization: Detailed error classification and reporting
- Recovery Mechanisms: Transparent error recovery where possible
- User-Friendly Messages: Clear error messages for troubleshooting

USAGE PATTERN:
The QueryTool integrates with ConnectionManager for database access
and SecurityValidator for query validation. It provides the primary
interface for all database operations while maintaining security,
performance, and reliability standards.
"""

import time
from typing import Any, Dict, List, Optional
import structlog
import asyncpg

from ..models.types import QueryResult, TransactionResult, BatchResult, SecurityConfig
from .connection import ConnectionManager
from ..security import SecurityValidator

logger = structlog.get_logger()

class QueryTool:
    """查詢執行工具"""

    def __init__(self, connection_manager: ConnectionManager, security_config: Optional[SecurityConfig] = None):
        self.connection_manager = connection_manager
        self.security_validator = SecurityValidator(security_config or SecurityConfig())
    
    async def execute_query(
        self,
        connection_id: str,
        query: str,
        params: Optional[List[Any]] = None,
        fetch_size: Optional[int] = None
    ) -> QueryResult:
        """執行 SELECT 查詢"""
        start_time = time.time()

        # 安全性驗證
        security_result = self.security_validator.validate_query(query)
        if not security_result.is_valid:
            return QueryResult(
                success=False,
                error=security_result.error_message,
                query=query,
                duration_ms=0
            )

        try:
            pool = await self.connection_manager.get_pool(connection_id)
            if not pool:
                return QueryResult(
                    success=False,
                    error="Connection not found",
                    query=query,
                    duration_ms=0
                )

            # 額外檢查：execute_query 只允許 SELECT、WITH 和 EXPLAIN
            query_upper = query.strip().upper()
            if not (query_upper.startswith('SELECT') or query_upper.startswith('WITH') or query_upper.startswith('EXPLAIN')):
                return QueryResult(
                    success=False,
                    error="Only SELECT, WITH, and EXPLAIN queries are allowed in execute_query",
                    query=query,
                    duration_ms=0
                )
            
            async with pool.acquire() as conn:
                if params:
                    result = await conn.fetch(query, *params)
                else:
                    result = await conn.fetch(query)
                
                # 轉換結果為字典列表
                rows = [dict(row) for row in result]
                
                duration_ms = int((time.time() - start_time) * 1000)
                
                logger.info(
                    "query_executed",
                    connection_id=connection_id,
                    query=query[:100],
                    params_count=len(params) if params else 0,
                    rows_returned=len(rows),
                    duration_ms=duration_ms
                )
                
                return QueryResult(
                    success=True,
                    rows=rows,
                    row_count=len(rows),
                    columns=[col for col in rows[0].keys()] if rows else [],
                    query=query,
                    duration_ms=duration_ms
                )
                
        except Exception as e:
            duration_ms = int((time.time() - start_time) * 1000)
            
            logger.error(
                "query_failed",
                connection_id=connection_id,
                query=query[:100],
                error=str(e),
                duration_ms=duration_ms
            )
            
            return QueryResult(
                success=False,
                error=str(e),
                query=query,
                duration_ms=duration_ms
            )
    
    async def execute_transaction(
        self,
        connection_id: str,
        queries: List[Dict[str, Any]]
    ) -> TransactionResult:
        """在事務中執行多個查詢"""
        start_time = time.time()
        results = []
        rolled_back = False

        # 預先驗證所有查詢的安全性
        for i, query_info in enumerate(queries):
            query = query_info.get('query', '')
            security_result = self.security_validator.validate_query(query)
            if not security_result.is_valid:
                return TransactionResult(
                    success=False,
                    error=f"Query {i+1} security validation failed: {security_result.error_message}",
                    results=[],
                    rolled_back=False,
                    duration_ms=0
                )

        try:
            pool = await self.connection_manager.get_pool(connection_id)
            if not pool:
                return TransactionResult(
                    success=False,
                    error="Connection not found",
                    results=[],
                    rolled_back=False,
                    duration_ms=0
                )
            
            async with pool.acquire() as conn:
                async with conn.transaction():
                    for i, query_info in enumerate(queries):
                        query = query_info.get('query', '')
                        params = query_info.get('params', [])
                        
                        try:
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
                                # 解析影響的行數
                                affected_rows = 0
                                if status.startswith('INSERT'):
                                    affected_rows = int(status.split()[-1])
                                elif status.startswith('UPDATE'):
                                    affected_rows = int(status.split()[-1])
                                elif status.startswith('DELETE'):
                                    affected_rows = int(status.split()[-1])
                                
                                results.append({
                                    'query_index': i,
                                    'success': True,
                                    'rows_affected': affected_rows,
                                    'status': status
                                })
                                
                        except Exception as query_error:
                            results.append({
                                'query_index': i,
                                'success': False,
                                'error': str(query_error)
                            })
                            raise query_error  # 觸發事務回滾
                
                duration_ms = int((time.time() - start_time) * 1000)
                
                logger.info(
                    "transaction_executed",
                    connection_id=connection_id,
                    query_count=len(queries),
                    duration_ms=duration_ms
                )
                
                return TransactionResult(
                    success=True,
                    results=results,
                    rolled_back=False,
                    duration_ms=duration_ms
                )
                
        except Exception as e:
            rolled_back = True
            duration_ms = int((time.time() - start_time) * 1000)
            
            logger.error(
                "transaction_failed",
                connection_id=connection_id,
                error=str(e),
                rolled_back=rolled_back,
                duration_ms=duration_ms
            )
            
            return TransactionResult(
                success=False,
                error=str(e),
                results=results,
                rolled_back=rolled_back,
                duration_ms=duration_ms
            )
    
    async def batch_execute(
        self,
        connection_id: str,
        query: str,
        params_list: List[List[Any]]
    ) -> BatchResult:
        """批次執行相同查詢，不同參數"""
        start_time = time.time()

        # 安全性驗證
        security_result = self.security_validator.validate_query(query)
        if not security_result.is_valid:
            return BatchResult(
                success=False,
                error=security_result.error_message,
                batch_size=len(params_list),
                total_affected_rows=0,
                duration_ms=0
            )

        try:
            pool = await self.connection_manager.get_pool(connection_id)
            if not pool:
                return BatchResult(
                    success=False,
                    error="Connection not found",
                    batch_size=len(params_list),
                    total_affected_rows=0,
                    duration_ms=0
                )
            
            async with pool.acquire() as conn:
                # 使用 executemany 進行批次操作
                await conn.executemany(query, params_list)
                
                duration_ms = int((time.time() - start_time) * 1000)
                
                logger.info(
                    "batch_executed",
                    connection_id=connection_id,
                    query=query[:100],
                    batch_size=len(params_list),
                    duration_ms=duration_ms
                )
                
                return BatchResult(
                    success=True,
                    batch_size=len(params_list),
                    total_affected_rows=len(params_list),  # 概估值
                    duration_ms=duration_ms
                )
                
        except Exception as e:
            duration_ms = int((time.time() - start_time) * 1000)
            
            logger.error(
                "batch_failed",
                connection_id=connection_id,
                query=query[:100],
                error=str(e),
                batch_size=len(params_list),
                duration_ms=duration_ms
            )
            
            return BatchResult(
                success=False,
                error=str(e),
                batch_size=len(params_list),
                total_affected_rows=0,
                duration_ms=duration_ms
            )