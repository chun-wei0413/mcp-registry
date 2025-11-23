# InMemory Profile Configuration Reference

## 🎯 Purpose
This is the reference implementation for InMemory profile configuration, showing the correct event propagation architecture.

## 📋 Event Flow Architecture
```
Repository.save() → MessageBus<DomainEvent> → Reactors (direct, synchronous)
```

## 📝 Complete Configuration Example

### InMemoryInfrastructureConfig.java
```java
package tw.teddysoft.aiscrum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;

/**
 * Infrastructure configuration for InMemory profile.
 * Provides essential beans for in-memory event processing.
 */
@Configuration
@Profile({"inmemory", "test-inmemory"})
public class InMemoryInfrastructureConfig {
    
    /**
     * MessageBus bean for direct event propagation.
     * In InMemory mode, events flow directly from Repository to Reactors via MessageBus.
     */
    @Bean
    public MessageBus<DomainEvent> messageBus() {
        return new BlockingMessageBus<>();
    }
    
    // Note: NO MessageBroker or MessageProducer needed in InMemory mode
}
```

### InMemoryRepositoryConfig.java
```java
package tw.teddysoft.aiscrum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.aiscrum.common.adapter.out.repository.GenericInMemoryRepository;
import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

@Configuration
@Profile({"inmemory", "test-inmemory"})
public class InMemoryRepositoryConfig {
    
    @Bean
    public Repository<Product, ProductId> productRepository(
            MessageBus<DomainEvent> messageBus) {
        // MessageBus is injected to enable direct event propagation
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    // Add other aggregate repositories following the same pattern
}
```

### InMemoryProjectionConfig.java
```java
package tw.teddysoft.aiscrum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection.InMemoryProductsProjection;
import tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection.ProductsProjection;

@Configuration
@Profile({"inmemory", "test-inmemory"})
public class InMemoryProjectionConfig {
    
    @Bean
    public ProductsProjection productsProjection() {
        return new InMemoryProductsProjection();
    }
    
    // Add other projections
}
```

## ⚠️ Key Points

### Required Beans for InMemory:
- ✅ `MessageBus<DomainEvent>` - For direct event propagation
- ✅ `Repository` beans with MessageBus injection
- ✅ InMemory Projection implementations

### NOT Required for InMemory:
- ❌ `MyInMemoryMessageBroker` - Only needed for Outbox
- ❌ `MessageProducer<DomainEventData>` - Only needed for Outbox
- ❌ `PgMessageDbClient` - Only needed for Outbox
- ❌ `DataSource` - No database needed
- ❌ `EntityManager` - No JPA needed

## 🔍 How Events Work in InMemory Mode

1. **Repository Save**: When `repository.save(aggregate)` is called
2. **Extract Events**: Repository extracts domain events from the aggregate
3. **Publish to MessageBus**: Events are published directly to MessageBus
4. **Trigger Reactors**: MessageBus immediately triggers registered Reactors
5. **Synchronous Processing**: All processing happens in the same transaction/thread

## 📦 Application Properties

### application-inmemory.yml
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
  jpa:
    enabled: false
```

### application-test-inmemory.yml
```yaml
spring:
  profiles:
    active: test-inmemory
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
  jpa:
    enabled: false
```

## ✅ Validation Checklist

- [ ] MessageBus<DomainEvent> bean is defined
- [ ] All repositories receive MessageBus via constructor
- [ ] No Outbox-related beans are created
- [ ] DataSource auto-configuration is excluded
- [ ] JPA auto-configuration is excluded
- [ ] Application starts without database connection