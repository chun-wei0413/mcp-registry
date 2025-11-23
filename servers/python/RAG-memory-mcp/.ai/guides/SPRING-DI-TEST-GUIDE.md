# Spring Dependency Injection Test Guide

## 問題背景

許多測試直接使用 `new` 創建物件，而不是使用 Spring DI，這導致：
1. 測試無法支援多個 Profile（test-inmemory vs test-outbox）
2. 測試與實際運行環境不一致
3. 無法利用 Spring 的測試功能（事務回滾、MockBean 等）

## 正確的測試架構

### 1. 使用 @SpringBootTest

```java
@SpringBootTest
@ActiveProfiles("test-inmemory")  // 或由環境變數控制
@EzFeature
@EzFeatureReport
public class CreateProductUseCaseTest {
    
    @Autowired
    private CreateProductUseCase createProductUseCase;
    
    @Autowired
    private Repository<Product, ProductId> productRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    // 測試方法...
}
```

### 2. 使用 @TestConfiguration 提供測試專用 Bean

```java
@TestConfiguration
public class TestUseCaseConfiguration {
    
    @Bean
    @Profile("test-inmemory")
    public CreateProductUseCase createProductUseCaseInMemory(
            Repository<Product, ProductId> repository) {
        return new CreateProductService(repository);
    }
    
    @Bean
    @Profile("test-outbox")
    public CreateProductUseCase createProductUseCaseOutbox(
            Repository<Product, ProductId> repository,
            OutboxRepository outboxRepository) {
        return new CreateProductService(repository, outboxRepository);
    }
}
```

### 3. Profile-aware 測試基類

```java
@SpringBootTest
public abstract class BaseUseCaseTest {
    
    @Value("${spring.profiles.active:test-inmemory}")
    protected String activeProfile;
    
    @Autowired
    protected ApplicationContext context;
    
    protected boolean isInMemoryProfile() {
        return activeProfile.contains("inmemory");
    }
    
    protected boolean isOutboxProfile() {
        return activeProfile.contains("outbox");
    }
}
```

## 遷移指南

### Step 1: 移除 Manual TestContext

❌ **錯誤做法**：
```java
static class TestContext {
    private Repository<Product, ProductId> productRepository;
    
    public static void reset() {
        getInstance().productRepository = new GenericInMemoryRepository<>(...);
    }
}
```

✅ **正確做法**：
```java
@Autowired
private Repository<Product, ProductId> productRepository;

@BeforeEach
void setUp() {
    // 使用 Spring 管理的 repository
    // 不需要手動創建
}
```

### Step 2: 使用 @DirtiesContext 處理狀態重置

```java
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class CreateProductUseCaseTest {
    // 每個測試方法後重置 Spring Context
}
```

### Step 3: 使用 @MockBean 模擬依賴

```java
@SpringBootTest
public class CreateProductUseCaseTest {
    
    @MockBean
    private MessageBus<DomainEvent> messageBus;
    
    @Test
    void testProductCreation() {
        // messageBus 會被自動注入為 mock
    }
}
```

## 測試 Profile 配置

### application-test.yml
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:test-inmemory}

---
spring:
  config:
    activate:
      on-profile: test-inmemory
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration

---
spring:
  config:
    activate:
      on-profile: test-outbox
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
```

## 檢查清單

- [ ] 所有 UseCase 測試使用 @SpringBootTest
- [ ] 不直接 new repository 或 service
- [ ] 使用 @Autowired 注入依賴
- [ ] 支援 test-inmemory 和 test-outbox 兩個 profile
- [ ] 使用 @TestConfiguration 提供測試 bean
- [ ] 沒有 static TestContext 內部類別

## 自動檢查

執行以下腳本檢查測試是否符合 Spring DI 規範：
```bash
.ai/scripts/check-test-spring-di.sh
```

## 常見錯誤

### 1. 硬編碼 Repository
```java
// ❌ 錯誤
productRepository = new GenericInMemoryRepository<>(messageBus);

// ✅ 正確
@Autowired
private Repository<Product, ProductId> productRepository;
```

### 2. 手動創建 Service
```java
// ❌ 錯誤
CreateProductUseCase useCase = new CreateProductService(repository);

// ✅ 正確
@Autowired
private CreateProductUseCase createProductUseCase;
```

### 3. 忽略 Profile 差異
```java
// ❌ 錯誤 - 只支援 InMemory
new InMemoryProductsProjection(repository);

// ✅ 正確 - 根據 Profile 注入不同實作
@Autowired
private ProductsProjection productsProjection;
```