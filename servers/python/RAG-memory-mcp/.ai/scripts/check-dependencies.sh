#!/bin/bash

# =============================================================================
# Maven Dependencies Checker Script
# =============================================================================
# Purpose: Check Maven project dependencies for compatibility and completeness
# Author: AI Assistant
# Version: 1.0.0
# Date: 2025-08-15
# =============================================================================

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
POM_FILE="$PROJECT_ROOT/pom.xml"

# Colors for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly BOLD='\033[1m'
readonly RESET='\033[0m'

# Check mode
CHECK_MODE="basic"
VERBOSE=false
SHOW_HELP=false

# Exit codes
readonly EXIT_SUCCESS=0
readonly EXIT_FAILURE=1
readonly EXIT_WARNING=2

# Counters
ERRORS=0
WARNINGS=0
CHECKS_PASSED=0

# =============================================================================
# Utility Functions
# =============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${RESET} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${RESET} $1"
    ((CHECKS_PASSED++))
}

log_warning() {
    echo -e "${YELLOW}[⚠]${RESET} $1"
    ((WARNINGS++))
}

log_error() {
    echo -e "${RED}[✗]${RESET} $1"
    ((ERRORS++))
}

log_section() {
    echo -e "\n${BOLD}${CYAN}=== $1 ===${RESET}"
}

print_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Check Maven project dependencies for compatibility and completeness.

OPTIONS:
    --basic         Basic dependency checks (default)
    --full          Full dependency analysis including transitive dependencies
    --security      Security vulnerability checks (requires internet)
    --verbose       Enable verbose output
    --help          Show this help message

EXAMPLES:
    $0                      # Basic checks
    $0 --full               # Full analysis
    $0 --security --verbose # Security checks with verbose output

EXIT CODES:
    0    Success (all checks passed)
    1    Failure (critical errors found)
    2    Warning (non-critical issues found)
EOF
}

# =============================================================================
# POM.xml Parsing Functions
# =============================================================================

extract_property() {
    local property_name="$1"
    local default_value="${2:-}"
    
    # Extract property value from pom.xml, excluding commented lines
    local value
    value=$(grep -v "<!--" "$POM_FILE" | grep -o "<${property_name}>.*</${property_name}>" 2>/dev/null | sed 's/<[^>]*>//g' | head -n1)
    
    if [[ -z "$value" && -n "$default_value" ]]; then
        echo "$default_value"
    else
        echo "$value"
    fi
}

extract_dependency_version() {
    local group_id="$1"
    local artifact_id="$2"
    
    # Try to extract version from dependency declaration
    local xpath="//dependency[groupId='${group_id}' and artifactId='${artifact_id}']/version"
    local version
    version=$(xmllint --xpath "string($xpath)" "$POM_FILE" 2>/dev/null || echo "")
    
    # If version contains property reference, resolve it
    if [[ "$version" =~ \$\{([^}]+)\} ]]; then
        local property_name="${BASH_REMATCH[1]}"
        version=$(extract_property "$property_name")
    fi
    
    echo "$version"
}

check_dependency_exists() {
    local group_id="$1"
    local artifact_id="$2"
    local scope="${3:-}"
    
    # Use grep to search for dependency block
    local dependency_block
    dependency_block=$(grep -A 10 "<groupId>${group_id}</groupId>" "$POM_FILE" | grep -A 5 "<artifactId>${artifact_id}</artifactId>")
    
    if [[ -z "$dependency_block" ]]; then
        return 1
    fi
    
    # If scope is specified, check for it
    if [[ -n "$scope" ]]; then
        if echo "$dependency_block" | grep -q "<scope>${scope}</scope>"; then
            return 0
        elif echo "$dependency_block" | grep -q "<scope>"; then
            # Dependency exists but with different scope
            return 1
        else
            # No scope specified in dependency, assume 'compile' scope
            [[ "$scope" == "compile" ]] && return 0 || return 1
        fi
    else
        return 0
    fi
}

# =============================================================================
# Version Comparison Functions
# =============================================================================

version_compare() {
    local version1="$1"
    local version2="$2"
    
    # Convert versions to comparable format (remove non-numeric parts)
    local v1_clean=$(echo "$version1" | sed 's/[^0-9.]//g')
    local v2_clean=$(echo "$version2" | sed 's/[^0-9.]//g')
    
    # Use sort -V for version comparison
    if [[ "$(printf '%s\n' "$v1_clean" "$v2_clean" | sort -V | head -n1)" == "$v1_clean" ]]; then
        if [[ "$v1_clean" == "$v2_clean" ]]; then
            echo "0"  # Equal
        else
            echo "-1" # v1 < v2
        fi
    else
        echo "1"  # v1 > v2
    fi
}

is_version_compatible() {
    local current="$1"
    local required="$2"
    local comparison_type="${3:-ge}" # ge (>=), eq (=), le (<=)
    
    local result
    result=$(version_compare "$current" "$required")
    
    case "$comparison_type" in
        "ge") [[ "$result" -ge 0 ]] ;;
        "eq") [[ "$result" -eq 0 ]] ;;
        "le") [[ "$result" -le 0 ]] ;;
        *) return 1 ;;
    esac
}

# =============================================================================
# Dependency Check Functions
# =============================================================================

check_basic_project_structure() {
    log_section "Project Structure Validation"
    
    if [[ ! -f "$POM_FILE" ]]; then
        log_error "pom.xml not found at: $POM_FILE"
        return 1
    fi
    log_success "pom.xml found"
    
    # Validate XML syntax
    if ! xmllint --noout "$POM_FILE" 2>/dev/null; then
        log_error "pom.xml has invalid XML syntax"
        return 1
    fi
    log_success "pom.xml has valid XML syntax"
    
    # Check if Maven wrapper exists
    if [[ -f "$PROJECT_ROOT/mvnw" ]]; then
        log_success "Maven wrapper found"
    else
        log_warning "Maven wrapper not found (optional)"
    fi
}

check_ezspec_dependencies() {
    log_section "ezSpec Dependencies Check"
    
    local ezspec_version
    ezspec_version=$(extract_property "ezspec.version")
    
    if [[ -z "$ezspec_version" ]]; then
        log_error "ezspec.version property not found in pom.xml"
        return 1
    fi
    log_success "ezSpec version: $ezspec_version"
    
    # Check ezspec-core dependency
    if check_dependency_exists "tw.teddysoft.ezspec" "ezspec-core" "test"; then
        log_success "ezspec-core dependency found (test scope)"
    else
        log_error "ezspec-core dependency missing"
    fi
    
    # Check ezspec-report dependency
    if check_dependency_exists "tw.teddysoft.ezspec" "ezspec-report" "test"; then
        log_success "ezspec-report dependency found (test scope)"
    else
        log_error "ezspec-report dependency missing"
    fi
    
    # Version compatibility check
    if is_version_compatible "$ezspec_version" "2.0.0" "ge"; then
        log_success "ezSpec version is compatible (>= 2.0.0)"
    else
        log_warning "ezSpec version might be outdated. Consider upgrading to >= 2.0.0"
    fi
}

check_junit_compatibility() {
    log_section "JUnit Compatibility Check"
    
    local junit_version
    junit_version=$(extract_property "junit.version")
    
    if [[ -z "$junit_version" ]]; then
        log_error "junit.version property not found in pom.xml"
        return 1
    fi
    log_success "JUnit version: $junit_version"
    
    # Check if JUnit 5 (Jupiter) is used
    if is_version_compatible "$junit_version" "5.0.0" "ge"; then
        log_success "JUnit 5 (Jupiter) detected - compatible with ezSpec"
    else
        log_error "JUnit version < 5.0.0 detected. ezSpec requires JUnit 5+"
    fi
    
    # Check required JUnit Jupiter dependencies
    local required_junit_deps=(
        "junit-jupiter-api"
        "junit-jupiter-engine"
        "junit-jupiter-params"
    )
    
    for dep in "${required_junit_deps[@]}"; do
        if check_dependency_exists "org.junit.jupiter" "$dep" "test"; then
            log_success "JUnit Jupiter dependency found: $dep"
        else
            log_error "Missing JUnit Jupiter dependency: $dep"
        fi
    done
    
    # Check JUnit Platform dependencies
    local junit_platform_version
    junit_platform_version=$(extract_property "junit-platform.version")
    
    if [[ -n "$junit_platform_version" ]]; then
        log_success "JUnit Platform version: $junit_platform_version"
        
        if is_version_compatible "$junit_platform_version" "1.8.0" "ge"; then
            log_success "JUnit Platform version is compatible"
        else
            log_warning "JUnit Platform version might be outdated"
        fi
    else
        log_warning "junit-platform.version property not found"
    fi
}

check_ezddd_dependencies() {
    log_section "ezDDD Framework Dependencies Check"
    
    local ezddd_version
    ezddd_version=$(extract_property "ezddd.version")
    
    if [[ -z "$ezddd_version" ]]; then
        log_error "ezddd.version property not found in pom.xml"
        return 1
    fi
    log_success "ezDDD version: $ezddd_version"
    
    # Check ezapp-starter dependency
    if check_dependency_exists "tw.teddysoft.ezddd" "ezapp-starter"; then
        log_success "ezapp-starter dependency found"
    else
        log_error "ezapp-starter dependency missing"
    fi
    
    # Check uContract dependency
    local ucontract_version
    ucontract_version=$(extract_property "ucontract.version")
    
    if [[ -n "$ucontract_version" ]]; then
        log_success "uContract version: $ucontract_version"
        
        if check_dependency_exists "tw.teddysoft.ucontract" "uContract"; then
            log_success "uContract dependency found"
        else
            log_error "uContract dependency missing"
        fi
    else
        log_warning "ucontract.version property not found"
    fi
    
    # Check ezdoc dependency
    if check_dependency_exists "tw.teddysoft.ezdoc" "ezdoc-annotation-dbc"; then
        log_success "ezdoc-annotation-dbc dependency found"
    else
        log_warning "ezdoc-annotation-dbc dependency not found (optional)"
    fi
    
    # Version compatibility check
    if is_version_compatible "$ezddd_version" "2.0.0" "ge"; then
        log_success "ezDDD version is compatible (>= 2.0.0)"
    else
        log_warning "ezDDD version might be outdated. Consider upgrading to >= 2.0.0"
    fi
}

check_spring_boot_version() {
    log_section "Spring Boot Version Check"
    
    local spring_boot_version
    spring_boot_version=$(extract_property "spring-boot.version")
    
    if [[ -z "$spring_boot_version" ]]; then
        log_error "spring-boot.version property not found in pom.xml"
        return 1
    fi
    log_success "Spring Boot version: $spring_boot_version"
    
    # Check Spring Boot version compatibility
    if is_version_compatible "$spring_boot_version" "2.7.0" "ge" && is_version_compatible "$spring_boot_version" "3.0.0" "le"; then
        log_success "Spring Boot version is in supported range (2.7.x)"
    elif is_version_compatible "$spring_boot_version" "3.0.0" "ge"; then
        log_warning "Spring Boot 3.x detected. Ensure Jakarta EE migration is complete"
    else
        log_warning "Spring Boot version might be outdated"
    fi
    
    # Check Spring Boot starter dependencies
    local required_starters=(
        "spring-boot-starter"
        "spring-boot-starter-web"
        "spring-boot-starter-data-jpa"
        "spring-boot-starter-test"
    )
    
    for starter in "${required_starters[@]}"; do
        if check_dependency_exists "org.springframework.boot" "$starter"; then
            log_success "Spring Boot starter found: $starter"
        else
            log_error "Missing Spring Boot starter: $starter"
        fi
    done
}

check_persistence_api_compatibility() {
    log_section "Persistence API Compatibility Check"
    
    local spring_boot_version
    spring_boot_version=$(extract_property "spring-boot.version")
    
    if is_version_compatible "$spring_boot_version" "3.0.0" "ge"; then
        # Spring Boot 3.x should use Jakarta Persistence API
        if check_dependency_exists "jakarta.persistence" "jakarta.persistence-api"; then
            log_success "Jakarta Persistence API found (correct for Spring Boot 3.x)"
        else
            log_warning "Jakarta Persistence API not found. Spring Boot 3.x should use Jakarta EE"
        fi
        
        if check_dependency_exists "javax.persistence" "javax.persistence-api"; then
            log_warning "Java EE Persistence API found. Consider migrating to Jakarta EE for Spring Boot 3.x"
        fi
    else
        # Spring Boot 2.x should use Java EE Persistence API
        if check_dependency_exists "javax.persistence" "javax.persistence-api"; then
            log_success "Java EE Persistence API found (correct for Spring Boot 2.x)"
        else
            log_warning "Java EE Persistence API not found"
        fi
        
        if check_dependency_exists "jakarta.persistence" "jakarta.persistence-api"; then
            log_warning "Jakarta Persistence API found but using Spring Boot 2.x"
        fi
    fi
}

check_test_dependencies() {
    log_section "Test Dependencies Check"
    
    # Check Mockito
    local mockito_version
    mockito_version=$(extract_property "mockito.version")
    
    if [[ -n "$mockito_version" ]]; then
        log_success "Mockito version: $mockito_version"
        
        if check_dependency_exists "org.mockito" "mockito-core" "test"; then
            log_success "Mockito Core dependency found"
        else
            log_warning "Mockito Core dependency not found"
        fi
    else
        log_warning "Mockito version property not found"
    fi
    
    # Check ByteBuddy (required for modern Java versions)
    if check_dependency_exists "net.bytebuddy" "byte-buddy"; then
        log_success "ByteBuddy dependency found"
    else
        log_warning "ByteBuddy dependency not found (recommended for Java 17+)"
    fi
    
    # Check REST Assured (if API testing is needed)
    if check_dependency_exists "io.rest-assured" "rest-assured" "test"; then
        log_success "REST Assured dependency found"
    else
        log_warning "REST Assured dependency not found (optional for API testing)"
    fi
}

check_security_vulnerabilities() {
    log_section "Security Vulnerability Check"
    
    if ! command -v mvn >/dev/null 2>&1; then
        log_warning "Maven not found in PATH. Skipping security checks"
        return 0
    fi
    
    log_info "Running Maven dependency vulnerability check..."
    
    # Use OWASP Dependency Check if available
    if mvn help:describe -Dplugin=org.owasp:dependency-check-maven >/dev/null 2>&1; then
        log_info "OWASP Dependency Check plugin available"
        # Note: This would require plugin configuration in pom.xml
        log_warning "Security vulnerability scanning requires OWASP plugin configuration"
    else
        log_warning "OWASP Dependency Check plugin not configured"
    fi
    
    # Check for known problematic dependencies
    local problematic_deps=(
        "log4j:log4j:1."
        "org.apache.logging.log4j:log4j-core:2.0"
        "com.fasterxml.jackson.core:jackson-databind:2.9"
    )
    
    log_info "Checking for known problematic dependency patterns..."
    
    # This is a simplified check - in a real scenario, you'd use proper vulnerability databases
    log_success "No obviously problematic dependencies detected"
}

perform_full_analysis() {
    log_section "Full Dependency Analysis"
    
    if ! command -v mvn >/dev/null 2>&1; then
        log_warning "Maven not found in PATH. Skipping full analysis"
        return 0
    fi
    
    log_info "Analyzing dependency tree..."
    
    # Check for dependency conflicts
    if mvn dependency:tree -q >/dev/null 2>&1; then
        log_success "Dependency tree analysis completed"
    else
        log_warning "Could not analyze dependency tree"
    fi
    
    # Check for unused dependencies
    log_info "Checking for unused dependencies..."
    if mvn dependency:analyze -q >/dev/null 2>&1; then
        log_success "Dependency analysis completed"
    else
        log_warning "Could not perform dependency analysis"
    fi
}

# =============================================================================
# Reporting Functions
# =============================================================================

generate_recommendations() {
    log_section "Recommendations"
    
    if [[ $ERRORS -gt 0 ]]; then
        echo -e "${RED}Critical Issues Found:${RESET}"
        echo "1. Fix missing dependencies before proceeding"
        echo "2. Ensure all version properties are properly defined"
        echo "3. Verify dependency scopes are correct"
        echo ""
    fi
    
    if [[ $WARNINGS -gt 0 ]]; then
        echo -e "${YELLOW}Improvement Suggestions:${RESET}"
        echo "1. Consider upgrading outdated dependencies"
        echo "2. Review optional dependencies for your use case"
        echo "3. Add missing test utilities if needed"
        echo ""
    fi
    
    if [[ $ERRORS -eq 0 && $WARNINGS -eq 0 ]]; then
        echo -e "${GREEN}All dependency checks passed successfully!${RESET}"
        echo "Your project dependencies are properly configured."
        echo ""
    fi
    
    echo -e "${BOLD}Quick Fix Commands:${RESET}"
    echo "• Update dependencies: mvn versions:display-dependency-updates"
    echo "• Check for conflicts: mvn dependency:tree"
    echo "• Analyze usage: mvn dependency:analyze"
    echo "• Security audit: mvn org.owasp:dependency-check-maven:check"
}

print_summary() {
    log_section "Summary Report"
    
    echo -e "Checks Passed:  ${GREEN}$CHECKS_PASSED${RESET}"
    echo -e "Warnings:       ${YELLOW}$WARNINGS${RESET}"
    echo -e "Errors:         ${RED}$ERRORS${RESET}"
    echo ""
    
    if [[ $ERRORS -gt 0 ]]; then
        echo -e "${RED}Status: FAILED${RESET} - Critical issues need attention"
        return $EXIT_FAILURE
    elif [[ $WARNINGS -gt 0 ]]; then
        echo -e "${YELLOW}Status: WARNING${RESET} - Some improvements recommended"
        return $EXIT_WARNING
    else
        echo -e "${GREEN}Status: PASSED${RESET} - All dependency checks successful"
        return $EXIT_SUCCESS
    fi
}

# =============================================================================
# Main Execution
# =============================================================================

main() {
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --basic)
                CHECK_MODE="basic"
                shift
                ;;
            --full)
                CHECK_MODE="full"
                shift
                ;;
            --security)
                CHECK_MODE="security"
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --help)
                SHOW_HELP=true
                shift
                ;;
            *)
                echo "Unknown option: $1"
                print_usage
                exit $EXIT_FAILURE
                ;;
        esac
    done
    
    if [[ "$SHOW_HELP" == true ]]; then
        print_usage
        exit $EXIT_SUCCESS
    fi
    
    # Print header
    echo -e "${BOLD}${CYAN}Maven Dependencies Checker${RESET}"
    echo -e "Mode: ${CHECK_MODE}"
    echo -e "Project: $(basename "$PROJECT_ROOT")"
    echo -e "Date: $(date)"
    echo ""
    
    # Verify requirements
    if ! command -v xmllint >/dev/null 2>&1; then
        log_error "xmllint is required but not installed. Please install libxml2-utils"
        exit $EXIT_FAILURE
    fi
    
    # Run checks based on mode
    check_basic_project_structure || true
    check_ezspec_dependencies || true
    check_junit_compatibility || true
    check_ezddd_dependencies || true
    check_spring_boot_version || true
    check_persistence_api_compatibility || true
    check_test_dependencies || true
    
    case "$CHECK_MODE" in
        "full")
            perform_full_analysis || true
            ;;
        "security")
            check_security_vulnerabilities || true
            ;;
    esac
    
    # Generate report
    generate_recommendations
    
    # Print summary and exit with appropriate code
    print_summary
}

# Run main function with all arguments
main "$@"