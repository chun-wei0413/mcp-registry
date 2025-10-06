
# ContextCore MCP Server - Cleanup Script
# 清理不必要的檔案和目錄

echo "=========================================="
echo "ContextCore MCP Server - 專案清理"
echo "=========================================="
echo ""

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 當前目錄
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo -e "${YELLOW}📁 清理項目：${NC}"
echo ""

# 1. 刪除臨時文檔
echo -e "${YELLOW}[1/5]${NC} 刪除臨時/過時的文檔檔案..."
rm -f BUILD_FIX.md
rm -f FIX_LOMBOK_JAVA25.md
rm -f MCP_MIGRATION.md
rm -f START_SERVER.md
rm -f RUN_SERVER.md
rm -f PROJECT_STRUCTURE.md
echo -e "${GREEN}✓ 已刪除臨時文檔${NC}"
echo ""

# 2. 清理空目錄（保留 .gitkeep）
echo -e "${YELLOW}[2/5]${NC} 清理空目錄..."
# data 和 logs 目錄需要保留，但可以清空內容
if [ -d "data" ]; then
    rm -rf data/*
    touch data/.gitkeep
    echo -e "${GREEN}✓ 已清理 data/ 目錄${NC}"
fi

if [ -d "logs" ]; then
    rm -rf logs/*
    touch logs/.gitkeep
    echo -e "${GREEN}✓ 已清理 logs/ 目錄${NC}"
fi
echo ""

# 3. 清理 Maven target 目錄
echo -e "${YELLOW}[3/5]${NC} 清理 Maven 編譯產物..."
if [ -d "target" ]; then
    rm -rf target
    echo -e "${GREEN}✓ 已刪除 target/ 目錄${NC}"
else
    echo -e "${GREEN}✓ target/ 目錄不存在，跳過${NC}"
fi
echo ""

# 4. 清理 Docker volumes（可選）
echo -e "${YELLOW}[4/5]${NC} 是否清理 Docker volumes？(y/N)"
read -r response
if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
    if [ -d "docker-volumes" ]; then
        rm -rf docker-volumes/*
        echo -e "${GREEN}✓ 已清理 docker-volumes/ 目錄${NC}"
        echo -e "${YELLOW}  注意：下次啟動時需要重新拉取 Ollama 模型${NC}"
    fi
else
    echo -e "${GREEN}✓ 保留 Docker volumes${NC}"
fi
echo ""

# 5. 清理測試生成的 SQLite 資料庫
echo -e "${YELLOW}[5/5]${NC} 清理測試資料庫..."
find . -name "*.db" -type f -delete 2>/dev/null
find . -name "*.db-journal" -type f -delete 2>/dev/null
echo -e "${GREEN}✓ 已清理 SQLite 資料庫檔案${NC}"
echo ""

echo "=========================================="
echo -e "${GREEN}✅ 清理完成！${NC}"
echo "=========================================="
echo ""
echo "保留的重要檔案："
echo "  ✓ pom.xml"
echo "  ✓ docker-compose.yml"
echo "  ✓ README.md"
echo "  ✓ DOCKER_SETUP.md"
echo "  ✓ TESTING_GUIDE.md"
echo "  ✓ test-mcp-tools.sh"
echo "  ✓ src/ (原始碼)"
if [[ ! "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
echo "  ✓ docker-volumes/ (Docker 資料)"
fi
echo ""
