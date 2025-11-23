#!/bin/bash

# Domain Events Compliance Check Script
# Ensures all domain events follow EZDDD framework conventions

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
echo -e "${CYAN}║    Domain Events Compliance Check      ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════╝${NC}"
echo ""

ERROR_COUNT=0
WARNING_COUNT=0

# Function to check if file contains custom ConstructionEvent/DestructionEvent interfaces
check_custom_interfaces() {
    local file=$1
    local errors=()
    
    # Check for custom ConstructionEvent interface
    if grep -q "^interface ConstructionEvent" "$file" 2>/dev/null; then
        errors+=("❌ Custom ConstructionEvent interface found (should use InternalDomainEvent.ConstructionEvent)")
        ((ERROR_COUNT++))
    fi
    
    # Check for custom DestructionEvent interface
    if grep -q "^interface DestructionEvent" "$file" 2>/dev/null; then
        errors+=("❌ Custom DestructionEvent interface found (should use InternalDomainEvent.DestructionEvent)")
        ((ERROR_COUNT++))
    fi
    
    # Check for wrong implementation
    if grep -q "implements.*ConstructionEvent[^.]" "$file" 2>/dev/null; then
        if ! grep -q "implements.*InternalDomainEvent\.ConstructionEvent" "$file" 2>/dev/null; then
            errors+=("❌ Using custom ConstructionEvent instead of InternalDomainEvent.ConstructionEvent")
            ((ERROR_COUNT++))
        fi
    fi
    
    if grep -q "implements.*DestructionEvent[^.]" "$file" 2>/dev/null; then
        if ! grep -q "implements.*InternalDomainEvent\.DestructionEvent" "$file" 2>/dev/null; then
            errors+=("❌ Using custom DestructionEvent instead of InternalDomainEvent.DestructionEvent")
            ((ERROR_COUNT++))
        fi
    fi
    
    if [ ${#errors[@]} -gt 0 ]; then
        echo -e "\n${RED}Issues found in: ${BOLD}$(basename "$file")${NC}"
        for error in "${errors[@]}"; do
            echo -e "  $error"
        done
        return 1
    fi
    return 0
}

# Function to check domain event structure
check_domain_event_structure() {
    local file=$1
    local warnings=()
    
    # Check if events implement InternalDomainEvent
    if ! grep -q "extends InternalDomainEvent" "$file" 2>/dev/null; then
        warnings+=("⚠️  Should extend InternalDomainEvent")
        ((WARNING_COUNT++))
    fi
    
    # Check for metadata() method
    if ! grep -q "public Map<String, String> metadata()" "$file" 2>/dev/null; then
        warnings+=("⚠️  Missing metadata() method implementation")
        ((WARNING_COUNT++))
    fi
    
    # Check for source() method
    if ! grep -q "public String source()" "$file" 2>/dev/null; then
        warnings+=("⚠️  Missing source() method implementation")
        ((WARNING_COUNT++))
    fi
    
    # Check for TypeMapper
    if ! grep -q "class TypeMapper" "$file" 2>/dev/null; then
        warnings+=("⚠️  Missing TypeMapper class for event serialization")
        ((WARNING_COUNT++))
    fi
    
    if [ ${#warnings[@]} -gt 0 ]; then
        echo -e "\n${YELLOW}Warnings for: ${BOLD}$(basename "$file")${NC}"
        for warning in "${warnings[@]}"; do
            echo -e "  $warning"
        done
    fi
}

# Find all domain event files
echo -e "${BLUE}Scanning for domain event files...${NC}"
DOMAIN_EVENT_FILES=$(find "$PROJECT_ROOT/src/main/java" -name "*Events.java" -o -name "*Event.java" 2>/dev/null | grep -E "entity|domain" || true)

if [ -z "$DOMAIN_EVENT_FILES" ]; then
    echo -e "${YELLOW}No domain event files found${NC}"
    exit 0
fi

TOTAL_FILES=0
PASSED_FILES=0

# Check each domain event file
while IFS= read -r file; do
    if [ -f "$file" ]; then
        ((TOTAL_FILES++))
        echo -e "\n${CYAN}Checking: $(basename "$file")${NC}"
        
        all_passed=true
        
        # Check for custom interfaces
        if ! check_custom_interfaces "$file"; then
            all_passed=false
        fi
        
        # Check domain event structure
        check_domain_event_structure "$file"
        
        if [ "$all_passed" = true ] && [ $WARNING_COUNT -eq 0 ]; then
            echo -e "  ${GREEN}✓ All checks passed${NC}"
            ((PASSED_FILES++))
        fi
    fi
done <<< "$DOMAIN_EVENT_FILES"

# Summary
echo -e "\n${CYAN}═══════════════════════════════════════${NC}"
echo -e "${BOLD}Summary:${NC}"
echo -e "  Total files checked: $TOTAL_FILES"
echo -e "  Files passed: ${GREEN}$PASSED_FILES${NC}"
echo -e "  Errors found: ${RED}$ERROR_COUNT${NC}"
echo -e "  Warnings found: ${YELLOW}$WARNING_COUNT${NC}"

# Provide guidance for fixes
if [ $ERROR_COUNT -gt 0 ]; then
    echo -e "\n${RED}${BOLD}❌ Domain Events Compliance Check Failed${NC}"
    echo -e "\n${YELLOW}How to fix:${NC}"
    echo -e "1. Remove custom ${BOLD}interface ConstructionEvent {}${NC} and ${BOLD}interface DestructionEvent {}${NC}"
    echo -e "2. Change ${BOLD}implements ProductBacklogItemEvents, ConstructionEvent${NC}"
    echo -e "   to ${GREEN}${BOLD}implements ProductBacklogItemEvents, InternalDomainEvent.ConstructionEvent${NC}"
    echo -e "3. Change ${BOLD}implements ProductBacklogItemEvents, DestructionEvent${NC}"
    echo -e "   to ${GREEN}${BOLD}implements ProductBacklogItemEvents, InternalDomainEvent.DestructionEvent${NC}"
    exit 1
else
    echo -e "\n${GREEN}${BOLD}✅ All Domain Events Compliance Checks Passed${NC}"
fi

if [ $WARNING_COUNT -gt 0 ]; then
    echo -e "\n${YELLOW}Note: Some warnings were found. Consider addressing them for better compliance.${NC}"
fi

exit 0