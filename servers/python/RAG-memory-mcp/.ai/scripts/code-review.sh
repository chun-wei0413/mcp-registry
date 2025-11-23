#!/bin/bash

# ====================================================================
# Intelligent Code Review Script
# 
# Purpose: 根據變更內容智能執行相關的 code review 檢查
# Usage: ./code-review.sh [commit-range]
# Example: ./code-review.sh HEAD~1..HEAD
# ====================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Default to comparing with main branch if no commit range specified
COMMIT_RANGE="${1:-main...HEAD}"

# Track what needs to be checked
CHECK_REPOSITORY=false
CHECK_MAPPER=false
CHECK_CODING_STANDARDS=false
CHECK_JPA_PROJECTION=false
CHECK_SPEC=false
CHECK_AGGREGATE=false
CHECK_USECASE=false
CHECK_CONTROLLER=false
CHECK_EVENT_SOURCING=false
CHECK_REACTOR=false
CHECK_TEST_SPRING_DI=false
CHECK_DOMAIN_EVENTS=false

# Track results
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNINGS=0

# Function to run a check
run_check() {
    local script_name=$1
    local description=$2
    
    ((TOTAL_CHECKS++))
    
    echo ""
    echo -e "${CYAN}▶ Running:${NC} $description"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    if [ -f "$SCRIPT_DIR/$script_name" ]; then
        if [ -x "$SCRIPT_DIR/$script_name" ]; then
            if "$SCRIPT_DIR/$script_name" 2>&1; then
                echo -e "${GREEN}✓ PASSED${NC}: $description"
                ((PASSED_CHECKS++))
            else
                echo -e "${RED}✗ FAILED${NC}: $description"
                ((FAILED_CHECKS++))
            fi
        else
            echo -e "${YELLOW}⚠ WARNING${NC}: $script_name is not executable"
            chmod +x "$SCRIPT_DIR/$script_name"
            echo "  Fixed: Made script executable"
            if "$SCRIPT_DIR/$script_name" 2>&1; then
                echo -e "${GREEN}✓ PASSED${NC}: $description"
                ((PASSED_CHECKS++))
            else
                echo -e "${RED}✗ FAILED${NC}: $description"
                ((FAILED_CHECKS++))
            fi
        fi
    else
        echo -e "${YELLOW}⚠ SKIPPED${NC}: $script_name not found"
        ((WARNINGS++))
    fi
}

# Header
echo ""
echo -e "${MAGENTA}╔════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║     Intelligent Code Review Check      ║${NC}"
echo -e "${MAGENTA}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Analyzing changes in: ${YELLOW}$COMMIT_RANGE${NC}"
echo ""

# ====================================================================
# Analyze what changed
# ====================================================================

echo -e "${CYAN}Analyzing changed files...${NC}"

# Get list of changed files
if [ "$COMMIT_RANGE" == "staged" ]; then
    # For staged changes
    CHANGED_FILES=$(git diff --cached --name-only)
else
    # For commit range
    CHANGED_FILES=$(git diff --name-only $COMMIT_RANGE 2>/dev/null || git diff --name-only)
fi

if [ -z "$CHANGED_FILES" ]; then
    echo -e "${YELLOW}No changes detected. Checking staged files...${NC}"
    CHANGED_FILES=$(git diff --cached --name-only)
    
    if [ -z "$CHANGED_FILES" ]; then
        echo -e "${YELLOW}No changes found to review.${NC}"
        exit 0
    fi
fi

# Count changed files
FILE_COUNT=$(echo "$CHANGED_FILES" | wc -l)
echo -e "Found ${CYAN}$FILE_COUNT${NC} changed file(s)"
echo ""

# ====================================================================
# Determine what to check based on changes
# ====================================================================

echo -e "${CYAN}Determining required checks...${NC}"

# Check each changed file
while IFS= read -r file; do
    # Skip deleted files
    if [ ! -f "$BASE_DIR/$file" ]; then
        continue
    fi
    
    # Repository Pattern
    if echo "$file" | grep -E "Repository\.java|repository/|/port/out/" > /dev/null; then
        if [ "$CHECK_REPOSITORY" = false ]; then
            echo "  • Repository changes detected → will check Repository Pattern compliance"
            CHECK_REPOSITORY=true
        fi
    fi
    
    # Event Sourcing Pattern
    if echo "$file" | grep -E "/entity/.*\.java" > /dev/null; then
        if grep -l "extends EsAggregateRoot" "$BASE_DIR/$file" > /dev/null 2>&1; then
            if [ "$CHECK_EVENT_SOURCING" = false ]; then
                echo "  • Event Sourcing Aggregate detected → will check ES patterns"
                CHECK_EVENT_SOURCING=true
            fi
        fi
    fi
    
    # Mapper Pattern
    if echo "$file" | grep -E "Mapper\.java|mapper/|/port/" > /dev/null; then
        if [ "$CHECK_MAPPER" = false ]; then
            echo "  • Mapper changes detected → will check Mapper compliance"
            CHECK_MAPPER=true
        fi
    fi
    
    # JPA Projection
    if echo "$file" | grep -E "Jpa.*Projection\.java|JpaConfiguration\.java" > /dev/null; then
        if [ "$CHECK_JPA_PROJECTION" = false ]; then
            echo "  • JPA Projection changes detected → will check JPA configuration"
            CHECK_JPA_PROJECTION=true
        fi
    fi
    
    # Aggregate
    if echo "$file" | grep -E "/entity/.*\.java|/aggregate/.*\.java" > /dev/null; then
        if [ "$CHECK_AGGREGATE" = false ]; then
            echo "  • Aggregate changes detected → will check DDD patterns"
            CHECK_AGGREGATE=true
        fi
    fi
    
    # Domain Events
    if echo "$file" | grep -E "Events\.java|Event\.java" > /dev/null; then
        if [ "$CHECK_DOMAIN_EVENTS" = false ]; then
            echo "  • Domain Event changes detected → will check Event compliance"
            CHECK_DOMAIN_EVENTS=true
        fi
    fi
    
    # Use Case
    if echo "$file" | grep -E "/usecase/.*UseCase\.java" > /dev/null; then
        if [ "$CHECK_USECASE" = false ]; then
            echo "  • Use Case changes detected → will check Use Case patterns"
            CHECK_USECASE=true
        fi
    fi
    
    # Controller
    if echo "$file" | grep -E "Controller\.java|/adapter/in/rest/" > /dev/null; then
        if [ "$CHECK_CONTROLLER" = false ]; then
            echo "  • Controller changes detected → will check REST API patterns"
            CHECK_CONTROLLER=true
        fi
    fi
    
    # Reactor
    if echo "$file" | grep -E "Reactor\.java|/reactor/" > /dev/null; then
        if [ "$CHECK_REACTOR" = false ]; then
            echo "  • Reactor changes detected → will check event handling patterns"
            CHECK_REACTOR=true
        fi
    fi
    
    # Test files
    if echo "$file" | grep -E "Test\.java|/test/" > /dev/null; then
        if [ "$CHECK_TEST_SPRING_DI" = false ]; then
            echo "  • Test changes detected → will check Spring DI compliance"
            CHECK_TEST_SPRING_DI=true
        fi
    fi
    
    # Coding Standards
    if echo "$file" | grep -E "\.ai/.*coding-standards|\.ai/prompts/.*\.md" > /dev/null; then
        if [ "$CHECK_CODING_STANDARDS" = false ]; then
            echo "  • Coding standards changes detected → will check documentation consistency"
            CHECK_CODING_STANDARDS=true
        fi
    fi
    
done <<< "$CHANGED_FILES"

# If nothing specific detected, run basic checks
if [ "$CHECK_REPOSITORY" = false ] && \
   [ "$CHECK_MAPPER" = false ] && \
   [ "$CHECK_JPA_PROJECTION" = false ] && \
   [ "$CHECK_AGGREGATE" = false ] && \
   [ "$CHECK_USECASE" = false ] && \
   [ "$CHECK_CONTROLLER" = false ] && \
   [ "$CHECK_REACTOR" = false ] && \
   [ "$CHECK_CODING_STANDARDS" = false ]; then
    echo "  • No specific patterns detected → will run core compliance checks"
    CHECK_REPOSITORY=true
    CHECK_MAPPER=true
fi

echo ""

# ====================================================================
# Run relevant checks
# ====================================================================

echo -e "${MAGENTA}════ Running Code Review Checks ════${NC}"

# Repository Pattern Check
if [ "$CHECK_REPOSITORY" = true ]; then
    run_check "check-repository-compliance.sh" "Repository Pattern Compliance"
fi

# Mapper Check
if [ "$CHECK_MAPPER" = true ]; then
    run_check "check-mapper-compliance.sh" "Mapper Design Compliance"
fi

# JPA Projection Check
if [ "$CHECK_JPA_PROJECTION" = true ]; then
    run_check "check-jpa-projection-config.sh" "JPA Projection Configuration"
fi

# Coding Standards Check
if [ "$CHECK_CODING_STANDARDS" = true ]; then
    run_check "check-coding-standards.sh" "Coding Standards Consistency"
fi

# Test Spring DI Check
if [ "$CHECK_TEST_SPRING_DI" = true ]; then
    run_check "check-test-spring-di.sh" "Test Spring DI Compliance"
fi

# Domain Events Check
if [ "$CHECK_DOMAIN_EVENTS" = true ]; then
    run_check "check-domain-events-compliance.sh" "Domain Events Compliance"
fi

# Event Sourcing Pattern Check
if [ "$CHECK_EVENT_SOURCING" = true ]; then
    run_check "check-event-sourcing-patterns.sh" "Event Sourcing Pattern Compliance"
fi

# ====================================================================
# Additional Pattern-Specific Checks (Display Recommendations)
# ====================================================================

if [ "$CHECK_AGGREGATE" = true ] || [ "$CHECK_USECASE" = true ] || \
   [ "$CHECK_CONTROLLER" = true ] || [ "$CHECK_REACTOR" = true ]; then
    
    echo ""
    echo -e "${MAGENTA}════ Pattern-Specific Recommendations ════${NC}"
    echo ""
    
    if [ "$CHECK_AGGREGATE" = true ]; then
        echo -e "${YELLOW}📋 Aggregate Review Checklist:${NC}"
        echo "  □ All ensure() checks have corresponding tests"
        echo "  □ Domain events include proper metadata"
        echo "  □ Constructor validates all required fields"
        echo "  □ State transitions are properly guarded"
        echo ""
    fi
    
    if [ "$CHECK_USECASE" = true ]; then
        echo -e "${YELLOW}📋 Use Case Review Checklist:${NC}"
        echo "  □ Input/Output are inner classes"
        echo "  □ Constructor injection used for dependencies"
        echo "  □ Proper transaction boundaries"
        echo "  □ Error handling with DomainException"
        echo ""
    fi
    
    if [ "$CHECK_CONTROLLER" = true ]; then
        echo -e "${YELLOW}📋 Controller Review Checklist:${NC}"
        echo "  □ Proper HTTP status codes"
        echo "  □ Request validation with @Valid"
        echo "  □ Error responses follow standard format"
        echo "  □ API documentation annotations present"
        echo ""
    fi
    
    if [ "$CHECK_REACTOR" = true ]; then
        echo -e "${YELLOW}📋 Reactor Review Checklist:${NC}"
        echo "  □ Extends Reactor<DomainEventData>"
        echo "  □ Uses Inquiry for cross-aggregate queries"
        echo "  □ Proper event filtering in canHandle()"
        echo "  □ Idempotent event processing"
        echo ""
    fi
    
    if [ "$CHECK_TEST_SPRING_DI" = true ]; then
        echo -e "${YELLOW}📋 Test Spring DI Checklist:${NC}"
        echo "  □ Uses @SpringBootTest annotation"
        echo "  □ Dependencies injected via @Autowired"
        echo "  □ Works with both test-inmemory and test-outbox profiles"
        echo "  □ No hardcoded repository instantiation"
        echo ""
    fi
fi

# ====================================================================
# Show files that were reviewed
# ====================================================================

echo -e "${MAGENTA}════ Files Reviewed ════${NC}"
echo ""
echo -e "${CYAN}Changed files in this review:${NC}"

# Group files by type
JAVA_FILES=$(echo "$CHANGED_FILES" | grep "\.java$" || true)
TEST_FILES=$(echo "$CHANGED_FILES" | grep "Test\.java$" || true)
CONFIG_FILES=$(echo "$CHANGED_FILES" | grep -E "\.yml$|\.yaml$|\.properties$|\.xml$" || true)
DOC_FILES=$(echo "$CHANGED_FILES" | grep -E "\.md$" || true)

if [ ! -z "$JAVA_FILES" ]; then
    JAVA_COUNT=$(echo "$JAVA_FILES" | wc -l)
    echo -e "  ${BLUE}Java files:${NC} $JAVA_COUNT"
fi

if [ ! -z "$TEST_FILES" ]; then
    TEST_COUNT=$(echo "$TEST_FILES" | wc -l)
    echo -e "  ${GREEN}Test files:${NC} $TEST_COUNT"
fi

if [ ! -z "$CONFIG_FILES" ]; then
    CONFIG_COUNT=$(echo "$CONFIG_FILES" | wc -l)
    echo -e "  ${YELLOW}Config files:${NC} $CONFIG_COUNT"
fi

if [ ! -z "$DOC_FILES" ]; then
    DOC_COUNT=$(echo "$DOC_FILES" | wc -l)
    echo -e "  ${CYAN}Documentation:${NC} $DOC_COUNT"
fi

# ====================================================================
# Results Summary
# ====================================================================

echo ""
echo -e "${MAGENTA}╔════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║        Code Review Summary             ║${NC}"
echo -e "${MAGENTA}╚════════════════════════════════════════╝${NC}"
echo ""

# Display results
echo -e "Files Reviewed: ${CYAN}$FILE_COUNT${NC}"
echo -e "Checks Performed: ${CYAN}$TOTAL_CHECKS${NC}"
echo -e "Passed: ${GREEN}$PASSED_CHECKS${NC}"
echo -e "Failed: ${RED}$FAILED_CHECKS${NC}"
echo -e "Warnings: ${YELLOW}$WARNINGS${NC}"

echo ""

# Overall status
if [ $FAILED_CHECKS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║    ✓ Code Review Passed!               ║${NC}"
    echo -e "${GREEN}║    Ready for merge                     ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
    exit 0
elif [ $FAILED_CHECKS -eq 0 ]; then
    echo -e "${YELLOW}╔════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║  ⚠ Review Passed with Warnings         ║${NC}"
    echo -e "${YELLOW}║  Consider addressing warnings          ║${NC}"
    echo -e "${YELLOW}╚════════════════════════════════════════╝${NC}"
    exit 0
else
    echo -e "${RED}╔════════════════════════════════════════╗${NC}"
    echo -e "${RED}║    ✗ Code Review Failed!               ║${NC}"
    echo -e "${RED}║    Fix issues before merging           ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════╝${NC}"
    
    echo ""
    echo -e "${YELLOW}Next Steps:${NC}"
    echo "1. Review the failed checks above"
    echo "2. Fix the compliance issues"
    echo "3. Run './code-review.sh' again"
    echo ""
    
    exit 1
fi