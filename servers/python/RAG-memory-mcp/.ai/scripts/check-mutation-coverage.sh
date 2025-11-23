#!/bin/bash

# ============================================
# Mutation Testing Coverage Check Script
# ============================================
# Purpose: Automate mutation testing with PIT and validate coverage thresholds
# Usage: .ai/scripts/check-mutation-coverage.sh [entity-name]
# Example: .ai/scripts/check-mutation-coverage.sh ProductBacklogItem
# ============================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
MAVEN_CMD="/opt/homebrew/bin/mvn"
TARGET_MUTATION_COVERAGE=80
TARGET_TEST_STRENGTH=85
BASELINE_REPORT="target/pit-reports/baseline"
CURRENT_REPORT="target/pit-reports/current"

# Function: Print colored message
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function: Check if entity name provided
check_entity_name() {
    if [ -z "$1" ]; then
        print_message $YELLOW "Usage: $0 [entity-name]"
        print_message $YELLOW "Example: $0 ProductBacklogItem"
        echo ""
        print_message $BLUE "Available entities:"
        find src/main/java -name "*.java" -path "*/entity/*" | \
            xargs basename -s .java | sort | uniq | \
            while read entity; do echo "  - $entity"; done
        exit 1
    fi
}

# Function: Verify POM configuration
verify_pom_config() {
    print_message $BLUE "🔍 Verifying PIT configuration in pom.xml..."
    
    if ! grep -q "pitest-maven" pom.xml; then
        print_message $RED "❌ PIT plugin not found in pom.xml"
        exit 1
    fi
    
    if ! grep -q "avoidCallsTo.*ucontract" pom.xml; then
        print_message $YELLOW "⚠️ uContract exclusion not configured in PIT"
        print_message $YELLOW "Add the following to PIT configuration:"
        cat << 'EOF'
        <avoidCallsTo>
            <avoidCallsTo>tw.teddysoft.ucontract.Contract</avoidCallsTo>
            <avoidCallsTo>tw.teddysoft.ucontract</avoidCallsTo>
        </avoidCallsTo>
EOF
        exit 1
    fi
    
    print_message $GREEN "✅ POM configuration verified"
}

# Function: Check existing contracts
analyze_existing_contracts() {
    local entity=$1
    local entity_path=$(find src/main/java -name "${entity}.java" -path "*/entity/*" | head -1)
    
    if [ -z "$entity_path" ]; then
        print_message $RED "❌ Entity ${entity} not found"
        exit 1
    fi
    
    print_message $BLUE "📊 Analyzing existing contracts in ${entity}..."
    
    local require_count=$(grep -c "require(" "$entity_path" 2>/dev/null || echo 0)
    local ensure_count=$(grep -c "ensure(" "$entity_path" 2>/dev/null || echo 0)
    local invariant_count=$(grep -c "invariant(" "$entity_path" 2>/dev/null || echo 0)
    
    echo "  Preconditions (require): $require_count"
    echo "  Postconditions (ensure): $ensure_count"
    echo "  Invariants: $invariant_count"
    echo "  Total: $((require_count + ensure_count + invariant_count))"
    echo ""
}

# Function: Run baseline mutation testing
run_baseline_test() {
    local entity=$1
    
    print_message $BLUE "🧪 Running baseline mutation testing for ${entity}..."
    
    # Create baseline report directory
    mkdir -p "$BASELINE_REPORT"
    
    # Run PIT with specific target
    $MAVEN_CMD org.pitest:pitest-maven:mutationCoverage \
        -DtargetClasses="*.${entity}" \
        -DtargetTests="*.${entity}*Test" \
        -DoutputFormats=HTML,XML \
        -DreportsDirectory="$BASELINE_REPORT" \
        -q > /tmp/pit_baseline.log 2>&1 || {
        print_message $RED "❌ Baseline mutation testing failed"
        cat /tmp/pit_baseline.log
        exit 1
    }
    
    # Extract metrics
    local coverage=$(grep -o 'Line Coverage: [0-9]*%' /tmp/pit_baseline.log | grep -o '[0-9]*' | head -1)
    local mutation_score=$(grep -o 'Mutation Score: [0-9]*%' /tmp/pit_baseline.log | grep -o '[0-9]*' | head -1)
    local test_strength=$(grep -o 'Test Strength: [0-9]*%' /tmp/pit_baseline.log | grep -o '[0-9]*' | head -1)
    
    print_message $YELLOW "📈 Baseline Metrics:"
    echo "  Line Coverage: ${coverage:-0}%"
    echo "  Mutation Score: ${mutation_score:-0}%"
    echo "  Test Strength: ${test_strength:-0}%"
    echo ""
    
    # Store baseline for comparison
    echo "${mutation_score:-0}" > /tmp/baseline_score.txt
    echo "${test_strength:-0}" > /tmp/baseline_strength.txt
}

# Function: Verify tests pass
verify_tests_pass() {
    local entity=$1
    
    print_message $BLUE "✅ Verifying all tests pass for ${entity}..."
    
    $MAVEN_CMD test -Dtest="*${entity}*Test" -q > /tmp/test_results.log 2>&1 || {
        print_message $RED "❌ Tests failed! Review and fix before proceeding."
        tail -20 /tmp/test_results.log
        exit 1
    }
    
    local test_count=$(grep -c "Tests run:" /tmp/test_results.log 2>/dev/null || echo 0)
    print_message $GREEN "✅ All ${test_count} tests passed"
}

# Function: Check for assertion-free test
check_assertion_free_test() {
    local entity=$1
    local test_file=$(find src/test/java -name "${entity}AssertionFreeTest.java" | head -1)
    
    if [ -z "$test_file" ]; then
        print_message $YELLOW "⚠️ No assertion-free test found for ${entity}"
        print_message $YELLOW "Consider creating: ${entity}AssertionFreeTest.java"
        echo ""
        cat << 'EOF'
Template for assertion-free test:
@Test
void exerciseCompleteLifecycle() {
    // Setup
    Entity entity = new Entity(...);
    
    // Exercise operations
    entity.operation1(...);
    entity.operation2(...);
    
    // Contracts validate everything
}
EOF
        echo ""
    else
        print_message $GREEN "✅ Assertion-free test found: $test_file"
    fi
}

# Function: Run final mutation testing
run_final_test() {
    local entity=$1
    
    print_message $BLUE "🧪 Running final mutation testing..."
    
    mkdir -p "$CURRENT_REPORT"
    
    $MAVEN_CMD org.pitest:pitest-maven:mutationCoverage \
        -DtargetClasses="*.${entity}" \
        -DtargetTests="*.${entity}*Test" \
        -DoutputFormats=HTML,XML \
        -DreportsDirectory="$CURRENT_REPORT" \
        -q > /tmp/pit_final.log 2>&1 || {
        print_message $RED "❌ Final mutation testing failed"
        cat /tmp/pit_final.log
        exit 1
    }
    
    # Extract final metrics
    local coverage=$(grep -o 'Line Coverage: [0-9]*%' /tmp/pit_final.log | grep -o '[0-9]*' | head -1)
    local mutation_score=$(grep -o 'Mutation Score: [0-9]*%' /tmp/pit_final.log | grep -o '[0-9]*' | head -1)
    local test_strength=$(grep -o 'Test Strength: [0-9]*%' /tmp/pit_final.log | grep -o '[0-9]*' | head -1)
    
    # Get baseline scores
    local baseline_score=$(cat /tmp/baseline_score.txt 2>/dev/null || echo 0)
    local baseline_strength=$(cat /tmp/baseline_strength.txt 2>/dev/null || echo 0)
    
    print_message $BLUE "📊 Final Results:"
    echo "  Line Coverage: ${coverage:-0}%"
    echo "  Mutation Score: ${mutation_score:-0}% (baseline: ${baseline_score}%)"
    echo "  Test Strength: ${test_strength:-0}% (baseline: ${baseline_strength}%)"
    echo ""
    
    # Check against targets
    if [ "${mutation_score:-0}" -ge "$TARGET_MUTATION_COVERAGE" ]; then
        print_message $GREEN "✅ Mutation coverage target met (≥${TARGET_MUTATION_COVERAGE}%)"
    else
        print_message $YELLOW "⚠️ Below target mutation coverage (target: ${TARGET_MUTATION_COVERAGE}%)"
    fi
    
    if [ "${test_strength:-0}" -ge "$TARGET_TEST_STRENGTH" ]; then
        print_message $GREEN "✅ Test strength target met (≥${TARGET_TEST_STRENGTH}%)"
    else
        print_message $YELLOW "⚠️ Below target test strength (target: ${TARGET_TEST_STRENGTH}%)"
    fi
    
    # Calculate improvement
    local score_improvement=$((${mutation_score:-0} - baseline_score))
    local strength_improvement=$((${test_strength:-0} - baseline_strength))
    
    echo ""
    print_message $BLUE "📈 Improvements:"
    echo "  Mutation Score: +${score_improvement}%"
    echo "  Test Strength: +${strength_improvement}%"
}

# Function: Generate recommendations
generate_recommendations() {
    local entity=$1
    local mutation_score=$(grep -o 'Mutation Score: [0-9]*%' /tmp/pit_final.log | grep -o '[0-9]*' | head -1)
    
    if [ "${mutation_score:-0}" -lt "$TARGET_MUTATION_COVERAGE" ]; then
        print_message $BLUE "💡 Recommendations to improve coverage:"
        echo ""
        echo "1. Add more postconditions (safest):"
        echo "   ensure(\"Result validation\", () -> condition);"
        echo ""
        echo "2. Add invariants for data consistency:"
        echo "   invariant(\"Consistency rule\", () -> condition);"
        echo ""
        echo "3. Carefully add preconditions (test after each):"
        echo "   require(\"Input validation\", () -> condition);"
        echo ""
        echo "4. Create comprehensive assertion-free tests"
        echo ""
        echo "5. Review uncovered mutations in:"
        echo "   ${CURRENT_REPORT}/index.html"
    fi
}

# Main execution
main() {
    local entity=$1
    
    print_message $GREEN "========================================="
    print_message $GREEN "Mutation Testing Coverage Check"
    print_message $GREEN "Entity: ${entity}"
    print_message $GREEN "========================================="
    echo ""
    
    # Step 1: Verify configuration
    verify_pom_config
    
    # Step 2: Analyze existing contracts
    analyze_existing_contracts "$entity"
    
    # Step 3: Run baseline test
    run_baseline_test "$entity"
    
    # Step 4: Verify tests pass
    verify_tests_pass "$entity"
    
    # Step 5: Check for assertion-free test
    check_assertion_free_test "$entity"
    
    # Step 6: Run final test (assuming contracts were added)
    run_final_test "$entity"
    
    # Step 7: Generate recommendations
    generate_recommendations "$entity"
    
    echo ""
    print_message $GREEN "========================================="
    print_message $GREEN "✅ Mutation testing analysis complete"
    print_message $GREEN "========================================="
}

# Check arguments and run
check_entity_name "$1"
main "$1"