"""Dependency injection factory for creating service instances."""

from typing import Dict, Any
import structlog

from ..core.interfaces import (
    IConnectionManager,
    IQueryExecutor,
    ISecurityValidator,
    IHealthChecker,
    IMetricsCollector,
    IConfigurationManager,
    ISchemaInspector
)

from ..infrastructure import (
    PostgreSQLConnectionPool,
    PostgreSQLQueryExecutor,
    PostgreSQLSecurityValidator,
    PostgreSQLHealthChecker,
    InMemoryMetricsCollector,
    EnvironmentConfigurationManager,
    PostgreSQLSchemaInspector
)

from ..domain.services import QueryBuilder, ConnectionValidator
from ..application.services import ConnectionService, QueryService
from .mcp_handlers import ConnectionHandler, QueryHandler

logger = structlog.get_logger()


class DependencyFactory:
    """Factory for creating and injecting dependencies following SOLID principles."""

    def __init__(self):
        self._instances: Dict[str, Any] = {}
        self._config_manager: IConfigurationManager = None

    async def initialize(self) -> None:
        """Initialize the dependency factory."""
        try:
            # Create configuration manager first
            self._config_manager = EnvironmentConfigurationManager()
            await self._config_manager.validate_config()

            logger.info("dependency_factory_initialized")

        except Exception as e:
            logger.error("dependency_factory_init_failed", error=str(e))
            raise

    async def get_config_manager(self) -> IConfigurationManager:
        """Get configuration manager instance."""
        if not self._config_manager:
            await self.initialize()
        return self._config_manager

    async def get_connection_manager(self) -> IConnectionManager:
        """Get connection manager instance."""
        if "connection_manager" not in self._instances:
            self._instances["connection_manager"] = PostgreSQLConnectionPool()

        return self._instances["connection_manager"]

    async def get_security_validator(self) -> ISecurityValidator:
        """Get security validator instance."""
        if "security_validator" not in self._instances:
            config_manager = await self.get_config_manager()
            security_config = await config_manager.get_security_config()

            self._instances["security_validator"] = PostgreSQLSecurityValidator(
                readonly_mode=security_config["readonly_mode"],
                allowed_operations=security_config["allowed_operations"],
                blocked_keywords=security_config["blocked_keywords"],
                max_query_length=security_config["max_query_length"]
            )

        return self._instances["security_validator"]

    async def get_query_executor(self) -> IQueryExecutor:
        """Get query executor instance."""
        if "query_executor" not in self._instances:
            connection_manager = await self.get_connection_manager()
            config_manager = await self.get_config_manager()
            db_config = await config_manager.get_database_config()

            self._instances["query_executor"] = PostgreSQLQueryExecutor(
                connection_manager=connection_manager,
                query_timeout=db_config["query_timeout"]
            )

        return self._instances["query_executor"]

    async def get_metrics_collector(self) -> IMetricsCollector:
        """Get metrics collector instance."""
        if "metrics_collector" not in self._instances:
            config_manager = await self.get_config_manager()
            monitoring_config = await config_manager.get_monitoring_config()

            self._instances["metrics_collector"] = InMemoryMetricsCollector(
                max_history=monitoring_config["metrics_history_size"]
            )

        return self._instances["metrics_collector"]

    async def get_health_checker(self) -> IHealthChecker:
        """Get health checker instance."""
        if "health_checker" not in self._instances:
            connection_manager = await self.get_connection_manager()
            config_manager = await self.get_config_manager()
            monitoring_config = await config_manager.get_monitoring_config()

            self._instances["health_checker"] = PostgreSQLHealthChecker(
                connection_manager=connection_manager,
                timeout_seconds=monitoring_config["health_check_timeout"]
            )

        return self._instances["health_checker"]

    async def get_schema_inspector(self) -> ISchemaInspector:
        """Get schema inspector instance."""
        if "schema_inspector" not in self._instances:
            connection_manager = await self.get_connection_manager()

            self._instances["schema_inspector"] = PostgreSQLSchemaInspector(
                connection_manager=connection_manager
            )

        return self._instances["schema_inspector"]

    async def get_query_builder(self) -> QueryBuilder:
        """Get query builder domain service."""
        if "query_builder" not in self._instances:
            self._instances["query_builder"] = QueryBuilder()

        return self._instances["query_builder"]

    async def get_connection_validator(self) -> ConnectionValidator:
        """Get connection validator domain service."""
        if "connection_validator" not in self._instances:
            self._instances["connection_validator"] = ConnectionValidator()

        return self._instances["connection_validator"]

    async def get_connection_service(self) -> ConnectionService:
        """Get connection application service."""
        if "connection_service" not in self._instances:
            connection_manager = await self.get_connection_manager()
            security_validator = await self.get_security_validator()
            connection_validator = await self.get_connection_validator()

            self._instances["connection_service"] = ConnectionService(
                connection_manager=connection_manager,
                security_validator=security_validator,
                connection_validator=connection_validator
            )

        return self._instances["connection_service"]

    async def get_query_service(self) -> QueryService:
        """Get query application service."""
        if "query_service" not in self._instances:
            query_executor = await self.get_query_executor()
            security_validator = await self.get_security_validator()
            metrics_collector = await self.get_metrics_collector()
            query_builder = await self.get_query_builder()

            self._instances["query_service"] = QueryService(
                query_executor=query_executor,
                security_validator=security_validator,
                metrics_collector=metrics_collector,
                query_builder=query_builder
            )

        return self._instances["query_service"]

    async def get_connection_handler(self) -> ConnectionHandler:
        """Get connection MCP handler."""
        if "connection_handler" not in self._instances:
            connection_service = await self.get_connection_service()

            self._instances["connection_handler"] = ConnectionHandler(
                connection_service=connection_service
            )

        return self._instances["connection_handler"]

    async def get_query_handler(self) -> QueryHandler:
        """Get query MCP handler."""
        if "query_handler" not in self._instances:
            query_service = await self.get_query_service()

            self._instances["query_handler"] = QueryHandler(
                query_service=query_service
            )

        return self._instances["query_handler"]

    async def cleanup(self) -> None:
        """Cleanup all instances and resources."""
        try:
            # Cleanup connection manager
            if "connection_manager" in self._instances:
                connection_manager = self._instances["connection_manager"]
                if hasattr(connection_manager, 'cleanup'):
                    await connection_manager.cleanup()

            # Clear all instances
            self._instances.clear()

            logger.info("dependency_factory_cleaned_up")

        except Exception as e:
            logger.error("dependency_factory_cleanup_failed", error=str(e))
            raise

    def get_all_instances(self) -> Dict[str, Any]:
        """Get all created instances for debugging."""
        return self._instances.copy()


# Global factory instance
_dependency_factory = None


async def get_dependency_factory() -> DependencyFactory:
    """Get global dependency factory instance."""
    global _dependency_factory

    if _dependency_factory is None:
        _dependency_factory = DependencyFactory()
        await _dependency_factory.initialize()

    return _dependency_factory


async def cleanup_dependency_factory() -> None:
    """Cleanup global dependency factory."""
    global _dependency_factory

    if _dependency_factory is not None:
        await _dependency_factory.cleanup()
        _dependency_factory = None