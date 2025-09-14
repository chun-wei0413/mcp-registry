"""
Core abstractions and interfaces for the PostgreSQL MCP Server.
"""

from .interfaces import (
    IConnectionManager,
    IQueryExecutor,
    ISecurityValidator,
    IHealthChecker,
    IMetricsCollector,
    IConfigurationManager,
    ISchemaInspector
)

from .exceptions import (
    ConnectionError,
    QueryError,
    SecurityError,
    ConfigurationError,
    MonitoringError
)

__all__ = [
    # Interfaces
    "IConnectionManager",
    "IQueryExecutor",
    "ISecurityValidator",
    "IHealthChecker",
    "IMetricsCollector",
    "IConfigurationManager",
    "ISchemaInspector",

    # Exceptions
    "ConnectionError",
    "QueryError",
    "SecurityError",
    "ConfigurationError",
    "MonitoringError"
]