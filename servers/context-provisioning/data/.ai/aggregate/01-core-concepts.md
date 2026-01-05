# Aggregate 核心概念

本文件說明 Aggregate 在 Domain-Driven Design 中的核心概念，包括定義、角色、邊界，以及在 ezKanban 專案中的應用。

---

## Aggregate 定義與核心概念

### 🎯 設計意圖 (Why)

**問題**：在複雜的領域模型中，如何確保資料的一致性？如何定義修改的邊界？

**答案**：Aggregate 是一組相關物件的集合，它們作為一個整體來維護資料的一致性。Aggregate 定義了：
- **一致性邊界（Consistency Boundary）**：在這個邊界內，所有修改必須保持一致
- **事務邊界（Transaction Boundary）**：一個 Aggregate 的修改在一個事務中完成
- **變更單位（Unit of Change）**：一起變更、一起儲存的最小單位

### 📋 實作規範 (How)

1. **Aggregate 是一個物件叢集（Cluster of Objects）**：
   - 包含一個 **Aggregate Root**（聚合根）
   - 可能包含多個 **Entity**（實體）
   - 可能包含多個 **Value Object**（值物件）

2. **外部只能通過 Aggregate Root 訪問內部物件**：
   - 所有修改都經過 Aggregate Root
   - 確保業務規則在 Root 中執行
   - 維護 Aggregate 內部的一致性

3. **Aggregate 之間通過 ID 引用**：
   - 不直接持有其他 Aggregate 的物件引用
   - 使用 ID（如 ProjectId、PlanId、TagId）進行關聯
   - 避免 Aggregate 之間的複雜依賴

### ✅ 正確範例

```java
// Plan Aggregate：包含多個 Project Entity
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    private PlanId planId;
    private String name;
    private List<Project> projects;  // 內部 Entity，外部不能直接訪問

    // 外部只能通過 Root 的方法操作 Project
    public void createProject(ProjectId projectId, ProjectName projectName) {
        requireNotNull("Project id", projectId);
        requireNotNull("Project name", projectName);

        // 業務規則在 Root 中執行
        require("Project not exists", () -> !hasProject(projectId));

        // 修改狀態 + 發布事件
        Project project = new Project(projectId, projectName);
        projects.add(project);

        apply(new PlanEvents.ProjectCreated(
            planId,
            projectId,
            projectName,
            new HashMap<>(),
            UUID.randomUUID(),
            DateProvider.now()
        ));

        ensure("Project exists", () -> hasProject(projectId));
    }

    // 封裝查詢邏輯
    public boolean hasProject(ProjectId projectId) {
        return projects.stream()
            .anyMatch(p -> p.getId().equals(projectId));
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤 1：外部直接修改 Aggregate 內部
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    private List<Project> projects;

    // ❌ 暴露內部集合的可變引用
    public List<Project> getProjects() {
        return projects;  // 外部可以直接 add/remove！
    }
}

// 外部程式碼
Plan plan = planRepository.findById(planId);
plan.getProjects().add(new Project(...));  // ❌ 繞過了 Aggregate Root！


// ❌ 錯誤 2：Aggregate 之間直接引用物件
public class Task extends AggregateRoot<TaskId> {
    private TaskId taskId;
    private Project project;  // ❌ 直接持有其他 Aggregate 的物件
}

// ✅ 正確：使用 ID 引用
public class Task extends AggregateRoot<TaskId> {
    private TaskId taskId;
    private ProjectId projectId;  // ✅ 只持有 ID
}
```

**症狀**：
- 錯誤 1：業務規則被繞過，資料不一致
- 錯誤 2：Aggregate 之間耦合，難以獨立演化

### 🔍 檢查清單

- [ ] Aggregate 有明確的邊界
- [ ] 外部只通過 Aggregate Root 訪問內部物件
- [ ] Aggregate 之間使用 ID 引用，不直接持有物件
- [ ] 所有業務規則在 Aggregate Root 中執行
- [ ] Getters 返回不可變集合或防禦性複製

---

## Aggregate Root 的角色

### 🎯 設計意圖 (Why)

**問題**：Aggregate 內部可能有多個 Entity，誰負責統籌協調？

**答案**：Aggregate Root 是 Aggregate 的「守門員」和「協調者」，負責：
- 控制對內部物件的訪問
- 執行業務規則
- 維護 Aggregate 的不變式（Invariant）
- 發布領域事件

### 📋 實作規範 (How)

1. **Aggregate Root 是 Aggregate 的入口點**：
   - 外部只能取得 Root 的引用
   - 所有操作都經過 Root 的方法

2. **Aggregate Root 維護全域不變式**：
   - 實作 `ensureInvariant()` 方法
   - 在每次狀態變更後驗證不變式
   - 確保 Aggregate 始終處於有效狀態

3. **Aggregate Root 發布領域事件**：
   - 通過 `apply()` 發布事件
   - 記錄 Aggregate 的狀態變更
   - 通知外部系統

### ✅ 正確範例

```java
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    public static final String CATEGORY = "Plan";

    private PlanId planId;
    private String name;
    private List<Project> projects = new ArrayList<>();
    private boolean isDeleted;

    // Root 統一協調所有操作
    public void renameProject(ProjectId projectId, ProjectName newName) {
        requireNotNull("Project id", projectId);
        requireNotNull("New name", newName);
        require("Plan must not be deleted", () -> !isDeleted);

        // 找到內部 Entity
        Project project = findProject(projectId);
        require("Project exists", () -> project != null);

        // 委託給 Entity 執行（但仍在 Root 的控制下）
        String oldName = project.getName();
        project.rename(newName);

        // Root 發布事件
        apply(new PlanEvents.ProjectRenamed(
            planId,
            projectId,
            oldName,
            newName,
            new HashMap<>(),
            UUID.randomUUID(),
            DateProvider.now()
        ));

        ensure("Project name updated", () ->
            findProject(projectId).getName().equals(newName));
    }

    // Root 維護不變式
    @Override
    public void ensureInvariant() {
        invariant("Category correct", () -> getCategory().equals(CATEGORY));
        invariantNotNull("Plan Id", planId);

        if (!isDeleted) {
            invariantNotNull("Plan name", name);
            invariant("Projects not null", () -> projects != null);
        }
    }

    @Override
    public PlanId getId() {
        return planId;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public boolean isDeleted() {
        return isDeleted;
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：Entity 自己發布領域事件
public class Project {
    private ProjectId projectId;
    private String name;

    public void rename(String newName) {
        this.name = newName;
        // ❌ Entity 不應該發布事件，應由 Root 發布
        DomainEventPublisher.publish(new ProjectRenamed(...));
    }
}


// ❌ 錯誤：Root 沒有維護不變式
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    // ❌ 缺少 ensureInvariant() 實作
    // 無法保證 Aggregate 始終有效
}
```

**症狀**：
- Entity 直接發布事件：事件來源不明確，難以追蹤
- 缺少不變式：Aggregate 可能處於無效狀態

### 🔍 檢查清單

- [ ] 只有 Aggregate Root 發布領域事件
- [ ] Root 實作了 `ensureInvariant()` 方法
- [ ] Root 控制對內部 Entity 的訪問
- [ ] 所有修改操作都經過 Root 的方法

---

## 一致性邊界與事務邊界

### 🎯 設計意圖 (Why)

**問題**：如何決定 Aggregate 的大小？什麼應該放在同一個 Aggregate 內？

**答案**：Aggregate 定義了**一致性邊界**：
- 邊界內的資料必須在同一個事務中保持一致
- 邊界外的資料可以**最終一致**（Eventually Consistent）
- 小而聚焦的 Aggregate 更容易維護和擴展

### 📋 實作規範 (How)

1. **設計小而聚焦的 Aggregate**：
   - 只包含必須同時修改的資料
   - 避免包含「可以稍後更新」的資料
   - 優先考慮 **最終一致性**

2. **一個事務只修改一個 Aggregate**：
   - 避免跨 Aggregate 的事務
   - 使用領域事件協調多個 Aggregate
   - 通過 Saga 或 Process Manager 處理複雜流程

3. **識別真正的一致性需求**：
   - **強一致性**：必須在同一事務中完成（同一 Aggregate）
   - **最終一致性**：可以稍後完成（不同 Aggregate，通過事件）

### ✅ 正確範例

```java
// ✅ 好的設計：小而聚焦的 Aggregate
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    private PlanId id;
    private String name;
    private List<Project> projects;  // 少量的嵌套 Entity

    // Plan 負責管理其內部的 Projects
    // 因為 Plan 和 Project 必須同時保持一致
}

public class Task extends AggregateRoot<TaskId> {
    private TaskId id;
    private String name;
    private ProjectId projectId;  // ✅ 使用 ID 引用

    // Task 是獨立的 Aggregate
    // Task 的變更不需要 Project 同時變更
}

// 通過領域事件協調
public class PlanApplicationService {
    public void deleteProject(PlanId planId, ProjectId projectId) {
        // 1. 修改 Plan Aggregate
        Plan plan = planRepository.findById(planId);
        plan.deleteProject(projectId);
        planRepository.save(plan);

        // 2. 領域事件 ProjectDeleted 被發布

        // 3. Event Handler 處理 Task 的更新（最終一致）
        // TaskEventHandler.on(ProjectDeleted event) {
        //     找到屬於該 Project 的所有 Task 並標記為孤兒
        // }
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：Aggregate 過大
public class Company {
    private CompanyId id;
    private List<Employee> employees;      // 可能有數千個
    private List<Department> departments;  // 可能有數百個
    private List<Project> projects;        // 可能有數百個
    private List<Customer> customers;      // 可能有數千個

    // ❌ 問題：
    // 1. 載入 Company 會載入所有關聯資料（效能問題）
    // 2. 修改任何一個 Employee 都需要鎖定整個 Company（並發問題）
    // 3. Company 的不變式變得非常複雜
}

// ✅ 正確：拆分為多個 Aggregate
public class Company {
    private CompanyId id;
    private String name;
    // 只保留核心屬性
}

public class Employee {
    private EmployeeId id;
    private CompanyId companyId;  // 通過 ID 引用
    // Employee 是獨立的 Aggregate
}
```

**症狀**：
- 載入時間過長（包含太多資料）
- 並發衝突頻繁（多人修改同一 Aggregate）
- 不變式複雜難以維護

### 🔍 檢查清單

- [ ] Aggregate 大小適中（不超過 2-3 層嵌套）
- [ ] 一次事務只修改一個 Aggregate
- [ ] 使用領域事件協調多個 Aggregate
- [ ] 區分強一致性和最終一致性需求
- [ ] Aggregate 之間使用 ID 引用

---

## ezKanban 專案 Package 結構

### 🎯 設計意圖 (Why)

**問題**：如何組織專案的 package 結構，讓 Aggregate 和相關元件容易管理？

**答案**：ezKanban 使用 **Bounded Context + Aggregate** 的命名策略：
- 每個 Bounded Context 有自己的 package
- 每個 Aggregate 在 Bounded Context 下有自己的子 package
- Entity 層包含 Aggregate Root、Domain Events、Value Objects、Enums

### 📋 實作規範 (How)

1. **Bounded Context 層級**：
   ```
   ntut.csie.sslab.[bounded-context].[aggregate]
   ```
   - 範例：`ntut.csie.sslab.accounts.user`
   - 範例：`ntut.csie.sslab.kanban.board`

2. **Entity 層級**：
   ```
   [bounded-context].[aggregate].entity
   ```
   - Aggregate Root、Domain Events、Value Objects、Enums 都放在此

3. **共用類別**：
   ```
   ntut.csie.sslab.common
   ```
   - DateProvider、共用的 Value Objects 等

### ✅ 正確範例

```
tw.teddysoft
├── example.common                   # 共用工具類別
│   └── DateProvider                # 時間提供者（用於測試）
│
└── example.plan                     # Plan Bounded Context
    ├── entity                       # 領域模型層
    │   ├── Plan.java               # Plan Aggregate Root
    │   ├── PlanId.java             # Plan ID Value Object
    │   ├── PlanEvents.java         # Plan Domain Events
    │   ├── Project.java            # Project Entity
    │   └── ProjectId.java          # Project ID Value Object
    ├── usecase                      # 應用層
    ├── adapter                      # 適配器層
    └── ...
```

**實際檔案範例**：

```java
// Plan Aggregate Root
package tw.teddysoft.example.plan.entity;

// 引用共用工具
import tw.teddysoft.example.common.DateProvider;

// 引用 ezddd 框架
import tw.teddysoft.ezddd.entity.AggregateRoot;
import static tw.teddysoft.ucontract.Contract.*;

import java.util.HashMap;
import java.util.UUID;

public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    private PlanId planId;
    private String name;
    // ...

    public Plan(PlanId planId, String name, String userId) {
        super();

        requireNotNull("Plan id", planId);
        // ...

        apply(new PlanEvents.PlanCreated(
            planId,
            name,
            userId,
            new HashMap<>(),
            UUID.randomUUID(),
            DateProvider.now()  // 使用共用的 DateProvider
        ));
    }
}
```

```java
// PlanId Value Object
package tw.teddysoft.example.plan.entity;

import tw.teddysoft.ezddd.entity.ValueObject;

public record PlanId(String value) implements ValueObject {
    public PlanId {
        Objects.requireNonNull(value, "PlanId cannot be null");
    }

    public static PlanId create() {
        return new PlanId(UUID.randomUUID().toString());
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤 1：所有類別都放在同一個 package
package tw.teddysoft.example.entity;

public class Plan { }
public class Sprint { }
public class Task { }
// 問題：不同 Bounded Context 的 Aggregate 混在一起


// ❌ 錯誤 2：Value Object 和 Aggregate 分開在不同 package
package tw.teddysoft.example.plan.entity;
public class Plan { }

package tw.teddysoft.example.plan.vo;  // ❌ 不需要獨立 vo package
public class PlanId { }

// ✅ 正確：都放在 entity package
package tw.teddysoft.example.plan.entity;
public class Plan { }
public class PlanId { }
```

### 🔍 檢查清單

- [ ] 遵循 `ntut.csie.sslab.[bounded-context].[aggregate]` 命名
- [ ] Aggregate Root、Events、Value Objects 都在 `entity` package
- [ ] 使用 `ntut.csie.sslab.common.DateProvider`
- [ ] 引用 ezddd 框架：`tw.teddysoft.ezddd.entity.AggregateRoot`
- [ ] 引用 uContract：`import static tw.teddysoft.ucontract.Contract.*;`

---

## 總結

Aggregate 是 DDD 的核心戰術模式：
- **定義一致性邊界**：確保資料的一致性
- **Aggregate Root 是守門員**：控制訪問、執行規則、發布事件
- **小而聚焦**：避免過大的 Aggregate
- **清晰的 Package 結構**：按 Bounded Context 和 Aggregate 組織