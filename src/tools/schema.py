"""
Schema inspection tools for PostgreSQL MCP Server
"""

from typing import Any, Dict, List, Optional
import structlog

from ..models.types import TableSchema, TableInfo, ExplainResult, ColumnInfo
from .connection import ConnectionManager

logger = structlog.get_logger()

class SchemaTool:
    """Schema 檢查工具"""
    
    def __init__(self, connection_manager: ConnectionManager):
        self.connection_manager = connection_manager
    
    async def get_table_schema(
        self,
        connection_id: str,
        table_name: str,
        schema: str = "public"
    ) -> TableSchema:
        """獲取表結構詳情"""
        try:
            pool = await self.connection_manager.get_pool(connection_id)
            if not pool:
                return TableSchema(
                    success=False,
                    error="Connection not found",
                    table_name=table_name,
                    schema=schema,
                    columns=[]
                )
            
            async with pool.acquire() as conn:
                # 獲取欄位資訊
                columns_query = """
                SELECT 
                    c.column_name,
                    c.data_type,
                    c.character_maximum_length,
                    c.numeric_precision,
                    c.numeric_scale,
                    c.is_nullable,
                    c.column_default,
                    c.ordinal_position
                FROM information_schema.columns c
                WHERE c.table_schema = $1 AND c.table_name = $2
                ORDER BY c.ordinal_position
                """
                
                column_rows = await conn.fetch(columns_query, schema, table_name)
                
                if not column_rows:
                    return TableSchema(
                        success=False,
                        error=f"Table {schema}.{table_name} not found",
                        table_name=table_name,
                        schema=schema,
                        columns=[]
                    )
                
                columns = []
                for row in column_rows:
                    columns.append(ColumnInfo(
                        name=row['column_name'],
                        data_type=row['data_type'],
                        max_length=row['character_maximum_length'],
                        precision=row['numeric_precision'],
                        scale=row['numeric_scale'],
                        is_nullable=row['is_nullable'] == 'YES',
                        default_value=row['column_default'],
                        ordinal_position=row['ordinal_position']
                    ))
                
                # 獲取主鍵資訊
                pk_query = """
                SELECT kcu.column_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                    ON tc.constraint_name = kcu.constraint_name
                    AND tc.table_schema = kcu.table_schema
                WHERE tc.constraint_type = 'PRIMARY KEY'
                    AND tc.table_schema = $1
                    AND tc.table_name = $2
                ORDER BY kcu.ordinal_position
                """
                
                pk_rows = await conn.fetch(pk_query, schema, table_name)
                primary_keys = [row['column_name'] for row in pk_rows]
                
                # 獲取索引資訊
                index_query = """
                SELECT 
                    i.relname as index_name,
                    array_agg(a.attname ORDER BY array_position(i.indkey, a.attnum)) as columns,
                    i.indisunique as is_unique,
                    i.indisprimary as is_primary
                FROM pg_index i
                JOIN pg_class c ON c.oid = i.indrelid
                JOIN pg_class ic ON ic.oid = i.indexrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey)
                WHERE n.nspname = $1 AND c.relname = $2
                GROUP BY i.relname, i.indisunique, i.indisprimary
                ORDER BY i.relname
                """
                
                index_rows = await conn.fetch(index_query, schema, table_name)
                indexes = []
                for row in index_rows:
                    indexes.append({
                        'name': row['index_name'],
                        'columns': row['columns'],
                        'is_unique': row['is_unique'],
                        'is_primary': row['is_primary']
                    })
                
                # 獲取外鍵資訊
                fk_query = """
                SELECT 
                    kcu.column_name,
                    ccu.table_schema AS foreign_table_schema,
                    ccu.table_name AS foreign_table_name,
                    ccu.column_name AS foreign_column_name,
                    rc.constraint_name
                FROM information_schema.referential_constraints rc
                JOIN information_schema.key_column_usage kcu
                    ON rc.constraint_name = kcu.constraint_name
                    AND rc.constraint_schema = kcu.constraint_schema
                JOIN information_schema.constraint_column_usage ccu
                    ON rc.unique_constraint_name = ccu.constraint_name
                    AND rc.unique_constraint_schema = ccu.constraint_schema
                WHERE kcu.table_schema = $1 AND kcu.table_name = $2
                """
                
                fk_rows = await conn.fetch(fk_query, schema, table_name)
                foreign_keys = []
                for row in fk_rows:
                    foreign_keys.append({
                        'column_name': row['column_name'],
                        'foreign_table_schema': row['foreign_table_schema'],
                        'foreign_table_name': row['foreign_table_name'],
                        'foreign_column_name': row['foreign_column_name'],
                        'constraint_name': row['constraint_name']
                    })
                
                logger.info(
                    "table_schema_retrieved",
                    connection_id=connection_id,
                    table_name=table_name,
                    schema=schema,
                    column_count=len(columns),
                    primary_key_count=len(primary_keys),
                    index_count=len(indexes),
                    foreign_key_count=len(foreign_keys)
                )
                
                return TableSchema(
                    success=True,
                    table_name=table_name,
                    schema=schema,
                    columns=columns,
                    primary_keys=primary_keys,
                    indexes=indexes,
                    foreign_keys=foreign_keys
                )
                
        except Exception as e:
            logger.error(
                "get_table_schema_failed",
                connection_id=connection_id,
                table_name=table_name,
                schema=schema,
                error=str(e)
            )
            
            return TableSchema(
                success=False,
                error=str(e),
                table_name=table_name,
                schema=schema,
                columns=[]
            )
    
    async def list_tables(
        self,
        connection_id: str,
        schema: str = "public"
    ) -> List[TableInfo]:
        """列出所有表"""
        try:
            pool = await self.connection_manager.get_pool(connection_id)
            if not pool:
                return []
            
            async with pool.acquire() as conn:
                query = """
                SELECT 
                    t.table_name,
                    t.table_type,
                    obj_description(c.oid, 'pg_class') as table_comment
                FROM information_schema.tables t
                LEFT JOIN pg_class c ON c.relname = t.table_name
                LEFT JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE t.table_schema = $1
                    AND (n.nspname = $1 OR n.nspname IS NULL)
                ORDER BY t.table_name
                """
                
                rows = await conn.fetch(query, schema)
                
                tables = []
                for row in rows:
                    tables.append(TableInfo(
                        name=row['table_name'],
                        schema=schema,
                        type=row['table_type'],
                        comment=row['table_comment']
                    ))
                
                logger.info(
                    "tables_listed",
                    connection_id=connection_id,
                    schema=schema,
                    table_count=len(tables)
                )
                
                return tables
                
        except Exception as e:
            logger.error(
                "list_tables_failed",
                connection_id=connection_id,
                schema=schema,
                error=str(e)
            )
            
            return []
    
    async def explain_query(
        self,
        connection_id: str,
        query: str,
        analyze: bool = False
    ) -> ExplainResult:
        """分析查詢執行計畫"""
        try:
            pool = await self.connection_manager.get_pool(connection_id)
            if not pool:
                return ExplainResult(
                    success=False,
                    error="Connection not found",
                    query=query,
                    plan=[]
                )
            
            async with pool.acquire() as conn:
                explain_query = f"EXPLAIN {'ANALYZE ' if analyze else ''}(FORMAT JSON) {query}"
                
                result = await conn.fetchval(explain_query)
                plan_data = result[0] if result else {}
                
                logger.info(
                    "query_explained",
                    connection_id=connection_id,
                    query=query[:100],
                    analyze=analyze
                )
                
                return ExplainResult(
                    success=True,
                    query=query,
                    plan=result,
                    total_cost=plan_data.get('Total Cost', 0) if analyze else plan_data.get('Plan', {}).get('Total Cost', 0),
                    execution_time=plan_data.get('Execution Time') if analyze else None
                )
                
        except Exception as e:
            logger.error(
                "explain_query_failed",
                connection_id=connection_id,
                query=query[:100],
                error=str(e)
            )
            
            return ExplainResult(
                success=False,
                error=str(e),
                query=query,
                plan=[]
            )