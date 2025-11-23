#!/bin/bash

# Framework API Compliance Checker
# Purpose: Detect common ezddd framework API integration issues

set -e

echo "================================================="
echo "     ezddd Framework API Compliance Checker     "
echo "================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
ERRORS=0
WARNINGS=0
CHECKS=0

# Function to check for pattern
check_pattern() {
    local pattern="$1"
    local message="$2"
    local severity="$3"  # ERROR or WARNING
    
    ((CHECKS++))
    
    if grep -r "$pattern" src/ --include="*.java" 2>/dev/null | grep -v "^Binary file" > /dev/null; then
        if [ "$severity" == "ERROR" ]; then
            echo -e "${RED}❌ $message${NC}"
            echo "   Found in:"
            grep -r "$pattern" src/ --include="*.java" 2>/dev/null | grep -v "^Binary file" | head -3 | sed 's/^/     /'
            ((ERRORS++))
        else
            echo -e "${YELLOW}⚠️  $message${NC}"
            ((WARNINGS++))
        fi
        return 1
    else
        echo -e "${GREEN}✅ $message${NC}"
        return 0
    fi
}

# Function to check for correct pattern
check_correct_pattern() {
    local pattern="$1"
    local message="$2"
    
    ((CHECKS++))
    
    if grep -r "$pattern" src/ --include="*.java" 2>/dev/null | grep -v "^Binary file" > /dev/null; then
        echo -e "${GREEN}✅ $message${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️  $message not found (may be needed)${NC}"
        ((WARNINGS++))
        return 1
    fi
}

echo ""
echo "1. PgMessageDbClient Creation Check"
echo "------------------------------------"

# Check for incorrect PgMessageDbClient instantiation
check_pattern "new PgMessageDbClient" \
    "Found direct PgMessageDbClient instantiation (Must use JpaRepositoryFactory!)" \
    "ERROR"

# Check for correct JpaRepositoryFactory usage
check_correct_pattern "JpaRepositoryFactory.*entityManager" \
    "Found correct JpaRepositoryFactory usage"

# Check for getRepository(PgMessageDbClient.class)
check_correct_pattern "getRepository(PgMessageDbClient.class)" \
    "Found correct PgMessageDbClient creation"

echo ""
echo "2. Outbox Pattern Compliance Check"
echo "-----------------------------------"

# Check for standalone OutboxMapper classes
echo -e "${BLUE}Checking for OutboxMapper issues...${NC}"
for file in $(find src/ -name "*OutboxMapper.java" 2>/dev/null); do
    echo -e "${RED}❌ Found standalone OutboxMapper: $file${NC}"
    echo "   OutboxMapper must be an inner class of the main Mapper!"
    ((ERRORS++))
done

if [ "$ERRORS" -eq 0 ]; then
    echo -e "${GREEN}✅ No standalone OutboxMapper classes found${NC}"
fi

# Check for inner Mapper classes (correct pattern)
MAPPER_COUNT=$(grep -r "static class.*Mapper.*implements.*OutboxMapper" src/ --include="*.java" 2>/dev/null | wc -l)
if [ "$MAPPER_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✅ Found $MAPPER_COUNT correct inner OutboxMapper classes${NC}"
fi

echo ""
echo "3. Import Package Check"
echo "-----------------------"

# Check for javax.persistence (should be jakarta)
check_pattern "import javax\\.persistence" \
    "Found javax.persistence imports (Must use jakarta.persistence!)" \
    "ERROR"

# Check for correct jakarta.persistence
check_correct_pattern "import jakarta\\.persistence" \
    "Found correct jakarta.persistence imports"

echo ""
echo "4. @Transient Annotation Check"
echo "-------------------------------"

# Check for domainEventDatas without @Transient
echo -e "${BLUE}Checking @Transient annotations...${NC}"

# Find files with domainEventDatas field
FILES_WITH_EVENTS=$(grep -l "domainEventDatas" src/ -r --include="*.java" 2>/dev/null || true)

if [ -n "$FILES_WITH_EVENTS" ]; then
    for file in $FILES_WITH_EVENTS; do
        # Check if @Transient appears before domainEventDatas
        if grep -B1 "domainEventDatas" "$file" | grep -q "@Transient"; then
            echo -e "${GREEN}✅ $file has @Transient for domainEventDatas${NC}"
        else
            echo -e "${RED}❌ $file missing @Transient for domainEventDatas${NC}"
            ((ERRORS++))
        fi
    done
else
    echo -e "${BLUE}ℹ️  No domainEventDatas fields found${NC}"
fi

# Check for streamName without @Transient
FILES_WITH_STREAM=$(grep -l "private.*streamName" src/ -r --include="*.java" 2>/dev/null || true)

if [ -n "$FILES_WITH_STREAM" ]; then
    for file in $FILES_WITH_STREAM; do
        if grep -B1 "streamName" "$file" | grep -q "@Transient"; then
            echo -e "${GREEN}✅ $file has @Transient for streamName${NC}"
        else
            echo -e "${YELLOW}⚠️  $file might need @Transient for streamName${NC}"
            ((WARNINGS++))
        fi
    done
fi

echo ""
echo "5. OutboxData Implementation Check"
echo "-----------------------------------"

# Check for classes implementing OutboxData
OUTBOX_DATA_COUNT=$(grep -r "implements.*OutboxData" src/ --include="*.java" 2>/dev/null | wc -l)
if [ "$OUTBOX_DATA_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✅ Found $OUTBOX_DATA_COUNT OutboxData implementations${NC}"
    
    # Check each OutboxData implementation for required annotations
    for file in $(grep -l "implements.*OutboxData" src/ -r --include="*.java" 2>/dev/null); do
        echo -e "${BLUE}  Checking $file:${NC}"
        
        # Check for @Entity
        grep -q "@Entity" "$file" && echo -e "${GREEN}    ✓ Has @Entity${NC}" || echo -e "${RED}    ✗ Missing @Entity${NC}"
        
        # Check for @Id
        grep -q "@Id" "$file" && echo -e "${GREEN}    ✓ Has @Id${NC}" || echo -e "${RED}    ✗ Missing @Id${NC}"
        
        # Check for @Version
        grep -q "@Version" "$file" && echo -e "${GREEN}    ✓ Has @Version${NC}" || echo -e "${YELLOW}    ⚠ Missing @Version${NC}"
    done
else
    echo -e "${BLUE}ℹ️  No OutboxData implementations found${NC}"
fi

echo ""
echo "6. Common Import Mistakes"
echo "-------------------------"

# Check for wrong ezddd imports
check_pattern "tw\\.teddysoft\\.ezddd\\.core\\.entity\\.DomainEventData" \
    "Found wrong DomainEventData import (use usecase.port.inout.domainevent)" \
    "ERROR"

check_pattern "tw\\.teddysoft\\.ezddd\\.core\\.entity\\.outbox" \
    "Found wrong outbox import path" \
    "ERROR"

# Check for correct imports
check_correct_pattern "tw\\.teddysoft\\.ezddd\\.usecase\\.port\\.inout\\.domainevent\\.DomainEventData" \
    "Found correct DomainEventData import"

check_correct_pattern "tw\\.teddysoft\\.ezddd\\.usecase\\.port\\.out\\.repository\\.impl\\.outbox\\.OutboxData" \
    "Found correct OutboxData import"

echo ""
echo "================================================="
echo "                    SUMMARY                     "
echo "================================================="

echo "Checks performed: $CHECKS"

if [ "$ERRORS" -eq 0 ] && [ "$WARNINGS" -eq 0 ]; then
    echo -e "${GREEN}✅ All framework API checks passed!${NC}"
    echo "Your code follows ezddd framework best practices."
elif [ "$ERRORS" -eq 0 ]; then
    echo -e "${YELLOW}⚠️  Found $WARNINGS warning(s)${NC}"
    echo "Review warnings to ensure optimal framework usage."
else
    echo -e "${RED}❌ Found $ERRORS error(s) and $WARNINGS warning(s)${NC}"
    echo ""
    echo "Critical fixes needed:"
    echo "1. PgMessageDbClient: Must use JpaRepositoryFactory, never use 'new'"
    echo "2. OutboxMapper: Must be inner class, not standalone"
    echo "3. Imports: Use jakarta.persistence, not javax.persistence"
    echo "4. @Transient: Required for domainEventDatas and streamName"
    echo ""
    echo "See: .ai/guides/FRAMEWORK-API-INTEGRATION-GUIDE.md for details"
fi

exit $ERRORS