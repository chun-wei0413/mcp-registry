# Common Failure Cases and Anti-Patterns

This document catalogs common mistakes and anti-patterns to avoid when implementing the system.

## Code Generation Failures

### 1. Input/Output File Generation
**❌ WRONG**: Creating separate files for Input/Output classes
```java
// Don't create these as separate files:
CreateProductInput.java
GetProductOutput.java
```

**✅ CORRECT**: Input/Output as inner classes of UseCase interface
```java
public interface CreateProductUseCase {
    class CreateProductInput { }  // Inner class
    class CreateProductOutput { }  // Inner class
}
```

### 2. Missing Mappers
**❌ WRONG**: Forgetting to create Mapper classes for DTOs
- Every DTO needs a corresponding Mapper
- Mappers should be in `usecase.port` package

**✅ CORRECT**: Always create Mapper with DTO
```java
// In usecase.port package
public class ProductMapper {
    public static ProductDto toDto(Product product) { }
}
```

### 3. Missing Projection Implementation
**❌ WRONG**: Query Use Case without Projection
- Query Use Cases need Projection interface
- Need JPA Projection implementation

**✅ CORRECT**: Complete Query implementation
```java
// Interface in usecase.port.out
public interface ProductProjection { }

// Implementation in adapter.out.database.springboot
@Repository
public class JpaProductProjection implements ProductProjection { }
```

### 4. Wrong Package Structure
**❌ WRONG**: Multiple Aggregates in same package
```java
tw.teddysoft.aiscrum.domain
    ├── Product.java
    └── Sprint.java  // Wrong! Different aggregate
```

**✅ CORRECT**: Each Aggregate in separate top-level package
```java
tw.teddysoft.aiscrum.product
    └── entity/Product.java
tw.teddysoft.aiscrum.sprint
    └── entity/Sprint.java
```

### 5. Missing Metadata in Domain Events
**❌ WRONG**: Domain Event without metadata field
```java
record PbiCreated(PbiId pbiId, String name) { }
```

**✅ CORRECT**: Always include metadata
```java
record PbiCreated(
    PbiId pbiId,
    String name,
    Map<String, String> metadata,  // Required!
    UUID id,
    Instant occurredOn
) { }
```

### 6. Wrong Metadata Initialization
**❌ WRONG**: Using Map.of() for metadata
```java
// In constructor
this.metadata = Map.of();  // Immutable!
```

**✅ CORRECT**: Use new HashMap<>()
```java
// In constructor
this.metadata = new HashMap<>();  // Mutable
```

## Architecture Violations

### 7. Custom Repository Interfaces
**❌ WRONG**: Creating custom repository interfaces
```java
public interface ProductRepository extends Repository<Product, ProductId> {
    List<Product> findByName(String name);  // Don't add custom methods!
}
```

**✅ CORRECT**: Use generic Repository directly
```java
@Service
public class CreateProductService {
    private final Repository<Product, ProductId> repository;  // Generic
}
```

### 8. Static Factory Methods in Aggregates
**❌ WRONG**: Using static factory methods
```java
public class Product {
    public static Product create(ProductId id, String name) {
        return new Product(id, name);
    }
}
```

**✅ CORRECT**: Use public constructors
```java
public class Product {
    public Product(ProductId id, String name) {
        // Direct constructor
    }
}
```

## Testing Failures

### 9. Plain JUnit Tests for Use Cases
**❌ WRONG**: Using plain @Test for Use Case tests
```java
@Test
void should_create_product() { }
```

**✅ CORRECT**: Use ezSpec BDD style
```java
@EzScenario
void should_create_product() {
    ezspec.given("a valid product request", () -> { })
          .when("creating the product", () -> { })
          .then("product should be created", () -> { });
}
```

### 10. Wrong Profile Configuration
**❌ WRONG**: Adding @ActiveProfiles to BaseUseCaseTest
```java
@ActiveProfiles("test-inmemory")  // Don't hardcode!
public abstract class BaseUseCaseTest { }
```

**✅ CORRECT**: Let configuration decide profile
```java
// No @ActiveProfiles annotation
public abstract class BaseUseCaseTest { }
```

## Outbox Pattern Failures

### 11. OutboxMapper as Separate Class
**❌ WRONG**: Creating OutboxMapper as independent class
```java
public class ProductOutboxMapper implements OutboxMapper { }
```

**✅ CORRECT**: OutboxMapper as inner class
```java
public class ProductMapper {
    static class Mapper implements OutboxMapper { }  // Inner class
}
```

### 12. Missing @Transient Annotations
**❌ WRONG**: Data class without @Transient
```java
public class ProductData {
    private List<DomainEventData> domainEventDatas;  // Missing @Transient
    private String streamName;  // Missing @Transient
}
```

**✅ CORRECT**: Mark transient fields
```java
public class ProductData {
    @Transient
    private List<DomainEventData> domainEventDatas;
    @Transient
    private String streamName;
}
```

## Import Failures

### 13. Using javax.persistence
**❌ WRONG**: Old javax imports
```java
import javax.persistence.Entity;
import javax.persistence.Transient;
```

**✅ CORRECT**: Use Jakarta
```java
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
```

### 14. Wrong Validation Imports
**❌ WRONG**: Mixed validation approaches
```java
import java.util.Objects;
import tw.teddysoft.ucontract.Contract;
// Using both inconsistently
```

**✅ CORRECT**: Use appropriate validation
- Aggregates: `Contract.requireNotNull()`
- Entities/VOs: `Objects.requireNonNull()`
- Services: `Contract.requireNotNull()` in constructor

## Common Compilation Errors

### 15. Missing Super Constructor Call
**❌ WRONG**: Forgetting super() in aggregate
```java
public Product(ProductId id) {
    // Missing super() call
    this.id = id;
}
```

**✅ CORRECT**: Always call super()
```java
public Product(ProductId id) {
    super();  // Required for EsAggregateRoot
    this.id = id;
}
```

### 16. Wrong Framework Class Names
**❌ WRONG**: Using non-existent classes
```java
extends AggregateRoot  // Doesn't exist
extends DomainObjectId  // Doesn't exist
```

**✅ CORRECT**: Use correct framework classes
```java
extends EsAggregateRoot<ProductId, ProductEvents>
record ProductId(UUID value) implements ValueObject { }
```

## Prevention Strategies

1. **Always run compilation check**: `mvn compile -q`
2. **Check for existing patterns**: Look at similar implementations
3. **Follow the templates**: Use CODE-TEMPLATES.md
4. **Read the standards**: Check coding-standards.md
5. **Use specialized sub-agents**: They have domain-specific knowledge