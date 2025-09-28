#!/bin/bash

# BDD 測試執行腳本
# 用於執行所有 MCP Registry Java 專案的 BDD 測試

set -e

# 設定顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 設定專案路徑
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_PROJECT_ROOT="${PROJECT_ROOT}/mcp-registry-java"

# 日誌函數
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 檢查 Java 環境
check_java_environment() {
    log_info "檢查 Java 環境..."

    if ! command -v java &> /dev/null; then
        log_error "Java 未安裝或不在 PATH 中"
        exit 1
    fi

    if ! command -v mvn &> /dev/null; then
        log_error "Maven 未安裝或不在 PATH 中"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d '"' -f 2)
    MAVEN_VERSION=$(mvn -version 2>&1 | head -n 1 | cut -d ' ' -f 3)

    log_success "Java 版本: ${JAVA_VERSION}"
    log_success "Maven 版本: ${MAVEN_VERSION}"
}

# 檢查 Docker 環境 (用於 TestContainers)
check_docker_environment() {
    log_info "檢查 Docker 環境..."

    if ! command -v docker &> /dev/null; then
        log_warning "Docker 未安裝，部分整合測試可能無法執行"
        return 1
    fi

    if ! docker info &> /dev/null; then
        log_warning "Docker 服務未啟動，部分整合測試可能無法執行"
        return 1
    fi

    log_success "Docker 環境正常"
    return 0
}

# 設定測試報告目錄
setup_test_reports() {
    local reports_dir="${PROJECT_ROOT}/test-reports"
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    local current_report_dir="${reports_dir}/bdd-tests-${timestamp}"

    mkdir -p "${current_report_dir}"

    # 建立符號連結指向最新報告
    ln -sfn "bdd-tests-${timestamp}" "${reports_dir}/latest"

    echo "${current_report_dir}"
}

# 執行特定模組的 BDD 測試
run_module_bdd_tests() {
    local module_name="$1"
    local module_path="${JAVA_PROJECT_ROOT}/${module_name}"
    local reports_dir="$2"

    if [ ! -d "${module_path}" ]; then
        log_warning "模組 ${module_name} 不存在，跳過測試"
        return 0
    fi

    log_info "執行 ${module_name} BDD 測試..."

    cd "${module_path}"

    # 執行 BDD 測試並產生報告
    mvn clean test \
        -Dtest="**/*BDDTest" \
        -Dmaven.test.failure.ignore=true \
        -Dsurefire.reportFormat=xml \
        -Dsurefire.useFile=true \
        -Dsurefire.reportsDirectory="${reports_dir}/${module_name}" \
        | tee "${reports_dir}/${module_name}-output.log"

    local exit_code=${PIPESTATUS[0]}

    if [ $exit_code -eq 0 ]; then
        log_success "${module_name} BDD 測試執行完成"
    else
        log_error "${module_name} BDD 測試執行失敗 (退出碼: ${exit_code})"
    fi

    cd "${PROJECT_ROOT}"
    return $exit_code
}

# 執行所有 BDD 測試
run_all_bdd_tests() {
    local reports_dir="$1"
    local overall_status=0

    log_info "開始執行所有 MCP Registry BDD 測試..."

    # 定義要測試的模組
    local modules=(
        "mcp-core"
        "mcp-postgresql-server"
        "mcp-mysql-server"
    )

    # 執行每個模組的測試
    for module in "${modules[@]}"; do
        if ! run_module_bdd_tests "${module}" "${reports_dir}"; then
            overall_status=1
        fi
        echo ""
    done

    return $overall_status
}

# 產生測試摘要報告
generate_test_summary() {
    local reports_dir="$1"
    local summary_file="${reports_dir}/test-summary.txt"

    log_info "產生測試摘要報告..."

    echo "MCP Registry BDD 測試摘要報告" > "${summary_file}"
    echo "==============================" >> "${summary_file}"
    echo "測試時間: $(date)" >> "${summary_file}"
    echo "" >> "${summary_file}"

    # 統計各模組測試結果
    for log_file in "${reports_dir}"/*-output.log; do
        if [ -f "${log_file}" ]; then
            local module_name=$(basename "${log_file}" -output.log)
            echo "模組: ${module_name}" >> "${summary_file}"

            # 從日誌中提取測試統計
            local tests_run=$(grep "Tests run:" "${log_file}" | tail -1 | sed 's/.*Tests run: \([0-9]*\).*/\1/')
            local failures=$(grep "Tests run:" "${log_file}" | tail -1 | sed 's/.*Failures: \([0-9]*\).*/\1/')
            local errors=$(grep "Tests run:" "${log_file}" | tail -1 | sed 's/.*Errors: \([0-9]*\).*/\1/')
            local skipped=$(grep "Tests run:" "${log_file}" | tail -1 | sed 's/.*Skipped: \([0-9]*\).*/\1/')

            echo "  - 執行測試: ${tests_run:-0}" >> "${summary_file}"
            echo "  - 失敗: ${failures:-0}" >> "${summary_file}"
            echo "  - 錯誤: ${errors:-0}" >> "${summary_file}"
            echo "  - 跳過: ${skipped:-0}" >> "${summary_file}"
            echo "" >> "${summary_file}"
        fi
    done

    # 顯示摘要
    cat "${summary_file}"
    log_success "測試摘要報告已儲存至: ${summary_file}"
}

# 清理測試環境
cleanup_test_environment() {
    log_info "清理測試環境..."

    # 停止所有 TestContainers
    docker ps -a --filter "label=org.testcontainers" --format "{{.ID}}" | xargs -r docker rm -f

    # 清理 TestContainers 映像檔 (可選)
    # docker images --filter "dangling=true" --format "{{.ID}}" | xargs -r docker rmi

    log_success "測試環境清理完成"
}

# 主執行函數
main() {
    local docker_available=false

    echo "================================================"
    echo "      MCP Registry BDD 測試執行器"
    echo "================================================"
    echo ""

    # 檢查環境
    check_java_environment

    if check_docker_environment; then
        docker_available=true
    fi

    # 設定測試報告目錄
    local reports_dir
    reports_dir=$(setup_test_reports)
    log_info "測試報告將儲存至: ${reports_dir}"

    # 執行測試
    local test_status=0
    if run_all_bdd_tests "${reports_dir}"; then
        log_success "所有 BDD 測試執行完成"
    else
        log_error "部分 BDD 測試執行失敗"
        test_status=1
    fi

    # 產生摘要報告
    generate_test_summary "${reports_dir}"

    # 清理環境
    if [ "$docker_available" = true ]; then
        cleanup_test_environment
    fi

    echo ""
    echo "================================================"
    if [ $test_status -eq 0 ]; then
        log_success "BDD 測試執行成功完成！"
    else
        log_error "BDD 測試執行完成，但有部分失敗！"
    fi
    echo "測試報告位置: ${reports_dir}"
    echo "================================================"

    exit $test_status
}

# 處理命令列參數
case "${1:-}" in
    --help|-h)
        echo "用法: $0 [選項]"
        echo ""
        echo "選項:"
        echo "  --help, -h     顯示此說明"
        echo "  --clean        執行前清理所有測試資料"
        echo "  --no-docker    跳過需要 Docker 的測試"
        echo ""
        echo "範例:"
        echo "  $0              # 執行所有 BDD 測試"
        echo "  $0 --clean      # 清理後執行所有 BDD 測試"
        exit 0
        ;;
    --clean)
        log_info "清理模式：將先清理所有測試資料..."
        # 清理所有 target 目錄
        find "${JAVA_PROJECT_ROOT}" -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
        ;;
    --no-docker)
        log_warning "跳過 Docker 模式：將跳過需要 TestContainers 的測試"
        export SKIP_INTEGRATION_TESTS=true
        ;;
esac

# 執行主程式
main "$@"