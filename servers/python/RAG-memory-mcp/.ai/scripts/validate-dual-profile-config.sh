#!/bin/bash

# Script: validate-dual-profile-config.sh
# Purpose: Validate dual-profile (InMemory + Outbox) configuration
# This ensures new Use Cases support both profiles correctly

echo "======================================"
echo "Validating Dual-Profile Configuration"
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

ERROR_COUNT=0
WARNING_COUNT=0

# Function to check if a file contains a pattern
check_pattern() {
    local file=$1
    local pattern=$2
    grep -q "$pattern" "$file" 2>/dev/null
}

echo -e "\n${BLUE}1. Checking UseCaseConfiguration...${NC}"
echo "=================================="

USE_CASE_CONFIG="src/main/java/tw/teddysoft/aiscrum/io/springboot/config/UseCaseConfiguration.java"

if [ -f "$USE_CASE_CONFIG" ]; then
    # Check for @Qualifier usage
    if grep -q "@Qualifier" "$USE_CASE_CONFIG"; then
        echo -e "${RED}❌ ERROR: UseCaseConfiguration uses @Qualifier${NC}"
        echo "   This breaks dual-profile support!"
        echo "   Found:"
        grep -n "@Qualifier" "$USE_CASE_CONFIG" | head -3
        ((ERROR_COUNT++))
    else
        echo -e "${GREEN}✅ UseCaseConfiguration doesn't use @Qualifier${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  WARNING: UseCaseConfiguration.java not found${NC}"
    ((WARNING_COUNT++))
fi

echo -e "\n${BLUE}2. Checking Repository Configurations...${NC}"
echo "=================================="

INMEMORY_CONFIG="src/main/java/tw/teddysoft/aiscrum/io/springboot/config/InMemoryRepositoryConfig.java"
OUTBOX_CONFIG="src/main/java/tw/teddysoft/aiscrum/io/springboot/config/OutboxRepositoryConfig.java"

# List of expected aggregates
AGGREGATES=("product" "sprint" "pbi" "scrumteam")

echo -e "\n${YELLOW}Checking InMemoryRepositoryConfig...${NC}"
if [ -f "$INMEMORY_CONFIG" ]; then
    for aggregate in "${AGGREGATES[@]}"; do
        # Convert to PascalCase for class names
        pascal_case=$(echo "$aggregate" | sed -r 's/(^|_)([a-z])/\U\2/g')
        
        if grep -qi "${aggregate}.*Repository\|${pascal_case}.*Repository" "$INMEMORY_CONFIG"; then
            echo -e "  ✅ Found ${aggregate} repository"
        else
            echo -e "  ${YELLOW}⚠️  Missing ${aggregate} repository${NC}"
            ((WARNING_COUNT++))
        fi
    done
    
    # Check for @Profile annotation
    if grep -q '@Profile.*inmemory' "$INMEMORY_CONFIG"; then
        echo -e "  ✅ Has correct @Profile annotation"
    else
        echo -e "  ${RED}❌ Missing or incorrect @Profile annotation${NC}"
        ((ERROR_COUNT++))
    fi
else
    echo -e "${RED}❌ ERROR: InMemoryRepositoryConfig.java not found${NC}"
    ((ERROR_COUNT++))
fi

echo -e "\n${YELLOW}Checking OutboxRepositoryConfig...${NC}"
if [ -f "$OUTBOX_CONFIG" ]; then
    for aggregate in "${AGGREGATES[@]}"; do
        pascal_case=$(echo "$aggregate" | sed -r 's/(^|_)([a-z])/\U\2/g')
        
        if grep -qi "${aggregate}.*Repository\|${pascal_case}.*Repository" "$OUTBOX_CONFIG"; then
            echo -e "  ✅ Found ${aggregate} repository"
        else
            echo -e "  ${YELLOW}⚠️  Missing ${aggregate} repository${NC}"
            ((WARNING_COUNT++))
        fi
    done
    
    # Check for @Profile annotation
    if grep -q '@Profile.*outbox' "$OUTBOX_CONFIG"; then
        echo -e "  ✅ Has correct @Profile annotation"
    else
        echo -e "  ${RED}❌ Missing or incorrect @Profile annotation${NC}"
        ((ERROR_COUNT++))
    fi
    
    # Check for @Primary annotations
    if grep -q '@Primary' "$OUTBOX_CONFIG"; then
        echo -e "  ✅ Has @Primary annotations"
    else
        echo -e "  ${YELLOW}⚠️  Missing @Primary annotations on repository beans${NC}"
        ((WARNING_COUNT++))
    fi
else
    echo -e "${RED}❌ ERROR: OutboxRepositoryConfig.java not found${NC}"
    ((ERROR_COUNT++))
fi

echo -e "\n${BLUE}3. Checking OrmClient Interfaces...${NC}"
echo "=================================="

ORM_DIR="src/main/java/tw/teddysoft/aiscrum/io/springboot/config/orm"

if [ -d "$ORM_DIR" ]; then
    orm_count=$(find "$ORM_DIR" -name "*OrmClient.java" -type f | wc -l)
    echo -e "Found $orm_count OrmClient interfaces"
    
    # Check each OrmClient
    for file in "$ORM_DIR"/*OrmClient.java; do
        if [ -f "$file" ]; then
            basename=$(basename "$file")
            # Check if it extends SpringJpaClient
            if grep -q "extends SpringJpaClient" "$file"; then
                echo -e "  ✅ $basename extends SpringJpaClient"
            else
                echo -e "  ${RED}❌ $basename doesn't extend SpringJpaClient${NC}"
                ((ERROR_COUNT++))
            fi
        fi
    done
else
    echo -e "${YELLOW}⚠️  WARNING: ORM directory not found${NC}"
    echo "   Expected: $ORM_DIR"
    ((WARNING_COUNT++))
fi

echo -e "\n${BLUE}4. Checking Test Suite Structure...${NC}"
echo "=================================="

# Find test suites
INMEMORY_SUITES=$(find src/test -name "InMemory*TestSuite.java" -type f 2>/dev/null)
OUTBOX_SUITES=$(find src/test -name "Outbox*TestSuite.java" -type f 2>/dev/null)

echo -e "\n${YELLOW}InMemory Test Suites:${NC}"
if [ -n "$INMEMORY_SUITES" ]; then
    for suite in $INMEMORY_SUITES; do
        basename=$(basename "$suite")
        echo -n "  Checking $basename... "
        
        # Check for ProfileSetter
        if grep -q "ProfileSetter" "$suite"; then
            # Check if ProfileSetter is first in @SelectClasses
            if grep -A1 "@SelectClasses" "$suite" | grep -q "ProfileSetter.class.*// *MUST be first"; then
                echo -e "${GREEN}OK${NC}"
            else
                echo -e "${YELLOW}ProfileSetter might not be first${NC}"
                ((WARNING_COUNT++))
            fi
        else
            echo -e "${RED}Missing ProfileSetter${NC}"
            ((ERROR_COUNT++))
        fi
    done
else
    echo -e "  ${YELLOW}No InMemory test suites found${NC}"
fi

echo -e "\n${YELLOW}Outbox Test Suites:${NC}"
if [ -n "$OUTBOX_SUITES" ]; then
    for suite in $OUTBOX_SUITES; do
        basename=$(basename "$suite")
        echo -n "  Checking $basename... "
        
        # Check for ProfileSetter
        if grep -q "ProfileSetter" "$suite"; then
            # Check if ProfileSetter is first in @SelectClasses
            if grep -A1 "@SelectClasses" "$suite" | grep -q "ProfileSetter.class.*// *MUST be first"; then
                echo -e "${GREEN}OK${NC}"
            else
                echo -e "${YELLOW}ProfileSetter might not be first${NC}"
                ((WARNING_COUNT++))
            fi
        else
            echo -e "${RED}Missing ProfileSetter${NC}"
            ((ERROR_COUNT++))
        fi
    done
else
    echo -e "  ${YELLOW}No Outbox test suites found${NC}"
fi

echo -e "\n${BLUE}5. Checking JPA Configuration...${NC}"
echo "=================================="

JPA_CONFIG="src/main/java/tw/teddysoft/aiscrum/io/springboot/config/JpaConfiguration.java"

if [ -f "$JPA_CONFIG" ]; then
    # Check if ORM package is included
    if grep -q "tw.teddysoft.aiscrum.io.springboot.config.orm" "$JPA_CONFIG"; then
        echo -e "${GREEN}✅ JpaConfiguration includes ORM package${NC}"
    else
        echo -e "${RED}❌ JpaConfiguration doesn't include ORM package${NC}"
        ((ERROR_COUNT++))
    fi
    
    # Check @Profile annotation
    if grep -q '@Profile.*outbox' "$JPA_CONFIG"; then
        echo -e "${GREEN}✅ JpaConfiguration has correct @Profile${NC}"
    else
        echo -e "${RED}❌ JpaConfiguration missing @Profile for outbox${NC}"
        ((ERROR_COUNT++))
    fi
else
    echo -e "${YELLOW}⚠️  WARNING: JpaConfiguration.java not found${NC}"
    ((WARNING_COUNT++))
fi

# Summary
echo -e "\n======================================"
echo "Summary"
echo "======================================"

if [ $ERROR_COUNT -eq 0 ] && [ $WARNING_COUNT -eq 0 ]; then
    echo -e "${GREEN}✅ Dual-profile configuration is valid!${NC}"
    echo "Both InMemory and Outbox profiles are properly configured."
    exit 0
else
    if [ $ERROR_COUNT -gt 0 ]; then
        echo -e "${RED}❌ Found $ERROR_COUNT critical errors${NC}"
        echo "   These must be fixed for dual-profile support to work."
    fi
    
    if [ $WARNING_COUNT -gt 0 ]; then
        echo -e "${YELLOW}⚠️  Found $WARNING_COUNT warnings${NC}"
        echo "   Consider addressing these for complete coverage."
    fi
    
    echo -e "\n${YELLOW}Common fixes:${NC}"
    echo "1. Remove @Qualifier from UseCaseConfiguration"
    echo "2. Ensure both InMemory and Outbox configs have all repositories"
    echo "3. Add @Primary to Outbox repository beans"
    echo "4. Create OrmClient interfaces for Outbox pattern"
    echo "5. Ensure ProfileSetter is first in @SelectClasses"
    
    echo -e "\n${BLUE}See .ai/checklists/DUAL-PROFILE-TEST-CHECKLIST.md for details${NC}"
    
    exit 1
fi