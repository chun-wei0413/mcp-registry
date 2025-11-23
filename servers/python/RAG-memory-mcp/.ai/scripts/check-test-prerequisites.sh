#!/bin/bash

# Check Test Prerequisites for ezSpec BDD Testing
# This script ensures all necessary test infrastructure is in place

echo "🔍 Checking test prerequisites for ezSpec BDD testing..."
echo "=============================================="

ERRORS=0
WARNINGS=0

# 1. Check if BaseUseCaseTest exists
echo ""
echo "1️⃣ Checking BaseUseCaseTest..."
BASE_TEST_FILE=$(find src/test/java -name "BaseUseCaseTest.java" 2>/dev/null | head -1)

if [ -z "$BASE_TEST_FILE" ]; then
    echo "❌ BaseUseCaseTest.java not found!"
    echo "   Action Required: Create BaseUseCaseTest from template"
    echo "   Template location: .ai/tech-stacks/java-ca-ezddd-spring/examples/test/BaseUseCaseTest.java"
    ERRORS=$((ERRORS + 1))
else
    echo "✅ BaseUseCaseTest found at: $BASE_TEST_FILE"
fi

# 2. Check if ezSpec dependency exists in pom.xml
echo ""
echo "2️⃣ Checking ezSpec dependency in pom.xml..."
if grep -q "ezspec" pom.xml 2>/dev/null; then
    echo "✅ ezSpec dependency found in pom.xml"
else
    echo "❌ ezSpec dependency not found in pom.xml!"
    echo "   Action Required: Add ezspec dependency to pom.xml"
    echo "   Dependency is included in ezapp-starter"
    ERRORS=$((ERRORS + 1))
fi

# 3. Check if test-generation-prompt.md exists
echo ""
echo "3️⃣ Checking test-generation-prompt.md..."
if [ -f ".ai/prompts/test-generation-prompt.md" ]; then
    echo "✅ test-generation-prompt.md exists"
else
    echo "⚠️ test-generation-prompt.md not found!"
    echo "   Location should be: .ai/prompts/test-generation-prompt.md"
    WARNINGS=$((WARNINGS + 1))
fi

# 4. Check if ezSpec examples exist
echo ""
echo "4️⃣ Checking ezSpec examples..."
EXAMPLE_DIR=".ai/tech-stacks/java-ca-ezddd-spring/examples/test"
if [ -d "$EXAMPLE_DIR" ]; then
    EXAMPLE_COUNT=$(ls -1 "$EXAMPLE_DIR"/*.java 2>/dev/null | wc -l)
    if [ "$EXAMPLE_COUNT" -gt 0 ]; then
        echo "✅ Found $EXAMPLE_COUNT ezSpec examples in $EXAMPLE_DIR"
    else
        echo "⚠️ No Java examples found in $EXAMPLE_DIR"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo "⚠️ Example directory not found: $EXAMPLE_DIR"
    WARNINGS=$((WARNINGS + 1))
fi

# 5. Check for @Test usage in existing tests (should use @EzScenario instead)
echo ""
echo "5️⃣ Checking for incorrect @Test usage..."
if [ -d "src/test/java" ]; then
    TEST_COUNT=$(grep -r "@Test" src/test/java --include="*UseCaseTest.java" 2>/dev/null | wc -l)
    if [ "$TEST_COUNT" -gt 0 ]; then
        echo "⚠️ Found $TEST_COUNT @Test annotations in UseCase tests"
        echo "   These should be replaced with @EzScenario for BDD style tests"
        WARNINGS=$((WARNINGS + 1))
    else
        echo "✅ No @Test annotations found in UseCase tests (good!)"
    fi
fi

# 6. Check if Spring profiles are configured correctly
echo ""
echo "6️⃣ Checking Spring test profiles..."
if [ -f "src/test/resources/application-test.yml" ] || [ -f "src/test/resources/application-test.properties" ]; then
    echo "✅ Test configuration file exists"
else
    echo "⚠️ No test configuration file found (application-test.yml or .properties)"
    echo "   Consider creating one for test-specific configurations"
    WARNINGS=$((WARNINGS + 1))
fi

# Summary
echo ""
echo "=============================================="
echo "📊 Test Prerequisites Check Summary"
echo "=============================================="

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo "✅ All test prerequisites are satisfied!"
    echo "   Ready to implement ezSpec BDD tests"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo "⚠️ Test prerequisites check completed with $WARNINGS warning(s)"
    echo "   You can proceed but consider addressing the warnings"
    exit 0
else
    echo "❌ Test prerequisites check failed with $ERRORS error(s) and $WARNINGS warning(s)"
    echo ""
    echo "📋 Required Actions:"
    echo "1. Create BaseUseCaseTest if missing"
    echo "2. Ensure ezSpec dependency is in pom.xml (via ezapp-starter)"
    echo "3. Read test-generation-prompt.md for guidelines"
    echo "4. Use ezSpec examples as templates"
    echo ""
    echo "🔧 Quick Fix Commands:"
    echo "   # Copy BaseUseCaseTest template"
    echo "   mkdir -p src/test/java/tw/teddysoft/aiscrum/test/base"
    echo "   cp .ai/tech-stacks/java-ca-ezddd-spring/examples/test/BaseUseCaseTest.java \\"
    echo "      src/test/java/tw/teddysoft/aiscrum/test/base/"
    exit 1
fi