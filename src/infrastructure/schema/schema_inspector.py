"""Database schema inspection implementation."""

from typing import List, Dict, Any, Optional
import asyncpg
import structlog

from ...core.interfaces import ISchemaInspector, IConnectionManager
from ...core.exceptions import QueryError
from ...domain.models import TableSchema, ColumnInfo, IndexInfo, ConstraintInfo

logger = structlog.get_logger()


class PostgreSQLSchemaInspector(ISchemaInspector):
    """PostgreSQL schema inspector implementation."""

    def __init__(self, connection_manager: IConnectionManager):
        self._connection_manager = connection_manager

    async def get_table_schema(
        self,
        connection_id: str,
        table_name: str,
        schema_name: str = "public"
    ) -> TableSchema:
        """Get detailed table schema information."""
        try:
            conn = await self._connection_manager.get_connection(connection_id)
            if not conn:
                raise QueryError(f"Connection {connection_id} not found")

            pool = await self._connection_manager._get_pool(connection_id)
            async with pool.acquire() as connection:
                # Get table columns
                columns = await self._get_table_columns(connection, table_name, schema_name)

                # Get table indexes
                indexes = await self._get_table_indexes(connection, table_name, schema_name)

                # Get table constraints
                constraints = await self._get_table_constraints(connection, table_name, schema_name)

                # Get table statistics
                stats = await self._get_table_statistics(connection, table_name, schema_name)

                schema = TableSchema(
                    table_name=table_name,
                    schema_name=schema_name,
                    columns=columns,
                    indexes=indexes,
                    constraints=constraints,
                    row_count=stats.get("row_count", 0),
                    table_size_bytes=stats.get("table_size", 0)
                )

                logger.debug(
                    "table_schema_retrieved",
                    connection_id=connection_id,
                    table_name=f"{schema_name}.{table_name}",
                    column_count=len(columns)
                )

                return schema

        except Exception as e:
            logger.error(
                "get_table_schema_failed",
                connection_id=connection_id,
                table_name=f"{schema_name}.{table_name}",
                error=str(e)
            )
            raise QueryError(f"Failed to get table schema: {e}")

    async def list_tables(
        self,
        connection_id: str,
        schema_name: str = "public"
    ) -> List[Dict[str, Any]]:
        """List all tables in a schema."""
        try:
            conn = await self._connection_manager.get_connection(connection_id)
            if not conn:
                raise QueryError(f"Connection {connection_id} not found")

            pool = await self._connection_manager._get_pool(connection_id)
            async with pool.acquire() as connection:
                query = """
                SELECT
                    t.table_name,
                    t.table_type,
                    obj_description(c.oid) as table_comment,
                    pg_size_pretty(pg_total_relation_size(c.oid)) as table_size
                FROM information_schema.tables t
                LEFT JOIN pg_class c ON c.relname = t.table_name
                LEFT JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE t.table_schema = $1
                AND n.nspname = $1
                ORDER BY t.table_name;
                """

                rows = await connection.fetch(query, schema_name)

                tables = []
                for row in rows:
                    tables.append({
                        "table_name": row["table_name"],
                        "table_type": row["table_type"],
                        "comment": row["table_comment"],
                        "size": row["table_size"]
                    })

                logger.debug(
                    "tables_listed",
                    connection_id=connection_id,
                    schema_name=schema_name,
                    table_count=len(tables)
                )

                return tables

        except Exception as e:
            logger.error(
                "list_tables_failed",
                connection_id=connection_id,
                schema_name=schema_name,
                error=str(e)
            )
            raise QueryError(f"Failed to list tables: {e}")

    async def list_schemas(self, connection_id: str) -> List[str]:
        """List all schemas in the database."""
        try:
            conn = await self._connection_manager.get_connection(connection_id)
            if not conn:
                raise QueryError(f"Connection {connection_id} not found")

            pool = await self._connection_manager._get_pool(connection_id)
            async with pool.acquire() as connection:
                query = """
                SELECT schema_name
                FROM information_schema.schemata
                WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
                ORDER BY schema_name;
                """

                rows = await connection.fetch(query)
                schemas = [row["schema_name"] for row in rows]

                logger.debug(
                    "schemas_listed",
                    connection_id=connection_id,
                    schema_count=len(schemas)
                )

                return schemas

        except Exception as e:
            logger.error(
                "list_schemas_failed",
                connection_id=connection_id,
                error=str(e)
            )
            raise QueryError(f"Failed to list schemas: {e}")

    async def _get_table_columns(
        self,
        connection: asyncpg.Connection,
        table_name: str,
        schema_name: str
    ) -> List[ColumnInfo]:
        """Get column information for a table."""
        query = """
        SELECT
            c.column_name,
            c.data_type,
            c.is_nullable::boolean,
            c.column_default,
            c.character_maximum_length,
            c.numeric_precision,
            c.numeric_scale,
            col_description(pgc.oid, c.ordinal_position) as column_comment
        FROM information_schema.columns c
        LEFT JOIN pg_class pgc ON pgc.relname = c.table_name
        LEFT JOIN pg_namespace pgn ON pgn.oid = pgc.relnamespace
        WHERE c.table_name = $1
        AND c.table_schema = $2
        AND pgn.nspname = $2
        ORDER BY c.ordinal_position;
        """

        rows = await connection.fetch(query, table_name, schema_name)

        columns = []
        for row in rows:
            column = ColumnInfo(
                name=row["column_name"],
                data_type=row["data_type"],
                is_nullable=row["is_nullable"],
                default_value=row["column_default"],
                max_length=row["character_maximum_length"],
                precision=row["numeric_precision"],
                scale=row["numeric_scale"],
                comment=row["column_comment"]
            )
            columns.append(column)

        return columns

    async def _get_table_indexes(
        self,
        connection: asyncpg.Connection,
        table_name: str,
        schema_name: str
    ) -> List[IndexInfo]:
        """Get index information for a table."""
        query = """
        SELECT
            i.indexname,
            i.indexdef,
            ix.indisunique,
            ix.indisprimary
        FROM pg_indexes i
        JOIN pg_class c ON c.relname = i.tablename
        JOIN pg_index ix ON ix.indexrelid = (
            SELECT oid FROM pg_class WHERE relname = i.indexname
        )
        WHERE i.tablename = $1
        AND i.schemaname = $2;
        """

        rows = await connection.fetch(query, table_name, schema_name)

        indexes = []
        for row in rows:
            index = IndexInfo(
                name=row["indexname"],
                definition=row["indexdef"],
                is_unique=row["indisunique"],
                is_primary=row["indisprimary"]
            )
            indexes.append(index)

        return indexes

    async def _get_table_constraints(
        self,
        connection: asyncpg.Connection,
        table_name: str,
        schema_name: str
    ) -> List[ConstraintInfo]:
        """Get constraint information for a table."""
        query = """
        SELECT
            tc.constraint_name,
            tc.constraint_type,
            kcu.column_name,
            ccu.table_name AS foreign_table_name,
            ccu.column_name AS foreign_column_name
        FROM information_schema.table_constraints tc
        LEFT JOIN information_schema.key_column_usage kcu
            ON tc.constraint_name = kcu.constraint_name
        LEFT JOIN information_schema.constraint_column_usage ccu
            ON ccu.constraint_name = tc.constraint_name
        WHERE tc.table_name = $1
        AND tc.table_schema = $2;
        """

        rows = await connection.fetch(query, table_name, schema_name)

        constraints = []
        for row in rows:
            constraint = ConstraintInfo(
                name=row["constraint_name"],
                type=row["constraint_type"],
                column_name=row["column_name"],
                foreign_table=row["foreign_table_name"],
                foreign_column=row["foreign_column_name"]
            )
            constraints.append(constraint)

        return constraints

    async def _get_table_statistics(
        self,
        connection: asyncpg.Connection,
        table_name: str,
        schema_name: str
    ) -> Dict[str, Any]:
        """Get table statistics."""
        try:
            query = """
            SELECT
                schemaname,
                tablename,
                attname,
                n_distinct,
                most_common_vals,
                most_common_freqs,
                histogram_bounds
            FROM pg_stats
            WHERE tablename = $1
            AND schemaname = $2;
            """

            rows = await connection.fetch(query, table_name, schema_name)

            # Get table size
            size_query = """
            SELECT pg_total_relation_size($1::regclass) as table_size;
            """
            size_row = await connection.fetchrow(size_query, f"{schema_name}.{table_name}")

            # Get row count estimate
            count_query = """
            SELECT reltuples::bigint as row_count
            FROM pg_class
            WHERE relname = $1;
            """
            count_row = await connection.fetchrow(count_query, table_name)

            return {
                "table_size": size_row["table_size"] if size_row else 0,
                "row_count": count_row["row_count"] if count_row else 0,
                "statistics": [dict(row) for row in rows]
            }

        except Exception as e:
            logger.warning(
                "table_statistics_failed",
                table_name=f"{schema_name}.{table_name}",
                error=str(e)
            )
            return {"table_size": 0, "row_count": 0, "statistics": []}