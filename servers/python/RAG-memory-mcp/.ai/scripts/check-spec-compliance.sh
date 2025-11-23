#!/bin/bash
# Spec Compliance Checker
# Usage: ./spec-compliance-check.sh <spec-file> <task-name>

SPEC_FILE=$1
TASK_NAME=$2

echo "🔍 Checking compliance for: $TASK_NAME"
echo "📋 Spec file: $SPEC_FILE"
echo ""

# Extract required components from spec
echo "📦 Required Components from Spec:"

# Check for mappers in spec - CRITICAL COMPONENT!
echo "🚨 CRITICAL CHECK: Mappers"
if grep -q '"mappers"' "$SPEC_FILE"; then
    echo "✅ Spec REQUIRES Mappers (MUST NOT BE SKIPPED!):"
    grep -A 10 '"mappers"' "$SPEC_FILE" | grep -E '"name"|"critical"|"note"' | while IFS= read -r line; do
        if echo "$line" | grep -q '"name"'; then
            name=$(echo "$line" | sed 's/.*"name": "\(.*\)".*/\1/')
            echo "  🔴 REQUIRED: $name"
        elif echo "$line" | grep -q '"critical": true'; then
            echo "      ⚠️  CRITICAL: This mapper CANNOT be omitted!"
        elif echo "$line" | grep -q '"note"'; then
            note=$(echo "$line" | sed 's/.*"note": "\(.*\)".*/\1/')
            echo "      📝 Note: $note"
        fi
    done
    
    # Check if mapper files exist
    echo ""
    echo "🔍 Checking Mapper Implementation:"
    MAPPER_COUNT=$(find src -name "*Mapper.java" -type f | wc -l)
    echo "  Found $MAPPER_COUNT mapper file(s)"
    
    # Check if mappers are in correct package
    echo ""
    echo "📍 Checking Mapper Package Location:"
    find src -name "*Mapper.java" -type f | while read file; do
        if grep -q "usecase.port;" "$file"; then
            echo "  ✅ $file (correct package: usecase.port)"
        elif grep -q "adapter.out.mapper;" "$file"; then
            echo "  ❌ $file (WRONG package: adapter.out.mapper - should be usecase.port!)"
        elif grep -q "usecase.mapper;" "$file"; then
            echo "  ❌ $file (WRONG package: usecase.mapper - should be usecase.port!)"
        else
            echo "  ⚠️  $file (check package location)"
        fi
    done
else
    echo "ℹ️  No mappers defined in spec"
fi

# Check for projections in spec
if grep -q '"projections"' "$SPEC_FILE"; then
    echo ""
    echo "✅ Spec requires Projections:"
    grep -A 5 '"projections"' "$SPEC_FILE" | grep '"name"' | sed 's/.*"name": "\(.*\)".*/  - \1/'
fi

# Check for DTOs in spec
if grep -q '"dataTransferObjects"' "$SPEC_FILE"; then
    echo ""
    echo "✅ Spec requires DTOs:"
    grep -A 5 '"dataTransferObjects"' "$SPEC_FILE" | grep '"name"' | sed 's/.*"name": "\(.*\)".*/  - \1/'
fi

echo ""
echo "📊 Compliance Summary:"
echo "-------------------"

# Final validation
MISSING_ITEMS=0

# Check each component type
for component in "mappers" "projections" "dataTransferObjects"; do
    if grep -q "\"$component\"" "$SPEC_FILE"; then
        COUNT=$(grep -A 20 "\"$component\"" "$SPEC_FILE" | grep -c '"name"')
        echo "  $component: Required $COUNT items"
    fi
done

if [ $MISSING_ITEMS -eq 0 ]; then
    echo ""
    echo "✅ All spec requirements appear to be addressed!"
else
    echo ""
    echo "⚠️  Some spec requirements may be missing!"
fi