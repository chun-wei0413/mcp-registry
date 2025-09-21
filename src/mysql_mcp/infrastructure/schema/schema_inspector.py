"""MySQL Schema Inspector Implementation"""

from typing import List, Optional
import structlog

from ...core.interfaces import ISchemaInspector, IConnectionPool, TableSchema, TableColumn, TableIndex, TableConstraint
from ...core.exceptions import SchemaInspectionError

logger = structlog.get_logger()


class MySQLSchemaInspector(ISchemaInspector):
    """MySQL schema inspection implementation"""

    def __init__(self, connection_pool: IConnectionPool):
        self._connection_pool = connection_pool

    async def get_table_schema(
        self,
        connection_id: str,
        table_name: str,
        schema_name: str = "mysql"
    ) -> TableSchema:
        """Get complete table schema for MySQL"""
        try:
            async with await self._connection_pool.get_connection(connection_id) as conn:
                async with conn.cursor() as cursor:
                    # Get table columns
                    columns = await self._get_table_columns(cursor, table_name, schema_name)

                    # Get table indexes
                    indexes = await self._get_table_indexes(cursor, table_name, schema_name)

                    # Get table constraints
                    constraints = await self._get_table_constraints(cursor, table_name, schema_name)

                    # Get table statistics
                    row_count, table_size = await self._get_table_statistics(cursor, table_name, schema_name)

            return TableSchema(
                table_name=table_name,
                schema_name=schema_name,
                columns=columns,
                indexes=indexes,
                constraints=constraints,
                row_count=row_count,
                table_size_bytes=table_size
            )

        except Exception as e:
            logger.error(
                "mysql_schema_inspection_failed",
                connection_id=connection_id,
                table_name=f"{schema_name}.{table_name}",
                error=str(e)
            )
            raise SchemaInspectionError(f"Failed to inspect MySQL table schema: {str(e)}")

    async def _get_table_columns(self, cursor, table_name: str, schema_name: str) -> List[TableColumn]:
        """Get table columns from INFORMATION_SCHEMA"""
        query = """
        SELECT
            COLUMN_NAME,
            DATA_TYPE,
            IS_NULLABLE,
            COLUMN_DEFAULT,
            CHARACTER_MAXIMUM_LENGTH,
            NUMERIC_PRECISION,
            NUMERIC_SCALE,
            COLUMN_COMMENT
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s
        ORDER BY ORDINAL_POSITION
        """

        await cursor.execute(query, (schema_name, table_name))
        rows = await cursor.fetchall()

        columns = []
        for row in rows:
            columns.append(TableColumn(
                name=row[0],
                data_type=row[1],
                is_nullable=row[2] == 'YES',
                default_value=row[3],
                max_length=row[4],
                precision=row[5],
                scale=row[6],
                comment=row[7]
            ))

        return columns

    async def _get_table_indexes(self, cursor, table_name: str, schema_name: str) -> List[TableIndex]:
        """Get table indexes from INFORMATION_SCHEMA"""
        query = """
        SELECT
            INDEX_NAME,
            CONCAT('INDEX ', INDEX_NAME, ' ON ', TABLE_NAME, ' (', GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX), ')') as definition,
            NOT NON_UNIQUE as is_unique,
            INDEX_NAME = 'PRIMARY' as is_primary
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s
        GROUP BY INDEX_NAME, NON_UNIQUE
        """

        await cursor.execute(query, (schema_name, table_name))
        rows = await cursor.fetchall()

        indexes = []
        for row in rows:
            indexes.append(TableIndex(
                name=row[0],
                definition=row[1],
                is_unique=bool(row[2]),
                is_primary=bool(row[3])
            ))

        return indexes

    async def _get_table_constraints(self, cursor, table_name: str, schema_name: str) -> List[TableConstraint]:
        """Get table constraints from INFORMATION_SCHEMA"""
        query = """
        SELECT
            CONSTRAINT_NAME,
            CONSTRAINT_TYPE,
            COLUMN_NAME,
            REFERENCED_TABLE_NAME,
            REFERENCED_COLUMN_NAME
        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
        JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
            ON kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
            AND kcu.TABLE_SCHEMA = tc.TABLE_SCHEMA
        WHERE kcu.TABLE_SCHEMA = %s AND kcu.TABLE_NAME = %s
        """

        await cursor.execute(query, (schema_name, table_name))
        rows = await cursor.fetchall()

        constraints = []
        for row in rows:
            constraints.append(TableConstraint(
                name=row[0],
                type=row[1],
                column_name=row[2],
                foreign_table=row[3],
                foreign_column=row[4]
            ))

        return constraints

    async def _get_table_statistics(self, cursor, table_name: str, schema_name: str) -> tuple[int, int]:
        """Get table row count and size"""
        # Get row count
        count_query = f"SELECT COUNT(*) FROM `{schema_name}`.`{table_name}`"
        await cursor.execute(count_query)
        row_count_result = await cursor.fetchone()
        row_count = row_count_result[0] if row_count_result else 0

        # Get table size
        size_query = """
        SELECT
            ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) AS size_mb
        FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s
        """
        await cursor.execute(size_query, (schema_name, table_name))
        size_result = await cursor.fetchone()
        table_size_mb = size_result[0] if size_result and size_result[0] else 0
        table_size_bytes = int(table_size_mb * 1024 * 1024) if table_size_mb else 0

        return row_count, table_size_bytes

    async def list_tables(
        self,
        connection_id: str,
        schema_name: Optional[str] = None
    ) -> List[str]:
        """List all tables in schema"""
        try:
            async with await self._connection_pool.get_connection(connection_id) as conn:
                async with conn.cursor() as cursor:
                    if schema_name:
                        query = """
                        SELECT TABLE_NAME
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_SCHEMA = %s AND TABLE_TYPE = 'BASE TABLE'
                        ORDER BY TABLE_NAME
                        """
                        await cursor.execute(query, (schema_name,))
                    else:
                        # Get current database if no schema specified
                        await cursor.execute("SELECT DATABASE()")
                        current_db = await cursor.fetchone()
                        if current_db and current_db[0]:
                            query = """
                            SELECT TABLE_NAME
                            FROM INFORMATION_SCHEMA.TABLES
                            WHERE TABLE_SCHEMA = %s AND TABLE_TYPE = 'BASE TABLE'
                            ORDER BY TABLE_NAME
                            """
                            await cursor.execute(query, (current_db[0],))
                        else:
                            return []

                    rows = await cursor.fetchall()
                    return [row[0] for row in rows]

        except Exception as e:
            logger.error(
                "mysql_list_tables_failed",
                connection_id=connection_id,
                schema_name=schema_name,
                error=str(e)
            )
            raise SchemaInspectionError(f"Failed to list MySQL tables: {str(e)}")

    async def list_schemas(self, connection_id: str) -> List[str]:
        """List all schemas/databases in MySQL"""
        try:
            async with await self._connection_pool.get_connection(connection_id) as conn:
                async with conn.cursor() as cursor:
                    query = """
                    SELECT SCHEMA_NAME
                    FROM INFORMATION_SCHEMA.SCHEMATA
                    WHERE SCHEMA_NAME NOT IN ('information_schema', 'performance_schema', 'mysql', 'sys')
                    ORDER BY SCHEMA_NAME
                    """
                    await cursor.execute(query)
                    rows = await cursor.fetchall()
                    return [row[0] for row in rows]

        except Exception as e:
            logger.error(
                "mysql_list_schemas_failed",
                connection_id=connection_id,
                error=str(e)
            )
            raise SchemaInspectionError(f"Failed to list MySQL schemas: {str(e)}")