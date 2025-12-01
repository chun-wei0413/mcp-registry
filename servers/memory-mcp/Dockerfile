# RAG Memory MCP Server - Dockerfile
# Layered architecture with Application Factory pattern

FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better caching
COPY requirements.txt .

# Install CPU-only PyTorch first to avoid downloading GPU/CUDA packages (saves ~1.5GB and 5-10 minutes)
RUN pip install --no-cache-dir torch torchvision torchaudio \
    --index-url https://download.pytorch.org/whl/cpu

# Install other Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application structure
COPY app.py .
COPY mcp_server.py .
COPY controllers/ ./controllers/
COPY services/ ./services/
COPY models/ ./models/
COPY utils/ ./utils/

# Create directory for persistent storage
RUN mkdir -p /app/chroma_db

# Expose port for MCP HTTP/SSE server
EXPOSE 3031

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV MCP_SERVER_NAME="RAG Memory MCP Server"
ENV CHROMA_DB_PATH=/app/chroma_db

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD python -c "from app import create_app; app = create_app(); print('OK')" || exit 1

# Run the MCP server (SSE transport on port 3031)
CMD ["python", "mcp_server.py"]
