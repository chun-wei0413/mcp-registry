# DDD + CA + CQRS 學習路徑

## 🎯 核心概念層級

### Level 1: 基礎概念
1. **Clean Architecture 原則**
   - 依賴方向：外層依賴內層
   - 業務邏輯獨立於框架
   - 測試優先設計

2. **DDD 戰術模式**
   - Aggregate：交易邊界
   - Value Object：不可變值
   - Domain Event：狀態變更記錄

3. **CQRS 基礎**
   - Command：改變狀態
   - Query：讀取資料
   - 讀寫分離

### Level 2: 實作模式
1. **Aggregate 實作**
   - 必看：`src/main/java/tw/teddysoft/aiscrum/product/domain/Product.java`
   - Pattern：Event Sourcing with `when()` method
   - Rule：使用 switch expression pattern matching

2. **Use Case 模式**
   - Command Use Case：返回 CqrsOutput
   - Query Use Case：返回自定義 Output
   - Service 實作：協調 Domain 和 Infrastructure

3. **測試策略**
   - Use Case Test：必須使用 ezSpec BDD
   - Domain Test：可用 JUnit 5
   - 原則：不直接操作 Aggregate

### Level 3: 進階實踐
1. **跨 Aggregate 協作**
   - Reactor Pattern：處理 Domain Events
   - 避免直接引用其他 Aggregate

2. **查詢模式選擇**
   - ID 查詢使用 Repository
   - 標準列表查詢使用 Projection
   - Reactor 中的跨聚合查詢使用 Inquiry
   - 軟刪除和歷史記錄使用 Archive

## 📚 必讀文件順序

### Phase 1: 理解架構
1. `CLAUDE.md` - 專案整體概念
2. `.ai/tech-stacks/java-ca-ezddd-spring/coding-standards.md` - 編碼標準
3. `.ai/tech-stacks/java-ca-ezddd-spring/coding-guide.md` - 實作指引

### Phase 2: 學習範例
1. **Aggregate 範例**
   - `Product.java` - 完整的 Aggregate 實作
   - `ProductEvents.java` - Domain Events 設計
   - `ProductId.java`, `SprintId.java` - Value Objects

2. **Use Case 範例**
   - `CreateProductUseCase.java` + `CreateProductService.java` - 創建 Aggregate
   - `CreateTaskUseCase.java` + `CreateTaskService.java` - Aggregate 內操作
   - `GetProductsUseCase.java` + `GetProductsService.java` - Query 範例

3. **測試範例**
   - `.ai/tech-stacks/java-ca-ezddd-spring/examples/test-example.md` - ezSpec 測試

4. **進階查詢模式**
   - `.ai/tech-stacks/java-ca-ezddd-spring/examples/inquiry-archive/README.md` - Inquiry 與 Archive 模式
   - `.ai/tech-stacks/java-ca-ezddd-spring/examples/inquiry-archive/USAGE-GUIDE.md` - 實作指南
   - `.ai/tech-stacks/java-ca-ezddd-spring/coding-standards/archive-standards.md` - Archive Pattern 規範 🆕
   - `.dev/adr/ADR-020-archive-pattern-implementation.md` - Archive Pattern 決策記錄 🆕

### Phase 3: 避免錯誤
1. `tech-stacks/java-ca-ezddd-spring/anti-patterns.md` - 常見反模式
2. `tech-stacks/java-ca-ezddd-spring/COMMON-MISTAKES-GUIDE.md` - 錯誤案例分析

## 🔑 關鍵原則速查

### DDD 原則
- **Aggregate 是交易邊界**：一次只修改一個 Aggregate
- **Value Object 不可變**：使用 record 實作
- **Domain Event 記錄所有變更**：Event Sourcing pattern

### Clean Architecture 原則
- **依賴倒置**：Use Case 依賴 Repository interface，不依賴實作
- **框架隔離**：Domain 層不含 Spring 註解
- **測試優先**：先寫測試，再寫實作

### CQRS 原則
- **Command 返回結果**：使用 CqrsOutput
- **Query 返回資料**：自定義 Output 包含 DTO
- **讀寫分離**：Command 修改，Query 只讀

## 💡 編碼風格特徵

### Java 17+ 特性
```java
// Pattern Matching in switch
switch (event) {
    case ProductEvents.ProductCreated e -> {
        this.productId = e.productId();
        this.name = e.name();
    }
}

// Record for Value Objects
public record ProductId(String value) implements ValueObject {}
```

### ezddd 框架特性
```java
// Aggregate 繼承
public class Product extends EsAggregateRoot<ProductId, ProductEvents>

// Use Case 介面
public interface CreateProductUseCase extends Command<CreateProductInput, CqrsOutput>
```

### 測試風格
```java
// ezSpec BDD style
@EzScenario
public void should_create_product_successfully() {
    feature.newScenario()
        .Given("valid product input", env -> {})
        .When("execute create product use case", env -> {})
        .Then("product created with domain event", env -> {})
        .Execute();
}
```

## 🚀 快速開始模板

當 LLM 需要實作新功能時，引導它：

1. **先看對應的範例**
   - 創建 Aggregate？看 `CreateProductUseCase`
   - Aggregate 內操作？看 `CreateTaskUseCase` 或 `EstimateProductBacklogItemUseCase`
   - 查詢功能？看 `GetProductsUseCase` 或 `GetSprintsUseCase`

2. **遵循命名規範**
   - Use Case：`[Operation][Aggregate]UseCase`
   - Service：`[Operation][Aggregate]Service`
   - Input：`[Operation][Aggregate]Input`

3. **套用正確模式**
   - Command：實作 `Command<Input, CqrsOutput>`
   - Query：實作 `Query<Input, CustomOutput>`
   - Test：使用 ezSpec 的 Given-When-Then

## ⚠️ 常見陷阱提醒

1. **不要自動產生 ezddd 框架類別**
2. **不要使用 JPA Lazy Loading**
3. **不要在 Use Case 注入 MessageBus**
4. **不要直接操作其他 Aggregate**
5. **不要在測試中直接呼叫 Aggregate 方法**

## 🤖 如何引導 LLM 學習你的風格

### 初始對話設定
```markdown
我正在使用 DDD + Clean Architecture + CQRS + Event Sourcing 開發系統。
請參考以下資源學習我的編碼風格：

1. 專案記憶：CLAUDE.md
2. 學習路徑：.ai/guides/LEARNING-PATH.md
3. 程式碼模板：.ai/CODE-TEMPLATES.md
4. 範例程式：src/main/java/tw/teddysoft/aiscrum/

重要原則：
- 使用 ezddd 框架，不要自動產生框架類別
- Aggregate 使用 switch pattern matching
- Use Case 測試必須用 ezSpec BDD style
- 不使用 JPA Lazy Loading
```

### 具體任務 Prompt 模式

#### 創建新 Aggregate
```markdown
請幫我實作 [Aggregate] 聚合根：
- 需要支援的操作：[列出操作]
- 包含的屬性：[列出屬性]
- 業務規則：[列出規則]

請產生：
1. [Aggregate].java - 參考 Product.java
2. [Aggregate]Events.java - 參考 ProductEvents.java
3. [Aggregate]Id.java - Value Object
4. Create[Aggregate]UseCase + Service
5. Create[Aggregate]UseCaseTest - ezSpec style
```

#### 添加新功能
```markdown
在 [Aggregate] 中新增 [功能] 功能：
- 業務需求：[描述需求]
- 輸入參數：[列出參數]
- 業務規則：[列出規則]

請產生：
1. 在 [Aggregate] 中新增方法
2. 在 [Aggregate]Events 中新增事件
3. [Operation][Aggregate]UseCase + Service
4. 更新 when() 方法處理新事件
5. 測試案例
```

#### Code Review
```markdown
請 review 這段程式碼是否符合我們的 DDD + CA 規範：

[貼上程式碼]

請檢查：
1. 是否遵循 Clean Architecture 依賴方向
2. Aggregate 的 when() 是否使用 switch pattern matching
3. Domain Events 是否使用 Value Objects
4. Use Case 是否正確實作 Command/Query interface
5. 測試是否使用正確的風格（ezSpec for Use Case）
```

### Prompt 最佳實踐

#### DO's ✅
1. 總是提供具體的參考檔案路徑
2. 使用一致的術語（Aggregate, Use Case, Domain Event）
3. 要求產生完整的程式碼組合（不只是片段）
4. 明確指定要使用的設計模式

#### DON'Ts ❌
1. 不要假設 LLM 記得之前的對話
2. 不要省略業務規則和需求
3. 不要接受不符合規範的程式碼
4. 不要忘記要求測試案例

### 錯誤修正循環
當 LLM 產生不符合規範的程式碼時：
1. 明確指出違反了哪條規則（引用 CLAUDE.md）
2. 提供正確的範例參考
3. 解釋背後的設計原則
4. 要求重新產生