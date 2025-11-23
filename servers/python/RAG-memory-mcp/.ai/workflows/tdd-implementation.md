12# Workflow: TDD 實現 (Test-Driven Development)

**標籤**: `#sub-agent-integrated` `#complete-integration` `#test-driven` `#testing` `#review`  
**整合狀態**: 🤖 完全整合 (3 Sub-agents)

## 概述

此工作流程指導 AI 使用測試驅動開發（TDD）方式實現功能，確保代碼質量和測試覆蓋。
本流程與 **Sub-agent System** 整合，在測試生成階段使用專門的 Test Generation Agent。

## 🤖 Sub-agent 整合
- **第一階段**：使用 Test Generation Agent 產生所有測試
- **第五階段**：使用 Code Generation Agent 重構程式碼
- **第五階段**：使用 Code Review Agent 審查品質

### 🚨 強制要求
**Use Case 測試必須使用 ezSpec BDD 風格**：
- 使用 `@EzFeature` 和 `@EzScenario` 註解
- 使用 Given-When-Then 格式
- 不得使用純 JUnit 風格的 Use Case 測試

## 如何啟動此 Workflow

### 基本指令
```
使用 TDD workflow 實現 [UseCase Name]
（Use Case 測試必須使用 ezSpec）
```

### 完整指令（推薦）
```
請使用 TDD 方式實現 CreateProduct use case：
1. 先寫測試（Use Case 測試必須使用 ezSpec）
2. 再實現功能
3. 最後重構

規格：[貼上 use case JSON 規格]
```

### 進階指令（包含所有組件）
```
請用 TDD 方式完整實現以下功能：
- Use Case: CreateProduct
- 需要生成：測試、use case、aggregate、domain events、value objects、repository
- 規格：[貼上 JSON]
- 測試框架：JUnit 5 + ezSpec
```

## TDD 實現步驟

### 第一階段：編寫所有 ezSpec 測試（紅燈）

#### 🤖 使用 Test Generation Sub-agent

**啟動方式**：
```
請啟動 Test Generation Sub-agent 根據 spec 產生所有 TDD 測試
```

**Sub-agent 執行**：
1. 載入 spec 檔案和 ezspec-test-template.md
2. 產生完整的 ezSpec BDD 測試套件
3. 為每個組件編寫測試（Use Case、Aggregate、Value Objects、Repository）
4. 確保所有測試都是失敗狀態（紅燈）
5. **⚠️ 重要：完成所有測試後必須暫停，等待開發人員確認**

**傳統 AI 行動**（若不使用 sub-agent）：
1. 根據規格生成完整的測試套件
2. 為每個組件編寫測試（Use Case、Aggregate、Value Objects、Repository）
3. **🚨 強制要求：Use Case 測試必須使用 ezSpec BDD 風格**
   - 必須使用 `@EzFeature` 和 `@EzScenario` 註解
   - 必須使用 Given-When-Then 格式
   - 必須參考下方範例，不得使用純 JUnit 風格
4. **⚠️ 重要：完成所有測試後必須暫停，等待開發人員確認**

**生成的完整測試列表**：
- Use Case Test（正常流程、異常流程、邊界情況）
- Aggregate Test（創建、業務邏輯、事件生成）
- Value Object Test（驗證邏輯、不變性）
- Repository Test（保存、查詢、刪除）

**範例輸出**：
```java
@EzFeature
public class CreateProductUseCaseTest {
    
    static Feature feature = Feature.New("Create Product Use Case");
    
    private CreateProductUseCase useCase;
    private Repository<Product, ProductId> repository;
    private MessageBus messageBus;
    
    @BeforeEach
    void setUp() {
        messageBus = new BlockingMessageBus();
        repository = new GenericInMemoryRepository<>(messageBus);
        useCase = new CreateProductUseCase(repository);
    }
    
    @EzScenario
    public void create_product_successfully() {
        feature.newScenario("Successfully create a product")
            .Given("valid product creation input", env -> {
                var command = new CreateProductCommand(
                    "product-123", 
                    "My Product", 
                    "user-456"
                );
                env.put("command", command);
            })
            .When("the use case is executed", env -> {
                var command = env.get("command", CreateProductCommand.class);
                var output = useCase.execute(command);
                env.put("output", output);
            })
            .Then("a product should be created successfully", env -> {
                var output = env.get("output", CqrsOutput.class);
                assertThat(output.isSuccessful()).isTrue();
                assertThat(output.getId()).isEqualTo("product-123");
                
                // 驗證 repository
                verify(repository).save(any(Product.class));
            });
    }
    
    @EzScenario
    public void should_fail_when_product_name_is_empty() {
        feature.newScenario("Fail when product name is empty")
            .Given("a command with empty product name", env -> {
                var command = new CreateProductCommand(
                    "product-123", 
                    "", // empty name
                    "user-456"
                );
                env.put("command", command);
            })
            .When("the use case is executed", env -> {
                var command = env.get("command", CreateProductCommand.class);
                assertThrows(InvalidProductNameException.class, () -> {
                    useCase.execute(command);
                });
            })
            .Then("no product should be saved", env -> {
                verify(repository, never()).save(any(Product.class));
            });
    }
}
```

**🛑 暫停點：開發人員確認**

在此階段完成後，AI 必須：
1. 總結已生成的所有測試
2. 說明測試涵蓋的場景和預期行為
3. **等待開發人員明確確認**（例如回覆 "確認測試，請繼續"）
4. 只有在收到確認後才能繼續下一階段

### 第二階段：實現最小可行代碼（綠燈）

**AI 行動**：
1. 創建 Use Case 介面和實現
2. 創建必要的 Input/Output 類
3. 實現最簡單的邏輯讓測試通過

**🚨 重要提醒**：
- 如果測試執行失敗，**絕對不要修改 ezSpec 的 Given-When-Then 內容**
- ezSpec 測試代表業務規格，不是實現細節
- 測試失敗時必須暫停並尋求人類確認如何處理

**生成順序**：
1. Use Case Interface
2. Input/Output DTOs
3. Use Case Implementation (最簡化版本)

### 第三階段：實現 Aggregate 和 Domain Objects

**AI 行動**：
1. 實現 Aggregate Root
2. 實現 Domain Events
3. 實現 Value Objects
4. 讓 Aggregate 測試通過

**範例輸出**：

### Aggregate, Entity, Domain Event 和 Value Object 測試（可使用標準 JUnit）
```java
public class ProductTest {
    
    @Test
    public void should_create_product_with_valid_data() {
        // Given
        var productId = new ProductId("product-123");
        var productName = new ProductName("My Product");
        var userId = new UserId("user-456");
        
        // When
        var product = new Product(productId, productName.toString(), userId.toString());
        
        // Then
        assertThat(product.getId()).isEqualTo(productId);
        assertThat(product.getVersion()).isEqualTo(1);
        assertThat(product.getUncommittedEvents()).hasSize(1);
        assertThat(product.getUncommittedEvents().get(0))
            .isInstanceOf(ProductCreated.class);
    }
}
```

**🚨 重要區別**：
- **Use Case 測試**：必須使用 ezSpec BDD 風格（@EzFeature, @EzScenario）
- **Aggregate/Entity/Domain Event/Value Object 測試**：可使用標準 JUnit
- **原因**：Use Case 代表業務場景，需要 BDD 風格來清晰表達；領域物件測試著重技術正確性

### 第四階段：實現 Repository

**AI 行動**：
1. 創建 Repository 介面
2. 實現 JPA Repository（如需要）
3. 實現 In-Memory Repository（用於測試）
4. 讓 Repository 測試通過

**生成組件**：
- `Product` (Aggregate Root)
- `ProductId`, `ProductName`, `UserId` (Value Objects)
- `ProductCreated` (Domain Event)
- `ProductRepository` (Interface)
- `JpaProductRepository` 或 `InMemoryProductRepository` (Implementation)

### 第五階段：重構和優化

#### 🤖 使用 Code Generation Sub-agent 重構
**啟動方式**：
```
請啟動 Code Generation Sub-agent 重構程式碼，保持測試通過
```

**Sub-agent 執行**：
1. 分析現有程式碼結構
2. 識別重複和可優化部分
3. 應用設計模式和最佳實踐
4. 重構後確保所有測試仍然通過

#### 🤖 使用 Code Review Sub-agent 審查
**啟動方式**：
```
請啟動 Code Review Sub-agent 審查 TDD 實作結果
```

**Sub-agent 執行**：
1. 檢查程式碼是否符合 coding standards
2. 驗證測試覆蓋率是否足夠
3. 識別潛在的 code smells
4. 提供改進建議
5. 確保 TDD 原則被正確遵守

**傳統 AI 行動**（若不使用 sub-agent）：
1. 檢查代碼重複
2. 提取共用邏輯
3. 優化命名和結構
4. 確保所有測試仍然通過

## TDD 實現順序和確認機制

### 第一階段：測試先行 📝
AI 會先生成**所有**測試，包括：
- Use Case Test（BDD 風格）
- Aggregate Test（領域邏輯測試）
- Value Object Test（驗證邏輯）
- Repository Test（持久化行為）

**🛑 重要：必須等待開發人員確認測試後才繼續**

### 第二階段：逐步實現 🔧
只有在測試確認後，AI 才會按順序實現：
1. Use Case Interface & Implementation
2. Aggregate Root & Domain Events
3. Value Objects
4. Repository Interface & Implementation
5. 重構和優化

### 開發人員確認要點 ✅
當 AI 生成所有測試後，請檢查：
- [ ] 測試案例涵蓋所有業務場景
- [ ] 測試命名清楚表達意圖
- [ ] BDD 風格的 Given-When-Then 結構
- [ ] 包含正常流程、異常處理、邊界條件
- [ ] 測試數據和期望結果合理

確認後回覆：**"確認測試，請繼續"** 或 **"測試需要調整：[具體建議]"**

## 最佳實踐

### 測試命名規範
```
should_[expected_behavior]_when_[condition]
// 例如：should_throw_exception_when_plan_name_is_empty
```

### 測試結構
- **Given**: 設定測試環境
- **When**: 執行測試動作
- **Then**: 驗證結果

### 覆蓋率要求
- Use Case: 100%
- Aggregate: 100%
- Value Objects: 重要邏輯 100%
- Repository: 介面行為 100%

## 🚨 執行 TDD 的標準指令

### 強制使用 ezSpec 的 TDD 指令（推薦）
```
使用 TDD workflow 實現 [UseCase名稱]
特別注意：
1. Use Case 測試必須使用 ezSpec（@EzFeature, @EzScenario）
2. 使用 Given-When-Then 格式
3. 參考本文件中的 ezSpec 範例
4. 完成測試後等待確認再實作
```

### 簡化版指令（仍必須用 ezSpec）
```
tdd '[use case name]' - 測試必須使用 ezSpec BDD 風格
```

## 常用 AI 指令範例

### 1. 基本 TDD 實現（自動確認模式）
```
使用 TDD 實現 CreateSprint，先寫所有測試再寫代碼
重要：Use Case 測試必須使用 ezSpec
```

### 2. 完整 TDD 流程（手動確認模式）
```
請使用 TDD workflow 實現 CreateProduct：
1. 先生成所有測試（等我確認）
2. 確認後再實現所有組件
3. 使用 ezSpec BDD 風格
```

### 3. 指定測試框架和確認流程
```
用 TDD 實現 SetProductGoal：
- 測試框架：JUnit 5 + ezSpec
- 流程：先出所有測試並等待確認
- 確認後實現：Use Case、Aggregate、Repository
```

### 4. 根據規格實現（包含確認）
```
根據以下規格用 TDD 實現：
[貼上 use-case-spec.json 內容]

流程：
1. 生成完整測試套件（等我確認）
2. 確認測試後實現所有組件
3. 每個階段都要先通過測試
```

### 5. 明確指出需要確認的指令
```
TDD 實現 EstimateProductBacklogItem：
⚠️ 重要：生成所有測試後請停下來等我確認
- 包含 Use Case、Aggregate、Value Object 測試
- 使用 BDD 風格
- 確認後再繼續實現代碼
```

## 故障排除

### 常見問題

1. **AI 沒有停下來等確認**
   - 在指令中明確說明：「生成測試後等我確認」
   - 使用指令：「⚠️ 重要：先寫測試並暫停等確認」

2. **測試無法編譯**
   - 確保先生成必要的介面和類別簽名
   - 使用 IDE 的自動修復功能

3. **測試覆蓋不足**
   - 在確認階段指出缺少的測試案例
   - 要求補充邊界案例和異常情況測試

4. **測試品質不符預期**
   - 在確認階段提供具體修改建議
   - 要求調整測試結構或命名

5. **重構破壞測試**
   - 小步重構
   - 每次改動後運行測試

6. **測試執行失敗** 🚨
   - **絕對不要**直接修改 ezSpec 的 Given-When-Then 內容
   - 測試失敗時必須暫停並尋求人類確認
   - 分析失敗原因並詢問：是測試規格錯誤還是實現錯誤？
   - 示範回應格式：
     ```
     測試執行失敗，錯誤訊息：[貼上具體錯誤]
     
     請確認：
     - 是 ezSpec 測試的 Given-When-Then 規格有誤？
     - 還是我的 production code 實現有問題？
     
     我應該修改測試規格還是修改實現代碼？
     ```

## 相關資源

### Sub-agent Prompts
- `.ai/prompts/test-generation-prompt.md` - Test Generation Agent
- `.ai/prompts/code-generation-prompt.md` - Code Generation Agent
- `.ai/prompts/code-review-prompt.md` - Code Review Agent

### 技術資源
- `.ai/SUB-AGENT-SYSTEM.md` - Sub-agent 系統說明
- [測試範例](../tech-stacks/java-ca-ezddd-spring/examples/test-example.md)
- [ezSpec 使用指南](../tech-stacks/java-ca-ezddd-spring/examples/test/README.md)
- `.ai/tech-stacks/` - 編碼標準
- `.dev/specs/` - Use Case 規格
- [測試最佳實踐](../tech-stacks/java-ca-ezddd-spring/best-practices.md)

---

*提示：使用 TDD 時，記得遵循 Red-Green-Refactor 循環！*