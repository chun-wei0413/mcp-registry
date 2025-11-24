"""
Intelligent Markdown Parser for separating code blocks from text descriptions.

This parser extracts code blocks from markdown and associates them with their
descriptive text, improving semantic search by only embedding the descriptions.
"""
import re
from typing import List, Dict, Tuple


class MarkdownParser:
    """
    Parse markdown content to separate text descriptions from code blocks.

    This improves semantic search quality by:
    1. Only embedding the descriptive text
    2. Storing code blocks separately in metadata
    3. Maintaining the association between descriptions and code
    """

    CODE_BLOCK_PATTERN = re.compile(
        r'^\s*```(\w+)\s*\n(.*?)^\s*```\s*$',
        re.MULTILINE | re.DOTALL
    )
    CODE_BLOCK_PLACEHOLDER_FORMAT = "[CODE_BLOCK_{}]"

    @staticmethod
    def _make_placeholder(position: int) -> str:
        """Create a placeholder string for a code block."""
        return MarkdownParser.CODE_BLOCK_PLACEHOLDER_FORMAT.format(position)

    @staticmethod
    def _create_chunk(description: str, code_blocks: List[Dict],
                     section_title: str = None, is_complete: bool = True) -> Dict:
        """Create a chunk dictionary with consistent structure."""
        return {
            'description': description.strip(),
            'code_blocks': code_blocks,
            'section_title': section_title,
            'is_complete': is_complete
        }

    @staticmethod
    def extract_code_blocks(content: str) -> Tuple[str, List[Dict[str, any]]]:
        """
        Extract code blocks from markdown content.

        Args:
            content: The markdown content containing code blocks

        Returns:
            Tuple of (text_only_content, code_blocks)
            - text_only_content: Content with code blocks replaced by placeholders
            - code_blocks: List of dicts with 'language', 'code', 'position'

        Example:
            Input:
            ```
            ## Constructor Rules
            - [ ] **Must not set state directly**
            ```java
            public Product(String name) {
                this.name = name;  // ❌ Wrong!
            }
            ```
            ```

            Output:
            text_only_content: "## Constructor Rules\n- [ ] **Must not set state directly**\n[CODE_BLOCK_0]"
            code_blocks: [{
                'language': 'java',
                'code': 'public Product(String name) {...}',
                'position': 0
            }]
        """
        code_blocks = []
        text_parts = []
        last_end = 0
        position = 0

        for match in MarkdownParser.CODE_BLOCK_PATTERN.finditer(content):
            # Extract text before this code block
            text_before = content[last_end:match.start()].rstrip()
            if text_before:
                text_parts.append(text_before)

            # Extract code block information
            language = match.group(1)
            code = match.group(2).rstrip()

            code_blocks.append({
                'language': language,
                'code': code,
                'position': position
            })

            # Add placeholder
            text_parts.append(MarkdownParser._make_placeholder(position))
            position += 1
            last_end = match.end()

        # Add remaining text after last code block
        if last_end < len(content):
            remaining_text = content[last_end:].rstrip()
            if remaining_text:
                text_parts.append(remaining_text)

        text_only_content = '\n\n'.join(text_parts)

        return text_only_content, code_blocks

    @staticmethod
    def extract_description_for_code(content: str, code_block_index: int) -> str:
        """
        Extract the descriptive text immediately before a code block.

        This is useful for associating each code block with its explanation.

        Args:
            content: The full markdown content
            code_block_index: Index of the code block to find description for

        Returns:
            The descriptive text above the code block
        """
        text_only, code_blocks = MarkdownParser.extract_code_blocks(content)

        if code_block_index >= len(code_blocks):
            return ""

        # Find the placeholder for this code block
        placeholder = MarkdownParser._make_placeholder(code_block_index)
        parts = text_only.split(placeholder)

        if len(parts) < 1:
            return ""

        # Get the text before this code block
        description = parts[0].strip()

        # If there's a previous code block, only take text after it
        if code_block_index > 0:
            prev_placeholder = MarkdownParser._make_placeholder(code_block_index - 1)
            if prev_placeholder in description:
                description = description.split(prev_placeholder)[-1].strip()

        return description

    @staticmethod
    def chunk_with_code_awareness(content: str, max_chunk_size: int = 4000) -> List[Dict[str, any]]:
        """
        Chunk markdown content while preserving code block associations.

        Each chunk will contain:
        - description: Text-only content (for embedding)
        - code_blocks: Associated code blocks
        - is_complete: Whether this is a complete section

        Args:
            content: The markdown content to chunk
            max_chunk_size: Maximum size of text content (excluding code)

        Returns:
            List of chunks, each containing description and code_blocks
        """
        # First extract ALL code blocks from the entire content
        full_text_only, all_code_blocks = MarkdownParser.extract_code_blocks(content)

        # If the entire content is small enough, return as single chunk
        if len(full_text_only) <= max_chunk_size:
            return [{
                'description': full_text_only.strip(),
                'code_blocks': all_code_blocks,
                'section_title': 'Complete Document',
                'is_complete': True
            }]

        # Otherwise, split by H2 headers
        chunks = []

        # Split by ## headers
        h2_pattern = re.compile(r'^##\s+(.+?)$', re.MULTILINE)
        matches = list(h2_pattern.finditer(full_text_only))

        if not matches:
            # No H2 headers, return entire content as one chunk
            return [{
                'description': full_text_only.strip(),
                'code_blocks': all_code_blocks,
                'section_title': 'Complete Document',
                'is_complete': True
            }]

        # Process sections
        sections = []

        # Add intro section if exists
        if matches[0].start() > 0:
            intro_text = full_text_only[:matches[0].start()].strip()
            if intro_text:
                sections.append(('Introduction', intro_text, 0))

        # Add H2 sections
        for i, match in enumerate(matches):
            section_title = match.group(1).strip()
            start = match.end()
            end = matches[i + 1].start() if i + 1 < len(matches) else len(full_text_only)
            section_text = full_text_only[start:end].strip()
            sections.append((section_title, section_text, match.start()))

        # Create chunks from sections
        for section_title, section_text, section_start in sections:
            # Find which code blocks belong to this section
            section_codes = []
            for code_block in all_code_blocks:
                placeholder = f"[CODE_BLOCK_{code_block['position']}]"
                if placeholder in section_text:
                    section_codes.append(code_block)

            # Add section header back
            full_description = f"## {section_title}\n\n{section_text}" if section_title != "Introduction" else section_text

            if len(section_text) <= max_chunk_size:
                # Section fits in one chunk
                chunks.append(MarkdownParser._create_chunk(
                    description=full_description,
                    code_blocks=section_codes,
                    section_title=section_title,
                    is_complete=True
                ))
            else:
                # Need to split section into multiple chunks
                # Split by paragraphs
                paragraphs = section_text.split('\n\n')
                current_chunk_text = []
                current_chunk_codes = []
                current_size = 0

                for para in paragraphs:
                    para_size = len(para)

                    if current_size + para_size > max_chunk_size and current_chunk_text:
                        # Save current chunk
                        chunk_desc = '\n\n'.join(current_chunk_text)
                        full_desc = f"## {section_title}\n\n{chunk_desc}"
                        chunks.append(MarkdownParser._create_chunk(
                            description=full_desc,
                            code_blocks=current_chunk_codes,
                            section_title=section_title,
                            is_complete=False
                        ))
                        current_chunk_text = []
                        current_chunk_codes = []
                        current_size = 0

                    current_chunk_text.append(para)
                    current_size += para_size

                    # Check if this paragraph has code block placeholders
                    for code_block in section_codes:
                        placeholder = MarkdownParser._make_placeholder(code_block['position'])
                        if placeholder in para and code_block not in current_chunk_codes:
                            current_chunk_codes.append(code_block)

                # Save last chunk
                if current_chunk_text:
                    chunk_desc = '\n\n'.join(current_chunk_text)
                    full_desc = f"## {section_title}\n\n{chunk_desc}"
                    chunks.append(MarkdownParser._create_chunk(
                        description=full_desc,
                        code_blocks=current_chunk_codes,
                        section_title=section_title,
                        is_complete=False
                    ))

        return chunks
