#!/bin/bash

# Script: check-data-class-annotations.sh
# Purpose: Check Data classes for incorrect @Enumerated usage on String fields
# This prevents JPA entity configuration errors

echo "=================================="
echo "Checking Data Classes Annotations"
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ERROR_COUNT=0
WARNING_COUNT=0

echo -e "\n${YELLOW}Scanning for Data classes...${NC}"

# Find all Data.java files
while IFS= read -r file; do
    echo -n "Checking $(basename "$file")... "
    
    # Check for @Enumerated on String fields
    if grep -q "@Enumerated" "$file"; then
        # Check if the @Enumerated is followed by String type
        if grep -B1 "private String" "$file" | grep -q "@Enumerated"; then
            echo -e "${RED}ERROR${NC}"
            echo -e "  ${RED}❌ Found @Enumerated on String field in:${NC}"
            echo "     $file"
            echo "  Lines with issues:"
            grep -n -B1 "private String" "$file" | grep -A1 "@Enumerated" | head -5
            ((ERROR_COUNT++))
        else
            # Check if there's any enum type field (which shouldn't exist in Data classes)
            if grep -q "@Enumerated.*\(private\|protected\|public\).*enum" "$file"; then
                echo -e "${YELLOW}WARNING${NC}"
                echo -e "  ${YELLOW}⚠️  Found enum field in Data class:${NC}"
                echo "     $file"
                echo "  Data classes should use String instead of enum types"
                ((WARNING_COUNT++))
            else
                echo -e "${GREEN}OK${NC}"
            fi
        fi
    else
        # Additional checks for Data class best practices
        
        # Check if all fields have @Column annotation
        if grep -q "private \(String\|boolean\|int\|long\|Integer\|Long\|Boolean\)" "$file"; then
            field_count=$(grep -c "private \(String\|boolean\|int\|long\|Integer\|Long\|Boolean\)" "$file")
            column_count=$(grep -c "@Column" "$file")
            
            if [ "$field_count" -gt "$column_count" ]; then
                echo -e "${YELLOW}WARNING${NC}"
                echo -e "  ${YELLOW}⚠️  Some fields might be missing @Column annotation${NC}"
                echo "     Fields: $field_count, @Column annotations: $column_count"
                ((WARNING_COUNT++))
            else
                echo -e "${GREEN}OK${NC}"
            fi
        else
            echo -e "${GREEN}OK${NC}"
        fi
    fi
done < <(find . -name "*Data.java" -type f -not -path "./target/*" -not -path "./.git/*")

echo -e "\n=================================="
echo "Additional Checks"
echo "=================================="

# Check for enum types in Data classes
echo -e "\n${YELLOW}Checking for enum type fields in Data classes...${NC}"
ENUM_FILES=$(grep -l "private.*enum\|private.*[A-Z][a-zA-Z]*State\|private.*[A-Z][a-zA-Z]*Type\|private.*[A-Z][a-zA-Z]*Status" $(find . -name "*Data.java" -type f -not -path "./target/*" -not -path "./.git/*") 2>/dev/null)

if [ -n "$ENUM_FILES" ]; then
    echo -e "${YELLOW}⚠️  Potential enum fields found in:${NC}"
    for file in $ENUM_FILES; do
        echo "   - $file"
        grep -n "private.*enum\|private.*State\|private.*Type\|private.*Status" "$file" | head -3
    done
    ((WARNING_COUNT++))
fi

# Summary
echo -e "\n=================================="
echo "Summary"
echo "=================================="

if [ $ERROR_COUNT -eq 0 ] && [ $WARNING_COUNT -eq 0 ]; then
    echo -e "${GREEN}✅ All Data classes are correctly annotated!${NC}"
    echo "No @Enumerated on String fields found."
    exit 0
else
    if [ $ERROR_COUNT -gt 0 ]; then
        echo -e "${RED}❌ Found $ERROR_COUNT critical errors${NC}"
        echo "   - @Enumerated cannot be used on String fields"
        echo "   - Fix: Remove @Enumerated annotation from String fields"
    fi
    
    if [ $WARNING_COUNT -gt 0 ]; then
        echo -e "${YELLOW}⚠️  Found $WARNING_COUNT warnings${NC}"
        echo "   - Consider using String instead of enum in Data classes"
        echo "   - Ensure all fields have @Column annotations"
    fi
    
    echo -e "\n${YELLOW}Recommendations:${NC}"
    echo "1. Data classes should only use primitive types and Strings"
    echo "2. Convert enums to String using enum.name()"
    echo "3. All fields should have explicit @Column(name=\"...\") annotations"
    echo "4. Never use @Enumerated on String fields"
    
    exit 1
fi