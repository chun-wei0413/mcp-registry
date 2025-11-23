# Aggregate 範例與模式

本目錄包含 Aggregate Root 的設計模式說明與實作範例。

## 📋 概述

Aggregate Root 是 DDD 中的核心概念，定義了一致性邊界和事務邊界。在 Event Sourcing 架構中，Aggregate Root 負責處理命令、產生事件並維護狀態。

## 🎯 核心概念

### 什麼是 Aggregate？
- **一致性邊界**：保證內部狀態的一致性
- **事務邊界**：所有變更在單一事務中完成
- **聚合根**：外部只能通過聚合根訪問聚合內部

### Event Sourcing 模式
- 狀態變更通過事件記錄
- 可重建歷史狀態
- 支援事件回放

## 📁 檔案結構

```
aggregate/
├── README.md           # 本文件
├── Plan.java          # 完整的 Plan Aggregate 範例
├── Project.java       # Project Entity 範例 (Aggregate 內的 Entity)
├── PlanEvents.java    # Plan Domain Events 定義 (⚠️ 所有 events 都在此檔案內部定義)
├── PlanId.java        # Plan 識別碼 Value Object
├── ProjectId.java     # Project 識別碼 Value Object
├── ProjectName.java   # Project 名稱 Value Object
├── TaskId.java        # Task 識別碼 Value Object
├── TagId.java         # Tag 識別碼 Value Object (用於兩個 Aggregate)
└── TagEvents.java     # Tag Domain Events 定義 (⚠️ 所有 events 都在此檔案內部定義)
```

### ⚠️ 重要提醒：Domain Events 檔案結構
**絕對不要**為每個 event 創建獨立檔案！所有 domain events 都應該定義在 sealed interface 內部：
- ✅ 正確：`PlanEvents.java` 包含所有 Plan 相關的 events (PlanCreated, PlanDeleted 等)
- ❌ 錯誤：創建獨立的 `PlanCreated.java`, `PlanDeleted.java` 檔案

## 🏗️ Entity 設計模式 (Project.java)

### Entity vs Aggregate Root
Entity 是 DDD 中具有身份識別的領域物件，但不是 Aggregate Root：
- **Entity**: 有唯一識別碼，有生命週期，但必須透過 Aggregate Root 訪問
- **Aggregate Root**: 聚合的入口點，管理整個聚合的一致性

### Project Entity 範例說明

`Project.java` 展示了 Aggregate 內部 Entity 的標準實作模式：

#### 1. Entity 介面實作
```java
public class Project implements Entity<ProjectId> {
    private final ProjectId id;
    private final ProjectName name;
    private final PlanId planId;
    private final Map<TaskId, Task> tasks;
```

#### 2. 核心特性
- **唯一識別**：使用 `ProjectId` 作為身份標識
- **所屬關係**：包含 `PlanId` 表示歸屬於哪個 Plan Aggregate
- **子實體管理**：管理內部的 Task 集合
- **不可變核心屬性**：id、name、planId 使用 final 修飾

#### 3. 業務方法封裝
```java
// 創建任務
public void createTask(TaskId taskId, String taskName) {
    if (taskName == null || taskName.trim().isEmpty()) {
        throw new IllegalArgumentException("Task name cannot be empty");
    }
    Task task = new Task(taskId, taskName, this.name);
    tasks.put(taskId, task);
}

// 檢查任務
public void checkTask(TaskId taskId) {
    Task task = tasks.get(taskId);
    if (task != null) {
        task.markAsDone();
    }
}
```

#### 4. Entity 設計原則
- **封裝性**：所有操作都透過方法進行，不直接暴露內部狀態
- **防禦性複製**：返回集合時使用 `new HashMap<>(tasks)` 防止外部修改
- **null 安全**：使用 `Objects.requireNonNull` 確保必要欄位不為 null
- **equals/hashCode**：基於 ID 實作，符合 Entity 的身份特性

### Entity vs Value Object 判斷標準

| 特性 | Entity (如 Project) | Value Object (如 ProjectName) |
|------|-------------------|---------------------------|
| 身份識別 | 有唯一 ID | 無 ID，由值定義 |
| 可變性 | 可變（透過方法） | 不可變 |
| 生命週期 | 有（創建、修改、刪除） | 無 |
| 相等性 | 基於 ID | 基於所有屬性值 |
| 範例 | Project, Task | ProjectId, ProjectName, TaskId |

## 💎 Value Object 設計模式

### Value Object 定義
Value Object 是 DDD 中表達領域概念的不可變物件：
- **無身份識別**：由其所有屬性值定義，而非 ID
- **不可變性**：一旦創建就不能修改
- **可替換性**：相同值的 Value Object 可以互相替換
- **無生命週期**：不需要追蹤創建、修改、刪除

### Value Object 範例分類

#### 1. 識別碼類型 (PlanId.java)
用於表示 Entity 或 Aggregate 的唯一識別：

```java
public record PlanId(String value) implements ValueObject {
    
    public PlanId {
        Objects.requireNonNull(value, "PlanId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("PlanId value cannot be empty");
        }
    }
    
    // 工廠方法：生成新 ID
    public static PlanId create() {
        return new PlanId(UUID.randomUUID().toString());
    }
    
    // 工廠方法：從現有值創建
    public static PlanId valueOf(String value) {
        return new PlanId(value);
    }
}
```

#### 2. 業務概念類型 (ProjectName.java)
封裝特定的業務規則和驗證：

```java
public record ProjectName(String value) implements ValueObject {
    
    public ProjectName {
        Objects.requireNonNull(value, "ProjectName value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("ProjectName value cannot be empty");
        }
        // 可加入更多業務規則，如長度限制、特殊字符檢查等
    }
    
    public static ProjectName valueOf(String value) {
        return new ProjectName(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

### Value Object 設計最佳實踐

#### 1. 使用 Java Record
- **簡潔性**：自動生成 equals、hashCode、toString
- **不可變性**：record 的欄位自動是 final
- **compact constructor**：方便進行驗證
- **null 安全**：使用 `Objects.requireNonNull` 確保必要欄位不為 null
- 
#### 2. 驗證規則
```java
public record Email(String value) implements ValueObject {
    public Email {
        // null 檢查
        Objects.requireNonNull(value, "Email cannot be null");
        
        // 格式驗證
        if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}
```

#### 3. 工廠方法模式
```java
public record Money(BigDecimal amount, Currency currency) implements ValueObject {
    // 多種創建方式
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }
    
    public static Money ofDollars(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("USD"));
    }
    
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
}
```

#### 4. 業務行為封裝
```java
public record DateRange(LocalDate start, LocalDate end) implements ValueObject {
    public DateRange {
        Objects.requireNonNull(start, "Start date cannot be null");
        Objects.requireNonNull(end, "End date cannot be null");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
    }
    
    // 業務方法
    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
    
    public long getDays() {
        return ChronoUnit.DAYS.between(start, end);
    }
}
```

### Value Object vs Primitive 的選擇

**使用 Value Object 而非 Primitive 的時機**：
- ✅ 需要驗證規則（如 Email、PhoneNumber）
- ✅ 需要封裝業務邏輯（如 Money、DateRange）
- ✅ 避免參數混淆（ProjectId vs TaskId vs String）
- ✅ 提高程式碼可讀性和類型安全

**可以使用 Primitive 的時機**：
- ✅ 簡單的內部狀態（如 boolean isDeleted）
- ✅ 標準的數值計算（如 int count）
- ✅ 臨時或局部變數

### 本目錄中的 Value Objects

| Value Object | 用途 | 特點 |
|--------------|------|------|
| PlanId | Plan 聚合根識別碼 | UUID 生成、唯一性保證 |
| ProjectId | Project 實體識別碼 | UUID 生成、唯一性保證 |
| ProjectName | 專案名稱 | 非空驗證、業務概念封裝 |
| TaskId | Task 實體識別碼 | UUID 生成、唯一性保證 |
| TagId | Tag 聚合根識別碼 | 跨聚合引用 |

## 🔧 實作要點

### 1. 基本結構

```java
public class [Aggregate] extends EsAggregateRoot<[Aggregate]Id, [Aggregate]Events> {
    // 常數定義
    public final static String CATEGORY = "[Aggregate]";
    
    // 聚合狀態
    private [Aggregate]Id id;
    private String name;
    private boolean isDeleted;
    
    // Constructor for event sourcing framework to rebuild aggregate from events
    public [Aggregate](List<[Aggregate]Events> domainEvents) {
        super(domainEvents);
    }
    
    // Public constructor for creating new instances
    public [Aggregate]([Aggregate]Id id, String name) {
        super();
        
        // 使用 Contract 驗證輸入
        requireNotNull("id", id);
        requireNotNull("name", name);
        
        // 發出創建事件
        apply(new [Aggregate]Events.[Aggregate]Created(
            id,
            name,
            new HashMap<>(),  // metadata (可變的 HashMap)
            UUID.randomUUID(),
            DateProvider.now()
        ));
        
        // 確保事件正確應用
        ensure("id is set", () -> getId().equals(id));
        ensure("name is set", () -> getName().equals(name));
    }
}
```

### 2. 使用方式

```java
// 創建新的 Aggregate（使用 constructor）
[Aggregate] aggregate = new [Aggregate](
    [Aggregate]Id.create(),
    "Name"
);

// 從事件重建（用於 Event Sourcing）
[Aggregate] aggregate = new [Aggregate](events);
```

### 3. 業務行為

```java
public void rename(String newName) {
   // 前置條件檢查
   requireNotNull("New name", newName);
   require("Name is different", () -> !this.name.equals(newName));

   // 發出事件
   apply(new [Aggregate]Events.[Aggregate]Renamed(
           getId(),
           newName,
           new HashMap<>(),  // metadata (可變的 HashMap)
           UUID.randomUUID(),
           DateProvider.now()
   ));

   // 後置條件確保
   ensure("Name is changed", () -> getName().equals(newName));
   ensure("A event is generated correctly", () -> getLastDomainEvent().equals(new [Aggregate]Events.[Aggregate]Renamed(getId(), newName, getLastDomainEvent().id(), getLastDomainEvent().occurredOn())));
}
```

### 4. 事件處理器

```java
@Override
protected void when([Aggregate]Events event) {
   switch (event) {
      case [Aggregate]Events.[Aggregate]Created e -> {
         this.id = e.id();
         this.name = e.name();
         this.isDeleted = false;
      }
      case [Aggregate]Events.[Aggregate]Renamed e -> {
         this.name = e.newName();
      }
      case [Aggregate]Events.[Aggregate]Deleted e -> {
         this.isDeleted = true;
      }
      default -> {
         // 處理未知事件
      }
   }
}
```

### 5. 不變條件

```java
@Override
public void ensureInvariant() {
   invariant("Is not marked as deleted", () -> !isDeleted());
   invariantNotNull("Id", getId());
   invariantNotNull("Name", getName());
   invariant("Name is not empty", () -> !getName().trim().isEmpty());
}
```

## 💡 設計原則

### 1. 封裝性
- 所有狀態變更必須通過方法進行
- 不提供直接的 setter
- 內部集合返回防禦性副本

### 2. 一致性
- 使用 Contract 進行前置條件檢查
- 使用 ensure 進行後置條件驗證
- 實作 ensureInvariant 維護不變條件

### 3. 事件驅動
- 所有狀態變更產生對應事件
- 事件包含完整的變更資訊
- 支援事件重放

## 📝 使用範例

```java
// 創建新的 Plan
Plan plan = new Plan(
                PlanId.create(),
                "My Study Plan",
                "user123"
        );

// 重新命名
plan.rename("Updated Study Plan");

// 創建專案
plan.createProject(
        ProjectId.create(), 
    ProjectName.valueOf("Java Learning")
);

// 創建任務
TaskId taskId = plan.createTask(
        "Java Learning",  // projectName
        "Learn Spring Boot"
);

// 標記任務完成
plan.checkTask(taskId);

// 保存聚合根
repository.save(plan);
```

## ⚠️ 注意事項

1. **不要直接修改狀態**
   - 永遠通過 apply(event) 來改變狀態
   - 狀態修改只能在 when() 方法中進行

2. **保持事件的不可變性**
   - 使用 record 或 immutable 類別
   - 包含所有必要的資訊

3. **處理並發**
   - 使用版本號或時間戳
   - 實作樂觀鎖定

4. **避免過大的聚合**
   - 保持聚合邊界合理
   - 考慮拆分過大的聚合

## 🔗 相關資源

- [Use Case 整合範例](../usecase/) - Use Case 設計模式與範例
- [Controller 範例](../controller/) - REST API Controller 實作
- [Repository 範例](../repository/) - Repository 模式實作
- [Projection 範例](../projection/) - CQRS 查詢投影
- [DTO 範例](../dto/) - 資料傳輸物件
- [Persistence 範例](../persistence/) - JPA 持久化實體
- [Mapper 範例](../mapper/) - 領域物件與 DTO 轉換
- [測試範例](../test/) - 測試模式與範例
- [Contract 範例](../contract/) - Design by Contract 模式