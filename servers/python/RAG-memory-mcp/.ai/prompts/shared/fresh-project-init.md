# Fresh Project Initialization Guide

## 🆕 When to Use This Guide
**For BRAND NEW projects (no existing Spring Boot app), follow these steps to initialize the basic infrastructure.**

**Note**: For complete profile configuration and advanced setup, delegate to the `profile-config-sub-agent`.

## Step 0: Check Project State
```bash
# Check if this is a fresh project - look for ANY main class
find . -name "*.java" -exec grep -l "@SpringBootApplication" {} \;
# If found, use existing; if not, create new
```

## Step 1: Create Spring Boot Application (if missing)
**CRITICAL: Check for existing main class first!**
- **Standard name**: `AiScrumApp.java` (not AiScrumApplication)
- **Standard location**: `tw.teddysoft.aiscrum` package (root)
- **Reference**: `.ai/prompts/shared/spring-boot-conventions.md`

```java
package tw.teddysoft.aiscrum;  // ROOT PACKAGE, NOT SUBPACKAGE!

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = "tw.teddysoft.aiscrum"
)
public class AiScrumApp {  // Use AiScrumApp, not AiScrumApplication
    public static void main(String[] args) {
        SpringApplication.run(AiScrumApp.class, args);
    }
}
```

## Step 2: Create Essential Common Classes (if missing)
**Read from `.ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md`:**
1. **DateProvider** - Unified date/time management
2. **GenericInMemoryRepository** - InMemory repository implementation
3. **MyInMemoryMessageBroker** - Event bus implementation
4. **MyInMemoryMessageProducer** - Message producer for outbox

## Step 3: Create Basic Application Configuration Files
```properties
# src/main/resources/application.properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:inmemory}
spring.application.name=aiscrum

# src/main/resources/application-inmemory.properties
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration

# src/test/resources/application-test.properties
spring.profiles.active=test-inmemory
```

## Step 4: Create Basic Spring Configuration Classes

### InMemoryRepositoryConfig.java
```java
@Configuration
@Profile({"inmemory", "test-inmemory"})
public class InMemoryRepositoryConfig {

    @Bean
    public MessageBus<DomainEvent> messageBus() {
        return new BlockingMessageBus<>();
    }

    @Bean
    public Repository<Product, ProductId> productRepository(MessageBus<DomainEvent> messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }

    // Add similar beans for other aggregates as needed
}
```

### UseCaseConfiguration.java
```java
@Configuration
public class UseCaseConfiguration {

    // UseCase beans
    @Bean
    public CreateProductUseCase createProductUseCase(Repository<Product, ProductId> productRepository) {
        return new CreateProductService(productRepository);
    }

    // Add other use case beans as needed
}
```

## ⚠️ IMPORTANT: Order of Execution
1. **ALWAYS** check if basic infrastructure exists FIRST
2. **NEVER** assume Spring Boot app or configs exist
3. **CREATE** missing infrastructure before implementing use case
4. **THEN** proceed with normal use case implementation

## 🔥 For Advanced Configuration
**For complete dual-profile support, outbox pattern, and test suite configuration:**
- Delegate to `profile-config-sub-agent` for comprehensive profile setup
- See `.ai/prompts/profile-config-sub-agent-prompt.md` for detailed configuration