#!/bin/bash

# 🔥 強制檢查腳本 - 確保 AI 不會犯同樣的錯誤
# 這個腳本會在關鍵時刻自動執行

set -e  # 任何錯誤都會停止執行

echo "=================================================="
echo "🔥 MANDATORY CHECKS - 強制合規檢查"
echo "=================================================="

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 計數器
ERRORS=0
WARNINGS=0

# 函數：報告錯誤
report_error() {
    echo -e "${RED}❌ 錯誤：$1${NC}"
    ERRORS=$((ERRORS + 1))
}

# 函數：報告警告
report_warning() {
    echo -e "${YELLOW}⚠️ 警告：$1${NC}"
    WARNINGS=$((WARNINGS + 1))
}

# 函數：報告成功
report_success() {
    echo -e "${GREEN}✅ 通過：$1${NC}"
}

echo ""
echo "🔍 檢查 1: 測試中的硬編碼 Repository"
echo "----------------------------------------"
if grep -r "new GenericInMemoryRepository" src/test/java --include="*.java" 2>/dev/null; then
    report_error "發現測試中硬編碼 GenericInMemoryRepository！必須使用 @Autowired"
else
    report_success "沒有硬編碼的 GenericInMemoryRepository"
fi

echo ""
echo "🔍 檢查 2: TestContext.getInstance() 使用"
echo "----------------------------------------"
if grep -r "TestContext.getInstance()" src/test/java --include="*.java" 2>/dev/null; then
    report_error "發現使用 TestContext.getInstance()！必須使用 @SpringBootTest"
else
    report_success "沒有使用 TestContext.getInstance()"
fi

echo ""
echo "🔍 檢查 3: Repository Pattern 合規性"
echo "----------------------------------------"
# 檢查是否有自定義 Repository 介面
CUSTOM_REPOS=$(find src -name "*Repository.java" -type f | grep -v "GenericInMemoryRepository" | grep -v "OutboxRepository" | grep -v "BaseRepository")
if [ -n "$CUSTOM_REPOS" ]; then
    report_warning "發現可能的自定義 Repository 介面："
    echo "$CUSTOM_REPOS"
    echo "提醒：應該直接使用 Repository<Entity, ID> 而不是自定義介面"
fi

echo ""
echo "🔍 檢查 4: Profile 硬編碼"
echo "----------------------------------------"
# 檢查 BaseUseCaseTest 是否有 @ActiveProfiles
if grep -q "@ActiveProfiles" src/test/java/**/BaseUseCaseTest.java 2>/dev/null; then
    report_error "BaseUseCaseTest 不應該有 @ActiveProfiles！讓環境決定 profile"
fi

echo ""
echo "🔍 檢查 5: Outbox Pattern 合規性"
echo "----------------------------------------"
# 檢查 OutboxMapper 是否為內部類別
OUTBOX_MAPPERS=$(find src -name "*OutboxMapper.java" -type f 2>/dev/null)
if [ -n "$OUTBOX_MAPPERS" ]; then
    report_error "發現獨立的 OutboxMapper 檔案！OutboxMapper 必須是內部類別（ADR-019）"
    echo "$OUTBOX_MAPPERS"
fi

# 檢查 @Transient 註解
for data_file in $(find src -name "*Data.java" -path "*/port/out/*" -type f 2>/dev/null); do
    if ! grep -q "@Transient" "$data_file" 2>/dev/null; then
        report_warning "$data_file 可能缺少 @Transient 註解"
    fi
done

echo ""
echo "🔍 檢查 6: Import 正確性"
echo "----------------------------------------"
# 檢查是否使用 javax.persistence（應該用 jakarta）
if grep -r "import javax.persistence" src --include="*.java" 2>/dev/null; then
    report_error "發現使用 javax.persistence！應該使用 jakarta.persistence"
fi

echo ""
echo "🔍 檢查 7: 測試註解正確性"
echo "----------------------------------------"
# 檢查需要 Spring 的測試是否有 @SpringBootTest
for test_file in $(find src/test/java -name "*Test.java" -o -name "*Tests.java" 2>/dev/null); do
    if [[ "$test_file" == *"BaseUseCaseTest.java" ]] || [[ "$test_file" == *"TestSuite.java" ]]; then
        continue
    fi
    
    if grep -q "@Autowired" "$test_file" 2>/dev/null; then
        if ! grep -q "@SpringBootTest" "$test_file" 2>/dev/null; then
            report_warning "$test_file 使用 @Autowired 但缺少 @SpringBootTest"
        fi
    fi
done

echo ""
echo "=================================================="
echo "📊 檢查結果摘要"
echo "=================================================="

if [ $ERRORS -gt 0 ]; then
    echo -e "${RED}❌ 發現 $ERRORS 個錯誤，必須立即修正！${NC}"
    echo ""
    echo "修正建議："
    echo "1. 所有測試使用 @SpringBootTest + @Autowired"
    echo "2. 不要硬編碼 Repository 或 TestContext"
    echo "3. OutboxMapper 必須是內部類別"
    echo "4. 使用 jakarta.persistence 而非 javax"
    echo "5. BaseUseCaseTest 不要加 @ActiveProfiles"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}⚠️ 發現 $WARNINGS 個警告，建議檢查${NC}"
    exit 0
else
    echo -e "${GREEN}✅ 所有檢查通過！系統完全合規${NC}"
    exit 0
fi