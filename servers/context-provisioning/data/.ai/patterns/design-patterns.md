# Aggregate 設計模式

本文件合併了最佳實踐和反模式，每個模式包含正確做法（✅）和錯誤做法（❌）。

---

## DateProvider 使用模式

### 🎯 設計意圖

確保時間的可測試性，避免使用系統時間 API。

### ✅ 正確做法

```java
// ✅ 使用 DateProvider.now()
apply(new PlanCreated(
    planId, name, userId,
    new HashMap<>(),
    UUID.randomUUID(),
    DateProvider.now()  // ✅ 可測試
));

// ✅ 測試中控制時間
@Test
void should_create_with_specific_time() {
    DateProvider.setDate("2024-01-01T10:00:00Z");
    var event = createEvent();
    assertThat(event.occurredOn()).isEqualTo("2024-01-01T10:00:00Z");
    DateProvider.resetDate();
}
```

### ❌ 常見錯誤

```java
// ❌ 直接使用時間 API
Instant.now()              // 不可測試
LocalDateTime.now()        // 不可測試
System.currentTimeMillis() // 不可測試
```

**症狀**：測試結果不穩定，無法重現特定時間點的行為。

### 🔍 檢查清單

- [ ] 所有 Domain Events 使用 DateProvider.now()
- [ ] 測試後呼叫 DateProvider.resetDate()
- [ ] 不使用 Instant.now() 或其他系統時間 API

---

## apply() vs addDomainEvent() 模式

### 🎯 設計意圖

`apply()` 提供 null 安全檢查，是發布事件的推薦方式。

### ✅ 正確做法

```java
apply(new PlanCreated(...));  // ✅ 有 null 檢查
```

### ❌ 常見錯誤

```java
addDomainEvent(new PlanCreated(...));  // ❌ 沒有 null 檢查
```

**症狀**：可能發生 NullPointerException。

### 🔍 檢查清單

- [ ] 永遠使用 apply() 發布事件
- [ ] 不使用 addDomainEvent()

---

## 小而聚焦的 Aggregate 模式

### 🎯 設計意圖

Aggregate 應該小而聚焦，避免過大導致效能和並發問題。

### ✅ 正確做法

```java
// ✅ 小而聚焦
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    private PlanId id;
    private String name;
    private List<Project> projects;  // 少量嵌套
}

public class Task extends AggregateRoot<TaskId> {
    private TaskId id;
    private ProjectId projectId;  // ✅ 使用 ID 引用
}
```

### ❌ 常見錯誤

```java
// ❌ Aggregate 過大
public class Company {
    private List<Employee> employees;      // 數千個
    private List<Department> departments;  // 數百個
    private List<Project> projects;
    private List<Customer> customers;
}
```

**症狀**：載入慢、並發衝突頻繁、不變式複雜。

### 🔍 檢查清單

- [ ] Aggregate 不超過 2-3 層嵌套
- [ ] 使用 ID 引用其他 Aggregate
- [ ] 一次事務只修改一個 Aggregate

---

## ignore vs require 模式

### 🎯 設計意圖

區分「避免不必要操作」和「前置條件驗證」。

### ✅ 正確做法

```java
public void rename(String newName) {
    // 1. require：前置條件
    requireNotNull("New name", newName);
    require("Name not empty", () -> !newName.isBlank());

    // 2. ignore：優化
    if (ignore("Name unchanged", () -> this.name.equals(newName))) {
        return;
    }

    apply(new Renamed(...));
}
```

### ❌ 常見錯誤

```java
// ❌ 混淆用途
public void deleteTask(TaskId taskId) {
    if (ignore("Task not found", () -> !hasTask(taskId))) {
        return;  // ❌ 應該拋異常，不是忽略
    }
}

// ✅ 正確
require("Task must exist", () -> hasTask(taskId));
```

**症狀**：業務異常被默默吞掉。

### 🔍 檢查清單

- [ ] require 用於前置條件（失敗拋異常）
- [ ] ignore 用於優化（返回 true 時 return）
- [ ] 先 require 再 ignore

---

## 測試規格尊重模式

### 🎯 設計意圖

測試失敗時不隨意修改測試規格，應先分析原因。

### ✅ 正確做法

```java
// 測試失敗時：
// 1. 停止並分析失敗原因
// 2. 與人類確認：規格錯誤？還是實現錯誤？
// 3. 得到明確指示後再修改
```

### ❌ 常見錯誤

```java
// ❌ 測試失敗就直接改測試
@Test
public void create_plan() {
    // 原本：預期某個業務規則
    // ❌ 因為失敗就降低期望值
    assertThat(plan.getName()).isNotNull();  // 移除了其他驗證
}
```

**症狀**：規格失真，業務規則被破壞。

### 🔍 檢查清單

- [ ] 測試失敗時暫停
- [ ] 分析失敗原因並記錄
- [ ] 尋求人類確認再修改

---

## 總結

設計模式的核心原則：
1. **DateProvider**：確保可測試性
2. **apply()**：確保 null 安全
3. **小 Aggregate**：避免過大
4. **ignore vs require**：區分用途
5. **尊重測試規格**：不隨意修改

參考：
- [aggregate/03-command-methods.md](../aggregate/03-command-methods.md)
- [contract/05-control-flow.md](../contract/05-control-flow.md)
