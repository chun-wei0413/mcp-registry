"""MySQL MCP Server Core Exceptions"""


class MySQLMCPServerError(Exception):
    """Base exception for MySQL MCP Server"""
    pass


class ConnectionError(MySQLMCPServerError):
    """Connection related errors"""
    pass


class QueryExecutionError(MySQLMCPServerError):
    """Query execution errors"""
    pass


class SchemaInspectionError(MySQLMCPServerError):
    """Schema inspection errors"""
    pass


class SecurityError(MySQLMCPServerError):
    """Security related errors"""
    pass


class ConfigurationError(MySQLMCPServerError):
    """Configuration errors"""
    pass


class ValidationError(MySQLMCPServerError):
    """Data validation errors"""
    pass


class TransactionError(MySQLMCPServerError):
    """Transaction related errors"""
    pass