"""Configuration management for PostgreSQL MCP Server."""

import os
from typing import Dict, Any, Optional
from pathlib import Path
import structlog
from dotenv import load_dotenv

from .models.types import ServerConfig, SecurityConfig

logger = structlog.get_logger()


class ConfigManager:
    """配置管理器"""

    def __init__(self, config_file: Optional[str] = None):
        # 載入環境變數
        if config_file and Path(config_file).exists():
            load_dotenv(config_file)
        else:
            # 嘗試載入預設的 .env 檔案
            for env_file in ['.env', '.env.local', 'config/.env']:
                if Path(env_file).exists():
                    load_dotenv(env_file)
                    logger.info("loaded_env_file", file=env_file)
                    break

        self.server_config = self._load_server_config()
        self.security_config = self._load_security_config()

    def _load_server_config(self) -> ServerConfig:
        """載入伺服器配置"""
        return ServerConfig(
            port=int(os.getenv("MCP_SERVER_PORT", "3000")),
            log_level=os.getenv("MCP_LOG_LEVEL", "INFO"),
            default_pool_size=int(os.getenv("DEFAULT_POOL_SIZE", "10")),
            query_timeout=int(os.getenv("QUERY_TIMEOUT", "30")),
            max_connections=int(os.getenv("MAX_CONNECTIONS", "100")),
            enable_query_cache=os.getenv("ENABLE_QUERY_CACHE", "false").lower() == "true",
            cache_ttl_seconds=int(os.getenv("CACHE_TTL_SECONDS", "300"))
        )

    def _load_security_config(self) -> SecurityConfig:
        """載入安全配置"""
        # 解析允許的操作
        allowed_operations_str = os.getenv("ALLOWED_OPERATIONS", "SELECT,INSERT,UPDATE,DELETE")
        allowed_operations = [op.strip().upper() for op in allowed_operations_str.split(",")]

        # 解析被阻擋的關鍵字
        blocked_keywords_str = os.getenv("BLOCKED_KEYWORDS", "DROP,TRUNCATE,ALTER")
        blocked_keywords = [kw.strip().upper() for kw in blocked_keywords_str.split(",")]

        return SecurityConfig(
            encryption_key=os.getenv("POSTGRES_MCP_ENCRYPTION_KEY"),
            readonly_mode=os.getenv("READONLY_MODE", "false").lower() == "true",
            allowed_operations=allowed_operations,
            blocked_keywords=blocked_keywords,
            max_query_length=int(os.getenv("MAX_QUERY_LENGTH", "10000")),
            enable_query_logging=os.getenv("ENABLE_QUERY_LOGGING", "true").lower() == "true"
        )

    def get_server_config(self) -> ServerConfig:
        """取得伺服器配置"""
        return self.server_config

    def get_security_config(self) -> SecurityConfig:
        """取得安全配置"""
        return self.security_config

    def get_database_url(self, connection_id: str) -> Optional[str]:
        """取得資料庫連線 URL"""
        url_key = f"DATABASE_URL_{connection_id.upper()}"
        return os.getenv(url_key) or os.getenv("DATABASE_URL")

    def get_connection_config(self, connection_id: str) -> Dict[str, Any]:
        """取得特定連線的配置"""
        prefix = f"DB_{connection_id.upper()}_"

        return {
            "host": os.getenv(f"{prefix}HOST", os.getenv("DB_HOST", "localhost")),
            "port": int(os.getenv(f"{prefix}PORT", os.getenv("DB_PORT", "5432"))),
            "database": os.getenv(f"{prefix}DATABASE", os.getenv("DB_DATABASE")),
            "user": os.getenv(f"{prefix}USER", os.getenv("DB_USER")),
            "password": os.getenv(f"{prefix}PASSWORD", os.getenv("DB_PASSWORD")),
            "pool_size": int(os.getenv(f"{prefix}POOL_SIZE", os.getenv("DB_POOL_SIZE", "10")))
        }

    def setup_logging(self):
        """設定日誌系統"""
        import logging

        # 設定 structlog
        logging.basicConfig(
            level=getattr(logging, self.server_config.log_level),
            format="%(message)s"
        )

        structlog.configure(
            processors=[
                structlog.stdlib.filter_by_level,
                structlog.stdlib.add_logger_name,
                structlog.stdlib.add_log_level,
                structlog.stdlib.PositionalArgumentsFormatter(),
                structlog.processors.StackInfoRenderer(),
                structlog.processors.format_exc_info,
                structlog.processors.UnicodeDecoder(),
                structlog.processors.JSONRenderer()
            ],
            context_class=dict,
            logger_factory=structlog.stdlib.LoggerFactory(),
            wrapper_class=structlog.stdlib.BoundLogger,
            cache_logger_on_first_use=True,
        )

        logger.info(
            "logging_configured",
            log_level=self.server_config.log_level,
            enable_query_logging=self.security_config.enable_query_logging
        )

    def validate_config(self) -> bool:
        """驗證配置的有效性"""
        try:
            # 檢查必要的配置項
            if self.server_config.port <= 0 or self.server_config.port > 65535:
                logger.error("invalid_server_port", port=self.server_config.port)
                return False

            if self.server_config.query_timeout <= 0:
                logger.error("invalid_query_timeout", timeout=self.server_config.query_timeout)
                return False

            if self.security_config.max_query_length <= 0:
                logger.error("invalid_max_query_length", length=self.security_config.max_query_length)
                return False

            # 檢查日誌級別
            valid_log_levels = ["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"]
            if self.server_config.log_level not in valid_log_levels:
                logger.error("invalid_log_level", level=self.server_config.log_level)
                return False

            logger.info("configuration_validated")
            return True

        except Exception as e:
            logger.error("config_validation_failed", error=str(e))
            return False

    def print_config_summary(self):
        """印出配置摘要"""
        logger.info(
            "server_configuration",
            port=self.server_config.port,
            log_level=self.server_config.log_level,
            default_pool_size=self.server_config.default_pool_size,
            query_timeout=self.server_config.query_timeout,
            max_connections=self.server_config.max_connections,
            readonly_mode=self.security_config.readonly_mode,
            max_query_length=self.security_config.max_query_length,
            allowed_operations=self.security_config.allowed_operations,
            blocked_keywords=self.security_config.blocked_keywords
        )