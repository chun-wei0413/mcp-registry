# Plan Aggregate 完整範例

本文件提供完整的 Aggregate Root 實作範例，展示 State-based 和 Event Sourcing 兩種模式。

---

## Plan Aggregate 概述

### 🎯 設計意圖 (Why)

**問題**：如何實作一個完整的 Aggregate Root，管理複雜的聚合結構？

**答案**：Plan Aggregate 展示如何管理學習計畫、專案（Project）和任務（Task），包含：
1. **一致性邊界**：Plan 作為聚合根，確保內部 Project 和 Task 的一致性
2. **完整的生命週期管理**：從創建、修改到刪除
3. **Contract-based Design**：使用前置條件、後置條件和不變式
4. **兩種實作模式**：State-based 和 Event Sourcing

### 📋 聚合結構

```
Plan (Aggregate Root)
├── PlanId (Value Object)
├── Projects (Entity Collection)
│   ├── Project (Entity)
│   │   ├── ProjectId (Value Object)
│   │   ├── ProjectName (Value Object)
│   │   └── Tasks (Entity Collection)
│   │       └── Task (Entity)
│   │           ├── TaskId (Value Object)
│   │           └── Tags (Value Object Collection)
│   │               └── TagId (Value Object)
└── Domain Events (PlanEvents)
```

### 🔍 檢查清單

- [ ] 定義 Value Object 識別碼（PlanId）
- [ ] 定義 Domain Events（PlanEvents）
- [ ] 選擇實作模式（State-based 或 Event Sourcing）
- [ ] 實作建構子（單一或雙建構子）
- [ ] 實作業務方法（Command Methods）
- [ ] 實作 ensureInvariant()

---

## PlanId - Value Object 識別碼

### 🎯 設計意圖 (Why)

**問題**：如何確保識別碼的類型安全和不可變性？

**答案**：使用 Java `record` 定義 Value Object 識別碼：
1. **類型安全**：避免使用原始 String 類型，防止參數混淆
2. **不可變性**：record 自動確保不可變
3. **驗證規則**：確保識別碼不為 null 且不為空字串
4. **工廠方法**：提供 create() 和 valueOf() 便利方法

### 📋 實作規範 (How)

```java
public record [Aggregate]Id(String value) implements ValueObject {
    // Compact constructor 驗證
    public [Aggregate]Id {
        Objects.requireNonNull(value, "[Aggregate]Id value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("[Aggregate]Id value cannot be empty");
        }
    }

    // 工廠方法
    public static [Aggregate]Id create() {
        return new [Aggregate]Id(UUID.randomUUID().toString());
    }

    public static [Aggregate]Id valueOf(String value) {
        return new [Aggregate]Id(value);
    }

    // 覆寫 toString 用於序列化
    @Override
    public String toString() {
        return value;
    }
}
```

### ✅ 正確範例

```java
package tw.teddysoft.example.plan.entity;

import tw.teddysoft.ezddd.entity.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record PlanId(String value) implements ValueObject {

    public PlanId {
        Objects.requireNonNull(value, "PlanId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("PlanId value cannot be empty");
        }
    }

    public static PlanId create(){
        return new PlanId(UUID.randomUUID().toString());
    }

    public static PlanId valueOf(String value) {
        return new PlanId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

// ✅ 使用方式
PlanId newId = PlanId.create();
PlanId existingId = PlanId.valueOf("existing-uuid-string");
Plan plan = new Plan(PlanId.create(), "My Study Plan", "user123");
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：使用 String 作為識別碼
public class Plan {
    private String planId;  // 沒有類型安全
}

// ❌ 錯誤：沒有驗證
public record PlanId(String value) implements ValueObject {
    // 沒有 compact constructor，無法驗證 null 或空字串
}

// ❌ 錯誤：使用 class 而非 record
public class PlanId implements ValueObject {
    private final String value;  // 需要手動實作 equals/hashCode/toString
}
```

**症狀**：參數混淆、null pointer exception、識別碼驗證不一致。

### 🔍 檢查清單

- [ ] 使用 record 定義
- [ ] 實作 ValueObject 介面
- [ ] Compact constructor 驗證 null 和空值
- [ ] 提供 create() 和 valueOf() 工廠方法
- [ ] 覆寫 toString() 返回純值

---

## PlanEvents - Domain Events 定義

### 🎯 設計意圖 (Why)

**問題**：如何定義所有 Aggregate 相關的領域事件？

**答案**：使用 `sealed interface` 統一定義所有事件：
1. **語意完整性**：每個事件包含完整的變更資訊
2. **類型安全**：sealed interface 確保所有事件都在此定義
3. **事件溯源支援**：包含 TypeMapper 用於序列化/反序列化
4. **ConstructionEvent / DestructionEvent**：標記聚合的創建和刪除
5. **Metadata 支援**：每個事件都包含 metadata 擴展資訊

### 📋 實作規範 (How)

```java
public sealed interface [Aggregate]Events extends InternalDomainEvent permits
        [Aggregate]Events.[Aggregate]Created,
        [Aggregate]Events.[Event2],
        [Aggregate]Events.[Event3] {

    [Aggregate]Id [aggregate]Id();

    @Override
    default String source() {
        return [aggregate]Id().value();
    }

    record [Aggregate]Created(
            [Aggregate]Id [aggregate]Id,
            // 其他業務欄位
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements [Aggregate]Events, ConstructionEvent {
        // Compact constructor 驗證所有欄位
        public [Aggregate]Created {
            Objects.requireNonNull([aggregate]Id);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    // TypeMapper 用於事件序列化
    class TypeMapper extends DomainEventTypeMapper.DefaultMapper {
        public static final String MAPPING_TYPE_PREFIX = "[Aggregate]Events$";
        public static final String [AGGREGATE]_CREATED = MAPPING_TYPE_PREFIX + "[Aggregate]Created";

        private static final DomainEventTypeMapper mapper;

        static {
            mapper = DomainEventTypeMapper.create();
            mapper.put([AGGREGATE]_CREATED, [Aggregate]Events.[Aggregate]Created.class);
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

### ✅ 正確範例

```java
package tw.teddysoft.example.plan.entity;

import tw.teddysoft.ezddd.entity.InternalDomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public sealed interface PlanEvents extends InternalDomainEvent permits
        PlanEvents.PlanCreated,
        PlanEvents.PlanRenamed,
        PlanEvents.PlanDeleted,
        PlanEvents.ProjectCreated,
        PlanEvents.ProjectDeleted {

    PlanId planId();

    @Override
    default String source() {
        return planId().value();
    }

    // ✅ 創建事件實作 ConstructionEvent
    record PlanCreated(
            PlanId planId,
            String name,
            String userId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents, ConstructionEvent {
        public PlanCreated {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(name);
            Objects.requireNonNull(userId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    // ✅ 一般事件
    record PlanRenamed(
            PlanId planId,
            String newName,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public PlanRenamed {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(newName);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    // ✅ 刪除事件實作 DestructionEvent
    record PlanDeleted(
            PlanId planId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents, DestructionEvent {
        public PlanDeleted {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record ProjectCreated(
            PlanId planId,
            ProjectId projectId,
            ProjectName projectName,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public ProjectCreated {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(projectName);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record ProjectDeleted(
            PlanId planId,
            ProjectId projectId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public ProjectDeleted {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    // ✅ TypeMapper 定義
    class TypeMapper extends DomainEventTypeMapper.DefaultMapper {
        public static final String MAPPING_TYPE_PREFIX = "PlanEvents$";
        public static final String PLAN_CREATED = MAPPING_TYPE_PREFIX + "PlanCreated";
        public static final String PLAN_RENAMED = MAPPING_TYPE_PREFIX + "PlanRenamed";
        public static final String PLAN_DELETED = MAPPING_TYPE_PREFIX + "PlanDeleted";
        public static final String PROJECT_CREATED = MAPPING_TYPE_PREFIX + "ProjectCreated";
        public static final String PROJECT_DELETED = MAPPING_TYPE_PREFIX + "ProjectDeleted";

        private static final DomainEventTypeMapper mapper;

        static {
            mapper = DomainEventTypeMapper.create();
            mapper.put(PLAN_CREATED, PlanEvents.PlanCreated.class);
            mapper.put(PLAN_RENAMED, PlanEvents.PlanRenamed.class);
            mapper.put(PLAN_DELETED, PlanEvents.PlanDeleted.class);
            mapper.put(PROJECT_CREATED, PlanEvents.ProjectCreated.class);
            mapper.put(PROJECT_DELETED, PlanEvents.ProjectDeleted.class);
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

### ❌ 常見錯誤

```java
// ❌ 錯誤：每個事件一個獨立檔案
// PlanCreated.java
public record PlanCreated(...) implements DomainEvent { }

// PlanRenamed.java
public record PlanRenamed(...) implements DomainEvent { }

// ❌ 錯誤：沒有使用 sealed interface
public interface PlanEvents extends InternalDomainEvent { }

// ❌ 錯誤：沒有驗證欄位
record PlanCreated(
    PlanId planId,
    String name
) implements PlanEvents {
    // 缺少 compact constructor 驗證
}

// ❌ 錯誤：缺少必要欄位
record PlanCreated(
    PlanId planId,
    String name
    // 缺少 metadata, id, occurredOn
) implements PlanEvents { }
```

**症狀**：事件分散在多個檔案、缺少類型安全、無法序列化、null pointer exception。

### 🔍 檢查清單

- [ ] 使用 sealed interface 定義所有事件
- [ ] 所有事件 record 都在 interface 內部定義
- [ ] ConstructionEvent 標記創建事件
- [ ] DestructionEvent 標記刪除事件（如果有軟刪除）
- [ ] 每個事件包含 metadata, id, occurredOn
- [ ] Compact constructor 驗證所有必要欄位
- [ ] 實作 TypeMapper 用於序列化

---

## Plan (State-based) - Aggregate Root 實作

### 🎯 設計意圖 (Why)

**問題**：如何實作簡單直接的 Aggregate Root？

**答案**：State-based Aggregate 直接修改內部狀態，然後發布領域事件，適合：
1. **簡單的狀態管理**：不需要完整的事件溯源歷史
2. **傳統資料庫持久化**：使用 ORM 或傳統 Repository 儲存當前狀態
3. **直覺的程式邏輯**：先改狀態，再發事件

### 📋 實作規範 (How)

**State-based 三大特徵**：
1. 繼承 `AggregateRoot<ID>`（不是 EsAggregateRoot）
2. 單一建構子（不需要事件重建建構子）
3. 不需要實作 `when()` 方法

### ✅ 正確範例

```java
package tw.teddysoft.example.plan.entity;

import tw.teddysoft.example.common.DateProvider;
import tw.teddysoft.ezddd.entity.AggregateRoot;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static tw.teddysoft.ucontract.Contract.*;

/**
 * Plan Aggregate Root - State-based 實作
 *
 * ✅ 使用傳統的狀態修改模式
 * ✅ 適合使用 ORM 或傳統資料庫持久化
 * ❌ 不需要實作 when() 方法
 */
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    public final static String CATEGORY = "Plan";

    private PlanId planId;
    private String name;
    private String userId;
    private Map<ProjectId, Project> projects;
    private boolean isDeleted;

    /**
     * ✅ 單一建構子：用於創建新的 Plan
     */
    public Plan(PlanId planId, String name, String userId) {
        super();

        // Step 1: Preconditions
        requireNotNull("Plan id", planId);
        requireNotNull("Plan name", name);
        requireNotNull("User id", userId);

        // Step 2: 直接設定狀態（State-based 關鍵）
        this.planId = planId;
        this.name = name;
        this.userId = userId;
        this.projects = new HashMap<>();
        this.isDeleted = false;

        // Step 3: 發布領域事件
        apply(new PlanEvents.PlanCreated(
                planId,
                name,
                userId,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));

        // Step 4: Postconditions
        ensure(format("Plan id is '%s'", planId), () -> getId().equals(planId));
        ensure(format("Plan name is '%s'", name), () -> getName().equals(name));
        ensure(format("User id is '%s'", userId), () -> getUserId().equals(userId));
        ensure("A PlanCreated event is generated correctly", () ->
            getLastDomainEvent() instanceof PlanEvents.PlanCreated created &&
            created.planId().equals(planId) &&
            created.name().equals(name) &&
            created.userId().equals(userId)
        );
    }

    /**
     * ✅ Command Method：重新命名 Plan
     */
    public void rename(String newName) {
        // Step 1: Preconditions
        requireNotNull("New name", newName);
        require("New name not empty", () -> !newName.trim().isEmpty());

        // Step 2: Early Exit (ignore)
        if (ignore("Name unchanged", () -> this.name.equals(newName))) {
            return;
        }

        // Step 3: 直接修改狀態
        this.name = newName;

        // Step 4: 發布領域事件
        apply(new PlanEvents.PlanRenamed(
                planId,
                newName,
                new HashMap<>(),
                UUID.randomUUID(),
                DateProvider.now()
        ));

        // Step 5: Postconditions
        ensure(format("Plan name is changed to '%s'", newName), () -> getName().equals(newName));
        ensure("A PlanRenamed event is generated correctly", () ->
            getLastDomainEvent() instanceof PlanEvents.PlanRenamed renamed &&
            renamed.planId().equals(planId) &&
            renamed.newName().equals(newName)
        );
    }

    /**
     * ✅ Command Method：創建新的 Project
     */
    public void createProject(ProjectId projectId, ProjectName projectName) {
        requireNotNull("Project id", projectId);
        requireNotNull("Project name", projectName);
        require("Project id must be unique", () -> !hasProject(projectId));

        // 直接修改狀態
        Project project = new Project(projectId, projectName, this.planId);
        this.projects.put(projectId, project);

        // 發布領域事件
        apply(new PlanEvents.ProjectCreated(
                planId,
                projectId,
                projectName,
                new HashMap<>(),
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure(format("Project with id '%s' exists", projectId), () -> hasProject(projectId));
        ensure(format("Project name is '%s'", projectName), () -> getProject(projectId).getName().equals(projectName));
    }

    /**
     * ✅ Command Method：刪除 Project
     */
    public void deleteProject(ProjectId projectId) {
        requireNotNull("Project id", projectId);
        require("Project must exist", () -> hasProject(projectId));

        // 直接修改狀態
        this.projects.remove(projectId);

        // 發布領域事件
        apply(new PlanEvents.ProjectDeleted(
                planId,
                projectId,
                new HashMap<>(),
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure(format("Project with id '%s' is deleted", projectId), () -> !hasProject(projectId));
    }

    /**
     * ✅ Command Method：標記 Plan 為已刪除（軟刪除）
     */
    public void delete() {
        require("Plan must not already be deleted", () -> !isDeleted);

        // 直接修改狀態
        this.isDeleted = true;

        // 發布領域事件
        apply(new PlanEvents.PlanDeleted(
                planId,
                new HashMap<>(),
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure("Plan is marked as deleted", () -> isDeleted());
    }

    @Override
    public boolean isDeleted() {
        return isDeleted;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public PlanId getId() {
        return planId;
    }

    @Override
    public void ensureInvariant() {
        invariant(format("Category is '%s'.", getCategory()), () -> getCategory().equals(CATEGORY));
        invariantNotNull("Plan Id", planId);
        if (!isDeleted) {
            invariantNotNull("Plan name", name);
            invariantNotNull("User Id", userId);
        }
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public boolean hasProject(ProjectId projectId) {
        return projects.containsKey(projectId);
    }

    public Project getProject(ProjectId projectId) {
        return projects.get(projectId);
    }

    public Map<ProjectId, Project> getProjects() {
        return new HashMap<>(projects);  // ✅ 防禦性複製
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：State-based 實作 when() 方法
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    @Override
    protected void when(PlanEvents event) {  // ❌ 不需要這個方法！
        // ...
    }
}

// ❌ 錯誤：先發事件，再改狀態
public void rename(String newName) {
    apply(new PlanEvents.PlanRenamed(...));  // ❌ 順序錯誤
    this.name = newName;
}

// ✅ 正確：先改狀態，再發事件
public void rename(String newName) {
    this.name = newName;  // ✅ 先改狀態
    apply(new PlanEvents.PlanRenamed(...));
}

// ❌ 錯誤：提供 setter
public void setName(String name) {  // ❌ 不要提供 setter
    this.name = name;
}

// ✅ 正確：使用業務方法
public void rename(String newName) {  // ✅ 業務方法
    this.name = newName;
    apply(new PlanEvents.PlanRenamed(...));
}
```

**症狀**：狀態不一致、事件與狀態不同步、破壞封裝性。

### 🔍 檢查清單

- [ ] 繼承 `AggregateRoot<ID>`（不是 EsAggregateRoot）
- [ ] 單一建構子
- [ ] 不實作 when() 方法
- [ ] 業務方法：先改狀態，再發事件
- [ ] 使用 apply() 發布事件
- [ ] 實作 ensureInvariant()
- [ ] 不提供 setter
- [ ] 返回集合時使用防禦性複製

---

## Plan (Event Sourcing) - Aggregate Root 實作

### 🎯 設計意圖 (Why)

**問題**：如何實作支援事件溯源的 Aggregate Root？

**答案**：Event Sourcing Aggregate 透過重播事件來重建狀態，適合：
1. **需要完整歷史**：需要追溯所有狀態變更歷史
2. **審計需求**：需要完整的事件日誌用於稽核
3. **時間旅行**：需要重建任意時間點的狀態
4. **Event-driven 架構**：系統本身就是 Event-driven 設計

### 📋 實作規範 (How)

**Event Sourcing 三大特徵**：
1. 繼承 `EsAggregateRoot<ID, Events>`（不是 AggregateRoot）
2. 雙建構子（業務建構子 + 事件重建建構子）
3. 必須實作 `when()` 方法處理事件

### ✅ 正確範例

```java
package tw.teddysoft.example.plan.entity;

import tw.teddysoft.example.common.DateProvider;
import tw.teddysoft.ezddd.entity.EsAggregateRoot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static tw.teddysoft.ucontract.Contract.*;

/**
 * Plan Aggregate Root - Event Sourcing 實作
 *
 * ✅ 使用事件溯源模式
 * ✅ 所有狀態變更都通過事件記錄
 * ✅ 必須實作 when() 方法
 */
public class Plan extends EsAggregateRoot<PlanId, PlanEvents> {
    public final static String CATEGORY = "Plan";

    private PlanId planId;
    private String name;
    private String userId;
    private Map<ProjectId, Project> projects;
    private boolean isDeleted;

    /**
     * ✅ 建構子 1：Event Sourcing 框架用於從事件重建 Aggregate
     */
    public Plan(List<PlanEvents> domainEvents) {
        super(domainEvents);
    }

    /**
     * ✅ 建構子 2：業務邏輯用於創建新的 Aggregate
     */
    public Plan(PlanId planId, String name, String userId) {
        super();

        // Step 1: Preconditions
        requireNotNull("Plan id", planId);
        requireNotNull("Plan name", name);
        requireNotNull("User id", userId);

        // Step 2: 發布事件（狀態變更在 when() 中進行）
        apply(new PlanEvents.PlanCreated(
                planId,
                name,
                userId,
                new HashMap<>(),
                UUID.randomUUID(),
                DateProvider.now()
        ));

        // Step 3: Postconditions
        ensure(format("Plan id is '%s'", planId), () -> getId().equals(planId));
        ensure(format("Plan name is '%s'", name), () -> getName().equals(name));
        ensure(format("User id is '%s'", userId), () -> getUserId().equals(userId));
        ensure("A PlanCreated event is generated correctly", () ->
            getLastDomainEvent() instanceof PlanEvents.PlanCreated created &&
            created.planId().equals(planId) &&
            created.name().equals(name) &&
            created.userId().equals(userId)
        );
    }

    /**
     * ✅ Command Method：重新命名 Plan
     */
    public void rename(String newName) {
        // Step 1: Preconditions
        requireNotNull("New name", newName);
        require("New name not empty", () -> !newName.trim().isEmpty());

        // Step 2: Early Exit (ignore)
        if (ignore("Name unchanged", () -> this.name.equals(newName))) {
            return;
        }

        // Step 3: 發布事件（狀態變更在 when() 中進行）
        apply(new PlanEvents.PlanRenamed(
                planId,
                newName,
                new HashMap<>(),
                UUID.randomUUID(),
                DateProvider.now()
        ));

        // Step 4: Postconditions
        ensure(format("Plan name is changed to '%s'", newName), () -> getName().equals(newName));
        ensure("A PlanRenamed event is generated correctly", () ->
            getLastDomainEvent() instanceof PlanEvents.PlanRenamed renamed &&
            renamed.planId().equals(planId) &&
            renamed.newName().equals(newName)
        );
    }

    /**
     * ✅ Command Method：創建新的 Project
     */
    public void createProject(ProjectId projectId, ProjectName projectName) {
        requireNotNull("Project id", projectId);
        requireNotNull("Project name", projectName);
        require("Project id must be unique", () -> !hasProject(projectId));

        apply(new PlanEvents.ProjectCreated(
                planId,
                projectId,
                projectName,
                new HashMap<>(),
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure(format("Project with id '%s' exists", projectId), () -> hasProject(projectId));
        ensure(format("Project name is '%s'", projectName), () -> getProject(projectId).getName().equals(projectName));
    }

    /**
     * ✅ Command Method：刪除 Project
     */
    public void deleteProject(ProjectId projectId) {
        requireNotNull("Project id", projectId);
        require("Project must exist", () -> hasProject(projectId));

        apply(new PlanEvents.ProjectDeleted(
                planId,
                projectId,
                new HashMap<>(),
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure(format("Project with id '%s' is deleted", projectId), () -> !hasProject(projectId));
    }

    /**
     * ✅ Command Method：標記 Plan 為已刪除（軟刪除）
     */
    public void delete() {
        require("Plan must not already be deleted", () -> !isDeleted);

        apply(new PlanEvents.PlanDeleted(
                planId,
                new HashMap<>(),
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure("Plan is marked as deleted", () -> isDeleted());
    }

    /**
     * ✅ when() 方法：處理所有事件並更新狀態
     *
     * Event Sourcing 的關鍵：所有狀態變更都在這裡進行
     */
    @Override
    protected void when(PlanEvents event) {
        switch (event) {
            case PlanEvents.PlanCreated e -> {
                // 初始化所有狀態
                this.planId = e.planId();
                this.name = e.name();
                this.userId = e.userId();
                this.projects = new HashMap<>();
                this.isDeleted = false;
            }
            case PlanEvents.PlanRenamed e -> {
                // 只修改名稱
                this.name = e.newName();
            }
            case PlanEvents.ProjectCreated e -> {
                // 新增專案
                Project project = new Project(e.projectId(), e.projectName(), this.planId);
                this.projects.put(e.projectId(), project);
            }
            case PlanEvents.ProjectDeleted e -> {
                // 刪除專案
                this.projects.remove(e.projectId());
            }
            case PlanEvents.PlanDeleted e -> {
                // 標記為已刪除
                this.isDeleted = true;
            }
            default -> {
                // 處理未知事件類型
            }
        }
    }

    @Override
    public boolean isDeleted() {
        return isDeleted;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public PlanId getId() {
        return planId;
    }

    @Override
    public void ensureInvariant() {
        invariant(format("Category is '%s'.", getCategory()), () -> getCategory().equals(CATEGORY));
        invariantNotNull("Plan Id", planId);
        if (!isDeleted) {
            invariantNotNull("Plan name", name);
            invariantNotNull("User Id", userId);
        }
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public boolean hasProject(ProjectId projectId) {
        return projects.containsKey(projectId);
    }

    public Project getProject(ProjectId projectId) {
        return projects.get(projectId);
    }

    public Map<ProjectId, Project> getProjects() {
        return new HashMap<>(projects);  // ✅ 防禦性複製
    }
}
```

### ✅ 使用範例

```java
// 創建新 Plan
Plan plan = new Plan(
    PlanId.create(),
    "2024 Learning Plan",
    "user123"
);

// 重新命名
plan.rename("2024 Tech Learning Plan");

// 創建專案
plan.createProject(
    ProjectId.create(),
    ProjectName.valueOf("Java Mastery")
);

// 從事件重建 Plan
List<PlanEvents> events = eventStore.loadEvents(planId);
Plan rebuiltPlan = new Plan(events);  // ✅ 狀態已完整重建
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：在業務方法中直接修改狀態
public void rename(String newName) {
    this.name = newName;  // ❌ 應該在 when() 中修改
    apply(new PlanEvents.PlanRenamed(...));
}

// ✅ 正確：只發事件，狀態在 when() 中修改
public void rename(String newName) {
    apply(new PlanEvents.PlanRenamed(...));  // ✅ 發事件
}

@Override
protected void when(PlanEvents event) {
    switch (event) {
        case PlanEvents.PlanRenamed e -> {
            this.name = e.newName();  // ✅ 在這裡修改狀態
        }
    }
}

// ❌ 錯誤：沒有實作 when() 方法
public class Plan extends EsAggregateRoot<PlanId, PlanEvents> {
    // ❌ 缺少 when() 方法
}

// ❌ 錯誤：只有單一建構子
public class Plan extends EsAggregateRoot<PlanId, PlanEvents> {
    public Plan(PlanId planId, String name, String userId) {
        // ❌ 缺少事件重建建構子
    }
}

// ✅ 正確：雙建構子
public Plan(List<PlanEvents> domainEvents) {  // ✅ 事件重建
    super(domainEvents);
}

public Plan(PlanId planId, String name, String userId) {  // ✅ 業務邏輯
    super();
    apply(new PlanEvents.PlanCreated(...));
}
```

**症狀**：事件重播失敗、狀態不一致、無法從事件重建 Aggregate。

### 🔍 檢查清單

- [ ] 繼承 `EsAggregateRoot<ID, Events>`（不是 AggregateRoot）
- [ ] 實作雙建構子（業務 + 事件重建）
- [ ] 實作 when() 方法處理所有事件
- [ ] 業務方法只發事件，不直接修改狀態
- [ ] when() 方法中修改狀態
- [ ] 使用 apply() 發布事件
- [ ] 實作 ensureInvariant()

---

## State-based vs Event Sourcing 選擇指南

### 🎯 設計意圖 (Why)

**問題**：什麼時候用 State-based？什麼時候用 Event Sourcing？

**答案**：根據業務需求和技術限制選擇合適的模式。

### 📋 決策矩陣

| 特性 | State-based | Event Sourcing |
|------|------------|----------------|
| **繼承類別** | `AggregateRoot<ID>` | `EsAggregateRoot<ID, Events>` |
| **建構子數量** | 單一建構子 | 雙建構子（業務 + 重建） |
| **狀態修改方式** | 直接修改 `this.field = value` | 透過 `when()` 方法 |
| **when() 方法** | ❌ 不需要實作 | ✅ 必須實作 |
| **事件發布時機** | 狀態修改後使用 `apply()` | 業務方法中使用 `apply()` |
| **持久化策略** | 儲存當前狀態 | 儲存事件流 |
| **狀態重建** | 從資料庫讀取 | 重播事件 |
| **複雜度** | 低 | 高 |
| **學習曲線** | 平緩 | 陡峭 |

### ✅ 使用 State-based 的時機

1. ✅ **簡單的業務邏輯**：狀態變更邏輯簡單直接
2. ✅ **不需要完整歷史**：只需要知道當前狀態
3. ✅ **傳統資料庫架構**：使用 ORM 或傳統 CRUD Repository
4. ✅ **團隊熟悉度**：開發團隊熟悉傳統的狀態修改模式
5. ✅ **快速開發**：減少 when() 方法的實作和維護

### ✅ 使用 Event Sourcing 的時機

1. ✅ **需要完整歷史**：需要追溯所有狀態變更歷史
2. ✅ **審計需求**：需要完整的事件日誌用於稽核
3. ✅ **時間旅行**：需要重建任意時間點的狀態
4. ✅ **複雜的業務邏輯**：透過事件重播確保狀態一致性
5. ✅ **Event-driven 架構**：系統本身就是 Event-driven 設計

### 🔍 檢查清單

選擇 State-based：
- [ ] 業務邏輯簡單
- [ ] 不需要歷史記錄
- [ ] 使用傳統資料庫
- [ ] 團隊熟悉傳統模式

選擇 Event Sourcing：
- [ ] 需要完整歷史
- [ ] 有審計需求
- [ ] 需要時間旅行
- [ ] Event-driven 架構

---

## 總結

Plan Aggregate 完整範例展示了：

1. ✅ **Value Object**：PlanId 使用 record 實作
2. ✅ **Domain Events**：使用 sealed interface 定義所有事件
3. ✅ **State-based Aggregate**：簡單直接的狀態修改模式
4. ✅ **Event Sourcing Aggregate**：事件溯源模式
5. ✅ **Contract-based Design**：前置條件、後置條件、不變式
6. ✅ **設計模式**：apply()、ignore、DateProvider

這個範例可以作為實作其他 Aggregate 的黃金樣板，根據專案需求選擇適合的模式。
