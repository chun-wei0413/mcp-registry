#!/bin/bash

# check-sub-agent-references.sh
# 檢查 Sub-agent prompts 是否包含必要的參考文件引用

echo "🔍 檢查 Sub-agent Prompts 參考文件設定..."
echo "========================================="

ERRORS=0
WARNINGS=0

# 定義必須檢查的 sub-agent prompts
declare -a SUB_AGENTS=(
    "command-sub-agent-prompt.md"
    "query-sub-agent-prompt.md"
    "outbox-sub-agent-prompt.md"
    "aggregate-sub-agent-prompt.md"
    "reactor-sub-agent-prompt.md"
)

# 定義關鍵參考文件
declare -a CRITICAL_REFS=(
    "COMPLETE-SPRING-BOOT-SETUP-GUIDE.md"
    "use-case-injection/README.md"
)

echo ""
echo "檢查 Sub-agent Prompts..."
echo "-------------------------"

for agent in "${SUB_AGENTS[@]}"; do
    agent_file=".ai/prompts/$agent"
    
    if [ ! -f "$agent_file" ]; then
        echo "⚠️  $agent 檔案不存在"
        ((WARNINGS++))
        continue
    fi
    
    echo -n "檢查 $agent... "
    
    # 檢查是否有 MANDATORY REFERENCES 區塊
    if grep -q "MANDATORY REFERENCES" "$agent_file"; then
        echo -n "✅ (有 MANDATORY REFERENCES 區塊) "
        
        # 檢查關鍵參考文件
        missing_refs=""
        for ref in "${CRITICAL_REFS[@]}"; do
            if ! grep -q "$ref" "$agent_file"; then
                missing_refs="$missing_refs $ref"
            fi
        done
        
        if [ -n "$missing_refs" ]; then
            echo ""
            echo "   ⚠️ 缺少參考:$missing_refs"
            ((WARNINGS++))
        else
            echo "✅"
        fi
    else
        echo "❌ 缺少 MANDATORY REFERENCES 區塊"
        ((ERRORS++))
    fi
done

echo ""
echo "檢查 CLAUDE.md 必讀文件區塊..."
echo "------------------------------"

if [ -f "CLAUDE.md" ]; then
    echo -n "檢查 CLAUDE.md... "
    
    if grep -q "新專案必讀文件" "CLAUDE.md"; then
        echo "✅ 有新專案必讀文件區塊"
        
        # 檢查是否包含關鍵文件路徑
        for ref in "${CRITICAL_REFS[@]}"; do
            if ! grep -q "$ref" "CLAUDE.md"; then
                echo "   ⚠️ CLAUDE.md 缺少參考: $ref"
                ((WARNINGS++))
            fi
        done
    else
        echo "❌ 缺少新專案必讀文件區塊"
        ((ERRORS++))
    fi
else
    echo "❌ CLAUDE.md 不存在"
    ((ERRORS++))
fi

echo ""
echo "檢查參考文件是否存在..."
echo "----------------------"

# 檢查關鍵文件是否實際存在
if [ -f ".ai/tech-stacks/java-ca-ezddd-spring/COMPLETE-SPRING-BOOT-SETUP-GUIDE.md" ]; then
    echo "✅ COMPLETE-SPRING-BOOT-SETUP-GUIDE.md 存在"
else
    echo "❌ COMPLETE-SPRING-BOOT-SETUP-GUIDE.md 不存在"
    ((ERRORS++))
fi

if [ -f ".ai/tech-stacks/java-ca-ezddd-spring/examples/use-case-injection/README.md" ]; then
    echo "✅ use-case-injection/README.md 存在"
else
    echo "❌ use-case-injection/README.md 不存在"
    ((ERRORS++))
fi

echo ""
echo "========================================="
echo "檢查結果："
echo "  錯誤: $ERRORS"
echo "  警告: $WARNINGS"

if [ $ERRORS -gt 0 ]; then
    echo ""
    echo "❌ 發現嚴重問題，Sub-agent 可能無法正確參考必要文件"
    echo "   請執行以下命令修復："
    echo "   1. 更新 sub-agent prompts 加入 MANDATORY REFERENCES"
    echo "   2. 確保 CLAUDE.md 包含新專案必讀文件區塊"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    echo ""
    echo "⚠️ 發現潛在問題，建議檢查並補充缺少的參考"
    exit 0
else
    echo ""
    echo "✅ 所有 Sub-agent 參考設定正確！"
    echo "   AI 將會自動讀取必要的配置文件"
    exit 0
fi