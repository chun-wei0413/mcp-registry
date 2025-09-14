from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional, Protocol
from datetime import datetime

from ..domain.models import Connection, Query, QueryResult, TableSchema, HealthStatus, MetricsData


class IConnectionManager(ABC):
    """Interface for database connection management."""

    @abstractmethod
    async def add_connection(self, connection: Connection) -> bool:
        """Add a new database connection."""
        pass

    @abstractmethod
    async def remove_connection(self, connection_id: str) -> bool:
        """Remove a database connection."""
        pass

    @abstractmethod
    async def test_connection(self, connection_id: str) -> bool:
        """Test if a connection is healthy."""
        pass

    @abstractmethod
    async def get_connection(self, connection_id: str) -> Optional[Connection]:
        """Get connection information."""
        pass

    @abstractmethod
    async def list_connections(self) -> List[Connection]:
        """List all active connections."""
        pass


class IQueryExecutor(ABC):
    """Interface for SQL query execution."""

    @abstractmethod
    async def execute_query(self, connection_id: str, query: Query) -> QueryResult:
        """Execute a single SQL query."""
        pass

    @abstractmethod
    async def execute_transaction(self, connection_id: str, queries: List[Query]) -> QueryResult:
        """Execute multiple queries in a transaction."""
        pass

    @abstractmethod
    async def execute_batch(self, connection_id: str, query: Query, params_list: List[List[Any]]) -> QueryResult:
        """Execute a query with multiple parameter sets."""
        pass


class ISecurityValidator(ABC):
    """Interface for security validation."""

    @abstractmethod
    async def validate_query(self, query: Query) -> bool:
        """Validate a query for security compliance."""
        pass

    @abstractmethod
    async def validate_connection(self, connection: Connection) -> bool:
        """Validate connection parameters for security."""
        pass


class IHealthChecker(ABC):
    """Interface for health monitoring."""

    @abstractmethod
    async def check_health(self) -> HealthStatus:
        """Check overall system health."""
        pass

    @abstractmethod
    async def check_connection_health(self, connection_id: str) -> bool:
        """Check specific connection health."""
        pass


class IMetricsCollector(ABC):
    """Interface for metrics collection."""

    @abstractmethod
    async def collect_metrics(self) -> MetricsData:
        """Collect system metrics."""
        pass

    @abstractmethod
    async def record_query_execution(self, connection_id: str, duration: float, success: bool) -> None:
        """Record query execution metrics."""
        pass


class IConfigurationManager(ABC):
    """Interface for configuration management."""

    @abstractmethod
    def load_config(self) -> Dict[str, Any]:
        """Load configuration from various sources."""
        pass

    @abstractmethod
    def validate_config(self) -> bool:
        """Validate loaded configuration."""
        pass

    @abstractmethod
    def get_database_config(self) -> Dict[str, Any]:
        """Get database-specific configuration."""
        pass

    @abstractmethod
    def get_security_config(self) -> Dict[str, Any]:
        """Get security-specific configuration."""
        pass


class ISchemaInspector(ABC):
    """Interface for database schema inspection."""

    @abstractmethod
    async def get_table_schema(self, connection_id: str, table_name: str, schema: str = "public") -> TableSchema:
        """Get detailed table schema information."""
        pass

    @abstractmethod
    async def list_tables(self, connection_id: str, schema: str = "public") -> List[str]:
        """List all tables in a schema."""
        pass

    @abstractmethod
    async def explain_query(self, connection_id: str, query: Query) -> Dict[str, Any]:
        """Get query execution plan."""
        pass