from typing import List, Dict, Any
from .models import Connection, Query


class ConnectionValidator:
    """Domain service for connection validation."""

    @staticmethod
    def validate_connection_parameters(connection: Connection) -> bool:
        """Validate connection parameters according to business rules."""
        if not connection.host or not connection.database:
            return False

        if not (1 <= connection.port <= 65535):
            return False

        if not connection.user:
            return False

        if not (1 <= connection.pool_size <= 100):
            return False

        return True


class QueryBuilder:
    """Domain service for safe query construction."""

    @staticmethod
    def build_parameterized_query(sql_template: str, params: List[Any]) -> Query:
        """Build a parameterized query with proper parameter binding."""
        return Query(sql=sql_template, params=params)

    @staticmethod
    def validate_query_structure(query: Query) -> bool:
        """Validate query structure according to business rules."""
        if not query.sql or not query.sql.strip():
            return False

        # Basic validation - ensure parameters match placeholders
        placeholder_count = query.sql.count('$')
        if placeholder_count != len(query.params):
            return False

        return True


class SchemaAnalyzer:
    """Domain service for schema analysis."""

    @staticmethod
    def analyze_table_relationships(schemas: List[Dict[str, Any]]) -> Dict[str, List[str]]:
        """Analyze relationships between database tables."""
        relationships = {}

        for schema in schemas:
            table_name = schema.get('table_name')
            foreign_keys = schema.get('foreign_keys', [])

            relationships[table_name] = [
                fk.get('referenced_table') for fk in foreign_keys
                if fk.get('referenced_table')
            ]

        return relationships

    @staticmethod
    def calculate_schema_complexity(schema: Dict[str, Any]) -> int:
        """Calculate complexity score for a table schema."""
        complexity = 0

        # Base complexity from column count
        columns = schema.get('columns', [])
        complexity += len(columns)

        # Additional complexity from constraints
        foreign_keys = schema.get('foreign_keys', [])
        complexity += len(foreign_keys) * 2

        indexes = schema.get('indexes', [])
        complexity += len(indexes)

        return complexity