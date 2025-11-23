# 測試資料準備指南 (Test Data Preparation Guide)

## 目的
本指南定義了如何在 Query Use Case 測試中準備完整的測試資料，確保查詢結果的完整性和正確性。

## 核心原則

### 1. 完整性原則
所有 Query Use Case 測試必須準備**完整的業務物件**，包含所有相關的屬性和關聯資料。

### 2. 真實性原則
測試資料應該反映真實的業務場景，透過呼叫其他 Use Case 來建立，而非直接操作 Aggregate。

### 3. 獨立性原則
每個測試案例的資料準備應該獨立，不依賴其他測試的執行順序或狀態。

## 實作方式

### 在 Spec 檔案中定義 testDataSetup

```json
{
  "query": "GetProduct",
  "testDataSetup": {
    "description": "準備完整的 Product 測試資料",
    "steps": [
      {
        "order": 1,
        "useCase": "CreateProductUseCase",
        "description": "創建基本的 Product",
        "input": {
          "id": "product-123",
          "name": "AI Scrum Assistant",
          "userId": "user-456"
        }
      },
      {
        "order": 2,
        "useCase": "SetProductGoalUseCase",
        "description": "設定 Product Goal",
        "input": {
          "productId": "product-123",
          "productGoalId": "goal-123",
          "name": "Deliver AI-powered Scrum tools",
          "description": "Build comprehensive AI assistant",
          "state": "ACTIVE"
        }
      },
      {
        "order": 3,
        "useCase": "DefineDefinitionOfDoneUseCase",
        "description": "定義 Definition of Done",
        "input": {
          "productId": "product-123",
          "name": "Standard DoD",
          "criteria": [
            "Code reviewed",
            "Unit tests written and passing",
            "Documentation updated",
            "Deployed to staging"
          ],
          "note": "Team agreed definition"
        }
      }
    ],
    "note": "執行完這些步驟後，必須清除設置產生的事件"
  }
}
```

## 各 Aggregate 測試資料準備要求

### Product 測試資料
- ✅ **必須包含**：
  - Product 基本資訊（id, name, state）
  - ProductGoal（透過 SetProductGoalUseCase）
  - DefinitionOfDone（透過 DefineDefinitionOfDoneUseCase）
  - 相關的 ProductBacklogItems（如果查詢需要）

### Sprint 測試資料
- ✅ **必須包含**：
  - Sprint 基本資訊（id, name, state）
  - Sprint Goal（透過 DefineSprintGoalUseCase）
  - 時間箱設定（透過 SetSprintTimeboxUseCase）
  - 已選入的 PBIs（透過 SelectProductBacklogItemUseCase）
  - Team 成員（如果查詢需要）

### ProductBacklogItem 測試資料
- ✅ **必須包含**：
  - PBI 基本資訊（id, name, description, state）
  - 估計值（透過 EstimateProductBacklogItemUseCase）
  - Tasks（透過 CreateTaskUseCase）
  - Sprint 關聯（如果已選入 Sprint）
  - 優先級和重要性

## 測試實作模板

### ezSpec 測試結構 (Spring DI 方式)

```java
@EzFeature
@EzFeatureReport
public class GetProductUseCaseTest extends BaseUseCaseTest {
    
    static Feature feature = Feature.New("Get Product");
    
    @Autowired
    private CreateProductUseCase createProductUseCase;
    
    @Autowired
    private SetProductGoalUseCase setProductGoalUseCase;
    
    @Autowired
    private DefineDefinitionOfDoneUseCase defineDefinitionOfDoneUseCase;
    
    @Autowired
    private GetProductUseCase getProductUseCase;
    
    @EzScenario
    public void should_get_product_with_complete_data() {
        feature.newScenario("Should get product with complete data")
            .Given("完整的 Product 資料已建立", env -> {
                // Step 1: 創建 Product
                CreateProductInput createInput = CreateProductInput.create();
                createInput.id = "product-123";
                createInput.name = "AI Scrum Assistant";
                createInput.userId = "user-456";
                
                CqrsOutput createOutput = createProductUseCase.execute(createInput);
                assertThat(createOutput.getExitCode()).isEqualTo(ExitCode.SUCCESS);
                
                // Step 2: 設定 Product Goal
                SetProductGoalInput goalInput = SetProductGoalInput.create();
                goalInput.productId = "product-123";
                goalInput.productGoalId = "goal-123";
                goalInput.name = "Deliver AI-powered Scrum tools";
                goalInput.description = "Build comprehensive AI assistant";
                goalInput.state = "ACTIVE";
                
                CqrsOutput goalOutput = setProductGoalUseCase.execute(goalInput);
                assertThat(goalOutput.getExitCode()).isEqualTo(ExitCode.SUCCESS);
                
                // Step 3: 定義 Definition of Done
                DefineDefinitionOfDoneInput dodInput = DefineDefinitionOfDoneInput.create();
                dodInput.productId = "product-123";
                dodInput.name = "Standard DoD";
                dodInput.criteria = List.of(
                    "Code reviewed",
                    "Unit tests written and passing",
                    "Documentation updated",
                    "Deployed to staging"
                );
                dodInput.note = "Team agreed definition";
                dodInput.definedAt = Instant.now();
                
                CqrsOutput dodOutput = defineDefinitionOfDoneUseCase.execute(dodInput);
                assertThat(dodOutput.getExitCode()).isEqualTo(ExitCode.SUCCESS);
                
                // 重要：等待事件被捕獲後清除
                await().untilAsserted(() -> 
                    assertEquals(3, getCapturedEvents().size())
                );
                clearCapturedEvents();
            })
            .When("查詢該 Product", env -> {
                GetProductInput input = GetProductInput.create();
                input.productId = "product-123";
                
                GetProductOutput output = getProductUseCase.execute(input);
                env.put("output", output);
            })
            .Then("應該返回完整的 Product 資料", env -> {
                GetProductOutput output = env.get("output", GetProductOutput.class);
                
                // 驗證基本資料
                assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
                assertThat(output.getId()).isEqualTo("product-123");
                
                ProductDto product = output.getProduct();
                assertThat(product).isNotNull();
                assertThat(product.getName()).isEqualTo("AI Scrum Assistant");
                
                // 驗證 Product Goal
                assertThat(product.getGoal()).isNotNull();
                assertThat(product.getGoal()).contains("Deliver AI-powered Scrum tools");
                
                // 驗證 Definition of Done
                assertThat(product.getDefinitionOfDone()).isNotNull();
                assertThat(product.getDefinitionOfDone().getName())
                    .isEqualTo("Standard DoD");
                assertThat(product.getDefinitionOfDone().getCriteria())
                    .hasSize(4)
                    .contains("Code reviewed", "Unit tests written and passing");
            })
            .Execute();  // 必須以 .Execute() 結尾
    }
}
```

## BaseUseCaseTest 提供的功能

```java
// BaseUseCaseTest 提供的事件捕獲方法
public abstract class BaseUseCaseTest extends BaseSpringBootTest {
    
    // 獲取捕獲的事件
    protected List<DomainEvent> getCapturedEvents() {
        return fakeEventListener.capturedEvents;
    }
    
    // 清除捕獲的事件
    protected void clearCapturedEvents() {
        fakeEventListener.capturedEvents.clear();
    }
    
    // 找尋特定類型的事件
    protected <T extends DomainEvent> T findEvent(
            List<DomainEvent> events, Class<T> eventClass) {
        return events.stream()
            .filter(eventClass::isInstance)
            .map(eventClass::cast)
            .findFirst()
            .orElse(null);
    }
}

// Spring Configuration 自動提供所有 Use Case 的 Bean
@Configuration
public class TestInMemoryConfiguration {
    
    @Bean
    public CreateProductUseCase createProductUseCase(
            Repository<Product, ProductId> repository) {
        return new CreateProductService(repository);
    }
    
    @Bean
    public SetProductGoalUseCase setProductGoalUseCase(
            Repository<Product, ProductId> repository) {
        return new SetProductGoalService(repository);
    }
    
    // ... 其他 Use Case Beans
}
```

## 🔴 InMemory Profile 測試資料隔離

### 重要：InMemory Repository 清理機制

當使用 InMemory profile 時，**必須**在每個測試前清空 repository 以確保測試隔離：

```java
@BeforeEach
public void setUp() {
    // 清空所有相關的 repository 確保測試隔離
    if (productRepository instanceof GenericInMemoryRepository<Product, ProductId> inMemoryRepo) {
        inMemoryRepo.clear();
    }
    
    // 如果有多個 repository，都要清空
    if (sprintRepository instanceof GenericInMemoryRepository<Sprint, SprintId> inMemoryRepo) {
        inMemoryRepo.clear();
    }
    
    // 清空捕獲的事件
    clearCapturedEvents();
}
```

### 為什麼需要這樣做
- InMemory repository 使用 Map 儲存資料，在測試之間不會自動清空
- `@DirtiesContext(classMode = AFTER_CLASS)` 只在整個測試類別結束後才重建 context
- 沒有清理會導致測試資料累積，造成測試失敗

### 典型失敗症狀
- `expected: <1> but was: <4>` - 資料從前一個測試累積
- `expected: <true> but was: <false>` - 期望空集合但有舊資料
- 測試單獨執行成功，但一起執行失敗

### 診斷檢查清單
- [ ] 確認所有測試類別都有 @BeforeEach 清理方法
- [ ] 檢查是否所有使用的 repository 都有被清空
- [ ] 確認 clearCapturedEvents() 有被呼叫
- [ ] 驗證測試資料不會跨測試方法累積

## 常見錯誤

### ❌ 錯誤 1：直接創建 Aggregate
```java
// 不要這樣做 - 直接操作 Aggregate
.Given("a product exists", env -> {
    Product product = new Product(productId, name, userId);
    productRepository.save(product);  // ❌ 直接使用 repository
})

// 正確做法 - 透過 Use Case
.Given("a product exists", env -> {
    CreateProductInput input = CreateProductInput.create();
    input.id = productId;
    input.name = name;
    createProductUseCase.execute(input);  // ✅ 使用 Use Case
})
```

### ❌ 錯誤 2：不完整的測試資料
```java
// 不要這樣做 - 只創建 Product 而沒有 Goal 和 DoD
.Given("a product exists", env -> {
    CreateProductInput input = CreateProductInput.create();
    input.id = "product-123";
    input.name = "Test Product";
    createProductUseCase.execute(input);
    // ❌ 缺少 SetProductGoal 和 DefineDefinitionOfDone
})
```

### ❌ 錯誤 3：忘記清除設置事件
```java
// 不要這樣做 - 忘記清除事件
.Given("setup test data", env -> {
    // ... 創建測試資料
    createProductUseCase.execute(createInput);
    setProductGoalUseCase.execute(goalInput);
    // ❌ 忘記清除事件，會影響 When 階段的事件驗證
})

// 正確做法 - 等待並清除事件
.Given("setup test data", env -> {
    // ... 創建測試資料
    createProductUseCase.execute(createInput);
    setProductGoalUseCase.execute(goalInput);
    
    // ✅ 等待事件被捕獲後清除
    await().untilAsserted(() -> 
        assertEquals(2, getCapturedEvents().size())
    );
    clearCapturedEvents();
})
```

## 檢查清單

測試資料準備完成前，請確認以下項目：

- [ ] 所有必要的 Use Case 都已呼叫
- [ ] 測試資料包含所有相關的屬性
- [ ] 使用 Use Case 而非直接操作 Aggregate
- [ ] 設置完成後清除了發布的事件
- [ ] Then 階段驗證了所有預期的資料
- [ ] 測試可以獨立執行，不依賴其他測試

## 參考資源

- [Profile-Based Testing Guide](./PROFILE-BASED-TESTING-GUIDE.md)
- [BaseUseCaseTest 原始碼](../src/test/java/tw/teddysoft/aiscrum/test/base/BaseUseCaseTest.java)
- [ezSpec 測試模板](../tech-stacks/java-ca-ezddd-spring/examples/reference/ezspec-test-template.md)
- [Use Case 實作指南](../tech-stacks/java-ca-ezddd-spring/examples/usecase/README.md)