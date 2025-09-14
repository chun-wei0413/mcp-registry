"""Security validation implementation."""

import re
from typing import List, Set
import structlog

from ...core.interfaces import ISecurityValidator
from ...core.exceptions import SecurityError
from ...domain.models import Query, Connection

logger = structlog.get_logger()


class PostgreSQLSecurityValidator(ISecurityValidator):
    """PostgreSQL security validator implementation."""

    def __init__(
        self,
        readonly_mode: bool = False,
        allowed_operations: List[str] = None,
        blocked_keywords: List[str] = None,
        max_query_length: int = 10000
    ):
        self._readonly_mode = readonly_mode
        self._allowed_operations = set(allowed_operations or [
            'SELECT', 'WITH', 'EXPLAIN'
        ])
        self._blocked_keywords = set(keyword.upper() for keyword in (blocked_keywords or [
            'DROP', 'DELETE', 'TRUNCATE', 'ALTER'
        ]))
        self._max_query_length = max_query_length

        # SQL injection patterns
        self._injection_patterns = [
            r"'[^']*'[^']*'",  # Potential string escape
            r";[\s]*\w+",      # Command chaining
            r"--[\s]*\w+",     # SQL comments
            r"/\*.*?\*/",      # Block comments
            r"\bunion\b.*\bselect\b",  # UNION injection
            r"\bor\b.*\b=\b.*\bor\b",  # OR-based injection
        ]

    async def validate_query(self, query: Query) -> bool:
        """Validate a query for security compliance."""
        try:
            # Length validation
            if len(query.sql) > self._max_query_length:
                logger.warning(
                    "query_too_long",
                    query_id=query.query_id,
                    length=len(query.sql),
                    max_length=self._max_query_length
                )
                return False

            # Extract SQL operation
            sql_upper = query.sql.strip().upper()
            operation = sql_upper.split()[0] if sql_upper else ""

            # Operation validation
            if self._readonly_mode and operation not in {'SELECT', 'WITH', 'EXPLAIN'}:
                logger.warning(
                    "readonly_violation",
                    query_id=query.query_id,
                    operation=operation
                )
                return False

            if operation not in self._allowed_operations:
                logger.warning(
                    "operation_not_allowed",
                    query_id=query.query_id,
                    operation=operation,
                    allowed=list(self._allowed_operations)
                )
                return False

            # Blocked keywords check
            for keyword in self._blocked_keywords:
                if keyword in sql_upper:
                    logger.warning(
                        "blocked_keyword_found",
                        query_id=query.query_id,
                        keyword=keyword
                    )
                    return False

            # SQL injection pattern check
            for pattern in self._injection_patterns:
                if re.search(pattern, sql_upper, re.IGNORECASE):
                    logger.warning(
                        "potential_injection",
                        query_id=query.query_id,
                        pattern=pattern
                    )
                    return False

            logger.debug(
                "query_validated",
                query_id=query.query_id,
                operation=operation
            )

            return True

        except Exception as e:
            logger.error(
                "validation_error",
                query_id=query.query_id,
                error=str(e)
            )
            return False

    async def validate_connection(self, connection: Connection) -> bool:
        """Validate connection parameters for security."""
        try:
            # Host validation - prevent connecting to localhost in production
            if connection.host in ['localhost', '127.0.0.1'] and not self._allow_localhost():
                logger.warning(
                    "localhost_connection_blocked",
                    connection_id=connection.connection_id
                )
                return False

            # Port validation
            if not (1 <= connection.port <= 65535):
                logger.warning(
                    "invalid_port",
                    connection_id=connection.connection_id,
                    port=connection.port
                )
                return False

            # Database name validation
            if not self._is_valid_identifier(connection.database):
                logger.warning(
                    "invalid_database_name",
                    connection_id=connection.connection_id,
                    database=connection.database
                )
                return False

            # User name validation
            if not self._is_valid_identifier(connection.user):
                logger.warning(
                    "invalid_username",
                    connection_id=connection.connection_id,
                    user=connection.user
                )
                return False

            logger.debug(
                "connection_validated",
                connection_id=connection.connection_id
            )

            return True

        except Exception as e:
            logger.error(
                "connection_validation_error",
                connection_id=connection.connection_id,
                error=str(e)
            )
            return False

    def _allow_localhost(self) -> bool:
        """Check if localhost connections are allowed."""
        # In production, this should return False
        import os
        return os.getenv("ENVIRONMENT", "development") == "development"

    def _is_valid_identifier(self, identifier: str) -> bool:
        """Validate SQL identifier for safety."""
        if not identifier or len(identifier) > 63:
            return False

        # PostgreSQL identifier rules
        pattern = r'^[a-zA-Z_][a-zA-Z0-9_]*$'
        return re.match(pattern, identifier) is not None