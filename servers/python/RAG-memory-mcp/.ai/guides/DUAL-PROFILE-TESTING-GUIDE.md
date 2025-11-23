# Dual-Profile Testing Guide

## 🎯 Overview
The EZDDD framework supports two repository patterns that must work seamlessly:
- **InMemory Pattern**: Fast, in-memory storage for development and unit testing
- **Outbox Pattern**: Production-ready pattern with database persistence and eventual consistency

This guide ensures your tests work correctly with both profiles.

## 🏗️ Architecture Overview

```
┌─────────────────┐         ┌─────────────────┐
│  InMemory Mode  │         │  Outbox Mode    │
├─────────────────┤         ├─────────────────┤
│ MessageBus      │         │ PostgreSQL DB   │
│ ↓               │         │ ↓               │
│ Event Published │         │ Store in Outbox │
│ Immediately     │         │ ↓               │
│ ↓               │         │ Relay (Async)   │
│ Test Captures   │         │ ↓               │
│ Event           │         │ Event Published │
└─────────────────┘         └─────────────────┘
```

## 🔑 Key Principles

### 1. Profile Independence
Tests should NOT hardcode which profile to use:
```java
// ❌ WRONG - Hardcoded profile
@ActiveProfiles("test-inmemory")
public class MyTest { }

// ✅ CORRECT - Profile determined externally
@SpringBootTest
public class MyTest extends BaseUseCaseTest { }
```

### 2. Repository Abstraction
Use Cases should depend on abstractions:
```java
// ❌ WRONG - Hardcoded implementation
@Qualifier("productInMemoryRepository")
Repository<Product, ProductId> repository

// ✅ CORRECT - Let Spring inject based on profile
Repository<Product, ProductId> repository
```

### 3. Event Handling Awareness
Tests must handle events differently per profile:
```java
if (activeProfile.contains("inmemory")) {
    // Events published immediately
    await().atMost(1, SECONDS).untilAsserted(() -> {
        assertThat(capturedEvents).hasSize(1);
    });
} else if (activeProfile.contains("outbox")) {
    // Events stored in DB, not immediately available
    // Verify aggregate state instead
}
```

## 📦 Required Components

### For InMemory Profile

#### 1. InMemoryRepositoryConfig
```java
@Configuration
@Profile({"inmemory", "test-inmemory"})
public class InMemoryRepositoryConfig {
    
    @Bean("productInMemoryRepository")
    public Repository<Product, ProductId> productInMemoryRepository(
            MessageBus<DomainEvent> messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
}
```

#### 2. InMemory Test Suite

**📖 See [Dual Profile Testing Configuration](../prompts/shared/dual-profile-testing.md) for complete ProfileSetter pattern implementation.**

Example structure:
```java
@Suite
@SuiteDisplayName("InMemory Pattern Tests")
@SelectClasses({
    InMemoryCreateProductTestSuite.ProfileSetter.class,  // MUST be first!
    CreateProductUseCaseTest.class
})
public class InMemoryCreateProductTestSuite {
    // ProfileSetter inner class - see shared guide
}
```

### For Outbox Profile

#### 1. OutboxRepositoryConfig
```java
@Configuration
@Profile({"outbox", "test-outbox"})
public class OutboxRepositoryConfig {
    
    @Bean
    @Primary  // Important!
    public Repository<Product, ProductId> productRepository() {
        return new OutboxRepository<>(
            new OutboxRepositoryPeerAdapter<>(productOutboxStore()),
            ProductMapper.newMapper()
        );
    }
}
```

#### 2. OrmClient Interface
```java
package tw.teddysoft.aiscrum.io.springboot.config.orm;

public interface ProductOrmClient extends SpringJpaClient<ProductData, String> {
}
```

#### 3. Data Class (NO @Enumerated!)
```java
@Entity
@Table(name = "products")
public class ProductData implements OutboxData<String> {
    
    @Column(name = "state", nullable = false)
    private String state;  // ✅ String, not enum
    
    // ❌ NEVER do this:
    // @Enumerated(EnumType.STRING)
    // private String state;
}
```

#### 4. Mapper with Inner Class
```java
public class ProductMapper {
    
    // Must be inner class (ADR-019)
    static class Mapper implements OutboxMapper<Product, ProductData> {
        @Override
        public Product toDomain(ProductData data) {
            // Convert String back to enum
            ProductLifecycleState state = ProductLifecycleState.valueOf(data.getState());
            // ...
        }
        
        @Override
        public ProductData toData(Product product) {
            // Convert enum to String
            data.setState(product.getState().name());
            // ...
        }
    }
}
```

#### 5. Outbox Test Suite

**📖 See [Dual Profile Testing Configuration](../prompts/shared/dual-profile-testing.md) for complete ProfileSetter pattern implementation.**

Example structure:
```java
@Suite
@SuiteDisplayName("Outbox Pattern Tests")
@SelectClasses({
    OutboxCreateProductTestSuite.ProfileSetter.class,  // MUST be first!
    CreateProductUseCaseTest.class
})
public class OutboxCreateProductTestSuite {
    // ProfileSetter inner class - see shared guide
}
```

## 🧪 Test Implementation

### Base Test Class
```java
@SpringBootTest(classes = AiScrumApp.class)
@EzFeature
@EzFeatureReport
public class CreateProductUseCaseTest extends BaseUseCaseTest {
    
    @Value("${spring.profiles.active:test-inmemory}")
    private String activeProfile;
    
    @Autowired
    private CreateProductUseCase createProductUseCase;
    
    @Autowired
    private Repository<Product, ProductId> productRepository;
    
    @EzScenario
    public void should_create_product_successfully() {
        feature.newScenario(SUCCESS_RULE)
            .Given("...", env -> { })
            .When("...", env -> { })
            .Then("product should be created", env -> {
                // Verify aggregate state (works for both profiles)
                Product product = productRepository.findById(productId).orElse(null);
                assertNotNull(product);
            })
            .And("events should be handled according to profile", env -> {
                if (activeProfile.contains("inmemory")) {
                    // InMemory: Events published immediately
                    await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
                        List<DomainEvent> events = getCapturedEvents();
                        assertThat(events).hasSize(1);
                    });
                } else if (activeProfile.contains("outbox")) {
                    // Outbox: Events in DB, not immediately published
                    System.out.println("Outbox mode: Events stored in database");
                    // The aggregate save is sufficient verification
                }
            })
            .Execute();
    }
}
```

## 🐛 Common Issues and Solutions

### Issue 1: NoSuchBeanDefinitionException
**Error**: No qualifying bean of type 'Repository<Product, ProductId>'

**Cause**: Repository not configured for the active profile

**Solution**:
1. Check both InMemoryRepositoryConfig and OutboxRepositoryConfig have the repository
2. Remove @Qualifier from UseCaseConfiguration
3. Add @Primary to outbox repository beans

### Issue 2: @Enumerated on String Field
**Error**: Property 'Data.state' is annotated '@Enumerated' but its type 'String' is not an enum

**Cause**: Incorrect JPA annotation on Data class

**Solution**:
```java
// Remove @Enumerated
@Column(name = "state", nullable = false)
private String state;
```

### Issue 3: ApplicationContext Threshold Exceeded
**Error**: ApplicationContext failure threshold (1) exceeded

**Cause**: ProfileSetter has @SpringBootTest causing context reload

**Solution**:
```java
// Remove all annotations from ProfileSetter
public static class ProfileSetter {
    static {
        System.setProperty("spring.profiles.active", "test-outbox");
    }
    @Test void setProfile() { }
}
```

### Issue 4: Events Not Captured in Outbox Mode
**Symptom**: Test timeout waiting for events in outbox profile

**Cause**: In outbox mode, events are stored in DB, not published immediately

**Solution**: Use profile-aware assertions (see test implementation above)

## 🔧 Validation Tools

### Check Data Class Annotations
```bash
.ai/scripts/check-data-class-annotations.sh
```

### Validate Dual-Profile Configuration
```bash
.ai/scripts/validate-dual-profile-config.sh
```

### Run Both Profile Tests
```bash
# InMemory tests
mvn test -Dtest=InMemory*TestSuite

# Outbox tests  
mvn test -Dtest=Outbox*TestSuite
```

## 📋 Quick Checklist

Before committing:
- [ ] Both InMemory and Outbox repository configs exist
- [ ] UseCaseConfiguration has no @Qualifier
- [ ] Data classes have no @Enumerated on String fields
- [ ] Test suites have ProfileSetter as first class
- [ ] Tests use profile-aware event assertions
- [ ] Both test suites pass

## 📚 References
- [DATA-CLASS-STANDARDS.md](./DATA-CLASS-STANDARDS.md)
- [DUAL-PROFILE-TEST-CHECKLIST.md](../checklists/DUAL-PROFILE-TEST-CHECKLIST.md)
- [ADR-021: Profile-Based Testing Architecture](../../.dev/adr/ADR-021-profile-based-testing-architecture.md)
- [Outbox Pattern Guide](../tech-stacks/java-ca-ezddd-spring/examples/outbox/README.md)