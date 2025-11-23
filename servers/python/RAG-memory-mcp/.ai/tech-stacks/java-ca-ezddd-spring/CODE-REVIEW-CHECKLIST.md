# 程式碼審查檢查清單 (Code Review Checklist)

> 本檢查清單幫助 AI 編碼助手進行系統化的程式碼審查，確保程式碼品質和一致性。

## 📋 目錄

1. [通用檢查項目](#通用檢查項目)
2. [Domain 層檢查](#domain-層檢查)
3. [UseCase 層檢查](#usecase-層檢查)
4. [Adapter 層檢查](#adapter-層檢查)
5. [測試檢查](#測試檢查)
6. [效能檢查](#效能檢查)
7. [安全性檢查](#安全性檢查)
8. [文檔檢查](#文檔檢查)

## ✅ 通用檢查項目

### 🚨 避免過度設計 (YAGNI - You Aren't Gonna Need It)
- [ ] **MUST**: 只實作 spec 檔案明確要求的功能
- [ ] **MUST**: Domain Events 必須與 spec 一對一對應
- [ ] **MUST**: 不預測未來需求，不實作「可能會用到」的功能
- [ ] **MUST**: 不因為範例有就照抄（範例只是參考）
- [ ] 沒有多餘的業務方法（spec 未要求的）
- [ ] 沒有多餘的 Entity 或 Value Object（spec 未定義的）

### 編碼規範
- [ ] 遵循 Java 命名規範（類別 PascalCase、方法 camelCase）
- [ ] 沒有未使用的 import
- [ ] 沒有註解掉的程式碼
- [ ] 適當的存取修飾符（private/protected/public）
- [ ] 遵循單一職責原則
- [ ] **類別成員順序**：Data members (fields) 必須在 methods 之前宣告
  - 順序：fields → constructors → factory methods → getters/setters → business methods → private methods

### 程式碼品質
- [ ] 方法長度不超過 30 行
- [ ] 類別長度不超過 300 行
- [ ] 圈複雜度不超過 10
- [ ] 沒有重複的程式碼
- [ ] 有意義的變數和方法名稱

### 錯誤處理
- [ ] 適當的例外處理
- [ ] 不捕獲過於廣泛的例外（避免 catch Exception）
- [ ] 有意義的錯誤訊息
- [ ] 資源正確關閉（try-with-resources）

## 🏛️ Domain 層檢查

### 🚨 Event Sourcing 合規性檢查（最高優先級）⭐⭐⭐

#### 🔴 Constructor 職責檢查（必須優先）
**黃金法則**：狀態只能透過 when() 方法從事件重建

- [ ] **CRITICAL**: Constructor 是否直接設定狀態欄位？
  - ❌ 錯誤：`this.id = id; this.name = name;`（直接賦值）
  - ✅ 正確：只呼叫 `apply(event)` 讓 when() 設定狀態
- [ ] **CRITICAL**: 是否透過 apply(event) 觸發 when()？
  - ✅ 必須：`apply(event);` 在創建 event 之後
  - ❌ 錯誤：只用 `addDomainEvent(event)` 不呼叫 when()
- [ ] **CRITICAL**: 狀態欄位賦值是否只出現在 when() 方法中？
  - ✅ 正確：`this.id = ...` 只在 when() 中
  - ❌ 錯誤：Constructor 和 when() 都有賦值

#### ✅ 正確的 Event Sourcing 模式
```java
public class ProductBacklogItem extends EsAggregateRoot<PbiId, ProductBacklogItemEvents> {

    // ✅ 正確：Constructor 只建立事件，不設定狀態
    public ProductBacklogItem(ProductId productId, PbiId pbiId, String name, ...) {
        // Step 1: Preconditions - 只驗證輸入
        requireNotNull("Product ID", productId);
        requireNotNull("PBI ID", pbiId);

        // Step 2: Initialize collections (required for when())
        this.tagRefs = new LinkedHashSet<>();
        this.acceptances = new LinkedHashSet<>();

        // Step 3: Create domain event (使用參數，不用 this.xxx)
        ProductBacklogItemEvents.PbiCreated event = new ProductBacklogItemEvents.PbiCreated(
            productId,    // ✅ 使用參數
            pbiId,        // ✅ 使用參數
            name,         // ✅ 使用參數
            // ...
        );

        // Step 4: Apply event (這會呼叫 when() 設定狀態)
        apply(event);  // ✅ 關鍵：透過 apply() 設定狀態

        // Step 5: Postconditions - 驗證 when() 正確設定狀態
        ensure("PBI id is set correctly", () -> this.id.equals(pbiId));
        ensure("PBI name is set correctly", () -> this.name.equals(name));
    }

    // ✅ 正確：when() 是唯一設定狀態的地方
    @Override
    protected void when(ProductBacklogItemEvents event) {
        switch (event) {
            case ProductBacklogItemEvents.PbiCreated e -> {
                // ✅ 狀態只在這裡設定
                this.productId = e.productId();
                this.id = e.pbiId();
                this.name = e.name();
                this.description = e.description();
                // ... 設定所有欄位
            }
        }
    }
}
```

#### ❌ 錯誤的反模式
```java
public class ProductBacklogItem extends EsAggregateRoot<PbiId, ProductBacklogItemEvents> {

    // ❌ 錯誤：Constructor 直接設定狀態
    public ProductBacklogItem(ProductId productId, PbiId pbiId, String name, ...) {
        requireNotNull("Product ID", productId);

        // ❌ 錯誤：直接設定狀態（違反 Event Sourcing）
        this.productId = productId;
        this.id = pbiId;
        this.name = name;
        // ...

        // 建立事件
        ProductBacklogItemEvents.PbiCreated event = new ProductBacklogItemEvents.PbiCreated(
            this.productId,  // ❌ 使用 this (狀態已設定)
            this.id,         // ❌ 使用 this
            this.name,       // ❌ 使用 this
            // ...
        );

        addDomainEvent(event);  // ❌ 沒有呼叫 apply()

        // 問題：狀態被設定兩次
        // 1. Constructor 中直接賦值
        // 2. when() 方法也會賦值（如果從 Event Store 重建）
    }
}
```

#### 檢查項目
- [ ] **MUST**: Constructor 不可直接設定狀態欄位（除了 collections 初始化）
- [ ] **MUST**: Constructor 必須呼叫 `apply(event)` 而非只用 `addDomainEvent()`
- [ ] **MUST**: 事件參數使用 constructor 參數，不用 `this.xxx`
- [ ] **MUST**: 所有狀態賦值（`this.field = ...`）只出現在 when() 方法
- [ ] **MUST**: when() 方法處理事件的所有欄位（包括 metadata 相關欄位）
- [ ] **MUST**: Collections（Set, List）在 constructor 初始化為空集合
- [ ] **違反此規則必須標記為 CRITICAL - MUST FIX IMMEDIATELY**

#### 為什麼這很重要？
1. **Event Store 重建**：從事件重建 Aggregate 時，只會呼叫 when()，不會執行 Constructor 的賦值邏輯
2. **狀態一致性**：避免狀態被設定兩次（Constructor + when()）導致不一致
3. **Event Sourcing 純度**：確保物件狀態完全來自事件的 replay

### 🚨 Aggregate 套件組織檢查（必須優先檢查）
- [ ] **每個 Aggregate 是否有獨立的頂層套件？**
- [ ] **Aggregate 之間只透過 ID 引用，不直接包含？**
- [ ] **檢查 Value Object 是否重複定義（2024-08-12 教訓）**
  - 執行：`find . -name "*Id.java" -exec basename {} \; | sort | uniq -d`
  - SprintId 只能在 sprint.entity，不能在 pbi.entity
  - 警訊：沒有 import 卻能使用某個類別 = 可能有重複定義
- [ ] **套件名稱是否反映 Aggregate 名稱（而非其他 Aggregate）？**
- [ ] **是否已參考 `.ai/AGGREGATE-IDENTIFICATION-CHECKLIST.md` 進行判斷？**

#### 🔴 紅旗警告
- [ ] 如果看到 `ProductBacklogItem` 在 `product` 套件下 → **立即修正**
- [ ] 如果看到多個 Aggregate 在同一套件下 → **立即修正**
- [ ] 如果看到 `[aggregate1].[aggregate2].entity` 結構 → **立即修正**

#### ✅ 正確範例
```
tw.teddysoft.aiscrum.product.entity.Product       ✓
tw.teddysoft.aiscrum.pbi.entity.ProductBacklogItem ✓
tw.teddysoft.aiscrum.sprint.entity.Sprint          ✓
```

#### ❌ 錯誤範例
```
tw.teddysoft.aiscrum.product.entity.ProductBacklogItem ✗ (PBI 應有獨立套件)
tw.teddysoft.aiscrum.entities.Product                  ✗ (不應集中放置)
tw.teddysoft.aiscrum.product.pbi.entity               ✗ (PBI 不是 product 子模組)
```

### ⚠️ 重要：驗證方式選擇原則
- **Aggregate Root**：使用 `Contract.requireNotNull()` 和 `Contract.require()`（來自 ucontract 框架）
- **Entity 和 Value Object**：使用 `Objects.requireNonNull()` 和標準 Java 驗證
- **原因**：Contract 是 Design by Contract 框架，主要用於 Aggregate 的前置/後置條件和不變條件檢查

### Aggregate Root

#### 🔴 reject vs require 的正確使用（2024-08-15 更新）
**重要觀念澄清**：
- `reject()` - **用於避免產生不必要的 domain event**（例如：新值與舊值相同）
- `require()` - **用於檢查前置條件**，條件不滿足時拋出異常

檢查項目：
- [ ] **`reject` 只用於避免不必要的 event，不用於錯誤處理？**
- [ ] **前置條件檢查使用 `require` 而非 `reject`？**
- [ ] **`reject` 的條件是否正確（true 時返回）？**
- [ ] **是否正確使用 if 語句包裝 `reject` 並 return？**

##### ✅ 正確範例：
```java
// 正確：使用 reject 避免不必要的 event
public void rename(String newName) {
    requireNotNull("New name", newName);
    
    // 使用 reject 避免產生不必要的 Renamed event
    if (reject("Name unchanged", () -> this.name.equals(newName))) {
        return; // 不產生 event
    }
    
    apply(new ProductRenamed(this.id, newName, ...));
}

// 正確：使用 require 檢查前置條件
public void deleteTask(TaskId taskId, String reason, String userId) {
    requireNotNull("taskId", taskId);
    
    // 使用 require 檢查 task 必須存在
    require("Task not found", () -> getTask(taskId).isPresent());
    
    apply(new TaskDeleted(this.id, taskId, reason, userId, ...));
}
```

##### ❌ 錯誤範例：
```java
// 錯誤：混淆 reject 和 require 的用途
public void deleteTask(TaskId taskId, String reason, String userId) {
    // 錯誤：task 不存在應該是異常，不是「避免產生 event」
    if (reject("Task not found", () -> !getTask(taskId).isPresent())) {
        return; // 這會默默地什麼都不做，不是預期行為
    }
    apply(new TaskDeleted(...));
}

// 錯誤：應該使用 reject 而不是直接 if
public void rename(String newName) {
    if (this.name.equals(newName)) {
        return; // 應該用 reject 表達意圖
    }
    apply(new ProductRenamed(...));
}
```

**使用原則**：
- `requireNotNull` / `require`：用於不可違反的前置條件（違反時拋出異常）
- `reject`：用於避免產生不必要的 domain event（相同值不需要更新）
- `ensure`：用於後置條件檢查（確保狀態變更正確）

#### 🔴 Command Method 後置條件檢查（2024-08-12 強制規定）
**必須遵守**: 每個 Aggregate 的 command method 必須使用 `ensure` 檢查：
1. 業務狀態變更的正確性
2. Domain Event 產生的正確性

##### 簡潔檢查規範（2024-08-12 更新）
**Nullable Fields 檢查方式**：
```java
// ✅ 正確：使用單一 ensure 語句處理 nullable fields
ensure("Sprint goal matches input", () -> 
    (goal == null && getGoal() == null) || 
    (goal != null && goal.equals(getGoal())));

ensure("Sprint capacity matches input", () -> 
    (capacity == null && getCapacity() == null) || 
    (capacity != null && capacity.equals(getCapacity())));

// ❌ 錯誤：冗餘的 if-else 檢查（違反簡潔原則）
if (goal != null) {
    ensure("Sprint goal is set", () -> getGoal() != null && getGoal().equals(goal));
} else {
    ensure("Sprint goal is null", () -> getGoal() == null);
}
```

##### 完整範例：
```java
// ✅ 正確：完整的後置條件檢查
public void createTask(TaskId taskId, String name, EstimatedHours estimatedHours, String creatorId) {
    requireNotNull("Task ID", taskId);
    requireNotNull("Name", name);
    
    // Apply domain event
    apply(new ProductBacklogItemEvents.TaskCreated(
        this.productId,
        this.id,
        taskId,
        name,
        estimatedHours,
        remainingHours,
        null,
        null,
        creatorId,
        new HashMap<>(),
        UUID.randomUUID(),
        DateProvider.now()
    ));
    
    // 必須檢查：業務狀態
    Task createdTask = tasks.stream()
        .filter(t -> t.getId().equals(taskId))
        .findFirst()
        .orElse(null);
    
    ensure("Task is created", () -> createdTask != null);
    ensure("Task ID is set", () -> createdTask.getId().equals(taskId));
    ensure("Task name is set", () -> createdTask.getName().equals(name));
    ensure("Task initial state is TODO", () -> createdTask.getState() == TaskState.TODO);
    
    // 必須檢查：Domain Event 正確性
    ensure("TaskCreated event is generated correctly", () -> 
        getLastDomainEvent() instanceof ProductBacklogItemEvents.TaskCreated created &&
        created.taskId().equals(taskId) &&
        created.name().equals(name) &&
        created.creatorId().equals(creatorId)
    );
}

// ❌ 錯誤：缺少後置條件檢查
public void createTask(TaskId taskId, String name, EstimatedHours estimatedHours, String creatorId) {
    requireNotNull("Task ID", taskId);
    requireNotNull("Name", name);
    
    apply(new ProductBacklogItemEvents.TaskCreated(...));
    // 沒有 ensure 檢查 - 違反規定！
}
```

**檢查項目**:
- [ ] **MUST**: 每個會改變狀態的 command method 都有 ensure
- [ ] **MUST**: ensure 檢查業務狀態變更是否正確
- [ ] **MUST**: ensure 檢查 domain event 是否正確產生
- [ ] **MUST**: ensure 檢查 event 的關鍵屬性是否正確
- [ ] **MUST**: Nullable fields 使用單一 ensure 語句（不用 if-else）
- [ ] **違反此規則必須標記為 MUST FIX**

#### 🔴 Aggregate 建構子模式（2024-08-13 強制規定）
**必須遵守**: Aggregate Root 必須使用公開建構子（public constructor）創建新實例，不使用靜態工廠方法（static factory method）

##### ✅ 正確範例：
```java
// 創建新 Aggregate - 使用公開建構子
ProductBacklogItem pbi = new ProductBacklogItem(
    productId,
    pbiId,
    "User Story",
    "Description",
    null,  // sprintId
    estimate,
    importance,
    PbiState.BACKLOGGED,
    "user-123"
);

// Use Case 中的使用
Plan plan = new Plan(
    PlanId.create(),
    input.name,
    input.userId
);
```

##### ❌ 錯誤範例：
```java
// ❌ 錯誤：不要使用靜態工廠方法
ProductBacklogItem pbi = ProductBacklogItem.create(
    productId,
    pbiId,
    "User Story"
);

// ❌ 錯誤：不要使用私有建構子配合工廠方法
Plan plan = Plan.create(
    PlanId.create(),
    "Plan Name",
    "user-123"
);
```

**檢查項目**:
- [ ] **MUST**: Aggregate Root 使用公開建構子創建新實例
- [ ] **MUST**: 不使用 `static create()` 等工廠方法
- [ ] **MUST**: 建構子包含所有必要參數
- [ ] **MUST**: 建構子內使用 `requireNotNull` 驗證參數
- [ ] **MUST**: 建構子內發出創建事件（如 PbiCreated）
- [ ] **MUST**: 建構子內使用 `ensure` 檢查後置條件

#### 基本 Aggregate Root 檢查
```java
// 檢查項目範例
public class Plan extends EsAggregateRoot<PlanId, PlanEvents> {
    // ✓ 繼承自 EsAggregateRoot
    // ✓ 泛型參數正確（ID 類型、Event 類型）
    
    // 必須的建構子（從事件重建）
    public Plan(List<PlanEvents> domainEvents) {
        super(domainEvents);
    }
    
    // 公開建構子（用於創建新實例）
    public Plan(PlanId id, String name, String userId) {
        super();
        // 使用 Contract 驗證（只在 Aggregate 中使用）
        requireNotNull("id", id);
        requireNotNull("name", name);
        requireNotNull("userId", userId);
        // ...發出創建事件
    }
}
```

- [ ] 繼承自 `EsAggregateRoot`
- [ ] 包含接受 `List<DomainEvent>` 的建構子（用於事件重建）
- [ ] **MUST**: 使用公開建構子創建新實例（不使用 static factory method）
- [ ] 使用 `Contract.requireNotNull` 進行驗證（只在 Aggregate 中）
- [ ] 實作 `when()` 方法處理所有事件
- [ ] 所有狀態變更透過事件
- [ ] 不直接修改內部狀態

### Entities (Aggregate 內部實體)
```java
// 檢查項目範例
public class Project implements Entity<ProjectId> {
    // ✓ 實作 Entity<ID> 介面
    // ✓ 有唯一識別碼 (ProjectId)
    // ✓ 有生命週期（創建、修改、刪除）
    // ✓ 封裝業務行為
    private final ProjectId id;
    private final ProjectName name;
    private final Map<TaskId, Task> tasks;
    
    public ProjectId getId() {
        return id;
    }
}
```

- [ ] 實作 `Entity<ID>` 介面
- [ ] 有唯一識別碼（ID）
- [ ] 實作 `getId()` 方法
- [ ] 封裝業務邏輯和行為
- [ ] 只能透過 Aggregate Root 訪問
- [ ] 基於 ID 實作 equals() 和 hashCode()

### Value Objects
```java
// 檢查項目範例
public record PlanId(String value) implements ValueObject {
    // ✓ 使用 record（不可變）
    // ✓ 實作 ValueObject 介面
    // ✓ 有驗證邏輯
    public PlanId {
        Objects.requireNonNull(value, "PlanId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("PlanId value cannot be empty");
        }
    }
    
    public static PlanId create() {
        return new PlanId(UUID.randomUUID().toString());
    }
    
    public static PlanId valueOf(String value) {
        return new PlanId(value);
    }
}
```

- [ ] 使用 record 或確保不可變性
- [ ] 實作 `ValueObject` 介面
- [ ] 包含驗證邏輯
- [ ] 正確實作 equals() 和 hashCode()

### Repository 實作

#### 🚨 Repository 實作規範（2024-01-22 更新）
**重要原則**：
- [ ] **直接使用 `GenericInMemoryRepository`，不要創建空的繼承類別**
- [ ] **不創建自定義 Repository interface（使用 generic `Repository<T, ID>`）**
- [ ] **Repository Bean 透過 @Configuration 注入**

##### ✅ 正確範例：
```java
// Configuration 中直接使用 GenericInMemoryRepository
@Bean
public Repository<Product, ProductId> productRepository(MessageBus<DomainEvent> messageBus) {
    return new GenericInMemoryRepository<>(messageBus);
}
```

##### ❌ 錯誤範例：
```java
// 錯誤：創建多餘的空繼承類別
public class InMemoryProductRepository extends GenericInMemoryRepository<Product, ProductId> {
    public InMemoryProductRepository(MessageBus<DomainEvent> messageBus) {
        super(messageBus);  // 只是空的繼承，完全多餘！
    }
}

// 錯誤：創建自定義 Repository interface
public interface ProductRepository extends Repository<Product, ProductId> {
    // 不需要這樣做
}
```

### Domain Events

#### 🚨 Domain Event 新規範（2024-08-12 更新）
```java
// 正確範例
record PlanCreated(
    PlanId planId,           // 使用 Value Object
    String name,
    Map<String, String> metadata,  // 必須包含 metadata 欄位
    UUID id,                       // metadata 之後
    Instant occurredOn
) implements PlanEvents, ConstructionEvent {
    
    public PlanCreated {
        Objects.requireNonNull(planId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(metadata);  // 必須驗證
        Objects.requireNonNull(id);
        Objects.requireNonNull(occurredOn);
    }
    
    @Override
    public Map<String, String> metadata() {
        return metadata;  // 返回欄位，不是 Map.of()
    }
    
    @Override
    public String source() {
        return "Plan";  // 返回 Aggregate 名稱
    }
}
```

#### 必須檢查項目
- [ ] **MUST**: 每個 Domain Event 都在 spec 檔案中有明確定義
- [ ] **MUST**: 不實作 spec 未要求的 Domain Events
- [ ] **MUST**: 包含 `Map<String, String> metadata` 欄位（在 UUID id 之前）
- [ ] **MUST**: Override `metadata()` 方法返回 metadata 欄位
- [ ] **MUST**: Override `source()` 方法返回 Aggregate 名稱
- [ ] **MUST**: 在 compact constructor 中驗證 metadata 非 null
- [ ] **MUST**: 創建事件時使用 `new HashMap<>()` 而非 `Map.of()`（支援冪等性）

#### ConstructionEvent 和 DestructionEvent 規則（2024-08-30 新增）
- [ ] **MUST**: 所有 Aggregate 的 `[Aggregate]Created` domain event 必須實作 `ConstructionEvent` 介面
- [ ] **MUST**: 所有 Aggregate 的 `[Aggregate]Deleted` domain event 必須實作 `DestructionEvent` 介面
- [ ] **重要**: 此規則只適用於 Aggregate 的產生與刪除領域事件
- [ ] **不適用於**: Aggregate 內部的 entities 或 value objects（如 TaskCreated、TaskDeleted）

##### 正確範例：
```java
// Aggregate 的創建事件
record ProductCreated(...) implements ProductEvents, ConstructionEvent { }
record SprintCreated(...) implements SprintEvents, ConstructionEvent { }

// Aggregate 的刪除事件
record ProductDeleted(...) implements ProductEvents, DestructionEvent { }
record SprintDeleted(...) implements SprintEvents, DestructionEvent { }

// 內部實體的事件（不需要實作這些介面）
record TaskCreated(...) implements ProductBacklogItemEvents { }  // ✓ 不需要 ConstructionEvent
record TaskDeleted(...) implements ProductBacklogItemEvents { }  // ✓ 不需要 DestructionEvent
```

#### 其他檢查項目
- [ ] 實作對應的 Events 介面（通常是 InternalDomainEvent）
- [ ] 包含 `UUID id` 和 `Instant occurredOn`
- [ ] 使用 record 確保不可變性
- [ ] 優先使用 Value Objects 作為屬性類型（非 primitive types）
- [ ] **必須**使用 `DateProvider.now()` 生成時間戳記
- [ ] **禁止**使用 `Instant.now()` 或 `LocalDateTime.now()`

#### TypeMapper 檢查
- [ ] 使用新的 TypeMapper 實作方式（參考 CODE-TEMPLATES.md）
- [ ] 包含 `static DomainEventTypeMapper mapper()` 方法

### 業務規則
- [ ] 使用 Contract 進行前置條件驗證
- [ ] 業務規則在 Domain 層實作
- [ ] 適當的領域方法命名
- [ ] 方法返回適當的結果

## 🔄 冪等性實作檢查（2024-08-12 新增）

### Metadata 可變性檢查
```java
// ✅ 正確：Aggregate 使用可變 HashMap
private Product(ProductId id, String name) {
    apply(new ProductEvents.ProductCreated(
        id,
        name,
        new HashMap<>(),  // 可變的 HashMap
        UUID.randomUUID(),
        DateProvider.now()
    ));
}

// ❌ 錯誤：使用不可變的 Map.of()
private Product(ProductId id, String name) {
    apply(new ProductEvents.ProductCreated(
        id,
        name,
        Map.of(),  // 不可變，Use Case 無法修改
        UUID.randomUUID(),
        DateProvider.now()
    ));
}
```

### Use Case 層冪等性實作
```java
// ✅ 正確：Use Case 修改 metadata 實現冪等性
public CqrsOutput execute(CreateProductInput input) {
    Product product = new Product( // ❌ 應改為使用建構子
        ProductId.valueOf(input.productId),
        input.name
    );
    
    // 取得事件並修改 metadata
    List<DomainEvent> events = product.getUncommittedEvents();
    ProductEvents.ProductCreated event = (ProductEvents.ProductCreated) events.get(0);
    
    // 添加冪等性相關資訊
    event.metadata().put("requestId", input.requestId);
    event.metadata().put("idempotencyKey", input.idempotencyKey);
    
    // 如果是重試，記錄原始事件
    if (input.originalEventId != null) {
        event.metadata().put("originalEventId", input.originalEventId);
    }
    
    repository.save(product);
    return output;
}
```

### 檢查項目
- [ ] **MUST**: Domain Event 創建時使用 `new HashMap<>()` 而非 `Map.of()`
- [ ] **MUST**: Use Case 有存取 metadata 的邏輯（如需冪等性）
- [ ] **SHOULD**: metadata 包含 requestId 或 idempotencyKey
- [ ] **SHOULD**: 重試場景記錄 originalEventId
- [ ] **SHOULD**: metadata 包含追蹤資訊（userId, source, timestamp）

## 📦 UseCase 層檢查

### UseCase Interface

#### 🔴 Input/Output 必須為 Inner Class（2024-08-14 強制規定）
**必須遵守**: Use Case 的 Input 和 Query Output 都必須宣告為 UseCase interface 的 inner class。

##### ✅ 正確範例：
```java
// Query UseCase 的正確結構
public interface GetProductUseCase extends Query<GetProductUseCase.GetProductInput, GetProductUseCase.GetProductOutput> {
    
    class GetProductInput implements Input {
        public String productId;
        
        public static GetProductInput create() {
            return new GetProductInput();
        }
    }
    
    class GetProductOutput extends CqrsOutput<GetProductOutput> {
        private ProductDto product;
        
        public static GetProductOutput create() {
            return new GetProductOutput();
        }
        
        public ProductDto getProduct() {
            return product;
        }
        
        public GetProductOutput setProduct(ProductDto product) {
            this.product = product;
            return this;
        }
    }
}

// Command UseCase 的正確結構
public interface CreateProductUseCase extends Command<CreateProductUseCase.CreateProductInput, CqrsOutput> {
    
    class CreateProductInput implements Input {
        public String productId;
        public String name;
        
        public static CreateProductInput create() {
            return new CreateProductInput();
        }
    }
    // Command 直接使用 CqrsOutput，不需要自訂 Output
}
```

##### ❌ 錯誤範例：
```java
// 錯誤：Input 和 Output 作為獨立檔案
// GetProductInput.java - 不應該存在
public class GetProductInput implements Input { ... }

// GetProductOutput.java - 不應該存在  
public class GetProductOutput extends CqrsOutput<GetProductOutput> { ... }

// GetProductUseCase.java - 缺少 inner classes
public interface GetProductUseCase extends Query<GetProductInput, GetProductOutput> {
    // 缺少 Input 和 Output 的 inner class 定義
}
```

**檢查項目**：
- [ ] **MUST**: Input 是 UseCase interface 的 inner class
- [ ] **MUST**: Query 的 Output 也是 UseCase interface 的 inner class
- [ ] **MUST**: Command 使用標準 CqrsOutput（不需要自訂 Output）
- [ ] **MUST**: 不存在獨立的 Input/Output 檔案
- [ ] **MUST**: Service 實作時使用完整的類別名稱（如 `GetProductUseCase.GetProductInput`）

### UseCase 基本檢查
- [ ] Command 繼承 `Command<Input, CqrsOutput>`
- [ ] Query 繼承 `Query<Input, Output>`
- [ ] 介面只有一個方法
- [ ] 方法名稱清晰表達意圖

### Service Implementation
```java
// 檢查項目範例
public class CreatePlanService implements CreatePlanUseCase {
    // ✗ 不要添加 @Component 或 @Service 註解
    // ✗ 不使用 @Transactional（Event Sourcing 不需要）
    // ✓ 建構子注入
    // ✓ 使用 generic Repository<T, ID>
    private final Repository<Plan, PlanId> repository;
    
    public CreatePlanService(Repository<Plan, PlanId> repository) {
        requireNotNull("repository", repository);
        this.repository = repository;
    }
}
```

- [ ] **🚨 重要：Use Case 實作類別「不可以」加 @Component 或 @Service** (2024-08-17 強化)
  - [ ] Use Case Service 必須在 `UseCaseConfiguration` 中用 `@Bean` 方法註冊
  - [ ] Service 類別本身保持 POJO，不依賴 Spring 註解
  - [ ] 確保依賴注入透過 Configuration 類別統一管理
  - [ ] 檢查 `UseCaseConfiguration` 是否包含對應的 Bean 宣告
  
  ```java
  // ✅ 正確：在 UseCaseConfiguration 中宣告
  @Bean
  public ConfigScrumBoardTaskStateUseCase configScrumBoardTaskStateUseCase(
          SprintRepository sprintRepository) {
      return new ConfigScrumBoardTaskStateService(sprintRepository);
  }
  
  // ❌ 錯誤：Service 類別上加 @Component
  @Component  // 不要這樣做！
  public class ConfigScrumBoardTaskStateService implements ConfigScrumBoardTaskStateUseCase {
  ```
- [ ] **不使用** `@Transactional`（Event Sourcing 專案不需要）
- [ ] 建構子注入依賴
- [ ] 使用 generic `Repository<T, ID>` 而非自定義 Repository 介面
- [ ] 使用 `requireNotNull` 驗證依賴
- [ ] 正確處理領域事件

### 🚨 Use Case Service 實作結構（必須遵守）
```java
// ✅ 正確：完整的 Service 實作模式
@Override
public CqrsOutput execute(EstimateTaskInput input) {
    // 1️⃣ Contracts 驗證（在 try-catch 外）
    requireNotNull("input", input);
    requireNotNull("productId", input.productId);
    requireNotNull("pbiId", input.pbiId);
    requireNotNull("taskId", input.taskId);
    requireNotNull("estimatedHours", input.estimatedHours);
    
    // 2️⃣ 整個 method body 都在一個大的 try-catch 內
    try {
        var output = CqrsOutput.create();
        
        // Step 1: Load the aggregate
        ProductBacklogItem pbi = repository.findById(PbiId.valueOf(input.pbiId)).orElse(null);
        if (null == pbi) {
            output.setId(input.taskId)
                  .setExitCode(ExitCode.FAILURE)
                  .setMessage("Estimate task failed: product backlog item not found, pbi id = " + input.pbiId);
            return output;
        }
        
        // Step 2: Execute business logic
        TaskId taskId = TaskId.valueOf(input.taskId);
        EstimatedHours estimatedHours = EstimatedHours.valueOf(input.estimatedHours);
        pbi.estimateTask(taskId, estimatedHours);
        
        // Step 3: Save the aggregate
        repository.save(pbi);
        
        // Step 4: Return success
        output.setId(input.taskId).setExitCode(ExitCode.SUCCESS);
        return output;
        
    } catch (Exception e) {
        throw new UseCaseFailureException(e.getMessage());
    }
}

// ❌ 錯誤1：output 在 try block 外
public CqrsOutput execute(EstimateTaskInput input) {
    requireNotNull("input", input);
    var output = CqrsOutput.create();  // ❌ 應該在 try block 內
    try {
        // ...
    }
}

// ❌ 錯誤2：使用 orElseThrow
public CqrsOutput execute(EstimateTaskInput input) {
    try {
        ProductBacklogItem pbi = repository.findById(PbiId.valueOf(input.pbiId))
            .orElseThrow(() -> new UseCaseFailureException("PBI not found"));  // ❌
    }
}

// ❌ 錯誤3：部分邏輯在 try block 外
public CqrsOutput execute(EstimateTaskInput input) {
    requireNotNull("input", input);
    PbiId pbiId = PbiId.valueOf(input.pbiId);  // ❌ 應該在 try block 內
    try {
        // ...
    }
}
```

- [ ] **MUST**: Contracts (`requireNotNull`) 在 try-catch **外**
- [ ] **MUST**: 所有其他程式碼都在 try-catch **內**（包括 `var output = CqrsOutput.create()`）
- [ ] **MUST**: 使用 `findById().orElse(null)` 而非 `orElseThrow()`
- [ ] **MUST**: 當找不到 Aggregate 時，返回失敗的 CqrsOutput
- [ ] **MUST**: 設定明確的錯誤訊息說明失敗原因
- [ ] **MUST**: 設定 `ExitCode.FAILURE`
- [ ] **MUST**: 仍然返回相關的 ID 供追蹤
- [ ] 返回適當的 Output

### Input/Output DTOs
```java
// 檢查項目範例
public record CreatePlanInput(
    @NotNull String name,
    @NotNull String userId
) implements Input {
    // ✓ 實作 Input 介面
    // ✓ 使用 Bean Validation
    // ✓ 使用 record
}
```

- [ ] Input 實作 `Input` 介面
- [ ] 使用 Bean Validation 註解
- [ ] 不包含業務邏輯
- [ ] 適當的欄位命名

## 🔌 Adapter 層檢查

### Controller

#### 🔴 Spring @RequestMapping 註解檢查（2024-08-15 更新）
**必須正確使用 @RequestMapping 避免路徑映射錯誤**

- [ ] **單一端點 Controller 是否直接在方法層級指定完整路徑？**
  - ✅ 正確：`@PutMapping("/v1/api/pbis/{pbiId}/tasks/{taskId}/reestimate")`
  - ❌ 錯誤：class 有 `@RequestMapping` + method 有 `@PutMapping`（無路徑）
- [ ] **多端點 Controller 是否正確分配路徑？**
  - ✅ 正確：class `@RequestMapping("/v1/api/products")` + method `@GetMapping("/{id}")`
  - ❌ 錯誤：class 有完整路徑，method 無路徑可映射
- [ ] **測試是否全部返回 404？**
  - 如果所有測試都是 404，檢查 @RequestMapping 配置
- [ ] **Spring Boot 啟動時是否顯示端點映射？**
  - 檢查 console 輸出確認端點已註冊
- [ ] **Integration Test 是否有 @DirtiesContext 註解？**
  - 當測試單獨執行成功但全部執行失敗時，需要加上 @DirtiesContext
- [ ] **測試是否有隔離問題？**
  - 單獨執行：`mvn test -Dtest=SpecificControllerIntegrationTest`
  - 全部執行：`mvn test`
  - 如果結果不同，表示有測試隔離問題

#### 🔴 REST API 路徑設計檢查（2024-08-15 更新）
**用巢狀的建立端點、用扁平的資源位址**

- [ ] **建立端點是否使用巢狀路徑？**
  - ✅ 正確：`POST /v1/api/products/{productId}/pbis`
  - ❌ 錯誤：`POST /v1/api/pbis` (body 含 productId)
- [ ] **資源位址是否使用扁平路徑？**
  - ✅ 正確：`GET/PATCH/DELETE /v1/api/pbis/{pbiId}`
  - ❌ 錯誤：`GET /v1/api/products/{productId}/pbis/{pbiId}`
- [ ] **父資源不存在時是否返回 404？**
  - 必須檢查並返回適當的錯誤碼（如 PRODUCT_NOT_FOUND）
- [ ] **路徑設計是否符合 Aggregate Root 的獨立性？**
  - 每個 Aggregate Root 應有獨立的資源路徑

#### 基本 Controller 檢查
```java
// 檢查項目範例
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {
    // ✓ RESTful URL 設計
    // ✓ 適當的 HTTP 方法
    // ✓ 統一的錯誤處理
}
```

- [ ] RESTful URL 設計
- [ ] 正確的 HTTP 方法（GET/POST/PUT/DELETE）
- [ ] 適當的 HTTP 狀態碼
- [ ] Request/Response DTO 分離
- [ ] 統一的錯誤格式

### Repository 使用規則
```java
// ⚠️ 重要：不要創建自定義 Repository 介面！
// ❌ 錯誤：
// public interface PlanRepository extends Repository<Plan, PlanId> { }

// ✅ 正確：直接使用 generic Repository
@Service
public class SomeService {
    private final Repository<Plan, PlanId> repository;
    
    public SomeService(Repository<Plan, PlanId> repository) {
        this.repository = repository;
    }
}
```

- [ ] **不創建**自定義 Repository 介面（如 PlanRepository、TagRepository）
- [ ] 直接注入 generic `Repository<T, ID>`
- [ ] 只使用三個標準方法：`findById`、`save`、`delete`
- [ ] 查詢需求使用 Projection 或 Query Service
- [ ] 測試使用 `GenericInMemoryRepository`

### JPA Entities
```java
// 檢查項目範例
@Entity
@Table(name = "plan")
public class PlanData implements OutboxData<String> {
    // ✓ @Entity 和 @Table 註解
    // ✓ 實作 OutboxData（如需要）
    // ✓ 使用 EAGER loading
}
```

- [ ] 適當的 JPA 註解
- [ ] **永遠使用 EAGER loading**
- [ ] 正確的關聯映射
- [ ] 包含版本欄位（@Version）
- [ ] 適當的級聯設定

### 🔴 JPA Projection 配置檢查（2024-08-24 新增）
**新增 JPA Projection 時必須檢查 Spring 配置**

#### 必要檢查項目：
- [ ] **JPA Projection 介面不應該有 @Repository 註解**
  ```java
  // ⚠️ 重要：不要加 @Repository，Spring Data JPA 會自動管理
  public interface JpaProductsProjection extends ProductsProjection, JpaRepository<ProductData, String> {
  ```
  
- [ ] **套件路徑是否已加入 JpaConfiguration？**
  ```java
  @EnableJpaRepositories(basePackages = {
      // ... 其他套件 ...
      "tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection",  // ✅ 必須包含
  })
  ```
  
- [ ] **選擇合適的 Bean 管理方式**
  - 方式一：透過 `@EnableJpaRepositories` 自動掃描（推薦）
  - 方式二：在 Configuration 類別中明確宣告 `@Bean`（需要特殊配置時）
  
- [ ] **Spring Boot 啟動時是否能找到 bean？**
  - 錯誤訊息：`Field xxx required a bean of type 'JpaXxxProjection' that could not be found`
  - 解決方法：檢查 `@EnableJpaRepositories` 配置或明確宣告 bean

#### 檢查步驟：
1. 確認 JPA Projection 在正確套件位置
2. **確認沒有 @Repository 註解**（Spring Data JPA 自動管理）
3. 確認 JpaConfiguration 包含該套件路徑
4. 測試 Spring Boot 啟動

## 🧪 測試檢查

### 🚨 測試資料 ID 使用規範（2024-08-31 新增）
**強制規定：測試中的實體 ID 必須使用 UUID 避免衝突**

#### ✅ 正確範例：
```java
@EzScenario
public void should_create_product_successfully() {
    feature.newScenario()
        .Given("valid input", env -> {
            CreateProductInput input = CreateProductInput.create();
            input.id = UUID.randomUUID().toString();  // ✅ 使用 UUID
            input.name = "Test Product";
            env.put("productId", input.id);  // 儲存供後續使用
        })
        .When("execute use case", env -> {
            String productId = env.gets("productId");  // 取得 ID
            // ...
        })
        .Execute();
}
```

#### ❌ 錯誤範例：
```java
// ❌ 錯誤：使用固定 ID
input.id = "product-1";   // 會造成 ID 重複錯誤
input.id = "test-123";     // 會造成測試失敗

// ❌ 錯誤：沒有儲存 ID 供後續使用
input.id = UUID.randomUUID().toString();
// 後續步驟無法取得這個 ID
```

#### 檢查項目：
- [ ] **MUST**: 所有實體 ID 使用 `UUID.randomUUID().toString()`
- [ ] **MUST**: ID 存入 env 變數：`env.put("productId", input.id)`
- [ ] **MUST**: 後續步驟使用 `env.gets("productId")` 取得 ID
- [ ] **MUST NOT**: 不使用固定字串 ID（如 "product-1", "test-123"）
- [ ] **MUST**: 多個實體使用不同的 UUID

### 🚨 Controller 測試品質檢查（2024-08-15 新增）
**必須避免無意義的測試，專注於業務價值**

#### 無意義測試識別
- [ ] **沒有重複的 Bean Validation 測試**
  - ❌ 多個 `@NotBlank` 欄位各自有測試
  - ❌ 多個 `@Size` 欄位各自有測試
  - ✅ 每種驗證機制只有一個代表性測試

- [ ] **沒有過度詳細的 JSON 結構檢查**
  - ❌ 檢查每個 DTO 欄位的具體值
  - ❌ 檢查巢狀物件的內部細節
  - ✅ 只驗證關鍵欄位存在

- [ ] **沒有重複的功能變化測試**
  - ❌ 分別測試有/無 header 的相同功能
  - ❌ 分別測試不同欄位的相同驗證邏輯
  - ✅ 合併相似場景為一個測試

#### 有意義測試確認
- [ ] **測試 Controller 的核心責任**
  - ✅ HTTP 映射（狀態碼、headers）
  - ✅ UseCase 整合（input 轉換、output 處理）
  - ✅ 錯誤處理（將業務錯誤映射為 HTTP 錯誤）

- [ ] **測試覆蓋必要場景**
  - ✅ 成功場景（200/201/202）
  - ✅ 業務錯誤映射（404/409/400）
  - ✅ 一個 Bean Validation 代表性測試
  - ✅ UseCase 異常處理（500）

- [ ] **測試名稱清楚表達目的**
  - ✅ 測試名稱明確說明測試的業務場景
  - ✅ 避免技術細節，聚焦業務價值

### 單元測試
```java
// 檢查項目範例
@EzFeature("Plan Management")
public class CreatePlanUseCaseTest {
    @EzScenario("Successfully create a plan")
    void testCreatePlan() {
        // Given-When-Then 結構
    }
}
```

- [ ] 使用 ezSpec 註解（@EzFeature, @EzScenario）
- [ ] Given-When-Then 結構
- [ ] 測試名稱描述場景
- [ ] Mock 外部依賴
- [ ] 驗證所有重要行為
- [ ] **🚨 測試失敗時不可直接修改 Given-When-Then**
- [ ] **測試失敗需分析原因並尋求人類確認**
- [ ] 使用 `GenericInMemoryRepository` 而非自定義實作

#### 🔴 Use Case 測試與 Aggregate/Repository 互動規範（2024-08-31 強制規定）
**必須遵守**: Given 和 When 區塊不能直接與 Aggregate 或 Repository 互動，但 Then 和後續 And 區塊可以。

##### 階段限制說明：
- **Given/When 階段**：設置和執行階段，必須透過 Use Case interface（模擬真實使用）
- **Then/And 階段**：驗證階段，可以直接查詢 Aggregate 狀態（深入驗證實作）

##### ❌ 錯誤範例（Given/When 階段）：
```java
// 錯誤：在 Given 階段直接創建 Aggregate
.Given("PBI exists", env -> {
    ProductBacklogItem pbi = new ProductBacklogItem(...);  // ❌ 直接創建
    pbiRepository.save(pbi);  // ❌ 直接保存
})

// 錯誤：在 When 階段直接操作 Aggregate
.When("changing description", env -> {
    ProductBacklogItem pbi = pbiRepository.findById(pbiId).orElseThrow();  // ❌ 直接查詢
    pbi.changeDescription("New description");  // ❌ 直接呼叫
    pbiRepository.save(pbi);  // ❌ 直接保存
})
```

##### ✅ 正確範例：
```java
// Given 階段：透過 Use Case 設置
.Given("PBI exists", env -> {
    CreateProductBacklogItemInput input = CreateProductBacklogItemInput.create();
    input.pbiId = "pbi-1";
    input.name = "User Story";
    createProductBacklogItemUseCase.execute(input);  // ✅ 透過 Use Case
})

// When 階段：透過 Use Case 執行
.When("changing description", env -> {
    ChangeDescriptionInput input = ChangeDescriptionInput.create();
    input.pbiId = "pbi-1";
    input.newDescription = "Updated description";
    var output = changeDescriptionUseCase.execute(input);  // ✅ 透過 Use Case
    env.put("output", output);
})

// Then 階段：可以直接驗證
.Then("operation succeeds", env -> {
    var output = env.get("output", CqrsOutput.class);
    assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
})

// And 階段：可以直接查詢 Aggregate
.And("PBI aggregate should have new description", env -> {
    PbiId pbiId = PbiId.valueOf("pbi-1");
    ProductBacklogItem pbi = pbiRepository.findById(pbiId).orElseThrow();  // ✅ Then/And 可直接查詢
    assertThat(pbi.getDescription()).isEqualTo("Updated description");  // ✅ 可直接驗證
})

// 也可以透過事件驗證
.And("event should be published", env -> {
    List<DomainEvent> publishedEvents = getCapturedEvents();
    // ... 驗證事件
})
```

**檢查項目**:
- [ ] **MUST**: Given 階段不直接創建或操作 Aggregate
- [ ] **MUST**: When 階段不直接呼叫 Aggregate 方法
- [ ] **MUST**: Given/When 只透過 Use Case interface 操作
- [ ] **MAY**: Then/And 階段可以直接查詢 Repository
- [ ] **MAY**: Then/And 階段可以直接檢查 Aggregate 狀態
- [ ] **SHOULD**: 同時使用事件驗證和狀態驗證確保完整性
- [ ] **違反 Given/When 規則必須標記為 MUST FIX**

##### 🔴 事件清除的正確時機（2024-08-31 新增）
**必須遵守**: 在 Given 階段執行 Use Case 後，若需要清除事件，必須先等待事件被捕獲。

```java
// ❌ 錯誤：競態條件風險
.Given("product exists", env -> {
    createProductUseCase.execute(input);
    clearCapturedEvents();  // ❌ 事件可能還沒被捕獲
})

// ✅ 正確：確保事件已被捕獲
.Given("product exists", env -> {
    createProductUseCase.execute(input);
    
    // 等待事件被捕獲
    await().untilAsserted(() -> 
        assertEquals(1, fakeEventListener.capturedEvents.size())
    );
    
    // 現在可以安全清除
    clearCapturedEvents();
})
```

**檢查重點**:
- [ ] **MUST**: execute() 後不能立即 clearCapturedEvents()
- [ ] **MUST**: 使用 await() 等待事件被捕獲
- [ ] **MUST**: 確認事件數量正確後才清除
- [ ] **WHY**: 事件發布是異步的，避免間歇性測試失敗

#### 🔴 ezSpec 測試語法規範（強制規定）
**必須遵守**: 所有 `@EzScenario` 測試必須以 `.Execute();` 結尾。

##### ❌ 錯誤範例：
```java
@EzScenario
public void should_create_plan() {
    feature.newScenario("Should create plan")
        .withRule(SUCCESS_RULE)
        .Given("valid input", env -> { /* setup */ })
        .When("creating plan", env -> { /* action */ })
        .Then("plan should be created", env -> { /* assertion */ });
        // 缺少 .Execute() - 測試不會執行！
}
```

##### ✅ 正確範例：
```java
@EzScenario
public void should_create_plan() {
    feature.newScenario("Should create plan")
        .withRule(SUCCESS_RULE)
        .Given("valid input", env -> { /* setup */ })
        .When("creating plan", env -> { /* action */ })
        .Then("plan should be created", env -> { /* assertion */ })
        .Execute();  // 必須以 .Execute() 結尾
}
```

**檢查項目**:
- [ ] **MUST**: 每個 @EzScenario 測試都以 `.Execute();` 結尾
- [ ] **MUST**: `.Execute()` 在最後一個 Then 或 And 之後
- [ ] **MUST**: 不要忘記分號 `.Execute();`
- [ ] **違反此規則必須標記為 MUST FIX**

#### 🔴 Use Case 測試 Given 階段規範（2024-08-13 強制規定）
**必須遵守**: Use Case 測試的 Given 階段只能透過呼叫（其他）Use Case 來設定測試資料，不可直接創建或操作 Aggregate。

##### ❌ 錯誤範例：
```java
// 不要在 Given 中直接創建 Aggregate
.Given("a product exists", env -> {
    Product product = new Product(productId, ProductName.valueOf("Test Product"));
    ctx.productRepository().save(product);  // 直接保存
})

// 不要在 Given 中直接操作 Aggregate
.Given("a PBI with estimate exists", env -> {
    ProductBacklogItem pbi = ctx.repository().findById(pbiId).orElseThrow();
    pbi.estimatePbi(new Estimate(EstimateType.STORY_POINT, "5"));  // 直接呼叫
    ctx.repository().save(pbi);
})
```

##### ✅ 正確範例：
```java
// 透過 Use Case 設定測試資料
.Given("a product exists", env -> {
    CreateProductUseCase.CreateProductInput input = CreateProductUseCase.CreateProductInput.create();
    input.id = "product-1";
    input.name = "Test Product";
    input.creatorId = "user-123";
    
    createProductUseCase.execute(input);  // 透過 use case 創建
})

// 透過多個 Use Case 設定複雜測試資料
.Given("a PBI with estimate exists", env -> {
    // 首先創建 PBI
    CreateProductBacklogItemUseCase.CreateProductBacklogItemInput createInput = 
        CreateProductBacklogItemUseCase.CreateProductBacklogItemInput.create();
    createInput.productId = "product-1";
    createInput.pbiId = "pbi-1";
    createInput.name = "Story";
    createProductBacklogItemUseCase.execute(createInput);
    
    // 然後估算 PBI
    EstimateProductBacklogItemUseCase.EstimateProductBacklogItemInput estimateInput = 
        EstimateProductBacklogItemUseCase.EstimateProductBacklogItemInput.create();
    estimateInput.productId = "product-1";
    estimateInput.pbiId = "pbi-1";
    estimateInput.estimateType = "STORY_POINT";
    estimateInput.estimateValue = "5";
    estimateProductBacklogItemUseCase.execute(estimateInput);
})
```

**檢查項目**:
- [ ] **MUST**: Given 階段透過 Use Case 設定所有測試資料
- [ ] **MUST**: 不在 Given 中直接創建 Aggregate 實例
- [ ] **MUST**: 不在 Given 中直接呼叫 Aggregate 的業務方法
- [ ] **MUST**: 不在 Given 中直接保存 Aggregate 到 repository
- [ ] **MUST**: 透過相關 Use Case 建立測試資料的依賴關係
- [ ] **MUST**: When/Then 階段也不可直接操作 Aggregate
- [ ] **違反此規則必須標記為 MUST FIX**

#### 🔴 測試事件檢查規範（2024-08-13 強制規定）
**必須遵守**: 所有 Use Case 測試中的事件檢查必須透過 MessageBus 監聽，不可直接檢查 Aggregate 的 `getDomainEvents()`。

##### ❌ 錯誤範例：
```java
// 不要直接檢查 Aggregate 的 domain events
.And("event should be published", env -> {
    ProductBacklogItem pbi = ctx.repository.findById(pbiId).orElseThrow();
    List<ProductBacklogItemEvents> events = pbi.getDomainEvents();
    assertThat(events).hasSizeGreaterThan(0);
    ProductBacklogItemEvents lastEvent = events.get(events.size() - 1);
    assertThat(lastEvent).isInstanceOf(ProductBacklogItemEvents.PbiEstimated.class);
});
```

##### ✅ 正確範例：
```java
// 透過 MessageBus 監聽事件
.And("event should be published", env -> {
    List<DomainEvent> publishedEvents = ctx.getPublishedEvents();
    ProductBacklogItemEvents.PbiEstimated estimatedEvent = publishedEvents.stream()
        .filter(e -> e instanceof ProductBacklogItemEvents.PbiEstimated)
        .map(e -> (ProductBacklogItemEvents.PbiEstimated) e)
        .findFirst()
        .orElse(null);
    
    assertThat(estimatedEvent).isNotNull();
    assertThat(estimatedEvent.pbiId()).isEqualTo(expectedPbiId);
});
```

**檢查項目**:
- [ ] **MUST**: 事件檢查透過 `ctx.getPublishedEvents()` 從 MessageBus 獲取
- [ ] **MUST**: 不直接呼叫 `aggregate.getDomainEvents()`
- [ ] **MUST**: 使用 stream filtering 找到特定事件類型
- [ ] **MUST**: 驗證事件的關鍵屬性值
- [ ] **MUST**: 在需要時使用 `ctx.clearPublishedEvents()` 清除先前事件
- [ ] **違反此規則必須標記為 MUST FIX**

### 測試覆蓋率
- [ ] UseCase 100% 覆蓋
- [ ] Domain 邏輯 100% 覆蓋
- [ ] 錯誤情況都有測試
- [ ] 邊界條件測試

### 測試品質
- [ ] 測試獨立執行
- [ ] 測試執行快速
- [ ] 測試可重複執行
- [ ] 清晰的斷言訊息

## 📦 Service 實作檢查

### Service 位置規範
```java
// ✅ 正確：Service 必須在 usecase.service 套件
package tw.teddysoft.aiplan.plan.usecase.service;
```

- [ ] Service 類別位於 `[aggregate].usecase.service` 套件
- [ ] **不可**放在 `usecase.port.in.service` 或其他位置
- [ ] Service 類別名稱格式：`[Operation]Service`

### Service 建構與錯誤處理
```java
// ✅ 正確：手動建構函數 + try-catch
public class CreateTaskService implements CreateTaskUseCase {
    private final Repository<Plan, PlanId> repository;
    
    public CreateTaskService(Repository<Plan, PlanId> repository) {
        requireNotNull("repository", repository);
        this.repository = repository;
    }
    
    @Override
    public CqrsOutput execute(CreateTaskInput input) {
        try {
            // 業務邏輯
        } catch (Exception e) {
            throw new UseCaseFailureException(e);
        }
    }
}
```

- [ ] 使用手動建構函數（不用 @AllArgsConstructor）
- [ ] 建構函數使用 `requireNotNull` 驗證依賴
- [ ] execute 方法包含 try-catch 區塊
- [ ] 捕獲異常並拋出 `UseCaseFailureException`

## 🎯 Input/Output 設計檢查

### Input 類別位置
```java
// ✅ 正確：Input 是 UseCase 的 inner class
public interface CreateTaskUseCase extends Command<CreateTaskUseCase.CreateTaskInput, CqrsOutput> {
    class CreateTaskInput implements Input {
        public PlanId planId;
        public String taskName;
        
        public static CreateTaskInput create() {
            return new CreateTaskInput();
        }
    }
}
```

- [ ] Input 必須是 UseCase interface 的 inner class
- [ ] **絕對不可**產生獨立的 Input 檔案
- [ ] Input 實作 `Input` 介面
- [ ] 提供 `create()` 靜態工廠方法

### Output 設計（Query）
```java
// ✅ 正確：Query 的 Output 也是 inner class
public interface GetPlansUseCase extends Query<GetPlansUseCase.GetPlansInput, GetPlansUseCase.GetPlansOutput> {
    class GetPlansOutput extends CqrsOutput {
        public List<PlanDto> plans;
        // getter/setter
    }
}
```

- [ ] Query 的 Output 是 UseCase 的 inner class
- [ ] Output 繼承 `CqrsOutput`
- [ ] Command 直接返回 `CqrsOutput`（不需要自定義 Output）

## 🎯 Spec 對照檢查 (防止過度設計)

### Spec 完整性檢查表
執行程式碼審查時，必須建立對照表確認沒有過度設計：

```markdown
## [UseCase Name] Spec 對照表
| Spec 要求項目 | 實作檔案/類別 | 符合性 |
|-------------|------------|--------|
| Domain Events | | |
| - ProductCreated | ProductEvents.ProductCreated | ✅ |
| - ~~其他未定義事件~~ | 不應存在 | ⚠️ |
| Entities | | |
| - Product | Product.java | ✅ |
| Value Objects | | |
| - ProductId | ProductId.java | ✅ |
| Business Methods | | |
| - constructor | new Product(...) | ✅ |
| - ~~其他未定義方法~~ | 不應存在 | ⚠️ |
```

### 檢查要點
- [ ] **MUST**: 建立 Spec 對照表
- [ ] **MUST**: 每個實作項目都能在 spec 中找到對應
- [ ] **MUST**: 標記並移除 spec 未要求的實作
- [ ] **MUST**: Domain Events 數量與 spec 完全一致
- [ ] 業務方法與 spec 描述一致
- [ ] 沒有「為了未來」而預留的介面或方法

## 🔀 Event Handler 模式檢查

### Switch Expression Pattern Matching
```java
// ✅ 正確：使用 switch expression
@Override
protected void when(PlanEvents event) {
    switch (event) {
        case PlanEvents.PlanCreated e -> {
            this.planId = e.planId();
            this.name = e.name();
        }
        case PlanEvents.TaskCreated e -> {
            // 處理邏輯
        }
        default -> {
            // 處理未知事件
        }
    }
}
```

- [ ] Aggregate 的 `when` 方法使用 switch expression
- [ ] **禁止**使用 if-else instanceof 鏈
- [ ] 每個 case 使用 pattern matching（`case Type variable ->`）
- [ ] 包含 default 處理未知事件

## 📚 Framework Import 檢查

### 正確的 Import 路徑
```java
// ✅ 正確的 ezddd framework imports
import tw.teddysoft.ezddd.entity.Entity;
import tw.teddysoft.ezddd.entity.ValueObject;
import tw.teddysoft.ezddd.entity.EsAggregateRoot;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.cqrs.usecase.query.Query;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
```

- [ ] Entity 從 `tw.teddysoft.ezddd.entity` 導入
- [ ] ValueObject 從 `tw.teddysoft.ezddd.entity` 導入
- [ ] Command/Query 從 `tw.teddysoft.ezddd.cqrs.usecase` 導入
- [ ] **不要**從 `domain` 套件導入（舊版路徑）

## ⚡ 效能檢查

### 查詢效能
- [ ] 使用 Projection 而非載入整個 Aggregate
- [ ] 避免 N+1 查詢問題
- [ ] 適當的索引使用
- [ ] 批次操作優化

### 記憶體使用
- [ ] 避免載入過多資料
- [ ] 及時釋放資源
- [ ] 使用分頁處理大量資料
- [ ] 避免記憶體洩漏

## 🔒 安全性檢查

### 輸入驗證
- [ ] 所有輸入都經過驗證
- [ ] 防止 SQL Injection
- [ ] 防止 XSS 攻擊
- [ ] 適當的權限檢查

### 敏感資訊
- [ ] 不記錄敏感資訊
- [ ] 不在錯誤訊息中暴露內部細節
- [ ] 密碼適當加密
- [ ] API 金鑰不寫死在程式碼

## 📚 文檔檢查

### 程式碼註解
- [ ] 複雜邏輯有註解說明
- [ ] 公開 API 有 JavaDoc
- [ ] 不包含過時的註解
- [ ] 註解描述「為什麼」而非「什麼」

### README 更新
- [ ] 新功能在 README 中說明
- [ ] API 變更有文檔
- [ ] 配置變更有說明

### 🔴 Task 檔案更新（2024-08-15 新增）
**必須遵守**: 執行完 task 後必須更新對應的 task 檔案記錄執行結果

#### ✅ 正確範例：
```json
{
  "id": "task-name",
  "status": "done",  // 從 "todo" 改為 "done"
  "results": [
    {
      "timestamp": "2024-08-15T10:30:00+08:00",
      "status": "done",
      "summary": "Successfully implemented with comprehensive testing",
      "outputFiles": [
        "產生的檔案列表"
      ],
      "testResults": "測試結果描述",
      "postChecksResults": {
        "審查結果詳情"
      }
    }
  ]
}
```

#### 檢查項目：
- [ ] **MUST**: Task 執行完畢後立即更新 task 檔案
- [ ] **MUST**: 將 status 從 "todo" 改為 "done"
- [ ] **MUST**: 在 results 陣列中新增執行結果記錄
- [ ] **MUST**: 記錄所有產生的檔案列表
- [ ] **MUST**: 記錄測試執行結果
- [ ] **MUST**: 記錄 post-check 結果（如有）
- [ ] **MUST**: 包含正確的時間戳記
- [ ] **MUST**: 記錄已修正的問題和待處理的問題

## 🚀 審查流程

### 1. 自動檢查
```bash
# 編譯檢查
mvn clean compile

# 測試檢查
mvn test

# 程式碼品質檢查（如有配置）
mvn sonar:sonar
```

### 2. 手動審查優先順序
1. **業務邏輯正確性**
2. **測試完整性**
3. **效能影響**
4. **安全性考量**
5. **程式碼可讀性**

### 3. 常見拒絕原因
- ❌ 缺少測試
- ❌ 破壞既有測試
- ❌ 不遵循專案規範
- ❌ 效能嚴重退化
- ❌ 安全性漏洞

## 🔄 Mapper 實作檢查

### Mapper 套件位置與設計規範（2024-08-14 更新）

#### 套件位置
```java
// ✅ 正確：Mapper 必須放在 usecase.port 套件
package tw.teddysoft.aiscrum.product.usecase.port;

public class ProductMapper {
    public static ProductDto toDto(Product product) { ... }
    public static ProductData toData(Product product) { ... }
    public static Product toDomain(ProductData data) { ... }
}

// ❌ 錯誤：不要放在其他套件
package tw.teddysoft.aiscrum.product.adapter.out.mapper;  // ❌ 錯誤
package tw.teddysoft.aiscrum.product.usecase.mapper;      // ❌ 錯誤
```

#### 使用方式
```java
// ✅ 正確：直接呼叫靜態方法
public class JpaProductDtoProjection implements ProductDtoProjection {
    private final Repository<Product, ProductId> productRepository;
    
    // 不需要注入 Mapper
    public JpaProductDtoProjection(Repository<Product, ProductId> productRepository) {
        this.productRepository = productRepository;
    }
    
    @Override
    public Optional<ProductDto> query(Input input) {
        return productRepository.findById(id)
            .map(ProductMapper::toDto);  // 直接使用方法引用
    }
}

// ❌ 錯誤：不要注入 Mapper
public class JpaProductDtoProjection implements ProductDtoProjection {
    private final ProductMapper mapper;  // ❌ 不需要
    
    public JpaProductDtoProjection(Repository repository, ProductMapper mapper) {
        this.mapper = mapper;  // ❌ 過度設計
    }
    
    public Optional<ProductDto> query(Input input) {
        return repository.findById(id).map(mapper::toDto);  // ❌ 不必要的複雜性
    }
}

// ❌ 錯誤：不要使用 Spring 註解
@Component  // ❌ 不需要
public class ProductMapper {
    public ProductDto toDto(Product product) { ... }  // ❌ 應該是 static
}
```

### Mapper 檢查項目
- [ ] **MUST**: Mapper 類別位於 `[aggregate].usecase.port` 套件
- [ ] **MUST**: 不可放在 `adapter.out.mapper` 套件（adapter 層不應包含轉換邏輯）
- [ ] **MUST**: 不可放在 `usecase.mapper` 子套件（應該在 port 層級）
- [ ] **MUST**: 每個 DTO 都有獨立的 Mapper（一個 DTO 一個 Mapper 原則）
- [ ] **MUST**: 不可在一個 Mapper 中處理多個不相關 DTO 的轉換
- [ ] **MUST**: 所有 Mapper 方法必須是 `public static`（無狀態工具類）
- [ ] **MUST**: Mapper 類別不可有 `@Component` 或 `@Service` 註解
- [ ] **MUST**: 不可透過依賴注入使用 Mapper（直接呼叫靜態方法）
- [ ] **MUST**: 包含 null 檢查（使用 Objects.requireNonNull 而非 Contract）
- [ ] **MUST**: 嵌套物件使用對應的 Mapper 處理（如 ProductMapper 呼叫 DefinitionOfDoneMapper）
- [ ] **SHOULD**: 提供批次轉換方法（如 `List<Dto> toDto(List<Entity>)`）
- [ ] **SHOULD**: Aggregate Mapper 包含 `newMapper()` 方法（實作 OutboxMapper）
- [ ] **SHOULD**: Entity Mapper 不包含 `newMapper()` 方法

### 為什麼 Mapper 要在 usecase.port？
1. **Clean Architecture 原則**: Mapper 是 Use Case 層的一部分，負責轉換業務物件
2. **關注點分離**: Adapter 層只負責技術實現，不應包含業務轉換邏輯
3. **依賴方向**: UseCase 層可以使用 Mapper，Adapter 層也可以使用（依賴內層）
4. **一致性**: 與 DTO、Projection Interface 在同一層級，保持架構一致

## 🏗️ Projection 實作檢查

### Projection 雙檔案原則
```java
// ✅ 必須同時產生兩個檔案：
// 1. Interface (usecase.port.out.projection)
public interface PlanDtosProjection extends Projection<...> { }

// 2. JPA Implementation (adapter.out.projection)
@Repository
public interface JpaPlanDtosProjection extends JpaRepository<...> { }
```

- [ ] 產生 Projection Interface（在 usecase.port.out.projection）
- [ ] 產生 JPA Implementation（在 adapter.out.projection）
- [ ] JPA 實作有 `@Query` 註解
- [ ] 使用 Mapper.toDto() 轉換結果
- [ ] **絕不**只產生 interface 而忘記實作

## 💡 審查建議

### 給予建設性回饋
```
// ❌ 不好的回饋
"這段程式碼寫得很糟"

// ✅ 好的回饋
"建議將這個方法拆分成更小的方法，以提高可讀性和可測試性。
例如可以將驗證邏輯抽取為 validateInput() 方法。"
```

### 關注重點
1. **先看測試**：了解預期行為
2. **再看實作**：確認符合預期
3. **最後看整合**：確保不破壞現有功能

## 📊 審查指標

### 程式碼品質指標
- 圈複雜度 < 10
- 方法長度 < 30 行
- 類別長度 < 300 行
- 測試覆蓋率 > 80%

### 時間指標
- 小型 PR（< 100 行）：30 分鐘內
- 中型 PR（100-500 行）：1 小時內
- 大型 PR（> 500 行）：考慮拆分

## 🔗 相關資源

- [CODING-STANDARDS.md](./CODING-STANDARDS.md) - 編碼標準
- [COMMON-MISTAKES-GUIDE.md](./COMMON-MISTAKES-GUIDE.md) - 常見錯誤
- [TEMPLATE-USAGE-GUIDE.md](./TEMPLATE-USAGE-GUIDE.md) - 範本使用
- [AI-BEHAVIOR-GUIDE.md](../../../AI-BEHAVIOR-GUIDE.md) - AI 行為準則