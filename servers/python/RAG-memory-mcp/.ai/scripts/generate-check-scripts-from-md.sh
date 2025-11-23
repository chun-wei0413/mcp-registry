#!/bin/bash

# ====================================================================
# Check Scripts Generator from Markdown
# 
# Purpose: 直接從 coding standards .md 文件生成檢查腳本
# Usage: ./generate-check-scripts-from-md.sh [--dry-run]
# 
# 這個腳本會：
# 1. 讀取 coding-standards/*.md 文件
# 2. 解析 ✅ 正確 和 ❌ 錯誤 的程式碼範例
# 3. 提取檢查規則並生成對應的檢查腳本
# ====================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
STANDARDS_DIR="$BASE_DIR/.ai/tech-stacks/java-ca-ezddd-spring/coding-standards"
OUTPUT_DIR="$SCRIPT_DIR/generated"
MD_PARSER_SCRIPT="$SCRIPT_DIR/parse-md-rules.py"

# Parse arguments
DRY_RUN=false
if [ "$1" == "--dry-run" ]; then
    DRY_RUN=true
    echo -e "${YELLOW}DRY RUN MODE - No files will be created${NC}"
fi

# Header
echo -e "${MAGENTA}╔════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║  Check Scripts Generator from Markdown ║${NC}"
echo -e "${MAGENTA}╚════════════════════════════════════════╝${NC}"
echo ""

# ====================================================================
# Step 1: Check Prerequisites
# ====================================================================

echo -e "${CYAN}Step 1: Checking prerequisites...${NC}"

# Check if Python is available
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}✗ Python 3 is required but not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Python 3 found${NC}"

# ====================================================================
# Step 2: Create Markdown Parser
# ====================================================================

echo ""
echo -e "${CYAN}Step 2: Creating markdown parser...${NC}"

if [ "$DRY_RUN" = false ]; then
    cat > "$MD_PARSER_SCRIPT" << 'PYTHON_PARSER'
#!/usr/bin/env python3
"""
Parse coding standards from Markdown files and generate shell script checks
"""

import re
import sys
import os
from pathlib import Path

class MarkdownRuleParser:
    def __init__(self, md_file):
        self.md_file = md_file
        self.rules = []
        
    def parse(self):
        """Parse markdown file and extract rules"""
        with open(self.md_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Extract rule sections
        self._extract_must_follow_rules(content)
        self._extract_code_examples(content)
        
        return self.rules
    
    def _extract_must_follow_rules(self, content):
        """Extract rules from '必須遵守的規則' sections"""
        # Find sections marked with 🔴 or "MUST FOLLOW"
        must_sections = re.findall(
            r'##\s*🔴.*?必須遵守.*?(?=##|\Z)|##.*?MUST FOLLOW.*?(?=##|\Z)',
            content, re.DOTALL | re.IGNORECASE
        )
        
        for section in must_sections:
            # Extract individual rules
            rules_in_section = re.findall(
                r'###\s*\d+\.\s*(.*?)\n(.*?)(?=###|\Z)',
                section, re.DOTALL
            )
            
            for rule_title, rule_content in rules_in_section:
                self._process_rule(rule_title.strip(), rule_content)
    
    def _extract_code_examples(self, content):
        """Extract ✅ correct and ❌ wrong examples"""
        # Find code blocks with ✅ or ❌ markers
        code_blocks = re.findall(
            r'(//\s*[✅❌].*?)\n(.*?)(?=```|//\s*[✅❌]|\Z)',
            content, re.DOTALL
        )
        
        for marker, code in code_blocks:
            if '❌' in marker:
                # This is a wrong pattern - we should check it doesn't exist
                self._add_antipattern_rule(marker, code)
            elif '✅' in marker:
                # This is a correct pattern - we might want to ensure it exists
                self._add_pattern_rule(marker, code)
    
    def _process_rule(self, title, content):
        """Process a rule section and extract checkable patterns"""
        # Look for specific patterns in the rule content
        
        # Check for "不要" (don't) or "禁止" (forbidden) patterns
        if '不要' in content or '禁止' in content or 'not' in content.lower() or "don't" in content.lower():
            # Extract forbidden patterns
            patterns = self._extract_forbidden_patterns(content)
            for pattern in patterns:
                self.rules.append({
                    'name': title,
                    'type': 'forbidden',
                    'pattern': pattern,
                    'description': f'Check: {title}'
                })
        
        # Check for "必須" (must) patterns
        if '必須' in content or 'must' in content.lower():
            patterns = self._extract_required_patterns(content)
            for pattern in patterns:
                self.rules.append({
                    'name': title,
                    'type': 'required',
                    'pattern': pattern,
                    'description': f'Check: {title}'
                })
    
    def _extract_forbidden_patterns(self, content):
        """Extract patterns that should NOT exist"""
        patterns = []
        
        # Look for interface definitions that shouldn't exist
        if 'Repository' in content and 'interface' in content:
            # Custom repository interfaces are forbidden
            patterns.append('interface.*Repository.*extends.*Repository')
        
        # Look for custom query methods
        if 'findBy' in content or 'deleteBy' in content:
            patterns.append('findBy|deleteBy|countBy|existsBy')
        
        # Look for field injection
        if '@Autowired' in content and 'private' in content and 'Field Injection' in content:
            patterns.append('@Autowired\\s+private')
        
        return patterns
    
    def _extract_required_patterns(self, content):
        """Extract patterns that SHOULD exist"""
        patterns = []
        
        # Look for required patterns
        if 'Repository<' in content:
            patterns.append('Repository<.*,.*>')
        
        if 'public static' in content and 'Mapper' in content:
            patterns.append('public static')
        
        if 'Constructor Injection' in content:
            patterns.append('@Autowired\\s+public.*\\(')
        
        return patterns
    
    def _add_antipattern_rule(self, marker, code):
        """Add a rule for code that should NOT exist"""
        # Extract the pattern from the code
        if 'interface' in code and 'Repository' in code:
            self.rules.append({
                'name': 'No Custom Repository Interface',
                'type': 'forbidden',
                'pattern': 'interface.*Repository.*extends.*Repository',
                'description': marker.strip()
            })
    
    def _add_pattern_rule(self, marker, code):
        """Add a rule for code that SHOULD exist"""
        # This could be enhanced to extract specific patterns
        pass

def generate_script_from_rules(rules, script_name, source_file):
    """Generate shell script from extracted rules"""
    
    script = f'''#!/bin/bash

# ====================================================================
# {script_name}
# 
# Generated from: {source_file}
# Purpose: Check compliance based on markdown documentation
# 
# THIS FILE IS AUTO-GENERATED FROM MARKDOWN - DO NOT EDIT MANUALLY
# Regenerate with: ./generate-check-scripts-from-md.sh
# ====================================================================

set -e

# Colors for output
RED='\\033[0;31m'
GREEN='\\033[0;32m'
YELLOW='\\033[1;33m'
BLUE='\\033[0;34m'
NC='\\033[0m' # No Color

# Directories
SCRIPT_DIR="$(cd "$(dirname "${{BASH_SOURCE[0]}}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
SRC_DIR="$BASE_DIR/src/main/java"

# Flags
HAS_VIOLATIONS=false

echo -e "${{BLUE}}=======================================${{NC}}"
echo -e "${{BLUE}}{script_name}${{NC}}"
echo -e "${{BLUE}}=======================================${{NC}}"
echo ""

# ====================================================================
# Checks Generated from Markdown
# ====================================================================
'''
    
    # Add checks for each rule
    for i, rule in enumerate(rules, 1):
        if rule['type'] == 'forbidden':
            # Check that pattern should NOT exist
            script += f'''
# Rule {i}: {rule['name']}
echo -e "${{YELLOW}}Checking: {rule['description']}${{NC}}"

# Pattern that should NOT exist: {rule['pattern']}
VIOLATIONS=$(find "$SRC_DIR" -name "*.java" -type f \\
    -exec grep -l "{rule['pattern']}" {{}} \\; 2>/dev/null || true)

if [ -n "$VIOLATIONS" ]; then
    echo -e "${{RED}}✗ Found violations:${{NC}}"
    for file in $VIOLATIONS; do
        filename=$(basename "$file")
        # Skip if it's the base interface itself
        if [[ "$filename" != "Repository.java" ]]; then
            echo -e "  ${{RED}}→${{NC}} $file"
            grep "{rule['pattern']}" "$file" | head -1 | sed 's/^/    /'
            HAS_VIOLATIONS=true
        fi
    done
else
    echo -e "${{GREEN}}✓ No violations found${{NC}}"
fi
echo ""
'''
        elif rule['type'] == 'required':
            # Check that pattern SHOULD exist
            script += f'''
# Rule {i}: {rule['name']}
echo -e "${{YELLOW}}Checking: {rule['description']}${{NC}}"

# Pattern that should exist: {rule['pattern']}
MATCHES=$(find "$SRC_DIR" -name "*.java" -type f \\
    -exec grep -l "{rule['pattern']}" {{}} \\; 2>/dev/null | wc -l)

if [ "$MATCHES" -gt 0 ]; then
    echo -e "${{GREEN}}✓ Found $MATCHES files with correct pattern${{NC}}"
else
    echo -e "${{YELLOW}}⚠ Warning: Pattern not found in any files${{NC}}"
fi
echo ""
'''
    
    # Add footer
    script += '''
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
'''
    
    return script

def main():
    if len(sys.argv) < 3:
        print("Usage: parse-md-rules.py <md-file> <output-file>")
        sys.exit(1)
    
    md_file = sys.argv[1]
    output_file = sys.argv[2]
    
    if not os.path.exists(md_file):
        print(f"Error: Markdown file not found: {md_file}")
        sys.exit(1)
    
    try:
        # Parse rules from markdown
        parser = MarkdownRuleParser(md_file)
        rules = parser.parse()
        
        if not rules:
            print(f"Warning: No rules extracted from {md_file}")
            rules = []
        
        # Generate script name from file name
        base_name = os.path.basename(md_file).replace('-standards.md', '')
        script_name = f"{base_name.title()} Compliance Check"
        
        # Generate script
        script = generate_script_from_rules(rules, script_name, os.path.basename(md_file))
        
        # Write output
        with open(output_file, 'w') as f:
            f.write(script)
        
        # Make executable
        os.chmod(output_file, 0o755)
        
        print(f"✓ Generated: {output_file} ({len(rules)} rules)")
        
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
PYTHON_PARSER

    chmod +x "$MD_PARSER_SCRIPT"
    echo -e "${GREEN}✓ Markdown parser created${NC}"
else
    echo -e "${YELLOW}⊖ Skipped (dry run)${NC}"
fi

# ====================================================================
# Step 3: Create Output Directory
# ====================================================================

echo ""
echo -e "${CYAN}Step 3: Creating output directory...${NC}"

if [ "$DRY_RUN" = false ]; then
    mkdir -p "$OUTPUT_DIR"
    echo -e "${GREEN}✓ Directory created${NC}"
else
    echo -e "${YELLOW}⊖ Skipped (dry run)${NC}"
fi

# ====================================================================
# Step 4: Process Markdown Files
# ====================================================================

echo ""
echo -e "${CYAN}Step 4: Processing markdown files...${NC}"
echo ""

if [ "$DRY_RUN" = false ]; then
    # Process each markdown file in coding-standards directory
    for md_file in "$STANDARDS_DIR"/*.md; do
        if [ -f "$md_file" ]; then
            base_name=$(basename "$md_file" .md)
            
            # Skip the main coding-standards.md (it's an overview)
            if [[ "$base_name" == "coding-standards" ]]; then
                continue
            fi
            
            # Generate output file name
            output_name="${base_name//-standards/}"
            output_file="$OUTPUT_DIR/check-${output_name}.sh"
            
            echo -e "${BLUE}Processing: $(basename "$md_file")${NC}"
            
            # Parse and generate
            python3 "$MD_PARSER_SCRIPT" "$md_file" "$output_file"
        fi
    done
else
    echo -e "${YELLOW}⊖ Skipped (dry run)${NC}"
    echo ""
    echo "Would process:"
    for md_file in "$STANDARDS_DIR"/*.md; do
        if [ -f "$md_file" ]; then
            echo "  - $(basename "$md_file")"
        fi
    done
fi

# ====================================================================
# Step 5: Update Symlinks
# ====================================================================

echo ""
echo -e "${CYAN}Step 5: Updating compatibility symlinks...${NC}"

if [ "$DRY_RUN" = false ]; then
    # Create/update symlinks for backward compatibility
    for script in "$OUTPUT_DIR"/check-*.sh; do
        if [ -f "$script" ]; then
            base_name=$(basename "$script" .sh)
            link_name="$SCRIPT_DIR/${base_name}-compliance.sh"
            
            # Remove old link if exists
            [ -L "$link_name" ] && rm "$link_name"
            
            # Create new link
            ln -sf "generated/$(basename "$script")" "$link_name"
            echo -e "${GREEN}✓ Linked $(basename "$link_name")${NC}"
        fi
    done
else
    echo -e "${YELLOW}⊖ Skipped (dry run)${NC}"
fi

# ====================================================================
# Summary
# ====================================================================

echo ""
echo -e "${MAGENTA}╔════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║           Generation Complete          ║${NC}"
echo -e "${MAGENTA}╚════════════════════════════════════════╝${NC}"
echo ""

if [ "$DRY_RUN" = false ]; then
    echo -e "${GREEN}Generated scripts are in: $OUTPUT_DIR${NC}"
    echo ""
    echo -e "${BLUE}Key Benefits:${NC}"
    echo "✅ Single Source of Truth: Markdown files"
    echo "✅ No duplicate maintenance"
    echo "✅ Automatic synchronization"
    echo "✅ Human-readable documentation = Machine-checkable rules"
    echo ""
    echo "Next steps:"
    echo "1. Review generated scripts in $OUTPUT_DIR"
    echo "2. Test with: $OUTPUT_DIR/check-repository.sh"
    echo "3. When markdown changes, just re-run this generator"
else
    echo -e "${YELLOW}Dry run complete. Run without --dry-run to generate files.${NC}"
fi

echo ""
echo -e "${BLUE}📚 The markdown documentation IS the specification!${NC}"