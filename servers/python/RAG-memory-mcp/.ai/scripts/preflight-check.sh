#!/bin/bash

# Pre-flight Check Script
# Purpose: Verify project setup before code generation
# This helps prevent common issues like duplicate main classes and test anti-patterns

echo "🚀 Running pre-flight checks..."
echo "================================"

# Color codes for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

# 1. Check for duplicate main classes
echo -n "Checking for duplicate @SpringBootApplication classes... "
# Exclude .ai, .dev/specs, and target directories from search
MAIN_CLASSES=$(find . -path "./.ai" -prune -o -path "./.dev/specs" -prune -o -path "./target" -prune -o -name "*.java" -exec grep -l "@SpringBootApplication" {} \; 2>/dev/null | grep -v "^./.ai" | grep -v "^./.dev/specs" | wc -l)
if [ $MAIN_CLASSES -gt 1 ]; then
    echo -e "${RED}❌ ERROR${NC}"
    echo "  Multiple @SpringBootApplication classes found:"
    find . -path "./.ai" -prune -o -path "./.dev/specs" -prune -o -path "./target" -prune -o -name "*.java" -exec grep -l "@SpringBootApplication" {} \; 2>/dev/null | grep -v "^./.ai" | grep -v "^./.dev/specs" | sed 's/^/    - /'
    ((ERRORS++))
elif [ $MAIN_CLASSES -eq 0 ]; then
    echo -e "${YELLOW}⚠️ WARNING${NC}"
    echo "  No @SpringBootApplication class found"
    ((WARNINGS++))
else
    echo -e "${GREEN}✅ OK${NC}"
fi

# 2. Check main class location
echo -n "Checking main class location... "
MAIN_IN_ROOT=$(find src/main/java/tw/teddysoft/aiscrum -maxdepth 1 -name "*App.java" 2>/dev/null | wc -l)
MAIN_IN_SUBPACKAGE=$(find src/main/java/tw/teddysoft/aiscrum -mindepth 2 -name "*App.java" -exec grep -l "@SpringBootApplication" {} \; 2>/dev/null | wc -l)

if [ $MAIN_IN_ROOT -eq 0 ] && [ $MAIN_IN_SUBPACKAGE -gt 0 ]; then
    echo -e "${YELLOW}⚠️ WARNING${NC}"
    echo "  Main class found in sub-package (should be in root package):"
    find src/main/java/tw/teddysoft/aiscrum -mindepth 2 -name "*App.java" -exec grep -l "@SpringBootApplication" {} \; 2>/dev/null | sed 's/^/    - /'
    ((WARNINGS++))
elif [ $MAIN_IN_ROOT -eq 1 ]; then
    echo -e "${GREEN}✅ OK${NC}"
else
    echo -e "${GREEN}✅ OK${NC} (No main class yet)"
fi

# 3. Check for test anti-patterns
echo -n "Checking for test anti-patterns... "
SUPER_SETUP_CALLS=$(grep -r "super\.setUp" src/test --include="*.java" 2>/dev/null | wc -l)
SUPER_SETUP_EVENT_CALLS=$(grep -r "super\.setUpEventCapture" src/test --include="*.java" 2>/dev/null | wc -l)
TOTAL_ANTIPATTERNS=$((SUPER_SETUP_CALLS + SUPER_SETUP_EVENT_CALLS))

if [ $TOTAL_ANTIPATTERNS -gt 0 ]; then
    echo -e "${YELLOW}⚠️ WARNING${NC}"
    echo "  Found super.setUp*() calls in tests (should use utility methods):"
    grep -r "super\.setUp" src/test --include="*.java" 2>/dev/null | head -5 | sed 's/^/    - /'
    grep -r "super\.setUpEventCapture" src/test --include="*.java" 2>/dev/null | head -5 | sed 's/^/    - /'
    ((WARNINGS++))
else
    echo -e "${GREEN}✅ OK${NC}"
fi

# 4. Check for conflicting application.properties
echo -n "Checking application.properties configuration... "
MAIN_PROFILE=$(grep "^spring.profiles.active" src/main/resources/application.properties 2>/dev/null | cut -d= -f2)
TEST_PROFILE=$(grep "^spring.profiles.active" src/test/resources/application.properties 2>/dev/null | cut -d= -f2)

if [ ! -z "$MAIN_PROFILE" ] && [[ "$MAIN_PROFILE" == "outbox" ]]; then
    if [ -z "$TEST_PROFILE" ]; then
        echo -e "${YELLOW}⚠️ WARNING${NC}"
        echo "  Main application.properties sets 'outbox' profile but no test override found"
        echo "  Consider adding src/test/resources/application.properties with test profile"
        ((WARNINGS++))
    else
        echo -e "${GREEN}✅ OK${NC}"
    fi
else
    echo -e "${GREEN}✅ OK${NC}"
fi

# 5. Check for multiple Repository implementations
echo -n "Checking for custom Repository interfaces... "
CUSTOM_REPOS=$(find src/main/java -name "*Repository.java" -exec grep -l "extends Repository<" {} \; 2>/dev/null | grep -v "GenericInMemoryRepository" | wc -l)
if [ $CUSTOM_REPOS -gt 0 ]; then
    echo -e "${YELLOW}⚠️ WARNING${NC}"
    echo "  Found custom Repository interfaces (should use generic Repository<T,ID>):"
    find src/main/java -name "*Repository.java" -exec grep -l "extends Repository<" {} \; 2>/dev/null | grep -v "GenericInMemoryRepository" | head -5 | sed 's/^/    - /'
    ((WARNINGS++))
else
    echo -e "${GREEN}✅ OK${NC}"
fi

# Summary
echo ""
echo "================================"
echo "Pre-flight Check Summary:"
echo "================================"

if [ $ERRORS -gt 0 ]; then
    echo -e "${RED}❌ Found $ERRORS error(s) - must fix before proceeding${NC}"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}⚠️ Found $WARNINGS warning(s) - review recommended${NC}"
    exit 0
else
    echo -e "${GREEN}✅ All checks passed - ready for code generation${NC}"
    exit 0
fi