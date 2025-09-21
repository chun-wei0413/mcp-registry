"""MySQL MCP Server Core Interfaces"""

from abc import ABC, abstractmethod
from datetime import datetime
from typing import Any, Dict, List, Optional

from pydantic import BaseModel


class HealthStatus(BaseModel):
    """Health status model"""
    is_healthy: bool
    response_time_ms: float
    last_check: datetime
    details: Dict[str, Any] = {}


class ConnectionMetrics(BaseModel):
    """Connection-specific metrics"""
    connection_id: str
    total_queries: int = 0
    successful_queries: int = 0
    failed_queries: int = 0
    average_execution_time_ms: float = 0.0
    last_query_time: Optional[datetime] = None
    query_history: List[Dict[str, Any]] = []


class GlobalMetrics(BaseModel):
    """Global server metrics"""
    total_connections: int = 0
    active_connections: int = 0
    total_queries: int = 0
    successful_queries: int = 0
    failed_queries: int = 0
    average_execution_time_ms: float = 0.0
    success_rate_percent: float = 0.0
    uptime_seconds: float = 0.0


class TableColumn(BaseModel):
    """Table column information"""
    name: str
    data_type: str
    is_nullable: bool
    default_value: Optional[str] = None
    max_length: Optional[int] = None
    precision: Optional[int] = None
    scale: Optional[int] = None
    comment: Optional[str] = None


class TableIndex(BaseModel):
    """Table index information"""
    name: str
    definition: str
    is_unique: bool = False
    is_primary: bool = False


class TableConstraint(BaseModel):
    """Table constraint information"""
    name: str
    type: str  # PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK
    column_name: Optional[str] = None
    foreign_table: Optional[str] = None
    foreign_column: Optional[str] = None


class TableSchema(BaseModel):
    """Complete table schema"""
    table_name: str
    schema_name: str = "public"
    columns: List[TableColumn] = []
    indexes: List[TableIndex] = []
    constraints: List[TableConstraint] = []
    row_count: int = 0
    table_size_bytes: int = 0


class IConnectionPool(ABC):
    """MySQL connection pool interface"""

    @abstractmethod
    async def create_connection(
        self,
        connection_id: str,
        host: str,
        port: int,
        user: str,
        password: str,
        database: str,
        pool_size: int = 10
    ) -> None:
        """Create a new connection pool"""
        pass

    @abstractmethod
    async def get_connection(self, connection_id: str):
        """Get a connection from pool"""
        pass

    @abstractmethod
    async def remove_connection(self, connection_id: str) -> bool:
        """Remove a connection pool"""
        pass

    @abstractmethod
    async def test_connection(self, connection_id: str) -> bool:
        """Test if connection is alive"""
        pass

    @abstractmethod
    async def list_connections(self) -> List[str]:
        """List all connection IDs"""
        pass

    @abstractmethod
    async def cleanup(self) -> None:
        """Cleanup all connections"""
        pass


class IQueryExecutor(ABC):
    """MySQL query executor interface"""

    @abstractmethod
    async def execute_query(
        self,
        connection_id: str,
        query: str,
        params: Optional[List[Any]] = None
    ) -> Dict[str, Any]:
        """Execute a single query"""
        pass

    @abstractmethod
    async def execute_transaction(
        self,
        connection_id: str,
        queries: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """Execute multiple queries in transaction"""
        pass

    @abstractmethod
    async def execute_batch(
        self,
        connection_id: str,
        query: str,
        params_list: List[List[Any]]
    ) -> Dict[str, Any]:
        """Execute batch operations"""
        pass


class ISchemaInspector(ABC):
    """MySQL schema inspection interface"""

    @abstractmethod
    async def get_table_schema(
        self,
        connection_id: str,
        table_name: str,
        schema_name: str = "public"
    ) -> TableSchema:
        """Get complete table schema"""
        pass

    @abstractmethod
    async def list_tables(
        self,
        connection_id: str,
        schema_name: Optional[str] = None
    ) -> List[str]:
        """List all tables in schema"""
        pass

    @abstractmethod
    async def list_schemas(self, connection_id: str) -> List[str]:
        """List all schemas/databases"""
        pass


class IHealthChecker(ABC):
    """Health checker interface"""

    @abstractmethod
    async def check_connection_health(self, connection_id: str) -> HealthStatus:
        """Check specific connection health"""
        pass

    @abstractmethod
    async def check_system_health(self) -> HealthStatus:
        """Check overall system health"""
        pass


class IMetricsCollector(ABC):
    """Metrics collector interface"""

    @abstractmethod
    async def record_query(
        self,
        connection_id: str,
        query: str,
        execution_time_ms: float,
        success: bool,
        error_message: Optional[str] = None
    ) -> None:
        """Record query execution metrics"""
        pass

    @abstractmethod
    async def get_connection_metrics(self, connection_id: str) -> ConnectionMetrics:
        """Get metrics for specific connection"""
        pass

    @abstractmethod
    async def get_global_metrics(self) -> Dict[str, Any]:
        """Get global server metrics"""
        pass

    @abstractmethod
    async def reset_metrics(self, connection_id: Optional[str] = None) -> None:
        """Reset metrics"""
        pass