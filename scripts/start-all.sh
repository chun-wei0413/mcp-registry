#!/bin/bash

# PostgreSQL & MySQL MCP Servers 統一啟動腳本
# 新版本使用統一的 src 目錄架構

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日誌函數
log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
}

info() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')] INFO: $1${NC}"
}

# 檢查 Docker 是否運行
check_docker() {
    if ! docker --version &> /dev/null; then
        error "Docker 未安裝或未啟動"
        exit 1
    fi

    if ! docker-compose --version &> /dev/null; then
        error "Docker Compose 未安裝"
        exit 1
    fi
}

# 檢查環境檔案
check_env() {
    if [ ! -f ".env" ]; then
        if [ -f ".env.example" ]; then
            log "複製 .env.example 到 .env"
            cp .env.example .env
        else
            warn ".env 檔案不存在，使用預設設定"
        fi
    fi
}

# 檢查專案結構
check_structure() {
    required_dirs=(
        "src/postgresql_mcp"
        "src/mysql_mcp"
        "deployment/docker/postgres"
        "deployment/docker/mysql"
    )

    for dir in "${required_dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            error "缺少必要目錄: $dir"
            exit 1
        fi
    done

    log "✅ 專案結構檢查通過"
}

# 主函數
main() {
    log "開始啟動 PostgreSQL & MySQL MCP Servers (統一架構)"

    # 檢查前置條件
    check_docker
    check_env
    check_structure

    # 建立日誌目錄
    mkdir -p logs/postgresql-mcp logs/mysql-mcp

    case "${1:-start}" in
        "start")
            log "啟動所有服務..."
            docker-compose up -d

            # 等待服務啟動
            log "等待服務啟動中..."
            sleep 15

            # 檢查服務狀態
            log "檢查服務狀態:"
            docker-compose ps

            info "服務端點:"
            info "  PostgreSQL MCP Server: http://localhost:3000"
            info "  MySQL MCP Server: http://localhost:3001"
            info "  PostgreSQL DB: localhost:5432"
            info "  MySQL DB: localhost:3306"

            info "🚀 統一架構特色:"
            info "  📁 源碼: src/postgresql_mcp/ 和 src/mysql_mcp/"
            info "  🐳 Docker: deployment/docker/postgres/ 和 deployment/docker/mysql/"
            info "  🧪 測試: tests/postgresql_mcp/ 和 tests/mysql_mcp/"
            ;;

        "stop")
            log "停止所有服務..."
            docker-compose down
            ;;

        "restart")
            log "重新啟動所有服務..."
            docker-compose down
            docker-compose up -d
            ;;

        "status")
            log "檢查服務狀態:"
            docker-compose ps
            ;;

        "logs")
            service=${2:-""}
            if [ -n "$service" ]; then
                log "顯示 $service 服務日誌:"
                docker-compose logs -f "$service"
            else
                log "顯示所有服務日誌:"
                docker-compose logs -f
            fi
            ;;

        "build")
            log "重新建置服務..."
            docker-compose build
            ;;

        "clean")
            log "清理所有容器和映像..."
            docker-compose down -v --rmi all
            ;;

        "health")
            log "檢查服務健康狀態:"

            # 檢查 PostgreSQL MCP Server
            if curl -s http://localhost:3000/health > /dev/null; then
                log "✅ PostgreSQL MCP Server: 健康"
            else
                error "❌ PostgreSQL MCP Server: 無法連線"
            fi

            # 檢查 MySQL MCP Server
            if curl -s http://localhost:3001/health > /dev/null; then
                log "✅ MySQL MCP Server: 健康"
            else
                error "❌ MySQL MCP Server: 無法連線"
            fi
            ;;

        "dev")
            info "本地開發模式:"
            info "PostgreSQL MCP Server:"
            info "  cd . && python -m src.postgresql_mcp.server"
            info ""
            info "MySQL MCP Server:"
            info "  cd . && python -m src.mysql_mcp.mysql_server"
            ;;

        "structure")
            info "目前專案結構:"
            echo "pg-mcp/"
            echo "├── src/"
            echo "│   ├── postgresql_mcp/     # PostgreSQL MCP Server 源碼"
            echo "│   └── mysql_mcp/          # MySQL MCP Server 源碼"
            echo "├── deployment/"
            echo "│   └── docker/"
            echo "│       ├── postgres/       # PostgreSQL Docker 配置"
            echo "│       └── mysql/          # MySQL Docker 配置"
            echo "├── tests/"
            echo "│   ├── postgresql_mcp/     # PostgreSQL 測試"
            echo "│   └── mysql_mcp/          # MySQL 測試"
            echo "├── docs/                   # 共用文檔"
            echo "├── scripts/                # 管理腳本"
            echo "├── logs/                   # 日誌目錄"
            echo "├── docker-compose.yml      # 統一部署"
            echo "└── pyproject.toml          # 統一配置"
            ;;

        "help"|*)
            info "使用方式: $0 [命令]"
            info ""
            info "可用命令:"
            info "  start      - 啟動所有服務 (預設)"
            info "  stop       - 停止所有服務"
            info "  restart    - 重新啟動所有服務"
            info "  status     - 檢查服務狀態"
            info "  logs       - 顯示服務日誌 (可指定服務名稱)"
            info "  build      - 重新建置服務"
            info "  clean      - 清理所有容器和映像"
            info "  health     - 檢查服務健康狀態"
            info "  dev        - 顯示本地開發指令"
            info "  structure  - 顯示專案結構"
            info "  help       - 顯示此說明"
            info ""
            info "範例:"
            info "  $0 start"
            info "  $0 logs postgresql-mcp-server"
            info "  $0 logs mysql-mcp-server"
            info "  $0 structure"
            ;;
    esac
}

# 執行主函數
main "$@"