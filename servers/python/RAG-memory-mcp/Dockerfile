# Memory MCP Server - Dockerfile
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

# Copy application code
COPY . .

# Create directory for persistent storage
RUN mkdir -p /app/chroma_db

# Expose port for MCP HTTP server (if using HTTP transport)
EXPOSE 3031

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV MCP_SERVER_NAME="Memory MCP Server"

# Run the MCP server in HTTP mode on port 3031
CMD ["python", "mcp_server.py"]
