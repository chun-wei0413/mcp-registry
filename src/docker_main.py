#!/usr/bin/env python3
"""Docker entry point for PostgreSQL MCP Server."""

import asyncio
import sys
import os

# Add the parent directory (project root) to Python path
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, project_root)

from src.server import PostgreSQLMCPServer
import structlog

logger = structlog.get_logger()


def main():
    """Main entry point for Docker."""
    try:
        logger.info("Starting PostgreSQL MCP Server for Docker...")

        # Create server instance
        server = PostgreSQLMCPServer()

        # Initialize and run server
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)

        # Initialize the server
        loop.run_until_complete(server.initialize())
        logger.info("Server initialization completed")

        logger.info("PostgreSQL MCP Server is ready and running on port 3000")

        # Run the server using uvicorn directly
        import uvicorn
        # Get the FastMCP streamable HTTP app instance and run with uvicorn
        uvicorn.run(server.app.streamable_http_app, host="0.0.0.0", port=3000)

    except KeyboardInterrupt:
        logger.info("Received keyboard interrupt")
    except Exception as e:
        logger.error("Server failed", error=str(e))
        raise


if __name__ == "__main__":
    main()