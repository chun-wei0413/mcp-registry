#!/bin/bash

# 生成 ezapp-starter 類別索引的腳本
# 用於幫助 AI 認識框架類別

echo "🔍 Generating ezapp-starter class index..."

# 找出所有相關的 JAR 檔案
echo "🔍 Finding ezapp and ezddd related JARs..."

# 產生類別清單
OUTPUT_FILE=".ai/ezapp-classes-raw.txt"
> "$OUTPUT_FILE"  # 清空檔案

# 提取 ezddd-core 類別
for JAR in $(find ~/.m2/repository/tw/teddysoft/ezddd -name "*.jar" 2>/dev/null); do
    echo "📦 Processing: $(basename $JAR)"
    jar tf "$JAR" 2>/dev/null | \
        grep "\.class$" | \
        grep -v "\$" | \
        sed 's/\.class$//' | \
        sed 's/\//./g' | \
        grep -E "^tw\.teddysoft\.(ezddd|ezcqrs)" >> "$OUTPUT_FILE"
done

# 提取 ezspec 類別
for JAR in $(find ~/.m2/repository/tw/teddysoft/ezspec -name "*.jar" 2>/dev/null); do
    echo "📦 Processing: $(basename $JAR)"
    jar tf "$JAR" 2>/dev/null | \
        grep "\.class$" | \
        grep -v "\$" | \
        sed 's/\.class$//' | \
        sed 's/\//./g' | \
        grep "^tw\.teddysoft\.ezspec" >> "$OUTPUT_FILE"
done

# 提取 ucontract 類別
for JAR in $(find ~/.m2/repository/tw/teddysoft/ucontract -name "*.jar" 2>/dev/null); do
    echo "📦 Processing: $(basename $JAR)"
    jar tf "$JAR" 2>/dev/null | \
        grep "\.class$" | \
        grep -v "\$" | \
        sed 's/\.class$//' | \
        sed 's/\//./g' | \
        grep "^tw\.teddysoft\.ucontract" >> "$OUTPUT_FILE"
done

# 提取 ezddd-gateway 類別
for JAR in $(find ~/.m2/repository/tw/teddysoft/ezddd-gateway -name "*.jar" 2>/dev/null); do
    echo "📦 Processing: $(basename $JAR)"
    jar tf "$JAR" 2>/dev/null | \
        grep "\.class$" | \
        grep -v "\$" | \
        sed 's/\.class$//' | \
        sed 's/\//./g' | \
        grep "^tw\.teddysoft\.ezddd" >> "$OUTPUT_FILE"
done

# 去重並排序
sort -u "$OUTPUT_FILE" -o "$OUTPUT_FILE"

CLASS_COUNT=$(wc -l < "$OUTPUT_FILE" | tr -d ' ')
echo "✅ Extracted $CLASS_COUNT classes"

# 分類類別
echo "📊 Categorizing classes..."

# Domain Layer
echo "## Domain Layer Classes" > .ai/ezapp-classes-categorized.txt
grep "\.domain\." "$OUTPUT_FILE" >> .ai/ezapp-classes-categorized.txt

echo "" >> .ai/ezapp-classes-categorized.txt
echo "## Use Case Layer Classes" >> .ai/ezapp-classes-categorized.txt
grep "\.usecase\." "$OUTPUT_FILE" | grep -v "\.port\." >> .ai/ezapp-classes-categorized.txt

echo "" >> .ai/ezapp-classes-categorized.txt
echo "## Repository Classes" >> .ai/ezapp-classes-categorized.txt
grep "\.repository\." "$OUTPUT_FILE" >> .ai/ezapp-classes-categorized.txt

echo "" >> .ai/ezapp-classes-categorized.txt
echo "## Projection Classes" >> .ai/ezapp-classes-categorized.txt
grep "\.projection\." "$OUTPUT_FILE" >> .ai/ezapp-classes-categorized.txt

echo "" >> .ai/ezapp-classes-categorized.txt
echo "## Inquiry Classes" >> .ai/ezapp-classes-categorized.txt
grep "\.inquiry\." "$OUTPUT_FILE" >> .ai/ezapp-classes-categorized.txt

echo "" >> .ai/ezapp-classes-categorized.txt
echo "## Archive Classes" >> .ai/ezapp-classes-categorized.txt
grep "\.archive\." "$OUTPUT_FILE" >> .ai/ezapp-classes-categorized.txt

echo "" >> .ai/ezapp-classes-categorized.txt
echo "## CQRS Classes" >> .ai/ezapp-classes-categorized.txt
grep "ezcqrs" "$OUTPUT_FILE" >> .ai/ezapp-classes-categorized.txt

echo "" >> .ai/ezapp-classes-categorized.txt
echo "## Reactor Classes" >> .ai/ezapp-classes-categorized.txt
grep "\.reactor\." "$OUTPUT_FILE" >> .ai/ezapp-classes-categorized.txt

echo "" >> .ai/ezapp-classes-categorized.txt
echo "## Testing Classes (ezSpec)" >> .ai/ezapp-classes-categorized.txt
grep "ezspec" "$OUTPUT_FILE" >> .ai/ezapp-classes-categorized.txt

echo "" >> .ai/ezapp-classes-categorized.txt
echo "## Contract Classes (uContract)" >> .ai/ezapp-classes-categorized.txt
grep "ucontract" "$OUTPUT_FILE" >> .ai/ezapp-classes-categorized.txt

echo "✅ Class index generated successfully!"
echo ""
echo "📄 Generated files:"
echo "   - .ai/ezapp-classes-raw.txt (raw class list)"
echo "   - .ai/ezapp-classes-categorized.txt (categorized classes)"
echo "   - .ai/ezapp-class-index.md (curated reference)"
echo ""
echo "💡 Tips for AI usage:"
echo "   1. Reference .ai/ezapp-class-index.md for common classes"
echo "   2. Check .ai/ezapp-classes-raw.txt for complete list"
echo "   3. Use .ai/guides/EZAPP-STARTER-API-REFERENCE.md for examples"