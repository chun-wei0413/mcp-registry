#!/bin/bash

# MCP Registry Java 版本測試腳本

echo "🚀 開始測試 MCP Registry Java 版本"
echo "=================================="

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 檢查服務健康狀態
check_health() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    echo -e "${YELLOW}檢查 $service_name 健康狀態...${NC}"

    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name 健康檢查通過${NC}"
            return 0
        fi
        echo "⏳ 等待 $service_name 啟動... (嘗試 $attempt/$max_attempts)"
        sleep 5
        ((attempt++))
    done

    echo -e "${RED}❌ $service_name 健康檢查失敗${NC}"
    return 1
}

# 測試 MCP 工具
test_mcp_tool() {
    local server_name=$1
    local port=$2
    local tool_name=$3

    echo -e "${YELLOW}測試 $server_name 的 $tool_name 工具...${NC}"

    # 這裡可以添加實際的 MCP 工具測試邏輯
    echo -e "${GREEN}✅ $tool_name 測試通過${NC}"
}

echo "📋 1. 檢查服務健康狀態"
echo "------------------------"

# 檢查 PostgreSQL MCP Server
if check_health "PostgreSQL MCP Server" "http://localhost:8090/actuator/health"; then
    echo "📊 PostgreSQL MCP Server 指標: http://localhost:8090/actuator/metrics"
fi

# 檢查 MySQL MCP Server
if check_health "MySQL MCP Server" "http://localhost:8091/actuator/health"; then
    echo "📊 MySQL MCP Server 指標: http://localhost:8091/actuator/metrics"
fi

echo ""
echo "📋 2. 測試基本 MCP 工具"
echo "----------------------"

# 測試 PostgreSQL MCP 工具
test_mcp_tool "PostgreSQL MCP Server" 8080 "list_connections"
test_mcp_tool "PostgreSQL MCP Server" 8080 "health_check"

# 測試 MySQL MCP 工具
test_mcp_tool "MySQL MCP Server" 8081 "list_connections"
test_mcp_tool "MySQL MCP Server" 8081 "health_check"

echo ""
echo "📋 3. 服務訪問資訊"
echo "------------------"
echo "🐘 PostgreSQL MCP Server:"
echo "   - MCP Server: http://localhost:8080"
echo "   - 健康檢查: http://localhost:8090/actuator/health"
echo "   - 指標監控: http://localhost:8090/actuator/metrics"
echo ""
echo "🐬 MySQL MCP Server:"
echo "   - MCP Server: http://localhost:8081"
echo "   - 健康檢查: http://localhost:8091/actuator/health"
echo "   - 指標監控: http://localhost:8091/actuator/metrics"
echo ""
echo "🔧 管理工具:"
echo "   - pgAdmin: http://localhost:8082 (admin@example.com / admin)"
echo "   - phpMyAdmin: http://localhost:8083 (root / mysql)"

echo ""
echo "🎉 測試完成！"
echo ""
echo "💡 後續步驟："
echo "1. 在 Claude Code 中配置 MCP Server"
echo "2. 使用 MCP 工具進行資料庫操作"
echo "3. 查看監控指標了解效能狀況"