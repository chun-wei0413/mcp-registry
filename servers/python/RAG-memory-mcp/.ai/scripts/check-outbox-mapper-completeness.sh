#!/bin/bash

# ====================================================================
# Outbox Mapper Completeness Checker
# 
# Purpose: Verify that all Outbox pattern Mappers correctly handle
#          entity collections within aggregates
# 
# Related ADR: ADR-023-outbox-mapper-complete-entity-mapping-requirement
# ====================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SRC_DIR="$PROJECT_ROOT/src/main/java"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "======================================================================"
echo "🔍 Outbox Mapper Completeness Check"
echo "======================================================================"
echo ""

# Find all Mapper classes
echo "📋 Finding all Mapper classes..."
MAPPERS=$(find "$SRC_DIR" -name "*Mapper.java" -type f | grep -E "usecase/port/.*Mapper\.java")

if [ -z "$MAPPERS" ]; then
    echo -e "${YELLOW}⚠️  No Mapper classes found${NC}"
    exit 0
fi

TOTAL_MAPPERS=$(echo "$MAPPERS" | wc -l | xargs)
echo "Found $TOTAL_MAPPERS mapper(s)"
echo ""

# Track issues
ISSUES_FOUND=0
WARNINGS_FOUND=0

# Function to check if a mapper handles collections
check_mapper() {
    local mapper_file=$1
    local mapper_name=$(basename "$mapper_file" .java)
    
    echo "Checking $mapper_name..."
    
    # Check if this is likely an aggregate mapper (has toData and toDomain methods)
    if ! grep -q "static.*toData\|toData.*static" "$mapper_file" 2>/dev/null; then
        echo -e "  ${YELLOW}⚠️  No static toData method found${NC}"
        return
    fi
    
    if ! grep -q "static.*toDomain\|toDomain.*static" "$mapper_file" 2>/dev/null; then
        echo -e "  ${YELLOW}⚠️  No static toDomain method found${NC}"
        return
    fi
    
    # Check for potential entity collections (Set, List, Collection of entities)
    local has_collections=false
    if grep -E "(Set<|List<|Collection<).*[A-Z][a-z]*>" "$mapper_file" | grep -v "String\|Integer\|Long\|BigDecimal" > /dev/null 2>&1; then
        has_collections=true
    fi
    
    if [ "$has_collections" = true ]; then
        # This mapper potentially handles collections, let's check more carefully
        
        # Check toData method
        echo "  Checking toData() method..."
        
        # Extract toData method (simplified - may need adjustment for complex cases)
        local todata_start=$(grep -n "static.*toData" "$mapper_file" | head -1 | cut -d: -f1)
        if [ ! -z "$todata_start" ]; then
            # Look for collection handling patterns in toData
            local todata_content=$(sed -n "${todata_start},/^    \}$/p" "$mapper_file" 2>/dev/null)
            
            # Check for patterns that indicate collection mapping
            if echo "$todata_content" | grep -E "(getTasks|getItems|getMembers|getCriteria)" > /dev/null 2>&1; then
                if echo "$todata_content" | grep -E "(setTaskDatas|setItems|setMembers|setCriteria|add.*Data)" > /dev/null 2>&1; then
                    echo -e "    ${GREEN}✓ toData() appears to handle collections${NC}"
                else
                    echo -e "    ${RED}✗ toData() may not be mapping collections to Data objects${NC}"
                    ISSUES_FOUND=$((ISSUES_FOUND + 1))
                fi
            fi
        fi
        
        # Check toDomain method
        echo "  Checking toDomain() method..."
        
        local todomain_start=$(grep -n "static.*toDomain" "$mapper_file" | head -1 | cut -d: -f1)
        if [ ! -z "$todomain_start" ]; then
            # Look for collection reconstruction patterns in toDomain
            local todomain_content=$(sed -n "${todomain_start},/^    \}$/p" "$mapper_file" 2>/dev/null)
            
            # Check for patterns that indicate collection reconstruction
            if echo "$todomain_content" | grep -E "(getTaskDatas|getItemDatas|getMemberDatas)" > /dev/null 2>&1; then
                if echo "$todomain_content" | grep -E "(new.*List|new.*Set|tasks\.add|items\.add)" > /dev/null 2>&1; then
                    echo -e "    ${GREEN}✓ toDomain() appears to reconstruct collections${NC}"
                else
                    echo -e "    ${YELLOW}⚠️  toDomain() may not be reconstructing collections properly${NC}"
                    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
                fi
            fi
        fi
    else
        echo -e "  ${BLUE}ℹ️  No entity collections detected (may be a simple mapper)${NC}"
    fi
    
    echo ""
}

# Special check for ProductBacklogItemMapper (known to have Tasks)
check_pbi_mapper() {
    local pbi_mapper=$(find "$SRC_DIR" -name "ProductBacklogItemMapper.java" -type f | head -1)
    
    if [ ! -z "$pbi_mapper" ]; then
        echo "🔎 Special check for ProductBacklogItemMapper (must handle Tasks)..."
        
        # Check if Tasks are mapped in toData
        if grep -A 100 "static.*toData.*ProductBacklogItem" "$pbi_mapper" | grep -E "getTasks\(\)" > /dev/null 2>&1; then
            if grep -A 100 "static.*toData.*ProductBacklogItem" "$pbi_mapper" | grep -E "setTaskDatas|taskDatas\.add" > /dev/null 2>&1; then
                echo -e "  ${GREEN}✓ ProductBacklogItemMapper.toData() handles Tasks${NC}"
            else
                echo -e "  ${RED}✗ ProductBacklogItemMapper.toData() does NOT map Tasks!${NC}"
                ISSUES_FOUND=$((ISSUES_FOUND + 1))
            fi
        else
            echo -e "  ${RED}✗ ProductBacklogItemMapper.toData() does NOT access getTasks()!${NC}"
            ISSUES_FOUND=$((ISSUES_FOUND + 1))
        fi
        
        # Check if Tasks are reconstructed in toDomain
        if grep -A 100 "toDomain.*ProductBacklogItemData" "$pbi_mapper" | grep -E "getTaskDatas\(\)" > /dev/null 2>&1; then
            echo -e "  ${GREEN}✓ ProductBacklogItemMapper.toDomain() reconstructs Tasks${NC}"
        else
            echo -e "  ${RED}✗ ProductBacklogItemMapper.toDomain() does NOT reconstruct Tasks!${NC}"
            ISSUES_FOUND=$((ISSUES_FOUND + 1))
        fi
        
        echo ""
    fi
}

# Check for test coverage
check_mapper_tests() {
    echo "📊 Checking for Mapper test coverage..."
    
    local test_dir="$PROJECT_ROOT/src/test/java"
    local mapper_tests=$(find "$test_dir" -name "*MapperTest.java" -type f 2>/dev/null | wc -l | xargs)
    
    if [ "$mapper_tests" -gt 0 ]; then
        echo -e "  ${GREEN}✓ Found $mapper_tests mapper test file(s)${NC}"
        
        # Check for round-trip tests
        local roundtrip_tests=$(grep -r "toData.*toDomain\|toDomain.*toData" "$test_dir" 2>/dev/null | wc -l | xargs)
        if [ "$roundtrip_tests" -gt 0 ]; then
            echo -e "  ${GREEN}✓ Found round-trip conversion tests${NC}"
        else
            echo -e "  ${YELLOW}⚠️  No round-trip conversion tests found${NC}"
            WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
        fi
    else
        echo -e "  ${YELLOW}⚠️  No mapper test files found${NC}"
        WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
    fi
    
    echo ""
}

# Main execution
echo "======================================================================"
echo "Checking individual mappers..."
echo "======================================================================"
echo ""

# Check each mapper
for mapper in $MAPPERS; do
    check_mapper "$mapper"
done

# Special checks
check_pbi_mapper
check_mapper_tests

# Summary
echo "======================================================================"
echo "📈 Summary"
echo "======================================================================"

if [ $ISSUES_FOUND -eq 0 ] && [ $WARNINGS_FOUND -eq 0 ]; then
    echo -e "${GREEN}✅ All mappers appear to be correctly implemented!${NC}"
    echo ""
    echo "No issues found. All entity collections seem to be properly mapped."
else
    if [ $ISSUES_FOUND -gt 0 ]; then
        echo -e "${RED}❌ Found $ISSUES_FOUND critical issue(s) that must be fixed${NC}"
    fi
    if [ $WARNINGS_FOUND -gt 0 ]; then
        echo -e "${YELLOW}⚠️  Found $WARNINGS_FOUND warning(s) that should be reviewed${NC}"
    fi
    echo ""
    echo "Recommendations:"
    echo "1. Ensure all toData() methods map entity collections to Data objects"
    echo "2. Ensure all toDomain() methods reconstruct entities from Data"
    echo "3. Add round-trip tests to verify mapper completeness"
    echo "4. Review ADR-023 for implementation guidelines"
fi

echo ""
echo "For more details, see:"
echo "  • ADR-023: .dev/adr/ADR-023-outbox-mapper-complete-entity-mapping-requirement.md"
echo "  • Coding Standards: .ai/tech-stacks/java-ca-ezddd-spring/coding-standards.md"
echo ""

# Exit with error if critical issues found
if [ $ISSUES_FOUND -gt 0 ]; then
    exit 1
fi

exit 0