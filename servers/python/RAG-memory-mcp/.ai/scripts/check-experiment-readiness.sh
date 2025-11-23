#!/bin/bash

# Experiment Readiness Checker
# 確保實驗環境準備就緒，避免重複錯誤

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "========================================="
echo "🔍 實驗準備度檢查"
echo "========================================="

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check counters
TOTAL_CHECKS=0
PASSED_CHECKS=0
WARNINGS=0

check_pass() {
    echo -e "${GREEN}✅ $1${NC}"
    ((PASSED_CHECKS++))
    ((TOTAL_CHECKS++))
}

check_fail() {
    echo -e "${RED}❌ $1${NC}"
    ((TOTAL_CHECKS++))
}

check_warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
    ((WARNINGS++))
}

echo ""
echo "1️⃣  環境檢查"
echo "-----------------------------------------"

# Check PostgreSQL
if docker ps | grep -q postgres || nc -z localhost 5800 2>/dev/null; then
    check_pass "PostgreSQL 運行中 (port 5800)"
else
    check_fail "PostgreSQL 未運行！請執行: docker-compose up -d postgres"
fi

# Check Java version
if java -version 2>&1 | grep -q "version \"21" || java -version 2>&1 | grep -q "version \"24"; then
    check_pass "Java 版本符合要求"
else
    check_warn "Java 版本可能不相容"
fi

# Check Maven
if command -v mvn >/dev/null 2>&1; then
    check_pass "Maven 已安裝"
else
    check_fail "Maven 未安裝"
fi

echo ""
echo "2️⃣  程式碼規範檢查"
echo "-----------------------------------------"

# Check for @ActiveProfiles in BaseUseCaseTest
if [ -f "$PROJECT_ROOT/src/test/java/tw/teddysoft/aiscrum/test/base/BaseUseCaseTest.java" ]; then
    if grep -q "@ActiveProfiles" "$PROJECT_ROOT/src/test/java/tw/teddysoft/aiscrum/test/base/BaseUseCaseTest.java"; then
        check_fail "BaseUseCaseTest 有 @ActiveProfiles (違反 ADR-021)"
    else
        check_pass "BaseUseCaseTest 沒有 @ActiveProfiles"
    fi
fi

# Check for ProfileSetter classes
if [ -f "$PROJECT_ROOT/src/test/java/tw/teddysoft/aiscrum/test/suite/OutboxProfileSetter.java" ]; then
    check_pass "OutboxProfileSetter 存在"
else
    check_warn "OutboxProfileSetter 不存在 (Test Suite 可能無法切換 profile)"
fi

if [ -f "$PROJECT_ROOT/src/test/java/tw/teddysoft/aiscrum/test/suite/InMemoryProfileSetter.java" ]; then
    check_pass "InMemoryProfileSetter 存在"
else
    check_warn "InMemoryProfileSetter 不存在 (Test Suite 可能無法切換 profile)"
fi

# Check for @Repository annotations in JPA interfaces
echo ""
echo "3️⃣  JPA Repository 檢查"
echo "-----------------------------------------"

JPA_FILES=$(find "$PROJECT_ROOT/src/main/java" -name "*OrmClient.java" -o -name "*ProjectionRepository.java" 2>/dev/null || true)
if [ -n "$JPA_FILES" ]; then
    HAS_REPOSITORY_ANNOTATION=false
    for file in $JPA_FILES; do
        if grep -q "@Repository" "$file"; then
            check_fail "$(basename $file) 有 @Repository 註解 (應該由 JpaConfiguration 處理)"
            HAS_REPOSITORY_ANNOTATION=true
        fi
    done
    if [ "$HAS_REPOSITORY_ANNOTATION" = false ]; then
        check_pass "JPA interfaces 沒有 @Repository 註解"
    fi
fi

# Check for Jakarta vs javax persistence
echo ""
echo "4️⃣  Jakarta Persistence 檢查"
echo "-----------------------------------------"

if find "$PROJECT_ROOT/src" -name "*.java" -exec grep -l "javax.persistence" {} \; 2>/dev/null | head -1 | grep -q .; then
    check_fail "發現 javax.persistence (應使用 jakarta.persistence)"
else
    check_pass "使用 jakarta.persistence"
fi

# Check for @Transient annotations in OutboxData
echo ""
echo "5️⃣  Outbox Pattern 檢查"
echo "-----------------------------------------"

PRODUCT_DATA="$PROJECT_ROOT/src/main/java/tw/teddysoft/aiscrum/product/usecase/port/out/ProductData.java"
if [ -f "$PRODUCT_DATA" ]; then
    if grep -q "@Transient.*domainEventDatas" "$PRODUCT_DATA" && grep -q "@Transient.*streamName" "$PRODUCT_DATA"; then
        check_pass "ProductData 有正確的 @Transient 註解"
    else
        check_warn "ProductData 可能缺少 @Transient 註解"
    fi
fi

# Check test cleanup
echo ""
echo "6️⃣  測試清理檢查"
echo "-----------------------------------------"

TEST_FILES=$(find "$PROJECT_ROOT/src/test/java" -name "*UseCaseTest.java" 2>/dev/null || true)
HAS_CLEANUP=false
for file in $TEST_FILES; do
    if grep -q "TRUNCATE TABLE" "$file"; then
        HAS_CLEANUP=true
        break
    fi
done

if [ "$HAS_CLEANUP" = true ]; then
    check_pass "測試有資料庫清理邏輯"
else
    check_warn "測試可能缺少資料庫清理 (Outbox profile 測試可能失敗)"
fi

# Compilation check
echo ""
echo "7️⃣  編譯檢查"
echo "-----------------------------------------"

cd "$PROJECT_ROOT"
if mvn clean compile -q 2>/dev/null; then
    check_pass "專案編譯成功"
else
    check_fail "專案編譯失敗"
fi

# Summary
echo ""
echo "========================================="
echo "📊 檢查結果摘要"
echo "========================================="
echo "通過: $PASSED_CHECKS / $TOTAL_CHECKS"
echo "警告: $WARNINGS"

if [ "$PASSED_CHECKS" -eq "$TOTAL_CHECKS" ] && [ "$WARNINGS" -eq 0 ]; then
    echo -e "${GREEN}✅ 實驗環境完全就緒！${NC}"
    exit 0
elif [ "$PASSED_CHECKS" -eq "$TOTAL_CHECKS" ]; then
    echo -e "${YELLOW}⚠️  實驗環境基本就緒，但有 $WARNINGS 個警告${NC}"
    exit 0
else
    FAILED=$((TOTAL_CHECKS - PASSED_CHECKS))
    echo -e "${RED}❌ 有 $FAILED 個檢查失敗，請修正後再開始實驗${NC}"
    echo ""
    echo "建議："
    echo "1. 查看 .dev/experiments/EXPERIMENT-SUCCESS-CHECKLIST.md"
    echo "2. 參考 ADR-021 和 ADR-019"
    echo "3. 執行 .ai/scripts/check-repository-compliance.sh"
    exit 1
fi