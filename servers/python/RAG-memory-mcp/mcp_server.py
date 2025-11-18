#!/usr/bin/env python3
"""
MCP Knowledge Base Server - Main Entry Point

Minimal startup script that creates and runs the MCP server.
All configuration and initialization logic is in app.py (Application Factory).
"""
from app import create_app


if __name__ == "__main__":
    # Create the application
    server = create_app()

    # Run the server with SSE transport
    print("[*] Starting MCP Server...")
    print("[*] Listening on 0.0.0.0:3031")
    print("[*] Press Ctrl+C to stop\n")

    server.run(transport="sse")
