#!/bin/bash

# ====================================================================
# Spring DI Test Compliance Checker
# Purpose: Ensure all tests use Spring dependency injection
# ====================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}     Spring DI Test Compliance Checker          ${NC}"
echo -e "${BLUE}=================================================${NC}"
echo ""

ERRORS=0
WARNINGS=0

# Check 1: Detect hardcoded repository instantiation in tests
echo "1. Checking for hardcoded repository instantiation in tests..."
echo "----------------------------------------------------------------"

# Pattern 1: Direct instantiation of InMemory repositories
if grep -r "new GenericInMemoryRepository" src/test --include="*.java" 2>/dev/null; then
    echo -e "${RED}❌ Found hardcoded GenericInMemoryRepository instantiation in tests!${NC}"
    echo "   Tests must use Spring DI to inject repositories."
    grep -r "new GenericInMemoryRepository" src/test --include="*.java" | head -5
    ((ERRORS++))
else
    echo -e "${GREEN}✅ No hardcoded GenericInMemoryRepository found${NC}"
fi

# Pattern 2: Direct instantiation of Projections
if grep -r "new InMemory.*Projection" src/test --include="*.java" 2>/dev/null; then
    echo -e "${RED}❌ Found hardcoded Projection instantiation in tests!${NC}"
    echo "   Tests must use Spring DI to inject projections."
    grep -r "new InMemory.*Projection" src/test --include="*.java" | head -5
    ((ERRORS++))
else
    echo -e "${GREEN}✅ No hardcoded Projection instantiation found${NC}"
fi

# Pattern 3: Direct instantiation of Services in non-unit tests
if grep -r "new .*Service(" src/test --include="*UseCaseTest.java" 2>/dev/null; then
    echo -e "${YELLOW}⚠️  Found direct Service instantiation in UseCase tests${NC}"
    echo "   Consider using Spring DI for integration tests."
    grep -r "new .*Service(" src/test --include="*UseCaseTest.java" | head -5
    ((WARNINGS++))
fi

echo ""
echo "2. Checking for Spring Boot Test annotations..."
echo "------------------------------------------------"

# Check if UseCase tests have Spring annotations
for file in $(find src/test -name "*UseCaseTest.java" 2>/dev/null); do
    filename=$(basename "$file")
    
    # Check for @SpringBootTest or @DataJpaTest or @WebMvcTest
    if ! grep -q "@SpringBootTest\|@DataJpaTest\|@WebMvcTest" "$file"; then
        # Check if it's using manual TestContext (old pattern)
        if grep -q "static class TestContext" "$file"; then
            echo -e "${RED}❌ $filename uses manual TestContext instead of Spring DI${NC}"
            ((ERRORS++))
        else
            echo -e "${YELLOW}⚠️  $filename might not be using Spring Test${NC}"
            ((WARNINGS++))
        fi
    else
        echo -e "${GREEN}✅ $filename uses Spring Test annotations${NC}"
    fi
done

echo ""
echo "3. Checking for @Autowired or constructor injection..."
echo "-------------------------------------------------------"

# Check if tests use dependency injection
for file in $(find src/test -name "*Test.java" 2>/dev/null); do
    filename=$(basename "$file")
    
    # Skip unit tests that don't need Spring
    if [[ "$filename" == *"UnitTest.java" ]]; then
        continue
    fi
    
    # Check for injection patterns
    if grep -q "@Autowired\|@Inject\|@Resource" "$file" || \
       grep -q "public.*Test.*(" "$file"; then
        echo -e "${GREEN}✅ $filename uses dependency injection${NC}"
    elif grep -q "static class TestContext" "$file"; then
        echo -e "${RED}❌ $filename uses manual TestContext pattern${NC}"
        ((ERRORS++))
    fi
done

echo ""
echo "4. Checking for Profile-aware testing..."
echo "-----------------------------------------"

# Check if tests can work with multiple profiles
if ! grep -r "@ActiveProfiles" src/test --include="*.java" 2>/dev/null | grep -q "test-"; then
    echo -e "${YELLOW}⚠️  No tests found with @ActiveProfiles annotation${NC}"
    echo "   Tests might not be profile-aware"
    ((WARNINGS++))
else
    echo -e "${GREEN}✅ Found tests with @ActiveProfiles${NC}"
fi

# Check for profile-specific test configurations
if ls src/test/resources/application-test*.yml 2>/dev/null || \
   ls src/test/resources/application-test*.properties 2>/dev/null; then
    echo -e "${GREEN}✅ Found test-specific configuration files${NC}"
else
    echo -e "${YELLOW}⚠️  No test-specific configuration files found${NC}"
    ((WARNINGS++))
fi

echo ""
echo "================================================="
echo "                    SUMMARY                     "
echo "================================================="

if [ $ERRORS -gt 0 ]; then
    echo -e "${RED}❌ Found $ERRORS critical issues that must be fixed${NC}"
    echo ""
    echo "Recommendations:"
    echo "1. Replace manual TestContext with @SpringBootTest"
    echo "2. Use @Autowired or constructor injection for dependencies"
    echo "3. Use @TestConfiguration for test-specific beans"
    echo "4. Ensure tests work with both test-inmemory and test-outbox profiles"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Found $WARNINGS warnings to review${NC}"
    exit 0
else
    echo -e "${GREEN}✅ All tests properly use Spring DI!${NC}"
    exit 0
fi