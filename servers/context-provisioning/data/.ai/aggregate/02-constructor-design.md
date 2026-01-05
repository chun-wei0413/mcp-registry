# Constructor 設計指南

本文件說明 Aggregate 建構子的設計原則，包括職責、Preconditions、Postconditions 和事件驗證。

---

## 建構子的職責

### 🎯 設計意圖 (Why)

**問題**：建構子應該做什麼？不應該做什麼？

**答案**：建構子負責創建一個**有效的 Aggregate 初始狀態**：
- 驗證所有必要參數
- 初始化所有欄位
- 發布 ConstructionEvent
- 驗證初始狀態正確

### 📋 實作規範 (How)

1. **State-based Aggregate**：單一建構子
   ```java
   public Plan(PlanId planId, String name, String userId) {
       super();  // 呼叫父類別建構子
       // Preconditions
       // 初始化欄位
       // 發布事件
       // Postconditions
   }
   ```

2. **Event Sourcing Aggregate**：雙建構子
   ```java
   // 業務建構子
   public Plan(PlanId planId, String name, ...) { }

   // Event Sourcing 重建構子
   public Plan(List<PlanEvents> events) {
       super(events);  // 重播事件
   }
   ```

3. **三個關鍵步驟**：
   - Step 1: Preconditions（驗證參數）
   - Step 2: 初始化狀態 + 發布事件
   - Step 3: Postconditions（驗證狀態和事件）

### ✅ 正確範例

```java
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    private PlanId planId;
    private String name;
    private String userId;
    private Map<ProjectId, Project> projects;
    private boolean isDeleted;

    public Plan(PlanId planId, String name, String userId) {
        super();

        // Step 1: Preconditions
        requireNotNull("Plan id", planId);
        requireNotNull("Plan name", name);
        requireNotNull("User id", userId);

        // Step 2: 初始化狀態 + 發布事件
        this.planId = planId;
        this.name = name;
        this.userId = userId;
        this.projects = new HashMap<>();
        this.isDeleted = false;  // 默認值

        apply(new PlanEvents.PlanCreated(
            planId, name, userId,
            new HashMap<>(), UUID.randomUUID(), DateProvider.now()
        ));

        // Step 3: Postconditions
        ensure("Plan id is set", () -> getId().equals(planId));
        ensure("Plan name is set", () -> getName().equals(name));
        ensure("Not deleted", () -> !isDeleted());
        ensure("PlanCreated event generated", () ->
            getLastDomainEvent() instanceof PlanEvents.PlanCreated);
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤 1：貧血模型 - 只有 getters/setters
public class Plan {
    private String id;
    private String name;

    // 只有 getter/setter，業務邏輯在 Service 中
}

// ❌ 錯誤 2：缺少 Postconditions
public Plan(PlanId planId, String name, String userId) {
    super();
    requireNotNull("Plan id", planId);
    this.planId = planId;
    apply(new PlanCreated(...));
    // ❌ 缺少 ensure！無法驗證狀態
}

// ❌ 錯誤 3：State-based 卻提供雙建構子
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    public Plan(PlanId planId, ...) { }  // 業務建構子
    public Plan(List<PlanEvents> events) { }  // ❌ State-based 不需要！
}
```

### 🔍 檢查清單

- [ ] 驗證所有必要參數（requireNotNull）
- [ ] 初始化所有欄位（包括默認值）
- [ ] 發布 ConstructionEvent
- [ ] 驗證初始狀態正確
- [ ] State-based 只需單一建構子

---

## Preconditions 設計

### 🎯 設計意圖 (Why)

**問題**：如何確保建構子的參數有效？

**答案**：使用 `requireNotNull()` 和 `require()` 驗證所有參數。

### 📋 實作規範 (How)

1. **驗證順序**：
   - 先 `requireNotNull()`（null 檢查）
   - 再 `require()`（業務規則檢查）

2. **清晰的錯誤訊息**：
   ```java
   requireNotNull("User id", userId);  // ✅ 清楚指出是哪個參數
   require("Name not empty", () -> !name.isBlank());  // ✅ 說明規則
   ```

### ✅ 正確範例

```java
public Plan(PlanId planId, String name, String userId) {
    super();

    // Null 檢查
    requireNotNull("Plan id", planId);
    requireNotNull("Plan name", name);
    requireNotNull("User id", userId);

    // 業務規則檢查
    require("Plan name not empty", () -> !name.trim().isEmpty());
    require("Plan name length valid", () ->
        name.trim().length() >= 1 && name.trim().length() <= 100);

    // ...
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：沒有驗證參數
public Plan(PlanId planId, String name) {
    super();
    this.planId = planId;  // 如果 planId 是 null？
    this.name = name;
}

// ❌ 錯誤：錯誤訊息不清楚
requireNotNull("Invalid input", userId);  // 哪個參數？
```

### 🔍 檢查清單

- [ ] 所有參數都經過 requireNotNull()
- [ ] 業務規則使用 require() 驗證
- [ ] 錯誤訊息清楚描述問題

---

## Postconditions 設計

### 🎯 設計意圖 (Why)

**問題**：如何確保建構子正確初始化了 Aggregate？

**答案**：使用 `ensure()` 驗證：
- 所有欄位正確設定
- ConstructionEvent 正確產生
- Aggregate 處於有效狀態

### 📋 實作規範 (How)

驗證兩大類：
1. **業務狀態**：欄位值正確
2. **Domain Event**：ConstructionEvent 產生且包含正確資料

### ✅ 正確範例

```java
public Plan(PlanId planId, String name, String userId) {
    super();
    requireNotNull(...);

    // 初始化
    this.planId = planId;
    this.name = name;
    this.userId = userId;
    this.projects = new HashMap<>();
    // ...

    apply(new PlanEvents.PlanCreated(...));

    // Postconditions - 業務狀態
    ensure("Plan id is set", () -> getId().equals(planId));
    ensure("Plan name is set", () -> getName().equals(name));
    ensure("Plan is not deleted", () -> !isDeleted());

    // Postconditions - Domain Event
    ensure("PlanCreated event generated correctly", () -> {
        var lastEvent = getLastDomainEvent();
        return lastEvent instanceof PlanEvents.PlanCreated created &&
               created.planId().equals(planId) &&
               created.name().equals(name) &&
               created.userId().equals(userId);
    });
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：缺少事件驗證
ensure("Plan id is set", () -> getId().equals(planId));
// ❌ 缺少：ensure("Event generated", ...)

// ❌ 錯誤：沒有驗證事件內容
ensure("Event generated", () -> getLastDomainEvent() != null);
// ❌ 應該驗證事件類型和內容
```

### 🔍 檢查清單

- [ ] 驗證所有欄位正確設定
- [ ] 驗證 ConstructionEvent 產生
- [ ] 驗證事件包含正確資料
- [ ] 驗證 Aggregate 處於有效狀態

---

## 事件產生驗證

### 🎯 設計意圖 (Why)

**問題**：如何確保建構子產生了正確的 ConstructionEvent？

**答案**：驗證事件的類型、內容和標記。

### 📋 實作規範 (How)

1. **驗證事件類型**：
   ```java
   ensure("Event is PlanCreated", () ->
       getLastDomainEvent() instanceof PlanEvents.PlanCreated);
   ```

2. **驗證事件內容**：
   ```java
   ensure("Event contains correct data", () -> {
       var event = (PlanEvents.PlanCreated) getLastDomainEvent();
       return event.planId().equals(planId) &&
              event.name().equals(name) &&
              event.userId().equals(userId);
   });
   ```

3. **驗證 ConstructionEvent 標記**：
   ```java
   // ConstructionEvent 必須實作 InternalDomainEvent.ConstructionEvent
   record PlanCreated(...)
       implements PlanEvents, InternalDomainEvent.ConstructionEvent {
   }
   ```

### ✅ 正確範例

```java
// Domain Event 定義
record PlanCreated(
    PlanId planId,
    String name,
    String userId,
    Map<String, String> metadata,
    UUID id,
    Instant occurredOn
) implements PlanEvents, InternalDomainEvent.ConstructionEvent {
    // ✅ 實作 ConstructionEvent 標記
}

// 建構子中驗證
public Plan(PlanId planId, String name, String userId) {
    super();
    // ...
    apply(new PlanEvents.PlanCreated(...));

    ensure("PlanCreated event generated correctly", () -> {
        var lastEvent = getLastDomainEvent();
        return lastEvent instanceof PlanEvents.PlanCreated created &&
               created.planId().equals(planId) &&
               created.name().equals(name) &&
               created.userId().equals(userId);
    });
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：事件沒有實作 ConstructionEvent
record PlanCreated(...)
    implements PlanEvents {  // ❌ 缺少 ConstructionEvent
}

// ❌ 錯誤：只驗證事件存在，不驗證內容
ensure("Event generated", () -> getLastDomainEvent() != null);
// 應該驗證事件包含正確的 planId, name 等
```

### 🔍 檢查清單

- [ ] ConstructionEvent 實作 `InternalDomainEvent.ConstructionEvent`
- [ ] 驗證事件類型正確
- [ ] 驗證事件包含所有必要資料
- [ ] 驗證事件的 id 和 occurredOn 不為 null

---

## 總結

建構子設計的關鍵原則：
1. **三步驟**：Preconditions → 初始化 → Postconditions
2. **State-based**：單一建構子
3. **完整驗證**：參數、狀態、事件都要驗證