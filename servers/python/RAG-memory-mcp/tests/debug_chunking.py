"""Debug chunking issue."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from utils.markdown_parser import MarkdownParser

markdown_content = """## Constructor Checks

### Business Constructor Rules

- [ ] **Business constructor must not set state directly**

```java
// Wrong
public Product(ProductId id) {
    this.productId = id;
}
```

### ES Rebuild Constructor

Must call super(events).

```java
// Correct
public Product(List<ProductEvents> events) {
    super(events);
}
```
"""

print("="*60)
print("Test: Extract Code Blocks")
print("="*60)
text_only, code_blocks = MarkdownParser.extract_code_blocks(markdown_content)
print(f"\nText only:\n{text_only}\n")
print(f"Code blocks found: {len(code_blocks)}")
for i, cb in enumerate(code_blocks):
    print(f"  [{i}] Language: {cb['language']}, Position: {cb['position']}")

print("\n" + "="*60)
print("Test: Chunk with Code Awareness")
print("="*60)
chunks = MarkdownParser.chunk_with_code_awareness(markdown_content, max_chunk_size=200)
print(f"\nChunks generated: {len(chunks)}")
for i, chunk in enumerate(chunks):
    print(f"\nChunk {i}:")
    print(f"  Section title: {repr(chunk['section_title'][:50])}")
    print(f"  Is complete: {chunk['is_complete']}")
    print(f"  Code blocks: {len(chunk['code_blocks'])}")
    print(f"  Description length: {len(chunk['description'])}")
