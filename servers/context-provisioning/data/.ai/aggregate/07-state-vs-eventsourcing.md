# State-based vs Event Sourcing 對比指南

本文件對比兩種 Aggregate 實作模式的差異、選擇指南和範例。

---

## 核心差異對比

### 🎯 設計意圖

**問題**：State-based 和 Event Sourcing 的核心差異是什麼？

**答案**：主要差異在於**狀態修改的方式**：

| 特性 | State-based Aggregate | Event Sourcing Aggregate |
|------|----------------------|--------------------------|
| **繼承類別** | `AggregateRoot<ID>` | `EsAggregateRoot<ID, Events>` |
| **建構子** | 單一建構子 | 雙建構子（業務 + 重建） |
| **when() 方法** | ❌ **不需要** | ✅ **必須實作** |
| **狀態修改** | apply() **之前**直接修改 | apply() 後由 when() 修改 |
| **持久化** | 傳統 ORM（儲存當前狀態） | Event Store（儲存事件流） |

---

## 選擇指南

### 🎯 何時使用 State-based

**適用場景**：
- 使用傳統資料庫（MySQL, PostgreSQL）
- 不需要完整的事件歷史
- 團隊不熟悉 Event Sourcing
- 簡單的 CRUD 應用

### 🎯 何時使用 Event Sourcing

**適用場景**：
- 需要完整的審計追蹤
- 需要事件溯源（重建任何時間點的狀態）
- 需要複雜的事件處理邏輯
- 事件驅動架構

---

## State-based 完整範例

### 📋 實作要點

1. **繼承 `AggregateRoot<ID>`**
2. **單一建構子**
3. **不需要 when() 方法**
4. **直接修改狀態，再 apply 事件**

### ✅ 正確範例

```java
public class User extends AggregateRoot<UserId> {
    public static final String CATEGORY = "User";

    private UserId userId;
    private String username;
    private boolean isDeleted;

    // 單一建構子
    public User(UserId userId, String username, ...) {
        super();

        requireNotNull("User id", userId);
        requireNotNull("Username", username);

        // 1. 直接修改狀態
        this.userId = userId;
        this.username = username;
        this.isDeleted = false;

        // 2. 發布事件
        apply(new UserEvents.UserRegistered(
            userId, username, ...,
            new HashMap<>(),
            UUID.randomUUID(),
            DateProvider.now()
        ));

        // 3. 驗證
        ensure("User id set", () -> getId().equals(userId));
    }

    // Command Method
    public void delete() {
        require("Not already deleted", () -> !isDeleted);

        // 1. 直接修改狀態
        this.isDeleted = true;

        // 2. 發布事件
        apply(new UserEvents.UserDeleted(...));

        ensure("Marked as deleted", () -> isDeleted());
    }

    // ❌ State-based 不需要 when() 方法

    @Override
    public UserId getId() {
        return userId;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public boolean isDeleted() {
        return isDeleted;
    }

    @Override
    public void ensureInvariant() {
        invariant("Category correct", () -> getCategory().equals(CATEGORY));
        invariantNotNull("User Id", userId);
        if (!isDeleted) {
            invariantNotNull("Username", username);
        }
    }
}
```

---

## Event Sourcing 完整範例

### 📋 實作要點

1. **繼承 `EsAggregateRoot<ID, Events>`**
2. **雙建構子**（業務 + 重建）
3. **必須實作 when() 方法**
4. **apply 事件，由 when() 修改狀態**

### ✅ 正確範例

```java
public class Plan extends EsAggregateRoot<PlanId, PlanEvents> {
    public static final String CATEGORY = "Plan";

    private PlanId planId;
    private String name;
    private boolean isDeleted;

    // 業務建構子
    public Plan(PlanId planId, String name, UserId userId) {
        super();

        requireNotNull("Plan id", planId);
        requireNotNull("Name", name);

        // ❌ 不直接修改狀態
        // this.planId = planId;  // 錯誤！

        // ✅ 透過 apply + when 修改
        apply(new PlanEvents.PlanCreated(
            planId, name, userId,
            new HashMap<>(),
            UUID.randomUUID(),
            DateProvider.now()
        ));

        ensure("Plan id set", () -> getId().equals(planId));
    }

    // Event Sourcing 重建構子
    public Plan(List<PlanEvents> events) {
        super(events);  // 重播事件
    }

    // Command Method
    public void rename(String newName) {
        requireNotNull("New name", newName);

        if (ignore("Name unchanged", () -> name.equals(newName))) {
            return;
        }

        // ❌ 不直接修改狀態
        // this.name = newName;  // 錯誤！

        // ✅ 透過 apply + when 修改
        apply(new PlanEvents.PlanRenamed(
            planId, newName,
            new HashMap<>(),
            UUID.randomUUID(),
            DateProvider.now()
        ));

        ensure("Name updated", () -> getName().equals(newName));
    }

    // ✅ Event Sourcing 必須實作 when() 方法
    @Override
    protected void when(PlanEvents event) {
        switch (event) {
            case PlanEvents.PlanCreated e -> {
                this.planId = e.planId();
                this.name = e.name();
                this.isDeleted = false;
            }
            case PlanEvents.PlanRenamed e -> {
                this.name = e.newName();
            }
            case PlanEvents.PlanDeleted e -> {
                this.isDeleted = true;
            }
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

    @Override
    public void ensureInvariant() {
        invariant("Category correct", () -> getCategory().equals(CATEGORY));
        invariantNotNull("Plan Id", planId);
        if (!isDeleted) {
            invariantNotNull("Name", name);
        }
    }
}
```

---

## 轉換指南

### State-based → Event Sourcing

1. 改繼承類別：`AggregateRoot<ID>` → `EsAggregateRoot<ID, Events>`
2. 新增 Event Sourcing 重建構子
3. 新增 when() 方法
4. 修改業務方法：先 apply，狀態由 when() 修改

### Event Sourcing → State-based

1. 改繼承類別：`EsAggregateRoot<ID, Events>` → `AggregateRoot<ID>`
2. 移除 Event Sourcing 重建構子
3. 移除 when() 方法
4. 修改業務方法：先修改狀態，再 apply

---

## 總結

- **State-based**：簡單、直觀、適合傳統應用
- **Event Sourcing**：複雜、強大、適合事件驅動

選擇取決於專案需求和團隊能力。