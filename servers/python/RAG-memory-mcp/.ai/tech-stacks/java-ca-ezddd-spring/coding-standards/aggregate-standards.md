# Aggregate 編碼規範

本文件定義 Aggregate、Entity、Value Object 和 Domain Event 的編碼標準。

## ⚠️ 關鍵警告：集合欄位初始化時機

**問題**: 在建構子中於 `super()` 之後初始化集合欄位會導致事件重播的資料被清空！

```java
// ❌ 絕對錯誤：會清空事件重播的資料
public class ScrumTeam extends AggregateRoot<ScrumTeamEvents> {
    private final List<TeamMember> members;
    
    public ScrumTeam(List<ScrumTeamEvents> domainEvents) {
        super(domainEvents);  // 事件重播，members 被填充
        this.members = new ArrayList<>();  // 錯誤！清空了剛重播的資料
    }
}

// ✅ 正確：在欄位宣告時初始化
public class ScrumTeam extends AggregateRoot<ScrumTeamEvents> {
    private final List<TeamMember> members = new ArrayList<>();  // 正確初始化時機
    
    public ScrumTeam(List<ScrumTeamEvents> domainEvents) {
        super(domainEvents);  // 事件重播時 members 已經存在
    }
}
```

## 🔴 必須遵守的規則 (MUST FOLLOW)

### 0. Soft Delete 欄位要求

**強制規定**: 每個 Aggregate 必須支援軟刪除功能：

#### Aggregate Root 必須有 isDeleted 欄位和方法
```java
// ✅ 正確：Aggregate Root 必須實作 isDeleted
public class ProductBacklogItem extends EsAggregateRoot<PbiId, ProductBacklogItemEvents> {
    private boolean deleted = false;  // 必須欄位：軟刪除標記
    
    // 必須有 isDeleted 方法供 Repository 檢查
    public boolean isDeleted() {
        return deleted;
    }
    
    // 在處理刪除事件時設置 deleted = true
    @Override
    protected void when(ProductBacklogItemEvents event) {
        switch (event) {
            case ProductBacklogItemEvents.ProductBacklogItemDeleted e -> {
                this.deleted = true;  // 標記為已刪除
            }
            // 其他事件處理...
        }
    }
}
```

#### AggregateData 必須有 isDeleted 欄位
```java
// ✅ 正確：Data 類別必須有 isDeleted 欄位
@Entity
@Table(name = "product_backlog_items")
public class ProductBacklogItemData extends BaseData {
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;  // 必須欄位：對應 Aggregate 的 deleted 狀態
    
    // getter/setter
    public boolean isDeleted() {
        return isDeleted;
    }
    
    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted;
    }
}
```

#### Aggregate Mapper 必須處理 isDeleted 欄位
**重要**: 只有 Aggregate Root 的 Mapper 需要處理 isDeleted 欄位，Entity 的 Mapper 不需要。

```java
// ✅ 正確：Aggregate Mapper 必須映射 isDeleted 欄位
public class ProductBacklogItemMapper {
    public static ProductBacklogItemData toData(ProductBacklogItem aggregate) {
        var data = new ProductBacklogItemData();
        data.setPbiId(aggregate.getId().value());
        data.setName(aggregate.getName());
        data.setDeleted(aggregate.isDeleted());  // MANDATORY: 必須映射 isDeleted
        // 其他欄位映射...
        return data;
    }
    
    // MANDATORY: toDomain 也必須處理 isDeleted 欄位
    public static ProductBacklogItem toDomain(ProductBacklogItemData data) {
        ProductBacklogItem aggregate;
        
        if (data.getDomainEventDatas() != null && !data.getDomainEventDatas().isEmpty()) {
            // Event sourcing 重建
            var domainEvents = data.getDomainEventDatas().stream()
                .map(DomainEventMapper::toDomain)
                .map(event -> (ProductBacklogItemEvents) event)
                .collect(Collectors.toList());
            aggregate = new ProductBacklogItem(domainEvents);
        } else {
            // 從當前狀態重建
            aggregate = new ProductBacklogItem(
                PbiId.valueOf(data.getPbiId()),
                data.getName(),
                // 其他建構參數...
            );
            
            // MANDATORY: 直接設置 deleted 狀態（當無事件時）
            if (data.isDeleted()) {
                aggregate.setDeleted(data.isDeleted());  // 或透過反射設置私有欄位
            }
        }
        
        aggregate.setVersion(data.getVersion());
        aggregate.clearDomainEvents();
        return aggregate;
    }
}

// ❌ 錯誤：Entity Mapper 不需要處理 isDeleted
public class TaskMapper {  // Task 是 Entity，不是 Aggregate
    public static TaskData toData(Task task) {
        var data = new TaskData();
        // Task 不需要 isDeleted 欄位，因為它不是 Aggregate Root
        return data;
    }
}
```

**適用範圍**：
- ✅ Product → ProductMapper.**toData() 和 toDomain()** **必須**映射 isDeleted
- ✅ Sprint → SprintMapper.**toData() 和 toDomain()** **必須**映射 isDeleted  
- ✅ ScrumTeam → ScrumTeamMapper.**toData() 和 toDomain()** **必須**映射 isDeleted
- ✅ ProductBacklogItem → ProductBacklogItemMapper.**toData() 和 toDomain()** **必須**映射 isDeleted
- ❌ Task → TaskMapper 不需要，因為 Task 是 Entity 不是 Aggregate

**關鍵要求**：
- **toData()**: `data.setDeleted(aggregate.isDeleted())` - 將 Aggregate 的軟刪除狀態保存到 Data
- **toDomain()**: 當無事件重建時，必須從 `data.isDeleted()` 恢復 Aggregate 的軟刪除狀態
```

#### Repository 必須過濾軟刪除的資料
```java
// GenericInMemoryRepository 已實作軟刪除過濾
// JPA Repository 查詢必須排除軟刪除資料
@Query("SELECT p FROM ProductBacklogItemData p WHERE p.isDeleted = false AND ...")
List<ProductBacklogItemData> findActiveItems();

// 或使用 @Where 註解
@Entity
@Where(clause = "is_deleted = false")  // 自動過濾軟刪除資料
public class ProductBacklogItemData extends BaseData {
    // ...
}
```

**違反後果**：
- Code Review 必須失敗 (MUST FIX)
- 軟刪除功能無法正常運作
- 會導致刪除的資料仍然出現在查詢結果中

### 1. Aggregate Command Method 後置條件檢查

**強制規定**: 每個 Aggregate 的 command method 必須使用 `ensure` 檢查：
1. 業務狀態變更的正確性
2. Domain Event 產生的正確性

#### 檢查方式規範
**必須使用簡潔的單一 ensure 語句處理 nullable fields**：

```java
// ✅ 最佳實踐：使用 Objects.equals() 進行 null-safe 比較
ensure("Sprint goal matches input", () -> Objects.equals(goal, getGoal()));
ensure("PBI description is set", () -> Objects.equals(description, this.getDescription()));

// ✅ 可接受：明確的 null 檢查（當需要更清楚的邏輯時）
ensure("Sprint goal matches input", () -> 
    (goal == null && getGoal() == null) || 
    (goal != null && goal.equals(getGoal())));

// ❌ 錯誤：冗餘的 if-else 檢查
if (goal != null) {
    ensure("Sprint goal is set", () -> getGoal() != null && getGoal().equals(goal));
} else {
    ensure("Sprint goal is null", () -> getGoal() == null);
}
```

**Contract 中的 null-safe 比較規則**：
- 優先使用 `Objects.equals()` 處理可能為 null 的欄位比較
- 保持 lambda 表達式簡潔，盡可能維持單行
- 減少 PIT mutation testing 的潛在變異點

### 2. Lambda 重構為 Private Method 規則

**強制規定**: 在 Aggregate 中，所有多行的 `ensure` 或 `require` lambda 必須重構為 private method：

#### 重構規則
1. **多行 lambda 必須重構**：超過一行的驗證邏輯必須抽取為 private method
2. **命名規範**：private method 名稱必須以 `_verify` 開頭
3. **PIT 配置**：`_verify*` 方法已在 PIT mutation testing 中排除

```java
// ❌ 錯誤：多行 lambda 直接寫在 ensure 中
ensure("A SprintCreated event is generated correctly", () -> {
    var lastEvent = getLastDomainEvent().orElse(null);
    return lastEvent instanceof SprintEvents.SprintCreated created &&
        created.sprintId().equals(sprintId) &&
        created.name().equals(name) &&
        created.productId().equals(productId) &&
        created.timebox().equals(timebox) &&
        created.state().equals(state) &&
        Objects.equals(goal, created.goal()) &&
        Objects.equals(dailyScrum, created.dailyScrum()) &&
        Objects.equals(review, created.review()) &&
        Objects.equals(retrospective, created.retrospective()) &&
        Objects.equals(note, created.note()) &&
        created.creatorId().equals(creatorId) &&
        Objects.equals(extension, created.extension()) &&
        created.sprintBoardConfig() != null &&
        created.sprintBoardConfig().equals(getSprintBoardConfig());
});

// ✅ 正確：重構為 private _verify method
ensure("A SprintCreated event is generated correctly", 
    () -> _verifySprintCreatedEvent(sprintId, name, productId, timebox, state, 
                                   goal, dailyScrum, review, retrospective, 
                                   note, creatorId, extension));

// Private verify method 定義
private boolean _verifySprintCreatedEvent(SprintId sprintId, SprintName name, ProductId productId,
                                        Timebox timebox, SprintState state, SprintGoal goal,
                                        SprintMeeting dailyScrum, SprintMeeting review,
                                        SprintMeeting retrospective, String note,
                                        String creatorId, String extension) {
    var lastEvent = getLastDomainEvent().orElse(null);
    return lastEvent instanceof SprintEvents.SprintCreated created &&
        created.sprintId().equals(sprintId) &&
        created.name().equals(name) &&
        created.productId().equals(productId) &&
        created.timebox().equals(timebox) &&
        created.state().equals(state) &&
        Objects.equals(goal, created.goal()) &&
        Objects.equals(dailyScrum, created.dailyScrum()) &&
        Objects.equals(review, created.review()) &&
        Objects.equals(retrospective, created.retrospective()) &&
        Objects.equals(note, created.note()) &&
        created.creatorId().equals(creatorId) &&
        Objects.equals(extension, created.extension()) &&
        created.sprintBoardConfig() != null &&
        created.sprintBoardConfig().equals(getSprintBoardConfig());
}
```

#### 實施細節
- **什麼時候重構**：當 lambda 表達式超過一行或包含複雜邏輯時
- **保持單行的情況**：簡單的比較或檢查可以保持為單行 lambda
- **PIT 配置**：pom.xml 中已配置 `<excludedMethod>_verify*</excludedMethod>`
- **測試要求**：重構過程不能破壞任何現有測試案例

**違反後果**：
- Code Review 必須失敗 (MUST FIX)
- 多行 lambda 必須重構為 `_verify*` method
- 重構後必須確保所有測試通過

#### 完整範例：
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
        Objects.equals(estimatedHours, created.estimatedHours()) &&  // null-safe 比較
        created.creatorId().equals(creatorId)
    );
}
```

**違反後果**: 
- Code Review 必須失敗 (MUST FIX)
- 不允許合併到主分支
- 必須補充完整的 ensure 檢查

## 🎯 Aggregate Root 設計原則

### 1. 繼承規則 (ezddd 框架)
```java
// ✅ Event Sourcing Aggregate
public class Product extends EsAggregateRoot<ProductId, ProductEvents> {
    // 必須實作的方法：
    @Override
    protected void when(ProductEvents event) { ... }
    
    @Override
    public void ensureInvariant() { ... }
    
    @Override
    public ProductId getId() { ... }
    
    @Override
    public String getCategory() { ... }
}

// ✅ State-based Aggregate  
public class Product extends AggregateRoot<ProductId> {
    // ..
}
```

### 2. 構造函數設計 (ezddd 框架)
```java
// ✅ 正確：提供兩個構造函數
public class Product extends EsAggregateRoot<ProductId, ProductEvents> {
    // 用於 Event Sourcing 重建的構造函數
    public Product(List<ProductEvents> events) {
        super(events);
    }
    
    // 用於創建新實例的公開構造函數
    public Product(ProductId id, String name, UserId creatorId) {
        super(); // 調用父類無參構造函數
        
        requireNotNull("Product ID", id);
        requireNotNull("Product name", name);
        requireNotNull("Creator ID", creatorId);
        
        apply(new ProductEvents.ProductCreated(
            id, 
            name, 
            creatorId,
            new HashMap<>(),  // metadata
            UUID.randomUUID(),
            DateProvider.now()
        ));
    }
}

// ❌ 錯誤：使用 static factory method
public static Product create(ProductId id, String name) {
    // 不要使用 static factory method
}
```

### 3. Command Method 模式

#### reject vs require 的正確使用

**🔴 重要觀念**：
- `reject()` - 用於避免產生不必要的 domain event（例如：新值與舊值相同）
- `require()` - 用於檢查前置條件，條件不滿足時拋出異常

```java
// ✅ 正確使用 reject - 避免不必要的 domain event
public void rename(String newName) {
    requireNotNull("New name", newName);
    require("Name must not be empty", () -> !newName.isBlank());
    
    // 使用 reject 避免產生不必要的 Renamed event
    if (reject("New name is the same as current name", 
                () -> this.name.equals(newName))) {
        return; // 不產生 event，直接返回
    }
    
    apply(new ProductRenamed(this.id, newName, ...));
    
    ensure("Name is updated", () -> this.name.equals(newName));
}

// ✅ 正確使用 require - 前置條件檢查
public void deleteTask(TaskId taskId, String reason, String userId) {
    requireNotNull("taskId", taskId);
    requireNotNull("userId", userId);
    
    // 使用 require 檢查前置條件，不滿足時拋出異常
    require("Task not found", () -> getTask(taskId).isPresent());
    
    apply(new TaskDeleted(this.id, taskId, reason, userId, ...));
    
    ensure("Task is deleted", () -> !getTask(taskId).isPresent());
}

// ❌ 錯誤：混淆 reject 和 require 的用途
public void deleteTask(TaskId taskId, String reason, String userId) {
    // 錯誤：task 不存在應該是異常，不是「避免產生 event」
    if (reject("Task not found", () -> !getTask(taskId).isPresent())) {
        return; // 這會默默地什麼都不做，不是預期行為
    }
    // ...
}
```

#### Command Method 完整模式
```java
public void updateName(String newName) {
    // 1. 前置條件檢查（使用 require）
    requireNotNull("New name", newName);
    require("Name must not be empty", () -> !newName.isBlank());
    
    // 2. 避免不必要的 event（使用 reject）
    if (reject("Name unchanged", () -> this.name.equals(newName))) {
        return; // 無需更新，不產生 event
    }
    
    // 3. 發布事件
    apply(new ProductNameUpdated(this.id, newName, ...));
    
    // 4. 後置條件檢查
    ensure("Name is updated", () -> this.name.equals(newName));
    ensure("Event is generated", () -> 
        getLastDomainEvent() instanceof ProductNameUpdated);
}
```

## 🎯 Value Object 設計原則

### 1. 基本結構
```java
// ✅ 使用 record（推薦）
// 重要：ValueObject 使用 Objects.requireNonNull，不用 Contract
public record ProductId(String value) implements ValueObject {
    public ProductId {
        Objects.requireNonNull(value, "Product ID cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be empty");
        }
    }
    
    public static ProductId create() {
        return new ProductId(UUID.randomUUID().toString());
    }
}

// ✅ 使用 class（當需要更複雜邏輯時）
public final class Money implements ValueObject {
    private final BigDecimal amount;
    private final Currency currency;
    
    public Money(BigDecimal amount, Currency currency) {
        this.amount = requireNotNull("Amount", amount);
        this.currency = requireNotNull("Currency", currency);
        require("Amount must be positive", () -> amount.compareTo(BigDecimal.ZERO) >= 0);
    }
    
    // equals, hashCode, toString
}
```

### 2. 不可變性原則
```java
// ✅ 正確：返回新實例
public Money add(Money other) {
    require("Same currency", () -> this.currency.equals(other.currency));
    return new Money(this.amount.add(other.amount), this.currency);
}

// ❌ 錯誤：修改內部狀態
public void add(Money other) {
    this.amount = this.amount.add(other.amount); // 違反不可變性！
}
```

## 🎯 Domain Event 設計規範 (ezddd 框架)

### 1. Event 結構

#### ⚠️ 關鍵規則：ConstructionEvent 和 DestructionEvent 介面使用
**強制規定**: 絕對不能自己定義 `ConstructionEvent` 或 `DestructionEvent` 介面！

```java
// ❌❌❌ 絕對錯誤：自定義介面
interface ConstructionEvent {}  // 死罪！
interface DestructionEvent {}   // 死罪！

public sealed interface ProductEvents extends InternalDomainEvent {
    record ProductCreated(...) implements ProductEvents, ConstructionEvent {  // 錯誤！
}

// ✅✅✅ 唯一正確：使用 InternalDomainEvent 的內部介面
public sealed interface ProductEvents extends InternalDomainEvent {
    record ProductCreated(
        ProductId productId,
        String name,
        UserId creatorId,
        Map<String, String> metadata,
        UUID id,
        Instant occurredOn
    ) implements ProductEvents, InternalDomainEvent.ConstructionEvent {  // 正確！
    
    record ProductDeleted(
        ProductId productId,
        UserId deletedBy,
        Map<String, String> metadata,
        UUID id,
        Instant occurredOn
    ) implements ProductEvents, InternalDomainEvent.DestructionEvent {  // 正確！
}
```

**違反後果**:
- Code Review 必須失敗 (MUST FIX)
- 框架無法正確識別事件類型
- Event Sourcing 功能會失效

### 2. 完整的 Event 結構範例
```java
// ✅ 正確：使用 sealed interface 和 InternalDomainEvent
public sealed interface ProductEvents extends InternalDomainEvent permits
        ProductEvents.ProductCreated,
        ProductEvents.ProductRenamed,
        ProductEvents.ProductDeleted {
    
    ProductId productId();
    
    @Override
    default String source() {
        return productId().value();  // 新版 API: source() 回傳聚合 ID
    }
    
    // 使用 record 定義具體事件
    record ProductCreated(
        ProductId productId,
        String name,
        UserId creatorId,
        Map<String, String> metadata,  // 必須可變
        UUID id,  // 注意：是 id 而非 eventId
        Instant occurredOn
    ) implements ProductEvents, InternalDomainEvent.ConstructionEvent {
        public ProductCreated {
            // 驗證必要欄位
            requireNotNull("Product ID", productId);
            requireNotNull("Name", name);
            requireNotNull("Creator ID", creatorId);
            requireNotNull("Metadata", metadata);
            requireNotNull("Event ID", id);
            requireNotNull("Occurred on", occurredOn);
        }
        
        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
        
        // 不需要覆寫 source()，已在介面層級定義
    }
    
    record ProductRenamed(
        ProductId productId,
        String newName,
        Map<String, String> metadata,
        UUID id,
        Instant occurredOn
    ) implements ProductEvents {
        // 建構子驗證...
        
        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
        
        // 不需要覆寫 source()，已在介面層級定義
    }
    
    record ProductDeleted(
        ProductId productId,
        UserId deletedBy,
        Map<String, String> metadata,
        UUID id,
        Instant occurredOn
    ) implements ProductEvents, DestructionEvent {
        // 建構子驗證...
        
        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
        
        // 不需要覆寫 source()，已在介面層級定義
    }
}
```

### 2. Metadata 處理
```java
// 在 Aggregate 中
apply(new ProductCreated(
    id,
    name,
    creatorId,
    new HashMap<>(),  // ✅ 使用可變的 HashMap
    UUID.randomUUID(),
    DateProvider.now()
));

// 在 Use Case 中可以修改 metadata
event.metadata().put("requestId", requestId);
event.metadata().put("userId", userId);
```

### 3. Event Handler (ezddd 框架)
```java
// ✅ 正確：使用 when() 方法和 switch expression
@Override
protected void when(ProductEvent event) {
    switch (event) {
        case ProductEvents.ProductCreated e -> {
            this.id = e.productId();
            this.name = e.name();
            this.creatorId = e.creatorId();
            this.state = ProductState.CREATED;
        }
        case ProductEvents.ProductRenamed e -> {
            this.name = e.newName();
        }
        case ProductEvents.ProductDeleted e -> {
            this.state = ProductState.DELETED;
            this.deletedAt = e.occurredOn();
        }
        // 處理其他事件...
    }
}

// ❌ 錯誤：在 Event Handler 中包含業務邏輯
protected void when(ProductEvent event) {
    switch (event) {
        case ProductEvents.TaskAdded e -> {
            this.tasks.add(e.task());
            // 錯誤：業務邏輯不應在 Event Handler 中！
            if (this.tasks.size() > MAX_TASKS) {
                throw new BusinessException("Too many tasks");
            }
        }
    }
}
```

## 🎯 Entity vs Value Object 選擇

### 選擇 Entity 當：
- 需要唯一標識符
- 有生命週期
- 狀態會改變
- 例如：Task, Sprint, User

### 選擇 Value Object 當：
- 通過屬性值識別
- 不可變
- 可替換
- 例如：ProductId, Money, DateRange

## 🔍 檢查清單

### Aggregate (ezddd 框架)
- [ ] 繼承 EsAggregateRoot<ID, Event>
- [ ] 提供 Event Sourcing 重建構造函數：Product(List<Event> events)
- [ ] 提供公開構造函數（非 static factory）
- [ ] 實作 protected void when(Event event) 方法
- [ ] 實作 public void ensureInvariant() 方法
- [ ] 實作 public ID getId() 方法
- [ ] 實作 public String getCategory() 方法
- [ ] Command method 有前置條件檢查 (require)
- [ ] Command method 有後置條件檢查 (ensure)
- [ ] 多行 ensure/require lambda 已重構為 `_verify*` private method
- [ ] 正確發布 Domain Event (apply)

### Value Object
- [ ] 實作 ValueObject 介面
- [ ] 不可變（final fields）
- [ ] 有驗證邏輯
- [ ] 實作 equals/hashCode

### Domain Event (ezddd 框架)
- [ ] 使用 sealed interface extends InternalDomainEvent
- [ ] 使用 record 定義具體事件
- [ ] 實作 source() 方法（在介面層級回傳聚合 ID）
- [ ] 包含必要的 metadata (Map<String, String>)
- [ ] metadata 使用可變 Map (HashMap)
- [ ] 包含 UUID id 和 Instant occurredOn
- [ ] 使用 DateProvider.now() 而非 Instant.now()

## 📋 快速複製模板

### Aggregate 完整模板

```java
package [package].[aggregate].entity;

import tw.teddysoft.ezddd.entity.EsAggregateRoot;
import static tw.teddysoft.ucontract.Contract.*;
import java.util.*;

public class [Aggregate] extends EsAggregateRoot<[Aggregate]Id, [Aggregate]Events> {
    public static final String CATEGORY = "[Aggregate]";
    
    private [Aggregate]Id [aggregate]Id;
    private String name;
    private boolean isDeleted;
    
    // Constructor for Event Sourcing
    public [Aggregate](List<[Aggregate]Events> domainEvents) {
        super(domainEvents);
    }
    
    // Constructor for creation (使用公開建構子，不是 static factory)
    public [Aggregate]([Aggregate]Id [aggregate]Id, String name) {
        super();
        
        requireNotNull("[Aggregate] id", [aggregate]Id);
        requireNotNull("Name", name);
        
        apply(new [Aggregate]Events.[Aggregate]Created(
            [aggregate]Id,
            name,
            new HashMap<>(),  // metadata 必須是可變的
            UUID.randomUUID(),
            DateProvider.now()
        ));
        
        ensure("State initialized", () -> 
            getId().equals([aggregate]Id) && 
            getName().equals(name)
        );
    }
    
    // Business methods
    public void rename(String newName) {
        requireNotNull("New name", newName);
        
        // 使用 reject 避免不必要的 event
        if (reject("Name unchanged", () -> this.name.equals(newName))) {
            return;
        }
        
        apply(new [Aggregate]Events.[Aggregate]Renamed(
            [aggregate]Id,
            newName,
            new HashMap<>(),
            UUID.randomUUID(),
            DateProvider.now()
        ));
        
        ensure("Name updated", () -> this.name.equals(newName));
    }
    
    @Override
    protected void when([Aggregate]Events event) {
        switch (event) {
            case [Aggregate]Events.[Aggregate]Created e -> {
                this.[aggregate]Id = e.[aggregate]Id();
                this.name = e.name();
                this.isDeleted = false;
            }
            case [Aggregate]Events.[Aggregate]Renamed e -> {
                this.name = e.newName();
            }
            case [Aggregate]Events.[Aggregate]Deleted e -> {
                this.isDeleted = true;
            }
        }
    }
    
    @Override
    public void ensureInvariant() {
        invariant("Category correct", () -> getCategory().equals(CATEGORY));
        invariantNotNull("[Aggregate] Id", [aggregate]Id);
        if (!isDeleted) {
            invariantNotNull("Name", name);
        }
    }
    
    // Getters
    public String getName() { return name; }
    
    @Override
    public [Aggregate]Id getId() { return [aggregate]Id; }
    
    @Override
    public String getCategory() { return CATEGORY; }
    
    @Override
    public boolean isDeleted() { return isDeleted; }
}
```

### Domain Events 模板

```java
package [package].[aggregate].entity;

import tw.teddysoft.ezddd.entity.DomainEventTypeMapper;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public sealed interface [Aggregate]Events extends InternalDomainEvent {
    
    [Aggregate]Id [aggregate]Id();
    
    @Override
    default String source() {
        return [aggregate]Id().value();  // 新版 API: source() 回傳聚合的 ID
    }
    
    record [Aggregate]Created(
        [Aggregate]Id [aggregate]Id,
        String name,
        Map<String, String> metadata,
        UUID id,
        Instant occurredOn
    ) implements [Aggregate]Events, ConstructionEvent {
        public [Aggregate]Created {
            Objects.requireNonNull([aggregate]Id);
            Objects.requireNonNull(name);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }
        
        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
        
        // 不需要覆寫 source()，已在介面層級定義
    }
    
    record [Aggregate]Renamed(
        [Aggregate]Id [aggregate]Id,
        String newName,
        Map<String, String> metadata,
        UUID id,
        Instant occurredOn
    ) implements [Aggregate]Events {
        public [Aggregate]Renamed {
            Objects.requireNonNull([aggregate]Id);
            Objects.requireNonNull(newName);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }
        
        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
        
        // 不需要覆寫 source()，已在介面層級定義
    }
    
    record [Aggregate]Deleted(
        [Aggregate]Id [aggregate]Id,
        Map<String, String> metadata,
        UUID id,
        Instant occurredOn
    ) implements [Aggregate]Events {
        public [Aggregate]Deleted {
            Objects.requireNonNull([aggregate]Id);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }
        
        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
        
        // 不需要覆寫 source()，已在介面層級定義
    }
    
    class TypeMapper {
        private static final String MAPPING_TYPE_PREFIX = "[Aggregate]Events$";
        public static final String [AGGREGATE]_CREATED = MAPPING_TYPE_PREFIX + "[Aggregate]Created";
        public static final String [AGGREGATE]_RENAMED = MAPPING_TYPE_PREFIX + "[Aggregate]Renamed";
        public static final String [AGGREGATE]_DELETED = MAPPING_TYPE_PREFIX + "[Aggregate]Deleted";
        
        private static final DomainEventTypeMapper mapper;
        
        static {
            mapper = DomainEventTypeMapper.create();
            mapper.put([AGGREGATE]_CREATED, [Aggregate]Events.[Aggregate]Created.class);
            mapper.put([AGGREGATE]_RENAMED, [Aggregate]Events.[Aggregate]Renamed.class);
            mapper.put([AGGREGATE]_DELETED, [Aggregate]Events.[Aggregate]Deleted.class);
        }
        
        public static DomainEventTypeMapper getInstance() {
            return mapper;
        }
    }
    
    static DomainEventTypeMapper mapper() {
        return TypeMapper.getInstance();
    }
}
```

### Value Object 模板

```java
package [package].[aggregate].entity;

import tw.teddysoft.ezddd.entity.ValueObject;
import java.util.Objects;

public record [ValueObject](String value) implements ValueObject {
    public [ValueObject] {
        // ValueObject 使用 Objects.requireNonNull
        Objects.requireNonNull(value, "[ValueObject] value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("[ValueObject] value cannot be empty");
        }
    }
    
    public static [ValueObject] of(String value) {
        return new [ValueObject](value);
    }
    
    public static [ValueObject] valueOf(String value) {
        return new [ValueObject](value);
    }
    
    public static [ValueObject] create() {
        return new [ValueObject](UUID.randomUUID().toString());
    }
    
    // 🔴 重要：必須覆寫 toString() 返回純值
    // 用於 Outbox Pattern stream name 生成
    @Override
    public String toString() {
        return value;
    }
}
```

## 相關文件
- [DDD 設計原則](./README.md#-ddd-設計原則)
- [事件處理規範](./README.md#-事件處理規範)
- [Aggregate 識別檢查清單](../../../checklists/AGGREGATE-IDENTIFICATION-CHECKLIST.md)