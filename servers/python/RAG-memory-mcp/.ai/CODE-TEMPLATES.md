# Code Templates

This document contains code templates and patterns commonly used in the project.

## Service Implementation Template

### Command Service Structure
```java
@Override
public CqrsOutput execute(CreateTaskInput input) {
    // 1️⃣ Contracts validation (outside try-catch)
    requireNotNull("input", input);
    requireNotNull("taskId", input.taskId);
    
    // 2️⃣ Entire method body in try-catch
    try {
        var output = CqrsOutput.create();
        
        // Step 1: Load aggregate
        ProductBacklogItem pbi = repository.findById(pbiId).orElse(null);
        if (null == pbi) {
            output.setExitCode(ExitCode.FAILURE)
                  .setMessage("PBI not found");
            return output;
        }
        
        // Step 2: Execute business logic
        pbi.createTask(...);
        
        // Step 3: Save aggregate
        repository.save(pbi);
        
        // Step 4: Return success
        output.setId(input.taskId).setExitCode(ExitCode.SUCCESS);
        return output;
        
    } catch (Exception e) {
        throw new UseCaseFailureException(e.getMessage());
    }
}
```

### Query Service Structure
```java
@Override
public GetProductOutput execute(GetProductInput input) {
    requireNotNull("input", input);
    requireNotNull("productId", input.productId);
    
    try {
        var output = GetProductOutput.create();
        
        // Query from projection
        var productDto = projection.findById(input.productId).orElse(null);
        if (null == productDto) {
            output.setExitCode(ExitCode.FAILURE)
                  .setMessage("Product not found");
            return output;
        }
        
        output.setProduct(productDto)
              .setExitCode(ExitCode.SUCCESS);
        return output;
        
    } catch (Exception e) {
        throw new UseCaseFailureException(e.getMessage());
    }
}
```

## Validation Pattern

### Aggregate Root Validation
```java
// Use Contract for aggregate roots
Contract.requireNotNull("productId cannot be null", productId);
Contract.require(name.length() <= 100, "name exceeds maximum length");
```

### Entity & Value Object Validation
```java
// Use Objects.requireNonNull for entities and value objects
Objects.requireNonNull(pbiId, "pbiId cannot be null");
Objects.requireNonNull(name, "name cannot be null");
```

### Service Constructor Validation
```java
public CreateProductService(Repository<Product, ProductId> repository) {
    Contract.requireNotNull("repository cannot be null", repository);
    this.repository = repository;
}
```

## Aggregate Constructor Template

```java
// ✅ CORRECT: Public constructor
public ProductBacklogItem(ProductId productId, PbiId pbiId, String name) {
    super();
    requireNotNull("productId", productId);
    requireNotNull("pbiId", pbiId);
    requireNotNull("name", name);
    apply(new ProductBacklogItemEvents.PbiCreated(...));
}

// ❌ WRONG: Static factory method
public static ProductBacklogItem create(...) { 
    // Don't use static factory methods
}
```

## Domain Event Template

```java
// ✅ CORRECT: With metadata field
record PbiCreated(
    PbiId pbiId,
    String name,
    Map<String, String> metadata,  // Required
    UUID id,
    Instant occurredOn
) implements ProductBacklogItemEvents {
    public PbiCreated {
        Objects.requireNonNull(pbiId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(metadata);  // Must validate
        Objects.requireNonNull(id);
        Objects.requireNonNull(occurredOn);
    }
    
    @Override
    public Map<String, String> metadata() {
        return metadata;  // Return field, not Map.of()
    }
}
```

## Use Case Interface Template

```java
// ✅ CORRECT: Input and Output as inner classes
public interface GetProductUseCase extends Query<GetProductUseCase.GetProductInput, 
                                                 GetProductUseCase.GetProductOutput> {
    class GetProductInput implements Input {
        public String productId;
        
        public static GetProductInput create() {
            return new GetProductInput();
        }
    }
    
    class GetProductOutput extends CqrsOutput<GetProductOutput> {
        private ProductDto product;
        // getter/setter methods
    }
}
```

## Repository Usage Template

```java
// ✅ CORRECT: Direct use of generic Repository
@Service
public class CreateProductService implements CreateProductUseCase {
    private final Repository<Product, ProductId> repository;
    
    public CreateProductService(Repository<Product, ProductId> repository) {
        this.repository = repository;
    }
}

// ❌ WRONG: Custom repository interface
interface ProductRepository extends Repository<Product, ProductId> {
    // Don't create custom repository interfaces
}
```

## Projection Template

```java
// Interface
public interface ProductProjection {
    Optional<ProductDto> findById(String productId);
    List<ProductDto> findAll();
}

// JPA Implementation
@Repository
public class JpaProductProjection implements ProductProjection {
    private final JpaProductRepository repository;
    
    @Override
    public Optional<ProductDto> findById(String productId) {
        return repository.findById(productId)
            .map(ProductMapper::toDto);
    }
}
```

## Mapper Template

```java
public class ProductMapper {
    public static ProductDto toDto(Product product) {
        var dto = new ProductDto();
        dto.setProductId(product.getProductId().value().toString());
        dto.setName(product.getName());
        dto.setGoal(product.getGoal());
        return dto;
    }
    
    public static Product toDomain(ProductDto dto) {
        // Convert DTO back to domain object if needed
    }
}
```