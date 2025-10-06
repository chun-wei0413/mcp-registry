#!/bin/bash

# ContextCore MCP Tools Testing Script
# This script tests all MCP Tools via the Test Controller

BASE_URL="http://localhost:8082/api/test"

echo "=========================================="
echo "ContextCore MCP Tools Testing"
echo "=========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Ping
echo -e "${YELLOW}[Test 1/6]${NC} Testing server ping..."
PING_RESULT=$(curl -s "${BASE_URL}/ping")
echo "Response: $PING_RESULT"
echo ""

# Test 2: Add Log - Feature
echo -e "${YELLOW}[Test 2/6]${NC} Testing addLog (FEATURE)..."
ADD_LOG_1=$(curl -s -X POST "${BASE_URL}/add-log" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "實現用戶登入功能",
    "content": "完成了用戶登入的後端 API，使用 JWT 進行身份驗證。包含密碼加密和 token 刷新機制。技術棧：Spring Security + JWT + BCrypt",
    "tags": "backend,authentication,jwt,security",
    "module": "user-service",
    "type": "FEATURE"
  }')
echo "Response: $ADD_LOG_1"
LOG_ID_1=$(echo $ADD_LOG_1 | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)
echo -e "${GREEN}Created Log ID: $LOG_ID_1${NC}"
echo ""

# Test 3: Add Log - Bug
echo -e "${YELLOW}[Test 3/6]${NC} Testing addLog (BUG)..."
ADD_LOG_2=$(curl -s -X POST "${BASE_URL}/add-log" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "修復記憶體洩漏問題",
    "content": "發現 UserSession 沒有正確釋放導致記憶體洩漏。已修改為使用 WeakHashMap 並添加定期清理機制。",
    "tags": "bug,memory,performance",
    "module": "session-manager",
    "type": "BUG"
  }')
echo "Response: $ADD_LOG_2"
LOG_ID_2=$(echo $ADD_LOG_2 | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)
echo -e "${GREEN}Created Log ID: $LOG_ID_2${NC}"
echo ""

# Test 4: Add Log - Decision
echo -e "${YELLOW}[Test 4/6]${NC} Testing addLog (DECISION)..."
ADD_LOG_3=$(curl -s -X POST "${BASE_URL}/add-log" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "技術選型：選擇 PostgreSQL 作為主資料庫",
    "content": "經過評估，決定使用 PostgreSQL 而非 MySQL。主要原因：1) 更好的 JSON 支援 2) ACID 保證 3) 豐富的擴展功能",
    "tags": "architecture,database,decision",
    "module": "infrastructure",
    "type": "DECISION"
  }')
echo "Response: $ADD_LOG_3"
LOG_ID_3=$(echo $ADD_LOG_3 | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)
echo -e "${GREEN}Created Log ID: $LOG_ID_3${NC}"
echo ""

# Wait for embedding processing
echo "⏳ Waiting 3 seconds for embeddings to be processed..."
sleep 3

# Test 5: Search Logs
echo -e "${YELLOW}[Test 5/6]${NC} Testing searchLogs..."
SEARCH_RESULT=$(curl -s -X POST "${BASE_URL}/search-logs" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "用戶身份驗證和安全",
    "limit": 3
  }')
echo "Search Query: '用戶身份驗證和安全'"
echo "Response: $SEARCH_RESULT" | python3 -m json.tool 2>/dev/null || echo "$SEARCH_RESULT"
echo ""

# Test 6: List Logs
echo -e "${YELLOW}[Test 6/6]${NC} Testing listLogs..."
LIST_RESULT=$(curl -s "${BASE_URL}/list-logs?limit=10")
echo "Response: $LIST_RESULT" | python3 -m json.tool 2>/dev/null || echo "$LIST_RESULT"
echo ""

# Test 7: Get Specific Log
if [ ! -z "$LOG_ID_1" ]; then
    echo -e "${YELLOW}[Bonus Test]${NC} Testing getLog with ID: $LOG_ID_1..."
    GET_RESULT=$(curl -s "${BASE_URL}/get-log/${LOG_ID_1}")
    echo "Response: $GET_RESULT" | python3 -m json.tool 2>/dev/null || echo "$GET_RESULT"
    echo ""
fi

# Test 8: Get Project Context
echo -e "${YELLOW}[Final Test]${NC} Testing getProjectContext..."
CONTEXT_RESULT=$(curl -s "${BASE_URL}/project-context?limit=5")
echo "Response: $CONTEXT_RESULT" | python3 -m json.tool 2>/dev/null || echo "$CONTEXT_RESULT"
echo ""

echo "=========================================="
echo -e "${GREEN}✅ All tests completed!${NC}"
echo "=========================================="
echo ""
echo "📊 Next steps:"
echo "1. Check Qdrant Dashboard: http://localhost:6333/dashboard"
echo "2. Verify collection 'contextcore_logs' exists"
echo "3. Check points count and vector data"
echo ""
