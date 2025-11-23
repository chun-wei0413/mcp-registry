#!/bin/bash

# =============================================================================
# ADR (Architecture Decision Record) Creation Script
# =============================================================================
# Purpose: Automate the creation of new ADRs and update relevant indexes
# Author: AI Assistant
# Version: 1.0.0
# Date: 2025-08-15
# =============================================================================

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ADR_DIR="$PROJECT_ROOT/.dev/adr"
ADR_TEMPLATE="$ADR_DIR/ADR-template.md"
ADR_README="$ADR_DIR/README.md"
CLAUDE_MD="$PROJECT_ROOT/CLAUDE.md"

# Colors for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly BOLD='\033[1m'
readonly RESET='\033[0m'

# =============================================================================
# Utility Functions
# =============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${RESET} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${RESET} $1"
}

log_warning() {
    echo -e "${YELLOW}[⚠]${RESET} $1"
}

log_error() {
    echo -e "${RED}[✗]${RESET} $1"
}

print_usage() {
    cat << EOF
Usage: $0 [OPTIONS] <title>

Create a new Architecture Decision Record (ADR) with automatic indexing.

ARGUMENTS:
    title           The title for the ADR (will be converted to kebab-case for filename)

OPTIONS:
    --status STATUS Set the initial status (Proposed/Accepted/Deprecated/Superseded)
                    Default: Proposed
    --core          Mark as core decision (will update CLAUDE.md)
    --help          Show this help message

EXAMPLES:
    $0 "Database Selection"
    $0 --status Accepted "API Versioning Strategy"
    $0 --core "Authentication Architecture"

The script will:
1. Find the next available ADR number
2. Create a new ADR file from template
3. Update the index in README.md
4. Optionally update CLAUDE.md for core decisions
EOF
}

# =============================================================================
# Core Functions
# =============================================================================

get_next_adr_number() {
    local last_adr
    last_adr=$(ls "$ADR_DIR"/ADR-*.md 2>/dev/null | grep -E "ADR-[0-9]{3}" | sort -V | tail -1 || echo "")
    
    if [[ -z "$last_adr" ]]; then
        echo "001"
    else
        local last_num
        last_num=$(basename "$last_adr" | sed -E 's/ADR-([0-9]{3}).*/\1/')
        printf "%03d" $((10#$last_num + 1))
    fi
}

convert_to_kebab_case() {
    local input="$1"
    echo "$input" | \
        tr '[:upper:]' '[:lower:]' | \
        sed 's/[^a-z0-9]/-/g' | \
        sed 's/-\+/-/g' | \
        sed 's/^-//;s/-$//'
}

create_adr_file() {
    local number="$1"
    local title="$2"
    local status="$3"
    local kebab_title
    kebab_title=$(convert_to_kebab_case "$title")
    
    local filename="ADR-${number}-${kebab_title}.md"
    local filepath="$ADR_DIR/$filename"
    
    if [[ -f "$filepath" ]]; then
        log_error "ADR file already exists: $filename"
        return 1
    fi
    
    # Create ADR from template
    if [[ -f "$ADR_TEMPLATE" ]]; then
        cp "$ADR_TEMPLATE" "$filepath"
        
        # Update the template with actual values
        local today
        today=$(date +%Y-%m-%d)
        
        # Update title, number, date, and status
        sed -i '' "s/# ADR-\[number\] - \[title\]/# ADR-${number} - ${title}/" "$filepath"
        sed -i '' "s/Date: .*/Date: ${today}/" "$filepath"
        sed -i '' "s/Status: .*/Status: ${status}/" "$filepath"
        
        log_success "Created ADR file: $filename"
    else
        # Create a basic ADR if template doesn't exist
        cat > "$filepath" << EOF
# ADR-${number} - ${title}

## Status
**${status}** - $(date +%Y-%m-%d)

## Context
[Describe the context and problem that needs to be addressed]

## Decision
[Describe the decision that was made]

## Consequences
[Describe the consequences of this decision]

### Positive Consequences
- [List positive outcomes]

### Negative Consequences
- [List negative outcomes or trade-offs]

## Alternatives Considered
[List and briefly describe alternatives that were considered]

## References
[List any relevant references or documentation]
EOF
        log_success "Created ADR file (basic template): $filename"
    fi
    
    echo "$filepath"
}

update_readme_index() {
    local number="$1"
    local title="$2"
    local kebab_title
    kebab_title=$(convert_to_kebab_case "$title")
    local filename="ADR-${number}-${kebab_title}.md"
    
    # Check if README exists
    if [[ ! -f "$ADR_README" ]]; then
        log_error "README.md not found at: $ADR_README"
        return 1
    fi
    
    # Find the index section and add the new entry
    local new_entry="- [ADR-${number}](./${filename}) - ${title}"
    
    # Check if the entry already exists
    if grep -q "ADR-${number}" "$ADR_README"; then
        log_warning "ADR-${number} already exists in README index"
        return 0
    fi
    
    # Add the new entry to the index
    # Find the line with "## Index of ADRs" and add after the last ADR entry
    local temp_file
    temp_file=$(mktemp)
    
    awk -v entry="$new_entry" '
        /^## Index of ADRs/ { in_index=1 }
        /^## / && !/^## Index of ADRs/ { in_index=0 }
        { print }
        /^- \[ADR-[0-9]/ && in_index { last_adr=NR }
        END {
            if (last_adr > 0) {
                # We need to insert after the last ADR
                system("sed -i '\''/" last_adr "/a\\\n" entry "'\'' " FILENAME)
            }
        }
    ' "$ADR_README" > "$temp_file"
    
    # Alternative approach: append to the end of the index section
    # This is simpler and more reliable
    sed -i '' "/^## Index of ADRs/,/^## /{
        /^## [^I]/ !{
            /^- \[ADR-/h
            /^- \[ADR-/!H
            \$!d
        }
        /^## [^I]/i\\
${new_entry}
    }" "$ADR_README" 2>/dev/null || {
        # Fallback: just add before the next section after Index
        perl -i -pe "s/(## Index of ADRs.*?(?:\n- \[ADR-.*?\].*?\n)*)\n(## )/\1${new_entry}\n\n\2/s" "$ADR_README"
    }
    
    log_success "Updated README.md index"
}

update_claude_md() {
    local number="$1"
    local title="$2"
    local description="$3"
    
    if [[ ! -f "$CLAUDE_MD" ]]; then
        log_warning "CLAUDE.md not found, skipping update"
        return 0
    fi
    
    # Check if already exists
    if grep -q "ADR-${number}" "$CLAUDE_MD"; then
        log_warning "ADR-${number} already exists in CLAUDE.md"
        return 0
    fi
    
    # Add the new ADR entry to CLAUDE.md
    local new_entry="- **ADR-${number}**: ${title} - ${description}"
    
    # Find the ADRs section and add the new entry
    sed -i '' "/^### 架構決策 (ADRs)/,/^##/{
        /^- \*\*ADR-/h
        /^- \*\*ADR-/!H
        \$!d
        /^##/i\\
${new_entry}
    }" "$CLAUDE_MD" 2>/dev/null || {
        log_warning "Could not automatically update CLAUDE.md. Please update manually."
    }
    
    log_success "Updated CLAUDE.md"
}

# =============================================================================
# Main Execution
# =============================================================================

main() {
    local title=""
    local status="Proposed"
    local is_core=false
    local show_help=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --status)
                status="$2"
                shift 2
                ;;
            --core)
                is_core=true
                shift
                ;;
            --help)
                show_help=true
                shift
                ;;
            -*)
                log_error "Unknown option: $1"
                print_usage
                exit 1
                ;;
            *)
                if [[ -z "$title" ]]; then
                    title="$1"
                else
                    log_error "Multiple titles provided"
                    print_usage
                    exit 1
                fi
                shift
                ;;
        esac
    done
    
    # Show help if requested
    if [[ "$show_help" == true ]]; then
        print_usage
        exit 0
    fi
    
    # Validate inputs
    if [[ -z "$title" ]]; then
        log_error "Title is required"
        print_usage
        exit 1
    fi
    
    # Validate status
    case "$status" in
        Proposed|Accepted|Deprecated|Superseded)
            ;;
        *)
            log_error "Invalid status: $status"
            echo "Valid statuses: Proposed, Accepted, Deprecated, Superseded"
            exit 1
            ;;
    esac
    
    # Print header
    echo -e "${BOLD}${CYAN}Creating New Architecture Decision Record${RESET}"
    echo -e "Title: ${title}"
    echo -e "Status: ${status}"
    echo -e "Core Decision: $([ "$is_core" == true ] && echo "Yes" || echo "No")"
    echo ""
    
    # Get next ADR number
    local adr_number
    adr_number=$(get_next_adr_number)
    log_info "Next ADR number: ${adr_number}"
    
    # Create ADR file
    local adr_file
    adr_file=$(create_adr_file "$adr_number" "$title" "$status")
    
    # Update README index
    update_readme_index "$adr_number" "$title"
    
    # Update CLAUDE.md if core decision
    if [[ "$is_core" == true ]]; then
        echo ""
        echo -e "${YELLOW}This is marked as a core decision.${RESET}"
        echo "Please provide a brief description for CLAUDE.md:"
        read -r description
        
        if [[ -n "$description" ]]; then
            update_claude_md "$adr_number" "$title" "$description"
        else
            log_warning "No description provided, skipping CLAUDE.md update"
        fi
    fi
    
    # Summary
    echo ""
    echo -e "${BOLD}${GREEN}✅ ADR Creation Complete${RESET}"
    echo -e "Created: $(basename "$adr_file")"
    echo -e "Location: $adr_file"
    echo ""
    echo -e "${CYAN}Next steps:${RESET}"
    echo "1. Edit the ADR file to fill in the details"
    echo "2. Commit the changes"
    if [[ "$is_core" == false ]]; then
        echo "3. If this becomes a core decision, run: $0 --core \"$title\""
    fi
}

# Run main function with all arguments
main "$@"