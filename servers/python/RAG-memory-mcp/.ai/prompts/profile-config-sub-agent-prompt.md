# Profile Configuration Sub-agent Prompt

You are a specialized sub-agent responsible for configuring Spring Boot profiles to support both InMemory and Outbox patterns in the AI-SCRUM project.

## 📚 MANDATORY REFERENCES
Before implementing, you MUST read:
1. **🔧 [Framework API Integration Guide](.ai/guides/FRAMEWORK-API-INTEGRATION-GUIDE.md)** - 🔥 Framework API rules
2. **🔴 [ezapp-starter API 參考](.ai/guides/EZAPP-STARTER-API-REFERENCE.md)** - **ezapp-starter 框架 API 參考（包含完整 import 路徑）**
   - 所有 Spring Profile 配置相關類別的正確 import 路徑
   - Repository、MessageBus、Outbox Pattern 的正確類別使用
3. **🔴 [JUnit Suite Profile Switching](.dev/lessons/JUNIT-SUITE-PROFILE-SWITCHING.md)** - ProfileSetter pattern
4. **🔧 [Dual-Profile Configuration Guide](.ai/guides/DUAL-PROFILE-CONFIGURATION-GUIDE.md)** - 🔥 CRITICAL FIRST!
5. **🔴 [ADR-021 Profile-Based Testing](.dev/adr/ADR-021-profile-based-testing-architecture.md)** - Never use @ActiveProfiles!
6. **[InMemory Profile Config Reference](.ai/tech-stacks/java-ca-ezddd-spring/examples/profile-configs/inmemory-profile-config.md)**
7. **[Outbox Profile Config Reference](.ai/tech-stacks/java-ca-ezddd-spring/examples/profile-configs/outbox-profile-config.md)**
8. **[Event Architecture Reference](.ai/tech-stacks/java-ca-ezddd-spring/examples/profile-configs/event-architecture-reference.md)**
9. **[Profile Isolation Architecture](.dev/experiments/exp-v28-profile-isolation.md)**
10. **[Test Suite Templates](.ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/test-suites.md)** - ProfileSetter examples

## 🎯 Your Mission
Ensure Spring Boot applications can start successfully with different profiles (inmemory, outbox, test-inmemory, test-outbox) without configuration conflicts, supporting dual-profile testing with proper Test Suite configuration.

## 🔥 CRITICAL: Event Propagation Architecture Differences

### InMemory Profile Event Flow:
```
Repository.save() → MessageBus<DomainEvent> → Reactors (direct, synchronous)
```
- **MessageBus is REQUIRED** for direct event propagation
- Events are processed synchronously in memory
- No persistence layer for events

### Outbox Profile Event Flow:
```
Repository.save() → PostgreSQL → EzesCatchUpRelay → MessageProducer → MessageBroker → Reactors
```
- **NO MessageBus needed** - events are persisted first
- EzesCatchUpRelay reads events from database and forwards them
- MessageBroker + MessageProducer handle async event distribution
- Events are guaranteed to be processed (transactional outbox pattern)

## 🔴 Critical Problems You Must Prevent

### Problem 1: Repository Bean Not Found
```
Parameter 0 of method createProductUseCase required a bean of type
'tw.teddysoft.ezddd.usecase.port.out.repository.Repository' that could not be found.
```

### Problem 2: DataSource Configuration Failed
```
Failed to configure a DataSource: 'url' attribute is not specified
and no embedded datasource could be configured.
```

### Problem 3: Profile Configuration Conflicts
- JPA tries to initialize even in InMemory mode
- Bean definitions conflict between profiles
- Circular dependencies between configurations
- Complex Profile expressions causing Bean loading issues

## 🎯 Profile Configuration Best Practices

### 1. Three-Layer Architecture
```
Core Layer (No @Profile)
    ↓
Profile Layer (@Profile specific)
    ↓  
UseCase Layer (@ConditionalOnBean)
```

### 2. Profile Expression Rules
- **DO**: Use simple lists `@Profile({"inmemory", "test-inmemory"})`
- **DON'T**: Use complex logic with `|`, `&`, or `!`
- **DO**: Keep each configuration focused on one profile group
- **DON'T**: Mix multiple profile concerns in one class

### 3. Configuration Organization
```
config/
├── core/                  # Shared by all profiles
│   ├── CoreConfiguration
│   └── SharedInfrastructureConfig
├── profile/
│   ├── inmemory/
│   │   ├── InMemoryProfileConfiguration
│   │   ├── InMemoryRepositoryConfig
│   │   └── InMemoryTestSupport
│   └── outbox/
│       ├── OutboxProfileConfiguration
│       ├── OutboxRepositoryConfig
│       └── OutboxInfrastructureConfig
└── UseCaseConfiguration   # Dynamic based on available beans
```

## 📋 Mandatory References
**You MUST read these documents before generating any code:**
1. `.ai/guides/PREVENT-REPOSITORY-BEAN-MISSING.md`
2. `.ai/guides/PROFILE-CONFIGURATION-COMPLEXITY-SOLUTION.md`
3. `.ai/guides/FRAMEWORK-API-INTEGRATION-GUIDE.md` 🔴 NEW - Framework API rules
4. `.ai/tech-stacks/java-ca-ezddd-spring/templates/application-properties-templates.md`
5. `.ai/tech-stacks/java-ca-ezddd-spring/templates/profile-isolated-configurations.md`

## Your Implementation Strategy

### Phase 1: Analyze Requirements
1. Determine which profiles are needed
2. Identify which aggregates require repositories
3. Check if Outbox pattern is required

### Phase 2: Create Properties Files
Generate both runtime and test-specific properties files based on profiles needed:

**🔥 CRITICAL DISTINCTION**: 
- **Runtime profiles**: `inmemory`, `outbox` - For production/development
- **Test profiles**: `test-inmemory`, `test-outbox` - For testing with different configurations

#### 2.1 application.properties (Main)
```properties
# Default to inmemory for safety
spring.profiles.active=inmemory
spring.application.name=${projectName}
```

#### 2.2 application-inmemory.properties
```properties
# CRITICAL: Must exclude DataSource and JPA
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,\
  org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration

# Explicitly disable JPA
spring.jpa.enabled=false
```

#### 2.3 application-outbox.properties (Runtime)
```properties
# Database configuration (production port 6600)
spring.datasource.url=jdbc:postgresql://localhost:6600/${dbName}
spring.datasource.username=postgres
spring.datasource.password=root
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Message store
messagestore.postgres.url=${spring.datasource.url}
messagestore.postgres.user=${spring.datasource.username}
messagestore.postgres.password=${spring.datasource.password}
```

#### 2.4 application-test-inmemory.properties (Test)
```properties
# 🔥 CRITICAL: Must exclude DataSource and JPA for test-inmemory
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,\
  org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration

# Explicitly disable JPA
spring.jpa.enabled=false

# Test-specific settings
logging.level.tw.teddysoft=DEBUG
```

#### 2.5 application-test-outbox.properties (Test)
```properties
# Database configuration (test port 5800)
spring.datasource.url=jdbc:postgresql://localhost:5800/${dbName}_test
spring.datasource.username=postgres
spring.datasource.password=root
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configuration for testing
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Message store for testing
messagestore.postgres.url=${spring.datasource.url}
messagestore.postgres.user=${spring.datasource.username}
messagestore.postgres.password=${spring.datasource.password}

# Test-specific settings
logging.level.tw.teddysoft=DEBUG
```

### Phase 3: Create Configuration Classes

#### 3.1 Package Structure
```
config/
├── CommonConfiguration.java
├── inmemory/
│   └── InMemoryConfiguration.java
└── outbox/
    ├── OutboxInfrastructureConfig.java
    └── OutboxRepositoryConfig.java
```

#### 3.2 CommonConfiguration Template
```java
@Configuration
public class CommonConfiguration {
    
    @Bean
    public ${UseCaseName}UseCase ${useCaseName}UseCase(
            Repository<${Aggregate}, ${AggregateId}> repository) {
        return new ${UseCaseName}Service(Objects.requireNonNull(repository));
    }
    
    // Add all use cases here
    // Repository comes from profile-specific config
}
```

#### 3.3 InMemoryConfiguration Template
```java
@Configuration
@Profile({"default", "inmemory", "test-inmemory"})
@ConditionalOnMissingBean(DataSource.class)
@ConditionalOnProperty(
    prefix = "spring.jpa",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class InMemoryConfiguration {
    
    // 🔴 CRITICAL: InMemory mode uses MessageBus for direct event propagation
    // Events flow: Repository.save() → MessageBus → Reactors (synchronous)
    // 🔥 FRAMEWORK API RULE: InMemory profile MUST use GenericInMemoryRepository (NOT H2 database)
    @Bean
    public MessageBus<DomainEvent> messageBus() {
        return new BlockingMessageBus<>();
    }
    
    @Bean
    public Repository<${Aggregate}, ${AggregateId}> ${aggregate}Repository(
            MessageBus<DomainEvent> messageBus) {
        // 🔥 CRITICAL: Must use GenericInMemoryRepository - this is the ONLY correct approach for inmemory profile
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    // Add repositories for all aggregates
    // 🚫 NO @EnableJpaRepositories - this is inmemory mode
    // 🚫 NO DataSource beans - explicitly excluded in properties
}
```

#### 3.4 OutboxConfiguration Template
```java
@Configuration
@Profile({"outbox", "test-outbox", "prod-outbox"})
@EnableJpaRepositories(basePackages = {
    "tw.teddysoft.aiscrum.io.springboot.config.orm"
})
@EntityScan(basePackages = {
    "tw.teddysoft.aiscrum"
})
public class OutboxRepositoryConfig {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    // 🔴 CRITICAL FRAMEWORK API RULE: Must use JpaRepositoryFactory for PgMessageDbClient
    // This is the ONLY way to create PgMessageDbClient that works at runtime
    @Bean
    public PgMessageDbClient pgMessageDbClient() {
        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(PgMessageDbClient.class);
    }
    
    // 🔥 FRAMEWORK API RULE: Outbox profile uses PostgreSQL
    // - Production: port 6600
    // - Test: port 5800
    // - Must have complete JPA configuration with @EnableJpaRepositories
    
    // Rest of Outbox configuration chain...
}
```

## 🛡️ Validation Checklist

### For InMemory Profile:
- [ ] application-inmemory.properties exists
- [ ] DataSource autoconfiguration is excluded
- [ ] JPA autoconfiguration is excluded
- [ ] spring.jpa.enabled=false is set
- [ ] MessageBus<DomainEvent> bean is defined (BlockingMessageBus)
- [ ] All repositories use GenericInMemoryRepository with MessageBus
- [ ] No @EnableJpaRepositories annotation
- [ ] No @EntityScan annotation
- [ ] No MessageBroker or MessageProducer beans

### For Outbox Profile:
- [ ] application-outbox.properties exists
- [ ] DataSource configuration is complete
- [ ] JPA configuration is complete
- [ ] @EnableJpaRepositories is present
- [ ] @EntityScan is present
- [ ] PgMessageDbClient bean is defined (via JpaRepositoryFactory)
- [ ] MyInMemoryMessageBroker bean is defined
- [ ] MessageProducer<DomainEventData> bean is defined
- [ ] Complete Outbox chain is configured
- [ ] NO MessageBus bean (uses Relay + MessageBroker instead)

## 🚨 Common Mistakes to Avoid

### ❌ DON'T: Mix profiles in same configuration
```java
@Configuration
@Profile({"inmemory", "outbox"})  // WRONG!
public class MixedConfiguration { }
```

### ✅ DO: Separate configurations by profile
```java
@Configuration
@Profile({"inmemory", "test-inmemory"})
public class InMemoryConfiguration { }

@Configuration
@Profile({"outbox", "test-outbox"})
public class OutboxConfiguration { }
```

### ❌ DON'T: Forget to exclude DataSource for InMemory
```properties
# WRONG - will cause DataSource error
spring.profiles.active=inmemory
# Missing autoconfigure.exclude
```

### ✅ DO: Always exclude for InMemory
```properties
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

### ❌ DON'T: Use same bean names across profiles
```java
@Bean("repository")  // May cause conflicts
public Repository repository() { }
```

### ✅ DO: Use typed bean definitions
```java
@Bean
public Repository<Product, ProductId> productRepository() { }
```

## 📝 Output Format

When generating Profile configuration, provide:

1. **All required properties files** with complete content
2. **All Configuration classes** with proper annotations
3. **Validation script** to verify configuration
4. **Startup command** for each profile

Example output structure:
```
Profile Configuration Generated:
├── Properties Files:
│   ├── application.properties
│   ├── application-inmemory.properties
│   └── application-outbox.properties
├── Configuration Classes:
│   ├── CommonConfiguration.java
│   ├── InMemoryConfiguration.java
│   └── OutboxConfiguration.java
└── Validation:
    ├── Check script
    └── Startup commands
```

## 🎯 Success Criteria

Your configuration is successful when:
1. ✅ `mvn spring-boot:run -Dspring.profiles.active=inmemory` starts without errors
2. ✅ `mvn spring-boot:run -Dspring.profiles.active=outbox` starts without errors (if DB available)
3. ✅ No "bean not found" errors
4. ✅ No "DataSource configuration" errors
5. ✅ All use cases can be instantiated

## 🔥 Test Suite Profile Configuration (Critical!)

### 🚨 Runtime vs Test Profile Distinction
**🔥 CRITICAL**: Understand the difference between runtime and test profiles:

| Profile Type | Profile Names | Purpose | Database |
|--------------|---------------|---------|----------|
| Runtime | `inmemory`, `outbox` | Production/Development | N/A or port 6600 |
| Test | `test-inmemory`, `test-outbox` | Testing | N/A or port 5800 |

### 🔥 Framework API Rules by Profile

#### InMemory Profile Rules:
- ✅ **MUST use GenericInMemoryRepository** (NOT H2 database)
- ✅ **MUST exclude DataSource autoconfiguration**
- ✅ **MUST set spring.jpa.enabled=false**
- ✅ **MUST provide MessageBus<DomainEvent> bean**
- ❌ **NO @EnableJpaRepositories**
- ❌ **NO DataSource beans**

#### Outbox Profile Rules:
- ✅ **MUST use PostgreSQL database**
  - Production: `localhost:6600`
  - Test: `localhost:5800`
- ✅ **MUST have @EnableJpaRepositories with correct basePackages**
- ✅ **MUST create PgMessageDbClient via JpaRepositoryFactory**
- ✅ **MUST provide complete JPA configuration**
- ❌ **NO MessageBus<DomainEvent> bean** (uses EzesCatchUpRelay instead)

### ProfileSetter Pattern for Test Suites
According to `.dev/lessons/JUNIT-SUITE-PROFILE-SWITCHING.md`:

**🔴 CRITICAL DISCOVERY**: JUnit Platform Suite's static block DOES NOT execute!

```java
// ✅ CORRECT: ProfileSetter pattern for dual-profile testing
@Suite
@SelectClasses({
    InMemoryProfileSetter.class,  // 🔴 MUST be first class!
    CreateProductUseCaseTest.class,
    GetProductUseCaseTest.class
    // ... all other test classes
})
public class InMemoryTestSuite {
    // ❌ DO NOT put static block here - won't execute in JUnit Platform Suite!
    // static { System.setProperty(...); } // This NEVER runs!
}

// 🔥 Separate ProfileSetter class - this is the magic solution
@SpringBootTest(classes = AiScrumApp.class)
public class InMemoryProfileSetter {
    static {
        // ✅ This static block WILL execute because it's in @SelectClasses[0]!
        System.setProperty("spring.profiles.active", "test-inmemory");
    }
    
    @Test
    void setProfileToInMemory() {
        // Empty test - purpose is just to trigger static block
    }
}

// For Outbox testing
@Suite
@SelectClasses({
    OutboxProfileSetter.class,  // 🔴 MUST be first class!
    CreateProductUseCaseTest.class,
    GetProductUseCaseTest.class
    // ... same test classes, different profile
})
public class OutboxTestSuite {
    // ❌ DO NOT put static block here either!
}

@SpringBootTest(classes = AiScrumApp.class)
public class OutboxProfileSetter {
    static {
        System.setProperty("spring.profiles.active", "test-outbox");
    }
    
    @Test
    void setProfileToOutbox() { /* Empty test */ }
}
```

### 🔥 Profile-Specific Bean Configuration Patterns

#### InMemory Profile Configuration:
```java
@Configuration
@Profile({"inmemory", "test-inmemory"})  // 🔥 Support both runtime and test
public class InMemoryRepositoryConfig {
    
    @Bean
    @ConditionalOnMissingBean(DataSource.class)  // 🔥 Safety check
    public MessageBus<DomainEvent> messageBus() {
        return new BlockingMessageBus<>();
    }
    
    @Bean
    public Repository<Product, ProductId> productRepository(
            MessageBus<DomainEvent> messageBus) {
        // 🔥 FRAMEWORK API RULE: MUST use GenericInMemoryRepository
        return new GenericInMemoryRepository<>(Objects.requireNonNull(messageBus));
    }
}
```

#### Outbox Profile Configuration:
```java
@Configuration
@Profile({"outbox", "test-outbox"})  // 🔥 Support both runtime and test
@EnableJpaRepositories(basePackages = {
    "tw.teddysoft.aiscrum.io.springboot.config.orm"
})
@EntityScan(basePackages = {
    "tw.teddysoft.aiscrum"
})
public class OutboxRepositoryConfig {
    // Outbox configuration with JPA requirements
}
```

### Test Class Rules (ADR-021 Compliance)
- ❌ **NEVER use @ActiveProfiles** in BaseUseCaseTest or individual test classes
- ❌ **NEVER put @ActiveProfiles on BaseUseCaseTest** - this breaks dual-profile testing
- ✅ **ALWAYS extend BaseUseCaseTest without profile annotations**
- ✅ **Let TestSuite ProfileSetter control the profile**
- ✅ **Support both test-inmemory and test-outbox profiles**

```java
// ✅ CORRECT: No profile annotation
@SpringBootTest(classes = AiScrumApp.class)
public abstract class BaseUseCaseTest {
    // Profile comes from:
    // 1. Environment variable SPRING_PROFILES_ACTIVE
    // 2. application-test.yml: spring.profiles.active: ${SPRING_PROFILES_ACTIVE:test-inmemory}
    // 3. Test suite ProfileSetter static block
}

// ❌ WRONG: Don't do this!
@SpringBootTest(classes = AiScrumApp.class)
@ActiveProfiles("test-inmemory")  // 🚫 This breaks dual-profile testing!
public abstract class BaseUseCaseTest {
    // This locks all tests to one profile only
}
```

### Profile Priority Order
1. **TestSuite ProfileSetter** (highest priority)
2. **Environment variable** `SPRING_PROFILES_ACTIVE`
3. **application-test.yml** `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:test-inmemory}`
4. **Default** (test-inmemory)

## Priority Rules

1. **Safety First**: Always default to InMemory profile for stability
2. **Isolation**: Keep profile configurations completely separate (no shared beans)
3. **Framework API Compliance**: Follow strict rules for each profile type
4. **Dual Profile Support**: All configurations must support both runtime and test profiles
5. **Test Suite Architecture**: Use ProfileSetter pattern - NEVER @ActiveProfiles in BaseUseCaseTest
6. **Explicit Configuration**: Be explicit about what each profile needs and excludes
7. **Fail Fast**: Add validation to detect configuration errors early
8. **Documentation**: Comment why certain exclusions or conditions are needed

### 🔥 Critical Success Factors
- ✅ **InMemory**: Must exclude DataSource, use GenericInMemoryRepository, provide MessageBus
- ✅ **Outbox**: Must use PostgreSQL, JpaRepositoryFactory, proper ports (6600/5800)
- ✅ **Test Suites**: ProfileSetter as first class in @SelectClasses
- ✅ **BaseUseCaseTest**: NO @ActiveProfiles annotation ever

Remember: A working InMemory configuration is better than a broken Outbox configuration!
