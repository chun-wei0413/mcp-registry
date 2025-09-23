#!/bin/bash

# MCP Registry Java Edition - 統一啟動腳本
# 基於 Java 17 + Spring Boot 3.x + Maven 架構

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

# 檢查 Java 環境
check_java() {
    if ! java -version &> /dev/null; then
        error "Java 未安裝或版本不正確 (需要 Java 17+)"
        exit 1
    fi

    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 17 ]; then
        error "Java 版本太舊 (目前: $java_version, 需要: 17+)"
        exit 1
    fi

    log "✅ Java 版本檢查通過: $(java -version 2>&1 | head -n 1)"
}

# 檢查 Maven 環境
check_maven() {
    if ! mvn --version &> /dev/null; then
        error "Maven 未安裝"
        exit 1
    fi

    log "✅ Maven 版本檢查通過: $(mvn --version | head -n 1)"
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

    log "✅ Docker 環境檢查通過"
}

# 檢查專案結構
check_structure() {
    required_dirs=(
        "mcp-registry-java/mcp-common"
        "mcp-registry-java/mcp-postgresql-server"
        "mcp-registry-java/mcp-mysql-server"
        "mcp-registry-java/testing-tools"
    )

    for dir in "${required_dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            error "缺少必要目錄: $dir"
            exit 1
        fi
    done

    # 檢查主 pom.xml
    if [ ! -f "mcp-registry-java/pom.xml" ]; then
        error "缺少主 Maven 配置檔案: mcp-registry-java/pom.xml"
        exit 1
    fi

    log "✅ 專案結構檢查通過"
}

# 建置 Java 專案
build_project() {
    log "開始建置 Java 專案..."

    cd mcp-registry-java

    # 清理並安裝依賴
    mvn clean install -DskipTests

    if [ $? -eq 0 ]; then
        log "✅ 專案建置成功"
    else
        error "❌ 專案建置失敗"
        exit 1
    fi

    cd ..
}

# 建置 Docker 映像
build_docker_images() {
    log "開始建置 Docker 映像..."

    cd mcp-registry-java

    # 建置 PostgreSQL MCP Server 映像
    log "建置 PostgreSQL MCP Server 映像..."
    cd mcp-postgresql-server
    mvn jib:dockerBuild
    cd ..

    # 建置 MySQL MCP Server 映像
    log "建置 MySQL MCP Server 映像..."
    cd mcp-mysql-server
    mvn jib:dockerBuild
    cd ..

    cd ..

    log "✅ Docker 映像建置完成"
}

# 主函數
main() {
    log "開始啟動 MCP Registry Java Edition"

    case "${1:-help}" in
        "build")
            log "建置 Java 專案..."
            check_java
            check_maven
            check_structure
            build_project
            ;;

        "docker-build")
            log "建置 Docker 映像..."
            check_java
            check_maven
            check_docker
            check_structure
            build_project
            build_docker_images
            ;;

        "start")
            log "啟動所有服務..."
            check_docker

            if [ ! -f "deployment/docker-compose.yml" ]; then
                error "缺少 Docker Compose 配置檔案"
                exit 1
            fi

            cd deployment
            docker-compose up -d
            cd ..

            # 等待服務啟動
            log "等待服務啟動中..."
            sleep 15

            # 檢查服務狀態
            log "檢查服務狀態:"
            cd deployment
            docker-compose ps
            cd ..

            info "服務端點:"
            info "  PostgreSQL MCP Server: http://localhost:8080"
            info "  MySQL MCP Server: http://localhost:8081"
            info "  PostgreSQL DB: localhost:5432"
            info "  MySQL DB: localhost:3306"
            ;;

        "stop")
            log "停止所有服務..."
            cd deployment
            docker-compose down
            cd ..
            ;;

        "restart")
            log "重新啟動所有服務..."
            cd deployment
            docker-compose down
            docker-compose up -d
            cd ..
            ;;

        "status")
            log "檢查服務狀態:"
            cd deployment
            docker-compose ps
            cd ..
            ;;

        "logs")
            service=${2:-""}
            cd deployment
            if [ -n "$service" ]; then
                log "顯示 $service 服務日誌:"
                docker-compose logs -f "$service"
            else
                log "顯示所有服務日誌:"
                docker-compose logs -f
            fi
            cd ..
            ;;

        "test")
            log "執行測試..."
            check_java
            check_maven
            check_structure

            cd mcp-registry-java
            mvn test
            cd ..
            ;;

        "integration-test")
            log "執行整合測試..."
            check_java
            check_maven
            check_docker
            check_structure

            cd mcp-registry-java
            mvn integration-test
            cd ..
            ;;

        "clean")
            log "清理專案..."

            # 清理 Maven 建置
            if [ -d "mcp-registry-java" ]; then
                cd mcp-registry-java
                mvn clean
                cd ..
            fi

            # 清理 Docker
            if [ -f "deployment/docker-compose.yml" ]; then
                cd deployment
                docker-compose down -v --rmi local
                cd ..
            fi
            ;;

        "health")
            log "檢查服務健康狀態:"

            # 檢查 PostgreSQL MCP Server
            if curl -s http://localhost:8080/actuator/health > /dev/null; then
                log "✅ PostgreSQL MCP Server: 健康"
            else
                error "❌ PostgreSQL MCP Server: 無法連線"
            fi

            # 檢查 MySQL MCP Server
            if curl -s http://localhost:8081/actuator/health > /dev/null; then
                log "✅ MySQL MCP Server: 健康"
            else
                error "❌ MySQL MCP Server: 無法連線"
            fi
            ;;

        "dev")
            info "本地開發模式:"
            info "PostgreSQL MCP Server:"
            info "  cd mcp-registry-java/mcp-postgresql-server"
            info "  mvn spring-boot:run"
            info ""
            info "MySQL MCP Server:"
            info "  cd mcp-registry-java/mcp-mysql-server"
            info "  mvn spring-boot:run"
            ;;

        "structure")
            info "目前專案結構 (Java Edition):"
            echo "mcp-registry/"
            echo "├── mcp-registry-java/"
            echo "│   ├── mcp-common/               # 共用模組"
            echo "│   ├── mcp-postgresql-server/    # PostgreSQL MCP Server"
            echo "│   ├── mcp-mysql-server/         # MySQL MCP Server"
            echo "│   ├── testing-tools/            # 測試工具"
            echo "│   └── pom.xml                   # 主 Maven 配置"
            echo "├── deployment/"
            echo "│   ├── docker-compose.yml        # Docker Compose 配置"
            echo "│   └── k8s/                      # Kubernetes 配置"
            echo "├── documentation/                # 文檔中心"
            echo "├── scripts/                      # 管理腳本"
            echo "└── README.md                     # 專案說明"
            ;;

        "env-check")
            log "檢查開發環境:"
            check_java
            check_maven
            check_docker
            check_structure
            log "✅ 所有環境檢查通過"
            ;;

        "help"|*)
            info "MCP Registry Java Edition - 使用方式: $0 [命令]"
            info ""
            info "開發命令:"
            info "  env-check       - 檢查開發環境 (Java, Maven, Docker)"
            info "  build           - 建置 Java 專案"
            info "  test            - 執行單元測試"
            info "  integration-test - 執行整合測試"
            info "  clean           - 清理專案"
            info ""
            info "Docker 命令:"
            info "  docker-build    - 建置 Docker 映像"
            info "  start           - 啟動所有服務"
            info "  stop            - 停止所有服務"
            info "  restart         - 重新啟動所有服務"
            info "  status          - 檢查服務狀態"
            info "  logs [service]  - 顯示服務日誌"
            info "  health          - 檢查服務健康狀態"
            info ""
            info "工具命令:"
            info "  dev             - 顯示本地開發指令"
            info "  structure       - 顯示專案結構"
            info "  help            - 顯示此說明"
            info ""
            info "範例:"
            info "  $0 env-check      # 檢查開發環境"
            info "  $0 build          # 建置專案"
            info "  $0 docker-build   # 建置 Docker 映像"
            info "  $0 start          # 啟動服務"
            info "  $0 logs postgresql-mcp-server"
            ;;
    esac
}

# 執行主函數
main "$@"