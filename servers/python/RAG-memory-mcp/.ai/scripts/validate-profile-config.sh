#!/bin/bash

# Profile Configuration Validation Script
# Purpose: Validate Spring Profile configuration to prevent startup failures

set -e

echo "================================================="
echo "   Spring Profile Configuration Validator       "
echo "================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
ERRORS=0
WARNINGS=0

# Function to check file exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✅${NC} $1 exists"
        return 0
    else
        echo -e "${RED}❌${NC} $1 is missing"
        ((ERRORS++))
        return 1
    fi
}

# Function to check property exists in file
check_property() {
    if grep -q "$2" "$1" 2>/dev/null; then
        echo -e "${GREEN}  ✓${NC} $2 found in $1"
        return 0
    else
        echo -e "${RED}  ✗${NC} $2 NOT found in $1"
        ((WARNINGS++))
        return 1
    fi
}

echo ""
echo "1. Checking Properties Files..."
echo "--------------------------------"

# Check main properties file
if check_file "src/main/resources/application.properties"; then
    # Check active profile setting
    ACTIVE_PROFILE=$(grep "spring.profiles.active" src/main/resources/application.properties 2>/dev/null | cut -d'=' -f2)
    if [ -n "$ACTIVE_PROFILE" ]; then
        echo -e "${GREEN}  ✓${NC} Active profile: $ACTIVE_PROFILE"
    else
        echo -e "${YELLOW}  ⚠${NC} No active profile set (will use default)"
        ((WARNINGS++))
    fi
fi

# Check InMemory properties
echo ""
echo "2. Checking InMemory Configuration..."
echo "--------------------------------------"
INMEM_PROPS="src/main/resources/application-inmemory.properties"

if check_file "$INMEM_PROPS"; then
    # Critical: Check DataSource exclusion
    if grep -q "spring.autoconfigure.exclude" "$INMEM_PROPS"; then
        if grep -q "DataSourceAutoConfiguration" "$INMEM_PROPS"; then
            echo -e "${GREEN}  ✓${NC} DataSource autoconfiguration is excluded"
        else
            echo -e "${RED}  ✗${NC} DataSource autoconfiguration is NOT excluded - will cause startup failure!"
            ((ERRORS++))
        fi
        
        if grep -q "HibernateJpaAutoConfiguration" "$INMEM_PROPS"; then
            echo -e "${GREEN}  ✓${NC} JPA autoconfiguration is excluded"
        else
            echo -e "${YELLOW}  ⚠${NC} JPA autoconfiguration is NOT excluded"
            ((WARNINGS++))
        fi
    else
        echo -e "${RED}  ✗${NC} No autoconfigure exclusions found - InMemory will fail!"
        ((ERRORS++))
    fi
    
    # Check JPA disabled
    check_property "$INMEM_PROPS" "spring.jpa.enabled=false"
fi

# Check Outbox properties
echo ""
echo "3. Checking Outbox Configuration..."
echo "------------------------------------"
OUTBOX_PROPS="src/main/resources/application-outbox.properties"

if [ -f "$OUTBOX_PROPS" ]; then
    echo -e "${GREEN}✅${NC} $OUTBOX_PROPS exists"
    
    # Check required database properties
    check_property "$OUTBOX_PROPS" "spring.datasource.url"
    check_property "$OUTBOX_PROPS" "spring.datasource.username"
    check_property "$OUTBOX_PROPS" "spring.datasource.driver-class-name"
    check_property "$OUTBOX_PROPS" "spring.jpa.hibernate.ddl-auto"
else
    echo -e "${YELLOW}⚠${NC} $OUTBOX_PROPS not found (OK if only using InMemory)"
    ((WARNINGS++))
fi

echo ""
echo "4. Checking Java Configuration Classes..."
echo "------------------------------------------"

# Check for Configuration classes
CONFIG_COUNT=$(find src/main/java -name "*Configuration.java" 2>/dev/null | wc -l)
if [ "$CONFIG_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✅${NC} Found $CONFIG_COUNT Configuration classes"
    
    # Check for @Profile annotations
    PROFILE_COUNT=$(grep -r "@Profile" src/main/java --include="*.java" 2>/dev/null | wc -l)
    if [ "$PROFILE_COUNT" -gt 0 ]; then
        echo -e "${GREEN}  ✓${NC} Found $PROFILE_COUNT @Profile annotations"
    else
        echo -e "${YELLOW}  ⚠${NC} No @Profile annotations found - beans may load in wrong profile"
        ((WARNINGS++))
    fi
    
    # Check for Repository beans
    REPO_BEAN_COUNT=$(grep -r "@Bean.*Repository" src/main/java --include="*.java" 2>/dev/null | wc -l)
    if [ "$REPO_BEAN_COUNT" -gt 0 ]; then
        echo -e "${GREEN}  ✓${NC} Found $REPO_BEAN_COUNT Repository bean definitions"
    else
        echo -e "${RED}  ✗${NC} No Repository beans found - will cause Bean Not Found error!"
        ((ERRORS++))
    fi
else
    echo -e "${RED}❌${NC} No Configuration classes found"
    ((ERRORS++))
fi

echo ""
echo "5. Checking for Common Issues..."
echo "---------------------------------"

# Check for conflicting profiles in same class
CONFLICT_COUNT=$(grep -r "@Profile.*inmemory.*outbox\|@Profile.*outbox.*inmemory" src/main/java --include="*.java" 2>/dev/null | wc -l)
if [ "$CONFLICT_COUNT" -gt 0 ]; then
    echo -e "${RED}❌${NC} Found conflicting profiles in same Configuration class!"
    ((ERRORS++))
else
    echo -e "${GREEN}✅${NC} No profile conflicts detected"
fi

# Check for MessageBus in InMemory config
if grep -r "MessageBus.*messageBus" src/main/java --include="*InMemory*.java" 2>/dev/null | grep -q "@Bean"; then
    echo -e "${GREEN}✅${NC} MessageBus bean found in InMemory configuration"
else
    echo -e "${YELLOW}⚠${NC} No MessageBus bean in InMemory configuration"
    ((WARNINGS++))
fi

echo ""
echo "================================================="
echo "                    SUMMARY                     "
echo "================================================="

if [ "$ERRORS" -eq 0 ] && [ "$WARNINGS" -eq 0 ]; then
    echo -e "${GREEN}✅ Configuration looks good!${NC}"
    echo "Your Spring Profile configuration should work correctly."
elif [ "$ERRORS" -eq 0 ]; then
    echo -e "${YELLOW}⚠ Configuration has $WARNINGS warning(s)${NC}"
    echo "The application should start, but review the warnings."
else
    echo -e "${RED}❌ Configuration has $ERRORS error(s) and $WARNINGS warning(s)${NC}"
    echo "The application will likely fail to start. Fix errors before proceeding."
    echo ""
    echo "Quick fixes:"
    echo "1. For InMemory DataSource errors: Add spring.autoconfigure.exclude to application-inmemory.properties"
    echo "2. For Bean Not Found errors: Ensure Repository beans are defined with correct @Profile"
    echo "3. Consider using: 請使用 profile-config-sub-agent workflow 配置 Spring Profiles"
fi

echo ""
echo "For detailed configuration guide, see:"
echo "- .ai/guides/PREVENT-REPOSITORY-BEAN-MISSING.md"
echo "- .ai/guides/PROFILE-CONFIGURATION-COMPLEXITY-SOLUTION.md"

exit $ERRORS