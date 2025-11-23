# 測試編碼規範

本文件定義各層級測試的編碼標準，包含單元測試、Use Case 測試、Controller 測試和整合測試。

## 🔴 必須遵守的規則 (MUST FOLLOW)

### ⚠️ 測試資料 ID 規範（重要！）

**強制規定**: 所有測試中的聚合根 ID 必須使用 UUID.randomUUID().toString() 來避免測試間的 ID 衝突
**例外規則**: userId 和 creatorId 可以使用固定字串

#### 1. ID 使用規範
```java
// ✅ 正確：使用 UUID 產生唯一 ID
@EzScenario
public void should_create_product_successfully() {
    feature.newScenario()
        .Given("valid input", env -> {
            CreateProductInput input = CreateProductInput.create();
            input.id = UUID.randomUUID().toString();  // 使用 UUID
            input.name = "Test Product";
            env.put("productId", input.id);  // 儲存以供後續使用
        })
        .When("...", env -> { /* ... */ })
        .Then("...", env -> {
            String productId = env.gets("productId");  // 從環境取得 ID
            // ...
        })
        .Execute();
}

// ❌ 錯誤：使用固定的 ID（會造成測試失敗）
@EzScenario
public void should_create_product_successfully() {
    feature.newScenario()
        .Given("valid input", env -> {
            CreateProductInput input = CreateProductInput.create();
            input.id = "product-1";  // 錯誤！固定 ID 會造成重複
            input.id = "product-123";  // 錯誤！固定 ID 會造成重複
            // ...
        })
        .Execute();
}
```

#### 2. 多個測試案例的 ID 管理
```java
@EzScenario
public void should_handle_multiple_entities() {
    feature.newScenario()
        .Given("multiple products exist", env -> {
            // 每個實體都使用不同的 UUID
            String productId1 = UUID.randomUUID().toString();
            String productId2 = UUID.randomUUID().toString();
            
            CreateProductInput input1 = CreateProductInput.create();
            input1.id = productId1;
            
            CreateProductInput input2 = CreateProductInput.create();
            input2.id = productId2;
            
            env.put("productId1", productId1);
            env.put("productId2", productId2);
        })
        .Execute();
}
```

#### 3. 為什麼要使用 UUID？
- **避免 ID 衝突**: 測試可能並行執行或資料庫未清理，固定 ID 會造成重複錯誤
- **測試隔離性**: 每個測試有獨立的資料，不會互相影響
- **更真實的測試**: 生產環境通常也使用 UUID，測試更接近實際情況

#### 4. userId 和 creatorId 的特殊規則
```java
// ✅ 可以接受：userId 和 creatorId 可以使用固定字串
@EzScenario
public void should_create_task() {
    feature.newScenario()
        .Given("a task creation request", env -> {
            CreateTaskInput input = CreateTaskInput.create();
            input.taskId = UUID.randomUUID().toString();  // 實體 ID 必須用 UUID
            input.userId = "user-123";      // ✅ userId 可以用固定字串
            input.creatorId = "creator-1";  // ✅ creatorId 可以用固定字串
        })
        .Execute();
}
```

**原因**：
- userId 和 creatorId 是操作者身份，不是聚合根 ID
- 這些 ID 通常來自外部系統（如認證服務）
- 在測試中使用固定值有助於追蹤和除錯
- 不會造成聚合根的 ID 衝突問題

#### 測試 ID 檢查清單
- [ ] 所有聚合根 ID（如 productId, pbiId, sprintId, taskId）使用 UUID.randomUUID().toString()
- [ ] userId 和 creatorId 可以使用固定字串（如 "user-123", "creator-1"）
- [ ] ID 存入 env 變數供後續步驟使用
- [ ] 不使用固定的聚合根 ID（如 "product-1", "pbi-123"）
- [ ] 多個聚合根實體使用不同的 UUID

### ⚠️ DomainEventMapper 測試隔離規則 (ADR-024)

**強制規定**: 所有使用 DomainEventMapper 的測試必須確保測試隔離

#### 1. Mapper 測試規範
```java
// ✅ 正確：使用 BootstrapConfig.initialize()
@BeforeAll
static void setUp() {
    // 使用完整的 BootstrapConfig 初始化，確保所有映射都存在
    tw.teddysoft.aiscrum.io.springboot.config.BootstrapConfig.initialize();
}

@AfterAll
static void tearDown() {
    // 恢復完整的 BootstrapConfig 設定，確保不影響其他測試
    tw.teddysoft.aiscrum.io.springboot.config.BootstrapConfig.initialize();
}

// ❌ 錯誤：直接設定 DomainEventMapper
@BeforeAll
static void setUp() {
    DomainEventTypeMapper mapper = DomainEventTypeMapper.create();
    // 直接設定會影響全局狀態
    DomainEventMapper.setMapper(mapper);  // 錯誤！
}
```

#### 2. Outbox 整合測試規範
```java
// ✅ 正確：防禦性初始化
@BeforeEach
void ensureBootstrapConfigInitialized() {
    // 確保 BootstrapConfig 被初始化
    // 這是必要的，因為其他測試可能會重置 DomainEventMapper
    tw.teddysoft.aiscrum.io.springboot.config.BootstrapConfig.initialize();
}
```

#### 3. 事件映射規範
```java
// ✅ 正確：使用統一的 MAPPING_TYPE_PREFIX
public static final String SPRINT_CREATED = MAPPING_TYPE_PREFIX + "SprintCreated";
public static final String MEMBER_CAPACITY_SET = MAPPING_TYPE_PREFIX + "MemberCapacitySet";

// ❌ 錯誤：直接使用字串常量
mapper.put("MemberCapacitySet", MemberCapacitySet.class);  // 錯誤！缺少前綴
```

#### 測試隔離檢查清單
- [ ] Mapper 測試使用 BootstrapConfig.initialize() 而非直接設定 DomainEventMapper
- [ ] Mapper 測試在 @AfterAll 中恢復狀態
- [ ] Outbox 測試在 @BeforeEach 中防禦性初始化
- [ ] 所有事件映射使用統一的 MAPPING_TYPE_PREFIX
- [ ] 測試可以單獨執行也可以一起執行

### 1. Use Case 測試必須使用 ezSpec BDD 風格

**強制規定**: Use Case 測試必須使用 ezSpec，不得使用純 JUnit 風格

### 2. Entity 層測試可選擇測試框架

**彈性規定**: Entity 層（包含 Aggregate、Entity、Value Object）的單元測試可以選擇使用 JUnit 或 ezSpec

```java
// ✅ 可以使用 JUnit（適合簡單的單元測試）
@Test
void should_not_generate_event_when_estimating_with_same_value() {
    // Arrange
    ProductBacklogItem pbi = new ProductBacklogItem(...);
    
    // Act
    pbi.estimate(existingValue);
    
    // Assert
    assertTrue(pbi.getDomainEvents().isEmpty());
}

// ✅ 也可以使用 ezSpec（適合複雜的行為測試）
@EzScenario
void should_transition_state_correctly() {
    feature.newScenario()
        .Given("a backlogged PBI", env -> { ... })
        .When("PBI is selected for sprint", env -> { ... })
        .Then("state should be SELECTED", env -> { ... })
        .Execute();
}
```

**選擇指引**：
- **使用 JUnit**：簡單的狀態驗證、序列化測試、事件生成測試
- **使用 ezSpec**：複雜的狀態機測試、多步驟行為測試、業務規則驗證

### 3. Use Case 測試詳細規範

```java
// ✅ 正確：使用 ezSpec BDD 風格
@EzFeature
public class CreateProductUseCaseTest {
    
    static Feature feature = Feature.New("Create Product Use Case", 
        "As a user, I want to create a product");
    
    @Autowired
    private CreateProductUseCase useCase;
    
    @EzScenario
    public void should_create_product_successfully() {
        feature.newScenario("Successfully create a product with valid input")
            .Given("valid product creation input", env -> {
                CreateProductInput input = CreateProductInput.create();
                input.productId = UUID.randomUUID().toString();
                input.name = "Test Product";
                input.userId = UUID.randomUUID().toString();
                env.put("input", input);
            })
            .When("the use case is executed", env -> {
                CreateProductInput input = env.get("input", CreateProductInput.class);
                CqrsOutput<ProductDto> output = useCase.execute(input);
                env.put("output", output);
            })
            .Then("the product should be created successfully", env -> {
                CqrsOutput<ProductDto> output = env.get("output", CqrsOutput.class);
                CreateProductInput input = env.get("input", CreateProductInput.class);
                assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
                assertThat(output.getData().getProductId()).isEqualTo(input.productId);
                assertThat(output.getData().getName()).isEqualTo("Test Product");
            })
            .Execute();
    }
}

// ❌ 錯誤：使用純 JUnit 風格
@Test
public void testCreateProduct() {  // 錯誤！Use Case 測試禁止使用 @Test
    // ...
}
```

## 🎯 測試分層策略

### 1. 測試金字塔
```
         /\
        /E2E\      <- 最少 (5%)
       /------\
      /  整合  \    <- 適中 (20%)
     /----------\
    / Controller \  <- 較多 (25%)
   /--------------\
  /   Use Case    \ <- 多 (25%)
 /------------------\
/    單元測試        \ <- 最多 (25%)
----------------------
```

### 2. 各層測試職責

| 層級 | 測試內容 | 測試框架 | Mock 策略 |
|------|---------|---------|-----------|
| Unit Test | Domain logic, Value Objects | JUnit 5 | No mocks |
| Use Case Test | Business flow | ezSpec | Mock Repository |
| Controller Test | HTTP behavior | MockMvc + REST Assured | Mock Use Case |
| Integration Test | Database, External API | SpringBootTest | Real dependencies |
| E2E Test | Complete user journey | Selenium/Cypress | No mocks |

## 🎯 單元測試規範

### 1. Domain Object 測試
```java
class ProductTest {
    
    @Test
    void should_create_product_with_valid_input() {
        // Given
        ProductId id = ProductId.create();
        String name = "Test Product";
        UserId creatorId = UserId.valueOf("user-123");
        
        // When
        Product product = new Product(id, name, creatorId);
        
        // Then
        assertThat(product.getId()).isEqualTo(id);
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getCreatorId()).isEqualTo(creatorId);
        assertThat(product.getState()).isEqualTo(ProductState.CREATED);
    }
    
    @Test
    void should_throw_exception_when_name_is_null() {
        // Given
        ProductId id = ProductId.create();
        String name = null;
        UserId creatorId = UserId.valueOf("user-123");
        
        // When/Then
        assertThatThrownBy(() -> new Product(id, name, creatorId))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Product name");
    }
}
```

### 2. Value Object 測試
```java
class MoneyTest {
    
    @Test
    void should_add_money_with_same_currency() {
        // Given
        Money money1 = new Money(new BigDecimal("100"), Currency.TWD);
        Money money2 = new Money(new BigDecimal("50"), Currency.TWD);
        
        // When
        Money result = money1.add(money2);
        
        // Then
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("150"));
        assertThat(result.getCurrency()).isEqualTo(Currency.TWD);
    }
    
    @Test
    void should_throw_exception_when_adding_different_currencies() {
        // Given
        Money money1 = new Money(new BigDecimal("100"), Currency.TWD);
        Money money2 = new Money(new BigDecimal("50"), Currency.USD);
        
        // When/Then
        assertThatThrownBy(() -> money1.add(money2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("currency");
    }
}
```

## 🎯 Use Case 測試規範 (ezSpec)

### 🔴 Profile-Based Testing Architecture (重要！)

**目的**: 支援多種 Repository 實作切換，包含 InMemory、Outbox、ESDB (Event Sourcing)、EZES 等

#### 1. 基本架構 - 使用 BaseUseCaseTest
```java
// ✅ 正確：繼承 BaseUseCaseTest，支援 Profile 切換
@EzFeature
@EzFeatureReport
public class CreateProductUseCaseTest extends BaseUseCaseTest {
    
    static Feature feature = Feature.New("Create Product Use Case");
    
    // 使用 Spring @Autowired 注入，不要手動建立
    @Autowired
    private CreateProductUseCase createProductUseCase;
    
    @Autowired
    private Repository<Product, ProductId> productRepository;
    
    @BeforeEach
    void setUp() {
        // 使用 BaseUseCaseTest 提供的方法
        // Event capture 已在 BaseUseCaseTest.setUpEventCapture() 中處理
    }
    
    @EzScenario
    public void should_create_product_successfully() {
        // Given-When-Then
    }
}

// ❌ 錯誤：使用手動 TestContext（舊方式）
public class CreateProductUseCaseTest {
    static class TestContext {
        private InMemoryRepository repository;  // 錯誤！硬編碼特定實作
        // ...
    }
}
```

#### 2. 支援的 Profiles

| Profile | Repository 實作 | 用途 | 狀態 |
|---------|----------------|------|------|
| test-inmemory | InMemoryRepository | 快速單元測試 | ✅ 已實作 |
| test-outbox | OutboxRepository + PostgreSQL | Outbox Pattern 測試 | ✅ 已實作 |
| test-esdb | EventStore DB | Event Sourcing 測試 | 🔄 計畫中 |
| test-ezes | EZES Database | Event Sourcing 測試 | 🔄 計畫中 |

#### 3. Profile 切換方式

```java
// 方式 1: 透過 Test Suite
@Suite
@SelectPackages("tw.teddysoft.aiscrum")
@IncludeClassNamePatterns(".*UseCaseTest")
public class InMemoryTestSuite {
    @BeforeAll
    static void setupProfile() {
        System.setProperty("spring.profiles.active", "test-inmemory");
    }
}

// 方式 2: 執行時指定
mvn test -Dspring.profiles.active=test-outbox

// 方式 3: 環境變數
export SPRING_PROFILES_ACTIVE=test-esdb
mvn test
```

#### 4. 事件捕獲機制

##### ⚠️ 重要：清除事件前必須等待非同步發布完成

```java
// ✅ 正確：在 Given 中執行其他 Use Case 後要等待事件發布
@EzScenario
public void should_create_task_successfully() {
    feature.newScenario()
        .Given("a PBI exists", env -> {
            // 執行 Use Case 準備測試資料
            createProductBacklogItemUseCase.execute(createPbiInput);
            
            // ✅ 正確：等待事件發布完成再清除
            await().untilAsserted(() -> 
                assertThat(fakeEventListener.capturedEvents.size()).isGreaterThan(0)
            );
            clearCapturedEvents();
        })
        .When("...", env -> { /* ... */ })
        .Then("...", env -> { /* ... */ })
        .Execute();
}

// ❌ 錯誤：沒有等待就清除事件
@EzScenario
public void should_create_task_successfully() {
    feature.newScenario()
        .Given("a PBI exists", env -> {
            createProductBacklogItemUseCase.execute(createPbiInput);
            clearCapturedEvents();  // 錯誤！事件可能還沒發布完成
        })
        .When("...", env -> { /* ... */ })
        .Execute();
}
```

##### 事件驗證最佳實踐

```java
// ✅ 使用 BaseUseCaseTest 提供的方法
@EzScenario
public void should_publish_domain_event() {
    feature.newScenario()
        .Given("...", env -> { /* ... */ })
        .When("...", env -> { /* ... */ })
        .Then("event should be published", env -> {
            // 等待事件發布
            await().untilAsserted(() -> 
                assertEquals(1, fakeEventListener.capturedEvents.size())
            );
            
            // 使用 getCapturedEvents() 而非 getDomainEvents()
            List<DomainEvent> events = getCapturedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof ProductCreated);
        })
        .Execute();
}

// BaseUseCaseTest 提供的事件相關方法：
// - getCapturedEvents(): 取得所有捕獲的事件
// - clearCapturedEvents(): 清除已捕獲的事件（注意：呼叫前要 await）
// - getLastCapturedEvent(): 取得最後一個事件
// - getCapturedEventsOfType(Class<T>): 取得特定類型的事件
```

### 5. 遷移指南（從舊 TestContext 到新架構）

```java
// Step 1: 移除 TestContext inner class
// 舊代碼：
static class TestContext {
    private InMemoryRepository<Product> repository;
    private MessageBus messageBus;
    // ...
}

// Step 2: 改為繼承 BaseUseCaseTest
public class CreateProductUseCaseTest extends BaseUseCaseTest {

// Step 3: 使用 @Autowired 注入
@Autowired
private CreateProductUseCase useCase;

@Autowired
private Repository<Product, ProductId> repository;

// Step 4: 更新事件驗證
// 舊：TestContext.getInstance().getPublishedEvents()
// 新：getCapturedEvents()
```

### 6. 基本結構（保留向後相容）

### 2. 測試資料準備 (Query Use Case)
```java
@EzScenario
public void should_get_product_with_complete_data() {
    feature.newScenario("Get product with all related data")
        .Given("a product exists with complete data", env -> {
            // 準備完整的測試資料
            Product product = createCompleteProduct();
            ProductDto dto = ProductMapper.toDto(product);
            
            when(projection.findById("product-123"))
                .thenReturn(Optional.of(dto));
            
            env.put("expectedDto", dto);
        })
        .When("getting the product", env -> {
            GetProductInput input = GetProductInput.create();
            input.productId = "product-123";
            
            GetProductOutput output = useCase.execute(input);
            env.put("output", output);
        })
        .Then("should return complete product data", env -> {
            GetProductOutput output = env.get("output", GetProductOutput.class);
            ProductDto expectedDto = env.get("expectedDto", ProductDto.class);
            
            assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
            assertThat(output.getProduct()).isEqualTo(expectedDto);
            assertThat(output.getProduct().getTasks()).hasSize(3);
            assertThat(output.getProduct().getGoal()).isNotNull();
        })
        .Execute();
}
```

## 🎯 Controller 測試規範

### 1. MockMvc 測試 (必須提供)
```java
@WebMvcTest(CreateProductController.class)
@ContextConfiguration(classes = {TestConfig.class})
public class CreateProductControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CreateProductUseCase useCase;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void should_create_product_successfully() throws Exception {
        // Given
        CreateProductController.CreateProductRequest request = 
            new CreateProductController.CreateProductRequest();
        request.setName("Test Product");
        request.setUserId("user-123");
        
        CqrsOutput<ProductDto> output = CqrsOutput.of(createProductDto());
        when(useCase.execute(any())).thenReturn(output);
        
        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }
    
    @Test
    void should_return_400_when_name_is_missing() throws Exception {
        // Given
        CreateProductController.CreateProductRequest request = 
            new CreateProductController.CreateProductRequest();
        request.setUserId("user-123");
        // name is missing
        
        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").exists());
    }
}
```

### 2. REST Assured 整合測試 (必須提供)
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "aiscrum.test-data.enabled=false"
})
public class CreateProductControllerIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @MockBean
    private CreateProductUseCase useCase;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }
    
    @Test
    void should_create_product_with_rest_assured() {
        // Given
        CreateProductController.CreateProductRequest request = 
            new CreateProductController.CreateProductRequest();
        request.setName("Test Product");
        request.setUserId("user-123");
        
        CqrsOutput<ProductDto> output = CqrsOutput.of(createProductDto());
        when(useCase.execute(any())).thenReturn(output);
        
        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/products")
        .then()
            .statusCode(201)
            .header("Location", notNullValue())
            .body("productId", notNullValue())
            .body("name", equalTo("Test Product"));
    }
}
```

## 🎯 測試命名規範

### 1. 命名模式
```java
// Pattern: should_[expected_result]_when_[condition]

// ✅ 好的命名
should_create_product_successfully_when_input_is_valid()
should_throw_exception_when_name_is_null()
should_return_404_when_product_not_found()

// ❌ 不好的命名
testCreateProduct()  // 太籠統
test1()              // 無意義
createProductTest()  // 沒有說明預期結果
```

### 2. ezSpec Scenario 命名
```java
@EzScenario
public void should_create_product_with_all_required_fields() {
    feature.newScenario("Successfully create product with name, description, and price")
        // ...
}
```

## 🎯 測試資料建構

### 1. Test Data Builder Pattern
```java
public class ProductTestDataBuilder {
    private ProductId id = ProductId.create();
    private String name = "Default Product";
    private UserId creatorId = UserId.valueOf("user-123");
    private ProductState state = ProductState.CREATED;
    
    public static ProductTestDataBuilder aProduct() {
        return new ProductTestDataBuilder();
    }
    
    public ProductTestDataBuilder withId(ProductId id) {
        this.id = id;
        return this;
    }
    
    public ProductTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public Product build() {
        return new Product(id, name, creatorId);
    }
}

// 使用
Product product = ProductTestDataBuilder.aProduct()
    .withName("Custom Product")
    .withState(ProductState.ACTIVE)
    .build();
```

### 2. Object Mother Pattern
```java
public class ProductMother {
    
    public static Product simple() {
        return new Product(
            ProductId.create(),
            "Simple Product",
            UserId.valueOf("user-123")
        );
    }
    
    public static Product withTasks() {
        Product product = simple();
        product.createTask(TaskId.create(), "Task 1", EstimatedHours.of(8));
        product.createTask(TaskId.create(), "Task 2", EstimatedHours.of(5));
        return product;
    }
    
    public static Product complete() {
        Product product = withTasks();
        product.setGoal(new ProductGoal("Complete product goal"));
        product.defineDefinitionOfDone(Arrays.asList("Tested", "Documented"));
        return product;
    }
}
```

## 🎯 Mock 使用準則

### 1. 何時使用 Mock
```java
// ✅ Mock 外部依賴
@MockBean
private ProductRepository repository;

@MockBean
private ExternalApiClient apiClient;

// ❌ 不要 Mock Value Objects 或 Domain Objects
Product product = mock(Product.class);  // 錯誤！
ProductId id = mock(ProductId.class);   // 錯誤！
```

### 2. Mock 驗證
```java
// 驗證方法呼叫
verify(repository).save(any(Product.class));
verify(repository, times(1)).findById(productId);
verify(repository, never()).delete(any());

// 驗證參數
ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
verify(repository).save(captor.capture());
Product saved = captor.getValue();
assertThat(saved.getName()).isEqualTo("Expected Name");
```

## 🔍 檢查清單

### Use Case 測試
- [ ] 使用 ezSpec BDD 風格
- [ ] 有 Given-When-Then 結構
- [ ] Mock Repository 和外部依賴
- [ ] 測試成功和失敗場景
- [ ] Query 測試準備完整資料

### Controller 測試
- [ ] 提供 MockMvc 測試
- [ ] 提供 REST Assured 測試
- [ ] 使用 Controller inner class Request/Response
- [ ] 測試各種 HTTP 狀態碼
- [ ] 驗證回應格式

### 單元測試
- [ ] 測試命名清晰
- [ ] 沒有外部依賴
- [ ] 快速執行
- [ ] 測試單一行為

### 測試品質
- [ ] 測試覆蓋率 > 80%
- [ ] 有邊界條件測試
- [ ] 有異常情況測試
- [ ] 測試可重複執行

## 相關文件
- [測試規範](../coding-standards.md#-測試規範)
- [ezSpec 測試模板](../examples/reference/ezspec-test-template.md)
- [測試範例](../examples/test/README.md)
- [測試資料準備指南](../../TEST-DATA-PREPARATION-GUIDE.md)
- [ADR-024: Test Isolation and DomainEventMapper Management](../../../.dev/adr/ADR-024-test-isolation-and-domain-event-mapper.md)