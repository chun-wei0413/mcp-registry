# New Project Test Setup Guide

## 🎯 Purpose
This guide provides step-by-step instructions for setting up the testing infrastructure in a new project to support multi-profile testing (test-inmemory and test-outbox).

## 📋 Prerequisites
Before starting, ensure you have:
1. Spring Boot project initialized
2. ezddd framework dependencies added
3. Common utilities created (MyInMemoryMessageBroker, GenericInMemoryRepository, etc.)

## 🚀 Setup Steps

### Step 1: Create Base Test Classes

#### 1.1 Create BaseSpringBootTest
Copy from: `.ai/tech-stacks/java-ca-ezddd-spring/examples/test/BaseSpringBootTest.java`
Target: `src/test/java/tw/teddysoft/[project]/test/base/BaseSpringBootTest.java`

**Key Requirements:**
- NO @ActiveProfiles annotation
- Import test configuration classes
- Set @DirtiesContext for test isolation

#### 1.2 Create BaseUseCaseTest
Copy from: `.ai/tech-stacks/java-ca-ezddd-spring/examples/test/BaseUseCaseTest.java`
Target: `src/test/java/tw/teddysoft/[project]/test/base/BaseUseCaseTest.java`

**Key Components:**
- Event capture mechanism (FakeEventListener)
- Profile detection logic
- Database cleanup for outbox profile
- Helper methods for event assertions

### Step 2: Create Test Configuration Classes

#### 2.1 TestInMemoryRepositoryConfiguration
```java
package tw.teddysoft.[project].test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.[project].common.adapter.out.repository.GenericInMemoryRepository;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

@Configuration
@Profile("test-inmemory")
public class TestInMemoryRepositoryConfiguration {
    
    @Bean
    public Repository<Product, ProductId> productRepository(MessageBus<DomainEvent> messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    // Add other repository beans as needed
}
```

#### 2.2 TestOutboxRepositoryConfiguration
```java
package tw.teddysoft.[project].test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Import;
import tw.teddysoft.[project].io.springboot.config.OutboxRepositoryConfig;

@Configuration
@Profile("test-outbox")
@Import(OutboxRepositoryConfig.class)
public class TestOutboxRepositoryConfiguration {
    // Imports the production OutboxRepositoryConfig
    // Can add test-specific overrides if needed
}
```

### Step 3: Configure Test Properties

#### 3.1 Create application-test.yml
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:test-inmemory}
  
  datasource:
    url: jdbc:postgresql://localhost:5800/test_db
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false

aiscrum:
  test-data:
    enabled: false  # Disable test data initialization

logging:
  level:
    org.springframework.web: WARN
    org.hibernate: WARN
```

### Step 4: Create Test Suite Classes (Optional)

#### 4.1 InMemoryTestSuite
```java
package tw.teddysoft.[project].test.suite;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("InMemory Profile Test Suite")
@SelectClasses({
    CreateProductUseCaseTest.class,
    // Add other test classes
})
public class InMemoryTestSuite {
    static {
        // Set profile before Spring context loads
        System.setProperty("spring.profiles.active", "test-inmemory");
    }
}
```

#### 4.2 OutboxTestSuite
```java
package tw.teddysoft.[project].test.suite;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Outbox Profile Test Suite")
@SelectClasses({
    CreateProductUseCaseTest.class,
    // Add other test classes
})
public class OutboxTestSuite {
    static {
        // Set profile before Spring context loads
        System.setProperty("spring.profiles.active", "test-outbox");
    }
}
```

## 🧪 Writing Tests

### Example Use Case Test
```java
@SpringBootTest
@EzFeature
@EzFeatureReport
public class CreateProductUseCaseTest extends BaseUseCaseTest {
    
    @Autowired
    private CreateProductUseCase createProductUseCase;
    
    @Autowired
    private Repository<Product, ProductId> productRepository;
    
    @BeforeEach
    void setUp() {
        clearCapturedEvents();
    }
    
    @EzScenario
    public void create_product_successfully() {
        // Test implementation using await() for events
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            List<DomainEvent> events = getCapturedEvents();
            assertThat(events).hasSize(1);
        });
    }
}
```

## ⚠️ Common Pitfalls to Avoid

### ❌ DON'T Do This:
1. Add @ActiveProfiles to test classes
2. Create hardcoded repositories in tests
3. Use Thread.sleep() instead of await()
4. Forget to clear events between test scenarios
5. Mix test and production configurations

### ✅ DO This:
1. Let profile be determined by configuration
2. Use @Autowired for all dependencies
3. Use Awaitility for async assertions
4. Clear captured events in @BeforeEach
5. Keep test configurations separate

## 🔍 Verification Checklist

- [ ] BaseSpringBootTest created without @ActiveProfiles
- [ ] BaseUseCaseTest extends BaseSpringBootTest
- [ ] Event capture mechanism implemented
- [ ] Database cleanup logic for outbox profile
- [ ] Test configuration classes for both profiles
- [ ] application-test.yml with profile switching
- [ ] All tests use @Autowired (no hardcoded instances)
- [ ] Tests use await() for async event assertions

## 📚 Related Documents
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/test/` - Template files
- `.ai/prompts/test-generation-prompt.md` - Test generation rules
- `.ai/guides/PROFILE-BASED-TESTING-GUIDE.md` - Profile testing architecture
- `.dev/adr/ADR-021-profile-based-testing.md` - Architecture decision