# Profile-Based Testing Architecture Guide

## 📋 Overview

本專案採用 Profile-Based Testing 架構，允許測試在不同的 Repository 實作間切換，支援多種持久化策略而不需修改測試代碼。

## 🎯 核心理念

**Write Once, Test Everywhere**: 測試一次編寫，可在所有 Repository 實作上執行。

```
┌─────────────────┐
│  Use Case Test  │
│   (ezSpec BDD)  │
└────────┬────────┘
         │ extends
┌────────▼────────┐
│ BaseUseCaseTest │
└────────┬────────┘
         │ uses
┌────────▼────────────────────────────────────┐
│     Spring Profile-Based DI                 │
├──────────────┬──────────────┬──────────────┤
│ test-inmemory│ test-outbox  │ test-esdb    │
├──────────────┼──────────────┼──────────────┤
│ InMemory     │ PostgreSQL   │ EventStore   │
│ Repository   │ + Outbox     │ DB           │
└──────────────┴──────────────┴──────────────┘
```

## 🔧 支援的 Profiles

### 1. test-inmemory (預設)
- **用途**: 快速單元測試，開發時使用
- **特點**: 記憶體內儲存，無需外部依賴
- **速度**: 最快
- **配置**: `application-test-inmemory.yml`

### 2. test-outbox
- **用途**: Outbox Pattern 整合測試
- **特點**: 真實 PostgreSQL 資料庫，測試事務性
- **速度**: 中等
- **配置**: `application-test-outbox.yml`
- **需求**: PostgreSQL on localhost:5800

### 3. test-esdb (計畫中)
- **用途**: Event Sourcing 測試
- **特點**: EventStore DB 整合
- **速度**: 較慢
- **配置**: `application-test-esdb.yml`

### 4. test-ezes (計畫中)
- **用途**: EZES Event Sourcing 測試
- **特點**: EZES 資料庫整合
- **速度**: 中等
- **配置**: `application-test-ezes.yml`

## 📝 測試編寫指南

### Step 1: 繼承 BaseUseCaseTest

```java
@EzFeature
@EzFeatureReport
public class CreateProductUseCaseTest extends BaseUseCaseTest {
    // 測試實作
}
```

### Step 2: 使用 Spring DI

```java
@Autowired
private CreateProductUseCase createProductUseCase;

@Autowired
private Repository<Product, ProductId> productRepository;
```

### Step 3: 編寫 ezSpec BDD 測試

```java
@EzScenario
public void should_create_product_successfully() {
    feature.newScenario("Create product with valid input")
        .Given("valid product creation input", env -> {
            CreateProductInput input = CreateProductInput.create();
            input.productId = "product-123";
            input.name = "Test Product";
            env.put("input", input);
        })
        .When("create product use case is executed", env -> {
            CreateProductInput input = env.get("input", CreateProductInput.class);
            CqrsOutput output = createProductUseCase.execute(input);
            env.put("output", output);
        })
        .Then("product should be created successfully", env -> {
            CqrsOutput output = env.get("output", CqrsOutput.class);
            assertEquals(ExitCode.SUCCESS, output.getExitCode());
            
            // 驗證事件
            List<DomainEvent> events = getCapturedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof ProductCreated);
        })
        .Execute();
}
```

## 🚀 執行測試

### 方式 1: Maven Profiles（推薦）

```bash
# 使用預設 profile (test-inmemory)
mvn test

# 執行 InMemory 測試
mvn test -Ptest-inmemory

# 執行 Outbox 測試（需要 PostgreSQL on port 5800）
mvn test -Ptest-outbox
```

### 方式 2: Test Suites with Maven

```bash
# 執行 InMemory Test Suite
mvn test -Dtest=InMemoryTestSuite

# 執行 Outbox Test Suite（需要先用 Maven profile）
mvn test -Ptest-outbox
# 或
mvn test -Dtest=OutboxTestSuite -Dspring.profiles.active=test-outbox
```

### 方式 3: 單一測試與 profile

```bash
# 執行特定測試與 profile
mvn test -Dtest=CreateProductUseCaseTest -Dspring.profiles.active=test-outbox
```

### 方式 4: Test Suites 設計

```java
@Suite
@SelectPackages("tw.teddysoft.aiscrum")
@IncludeClassNamePatterns(".*UseCaseTest")
public class OutboxTestSuite {
    static {
        // 使用 static initializer 確保在 Spring context 初始化前設定 profile
        System.setProperty("spring.profiles.active", "test-outbox");
    }
}
```

### 方式 5: IDE 配置

IntelliJ IDEA:
1. Run Configuration → Environment Variables
2. 新增 `SPRING_PROFILES_ACTIVE=test-outbox`

VS Code:
1. `.vscode/launch.json`
2. 新增 `"env": { "SPRING_PROFILES_ACTIVE": "test-outbox" }`

## 🔄 遷移現有測試

### Before (舊方式 - TestContext)

```java
public class CreateProductUseCaseTest {
    
    static class TestContext {
        private static TestContext instance;
        private InMemoryRepository<Product> repository;
        private MessageBus messageBus;
        private List<DomainEvent> publishedEvents;
        
        public static TestContext getInstance() {
            if (instance == null) {
                instance = new TestContext();
            }
            return instance;
        }
        
        public CreateProductUseCase newCreateProductUseCase() {
            return new CreateProductService(repository);
        }
    }
    
    @BeforeEach
    void setUp() {
        TestContext.reset();
    }
    
    @Test
    void test() {
        CreateProductUseCase useCase = TestContext.getInstance().newCreateProductUseCase();
        // ...
        List<DomainEvent> events = TestContext.getInstance().getPublishedEvents();
    }
}
```

### After (新方式 - Spring DI)

```java
@EzFeature
@EzFeatureReport
public class CreateProductUseCaseTest extends BaseUseCaseTest {
    
    @Autowired
    private CreateProductUseCase createProductUseCase;
    
    // ⚠️ 注意：不應直接注入 Repository
    // Use Case 測試應該只透過 Use Case interface 進行測試
    
    @BeforeEach
    void setUp() {
        // Event capture 自動處理
    }
    
    @EzScenario
    public void test() {
        // 直接使用注入的 useCase
        var output = createProductUseCase.execute(input);
        // ...
        List<DomainEvent> events = getCapturedEvents();
    }
}
```

## 🎯 Base Classes 架構

### BaseSpringBootTest
- 提供 Spring Boot 測試環境
- 配置 Profile 切換機制
- 設定基本 Spring properties

### BaseUseCaseTest
- 繼承 BaseSpringBootTest
- 提供事件捕獲機制
- 提供測試輔助方法

### BaseControllerTest
- 繼承 BaseSpringBootTest
- 提供 MockMvc 設定
- 提供 REST 測試輔助

## 📊 測試覆蓋策略

```
Profile Coverage Matrix:

                 InMemory  Outbox  ESDB  EZES
Use Case Tests      ✅       ✅     🔄    🔄
Controller Tests    ✅       ✅     -     -
Integration Tests   -        ✅     ✅    ✅
E2E Tests          -        ✅     -     -
```

## ⚠️ 注意事項

### 1. 不要硬編碼 Repository 實作
```java
// ❌ 錯誤
@Autowired
private InMemoryRepository<Product> repository;

// ✅ 正確
@Autowired
private Repository<Product, ProductId> repository;
```

### 2. 不要使用 @ActiveProfiles
```java
// ❌ 錯誤
@ActiveProfiles("test-inmemory")
public class MyTest {

// ✅ 正確 - 讓 Profile 可動態切換
public class MyTest extends BaseUseCaseTest {
```

### 3. 事件驗證方法
```java
// ❌ 錯誤 (舊方式)
TestContext.getInstance().getPublishedEvents()

// ✅ 正確 (新方式)
getCapturedEvents()
```

## ⚠️ 重要限制：Use Case 測試階段性存取規範

### 核心規則
**Given 和 When 區塊不能直接使用 Repository，但 Then 和後續 And 區塊可以。**

### 規則說明
- **Given/When 階段**：設置和執行階段，必須透過 Use Case interface（模擬真實使用場景）
- **Then/And 階段**：驗證階段，可以直接查詢 Aggregate 狀態（深入驗證實作細節）

這樣的設計既保持測試的真實性（Given/When 模擬實際使用），又能完整驗證實作（Then/And 深入檢查）。

### ❌ 錯誤做法（Given/When 階段）
```java
// 錯誤：在 Given 直接創建 Aggregate
.Given("PBI exists", env -> {
    ProductBacklogItem pbi = new ProductBacklogItem(...);  // ❌
    pbiRepository.save(pbi);  // ❌
})

// 錯誤：在 When 直接操作 Aggregate
.When("changing description", env -> {
    ProductBacklogItem pbi = pbiRepository.findById(pbiId).orElseThrow();  // ❌
    pbi.changeDescription("New");  // ❌
    pbiRepository.save(pbi);  // ❌
})
```

### ✅ 正確做法
```java
// Given：透過 Use Case 設置
.Given("PBI exists", env -> {
    CreateProductBacklogItemInput input = CreateProductBacklogItemInput.create();
    input.pbiId = "pbi-1";
    createProductBacklogItemUseCase.execute(input);  // ✅ 透過 Use Case
})

// When：透過 Use Case 執行
.When("changing description", env -> {
    ChangeDescriptionInput input = ChangeDescriptionInput.create();
    input.newDescription = "Updated description";
    var output = changeDescriptionUseCase.execute(input);  // ✅ 透過 Use Case
    env.put("output", output);
})

// Then：可以直接驗證
.Then("operation succeeds", env -> {
    var output = env.get("output", CqrsOutput.class);
    assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
})

// And：可以直接查詢 Aggregate（Then 之後）
.And("PBI aggregate should have new description", env -> {
    @Autowired
    Repository<ProductBacklogItem, PbiId> pbiRepository;  // ✅ Then/And 可注入
    
    ProductBacklogItem pbi = pbiRepository.findById(pbiId).orElseThrow();  // ✅ 可直接查詢
    assertThat(pbi.getDescription()).isEqualTo("Updated description");  // ✅ 可直接驗證
})

// 也可以透過事件驗證
.And("event should be published", env -> {
    List<DomainEvent> publishedEvents = getCapturedEvents();
    ProductBacklogItemEvents.PbiDescriptionChanged event = findEvent(publishedEvents);
    assertThat(event.newDescription()).isEqualTo("Updated description");
})
```

### 階段規範總結表

| 測試階段 | 可否直接使用 Repository | 可否直接操作 Aggregate | 說明 |
|---------|------------------------|------------------------|------|
| Given   | ❌ 禁止 | ❌ 禁止 | 必須透過 Use Case 設置測試資料 |
| When    | ❌ 禁止 | ❌ 禁止 | 必須透過 Use Case 執行業務操作 |
| Then    | ✅ 允許 | ✅ 允許（查詢） | 可直接查詢驗證狀態 |
| And     | ✅ 允許（Then 之後） | ✅ 允許（查詢） | 可直接查詢驗證狀態 |

### 🚨 重要：事件清除的正確時機

在 Given 階段執行 Use Case 後若需要清除事件（例如只想測試 When 階段產生的事件），**必須先等待事件被捕獲再清除**。

#### ❌ 錯誤做法（競態條件風險）
```java
.Given("product exists", env -> {
    CreateProductInput input = CreateProductInput.create();
    input.id = UUID.randomUUID().toString();
    input.name = "Test Product";
    
    createProductUseCase.execute(input);
    clearCapturedEvents();  // ❌ 危險！事件可能還在傳遞中
})
```

#### ✅ 正確做法
```java
.Given("product exists", env -> {
    CreateProductInput input = CreateProductInput.create();
    input.id = UUID.randomUUID().toString();
    input.name = "Test Product";
    
    createProductUseCase.execute(input);
    
    // 等待事件被捕獲（假設創建會產生 1 個事件）
    await().untilAsserted(() -> 
        assertEquals(1, fakeEventListener.capturedEvents.size())
    );
    
    // 確認事件已被捕獲後才清除
    clearCapturedEvents();
})
```

#### 為什麼這很重要？
1. **事件發布是異步的**：execute() 方法返回不代表事件已經被 EventListener 捕獲
2. **避免間歇性失敗**：直接清除可能導致測試有時成功有時失敗
3. **確保測試穩定性**：等待機制確保測試行為一致可預測

#### 最佳實踐
- 在 Given 階段執行任何會產生事件的 Use Case 後，都要 await
- 使用明確的事件數量檢查（如 `assertEquals(1, ...)`）
- 只在確實需要隔離 When 階段事件時才清除 Given 階段的事件

## 🔍 疑難排解

### 問題 1: ApplicationContext 載入失敗
**原因**: Profile 配置檔案缺失或設定錯誤
**解決**: 檢查 `src/test/resources/application-test-{profile}.yml`

### 問題 2: Repository 注入失敗
**原因**: Profile 對應的 Configuration 未正確設定
**解決**: 檢查 `TestInMemoryConfiguration` 或 `TestOutboxConfiguration`

### 問題 3: 事件未被捕獲
**原因**: 未繼承 BaseUseCaseTest
**解決**: 確保測試類別繼承 BaseUseCaseTest

### 問題 4: OutboxTestSuite 執行時 profile 未切換
**原因**: Spring Boot context 在 JUnit @BeforeAll 之前初始化
**解決**: 
1. 使用 Maven profile: `mvn test -Ptest-outbox`
2. 或在 Test Suite 使用 static initializer 而非 @BeforeAll
3. 或直接指定: `mvn test -Dtest=OutboxTestSuite -Dspring.profiles.active=test-outbox`

## 📚 相關文件

- [測試編碼規範](tech-stacks/java-ca-ezddd-spring/coding-standards/test-standards.md)
- [BaseUseCaseTest 原始碼](../src/test/java/tw/teddysoft/aiscrum/test/base/BaseUseCaseTest.java)
- [Test Suite 設定](../src/test/java/tw/teddysoft/aiscrum/test/suite/)
- [ADR-025: Profile-Based Testing Architecture](../.dev/adr/ADR-025-profile-based-testing.md)

## 🚧 未來擴充

1. **ESDB Profile 實作** (需要時再實作)
   - EventStore DB 整合
   - Event Sourcing 測試支援

2. **EZES Profile 實作** (需要時再實作)
   - EZES Database 整合
   - 分散式事件處理測試

3. **測試資料自動化**
   - Profile-aware test data builders
   - 自動化測試資料清理

4. **效能測試 Profile**
   - 專門的效能測試配置
   - 大量資料測試支援