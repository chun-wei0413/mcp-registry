"""PostgreSQL MCP Server - SOLID Architecture Implementation"""

from .core import (
    IConnectionManager,
    IQueryExecutor,
    ISecurityValidator,
    IHealthChecker,
    IMetricsCollector,
    IConfigurationManager,
    ISchemaInspector,
    ConnectionError,
    QueryError,
    SecurityError,
    ConfigurationError,
    MonitoringError
)

from .domain import (
    Connection,
    Query,
    QueryResult,
    TableSchema,
    ColumnInfo,
    IndexInfo,
    ConstraintInfo,
    HealthStatus,
    MetricsData,
    QueryBuilder,
    ConnectionValidator
)

from .infrastructure import (
    PostgreSQLConnectionPool,
    PostgreSQLQueryExecutor,
    PostgreSQLSecurityValidator,
    PostgreSQLHealthChecker,
    InMemoryMetricsCollector,
    EnvironmentConfigurationManager,
    PostgreSQLSchemaInspector
)

from .application import (
    ConnectionService,
    QueryService
)

from .api import (
    ConnectionHandler,
    QueryHandler,
    DependencyFactory,
    get_dependency_factory,
    cleanup_dependency_factory
)

# Version info
__version__ = "1.0.0"
__author__ = "Frank Li"
__email__ = "a910413frank@gmail.com"

__all__ = [
    # Core interfaces
    "IConnectionManager",
    "IQueryExecutor",
    "ISecurityValidator",
    "IHealthChecker",
    "IMetricsCollector",
    "IConfigurationManager",
    "ISchemaInspector",

    # Core exceptions
    "ConnectionError",
    "QueryError",
    "SecurityError",
    "ConfigurationError",
    "MonitoringError",

    # Domain models
    "Connection",
    "Query",
    "QueryResult",
    "TableSchema",
    "ColumnInfo",
    "IndexInfo",
    "ConstraintInfo",
    "HealthStatus",
    "MetricsData",

    # Domain services
    "QueryBuilder",
    "ConnectionValidator",

    # Infrastructure implementations
    "PostgreSQLConnectionPool",
    "PostgreSQLQueryExecutor",
    "PostgreSQLSecurityValidator",
    "PostgreSQLHealthChecker",
    "InMemoryMetricsCollector",
    "EnvironmentConfigurationManager",
    "PostgreSQLSchemaInspector",

    # Application services
    "ConnectionService",
    "QueryService",

    # API handlers
    "ConnectionHandler",
    "QueryHandler",
    "DependencyFactory",
    "get_dependency_factory",
    "cleanup_dependency_factory"
]