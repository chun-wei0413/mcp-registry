# Result Validation 設計指南

本文件說明如何使用 ensureResult() 和 ensureImmutableCollection() 驗證返回值。

---

## ensureResult() 驗證返回值

### 🎯 設計意圖 (Why)

**問題**：如何確保方法的返回值符合特定條件？

**答案**：使用 `ensureResult()` 在返回值之前驗證其正確性：
1. **返回值驗證**：確保返回的物件符合業務規則
2. **null 檢查**：防止返回 null 或無效物件
3. **業務規則檢查**：驗證返回的物件狀態正確
4. **集合驗證**：確保返回的集合內容符合預期

### 📋 實作規範 (How)

```java
return ensureResult("Description", returnValue, value -> boolean條件);
```

**參數說明**：
- 第一個參數：錯誤訊息描述
- 第二個參數：要返回的值
- 第三個參數：驗證條件的 Lambda 表達式

### ✅ 正確範例

```java
// ✅ 範例 1：驗證單一物件
public TaskDto findTaskById(String taskId) {
    TaskDto task = taskRepository.findById(taskId);

    // ✅ 驗證返回的任務符合條件
    return ensureResult("Task must be valid and not deleted", task, t ->
        t != null &&
        !t.isDeleted() &&
        t.getTaskId().equals(taskId)
    );
}

// ✅ 範例 2：驗證集合內容
public List<ProjectDto> getActiveProjects() {
    List<ProjectDto> projects = projectRepository.findActive();

    // ✅ 驗證所有返回的專案都是活躍的
    return ensureResult("All projects must be active", projects, list ->
        list != null &&
        !list.isEmpty() &&
        list.stream().allMatch(p -> p.isActive() && !p.isArchived())
    );
}

// ✅ 範例 3：驗證計算結果
public Money calculateTotal() {
    Money total = orderItems.stream()
        .map(item -> item.getPrice().multiply(item.getQuantity()))
        .reduce(Money.ZERO, Money::add);

    // ✅ 驗證總金額為正數且不為 null
    return ensureResult("Total must be positive", total, t ->
        t != null &&
        t.isPositive()
    );
}

// ✅ 範例 4：驗證複雜業務規則
public OrderSummary getSummary() {
    List<OrderItem> items = getOrderItems();
    Money total = calculateTotal();

    OrderSummary summary = new OrderSummary(
        orderId,
        customerName,
        items,
        total,
        status
    );

    // ✅ 驗證摘要資料完整性
    return ensureResult("Summary is valid", summary, s ->
        s != null &&
        s.getOrderId().equals(orderId) &&
        s.getCustomerName() != null &&
        !s.getCustomerName().isEmpty() &&
        s.getTotal().equals(total) &&
        s.getItems().size() == items.size() &&
        s.getStatus() == status
    );
}

// ✅ 範例 5：驗證可選值
public Optional<Task> findTaskByName(String taskName) {
    Optional<Task> task = tasks.stream()
        .filter(t -> t.getName().equals(taskName))
        .findFirst();

    // ✅ 驗證 Optional 中的值（如果存在）符合條件
    return ensureResult("Task must not be deleted", task, opt ->
        opt.isEmpty() || !opt.get().isDeleted()
    );
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：不驗證返回值
public TaskDto findTaskById(String taskId) {
    TaskDto task = taskRepository.findById(taskId);
    return task;  // ❌ 沒有驗證，可能返回 null 或已刪除的任務
}

// ✅ 正確：使用 ensureResult 驗證
public TaskDto findTaskById(String taskId) {
    TaskDto task = taskRepository.findById(taskId);
    return ensureResult("Task must be valid", task, t ->
        t != null && !t.isDeleted());
}

// ❌ 錯誤：驗證條件太弱
public List<ProjectDto> getActiveProjects() {
    List<ProjectDto> projects = projectRepository.findActive();
    return ensureResult("Projects must not be null", projects, list -> list != null);  // ❌ 只檢查 null
}

// ✅ 正確：完整的驗證條件
public List<ProjectDto> getActiveProjects() {
    List<ProjectDto> projects = projectRepository.findActive();
    return ensureResult("All projects must be active", projects, list ->
        list != null &&
        list.stream().allMatch(p -> p.isActive() && !p.isArchived())
    );
}

// ❌ 錯誤：在驗證前已經返回
public Money calculateTotal() {
    Money total = calculateTotalAmount();
    if (total == null) {
        return Money.ZERO;  // ❌ 提前返回，跳過驗證
    }
    return ensureResult("Total is positive", total, t -> t.isPositive());
}

// ✅ 正確：所有返回路徑都經過驗證
public Money calculateTotal() {
    Money total = calculateTotalAmount();
    if (total == null) {
        total = Money.ZERO;
    }
    return ensureResult("Total is positive", total, t -> t.isPositive());
}

// ❌ 錯誤：使用 ensure 而非 ensureResult
public TaskDto findTaskById(String taskId) {
    TaskDto task = taskRepository.findById(taskId);
    ensure("Task is valid", () -> task != null);  // ❌ ensure 不會返回值
    return task;
}

// ✅ 正確：使用 ensureResult
public TaskDto findTaskById(String taskId) {
    TaskDto task = taskRepository.findById(taskId);
    return ensureResult("Task is valid", task, t -> t != null);  // ✅ ensureResult 返回值
}
```

**症狀**：返回無效物件、null pointer exception、業務規則被破壞、資料不一致。

### 🔍 檢查清單

- [ ] 所有返回值都經過 ensureResult 驗證
- [ ] 驗證條件檢查 null
- [ ] 驗證條件檢查業務規則
- [ ] 錯誤訊息清楚描述預期條件
- [ ] 所有返回路徑都經過驗證（包括提前返回）
- [ ] 集合驗證包含內容檢查（不只檢查 null）

---

## ensureImmutableCollection() 確保集合不可變

### 🎯 設計意圖 (Why)

**問題**：如何確保返回的集合不能被外部修改？

**答案**：使用 `ensureImmutableCollection()` 確保返回的集合是不可變的：
1. **封裝性**：防止外部直接修改內部狀態
2. **防禦性複製**：返回集合的不可變副本
3. **執行時檢查**：確保返回的集合真的不可變
4. **支援多種集合**：List, Set, Map 都支援

### 📋 實作規範 (How)

```java
return ensureImmutableCollection(Collections.unmodifiableXXX(collection));
```

**使用模式**：
1. 使用 `new ArrayList<>(原集合)` 建立防禦性複製
2. 使用 `Collections.unmodifiableList()` 包裝成不可變集合
3. 使用 `ensureImmutableCollection()` 驗證不可變性

### ✅ 正確範例

```java
// ✅ 範例 1：返回不可變 List
public List<Tag> getTags() {
    // ✅ 防禦性複製 + 不可變包裝 + 驗證
    return ensureImmutableCollection(
        Collections.unmodifiableList(new ArrayList<>(tags))
    );
}

// ✅ 範例 2：返回不可變 Set
public Set<String> getPermissions() {
    // ✅ 對於 Set 也同樣適用
    return ensureImmutableCollection(
        Collections.unmodifiableSet(new HashSet<>(permissions))
    );
}

// ✅ 範例 3：返回不可變 Map
public Map<String, Task> getTaskMap() {
    // ✅ Map 也可以使用
    return ensureImmutableCollection(
        Collections.unmodifiableMap(new HashMap<>(taskMap))
    );
}

// ✅ 範例 4：返回空集合
public List<Project> getProjects() {
    if (projects.isEmpty()) {
        // ✅ 返回不可變的空集合
        return ensureImmutableCollection(Collections.emptyList());
    }
    return ensureImmutableCollection(
        Collections.unmodifiableList(new ArrayList<>(projects))
    );
}

// ✅ 範例 5：返回過濾後的集合
public List<Task> getActiveTasks() {
    List<Task> activeTasks = tasks.stream()
        .filter(task -> !task.isCompleted())
        .collect(Collectors.toList());

    // ✅ 過濾後的結果也要確保不可變
    return ensureImmutableCollection(
        Collections.unmodifiableList(activeTasks)
    );
}

// ✅ 範例 6：使用 Java 9+ 的不可變集合工廠方法
public List<String> getTagNames() {
    // ✅ Java 9+ List.copyOf() 已經是不可變的
    return ensureImmutableCollection(
        List.copyOf(tags.stream().map(Tag::getName).toList())
    );
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：直接返回內部集合
public List<Tag> getTags() {
    return tags;  // ❌ 外部可以修改內部狀態！
}

// 外部程式碼
var tags = aggregate.getTags();
tags.clear();  // ❌ 破壞了 Aggregate 的封裝性！

// ✅ 正確：返回不可變集合
public List<Tag> getTags() {
    return ensureImmutableCollection(
        Collections.unmodifiableList(new ArrayList<>(tags))
    );
}

// ❌ 錯誤：只做防禦性複製，沒有不可變包裝
public List<Tag> getTags() {
    return new ArrayList<>(tags);  // ❌ 還是可以被修改
}

// ✅ 正確：防禦性複製 + 不可變包裝
public List<Tag> getTags() {
    return ensureImmutableCollection(
        Collections.unmodifiableList(new ArrayList<>(tags))
    );
}

// ❌ 錯誤：只做不可變包裝，沒有防禦性複製
public List<Tag> getTags() {
    return Collections.unmodifiableList(tags);  // ❌ 如果內部 tags 改變，外部看到的也會變
}

// ✅ 正確：先複製再包裝
public List<Tag> getTags() {
    return ensureImmutableCollection(
        Collections.unmodifiableList(new ArrayList<>(tags))
    );
}

// ❌ 錯誤：沒有使用 ensureImmutableCollection 驗證
public List<Tag> getTags() {
    return Collections.unmodifiableList(new ArrayList<>(tags));  // ❌ 沒有執行時驗證
}

// ✅ 正確：使用 ensureImmutableCollection 驗證
public List<Tag> getTags() {
    return ensureImmutableCollection(
        Collections.unmodifiableList(new ArrayList<>(tags))
    );
}

// ❌ 錯誤：返回 null
public List<Tag> getTags() {
    if (tags == null) {
        return null;  // ❌ 不應該返回 null
    }
    return ensureImmutableCollection(
        Collections.unmodifiableList(new ArrayList<>(tags))
    );
}

// ✅ 正確：返回空集合而非 null
public List<Tag> getTags() {
    if (tags == null || tags.isEmpty()) {
        return ensureImmutableCollection(Collections.emptyList());
    }
    return ensureImmutableCollection(
        Collections.unmodifiableList(new ArrayList<>(tags))
    );
}
```

**症狀**：內部狀態被外部修改、封裝性被破壞、並發問題、資料不一致。

### 🔍 檢查清單

- [ ] 所有返回集合的方法都使用 ensureImmutableCollection
- [ ] 先做防禦性複製（new ArrayList）
- [ ] 再做不可變包裝（Collections.unmodifiableList）
- [ ] 最後用 ensureImmutableCollection 驗證
- [ ] 不返回 null，返回空集合
- [ ] 考慮使用 Java 9+ 的 List.copyOf() 等方法

---

## ensureResult() + ensureImmutableCollection() 組合

### 🎯 設計意圖 (Why)

**問題**：返回集合時如何同時驗證內容和不可變性？

**答案**：組合使用兩個方法，先驗證內容，再確保不可變性。

### ✅ 正確範例

```java
// ✅ 先驗證內容，再確保不可變性
public List<Task> getActiveTasks() {
    List<Task> activeTasks = tasks.stream()
        .filter(task -> !task.isCompleted() && !task.isDeleted())
        .collect(Collectors.toList());

    // Step 1: 驗證內容
    ensureResult("All tasks must be active", activeTasks, list ->
        list != null &&
        list.stream().allMatch(t -> !t.isCompleted() && !t.isDeleted())
    );

    // Step 2: 確保不可變性並返回
    return ensureImmutableCollection(
        Collections.unmodifiableList(activeTasks)
    );
}

// ✅ 或者合併成一個返回語句
public List<Project> getActiveProjects() {
    List<Project> activeProjects = projects.values().stream()
        .filter(p -> p.isActive())
        .collect(Collectors.toList());

    // ✅ 先驗證再確保不可變
    return ensureImmutableCollection(
        ensureResult("All projects must be active",
            Collections.unmodifiableList(activeProjects),
            list -> list.stream().allMatch(Project::isActive)
        )
    );
}
```

### 🔍 檢查清單

- [ ] 使用 ensureResult 驗證集合內容
- [ ] 使用 ensureImmutableCollection 確保不可變性
- [ ] 兩者都有清楚的錯誤訊息
- [ ] 不返回 null，返回空集合

---

## 總結

Result Validation 的設計要點：
1. **ensureResult()**：驗證返回值符合業務規則
2. **ensureImmutableCollection()**：確保集合不可變
3. **防禦性複製**：先複製再包裝再驗證
4. **組合使用**：內容驗證 + 不可變性驗證