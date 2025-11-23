#!/bin/bash

# ConstructionEvent 和 DestructionEvent 介面實作檢查腳本
# 檢查所有 Aggregate 的 Created 和 Deleted 事件是否正確實作對應介面
# 根據 coding-standards.md 和 CODE-REVIEW-CHECKLIST.md 的規範
# 創建日期：2025-08-30

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "🔍 檢查 ConstructionEvent 和 DestructionEvent 介面實作..."
echo "=========================================="

# 定義顏色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 計數器
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNINGS=0

# 找出所有的 Events.java 檔案
EVENT_FILES=$(find src/main/java -name "*Events.java" -type f 2>/dev/null || true)

if [ -z "$EVENT_FILES" ]; then
    echo -e "${YELLOW}⚠️ 沒有找到任何 *Events.java 檔案${NC}"
    exit 0
fi

echo "找到以下 Event 檔案："
echo "$EVENT_FILES" | while read -r file; do
    echo "  - $(basename "$file")"
done
echo ""

# 處理每個檔案
for file in $EVENT_FILES; do
    [ -z "$file" ] && continue
    
    AGGREGATE_NAME=$(basename "$file" | sed 's/Events.java//')
    echo "檢查 $AGGREGATE_NAME 的事件..."
    
    # 檢查是否定義了 ConstructionEvent 和 DestructionEvent 介面
    if ! grep -q "^interface ConstructionEvent" "$file"; then
        echo -e "  ${YELLOW}⚠️ 缺少 ConstructionEvent 介面定義${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    if ! grep -q "^interface DestructionEvent" "$file"; then
        echo -e "  ${YELLOW}⚠️ 缺少 DestructionEvent 介面定義${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    # 檢查 [Aggregate]Created 事件
    CREATED_EVENT="${AGGREGATE_NAME}Created"
    
    # 使用更精確的方式檢查 record 定義
    if grep -E "record\s+${CREATED_EVENT}\s*\(" "$file" > /dev/null 2>&1; then
        TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
        
        # 獲取 record 定義到 implements 部分的內容（可能跨多行）
        # 使用 awk 找到從 record 開始到包含 implements 的完整定義
        RECORD_DEF=$(awk "/record ${CREATED_EVENT}/,/implements.*\{/" "$file" 2>/dev/null || true)
        
        if echo "$RECORD_DEF" | grep -q "ConstructionEvent"; then
            echo -e "  ${GREEN}✓${NC} ${CREATED_EVENT} 正確實作 ConstructionEvent"
            PASSED_CHECKS=$((PASSED_CHECKS + 1))
        else
            echo -e "  ${RED}✗${NC} ${CREATED_EVENT} 未實作 ConstructionEvent"
            echo -e "    應該是: record ${CREATED_EVENT}(...) implements ${AGGREGATE_NAME}Events, ConstructionEvent"
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
        fi
    fi
    
    # 檢查 [Aggregate]Deleted 事件
    DELETED_EVENT="${AGGREGATE_NAME}Deleted"
    
    if grep -E "record\s+${DELETED_EVENT}\s*\(" "$file" > /dev/null 2>&1; then
        TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
        
        # 獲取 record 定義到 implements 部分的內容
        RECORD_DEF=$(awk "/record ${DELETED_EVENT}/,/implements.*\{/" "$file" 2>/dev/null || true)
        
        if echo "$RECORD_DEF" | grep -q "DestructionEvent"; then
            echo -e "  ${GREEN}✓${NC} ${DELETED_EVENT} 正確實作 DestructionEvent"
            PASSED_CHECKS=$((PASSED_CHECKS + 1))
        else
            echo -e "  ${RED}✗${NC} ${DELETED_EVENT} 未實作 DestructionEvent"
            echo -e "    應該是: record ${DELETED_EVENT}(...) implements ${AGGREGATE_NAME}Events, DestructionEvent"
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
        fi
    fi
    
    # 檢查內部實體事件（不應該實作這些介面）
    # 檢查 TaskCreated (如果存在)
    if grep -E "record\s+TaskCreated" "$file" > /dev/null 2>&1; then
        TASK_CREATED_DEF=$(awk "/record TaskCreated/,/implements.*\{/" "$file" 2>/dev/null || true)
        if echo "$TASK_CREATED_DEF" | grep -q "ConstructionEvent"; then
            echo -e "  ${RED}✗${NC} TaskCreated 不應該實作 ConstructionEvent（Task 不是 Aggregate）"
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
            TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
        fi
    fi
    
    # 檢查 TaskDeleted (如果存在)
    if grep -E "record\s+TaskDeleted" "$file" > /dev/null 2>&1; then
        TASK_DELETED_DEF=$(awk "/record TaskDeleted/,/implements.*\{/" "$file" 2>/dev/null || true)
        if echo "$TASK_DELETED_DEF" | grep -q "DestructionEvent"; then
            echo -e "  ${RED}✗${NC} TaskDeleted 不應該實作 DestructionEvent（Task 不是 Aggregate）"
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
            TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
        fi
    fi
    
    echo ""
done

# 總結報告
echo "=========================================="
echo "📊 檢查結果總結："
echo ""
echo "總檢查項目: $TOTAL_CHECKS"
echo -e "通過檢查: ${GREEN}$PASSED_CHECKS${NC}"
echo -e "失敗檢查: ${RED}$FAILED_CHECKS${NC}"
echo -e "警告: ${YELLOW}$WARNINGS${NC}"
echo ""

if [ "$FAILED_CHECKS" -gt 0 ]; then
    echo -e "${RED}❌ 發現 $FAILED_CHECKS 個問題需要修正${NC}"
    echo ""
    echo "📚 相關規範文件："
    echo "  - .ai/tech-stacks/java-ca-ezddd-spring/coding-standards.md (第 641-681 行)"
    echo "  - .ai/tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md (第 444-463 行)"
    echo ""
    echo "修正建議："
    echo "1. 所有 Aggregate 的 [Aggregate]Created 事件必須實作 ConstructionEvent"
    echo "2. 所有 Aggregate 的 [Aggregate]Deleted 事件必須實作 DestructionEvent"
    echo "3. 內部實體（如 Task）的事件不需要實作這些介面"
    exit 1
elif [ "$WARNINGS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️ 發現 $WARNINGS 個警告，請考慮添加介面定義${NC}"
    exit 0
else
    echo -e "${GREEN}✅ 所有 ConstructionEvent 和 DestructionEvent 介面實作正確！${NC}"
    exit 0
fi