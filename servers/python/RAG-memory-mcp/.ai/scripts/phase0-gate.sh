#!/bin/bash

# 🔥 Phase 0 強制檢查門檻 - 防止測試中硬編碼 Repository
# 這個腳本必須在 exp-v25b 的每個 Phase 之前和之後執行

echo "=================================================="
echo "🔥 Phase 0 Gate: 防止硬編碼 Repository 檢查"
echo "=================================================="
echo ""

VIOLATIONS_FOUND=0
VIOLATIONS_FILE=".dev/PHASE0-VIOLATIONS.md"

# 創建違規記錄檔案
cat > "$VIOLATIONS_FILE" << 'EOF'
# Phase 0 違規記錄

## 檢查時間
EOF
echo "- $(date '+%Y-%m-%d %H:%M:%S')" >> "$VIOLATIONS_FILE"
echo "" >> "$VIOLATIONS_FILE"
echo "## 違規項目" >> "$VIOLATIONS_FILE"

echo "🔍 檢查 1: 搜尋硬編碼的 GenericInMemoryRepository..."
if grep -r "new GenericInMemoryRepository" src/test/java --include="*.java" 2>/dev/null; then
    echo "❌ 錯誤：發現測試中硬編碼 GenericInMemoryRepository！"
    echo "### ❌ 硬編碼 GenericInMemoryRepository" >> "$VIOLATIONS_FILE"
    grep -r "new GenericInMemoryRepository" src/test/java --include="*.java" 2>/dev/null >> "$VIOLATIONS_FILE"
    VIOLATIONS_FOUND=$((VIOLATIONS_FOUND + 1))
else
    echo "✅ 通過：沒有硬編碼的 GenericInMemoryRepository"
fi

echo ""
echo "🔍 檢查 2: 搜尋測試中直接 new 的 Repository..."
SUSPICIOUS_REPOS=$(grep -r "new .*Repository<" src/test/java --include="*.java" 2>/dev/null | grep -v "@Autowired" | grep -v "// OK" | grep -v "@Bean")
if [ -n "$SUSPICIOUS_REPOS" ]; then
    echo "❌ 錯誤：發現測試中可能硬編碼的 Repository 實例："
    echo "$SUSPICIOUS_REPOS"
    echo "### ❌ 可能硬編碼的 Repository" >> "$VIOLATIONS_FILE"
    echo "$SUSPICIOUS_REPOS" >> "$VIOLATIONS_FILE"
    VIOLATIONS_FOUND=$((VIOLATIONS_FOUND + 1))
else
    echo "✅ 通過：沒有可疑的硬編碼 Repository"
fi

echo ""
echo "🔍 檢查 3: 搜尋測試中的 TestContext.getInstance()..."
if grep -r "TestContext.getInstance()" src/test/java --include="*.java" 2>/dev/null; then
    echo "❌ 錯誤：發現使用 TestContext.getInstance()！應該使用 @SpringBootTest"
    echo "### ❌ 使用 TestContext.getInstance()" >> "$VIOLATIONS_FILE"
    grep -r "TestContext.getInstance()" src/test/java --include="*.java" 2>/dev/null >> "$VIOLATIONS_FILE"
    VIOLATIONS_FOUND=$((VIOLATIONS_FOUND + 1))
else
    echo "✅ 通過：沒有使用 TestContext.getInstance()"
fi

echo ""
echo "🔍 檢查 4: 確認測試使用 @SpringBootTest..."
TEST_FILES=$(find src/test/java -name "*Test.java" -o -name "*Tests.java" 2>/dev/null)
MISSING_SPRING_BOOT_TEST=""
for file in $TEST_FILES; do
    # 跳過 BaseUseCaseTest 和 TestSuite
    if [[ "$file" == *"BaseUseCaseTest.java" ]] || [[ "$file" == *"TestSuite.java" ]]; then
        continue
    fi
    
    # 檢查是否有 @SpringBootTest
    if ! grep -q "@SpringBootTest" "$file" 2>/dev/null; then
        # 檢查是否是需要 Spring 的測試（有 @Autowired 或 Repository）
        if grep -q -E "@Autowired|Repository<" "$file" 2>/dev/null; then
            MISSING_SPRING_BOOT_TEST="${MISSING_SPRING_BOOT_TEST}${file}\n"
        fi
    fi
done

if [ -n "$MISSING_SPRING_BOOT_TEST" ]; then
    echo "⚠️ 警告：以下測試可能需要 @SpringBootTest："
    echo -e "$MISSING_SPRING_BOOT_TEST"
    echo "### ⚠️ 可能缺少 @SpringBootTest" >> "$VIOLATIONS_FILE"
    echo -e "$MISSING_SPRING_BOOT_TEST" >> "$VIOLATIONS_FILE"
fi

echo ""
echo "🔍 檢查 5: 確認測試使用 @Autowired 注入 Repository..."
MANUAL_REPO_CREATION=$(grep -r "Repository.*=.*new" src/test/java --include="*.java" 2>/dev/null | grep -v "@Bean" | grep -v "// OK")
if [ -n "$MANUAL_REPO_CREATION" ]; then
    echo "❌ 錯誤：發現手動創建 Repository！應該使用 @Autowired"
    echo "$MANUAL_REPO_CREATION"
    echo "### ❌ 手動創建 Repository" >> "$VIOLATIONS_FILE"
    echo "$MANUAL_REPO_CREATION" >> "$VIOLATIONS_FILE"
    VIOLATIONS_FOUND=$((VIOLATIONS_FOUND + 1))
else
    echo "✅ 通過：Repository 都使用依賴注入"
fi

echo ""
echo "=================================================="

if [ $VIOLATIONS_FOUND -gt 0 ]; then
    echo "❌ Phase 0 檢查失敗！發現 $VIOLATIONS_FOUND 個違規項目"
    echo "違規詳情已記錄在: $VIOLATIONS_FILE"
    echo ""
    echo "🔥 必須修正所有違規項目才能繼續執行 exp-v25b！"
    echo ""
    echo "修正指引："
    echo "1. 所有測試必須使用 @SpringBootTest"
    echo "2. Repository 必須使用 @Autowired 注入"
    echo "3. 不能使用 TestContext.getInstance()"
    echo "4. 不能 new GenericInMemoryRepository"
    exit 1
else
    echo "✅ Phase 0 檢查全部通過！可以繼續執行 exp-v25b"
    echo "" >> "$VIOLATIONS_FILE"
    echo "## ✅ 所有檢查通過" >> "$VIOLATIONS_FILE"
    echo "可以安全執行 exp-v25b" >> "$VIOLATIONS_FILE"
fi

echo "=================================================="