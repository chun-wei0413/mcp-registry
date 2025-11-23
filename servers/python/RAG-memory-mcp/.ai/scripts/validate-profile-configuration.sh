#!/bin/bash

# ============================================================================
# Profile Configuration Validation Script
# ============================================================================
# Purpose: Validate Spring Boot dual-profile configuration
# Usage: bash .ai/scripts/validate-profile-configuration.sh
# ============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Profile Configuration Validation${NC}"
echo -e "${BLUE}========================================${NC}"

# Track errors
errors=0
warnings=0

# Function to check if file exists
check_file() {
    local file=$1
    local description=$2
    if [ -f "$file" ]; then
        echo -e "${GREEN}✅ $description exists${NC}"
        return 0
    else
        echo -e "${RED}❌ $description missing: $file${NC}"
        ((errors++))
        return 1
    fi
}

# Function to check file content
check_content() {
    local file=$1
    local pattern=$2
    local description=$3
    if grep -q "$pattern" "$file" 2>/dev/null; then
        echo -e "${GREEN}✅ $description${NC}"
        return 0
    else
        echo -e "${RED}❌ $description not found in $file${NC}"
        ((errors++))
        return 1
    fi
}

# Function to check for problematic patterns
check_no_pattern() {
    local file=$1
    local pattern=$2
    local description=$3
    if grep -q "$pattern" "$file" 2>/dev/null; then
        echo -e "${RED}❌ $description found in $file (should not exist)${NC}"
        ((errors++))
        return 1
    else
        echo -e "${GREEN}✅ $description not present${NC}"
        return 0
    fi
}

echo -e "\n${YELLOW}1. Checking Property Files...${NC}"
echo "----------------------------------------"

# Check main properties
check_file "src/main/resources/application.properties" "Main application.properties"
check_file "src/main/resources/application-inmemory.properties" "InMemory profile properties"
check_file "src/main/resources/application-outbox.properties" "Outbox profile properties"

# Check test properties
check_file "src/test/resources/application.properties" "Test application.properties"
if [ -f "src/test/resources/application.properties" ]; then
    check_content "src/test/resources/application.properties" "SPRING_PROFILES_ACTIVE:" "Environment variable support"
fi

check_file "src/test/resources/application-test-inmemory.properties" "Test InMemory properties"
check_file "src/test/resources/application-test-outbox.properties" "Test Outbox properties"

echo -e "\n${YELLOW}2. Checking InMemory Configuration...${NC}"
echo "----------------------------------------"

# Check InMemory properties
if [ -f "src/main/resources/application-inmemory.properties" ]; then
    check_content "src/main/resources/application-inmemory.properties" \
        "spring.autoconfigure.exclude.*DataSourceAutoConfiguration" \
        "DataSource exclusion for InMemory"
    
    check_content "src/main/resources/application-inmemory.properties" \
        "spring.jpa.enabled=false" \
        "JPA disabled for InMemory"
fi

# Check InMemoryRepositoryConfig
INMEMORY_CONFIG=$(find src/main/java -name "*InMemoryRepositoryConfig.java" -o -name "*InMemoryConfiguration.java" | head -1)
if [ -n "$INMEMORY_CONFIG" ]; then
    echo -e "${GREEN}✅ InMemory configuration class found${NC}"
    
    # Check for correct profile annotation
    if grep -E '@Profile.*inmemory.*test-inmemory' "$INMEMORY_CONFIG" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ InMemory config supports both profiles${NC}"
    else
        echo -e "${RED}❌ InMemory config missing dual profile support${NC}"
        ((errors++))
    fi
    
    # Check for GenericInMemoryRepository usage
    check_content "$INMEMORY_CONFIG" "GenericInMemoryRepository" "Uses GenericInMemoryRepository"
    
    # Check for MessageBus
    check_content "$INMEMORY_CONFIG" "MessageBus<DomainEvent>" "MessageBus bean defined"
else
    echo -e "${RED}❌ InMemory configuration class not found${NC}"
    ((errors++))
fi

echo -e "\n${YELLOW}3. Checking Outbox Configuration...${NC}"
echo "----------------------------------------"

# Check Outbox properties
if [ -f "src/main/resources/application-outbox.properties" ]; then
    check_content "src/main/resources/application-outbox.properties" \
        "spring.datasource.url" \
        "DataSource URL configured"
    
    check_content "src/main/resources/application-outbox.properties" \
        "spring.jpa" \
        "JPA configuration present"
fi

# Check OutboxRepositoryConfig
OUTBOX_CONFIG=$(find src/main/java -name "*OutboxRepositoryConfig.java" -o -name "*OutboxConfiguration.java" | head -1)
if [ -n "$OUTBOX_CONFIG" ]; then
    echo -e "${GREEN}✅ Outbox configuration class found${NC}"
    
    # Check for @EnableJpaRepositories
    check_content "$OUTBOX_CONFIG" "@EnableJpaRepositories" "JPA repositories enabled"
    
    # Check for JpaRepositoryFactory usage
    check_content "$OUTBOX_CONFIG" "JpaRepositoryFactory" "Uses JpaRepositoryFactory for PgMessageDbClient"
else
    echo -e "${YELLOW}⚠️  Outbox configuration class not found (OK if only using InMemory)${NC}"
    ((warnings++))
fi

echo -e "\n${YELLOW}4. Checking Default Configuration...${NC}"
echo "----------------------------------------"

DEFAULT_CONFIG=$(find src/main/java -name "DefaultRepositoryConfig.java" | head -1)
if [ -n "$DEFAULT_CONFIG" ]; then
    echo -e "${GREEN}✅ Default configuration found${NC}"
    
    # Check that it excludes all specific profiles
    if grep -E '@Profile.*!test-inmemory' "$DEFAULT_CONFIG" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Default config excludes test-inmemory${NC}"
    else
        echo -e "${RED}❌ Default config missing test-inmemory exclusion${NC}"
        ((errors++))
    fi
fi

echo -e "\n${YELLOW}5. Checking Test Configuration...${NC}"
echo "----------------------------------------"

# Check BaseUseCaseTest for @ActiveProfiles (should NOT have it)
BASE_TEST=$(find src/test/java -name "BaseUseCaseTest.java" -o -name "BaseSpringBootTest.java" | head -1)
if [ -n "$BASE_TEST" ]; then
    check_no_pattern "$BASE_TEST" "@ActiveProfiles" "No @ActiveProfiles in base test class"
fi

# Check for TestSuite with ProfileSetter
INMEMORY_SUITE=$(find src/test/java -name "*InMemory*TestSuite.java" -o -name "*InMemory*Suite.java" | head -1)
if [ -n "$INMEMORY_SUITE" ]; then
    echo -e "${GREEN}✅ InMemory TestSuite found${NC}"
    check_content "$INMEMORY_SUITE" "ProfileSetter" "ProfileSetter pattern used"
fi

echo -e "\n${YELLOW}6. Checking Controller Profile Restrictions...${NC}"
echo "----------------------------------------"

# Check BurndownController for profile restriction
BURNDOWN_CONTROLLER="src/main/java/tw/teddysoft/aiscrum/burndown/adapter/in/web/BurndownController.java"
if [ -f "$BURNDOWN_CONTROLLER" ]; then
    check_content "$BURNDOWN_CONTROLLER" "@Profile" "BurndownController has profile restriction"
fi

echo -e "\n${YELLOW}7. Running Quick Validation Tests...${NC}"
echo "----------------------------------------"

# Try to compile the project
echo "Compiling project..."
if mvn compile -q > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Project compiles successfully${NC}"
else
    echo -e "${RED}❌ Project compilation failed${NC}"
    ((errors++))
fi

# Check if InMemory tests can run
echo "Testing InMemory profile activation..."
if mvn test -Dtest=InMemoryUseCaseTestSuite -Dspring.profiles.active=test-inmemory -q > /dev/null 2>&1; then
    echo -e "${GREEN}✅ InMemory profile tests pass${NC}"
else
    echo -e "${YELLOW}⚠️  InMemory profile tests failed (check manually)${NC}"
    ((warnings++))
fi

echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}Validation Summary${NC}"
echo -e "${BLUE}========================================${NC}"

if [ $errors -eq 0 ]; then
    if [ $warnings -eq 0 ]; then
        echo -e "${GREEN}✅ All checks passed! Profile configuration is correct.${NC}"
    else
        echo -e "${YELLOW}⚠️  Configuration has $warnings warning(s) but no critical errors.${NC}"
    fi
    exit 0
else
    echo -e "${RED}❌ Found $errors error(s) in profile configuration!${NC}"
    echo -e "${RED}Please fix the issues above before proceeding.${NC}"
    
    echo -e "\n${YELLOW}Common fixes:${NC}"
    echo "1. Ensure test application.properties uses: spring.profiles.active=\${SPRING_PROFILES_ACTIVE:test-inmemory}"
    echo "2. InMemoryRepositoryConfig must have: @Profile({\"inmemory\", \"test-inmemory\"})"
    echo "3. DefaultRepositoryConfig must exclude: !test-inmemory"
    echo "4. Remove @ActiveProfiles from BaseUseCaseTest"
    echo "5. Use ProfileSetter pattern in TestSuites"
    
    exit 1
fi