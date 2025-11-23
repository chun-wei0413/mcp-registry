# Outbox Profile Configuration Reference

## 🎯 Purpose
This is the reference implementation for Outbox profile configuration, showing the correct event persistence and relay architecture.

## 📋 Event Flow Architecture
```
Repository.save() → PostgreSQL → EzesCatchUpRelay → MessageProducer → MessageBroker → Reactors
```

## 📝 Complete Configuration Example

### OutboxInfrastructureConfig.java
```java
package tw.teddysoft.aiscrum.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import tw.teddysoft.aiscrum.common.MyInMemoryMessageBroker;
import tw.teddysoft.aiscrum.common.MyInMemoryMessageProducer;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer;

/**
 * Infrastructure configuration for Outbox profile.
 * Provides essential beans for event persistence and relay.
 */
@Configuration
@Profile({"outbox", "test-outbox"})
public class OutboxInfrastructureConfig {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * PgMessageDbClient for reading events from PostgreSQL.
     * MUST use JpaRepositoryFactory - direct instantiation will fail!
     */
    @Bean
    public PgMessageDbClient pgMessageDbClient() {
        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(PgMessageDbClient.class);
    }
    
    /**
     * MessageBroker for async event distribution.
     * Started in AiScrumApp.init() as a separate thread.
     */
    @Bean
    public MyInMemoryMessageBroker messageBroker() {
        return new MyInMemoryMessageBroker();
    }
    
    /**
     * MessageProducer that sends events to MessageBroker.
     * Used by EzesCatchUpRelay to forward events from database.
     */
    @Bean
    public MessageProducer<DomainEventData> messageProducer(
            MyInMemoryMessageBroker messageBroker) {
        return new MyInMemoryMessageProducer(messageBroker);
    }
    
    // Note: NO MessageBus<DomainEvent> bean in Outbox mode
}
```

### OutboxRepositoryConfig.java
```java
package tw.teddysoft.aiscrum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.aiscrum.product.adapter.out.database.springboot.outbox.ProductOrmClient;
import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.aiscrum.product.mapper.ProductMapper;
import tw.teddysoft.aiscrum.product.usecase.port.out.ProductData;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxRepositoryPeerAdapter;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxStore;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.PgMessageOutboxStore;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository;

@Configuration
@Profile({"outbox", "test-outbox"})
public class OutboxRepositoryConfig {
    
    @Bean
    public OutboxStore<ProductData, String> productOutboxStore(
            PgMessageDbClient pgMessageDbClient) {
        return new PgMessageOutboxStore<>(pgMessageDbClient);
    }
    
    @Bean
    public Repository<Product, ProductId> productRepository(
            OutboxStore<ProductData, String> outboxStore) {
        
        // Create ORM client for database operations
        ProductOrmClient ormClient = new ProductOrmClient();
        
        // Create Outbox client with mapper
        EzOutboxClient<Product, ProductData> outboxClient = 
            new EzOutboxClient<>(new ProductMapper.Mapper(), outboxStore);
        
        // Create repository peer adapter
        OutboxRepositoryPeerAdapter<Product, ProductData, String> peerAdapter =
            new OutboxRepositoryPeerAdapter<>(
                ormClient,
                outboxClient,
                new ProductMapper.Mapper()
            );
        
        // Return OutboxRepository
        return new OutboxRepository<>(peerAdapter, ormClient);
    }
    
    // Add other aggregate repositories following the same pattern
}
```

### JpaConfiguration.java
```java
package tw.teddysoft.aiscrum.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile({"outbox", "test-outbox"})
@EnableJpaRepositories(basePackages = {
    "tw.teddysoft.aiscrum.product.adapter.out.database.springboot.outbox",
    "tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection"
    // Add other packages as needed
})
@EntityScan(basePackages = {
    "tw.teddysoft.aiscrum.product.usecase.port.out"
    // Add other entity packages
})
public class JpaConfiguration {
    // JPA configuration for Outbox profile
}
```

## ⚠️ Key Points

### Required Beans for Outbox:
- ✅ `PgMessageDbClient` - Created via JpaRepositoryFactory
- ✅ `MyInMemoryMessageBroker` - For async event distribution
- ✅ `MessageProducer<DomainEventData>` - For sending events
- ✅ `OutboxStore` instances for each aggregate
- ✅ `DataSource` - PostgreSQL connection
- ✅ `EntityManager` - JPA persistence context

### NOT Required for Outbox:
- ❌ `MessageBus<DomainEvent>` - Events go through database instead
- ❌ Direct event propagation - Uses persistence + relay

## 🔍 How Events Work in Outbox Mode

1. **Repository Save**: When `repository.save(aggregate)` is called
2. **Persist to Database**: Events are saved to PostgreSQL message store
3. **EzesCatchUpRelay**: Reads unprocessed events from database
4. **MessageProducer**: Forwards events to MessageBroker
5. **MessageBroker**: Distributes events to registered Reactors
6. **Async Processing**: Events are processed asynchronously with guarantee

## 🚀 AiScrumApp Integration

The AiScrumApp.init() method starts the relay mechanism:

```java
@PostConstruct
public void init() {
    if (messageBroker != null && pgMessageDbClient != null && messageProducer != null) {
        // Start MessageBroker thread
        executor.execute(messageBroker);
        
        // Start EzesCatchUpRelay
        executor.execute(createEzesCatchUpEventRelay(
            pgMessageDbClient,
            messageProducer,
            RDB_SCRUM_CHECKPOINT_PATH
        ));
    }
}
```

## 📦 Application Properties

### application-outbox.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5500/ezscrum
    username: ezscrum
    password: ezscrum
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
```

### application-test-outbox.yml
```yaml
spring:
  profiles:
    active: test-outbox
  datasource:
    url: jdbc:postgresql://localhost:5800/ezscrum_test
    username: ezscrum
    password: ezscrum
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

## ✅ Validation Checklist

- [ ] PgMessageDbClient created via JpaRepositoryFactory
- [ ] MyInMemoryMessageBroker bean defined
- [ ] MessageProducer<DomainEventData> bean defined
- [ ] NO MessageBus<DomainEvent> bean
- [ ] OutboxMapper is inner class (per ADR-019)
- [ ] @Transient on domainEventDatas and streamName
- [ ] Using jakarta.persistence (not javax)
- [ ] PostgreSQL container running on correct port