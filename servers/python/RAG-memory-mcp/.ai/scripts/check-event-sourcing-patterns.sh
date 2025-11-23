#!/bin/bash

# Event Sourcing Pattern Compliance Check
# 檢查 Event Sourcing 實作是否符合規範

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "======================================="
echo "Event Sourcing Pattern Compliance Check"
echo "======================================="
echo ""

VIOLATIONS=0

# Function to check files
check_aggregate_constructors() {
    echo "🔍 Checking Aggregate Constructors..."
    
    # Find all aggregate files
    AGGREGATE_FILES=$(find "$PROJECT_ROOT/src" -name "*.java" -type f | xargs grep -l "extends EsAggregateRoot")
    
    for file in $AGGREGATE_FILES; do
        FILENAME=$(basename "$file")
        
        # Check for direct state assignment in constructors
        # Look for pattern: public ClassName(...) { ... this.field = ... apply(...) }
        if grep -A 20 "public.*${FILENAME%.java}.*(" "$file" | grep -B 10 "apply(" | grep "this\.\w* = " > /dev/null; then
            echo "  ❌ $file"
            echo "     Constructor sets state directly before apply()"
            echo "     States should only be set in when() method!"
            ((VIOLATIONS++))
        fi
        
        # Check for manual event replay in ES constructor
        if grep -A 10 "List.*Events.*events" "$file" | grep "for.*event.*when(event)" > /dev/null; then
            echo "  ❌ $file"
            echo "     ES constructor manually replays events"
            echo "     Should use super(events) instead!"
            ((VIOLATIONS++))
        fi
    done
    
    if [ $VIOLATIONS -eq 0 ]; then
        echo "  ✅ All aggregate constructors follow Event Sourcing pattern"
    fi
}

# Function to check when() methods
check_when_methods() {
    echo ""
    echo "🔍 Checking when() Method Implementation..."
    
    AGGREGATE_FILES=$(find "$PROJECT_ROOT/src" -name "*.java" -type f | xargs grep -l "extends EsAggregateRoot")
    
    for file in $AGGREGATE_FILES; do
        # Check if when() method exists
        if ! grep -q "protected void when(" "$file"; then
            echo "  ⚠️  $file"
            echo "     Missing when() method implementation"
            ((VIOLATIONS++))
        fi
    done
    
    if [ $VIOLATIONS -eq 0 ]; then
        echo "  ✅ All aggregates have when() method"
    fi
}

# Function to check for state modifications outside when()
check_state_modifications() {
    echo ""
    echo "🔍 Checking for State Modifications Outside when()..."
    
    AGGREGATE_FILES=$(find "$PROJECT_ROOT/src" -name "*.java" -type f | xargs grep -l "extends EsAggregateRoot")
    
    for file in $AGGREGATE_FILES; do
        # Extract methods that are not constructors or when()
        # Look for this.field = assignments outside when()
        if grep -E "^\s*(public|private|protected).*\(" "$file" | \
           grep -v "constructor\|when(" | \
           while read -r method; do
               # Check if method contains direct state assignment
               grep -A 30 "$method" "$file" | grep "this\.\w* = " | grep -v "apply("
           done > /dev/null 2>&1; then
            echo "  ⚠️  $file"
            echo "     Possible state modification outside when() method"
        fi
    done
}

# Run all checks
check_aggregate_constructors
check_when_methods
check_state_modifications

echo ""
echo "======================================="
if [ $VIOLATIONS -eq 0 ]; then
    echo "✅ All Event Sourcing patterns are correct!"
else
    echo "❌ Found $VIOLATIONS Event Sourcing pattern violations"
    echo ""
    echo "📖 Reference: .ai/checklists/EVENT-SOURCING-REVIEW-CHECKLIST.md"
    exit 1
fi