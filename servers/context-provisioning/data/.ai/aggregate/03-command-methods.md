# Command Methods 設計指南

本文件說明如何設計 Aggregate 的 Command Methods，包括 ignore vs require 的正確使用、四步驟模式，以及 apply() 的使用。

---

## ignore vs require 的正確使用

### 🎯 設計意圖 (Why)

**問題**：如何區分「避免不必要操作」和「前置條件驗證」？

**答案**：
- `ignore()`：用於避免產生不必要的 domain event（業務優化，提升效能）
- `require()`：用於檢查前置條件，不滿足時拋出異常（業務規則，必須滿足）

**關鍵差異**：
| | ignore() | require() |
|---|---|---|
| **用途** | 避免不必要操作 | 驗證前置條件 |
| **返回值** | boolean（true=應該忽略） | void（失敗拋異常） |
| **失敗行為** | return（提前退出） | throw Exception |
| **典型場景** | 新值與舊值相同 | 輸入參數為 null |

### 📋 實作規範 (How)

1. **require**：用於驗證輸入參數、業務前置條件
   - 參數不能為 null
   - 業務狀態必須滿足（如「未刪除」、「存在」）
   - 失敗時拋出 `PreconditionViolationException`

2. **ignore**：用於檢查「是否需要執行操作」
   - 返回 true 時提前退出（不產生 event）
   - 返回 false 時繼續執行
   - 典型用法：`if (ignore(...)) { return; }`

3. **順序**：先 require（驗證），再 ignore（優化）
   - 確保在做比較前輸入已驗證（避免 NullPointerException）

### ✅ 正確範例

```java
public void rename(String newName) {
    // 1. Preconditions（使用 require）
    requireNotNull("New name", newName);
    require("Name must not be empty", () -> !newName.isBlank());

    // 2. Early Exit（使用 ignore）
    if (ignore("Name unchanged", () -> this.name.equals(newName))) {
        return; // 不產生 event，直接返回
    }

    // 3. Action
    this.name = newName;
    apply(new ProductRenamed(this.id, newName, ...));

    // 4. Postconditions
    ensure("Name is updated", () -> this.name.equals(newName));
}

// ✅ 正確：require 用於前置條件檢查
public void deleteTask(TaskId taskId, String reason, String userId) {
    requireNotNull("taskId", taskId);
    requireNotNull("userId", userId);

    // 使用 require 檢查前置條件，不滿足時拋出異常
    require("Task must exist", () -> getTask(taskId).isPresent());

    apply(new TaskDeleted(this.id, taskId, reason, userId, ...));

    ensure("Task is deleted", () -> !getTask(taskId).isPresent());
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤 1：混淆 ignore 和 require 的用途
public void deleteTask(TaskId taskId) {
    // ❌ 錯誤：task 不存在應該拋異常，不是「忽略操作」
    if (ignore("Task not found", () -> !hasTask(taskId))) {
        return; // 這會默默地什麼都不做！使用者不知道發生了什麼
    }

    // ✅ 正確：應該用 require
    require("Task must exist", () -> hasTask(taskId));
}

// ❌ 錯誤 2：順序錯誤
public void rename(String newName) {
    // ❌ 錯誤：先 ignore 再 require，可能對 null 做比較
    if (ignore("Name unchanged", () -> this.name.equals(newName))) {
        return; // 如果 newName 是 null，會拋 NullPointerException
    }

    requireNotNull("New name", newName); // 太晚了！

    // ✅ 正確順序：先 require 再 ignore
}

// ❌ 錯誤 3：用 require 做優化檢查
public void updateStatus(Status newStatus) {
    // ❌ 錯誤：狀態相同不是「錯誤」，只是不需要執行
    require("Status changed", () -> !this.status.equals(newStatus));

    // ✅ 正確：應該用 ignore
    if (ignore("Status unchanged", () -> this.status.equals(newStatus))) {
        return;
    }
}
```

**症狀**：
- 錯誤 1：業務異常被默默吞掉，難以除錯
- 錯誤 2：NullPointerException 而非清晰的 PreconditionViolationException
- 錯誤 3：正常情況被當作錯誤處理

### 🔍 檢查清單

- [ ] require 用於前置條件，失敗時拋異常
- [ ] ignore 用於避免不必要操作，返回 true 時 return
- [ ] require 在 ignore 之前執行
- [ ] ignore 的條件不會引發 NullPointerException
- [ ] 不混淆「錯誤」和「不需要執行」

---

## Command Method 四步驟模式

### 🎯 設計意圖 (Why)

**問題**：如何設計一個完整且安全的 Command Method？

**答案**：遵循 **Preconditions → Early Exit → Action → Postconditions** 四步驟模式：
1. **Preconditions**：驗證輸入和業務前置條件
2. **Early Exit**：避免不必要的操作
3. **Action**：修改狀態 + 發布事件
4. **Postconditions**：驗證結果和事件

### 📋 實作規範 (How)

```
Command Method 標準結構：

public void commandName(Parameters...) {
    // Step 1: Preconditions
    requireNotNull(...)
    require(...)

    // Step 2: Early Exit (optional)
    if (ignore(...)) { return; }

    // Step 3: Action
    var oldState = old(() -> ...);  // 捕獲舊狀態（if needed）
    // 修改狀態（State-based）或 apply 事件（Event Sourcing）
    this.field = newValue;
    apply(new Event(...));

    // Step 4: Postconditions
    ensure(...)
    ensureAssignable(...)  // 驗證只有允許的欄位改變
}
```

### ✅ 正確範例

```java
public void updateName(String newName) {
    // Step 1: Preconditions
    requireNotNull("New name", newName);
    require("Name must not be empty", () -> !newName.isBlank());
    require("Name length valid", () -> newName.length() <= 100);
    require("Plan must not be deleted", () -> !isDeleted);

    // Step 2: Early Exit
    if (ignore("Name unchanged", () -> this.name.equals(newName))) {
        return;
    }

    // Step 3: Action
    var oldName = this.name;
    this.name = newName;  // State-based：先修改狀態

    apply(new PlanEvents.PlanRenamed(
        planId,
        oldName,
        newName,
        new HashMap<>(),
        UUID.randomUUID(),
        DateProvider.now()
    ));

    // Step 4: Postconditions
    ensure("Name is updated", () -> this.name.equals(newName));
    ensure("Name is different from old", () -> !this.name.equals(oldName));
    ensure("Event is generated correctly", () -> {
        var lastEvent = getLastDomainEvent();
        return lastEvent instanceof PlanEvents.PlanRenamed renamed &&
               renamed.planId().equals(planId) &&
               renamed.newName().equals(newName) &&
               renamed.oldName().equals(oldName);
    });
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤 1：缺少後置條件驗證
public void updateName(String newName) {
    requireNotNull("New name", newName);
    this.name = newName;
    apply(new PlanRenamed(...));
    // ❌ 缺少 ensure！無法驗證狀態是否正確更新
}

// ❌ 錯誤 2：State-based 時先 apply 再修改狀態
public void updateName(String newName) {
    requireNotNull("New name", newName);
    apply(new PlanRenamed(...)); // ❌ 順序錯誤
    this.name = newName;         // State-based 應該先改狀態
}

// ❌ 錯誤 3：沒有檢查業務狀態
public void updateName(String newName) {
    requireNotNull("New name", newName);
    // ❌ 缺少：require("Not deleted", () -> !isDeleted);
    this.name = newName;
    apply(...);
    // 問題：可能更新已刪除的 Aggregate
}
```

**症狀**：
- 錯誤 1：狀態更新失敗但沒被發現
- 錯誤 2：事件包含錯誤的資料（舊狀態）
- 錯誤 3：違反業務規則（修改已刪除的物件）

### 🔍 檢查清單

- [ ] 包含 4 個步驟：Preconditions → Early Exit → Action → Postconditions
- [ ] State-based：先修改狀態，再 apply
- [ ] Event Sourcing：先 apply，狀態由 when() 修改
- [ ] 後置條件同時驗證狀態和事件
- [ ] 檢查 Aggregate 的業務狀態（如 isDeleted）

---

## apply() 的正確使用

### 🎯 設計意圖 (Why)

**問題**：為什麼要使用 `apply()` 而非 `addDomainEvent()`？

**答案**：`apply()` 提供了 **null 安全檢查**：
- `apply()` 會檢查 event 是否為 null，避免 NullPointerException
- `addDomainEvent()` 沒有 null 檢查，直接加入 event 可能導致錯誤
- `apply()` 是框架推薦的最佳實踐

### 📋 實作規範 (How)

1. **永遠使用 `apply()` 發布事件**：
   ```java
   apply(new PlanCreated(...));  // ✅ 正確
   ```

2. **不使用 `addDomainEvent()`**：
   ```java
   addDomainEvent(new PlanCreated(...));  // ❌ 錯誤
   ```

3. **State-based Aggregate 的順序**：
   ```java
   // 1. 先修改狀態
   this.name = newName;

   // 2. 再 apply 事件
   apply(new NameChanged(...));
   ```

4. **Event Sourcing Aggregate 的順序**：
   ```java
   // 1. 先 apply 事件
   apply(new NameChanged(...));

   // 2. when() 方法修改狀態
   @Override
   protected void when(Events event) {
       switch (event) {
           case NameChanged e -> this.name = e.newName();
       }
   }
   ```

### ✅ 正確範例

```java
// ✅ State-based Aggregate
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    private String name;

    public void rename(String newName) {
        requireNotNull("New name", newName);

        // 1. 先修改狀態
        this.name = newName;

        // 2. 使用 apply() 發布事件
        apply(new PlanEvents.PlanRenamed(
            planId,
            newName,
            new HashMap<>(),  // metadata 必須是可變的
            UUID.randomUUID(),
            DateProvider.now()  // 使用 DateProvider
        ));

        ensure("Name updated", () -> this.name.equals(newName));
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤 1：使用 addDomainEvent()
public void rename(String newName) {
    this.name = newName;
    addDomainEvent(new PlanRenamed(...));  // ❌ 沒有 null 檢查！
}

// ❌ 錯誤 2：State-based 時先 apply 再修改狀態
public void rename(String newName) {
    // ❌ 錯誤順序
    apply(new PlanRenamed(planId, newName, ...));
    this.name = newName;  // 事件包含的是舊的 name！

    // ✅ 正確順序
    this.name = newName;
    apply(new PlanRenamed(planId, newName, ...));
}

// ❌ 錯誤 3：metadata 使用不可變 Map
public void rename(String newName) {
    apply(new PlanRenamed(
        planId,
        newName,
        Map.of(),  // ❌ 不可變的 Map！框架無法添加 metadata
        UUID.randomUUID(),
        DateProvider.now()
    ));

    // ✅ 正確：使用可變的 HashMap
    apply(new PlanRenamed(
        planId,
        newName,
        new HashMap<>(),  // ✅ 可變的 Map
        UUID.randomUUID(),
        DateProvider.now()
    ));
}
```

**症狀**：
- 錯誤 1：可能發生 NullPointerException
- 錯誤 2：事件包含錯誤的資料
- 錯誤 3：框架無法添加 audit metadata（如 userId）

### 🔍 檢查清單

- [ ] 使用 `apply()` 而非 `addDomainEvent()`
- [ ] State-based：先修改狀態，再 apply
- [ ] Event Sourcing：先 apply，狀態由 when() 修改
- [ ] metadata 使用 `new HashMap<>()`
- [ ] 使用 `DateProvider.now()` 而非 `Instant.now()`
- [ ] 使用 `UUID.randomUUID()` 產生 event id

---

## 總結

Command Methods 的設計原則：
1. **ignore vs require**：區分「不需要執行」和「前置條件錯誤」
2. **四步驟模式**：Preconditions → Early Exit → Action → Postconditions
3. **使用 apply()**：提供 null 安全檢查

遵循這些原則可以寫出安全、清晰的 Command Methods。