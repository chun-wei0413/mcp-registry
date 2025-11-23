#!/usr/bin/env python3
"""
MCP Knowledge Base Server - Main Entry Point

Minimal startup script that creates and runs the MCP server.
All configuration and initialization logic is in app.py (Application Factory).
"""

# Suppress unnecessary warnings before importing any libraries
import os
import warnings

# Suppress TensorFlow warnings
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'  # Only show errors
os.environ['TF_ENABLE_ONEDNN_OPTS'] = '0'  # Disable oneDNN messages

# Suppress protobuf warnings
os.environ['PROTOCOL_BUFFERS_PYTHON_IMPLEMENTATION'] = 'python'

# Suppress general Python warnings
warnings.filterwarnings('ignore', category=DeprecationWarning)
warnings.filterwarnings('ignore', category=FutureWarning)

from app import create_app


if __name__ == "__main__":
    # Create the application
    server = create_app()

    # Run the server with SSE transport
    print("[*] Starting MCP Server...")
    print("[*] Listening on 0.0.0.0:3031")
    print("[*] Press Ctrl+C to stop\n")

    server.run(transport="sse")
