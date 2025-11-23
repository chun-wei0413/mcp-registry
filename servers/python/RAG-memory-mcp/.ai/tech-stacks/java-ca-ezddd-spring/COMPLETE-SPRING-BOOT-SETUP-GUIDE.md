# Complete Spring Boot Setup Guide (Based on Actual Implementation) 🔥

## ⚠️ IMPORTANT NOTICE
**This guide is updated based on ACTUAL SOURCE CODE in the aiscrum project.**
All import paths, class names, and configurations reflect the real working implementation.

## 🎯 Purpose
This guide provides **COMPLETE, WORKING** Spring Boot configurations based on the actual aiscrum codebase.
All configurations here are extracted from the working implementation with Spring Boot 3.5.3 + ezddd 3.0.1.

## 🔥 Quick Start with Profile Configuration Sub-agent

If you're starting a new project, use the Profile Configuration Sub-agent to automatically set up all profiles correctly:

```
請使用 profile-config-sub-agent workflow 配置 Spring Profiles
```

This will automatically:
- Create all necessary properties files with correct exclusions
- Set up profile-isolated Configuration classes
- Prevent Repository Bean Not Found errors
- Prevent DataSource configuration errors

## 📦 Complete pom.xml Configuration

**🔥 參考經過驗證的模板**: `.ai/tech-stacks/java-ca-ezddd-spring/examples/pom/pom.xml`

這個完整的 pom.xml 已經過實際專案驗證，包含所有必要的依賴和正確的版本配置。請直接使用此模板作為基礎。
## 🔧 Application Properties Configuration

**🔥 參考經過驗證的模板**: `.ai/tech-stacks/java-ca-ezddd-spring/examples/spring/`

包含完整的配置檔案：
- `application.properties` - 主配置檔案
- `application-inmemory.properties` - InMemory profile（純 Java，無資料庫）
- `application-outbox.properties` - Outbox profile（PostgreSQL）
- `application-eventsourcing.properties` - EventSourcing profile
        </dependency>
        <dependency>
            <groupId>tw.teddysoft.ezddd-gateway</groupId>
            <artifactId>ez-es</artifactId>
            <version>${ezddd-gateway.version}</version>
        </dependency>

        <!-- Jakarta Persistence API (CRITICAL!) -->
        <dependency>
            <groupId>jakarta.persistence</groupId>
            <artifactId>jakarta.persistence-api</artifactId>
        </dependency>

        <!-- ByteBuddy (CRITICAL: Must NOT be in test scope!) -->
        <dependency>
            <groupId>net.bytebuddy</groupId>
            <artifactId>byte-buddy</artifactId>
            <version>${byte-buddy.version}</version>
            <!-- NO scope specified = compile/runtime scope -->
        </dependency>

        <!-- Google Guava (for EventBus) -->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>32.1.2-jre</version>
        </dependency>

        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>tw.teddysoft.ezspec</groupId>
            <artifactId>ezspec-core</artifactId>
            <version>${ezspec.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>tw.teddysoft.ezspec</groupId>
            <artifactId>ezspec-report</artifactId>
            <version>${ezspec.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>tw.teddysoft.ucontract</groupId>
            <artifactId>uContract</artifactId>
            <version>${ucontract.version}</version>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.platform</groupId>
            <artifactId>junit-platform-suite</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## 🚀 Main Application Class

```java
package tw.teddysoft.aiscrum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiScrumApp {
    public static void main(String[] args) {
        SpringApplication.run(AiScrumApp.class, args);
    }
}
```

## 📝 Application Configuration Files

### application.yml (Default)
```yaml
spring:
  application:
    name: aiscrum
  profiles:
    active: prod-outbox  # Default to production with outbox

server:
  port: 8080
```

### application-prod-outbox.yml
```yaml
server:
  port: 6600

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/board?currentSchema=public
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false

  # Message store for outbox pattern
  message:
    store:
      datasource:
        url: jdbc:postgresql://localhost:5432/board?currentSchema=message_store
        username: postgres
        password: postgres
```

### application-test-inmemory.yml
```yaml
server:
  port: 8081

test:
  repository:
    type: inmemory

logging:
  level:
    tw.teddysoft: DEBUG
```

### application-test-outbox.yml
```yaml
server:
  port: 5800

spring:
  datasource:
    url: jdbc:postgresql://localhost:5800/board_test?currentSchema=public
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    show-sql: true

  message:
    store:
      datasource:
        url: jdbc:postgresql://localhost:5800/board_test?currentSchema=message_store
        username: postgres
        password: postgres

test:
  repository:
    type: outbox
```

## 🔧 Core Configuration Classes

### 1. JpaConfiguration (CRITICAL!)
```java
package tw.teddysoft.aiscrum.io.springboot.config.orm;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
    "tw.teddysoft.aiscrum",  // Your domain entities
    "tw.teddysoft.ezddd.data.io.ezes.store"  // Framework entities (MessageData) - ACTUAL PATH IN CODE
})
@EnableJpaRepositories(basePackages = {
    "tw.teddysoft.aiscrum.io.springboot.config.orm",  // OrmClients
    "tw.teddysoft.aiscrum.adapter.out.database.springboot.projection",  // Projections
    "tw.teddysoft.ezddd.data.io.ezes.store"  // Framework repositories - ACTUAL PATH IN CODE
})
public class JpaConfiguration {
}
```

### 2. OutboxInfrastructureConfig
```java
package tw.teddysoft.aiscrum.io.springboot.config;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.jdbc.core.JdbcTemplate;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;
import javax.sql.DataSource;

@Configuration
public class OutboxInfrastructureConfig {
    
    @Bean(name = "pgMessageDbClientInScrum")
    @Profile({"outbox", "test-outbox"})  // Note: actual code uses "outbox" not "prod-outbox"
    public PgMessageDbClient pgMessageDbClient(EntityManager entityManager) {
        // CRITICAL: Must use JpaRepositoryFactory, NOT new!
        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(PgMessageDbClient.class);
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### 3. InMemoryConfiguration
```java
package tw.teddysoft.aiscrum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;
import tw.teddysoft.aiscrum.common.MyInMemoryMessageBroker;

@Configuration
public class UseCaseConfiguration {
    
    @Bean
    public MessageBus<DomainEvent> messageBus() {
        // Actual implementation uses BlockingMessageBus
        return new BlockingMessageBus<>();
    }
    
    @Bean
    public MyInMemoryMessageBroker messageBroker() {
        return new MyInMemoryMessageBroker();
    }
    
    // Additional beans for MessageProducer, etc.
    // See actual UseCaseConfiguration.java for complete implementation
}
```

## 🎯 UseCaseInjection Pattern (CRITICAL for Outbox!)

### Complete UseCaseInjection Template
```java
package tw.teddysoft.aiscrum.io.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository;
import tw.teddysoft.ezddd.data.io.springboot.MessageStore;
import tw.teddysoft.ezddd.gateway.io.springboot.webclient.EzdddGateway;

// Import your domain classes
import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.aiscrum.product.usecase.CreateProductService;
import tw.teddysoft.aiscrum.product.usecase.CreateProductUseCase;
import tw.teddysoft.aiscrum.product.usecase.port.ProductMapper;
import tw.teddysoft.aiscrum.product.usecase.port.out.ProductData;
import tw.teddysoft.aiscrum.io.springboot.config.orm.ProductOrmClient;
import tw.teddysoft.aiscrum.common.adapter.out.repository.GenericInMemoryRepository;
import tw.teddysoft.aiscrum.common.MyInMemoryMessageProducer;

@Configuration
public class UseCaseInjection {
    
    // ========== Repository Beans ==========
    
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
    
    // ========== Use Case Beans ==========
    
    @Bean
    public CreateProductUseCase createProductUseCase(
            Repository<Product, ProductId> productRepository) {
        return new CreateProductService(productRepository);
    }
    
    // Add more use cases following the same pattern...
}
```

### UseCaseInjection for Multiple Aggregates
```java
@Configuration
public class UseCaseInjection {
    
    // ========== Product Aggregate ==========
    
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
    
    // ========== Sprint Aggregate ==========
    
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
    
    // ========== Use Cases ==========
    
    @Bean
    public CreateProductUseCase createProductUseCase(
            Repository<Product, ProductId> productRepository) {
        return new CreateProductService(productRepository);
    }
    
    @Bean
    public CreateSprintUseCase createSprintUseCase(
            Repository<Sprint, SprintId> sprintRepository) {
        return new CreateSprintService(sprintRepository);
    }
}
```

## ⚠️ Critical Configuration Pitfalls to Avoid

### 1. JPA Annotation Errors
```java
// ❌ WRONG - Will cause Hibernate startup failure!
@Column(name = "goal_state")
@Enumerated(EnumType.STRING)  // String cannot have @Enumerated!
private String goalState;

// ✅ CORRECT
@Column(name = "goal_state")
private String goalState;  // Store enum name as plain String
```

### 2. ByteBuddy Dependency Scope
```xml
<!-- ❌ WRONG - Will cause ClassNotFoundException at runtime -->
<dependency>
    <groupId>net.bytebuddy</groupId>
    <artifactId>byte-buddy</artifactId>
    <scope>test</scope>
</dependency>

<!-- ✅ CORRECT -->
<dependency>
    <groupId>net.bytebuddy</groupId>
    <artifactId>byte-buddy</artifactId>
    <!-- No scope = available at runtime -->
</dependency>
```

### 3. SpringJpaClient Creation
```java
// ❌ WRONG - Will not work!
@Bean
public SpringJpaClient springJpaClient(DataSource dataSource) {
    return new SpringJpaClient(dataSource);  // Constructor doesn't exist!
}

// ✅ CORRECT - Must use JpaRepositoryFactory
@Bean
public SpringJpaClient springJpaClient(EntityManager entityManager) {
    RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
    return factory.getRepository(SpringJpaClient.class);
}
```

### 4. Missing Jakarta Persistence API
```xml
<!-- ❌ WRONG - Missing dependency will cause runtime errors -->
<!-- No jakarta.persistence-api dependency -->

<!-- ✅ CORRECT - Must explicitly include -->
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
</dependency>
```

### 5. Profile Configuration for Beans
```java
// ❌ WRONG - Only supports one profile
@Bean
@Profile("test-outbox")
public Repository<Product, ProductId> productRepository(...) {
    // Won't work for prod-outbox!
}

// ✅ CORRECT - Support both test and prod
@Bean
@Profile({"prod-outbox", "test-outbox"})
public Repository<Product, ProductId> productRepository(...) {
    // Works for both profiles
}
```

## 📋 Validation Checklist

Before running your Spring Boot application:

1. ✅ **pom.xml**
   - [ ] ByteBuddy is NOT in test scope
   - [ ] jakarta.persistence-api is included
   - [ ] All ezddd dependencies are correct versions

2. ✅ **JpaConfiguration**
   - [ ] @EntityScan includes your domain packages
   - [ ] @EntityScan includes "tw.teddysoft.ezddd.data.adapter.repository.outbox"
   - [ ] @EnableJpaRepositories includes all necessary packages

3. ✅ **UseCaseInjection**
   - [ ] Each aggregate has both InMemory and Outbox repository beans
   - [ ] Beans have correct @Profile annotations
   - [ ] OutboxRepository uses correct constructor parameters

4. ✅ **Data Classes**
   - [ ] No @Enumerated on String fields
   - [ ] @Transient on domainEventDatas and streamName
   - [ ] All fields have proper JPA annotations

5. ✅ **Application Properties**
   - [ ] Database URLs use correct ports
   - [ ] Schema specified in URL with ?currentSchema=
   - [ ] Message store configuration for outbox profiles

## 🚀 Quick Start Commands

```bash
# Compile the project
mvn clean compile

# Run with default profile (prod-outbox)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring.profiles.active=test-inmemory

# Run tests
mvn test

# Check for configuration issues
.ai/scripts/check-spring-config.sh
```

## 📚 References

- Experiment v17 validation: `.dev/experiments/v17-complete-rebuild.md`
- Spring Boot configuration checklist: `.ai/tech-stacks/java-ca-ezddd-spring/SPRING-BOOT-CONFIGURATION-CHECKLIST.md`
- Outbox Pattern implementation: `.ai/tech-stacks/java-ca-ezddd-spring/examples/outbox/`