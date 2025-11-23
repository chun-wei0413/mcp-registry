#!/bin/bash

# =============================================================================
# ADR Index Update Script
# =============================================================================
# Purpose: Update ADR indexes in README.md and CLAUDE.md after manual ADR creation
# Author: AI Assistant
# Version: 1.0.0
# Date: 2025-08-15
# =============================================================================

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ADR_DIR="$PROJECT_ROOT/.dev/adr"
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
Usage: $0 [OPTIONS]

Scan the ADR directory and update all indexes to ensure they're in sync.

OPTIONS:
    --dry-run    Show what would be updated without making changes
    --help       Show this help message

EXAMPLES:
    $0              # Update all ADR indexes
    $0 --dry-run    # Preview changes without updating

The script will:
1. Scan all ADR files in .dev/adr/
2. Update README.md index to match actual files
3. Check CLAUDE.md for missing core ADRs
4. Report any inconsistencies
EOF
}

# =============================================================================
# Core Functions
# =============================================================================

extract_adr_title() {
    local file="$1"
    # Extract title from the first # heading in the file
    grep -m1 "^# ADR-" "$file" | sed 's/^# ADR-[0-9]\{3\} - //'
}

scan_adr_files() {
    local adr_files=()
    
    # Find all ADR files except template
    while IFS= read -r file; do
        if [[ $(basename "$file") != "ADR-template.md" ]]; then
            adr_files+=("$file")
        fi
    done < <(find "$ADR_DIR" -name "ADR-*.md" -type f | sort -V)
    
    printf '%s\n' "${adr_files[@]}"
}

generate_readme_index() {
    local dry_run="$1"
    local current_index=""
    local new_index=""
    local adr_count=0
    
    log_info "Scanning ADR files..."
    
    # Build new index from actual files
    while IFS= read -r file; do
        if [[ -n "$file" ]]; then
            local basename
            basename=$(basename "$file")
            local title
            title=$(extract_adr_title "$file")
            
            if [[ -n "$title" ]]; then
                new_index+="- [${basename%.md}](./${basename}) - ${title}\n"
                ((adr_count++))
            fi
        fi
    done < <(scan_adr_files)
    
    log_info "Found ${adr_count} ADR files"
    
    if [[ "$dry_run" == true ]]; then
        echo -e "${CYAN}Would update README.md index with:${RESET}"
        echo -e "$new_index"
        return 0
    fi
    
    # Create backup
    cp "$ADR_README" "${ADR_README}.bak"
    
    # Update README.md
    # This is complex, so we'll use a temporary file
    local temp_file
    temp_file=$(mktemp)
    
    local in_index=false
    local index_updated=false
    
    while IFS= read -r line; do
        if [[ "$line" == "## Index of ADRs" ]]; then
            in_index=true
            echo "$line" >> "$temp_file"
            echo "" >> "$temp_file"
            echo -e "$new_index" >> "$temp_file"
            index_updated=true
        elif [[ "$in_index" == true && "$line" =~ ^## ]]; then
            in_index=false
            echo "$line" >> "$temp_file"
        elif [[ "$in_index" == false || "$index_updated" == false ]]; then
            if [[ "$in_index" == false || ! "$line" =~ ^-\ \[ADR- ]]; then
                echo "$line" >> "$temp_file"
            fi
        fi
    done < "$ADR_README"
    
    # Replace original with updated version
    mv "$temp_file" "$ADR_README"
    
    log_success "Updated README.md index with ${adr_count} ADRs"
}

check_claude_md() {
    local dry_run="$1"
    
    log_info "Checking CLAUDE.md for ADR references..."
    
    if [[ ! -f "$CLAUDE_MD" ]]; then
        log_warning "CLAUDE.md not found"
        return 0
    fi
    
    local missing_adrs=()
    
    # Check each ADR file
    while IFS= read -r file; do
        if [[ -n "$file" ]]; then
            local basename
            basename=$(basename "$file" .md)
            local adr_num
            adr_num=$(echo "$basename" | grep -oE "ADR-[0-9]{3}")
            
            # Check if this ADR is mentioned in CLAUDE.md
            if ! grep -q "$adr_num" "$CLAUDE_MD"; then
                local title
                title=$(extract_adr_title "$file")
                missing_adrs+=("$adr_num: $title")
            fi
        fi
    done < <(scan_adr_files)
    
    if [[ ${#missing_adrs[@]} -gt 0 ]]; then
        log_warning "The following ADRs are not referenced in CLAUDE.md:"
        for adr in "${missing_adrs[@]}"; do
            echo "  - $adr"
        done
        echo ""
        echo -e "${YELLOW}Consider adding these as core decisions if they're important.${RESET}"
        echo -e "Use: ${CYAN}.ai/scripts/add-adr.sh --core \"Title\"${RESET}"
    else
        log_success "All ADRs that should be in CLAUDE.md are present"
    fi
}

verify_consistency() {
    log_info "Verifying consistency..."
    
    local issues=0
    
    # Check for orphaned entries in README
    while IFS= read -r line; do
        if [[ "$line" =~ ^\-\ \[ADR-([0-9]{3})\]\(\./([^\)]+)\) ]]; then
            local filename="${BASH_REMATCH[2]}"
            if [[ ! -f "$ADR_DIR/$filename" ]]; then
                log_error "README references non-existent file: $filename"
                ((issues++))
            fi
        fi
    done < "$ADR_README"
    
    # Check for duplicate ADR numbers
    local numbers=()
    while IFS= read -r file; do
        if [[ -n "$file" ]]; then
            local num
            num=$(basename "$file" | grep -oE "[0-9]{3}")
            if [[ " ${numbers[@]} " =~ " ${num} " ]]; then
                log_error "Duplicate ADR number found: $num"
                ((issues++))
            else
                numbers+=("$num")
            fi
        fi
    done < <(scan_adr_files)
    
    if [[ $issues -eq 0 ]]; then
        log_success "No consistency issues found"
    else
        log_warning "Found $issues consistency issues"
    fi
    
    return $issues
}

# =============================================================================
# Main Execution
# =============================================================================

main() {
    local dry_run=false
    local show_help=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dry-run)
                dry_run=true
                shift
                ;;
            --help)
                show_help=true
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                print_usage
                exit 1
                ;;
        esac
    done
    
    # Show help if requested
    if [[ "$show_help" == true ]]; then
        print_usage
        exit 0
    fi
    
    # Print header
    echo -e "${BOLD}${CYAN}ADR Index Update Tool${RESET}"
    if [[ "$dry_run" == true ]]; then
        echo -e "${YELLOW}Running in DRY-RUN mode (no changes will be made)${RESET}"
    fi
    echo ""
    
    # Check if ADR directory exists
    if [[ ! -d "$ADR_DIR" ]]; then
        log_error "ADR directory not found: $ADR_DIR"
        exit 1
    fi
    
    # Update README index
    generate_readme_index "$dry_run"
    
    # Check CLAUDE.md
    check_claude_md "$dry_run"
    
    # Verify consistency
    if [[ "$dry_run" == false ]]; then
        verify_consistency || true
    fi
    
    # Summary
    echo ""
    echo -e "${BOLD}${GREEN}✅ Index Update Complete${RESET}"
    
    if [[ "$dry_run" == true ]]; then
        echo -e "${YELLOW}This was a dry run. No files were modified.${RESET}"
        echo "Run without --dry-run to apply changes."
    else
        echo "Backup created: ${ADR_README}.bak"
    fi
}

# Run main function with all arguments
main "$@"