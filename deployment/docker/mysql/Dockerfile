FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y     gcc     pkg-config     default-libmysqlclient-dev     && rm -rf /var/lib/apt/lists/*

# Copy source code first
COPY src/ src/

# Copy requirements and install Python dependencies
COPY pyproject.toml .
RUN pip install --no-cache-dir -e .

# Create logs directory
RUN mkdir -p logs

# Set environment variables
ENV PYTHONPATH=/app
ENV MCP_SERVER_PORT=3001
ENV MCP_LOG_LEVEL=INFO

# Expose port
EXPOSE 3001

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3     CMD python -c "import requests; requests.get(\"http://localhost:3001/health\")" || exit 1

# Run the MySQL MCP server
CMD ["python", "-m", "src.mysql_mcp.mysql_server"]
