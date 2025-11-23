#!/bin/bash

echo "🔍 檢查 Spring Boot 配置..."
echo "================================"

ERRORS=0
WARNINGS=0

# 檢查 ByteBuddy scope
echo -n "檢查 ByteBuddy 依賴配置... "
if grep -q '<artifactId>byte-buddy</artifactId>' pom.xml 2>/dev/null; then
    if grep -A3 '<artifactId>byte-buddy</artifactId>' pom.xml | grep -q '<scope>test</scope>'; then
        echo "❌ ByteBuddy 不應該只在 test scope"
        ((ERRORS++))
    else
        echo "✅"
    fi
else
    echo "⚠️ 未找到 ByteBuddy 依賴"
    ((WARNINGS++))
fi

# 檢查 Jakarta Persistence API
echo -n "檢查 Jakarta Persistence API... "
if grep -q 'jakarta.persistence-api' pom.xml 2>/dev/null; then
    echo "✅"
else
    echo "❌ 缺少 jakarta.persistence-api 依賴"
    ((ERRORS++))
fi

# 檢查 @Enumerated on String
echo -n "檢查 @Enumerated 使用... "
if find src/main/java -name "*.java" -exec grep -l "@Enumerated.*String\|String.*@Enumerated" {} \; 2>/dev/null | head -1 | grep -q .; then
    echo "❌ 發現 String 欄位使用 @Enumerated"
    ((ERRORS++))
else
    echo "✅"
fi

# 檢查 EntityScan
echo -n "檢查 JPA EntityScan 配置... "
if [ -f "src/main/java/tw/teddysoft/aiscrum/io/springboot/config/orm/JpaConfiguration.java" ]; then
    if grep -q "tw.teddysoft.ezddd" src/main/java/tw/teddysoft/aiscrum/io/springboot/config/orm/JpaConfiguration.java; then
        echo "✅"
    else
        echo "⚠️ 可能缺少 ezddd entity 掃描"
        ((WARNINGS++))
    fi
else
    echo "⚠️ 未找到 JpaConfiguration"
    ((WARNINGS++))
fi

# 檢查 Profile 配置
echo -n "檢查 Profile 配置... "
if [ -f "src/main/java/tw/teddysoft/aiscrum/io/springboot/config/UseCaseConfiguration.java" ]; then
    if grep -q "prod-outbox" src/main/java/tw/teddysoft/aiscrum/io/springboot/config/UseCaseConfiguration.java; then
        echo "✅"
    else
        echo "⚠️ 可能缺少 prod-outbox profile 支援"
        ((WARNINGS++))
    fi
else
    echo "⚠️ 未找到 UseCaseConfiguration"
    ((WARNINGS++))
fi

# 檢查資料庫 URL 格式
echo -n "檢查資料庫 URL 配置... "
if find src/main/resources -name "application*.yml" -exec grep -l "jdbc:postgresql://localhost:5500" {} \; 2>/dev/null | head -1 | grep -q .; then
    echo "❌ 發現使用錯誤的 port 5500"
    ((ERRORS++))
elif find src/main/resources -name "application*.yml" -exec grep -l "currentSchema=" {} \; 2>/dev/null | head -1 | grep -q .; then
    echo "✅"
else
    echo "⚠️ 建議在 URL 中指定 schema"
    ((WARNINGS++))
fi

# 檢查 server port
echo -n "檢查 Server Port 設定... "
for port in $(grep -h "port:" src/main/resources/application*.yml 2>/dev/null | grep -v "#" | awk '{print $2}' | sort -u); do
    if lsof -i :$port >/dev/null 2>&1; then
        echo "⚠️ Port $port 已被佔用"
        ((WARNINGS++))
    fi
done
if [ $? -eq 0 ]; then
    echo "✅"
fi

echo "================================"
echo "檢查結果："
echo "  錯誤: $ERRORS"
echo "  警告: $WARNINGS"

if [ $ERRORS -gt 0 ]; then
    echo ""
    echo "❌ 發現配置錯誤，請參考 .ai/tech-stacks/java-ca-ezddd-spring/SPRING-BOOT-CONFIGURATION-CHECKLIST.md"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    echo ""
    echo "⚠️ 發現潛在問題，建議檢查"
    exit 0
else
    echo ""
    echo "✅ 所有檢查通過！"
    exit 0
fi