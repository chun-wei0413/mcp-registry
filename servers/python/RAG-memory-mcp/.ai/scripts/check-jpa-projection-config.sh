#!/bin/bash

# JPA Projection Configuration Checker
# This script validates that all JPA Projections are properly configured in JpaConfiguration
# Author: AI Assistant
# Date: 2025-08-24

echo "================================================"
echo "JPA Projection Configuration Checker"
echo "================================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
TOTAL_ISSUES=0
PROJECTIONS_FOUND=0
MISSING_CONFIGS=0

# Find JpaConfiguration file
JPA_CONFIG=$(find src -name "JpaConfiguration.java" -type f 2>/dev/null | head -1)

if [ -z "$JPA_CONFIG" ]; then
    echo -e "${RED}✗ JpaConfiguration.java not found!${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Found JpaConfiguration: $JPA_CONFIG${NC}"
echo ""

# Extract configured packages from JpaConfiguration
echo "Extracting configured packages from @EnableJpaRepositories..."
# Need to handle multi-line @EnableJpaRepositories with proper extraction
# Look for more lines and handle comments properly
# Also trim trailing whitespace
CONFIGURED_PACKAGES=$(grep -A 40 "@EnableJpaRepositories" "$JPA_CONFIG" | grep -E '^\s*"[^"]+"\s*,?\s*(//.*)?$' | sed 's/[",]//g' | sed 's/^[[:space:]]*//g' | sed 's/\s*\/\/.*//' | sed 's/[[:space:]]*$//')

echo "Configured packages:"
echo "$CONFIGURED_PACKAGES" | while read -r pkg; do
    echo "  - $pkg"
done
echo ""

# Find all JPA Projection files
echo "Searching for JPA Projection implementations..."
echo ""

# Find all files with pattern Jpa*Projection.java
JPA_PROJECTIONS=$(find src -name "Jpa*Projection.java" -type f 2>/dev/null)

if [ -z "$JPA_PROJECTIONS" ]; then
    echo -e "${YELLOW}⚠ No JPA Projection files found (pattern: Jpa*Projection.java)${NC}"
    echo ""
else
    while IFS= read -r projection_file; do
        PROJECTIONS_FOUND=$((PROJECTIONS_FOUND + 1))
        
        # Extract package name from the file
        PACKAGE=$(grep "^package " "$projection_file" | sed 's/package //g' | sed 's/;//g' | sed 's/^[[:space:]]*//g')
        
        # Extract class name
        CLASS_NAME=$(basename "$projection_file" .java)
        
        echo "Found: $CLASS_NAME"
        echo "  File: $projection_file"
        echo "  Package: $PACKAGE"
        
        # Check if package is configured in JpaConfiguration
        if echo "$CONFIGURED_PACKAGES" | grep -q "^$PACKAGE$"; then
            echo -e "  ${GREEN}✓ Package is configured in JpaConfiguration${NC}"
        else
            echo -e "  ${RED}✗ Package NOT configured in JpaConfiguration!${NC}"
            echo -e "  ${YELLOW}  Add to @EnableJpaRepositories: \"$PACKAGE\"${NC}"
            MISSING_CONFIGS=$((MISSING_CONFIGS + 1))
            TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
        fi
        
        # Check for @Repository annotation (should NOT have it)
        # Only check for actual annotation, not comments
        # Look for @Repository at the beginning of a line (with possible spaces/tabs) or after import
        if grep -E "^[[:space:]]*@Repository|^import.*Repository" "$projection_file" | grep -E "^[[:space:]]*@Repository" > /dev/null; then
            echo -e "  ${YELLOW}⚠ Has @Repository annotation (not needed for JPA interfaces)${NC}"
            echo -e "  ${YELLOW}  Spring Data JPA automatically manages the bean${NC}"
        else
            echo -e "  ${GREEN}✓ Correctly does not have @Repository annotation${NC}"
        fi
        
        # Check if extends JpaRepository
        if grep -q "extends.*JpaRepository" "$projection_file"; then
            echo -e "  ${GREEN}✓ Extends JpaRepository${NC}"
        else
            echo -e "  ${YELLOW}⚠ Does not extend JpaRepository (might be intentional)${NC}"
        fi
        
        echo ""
    done <<< "$JPA_PROJECTIONS"
fi

# Also check for potential JPA projections that don't follow naming convention
echo "Checking for other potential JPA repositories in projection packages..."
PROJECTION_REPOS=$(find src -path "*/projection/*.java" -type f 2>/dev/null | xargs grep -l "JpaRepository\|CrudRepository" 2>/dev/null)

if [ ! -z "$PROJECTION_REPOS" ]; then
    while IFS= read -r repo_file; do
        # Skip if already checked
        if echo "$JPA_PROJECTIONS" | grep -q "$repo_file"; then
            continue
        fi
        
        CLASS_NAME=$(basename "$repo_file" .java)
        PACKAGE=$(grep "^package " "$repo_file" | sed 's/package //g' | sed 's/;//g' | sed 's/^[[:space:]]*//g')
        
        echo -e "${YELLOW}⚠ Found potential JPA repository: $CLASS_NAME${NC}"
        echo "  File: $repo_file"
        echo "  Package: $PACKAGE"
        
        if echo "$CONFIGURED_PACKAGES" | grep -q "^$PACKAGE$"; then
            echo -e "  ${GREEN}✓ Package is configured${NC}"
        else
            echo -e "  ${YELLOW}  Consider adding to JpaConfiguration if needed${NC}"
        fi
        echo ""
    done <<< "$PROJECTION_REPOS"
fi

# Summary
echo "================================================"
echo "Summary"
echo "================================================"
echo ""
echo "JPA Projections found: $PROJECTIONS_FOUND"
echo "Missing configurations: $MISSING_CONFIGS"
echo "Total issues: $TOTAL_ISSUES"
echo ""

if [ $TOTAL_ISSUES -eq 0 ]; then
    echo -e "${GREEN}✓ All JPA Projections are properly configured!${NC}"
    exit 0
else
    echo -e "${RED}✗ Found $TOTAL_ISSUES configuration issues that need attention.${NC}"
    echo ""
    echo "To fix:"
    echo "1. Add missing packages to @EnableJpaRepositories in JpaConfiguration.java"
    echo "2. Remove @Repository annotation from JPA Projection interfaces (not needed)"
    echo "3. Spring Data JPA will automatically create beans for interfaces extending JpaRepository"
    echo ""
    exit 1
fi