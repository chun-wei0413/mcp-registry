#!/bin/bash

# Script to check if dual-profile test suites are generated for use cases
# This enforces the mandatory requirement in sub-agent prompts

set -e

echo "================================================"
echo "Checking Dual-Profile Test Generation"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counter for missing test suites
MISSING_COUNT=0
TOTAL_USECASES=0

# Function to check if test suites exist for a use case
check_test_suites() {
    local test_file="$1"
    local test_name=$(basename "$test_file" .java)
    local use_case_name=${test_name%ServiceTest}
    local test_dir=$(dirname "$test_file")
    
    echo -n "Checking $use_case_name: "
    
    # Check for InMemory test suite
    local inmemory_suite="${test_dir}/InMemory${use_case_name}TestSuite.java"
    local outbox_suite="${test_dir}/Outbox${use_case_name}TestSuite.java"
    
    local has_inmemory=false
    local has_outbox=false
    
    if [ -f "$inmemory_suite" ]; then
        has_inmemory=true
    fi
    
    if [ -f "$outbox_suite" ]; then
        has_outbox=true
    fi
    
    if $has_inmemory && $has_outbox; then
        echo -e "${GREEN}✓ Both test suites found${NC}"
    elif $has_inmemory && ! $has_outbox; then
        echo -e "${YELLOW}⚠ Missing Outbox test suite${NC}"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    elif ! $has_inmemory && $has_outbox; then
        echo -e "${YELLOW}⚠ Missing InMemory test suite${NC}"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    else
        echo -e "${RED}✗ Both test suites missing!${NC}"
        MISSING_COUNT=$((MISSING_COUNT + 2))
    fi
    
    TOTAL_USECASES=$((TOTAL_USECASES + 1))
}

# Check project-config.json for dualProfileSupport setting
CONFIG_FILE=".dev/project-config.json"
if [ -f "$CONFIG_FILE" ]; then
    DUAL_SUPPORT=$(grep -o '"dualProfileSupport"[[:space:]]*:[[:space:]]*[^,}]*' "$CONFIG_FILE" | grep -o 'true\|false' || echo "false")
    echo "Dual Profile Support: $DUAL_SUPPORT"
    echo ""
    
    if [ "$DUAL_SUPPORT" != "true" ]; then
        echo -e "${YELLOW}Note: dualProfileSupport is not enabled in project-config.json${NC}"
        echo "Test suites are not mandatory when dualProfileSupport is false."
        exit 0
    fi
else
    echo -e "${YELLOW}Warning: project-config.json not found${NC}"
    echo ""
fi

# Find all *ServiceTest.java files (excluding test suites themselves)
echo "Scanning for use case tests..."
echo "--------------------------------"

while IFS= read -r test_file; do
    # Skip test suite files
    if [[ ! "$test_file" =~ TestSuite\.java$ ]]; then
        check_test_suites "$test_file"
    fi
done < <(find src/test -name "*ServiceTest.java" 2>/dev/null)

# Also check for Reactor tests
while IFS= read -r test_file; do
    # Skip test suite files
    if [[ ! "$test_file" =~ TestSuite\.java$ ]]; then
        check_test_suites "$test_file"
    fi
done < <(find src/test -name "*ReactorTest.java" 2>/dev/null)

echo ""
echo "================================================"
echo "Summary"
echo "================================================"

if [ $MISSING_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ All use cases have dual-profile test suites!${NC}"
    echo "Total use cases checked: $TOTAL_USECASES"
    exit 0
else
    echo -e "${RED}✗ Missing test suites detected!${NC}"
    echo "Total use cases checked: $TOTAL_USECASES"
    echo "Missing test suites: $MISSING_COUNT"
    echo ""
    echo -e "${YELLOW}Action Required:${NC}"
    echo "Generate missing test suites using the ProfileSetter pattern."
    echo "Refer to .ai/prompts/command-sub-agent-prompt.md for examples."
    exit 1
fi