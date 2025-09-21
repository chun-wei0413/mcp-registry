#!/usr/bin/env python3
"""Docker entry point for PostgreSQL MCP Server."""

import asyncio
import sys
import os

# Add the parent directory (project root) to Python path
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, project_root)

from src.postgresql_mcp.server import PostgreSQLMCPServer
import structlog

logger = structlog.get_logger()


def main():
    """Main entry point for Docker."""
    try:
        logger.info("Starting PostgreSQL MCP Server for Docker...")

        # Create server instance
        server = PostgreSQLMCPServer()

        # Use run_sync_http method that doesn't conflict with asyncio
        server.run_sync_http()

    except KeyboardInterrupt:
        logger.info("Received keyboard interrupt")
    except Exception as e:
        logger.error("Server failed", error=str(e))
        raise


if __name__ == "__main__":
    main()