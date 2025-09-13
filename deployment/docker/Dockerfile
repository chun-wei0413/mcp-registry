# PostgreSQL MCP Server Dockerfile
FROM python:3.11-slim

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better caching
COPY pyproject.toml ./

# Install Python dependencies
RUN pip install --no-cache-dir -e .

# Copy source code
COPY src/ ./src/
COPY .env.example .env

# Create non-root user for security
RUN groupadd -r mcpuser && useradd -r -g mcpuser mcpuser
RUN chown -R mcpuser:mcpuser /app
USER mcpuser

# Expose MCP server port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "import asyncio; from src.tools.connection import ConnectionManager; from src.models.types import SecurityConfig; cm = ConnectionManager(SecurityConfig()); print('Health check passed')"

# Default command
CMD ["python", "-m", "src.server"]

# Metadata
LABEL maintainer="FrankLi <a910413frank@gmail.com>" \
      description="PostgreSQL MCP Server - A universal MCP server for PostgreSQL operations" \
      version="0.1.0"