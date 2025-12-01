"""
Test suite for Markdown Parser with code block extraction.
"""
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from utils.markdown_parser import MarkdownParser


def test_extract_code_blocks():
    """Test code block extraction from markdown."""
    print("=" * 60)
    print("Test 1: Extract Code Blocks")
    print("=" * 60)

    markdown_content = """## Constructor Rules

- [ ] **Business constructor must not set state directly**

```java
// [X] Wrong
public Product(ProductId id, ProductName name) {
    this.productId = id;  // Don't do this!
    this.productName = name;  // Don't do this!
    apply(new ProductCreated(...));
}
```

- [ ] **Correct approach: only emit events**

```java
// [OK] Correct
public Product(ProductId id, ProductName name) {
    apply(new ProductCreated(...));  // Only emit event
}
```

## Event Sourcing Rules

Event sourcing rebuild constructor must call super(events).

```java
// [OK] Correct
public Product(List<ProductEvents> events) {
    super(events);  // Let framework handle it
}
```
"""

    text_only, code_blocks = MarkdownParser.extract_code_blocks(markdown_content)

    print("\n[Text Only (for embedding)]:")
    print("-" * 60)
    print(text_only)

    print("\n[Code Blocks (stored separately)]:")
    print("-" * 60)
    for i, block in enumerate(code_blocks):
        print(f"\n[Code Block {i}]")
        print(f"Language: {block['language']}")
        print(f"Position: {block['position']}")
        print(f"Code:\n{block['code'][:100]}...")

    print(f"\n[OK] Extracted {len(code_blocks)} code blocks")
    print(f"[OK] Text content length: {len(text_only)} characters")
    print(f"[OK] Original content length: {len(markdown_content)} characters")
    print(f"[OK] Size reduction for embedding: {100 - (len(text_only) / len(markdown_content) * 100):.1f}%")


def test_chunk_with_code_awareness():
    """Test intelligent chunking that preserves code associations."""
    print("\n" + "=" * 60)
    print("Test 2: Intelligent Chunking with Code Awareness")
    print("=" * 60)

    markdown_content = """## Constructor Checks

### Business Constructor Rules

- [ ] **Business constructor must not set state directly**

```java
// [X] Wrong
public Product(ProductId id) {
    this.productId = id;  // Don't!
}
```

- [ ] **Correct approach**

```java
// [OK] Correct
public Product(ProductId id) {
    apply(new ProductCreated(...));
}
```

### ES Rebuild Constructor

Must call super(events).

```java
// [OK] Correct
public Product(List<ProductEvents> events) {
    super(events);
}
```

## Aggregate Rules

### State Management

All state changes must go through apply().

```java
public void updatePrice(Money newPrice) {
    apply(new PriceChanged(this.id, newPrice));
}
```
"""

    chunks = MarkdownParser.chunk_with_code_awareness(markdown_content, max_chunk_size=200)

    print(f"\n[CHUNK] Generated {len(chunks)} chunks")

    for i, chunk in enumerate(chunks):
        print(f"\n{'='*60}")
        print(f"Chunk {i + 1}:")
        print(f"Section: {chunk['section_title']}")
        print(f"Complete: {chunk['is_complete']}")
        print(f"Code blocks: {len(chunk['code_blocks'])}")

        print(f"\n[DESC] Description ({len(chunk['description'])} chars):")
        print("-" * 60)
        print(chunk['description'][:150] + "..." if len(chunk['description']) > 150 else chunk['description'])

        if chunk['code_blocks']:
            print(f"\n[CODE] Associated Code Blocks:")
            print("-" * 60)
            for code in chunk['code_blocks']:
                print(f"  - Language: {code['language']}")
                print(f"    Position: {code['position']}")
                print(f"    Code preview: {code['code'][:60]}...")


def test_real_world_example():
    """Test with a real-world Event Sourcing documentation example."""
    print("\n" + "=" * 60)
    print("Test 3: Real-World Event Sourcing Documentation")
    print("=" * 60)

    # Real example from Event Sourcing documentation
    es_doc = """# Event Sourcing Code Review Checklist

## [\!] Critical Checks (Must Pass)

### Constructor Validation

- [ ] **Business constructor cannot directly set state**
  ```java
  // [X] Wrong
  public Product(ProductId id, ProductName name) {
      this.productId = id;  // Wrong!
      this.productName = name;  // Wrong!
      apply(new ProductCreated(...));
  }

  // [OK] Correct
  public Product(ProductId id, ProductName name) {
      apply(new ProductCreated(...));  // Only emit events
  }
  ```

- [ ] **ES rebuild constructor must call super(events)**
  ```java
  // [X] Wrong
  public Product(List<ProductEvents> events) {
      for (ProductEvents event : events) {
          when(event);  // Don't handle yourself!
      }
  }

  // [OK] Correct
  public Product(List<ProductEvents> events) {
      super(events);  // Let framework handle
  }
  ```

### Event Application

All state changes must use apply().

```java
// [OK] Correct
public void changePrice(Money newPrice) {
    apply(new PriceChanged(this.id, newPrice));
}
```
"""

    text_only, code_blocks = MarkdownParser.extract_code_blocks(es_doc)

    print(f"\n[STATS] Statistics:")
    print(f"  - Original size: {len(es_doc)} characters")
    print(f"  - Text only size: {len(text_only)} characters")
    print(f"  - Code blocks extracted: {len(code_blocks)}")
    print(f"  - Embedding size reduction: {100 - (len(text_only) / len(es_doc) * 100):.1f}%")

    print(f"\n[TARGET] Benefit: Semantic search will focus on:")
    print(f"  - Descriptions and rules")
    print(f"  - Checklist items")
    print(f"  - Concept explanations")
    print(f"\n[X] Semantic search will NOT be diluted by:")
    print(f"  - Java syntax")
    print(f"  - Code comments")
    print(f"  - Implementation details")

    print(f"\n[OK] But users will still get:")
    print(f"  - Complete code examples in results")
    print(f"  - All {len(code_blocks)} code blocks associated with relevant text")


if __name__ == "__main__":
    test_extract_code_blocks()
    test_chunk_with_code_awareness()
    test_real_world_example()

    print("\n" + "=" * 60)
    print("[OK] All tests completed!")
    print("=" * 60)
