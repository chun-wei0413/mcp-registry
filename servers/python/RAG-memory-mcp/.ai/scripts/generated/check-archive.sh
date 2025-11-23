#!/bin/bash

# ====================================================================
# Archive Compliance Check
# 
# Generated from: archive-standards.md
# Purpose: Check compliance based on markdown documentation
# 
# THIS FILE IS AUTO-GENERATED FROM MARKDOWN - DO NOT EDIT MANUALLY
# Regenerate with: ./generate-check-scripts-from-md.sh
# ====================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
SRC_DIR="$BASE_DIR/src/main/java"

# Flags
HAS_VIOLATIONS=false

echo -e "${BLUE}=======================================${NC}"
echo -e "${BLUE}Archive Compliance Check${NC}"
echo -e "${BLUE}=======================================${NC}"
echo ""

# ====================================================================
# Checks Generated from Markdown
# ====================================================================

# Rule 1: No Custom Repository Interface
echo -e "${YELLOW}Checking: // ❌ 錯誤：不要使用其他命名模式${NC}"

# Pattern that should NOT exist: interface.*Repository.*extends.*Repository
VIOLATIONS=$(find "$SRC_DIR" -name "*.java" -type f \
    -exec grep -l "interface.*Repository.*extends.*Repository" {} \; 2>/dev/null || true)

if [ -n "$VIOLATIONS" ]; then
    echo -e "${RED}✗ Found violations:${NC}"
    for file in $VIOLATIONS; do
        filename=$(basename "$file")
        # Skip if it's the base interface itself
        if [[ "$filename" != "Repository.java" ]]; then
            echo -e "  ${RED}→${NC} $file"
            grep "interface.*Repository.*extends.*Repository" "$file" | head -1 | sed 's/^/    /'
            HAS_VIOLATIONS=true
        fi
    done
else
    echo -e "${GREEN}✓ No violations found${NC}"
fi
echo ""

# ====================================================================
# Summary
# ====================================================================

if [ "$HAS_VIOLATIONS" = true ]; then
    echo -e "${RED}✗ Violations found! Please fix the issues above.${NC}"
    exit 1
else
    echo -e "${GREEN}✓ All checks passed!${NC}"
    exit 0
fi
