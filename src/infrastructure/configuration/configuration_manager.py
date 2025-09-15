"""Configuration management implementation."""

import os
from typing import Dict, Any, Optional
import json
import structlog

from ...core.interfaces import IConfigurationManager
from ...core.exceptions import ConfigurationError

logger = structlog.get_logger()


class EnvironmentConfigurationManager(IConfigurationManager):
    """Environment-based configuration manager implementation."""

    def __init__(self):
        self._config_cache = {}
        self._defaults = {
            "server": {
                "host": "0.0.0.0",
                "port": 3000,
                "debug": False,
                "log_level": "INFO"
            },
            "database": {
                "pool_size": 10,
                "pool_timeout": 30,
                "query_timeout": 60,
                "max_connections": 100
            },
            "security": {
                "readonly_mode": False,
                "max_query_length": 10000,
                "allowed_operations": ["SELECT", "INSERT", "UPDATE", "DELETE"],
                "blocked_keywords": ["DROP", "TRUNCATE", "ALTER"],
                "allow_localhost": True
            },
            "monitoring": {
                "health_check_timeout": 30,
                "metrics_history_size": 1000,
                "enable_query_logging": True
            }
        }

    async def get_server_config(self) -> Dict[str, Any]:
        """Get server configuration."""
        try:
            config = self._defaults["server"].copy()

            # Override with environment variables
            config.update({
                "host": os.getenv("MCP_SERVER_HOST", config["host"]),
                "port": int(os.getenv("MCP_SERVER_PORT", str(config["port"]))),
                "debug": os.getenv("MCP_DEBUG", "false").lower() == "true",
                "log_level": os.getenv("MCP_LOG_LEVEL", config["log_level"])
            })

            logger.debug("server_config_loaded", config=config)
            return config

        except (ValueError, TypeError) as e:
            logger.error("invalid_server_config", error=str(e))
            raise ConfigurationError(f"Invalid server configuration: {e}")

    async def get_database_config(self) -> Dict[str, Any]:
        """Get database configuration."""
        try:
            config = self._defaults["database"].copy()

            # Override with environment variables
            config.update({
                "pool_size": int(os.getenv("DB_POOL_SIZE", str(config["pool_size"]))),
                "pool_timeout": int(os.getenv("DB_POOL_TIMEOUT", str(config["pool_timeout"]))),
                "query_timeout": int(os.getenv("DB_QUERY_TIMEOUT", str(config["query_timeout"]))),
                "max_connections": int(os.getenv("DB_MAX_CONNECTIONS", str(config["max_connections"])))
            })

            logger.debug("database_config_loaded", config=config)
            return config

        except (ValueError, TypeError) as e:
            logger.error("invalid_database_config", error=str(e))
            raise ConfigurationError(f"Invalid database configuration: {e}")

    async def get_security_config(self) -> Dict[str, Any]:
        """Get security configuration."""
        try:
            config = self._defaults["security"].copy()

            # Override with environment variables
            config.update({
                "readonly_mode": os.getenv("SECURITY_READONLY", "false").lower() == "true",
                "max_query_length": int(os.getenv("SECURITY_MAX_QUERY_LENGTH", str(config["max_query_length"]))),
                "allow_localhost": os.getenv("SECURITY_ALLOW_LOCALHOST", "true").lower() == "true"
            })

            # Handle JSON arrays for operations and keywords
            allowed_ops_env = os.getenv("SECURITY_ALLOWED_OPERATIONS")
            if allowed_ops_env:
                try:
                    config["allowed_operations"] = json.loads(allowed_ops_env)
                except json.JSONDecodeError:
                    config["allowed_operations"] = allowed_ops_env.split(",")

            blocked_keywords_env = os.getenv("SECURITY_BLOCKED_KEYWORDS")
            if blocked_keywords_env:
                try:
                    config["blocked_keywords"] = json.loads(blocked_keywords_env)
                except json.JSONDecodeError:
                    config["blocked_keywords"] = blocked_keywords_env.split(",")

            logger.debug("security_config_loaded", config=config)
            return config

        except (ValueError, TypeError) as e:
            logger.error("invalid_security_config", error=str(e))
            raise ConfigurationError(f"Invalid security configuration: {e}")

    async def get_monitoring_config(self) -> Dict[str, Any]:
        """Get monitoring configuration."""
        try:
            config = self._defaults["monitoring"].copy()

            # Override with environment variables
            config.update({
                "health_check_timeout": int(os.getenv("MONITORING_HEALTH_TIMEOUT", str(config["health_check_timeout"]))),
                "metrics_history_size": int(os.getenv("MONITORING_METRICS_HISTORY", str(config["metrics_history_size"]))),
                "enable_query_logging": os.getenv("MONITORING_QUERY_LOGGING", "true").lower() == "true"
            })

            logger.debug("monitoring_config_loaded", config=config)
            return config

        except (ValueError, TypeError) as e:
            logger.error("invalid_monitoring_config", error=str(e))
            raise ConfigurationError(f"Invalid monitoring configuration: {e}")

    async def get_config(self, section: str) -> Dict[str, Any]:
        """Get configuration for a specific section."""
        try:
            if section in self._config_cache:
                return self._config_cache[section]

            if section == "server":
                config = await self.get_server_config()
            elif section == "database":
                config = await self.get_database_config()
            elif section == "security":
                config = await self.get_security_config()
            elif section == "monitoring":
                config = await self.get_monitoring_config()
            else:
                raise ConfigurationError(f"Unknown configuration section: {section}")

            self._config_cache[section] = config
            return config

        except Exception as e:
            logger.error("config_get_failed", section=section, error=str(e))
            raise ConfigurationError(f"Failed to get configuration for {section}: {e}")

    async def validate_config(self) -> bool:
        """Validate all configuration sections."""
        try:
            # Validate all sections
            sections = ["server", "database", "security", "monitoring"]
            for section in sections:
                await self.get_config(section)

            logger.info("configuration_validated", sections=sections)
            return True

        except Exception as e:
            logger.error("configuration_validation_failed", error=str(e))
            return False

    async def reload_config(self) -> None:
        """Reload configuration from environment."""
        try:
            self._config_cache.clear()
            logger.info("configuration_reloaded")

        except Exception as e:
            logger.error("config_reload_failed", error=str(e))
            raise ConfigurationError(f"Failed to reload configuration: {e}")

    async def load_config(self) -> Dict[str, Any]:
        """Load all configuration sections."""
        try:
            server_config = await self.get_server_config()
            database_config = await self.get_database_config()
            security_config = await self.get_security_config()
            monitoring_config = await self.get_monitoring_config()

            config = {
                "server": server_config,
                "database": database_config,
                "security": security_config,
                "monitoring": monitoring_config
            }

            logger.debug("full_config_loaded", sections=list(config.keys()))
            return config

        except Exception as e:
            logger.error("load_config_failed", error=str(e))
            raise ConfigurationError(f"Failed to load configuration: {e}")

    async def get_connection_string(
        self,
        host: str,
        port: int,
        database: str,
        user: str,
        password: str,
        **kwargs
    ) -> str:
        """Build PostgreSQL connection string."""
        try:
            # Base connection parameters
            params = {
                "host": host,
                "port": port,
                "database": database,
                "user": user,
                "password": password
            }

            # Add additional parameters
            params.update(kwargs)

            # Build connection string
            param_string = "&".join(f"{k}={v}" for k, v in params.items())
            connection_string = f"postgresql://{user}:{password}@{host}:{port}/{database}"

            # Add query parameters if any additional kwargs
            if kwargs:
                additional_params = "&".join(f"{k}={v}" for k, v in kwargs.items())
                connection_string += f"?{additional_params}"

            logger.debug(
                "connection_string_built",
                host=host,
                port=port,
                database=database,
                user=user
            )

            return connection_string

        except Exception as e:
            logger.error(
                "connection_string_build_failed",
                host=host,
                port=port,
                database=database,
                error=str(e)
            )
            raise ConfigurationError(f"Failed to build connection string: {e}")