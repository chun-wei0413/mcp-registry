# UseCaseInjection Complete Implementation Templates

## Overview
UseCaseInjection is the central dependency injection configuration class that wires together repositories, use cases, and infrastructure components for both test and production profiles.

## Key Principles

1. **Profile-Based Repository Selection**: Each aggregate has two repository implementations - InMemory for testing and Outbox for production/integration testing
2. **Constructor Injection**: All dependencies are injected via constructors
3. **Single Configuration Source**: All use case beans are defined in one place for easier maintenance

## Complete Template for New Project

```java
package tw.teddysoft.aiscrum.io.springboot.config;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.RepositoryFactorySupport;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository;
import tw.teddysoft.ezddd.data.io.ezoutbox.MessageStore;
import tw.teddysoft.ezddd.data.io.ezoutbox.PgMessageDbClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.PostgresMessageStore;
import tw.teddysoft.ezddd.gateway.io.springboot.webclient.EzdddGateway;
import tw.teddysoft.aiscrum.common.GenericInMemoryRepository;
import tw.teddysoft.aiscrum.common.MyInMemoryMessageProducer;

@Configuration
public class UseCaseInjection {
    
    // ==================== Infrastructure Beans ====================
    
    @Bean
    @Profile({"prod-outbox", "test-outbox"})
    public PgMessageDbClient pgMessageDbClient(EntityManager entityManager) {
        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(PgMessageDbClient.class);
    }
    
    @Bean
    @Profile({"prod-outbox", "test-outbox"})
    public MessageStore messageStore(PgMessageDbClient pgMessageDbClient) {
        return new PostgresMessageStore(pgMessageDbClient);
    }
    
    @Bean
    @Profile({"prod-outbox", "test-outbox"})
    public EzdddGateway ezdddGateway(MessageStore messageStore) {
        return new EzdddGateway(messageStore);
    }
    
    @Bean
    @Profile({"prod-outbox", "test-outbox"})
    public MyInMemoryMessageProducer messageProducer() {
        return new MyInMemoryMessageProducer();
    }
    
    // ==================== Repository Beans ====================
    
    // Add repository beans for each aggregate here
    
    // ==================== Use Case Beans ====================
    
    // Add use case beans here
}
```

## Adding a New Aggregate

When adding a new aggregate (e.g., Product), follow this template:

### Step 1: Add Repository Beans

```java
// ========== Product Aggregate Repositories ==========

@Bean
@Profile("test-inmemory")
public Repository<Product, ProductId> productInMemoryRepository(
        MessageBus<DomainEvent> messageBus) {
    return new GenericInMemoryRepository<>(messageBus);
}

@Bean
@Profile({"prod-outbox", "test-outbox"})
public Repository<Product, ProductId> productOutboxRepository(
        ProductOrmClient productOrmClient,
        MessageStore messageStore,
        EzdddGateway ezdddGateway,
        MyInMemoryMessageProducer messageProducer) {
    
    return new OutboxRepository<>(
        productOrmClient,
        new ProductMapper(),
        messageStore,
        ezdddGateway,
        messageProducer
    );
}
```

### Step 2: Add Use Case Beans

```java
// ========== Product Use Cases ==========

@Bean
public CreateProductUseCase createProductUseCase(
        Repository<Product, ProductId> productRepository) {
    return new CreateProductService(productRepository);
}

@Bean
public UpdateProductUseCase updateProductUseCase(
        Repository<Product, ProductId> productRepository) {
    return new UpdateProductService(productRepository);
}

@Bean
public GetProductUseCase getProductUseCase(
        ProductProjection productProjection) {
    return new GetProductService(productProjection);
}
```

## Complete Example with Multiple Aggregates

```java
package tw.teddysoft.aiscrum.io.springboot.config;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.RepositoryFactorySupport;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository;
import tw.teddysoft.ezddd.data.io.ezoutbox.MessageStore;
import tw.teddysoft.ezddd.data.io.ezoutbox.PgMessageDbClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.PostgresMessageStore;
import tw.teddysoft.ezddd.gateway.io.springboot.webclient.EzdddGateway;

// Import common utilities
import tw.teddysoft.aiscrum.common.GenericInMemoryRepository;
import tw.teddysoft.aiscrum.common.MyInMemoryMessageProducer;

// Import Product aggregate
import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.aiscrum.product.usecase.CreateProductUseCase;
import tw.teddysoft.aiscrum.product.usecase.CreateProductService;
import tw.teddysoft.aiscrum.product.usecase.port.ProductMapper;
import tw.teddysoft.aiscrum.io.springboot.config.orm.ProductOrmClient;

// Import Sprint aggregate
import tw.teddysoft.aiscrum.sprint.entity.Sprint;
import tw.teddysoft.aiscrum.sprint.entity.SprintId;
import tw.teddysoft.aiscrum.sprint.usecase.CreateSprintUseCase;
import tw.teddysoft.aiscrum.sprint.usecase.CreateSprintService;
import tw.teddysoft.aiscrum.sprint.usecase.port.SprintMapper;
import tw.teddysoft.aiscrum.io.springboot.config.orm.SprintOrmClient;

// Import Backlog Item aggregate
import tw.teddysoft.aiscrum.backlogitem.entity.BacklogItem;
import tw.teddysoft.aiscrum.backlogitem.entity.BacklogItemId;
import tw.teddysoft.aiscrum.backlogitem.usecase.CreateBacklogItemUseCase;
import tw.teddysoft.aiscrum.backlogitem.usecase.CreateBacklogItemService;
import tw.teddysoft.aiscrum.backlogitem.usecase.port.BacklogItemMapper;
import tw.teddysoft.aiscrum.io.springboot.config.orm.BacklogItemOrmClient;

@Configuration
public class UseCaseInjection {
    
    // ==================== Infrastructure Beans ====================
    
    @Bean
    @Profile({"prod-outbox", "test-outbox"})
    public PgMessageDbClient pgMessageDbClient(EntityManager entityManager) {
        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(PgMessageDbClient.class);
    }
    
    @Bean
    @Profile({"prod-outbox", "test-outbox"})
    public MessageStore messageStore(PgMessageDbClient pgMessageDbClient) {
        return new PostgresMessageStore(pgMessageDbClient);
    }
    
    @Bean
    @Profile({"prod-outbox", "test-outbox"})
    public EzdddGateway ezdddGateway(MessageStore messageStore) {
        return new EzdddGateway(messageStore);
    }
    
    @Bean
    @Profile({"prod-outbox", "test-outbox"})
    public MyInMemoryMessageProducer messageProducer() {
        return new MyInMemoryMessageProducer();
    }
    
    // ==================== Product Aggregate ====================
    
    @Bean
    @Profile("test-inmemory")
    public Repository<Product, ProductId> productInMemoryRepository(
            MessageBus<DomainEvent> messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    @Bean
    @Profile({"prod-outbox", "test-outbox"})
    public Repository<Product, ProductId> productOutboxRepository(
            ProductOrmClient productOrmClient,
            MessageStore messageStore,
            EzdddGateway ezdddGateway,
            MyInMemoryMessageProducer messageProducer) {
        
        return new OutboxRepository<>(
            productOrmClient,
            new ProductMapper(),
            messageStore,
            ezdddGateway,
            messageProducer
        );
    }
    
    @Bean
    public CreateProductUseCase createProductUseCase(
            Repository<Product, ProductId> productRepository) {
        return new CreateProductService(productRepository);
    }
    
    // ==================== Sprint Aggregate ====================
    
    @Bean
    @Profile("test-inmemory")
    public Repository<Sprint, SprintId> sprintInMemoryRepository(
            MessageBus<DomainEvent> messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    @Bean
    @Profile({"prod-outbox", "test-outbox"})
    public Repository<Sprint, SprintId> sprintOutboxRepository(
            SprintOrmClient sprintOrmClient,
            MessageStore messageStore,
            EzdddGateway ezdddGateway,
            MyInMemoryMessageProducer messageProducer) {
        
        return new OutboxRepository<>(
            sprintOrmClient,
            new SprintMapper(),
            messageStore,
            ezdddGateway,
            messageProducer
        );
    }
    
    @Bean
    public CreateSprintUseCase createSprintUseCase(
            Repository<Sprint, SprintId> sprintRepository) {
        return new CreateSprintService(sprintRepository);
    }
    
    // ==================== Backlog Item Aggregate ====================
    
    @Bean
    @Profile("test-inmemory")
    public Repository<BacklogItem, BacklogItemId> backlogItemInMemoryRepository(
            MessageBus<DomainEvent> messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    @Bean
    @Profile({"prod-outbox", "test-outbox"})
    public Repository<BacklogItem, BacklogItemId> backlogItemOutboxRepository(
            BacklogItemOrmClient backlogItemOrmClient,
            MessageStore messageStore,
            EzdddGateway ezdddGateway,
            MyInMemoryMessageProducer messageProducer) {
        
        return new OutboxRepository<>(
            backlogItemOrmClient,
            new BacklogItemMapper(),
            messageStore,
            ezdddGateway,
            messageProducer
        );
    }
    
    @Bean
    public CreateBacklogItemUseCase createBacklogItemUseCase(
            Repository<BacklogItem, BacklogItemId> backlogItemRepository) {
        return new CreateBacklogItemService(backlogItemRepository);
    }
}
```

## Common Mistakes to Avoid

### ❌ Wrong: Missing Profile Annotation
```java
@Bean  // Missing @Profile!
public Repository<Product, ProductId> productRepository(...) {
    // This will be created for ALL profiles!
}
```

### ❌ Wrong: Incorrect PgMessageDbClient Creation
```java
@Bean
public PgMessageDbClient pgMessageDbClient(DataSource dataSource) {
    return new PgMessageDbClient(dataSource);  // Constructor doesn't exist!
}
```

### ❌ Wrong: Mixing Repository Types in Same Profile
```java
@Bean
@Profile("test-outbox")
public Repository<Product, ProductId> productRepository(...) {
    return new GenericInMemoryRepository<>(...);  // Should use OutboxRepository!
}
```

### ✅ Correct: Proper Profile-Based Configuration
```java
@Bean
@Profile("test-inmemory")
public Repository<Product, ProductId> productInMemoryRepository(
        MessageBus<DomainEvent> messageBus) {
    return new GenericInMemoryRepository<>(messageBus);
}

@Bean
@Profile({"prod-outbox", "test-outbox"})
public Repository<Product, ProductId> productOutboxRepository(
        ProductOrmClient productOrmClient,
        MessageStore messageStore,
        EzdddGateway ezdddGateway,
        MyInMemoryMessageProducer messageProducer) {
    
    return new OutboxRepository<>(
        productOrmClient,
        new ProductMapper(),
        messageStore,
        ezdddGateway,
        messageProducer
    );
}
```

## Testing Your Configuration

### 1. Verify Bean Creation

**⚠️ Important: According to ADR-021, never use @ActiveProfiles in test classes. Use TestSuite with ProfileSetter pattern instead.**

```java
// ❌ WRONG - Don't use @ActiveProfiles
// @ActiveProfiles("test-inmemory")

// ✅ CORRECT - Extend BaseUseCaseTest without profile annotation
@SpringBootTest
class UseCaseInjectionTest extends BaseUseCaseTest {
    
    @Autowired
    private ApplicationContext context;
    
    @Value("${spring.profiles.active}")
    private String activeProfile;
    
    @Test
    void should_create_correct_repository_based_on_profile() {
        System.out.println("Active profile: " + activeProfile);
        
        if (activeProfile.contains("inmemory")) {
            assertTrue(context.containsBean("productInMemoryRepository"));
            assertFalse(context.containsBean("productOutboxRepository"));
        } else if (activeProfile.contains("outbox")) {
            assertTrue(context.containsBean("productOutboxRepository"));
            assertFalse(context.containsBean("productInMemoryRepository"));
        }
    }
}
```

### 2. Verify Use Case Wiring
```java
@Test
void should_inject_correct_repository_into_use_case() {
    CreateProductUseCase useCase = context.getBean(CreateProductUseCase.class);
    assertNotNull(useCase);
    
    // Execute use case to verify it's properly wired
    CreateProductUseCase.Input input = new CreateProductUseCase.Input("Test Product");
    CqrsOutput<CreateProductUseCase.Output> output = useCase.execute(input);
    
    assertNotNull(output);
    assertEquals(ExitCode.SUCCESS, output.getExitCode());
}
```

## Checklist for New Implementation

- [ ] Create infrastructure beans (PgMessageDbClient, MessageStore, EzdddGateway)
- [ ] Add InMemory repository bean with `@Profile("test-inmemory")`
- [ ] Add Outbox repository bean with `@Profile({"prod-outbox", "test-outbox"})`
- [ ] Create use case beans (no profile annotation needed)
- [ ] Verify OrmClient exists for Outbox repository
- [ ] Verify Mapper class exists and is instantiated correctly
- [ ] Run tests with both profiles to ensure correct wiring
- [ ] Check that Spring Boot starts successfully with default profile