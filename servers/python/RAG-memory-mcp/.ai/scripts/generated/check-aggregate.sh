#!/bin/bash

# Aggregate Compliance Check Script
# Ensures all aggregates follow EZDDD framework conventions

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

echo -e "${CYAN}╔════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     Aggregate Compliance Check         ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════╝${NC}"
echo ""

ERROR_COUNT=0
WARNING_COUNT=0

# Function to check aggregate structure
check_aggregate_structure() {
    local file=$1
    local aggregate_name=$(basename "$file" .java)
    local errors=()
    local warnings=()
    
    # Check if extends EsAggregateRoot
    if ! grep -q "extends EsAggregateRoot<" "$file" 2>/dev/null; then
        errors+=("❌ Must extend EsAggregateRoot<ID, Events>")
        ((ERROR_COUNT++))
    fi
    
    # Check for required methods
    if ! grep -q "protected void when(" "$file" 2>/dev/null; then
        errors+=("❌ Missing required method: protected void when(Events event)")
        ((ERROR_COUNT++))
    fi
    
    if ! grep -q "public void ensureInvariant()" "$file" 2>/dev/null; then
        warnings+=("⚠️  Missing method: public void ensureInvariant()")
        ((WARNING_COUNT++))
    fi
    
    if ! grep -q "public.*getId()" "$file" 2>/dev/null; then
        errors+=("❌ Missing required method: public ID getId()")
        ((ERROR_COUNT++))
    fi
    
    if ! grep -q "public String getCategory()" "$file" 2>/dev/null; then
        warnings+=("⚠️  Missing method: public String getCategory()")
        ((WARNING_COUNT++))
    fi
    
    # Check for isDeleted support
    if ! grep -q "private boolean deleted" "$file" 2>/dev/null && ! grep -q "private boolean isDeleted" "$file" 2>/dev/null; then
        errors+=("❌ Missing soft delete support: private boolean deleted field")
        ((ERROR_COUNT++))
    fi
    
    if ! grep -q "public boolean isDeleted()" "$file" 2>/dev/null; then
        errors+=("❌ Missing soft delete support: public boolean isDeleted() method")
        ((ERROR_COUNT++))
    fi
    
    # Check for wrong imports
    if grep -q "tw.teddysoft.ezddd.domain.model" "$file" 2>/dev/null; then
        errors+=("❌ Wrong import path: should be tw.teddysoft.ezddd.entity")
        ((ERROR_COUNT++))
    fi
    
    # Check for ensure/require usage
    if ! grep -q "Contract.ensure\|ensure(" "$file" 2>/dev/null; then
        warnings+=("⚠️  No postcondition checks found (should use ensure())")
        ((WARNING_COUNT++))
    fi
    
    if ! grep -q "Contract.requireNotNull\|requireNotNull(" "$file" 2>/dev/null; then
        warnings+=("⚠️  No precondition checks found (should use requireNotNull())")
        ((WARNING_COUNT++))
    fi
    
    # Output results
    if [ ${#errors[@]} -gt 0 ] || [ ${#warnings[@]} -gt 0 ]; then
        echo -e "\n${BOLD}Checking: $aggregate_name${NC}"
        
        if [ ${#errors[@]} -gt 0 ]; then
            echo -e "${RED}Errors:${NC}"
            for error in "${errors[@]}"; do
                echo -e "  $error"
            done
        fi
        
        if [ ${#warnings[@]} -gt 0 ]; then
            echo -e "${YELLOW}Warnings:${NC}"
            for warning in "${warnings[@]}"; do
                echo -e "  $warning"
            done
        fi
        
        return 1
    else
        echo -e "${GREEN}✓ $aggregate_name is compliant${NC}"
        return 0
    fi
}

# Function to check if aggregate has corresponding events
check_aggregate_events() {
    local aggregate_file=$1
    local aggregate_name=$(basename "$aggregate_file" .java)
    local aggregate_dir=$(dirname "$aggregate_file")
    
    # Look for corresponding Events file
    local events_file=""
    local possible_locations=(
        "$aggregate_dir/${aggregate_name}Events.java"
        "${aggregate_dir/entity/entity}/${aggregate_name}Events.java"
        "$(dirname "$aggregate_dir")/entity/${aggregate_name}Events.java"
    )
    
    for location in "${possible_locations[@]}"; do
        if [ -f "$location" ]; then
            events_file="$location"
            break
        fi
    done
    
    if [ -z "$events_file" ]; then
        echo -e "  ${YELLOW}⚠️  No corresponding ${aggregate_name}Events.java found${NC}"
        ((WARNING_COUNT++))
        return 1
    else
        echo -e "  ${CYAN}Found events: $(basename "$events_file")${NC}"
        
        # Check events compliance
        if "$SCRIPT_DIR/check-domain-events-compliance.sh" 2>&1 | grep -q "$(basename "$events_file")" | grep -q "✓ All checks passed"; then
            echo -e "  ${GREEN}✓ Events are compliant${NC}"
        else
            echo -e "  ${RED}❌ Events are NOT compliant - must regenerate with Aggregate Sub-agent${NC}"
            ((ERROR_COUNT++))
            return 1
        fi
    fi
    
    return 0
}

# Find all aggregate files
echo -e "${BLUE}Scanning for aggregate files...${NC}"
AGGREGATE_FILES=$(find "$PROJECT_ROOT/src/main/java" -name "*.java" -type f | xargs grep -l "extends.*AggregateRoot\|extends.*EsAggregateRoot" 2>/dev/null || true)

if [ -z "$AGGREGATE_FILES" ]; then
    echo -e "${YELLOW}No aggregate files found${NC}"
    exit 0
fi

TOTAL_FILES=0
PASSED_FILES=0

# Check each aggregate file
while IFS= read -r file; do
    if [ -f "$file" ]; then
        ((TOTAL_FILES++))
        
        all_passed=true
        
        # Check aggregate structure
        if ! check_aggregate_structure "$file"; then
            all_passed=false
        fi
        
        # Check corresponding events
        if ! check_aggregate_events "$file"; then
            all_passed=false
        fi
        
        if [ "$all_passed" = true ]; then
            ((PASSED_FILES++))
        fi
    fi
done <<< "$AGGREGATE_FILES"

# Summary
echo -e "\n${CYAN}═══════════════════════════════════════${NC}"
echo -e "${BOLD}Summary:${NC}"
echo -e "  Total aggregates checked: $TOTAL_FILES"
echo -e "  Aggregates passed: ${GREEN}$PASSED_FILES${NC}"
echo -e "  Errors found: ${RED}$ERROR_COUNT${NC}"
echo -e "  Warnings found: ${YELLOW}$WARNING_COUNT${NC}"

# Provide guidance
if [ $ERROR_COUNT -gt 0 ]; then
    echo -e "\n${RED}${BOLD}❌ Aggregate Compliance Check Failed${NC}"
    echo -e "\n${YELLOW}Action Required:${NC}"
    echo -e "1. ${BOLD}DO NOT use these non-compliant aggregates${NC}"
    echo -e "2. ${BOLD}MUST delegate to Aggregate Sub-agent to regenerate${NC}"
    echo -e "3. Delete existing non-compliant files before regenerating"
    echo -e "\n${CYAN}Command to regenerate:${NC}"
    echo -e "  請使用 aggregate-sub-agent workflow 重新產生 [Aggregate]"
    exit 1
else
    if [ $WARNING_COUNT -gt 0 ]; then
        echo -e "\n${YELLOW}⚠️  Aggregates have warnings but are usable${NC}"
    else
        echo -e "\n${GREEN}${BOLD}✅ All Aggregates are Fully Compliant${NC}"
    fi
fi

exit 0