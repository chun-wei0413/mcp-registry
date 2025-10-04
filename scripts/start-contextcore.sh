#!/bin/bash

# ContextCore MCP 啟動腳本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/deployment/contextcore-docker-compose.yml"

echo "========================================="
echo "  ContextCore MCP 啟動腳本"
echo "========================================="
echo ""

# 檢查 Docker 是否運行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker 未運行，請先啟動 Docker"
    exit 1
fi

echo "✅ Docker 已運行"
echo ""

# 1. 啟動依賴服務（Qdrant + Ollama）
echo "📦 啟動依賴服務 (Qdrant + Ollama)..."
docker-compose -f "$DOCKER_COMPOSE_FILE" up -d

echo "⏳ 等待服務啟動..."
sleep 10

# 2. 檢查 Qdrant 狀態
echo ""
echo "🔍 檢查 Qdrant 狀態..."
if curl -s http://localhost:6333/ > /dev/null; then
    echo "✅ Qdrant 已啟動"
else
    echo "❌ Qdrant 啟動失敗"
    exit 1
fi

# 3. 檢查 Ollama 狀態
echo ""
echo "🔍 檢查 Ollama 狀態..."
if curl -s http://localhost:11434/ > /dev/null; then
    echo "✅ Ollama 已啟動"
else
    echo "❌ Ollama 啟動失敗"
    exit 1
fi

# 4. 下載 Embedding 模型
echo ""
echo "📥 下載 Embedding 模型 (nomic-embed-text)..."
echo "   (首次下載約需 2-3 分鐘，請耐心等待)"
docker exec contextcore-ollama ollama pull nomic-embed-text

if [ $? -eq 0 ]; then
    echo "✅ Embedding 模型下載完成"
else
    echo "⚠️  模型下載失敗，但可以繼續啟動服務"
fi

# 5. 建置 ContextCore MCP Server
echo ""
echo "🔨 建置 ContextCore MCP Server..."
cd "$PROJECT_ROOT/mcp-registry-java/mcp-contextcore-server"

if mvn clean package -DskipTests; then
    echo "✅ 建置成功"
else
    echo "❌ 建置失敗"
    exit 1
fi

# 6. 啟動 MCP Server
echo ""
echo "🚀 啟動 ContextCore MCP Server..."
echo "   (按 Ctrl+C 停止)"
echo ""

java -jar target/mcp-contextcore-server-1.0.0-SNAPSHOT.jar

# 清理（當使用者按 Ctrl+C 時）
echo ""
echo "🛑 停止服務..."
docker-compose -f "$DOCKER_COMPOSE_FILE" down
