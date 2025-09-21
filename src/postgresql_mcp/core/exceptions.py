class PostgreSQLMCPError(Exception):
    """Base exception for PostgreSQL MCP Server."""
    pass


class ConnectionError(PostgreSQLMCPError):
    """Raised when database connection operations fail."""
    pass


class QueryError(PostgreSQLMCPError):
    """Raised when query execution fails."""
    pass


class SecurityError(PostgreSQLMCPError):
    """Raised when security validation fails."""
    pass


class ConfigurationError(PostgreSQLMCPError):
    """Raised when configuration is invalid."""
    pass


class MonitoringError(PostgreSQLMCPError):
    """Raised when monitoring operations fail."""
    pass


class ValidationError(PostgreSQLMCPError):
    """Raised when data validation fails."""
    pass


class TimeoutError(PostgreSQLMCPError):
    """Raised when operations timeout."""
    pass