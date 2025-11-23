#!/bin/bash

# ======================================================
# ADR Cross-Reference Integrity Check Script
# ======================================================
# Purpose: Validate ADR numbering consistency and cross-references
# Author: AI-SCRUM Team
# Date: 2025-08-30
# ======================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Directories
ADR_DIR=".dev/adr"
INDEX_FILE=".ai/ADR-INDEX.md"
CLAUDE_FILE="CLAUDE.md"

# Counters
TOTAL_ISSUES=0
TOTAL_CHECKS=0

# Function: Print colored message
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function: Check for duplicate ADR numbers
check_duplicate_numbers() {
    print_message "$CYAN" "\n=== Checking for Duplicate ADR Numbers ==="
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    local duplicates=$(ls $ADR_DIR/ADR-*.md 2>/dev/null | sed 's/.*ADR-//' | sed 's/-.*//' | sort | uniq -d)
    
    if [ -z "$duplicates" ]; then
        print_message "$GREEN" "✅ No duplicate ADR numbers found"
    else
        print_message "$RED" "❌ Found duplicate ADR numbers:"
        for num in $duplicates; do
            print_message "$YELLOW" "  ADR-$num:"
            ls $ADR_DIR/ADR-${num}-*.md | while read file; do
                echo "    - $(basename $file)"
            done
            TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
        done
    fi
}

# Function: Check ADR file naming convention
check_naming_convention() {
    print_message "$CYAN" "\n=== Checking ADR Naming Convention ==="
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    local invalid_files=0
    for file in $ADR_DIR/ADR-*.md; do
        if [ -f "$file" ]; then
            basename=$(basename "$file")
            if ! [[ "$basename" =~ ^ADR-[0-9]{3}-[a-z\-]+\.md$ ]]; then
                if [ $invalid_files -eq 0 ]; then
                    print_message "$RED" "❌ Files not following naming convention (ADR-XXX-description.md):"
                fi
                echo "    - $basename"
                invalid_files=$((invalid_files + 1))
                TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
            fi
        fi
    done
    
    if [ $invalid_files -eq 0 ]; then
        print_message "$GREEN" "✅ All ADR files follow naming convention"
    fi
}

# Function: Check if all ADRs are referenced in index
check_index_references() {
    print_message "$CYAN" "\n=== Checking ADR References in Index ==="
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    local missing_refs=0
    for file in $ADR_DIR/ADR-*.md; do
        if [ -f "$file" ]; then
            basename=$(basename "$file" .md)
            adr_num=$(echo $basename | sed 's/ADR-//' | sed 's/-.*//')
            
            if ! grep -q "ADR-$adr_num" "$INDEX_FILE" 2>/dev/null; then
                if [ $missing_refs -eq 0 ]; then
                    print_message "$RED" "❌ ADRs not referenced in $INDEX_FILE:"
                fi
                echo "    - $basename"
                missing_refs=$((missing_refs + 1))
                TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
            fi
        fi
    done
    
    if [ $missing_refs -eq 0 ]; then
        print_message "$GREEN" "✅ All ADRs are referenced in index"
    fi
}

# Function: Check for broken cross-references
check_broken_references() {
    print_message "$CYAN" "\n=== Checking for Broken Cross-References ==="
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    local broken_refs=0
    
    # Check references in all markdown files
    for file in $(find .ai -name "*.md" -type f) $CLAUDE_FILE; do
        if [ -f "$file" ]; then
            # Extract ADR references (ADR-XXX)
            refs=$(grep -oE "ADR-[0-9]{3}" "$file" 2>/dev/null | sort -u)
            
            for ref in $refs; do
                # Check if corresponding ADR file exists
                if ! ls $ADR_DIR/${ref}-*.md >/dev/null 2>&1; then
                    if [ $broken_refs -eq 0 ]; then
                        print_message "$RED" "❌ Broken ADR references found:"
                    fi
                    echo "    - $ref referenced in $file but file doesn't exist"
                    broken_refs=$((broken_refs + 1))
                    TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
                fi
            done
        fi
    done
    
    if [ $broken_refs -eq 0 ]; then
        print_message "$GREEN" "✅ No broken ADR references found"
    fi
}

# Function: Check ADR numbering sequence
check_numbering_sequence() {
    print_message "$CYAN" "\n=== Checking ADR Numbering Sequence ==="
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    # Get all ADR numbers sorted
    numbers=$(ls $ADR_DIR/ADR-*.md 2>/dev/null | sed 's/.*ADR-//' | sed 's/-.*//' | sort -nu)
    
    if [ -z "$numbers" ]; then
        print_message "$YELLOW" "⚠️  No ADR files found"
        return
    fi
    
    # Find gaps in numbering
    local gaps=""
    prev=0
    for num in $numbers; do
        # Skip non-numeric entries like "template"
        if [[ "$num" =~ ^[0-9]+$ ]]; then
            num_int=$((10#$num))  # Convert to integer (handle leading zeros)
            
            if [ $prev -ne 0 ] && [ $((num_int - prev)) -gt 1 ]; then
                for ((i=prev+1; i<num_int; i++)); do
                    gaps="$gaps $(printf "%03d" $i)"
                done
            fi
            prev=$num_int
        fi
    done
    
    if [ -n "$gaps" ]; then
        print_message "$YELLOW" "⚠️  Gaps in ADR numbering (missing numbers):"
        echo "    $gaps"
    else
        print_message "$GREEN" "✅ ADR numbering is sequential (no gaps)"
    fi
    
    # Report highest ADR number
    highest=$(echo "$numbers" | tail -1)
    print_message "$BLUE" "ℹ️  Highest ADR number: ADR-$highest"
}

# Function: Check consistency between index and files
check_index_consistency() {
    print_message "$CYAN" "\n=== Checking Index Consistency ==="
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    # Extract ADR references from index
    index_refs=$(grep -oE "ADR-[0-9]{3}" "$INDEX_FILE" 2>/dev/null | sort -u)
    
    # Get actual ADR files
    actual_adrs=$(ls $ADR_DIR/ADR-*.md 2>/dev/null | sed 's/.*\(ADR-[0-9]\{3\}\).*/\1/' | sort -u)
    
    # Find references in index that don't have files
    local phantom_refs=0
    for ref in $index_refs; do
        if ! echo "$actual_adrs" | grep -q "^$ref$"; then
            if [ $phantom_refs -eq 0 ]; then
                print_message "$RED" "❌ References in index without corresponding files:"
            fi
            echo "    - $ref"
            phantom_refs=$((phantom_refs + 1))
            TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
        fi
    done
    
    if [ $phantom_refs -eq 0 ]; then
        print_message "$GREEN" "✅ All index references have corresponding files"
    fi
}

# Function: Generate summary report
generate_report() {
    print_message "$CYAN" "\n========================================="
    print_message "$CYAN" "          ADR INTEGRITY REPORT"
    print_message "$CYAN" "========================================="
    
    # Count total ADRs
    total_adrs=$(ls $ADR_DIR/ADR-*.md 2>/dev/null | wc -l)
    print_message "$BLUE" "Total ADR files: $total_adrs"
    print_message "$BLUE" "Total checks performed: $TOTAL_CHECKS"
    
    if [ $TOTAL_ISSUES -eq 0 ]; then
        print_message "$GREEN" "\n✅ PASSED: No issues found!"
        print_message "$GREEN" "All ADR cross-references are consistent."
    else
        print_message "$RED" "\n❌ FAILED: Found $TOTAL_ISSUES issue(s)"
        print_message "$YELLOW" "Please fix the issues above to maintain ADR integrity."
    fi
    
    # Show last update time from index
    if [ -f "$INDEX_FILE" ]; then
        last_update=$(grep "最後更新:" "$INDEX_FILE" 2>/dev/null || echo "Not found")
        print_message "$BLUE" "\nIndex last updated: $last_update"
    fi
}

# Function: Suggest fixes
suggest_fixes() {
    if [ $TOTAL_ISSUES -gt 0 ]; then
        print_message "$CYAN" "\n=== Suggested Fixes ==="
        print_message "$YELLOW" "1. Run this command to find duplicate numbers:"
        echo "   ls $ADR_DIR/ADR-*.md | sed 's/.*ADR-//' | sed 's/-.*//' | sort | uniq -d"
        
        print_message "$YELLOW" "\n2. To rename an ADR file:"
        echo "   mv $ADR_DIR/ADR-OLD-name.md $ADR_DIR/ADR-NEW-name.md"
        
        print_message "$YELLOW" "\n3. To update the index:"
        echo "   .ai/scripts/update-adr-index.sh"
        
        print_message "$YELLOW" "\n4. To add a new ADR:"
        echo "   .ai/scripts/add-adr.sh \"ADR Title\""
    fi
}

# Main execution
main() {
    print_message "$CYAN" "Starting ADR Cross-Reference Check..."
    
    # Run all checks
    check_duplicate_numbers
    check_naming_convention
    check_index_references
    check_broken_references
    check_numbering_sequence
    check_index_consistency
    
    # Generate report
    generate_report
    
    # Suggest fixes if needed
    suggest_fixes
    
    # Exit with appropriate code
    if [ $TOTAL_ISSUES -gt 0 ]; then
        exit 1
    else
        exit 0
    fi
}

# Run main function
main