#!/bin/bash

################################################################################
# RAG Memory MCP Server - macOS 啟動腳本
#
# 使用方式：
#   ./start.sh              # 首次執行需要 chmod +x start.sh
#   bash start.sh           # 或直接用 bash 執行
#
# 功能：
#   1. 自動建立虛擬環境（如果不存在）
#   2. 安裝 Python 依賴
#   3. 啟動 MCP Server
#
# 說明：
#   - 首次執行會下載 embedding 模型，預計 5-10 分鐘
#   - 後續執行只需 10-20 秒
#   - 按 Ctrl+C 停止伺服器
################################################################################

set -e

# 取得腳本所在目錄（支援相對和絕對路徑）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 顏色定義（macOS 相容）
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 訊息函數
print_step() {
    echo -e "${BLUE}[*]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

# 檢查 Python 是否已安裝
print_step "檢查 Python 版本..."
if ! command -v python3 &> /dev/null; then
    print_error "Python 3 未安裝"
    echo "請下載安裝：https://www.python.org/downloads/"
    exit 1
fi

PYTHON_VERSION=$(python3 --version | awk '{print $2}')
print_success "Python $PYTHON_VERSION 已安裝"

# 檢查 Python 版本是否 >= 3.11
PYTHON_MAJOR=$(echo $PYTHON_VERSION | cut -d. -f1)
PYTHON_MINOR=$(echo $PYTHON_VERSION | cut -d. -f2)

if [ "$PYTHON_MAJOR" -lt 3 ] || ([ "$PYTHON_MAJOR" -eq 3 ] && [ "$PYTHON_MINOR" -lt 11 ]); then
    print_warning "建議使用 Python 3.11 或更新版本（目前版本：$PYTHON_VERSION）"
fi

# 建立虛擬環境（如果不存在）
if [ ! -d venv ]; then
    print_step "建立虛擬環境..."
    python3 -m venv venv
    print_success "虛擬環境已建立"
else
    print_success "虛擬環境已存在"
fi

# 安裝依賴
print_step "安裝 Python 依賴..."
print_warning "首次執行會下載 embedding 模型，預計 5-10 分鐘..."

# 使用虛擬環境中的 pip，抑制不必要的輸出
./venv/bin/pip install -q --upgrade pip
./venv/bin/pip install -q -r requirements.txt

print_success "所有依賴已安裝"

# 驗證重要套件
print_step "驗證必要套件..."
./venv/bin/python -c "import mcp; print('✓ mcp')" 2>/dev/null && \
./venv/bin/python -c "import chromadb; print('✓ chromadb')" 2>/dev/null && \
./venv/bin/python -c "import sentence_transformers; print('✓ sentence-transformers')" 2>/dev/null
print_success "所有必要套件已驗證"

# 啟動 MCP Server
echo ""
print_step "啟動 MCP Server..."
print_warning "伺服器監聽位址：http://localhost:3031"
print_warning "按 Ctrl+C 停止伺服器"
echo ""

./venv/bin/python mcp_server.py
