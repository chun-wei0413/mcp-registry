"""Connection management application service."""

from typing import List, Optional
import structlog

from ...core.interfaces import IConnectionManager, ISecurityValidator
from ...core.exceptions import ConnectionError, SecurityError
from ...domain.models import Connection
from ...domain.services import ConnectionValidator

logger = structlog.get_logger()


class ConnectionService:
    """Application service for connection management operations."""

    def __init__(
        self,
        connection_manager: IConnectionManager,
        security_validator: ISecurityValidator,
        connection_validator: ConnectionValidator
    ):
        self._connection_manager = connection_manager
        self._security_validator = security_validator
        self._connection_validator = connection_validator

    async def create_connection(
        self,
        connection_id: str,
        host: str,
        port: int,
        database: str,
        user: str,
        password: str,
        pool_size: int = 10
    ) -> bool:
        """Create a new database connection with full validation."""

        # Create connection entity
        connection = Connection(
            connection_id=connection_id,
            host=host,
            port=port,
            database=database,
            user=user,
            encrypted_password=password,  # TODO: Implement encryption
            pool_size=pool_size
        )

        # Domain validation
        if not self._connection_validator.validate_connection_parameters(connection):
            logger.warning(
                "connection_validation_failed",
                connection_id=connection_id,
                reason="domain_validation"
            )
            raise ConnectionError("Invalid connection parameters")

        # Security validation
        if not await self._security_validator.validate_connection(connection):
            logger.warning(
                "connection_security_failed",
                connection_id=connection_id,
                reason="security_validation"
            )
            raise SecurityError("Connection failed security validation")

        # Create connection through infrastructure
        try:
            success = await self._connection_manager.add_connection(connection)

            if success:
                logger.info(
                    "connection_created",
                    connection_id=connection_id,
                    host=host,
                    database=database
                )

            return success

        except Exception as e:
            logger.error(
                "connection_creation_failed",
                connection_id=connection_id,
                error=str(e)
            )
            raise ConnectionError(f"Failed to create connection: {e}")

    async def test_connection(self, connection_id: str) -> bool:
        """Test if a connection is healthy."""
        try:
            return await self._connection_manager.test_connection(connection_id)
        except Exception as e:
            logger.error(
                "connection_test_failed",
                connection_id=connection_id,
                error=str(e)
            )
            return False

    async def remove_connection(self, connection_id: str) -> bool:
        """Remove a database connection."""
        try:
            success = await self._connection_manager.remove_connection(connection_id)

            if success:
                logger.info("connection_removed", connection_id=connection_id)

            return success

        except Exception as e:
            logger.error(
                "connection_removal_failed",
                connection_id=connection_id,
                error=str(e)
            )
            return False

    async def get_connection(self, connection_id: str) -> Optional[Connection]:
        """Get connection information."""
        return await self._connection_manager.get_connection(connection_id)

    async def list_connections(self) -> List[Connection]:
        """List all active connections."""
        return await self._connection_manager.list_connections()