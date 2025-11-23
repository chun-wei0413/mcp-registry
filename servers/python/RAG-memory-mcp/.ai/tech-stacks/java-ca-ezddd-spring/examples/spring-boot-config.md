# Spring Boot 配置範例

## 基本配置

### 1. Application 主類
```java
package [rootPackage];

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2. 基礎設施配置 (InfrastructureConfig)
```java
package [rootPackage].config;

import [rootPackage].adapter.out.repository.GenericInMemoryRepository;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class InfrastructureConfig {
    
    // 開發環境使用 BlockingMessageBus (ezddd 框架內建)
    @Bean
    @Profile({"dev", "test"})
    public MessageBus<DomainEvent> messageBus() {
        return new BlockingMessageBus();
    }
    
    // 生產環境可能使用其他實現，如 RabbitMQ、Kafka 等
    @Bean
    @Profile("prod")
    public MessageBus<DomainEvent> productionMessageBus() {
        // 返回生產環境的 MessageBus 實現
        throw new UnsupportedOperationException("Production MessageBus not implemented");
    }
}
```

### 3. Repository 配置
```java
package [rootPackage].config;

import [rootPackage].adapter.out.repository.GenericInMemoryRepository;
import [rootPackage].entity.aggregate.plan.Plan;
import [rootPackage].entity.aggregate.plan.PlanId;
import [rootPackage].entity.aggregate.user.User;
import [rootPackage].entity.aggregate.user.UserId;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class RepositoryConfig {
    
    // 開發/測試環境使用 InMemory Repository
    @Bean
    @Profile({"dev", "test"})
    public Repository<Plan, PlanId> planRepository(MessageBus<DomainEvent> messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    @Bean
    @Profile({"dev", "test"})
    public Repository<User, UserId> userRepository(MessageBus<DomainEvent> messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    // 生產環境使用真實的 Repository 實現
    @Bean
    @Profile("prod")
    public Repository<Plan, PlanId> productionPlanRepository(MessageBus<DomainEvent> messageBus) {
        // 返回生產環境的 Repository 實現，如 JPA、MongoDB 等
        throw new UnsupportedOperationException("Production PlanRepository not implemented");
    }
}
```

### 4. Use Case 配置
```java
package [rootPackage].config;

import [rootPackage].plan.usecase.createplan.CreatePlanService;
import [rootPackage].plan.usecase.createplan.CreatePlanUseCase;
import [rootPackage].entity.aggregate.plan.Plan;
import [rootPackage].entity.aggregate.plan.PlanId;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
    
    @Bean
    public CreatePlanUseCase createPlanUseCase(
            Repository<Plan, PlanId> planRepository) {
        return new CreatePlanService(planRepository);
    }
    
    // 其他 Use Case 的配置...
}
```

### 5. Event Sourcing 配置 (如果使用)
```java
package [rootPackage].config;

import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;
import tw.teddysoft.ezddd.usecase.port.in.eventsourcing.EventSourcingUseCase;
import tw.teddysoft.ezddd.adapter.in.rest.eventsourcing.EventSourcingController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ezddd.event-sourcing.enabled", havingValue = "true")
public class EventSourcingConfig {
    
    @Bean
    public EventSourcingController eventSourcingController(
            EventSourcingUseCase eventSourcingUseCase) {
        return new EventSourcingController(eventSourcingUseCase);
    }
    
    // Bootstrap 配置用於註冊事件處理器
    @Bean
    public BootstrapConfig bootstrapConfig() {
        BootstrapConfig config = new BootstrapConfig();
        
        // 註冊事件類型
        config.registerEvent(PlanCreated.class);
        config.registerEvent(TaskCreated.class);
        config.registerEvent(UserRegistered.class);
        
        // 註冊事件處理器
        config.registerEventHandler(new PlanProjectionHandler());
        config.registerEventHandler(new UserProjectionHandler());
        
        return config;
    }
}
```

### 6. 測試專用配置
```java
package [rootPackage].config;

import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {
    
    // 測試時使用 BlockingMessageBus 進行同步測試 (ezddd 框架內建)
    @Bean
    @Primary
    public MessageBus<DomainEvent> messageBus() {
        return new BlockingMessageBus();
    }
}
```

## application.properties 配置

### application.properties
```properties
spring.application.name=ez-ddd-application
spring.profiles.active=dev

# Event Sourcing 配置
ezddd.event-sourcing.enabled=true
```

### application-dev.properties
```properties
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=create-drop

logging.level.root=INFO
logging.level.[rootPackage]=DEBUG
```

### application-test.properties
```properties
spring.jpa.hibernate.ddl-auto=create-drop

logging.level.root=WARN
logging.level.[rootPackage]=DEBUG
```

## 使用範例

### 1. 在 Controller 中使用
```java
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {
    private final CreatePlanUseCase createPlanUseCase;
    
    public PlanController(CreatePlanUseCase createPlanUseCase) {
        this.createPlanUseCase = createPlanUseCase;
    }
    
    @PostMapping
    public ResponseEntity<CqrsOutput<CreatePlanOutput>> createPlan(
            @RequestBody CreatePlanInput input) {
        var output = createPlanUseCase.execute(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }
}
```

### 2. 在整合測試中使用
```java
@SpringBootTest
@AutoConfigureMockMvc
class PlanIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private MessageBus<DomainEvent> messageBus;
    
    private List<DomainEvent> publishedEvents = new ArrayList<>();
    
    @BeforeEach
    void setUp() {
        // Register a reactor to capture domain events
        messageBus.register(event -> {
            if (event instanceof DomainEvent) {
                publishedEvents.add((DomainEvent) event);
            }
        });
    }
    
    @Test
    void should_create_plan_and_publish_event() throws Exception {
        // Given
        var request = """
            {
                "name": "Q1 Planning",
                "userId": "user123"
            }
            """;
        
        // When
        mockMvc.perform(post("/api/v1/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated());
        
        // Then
        assertThat(messageBus.getPublishedEvents()).hasSize(1);
        assertThat(messageBus.getPublishedEvents().get(0))
            .isInstanceOf(PlanCreated.class);
    }
}
```

## 重要提醒

1. **BlockingMessageBus 僅用於開發和測試**
   - 不適合生產環境
   - 生產環境應使用真正的消息隊列

2. **GenericInMemoryRepository 僅用於開發和測試**
   - 資料不會持久化
   - 重啟應用會遺失所有資料

3. **Profile 配置**
   - `dev`: 開發環境
   - `test`: 測試環境
   - `prod`: 生產環境

4. **依賴注入**
   - 使用構造函數注入
   - 避免使用 @Autowired 在字段上