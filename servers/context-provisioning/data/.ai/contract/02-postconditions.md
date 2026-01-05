# Postconditions 設計指南

本文件說明如何使用 ensure() 設計後置條件，驗證方法執行後的狀態。

---

## ensure() 基本使用

### 🎯 設計意圖 (Why)

**問題**：如何驗證方法執行後，Aggregate 的狀態符合預期？

**答案**：使用 `ensure()` 在方法結尾驗證後置條件，確保：
1. **狀態正確更新**：所有欄位都符合預期值
2. **事件正確產生**：Domain Event 包含正確的資料
3. **版本正確遞增**：Aggregate 版本號增加
4. **業務規則滿足**：所有業務約束都被滿足

### 📋 實作規範 (How)

```java
ensure("Description of expected state", () -> boolean條件);
```

**後置條件檢查順序**：
1. 檢查狀態欄位是否正確更新
2. 檢查 Domain Event 是否正確產生
3. 檢查版本號是否遞增（Event Sourcing）
4. 檢查業務規則是否滿足

### ✅ 正確範例

```java
public void rename(String newName) {
    requireNotNull("New name", newName);
    require("New name not empty", () -> !newName.trim().isEmpty());

    // 儲存舊狀態用於比較
    String oldName = this.name;
    long oldVersion = getVersion();
    int oldEventCount = getDomainEvents().size();

    // Early Exit
    if (ignore("Name unchanged", () -> name.equals(newName.trim()))) {
        return;
    }

    // 執行業務邏輯
    apply(new TagEvents.TagRenamed(
        tagId,
        newName.trim(),
        UUID.randomUUID(),
        DateProvider.now()
    ));

    // ✅ Postcondition 1: 狀態欄位檢查
    ensure(format("Tag name changed from '%s' to '%s'", oldName, newName.trim()),
        () -> getName().equals(newName.trim()));
    ensure("Tag name is different from old name",
        () -> !getName().equals(oldName));

    // ✅ Postcondition 2: 事件檢查
    ensure("Exactly one new event was generated",
        () -> getDomainEvents().size() == oldEventCount + 1);
    ensure("The last event is TagRenamed",
        () -> getLastDomainEvent() instanceof TagEvents.TagRenamed);
    ensure("TagRenamed event contains correct data", () -> {
        var event = (TagEvents.TagRenamed) getLastDomainEvent();
        return event.tagId().equals(tagId) &&
               event.newName().equals(newName.trim());
    });

    // ✅ Postcondition 3: 版本檢查
    ensure("Version was incremented", () -> getVersion() == oldVersion + 1);
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：沒有後置條件
public void rename(String newName) {
    requireNotNull("New name", newName);
    apply(new TagRenamed(...));
    // 缺少 ensure，無法確認狀態是否正確更新
}

// ❌ 錯誤：後置條件太弱
public void rename(String newName) {
    requireNotNull("New name", newName);
    apply(new TagRenamed(...));
    ensure("Name not null", () -> name != null);  // 太弱，應該檢查值是否正確
}

// ✅ 正確：詳細的後置條件
public void rename(String newName) {
    requireNotNull("New name", newName);
    apply(new TagRenamed(...));
    ensure("Name is updated to new value", () -> name.equals(newName.trim()));
    ensure("Event generated correctly", () ->
        getLastDomainEvent() instanceof TagRenamed);
}

// ❌ 錯誤：在前置條件中使用 ensure
public void rename(String newName) {
    ensure("Name not null", () -> newName != null);  // ❌ 應該用 require
    apply(new TagRenamed(...));
}

// ✅ 正確：前置條件用 require，後置條件用 ensure
public void rename(String newName) {
    requireNotNull("New name", newName);  // ✅ 前置條件
    apply(new TagRenamed(...));
    ensure("Name is updated", () -> name.equals(newName));  // ✅ 後置條件
}
```

**症狀**：狀態更新失敗但沒被發現、事件資料錯誤、版本號異常。

### 🔍 檢查清單

- [ ] 檢查所有應該改變的欄位
- [ ] 檢查 Domain Event 產生數量
- [ ] 檢查 Domain Event 類型正確
- [ ] 檢查 Domain Event 資料正確
- [ ] 檢查版本號遞增（Event Sourcing）
- [ ] 錯誤訊息清楚描述預期狀態
- [ ] 使用 Lambda 表達式避免提前求值

---

## 使用 Objects.equals() 比較物件

### 🎯 設計意圖 (Why)

**問題**：如何安全地比較兩個物件是否相等（處理 null 情況）？

**答案**：使用 `Objects.equals()` 進行 null-safe 的物件比較。

### 📋 實作規範 (How)

```java
Objects.equals(object1, object2)  // null-safe 比較
```

**何時使用**：
- 比較兩個可能為 null 的物件
- 比較 Value Object 是否相等
- 驗證狀態變更

### ✅ 正確範例

```java
public void changeEmail(String newEmail) {
    requireNotNull("New email", newEmail);
    require("Email format is valid", () -> isValidEmail(newEmail));

    // 儲存舊狀態
    String oldEmail = this.email;

    // 執行業務邏輯
    apply(new EmailChanged(userId, newEmail, UUID.randomUUID(), DateProvider.now()));

    // ✅ 使用 Objects.equals 比較
    ensure("Email changed", () -> !Objects.equals(email, oldEmail));
    ensure("Email is new value", () -> Objects.equals(email, newEmail));
}

// ✅ 比較 Value Object
public void assignProject(ProjectId projectId) {
    requireNotNull("Project id", projectId);

    apply(new ProjectAssigned(userId, projectId, UUID.randomUUID(), DateProvider.now()));

    // ✅ Value Object 比較
    ensure("Project assigned", () ->
        Objects.equals(currentProjectId, projectId));
}

// ✅ 處理 null 的情況
public void setDeadline(LocalDate deadline) {
    // deadline 可以是 null（移除截止日期）

    LocalDate oldDeadline = this.deadline;

    apply(new DeadlineSet(taskId, deadline, UUID.randomUUID(), DateProvider.now()));

    // ✅ 正確處理 null
    ensure("Deadline changed", () -> !Objects.equals(deadline, oldDeadline));
    ensure("Deadline is new value", () -> Objects.equals(this.deadline, deadline));
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：直接使用 equals()，可能 NullPointerException
public void setDeadline(LocalDate deadline) {
    apply(new DeadlineSet(...));
    ensure("Deadline is new value", () -> this.deadline.equals(deadline));  // ❌ 如果 deadline 是 null 會 NPE
}

// ✅ 正確：使用 Objects.equals()
public void setDeadline(LocalDate deadline) {
    apply(new DeadlineSet(...));
    ensure("Deadline is new value", () -> Objects.equals(this.deadline, deadline));  // ✅ null-safe
}

// ❌ 錯誤：手動檢查 null
public void setDeadline(LocalDate deadline) {
    apply(new DeadlineSet(...));
    ensure("Deadline is new value", () ->
        (this.deadline == null && deadline == null) ||
        (this.deadline != null && this.deadline.equals(deadline))  // ❌ 冗長且容易出錯
    );
}

// ✅ 正確：簡潔的 Objects.equals()
public void setDeadline(LocalDate deadline) {
    apply(new DeadlineSet(...));
    ensure("Deadline is new value", () -> Objects.equals(this.deadline, deadline));  // ✅
}
```

**症狀**：NullPointerException、錯誤的相等性比較、程式碼冗長難讀。

### 🔍 檢查清單

- [ ] 比較可能為 null 的欄位時使用 Objects.equals()
- [ ] 比較 Value Object 時使用 Objects.equals()
- [ ] 避免手動檢查 null 的冗長程式碼
- [ ] 避免直接呼叫可能為 null 的物件的 equals() 方法

---

## Lambda 表達式重構

### 🎯 設計意圖 (Why)

**問題**：複雜的後置條件檢查如何保持可讀性？

**答案**：將複雜的檢查邏輯提取到私有方法或使用多個 ensure() 分別檢查，保持每個 ensure() 的簡潔性。

### 📋 實作規範 (How)

**原則**：
1. 每個 ensure() 只檢查一個概念
2. 複雜邏輯提取到私有方法
3. 使用有意義的方法名稱
4. 保持 Lambda 表達式簡短

### ✅ 正確範例

```java
// ✅ 方法 1：提取到私有方法
public void createProject(ProjectId projectId, String name) {
    requireNotNull("Project id", projectId);
    requireNotNull("Project name", name);

    apply(new ProjectCreated(planId, projectId, name, UUID.randomUUID(), DateProvider.now()));

    // ✅ 使用私有方法，語意清楚
    ensure("Project created successfully", () -> isProjectCreatedSuccessfully(projectId, name));
}

private boolean isProjectCreatedSuccessfully(ProjectId projectId, String name) {
    return hasProject(projectId) &&
           getProject(projectId).getName().equals(name) &&
           getProject(projectId).getTasks().isEmpty();
}

// ✅ 方法 2：多個 ensure 分別檢查
public void createProject(ProjectId projectId, String name) {
    requireNotNull("Project id", projectId);
    requireNotNull("Project name", name);

    apply(new ProjectCreated(planId, projectId, name, UUID.randomUUID(), DateProvider.now()));

    // ✅ 分別檢查，每個概念清楚
    ensure("Project exists", () -> hasProject(projectId));
    ensure("Project name is correct", () -> getProject(projectId).getName().equals(name));
    ensure("Project has no tasks", () -> getProject(projectId).getTasks().isEmpty());
}

// ✅ 方法 3：結合私有方法和多個 ensure
public void assignTag(ProjectId projectId, TaskId taskId, TagId tagId) {
    requireNotNull("Project id", projectId);
    requireNotNull("Task id", taskId);
    requireNotNull("Tag id", tagId);
    require("Valid assignment context", () -> isValidAssignmentContext(projectId, taskId, tagId));

    int oldTagCount = getTask(projectId, taskId).getTags().size();

    apply(new TagAssigned(planId, projectId, taskId, tagId, UUID.randomUUID(), DateProvider.now()));

    // ✅ 混合使用
    ensure("Tag is assigned to task", () -> getTask(projectId, taskId).hasTag(tagId));
    ensure("Task tag count increased by 1", () ->
        getTask(projectId, taskId).getTags().size() == oldTagCount + 1);
    ensure("TagAssigned event is correct", () -> isTagAssignedEventCorrect(tagId));
}

private boolean isValidAssignmentContext(ProjectId projectId, TaskId taskId, TagId tagId) {
    return !isDeleted() &&
           hasProject(projectId) &&
           getProject(projectId).hasTask(taskId) &&
           !getTask(projectId, taskId).hasTag(tagId);
}

private boolean isTagAssignedEventCorrect(TagId tagId) {
    if (!(getLastDomainEvent() instanceof PlanEvents.TagAssigned event)) {
        return false;
    }
    return event.planId().equals(planId) &&
           event.tagId().equals(tagId);
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：Lambda 表達式過於複雜
public void assignTag(ProjectId projectId, TaskId taskId, TagId tagId) {
    apply(new TagAssigned(...));

    // ❌ 太複雜，難以閱讀和維護
    ensure("Tag assignment is correct", () ->
        getProject(projectId) != null &&
        getProject(projectId).getTask(taskId) != null &&
        getProject(projectId).getTask(taskId).hasTag(tagId) &&
        getTask(projectId, taskId).getTags().size() > 0 &&
        getLastDomainEvent() instanceof PlanEvents.TagAssigned &&
        ((PlanEvents.TagAssigned) getLastDomainEvent()).tagId().equals(tagId) &&
        ((PlanEvents.TagAssigned) getLastDomainEvent()).projectId().equals(projectId) &&
        getVersion() > 0
    );
}

// ✅ 正確：拆分成多個 ensure
public void assignTag(ProjectId projectId, TaskId taskId, TagId tagId) {
    int oldTagCount = getTask(projectId, taskId).getTags().size();
    long oldVersion = getVersion();

    apply(new TagAssigned(...));

    // ✅ 每個 ensure 檢查一個概念
    ensure("Tag is assigned to task", () -> getTask(projectId, taskId).hasTag(tagId));
    ensure("Task tag count increased", () ->
        getTask(projectId, taskId).getTags().size() == oldTagCount + 1);
    ensure("Event is TagAssigned", () ->
        getLastDomainEvent() instanceof PlanEvents.TagAssigned);
    ensure("Event data is correct", () -> isEventDataCorrect(projectId, taskId, tagId));
    ensure("Version incremented", () -> getVersion() == oldVersion + 1);
}

private boolean isEventDataCorrect(ProjectId projectId, TaskId taskId, TagId tagId) {
    var event = (PlanEvents.TagAssigned) getLastDomainEvent();
    return event.planId().equals(planId) &&
           event.projectId().equals(projectId) &&
           event.taskId().equals(taskId) &&
           event.tagId().equals(tagId);
}

// ❌ 錯誤：沒有意義的私有方法名稱
private boolean check1() {  // ❌ 名稱不清楚
    return getProject(projectId).hasTask(taskId);
}

// ✅ 正確：有意義的方法名稱
private boolean taskExistsInProject(ProjectId projectId, TaskId taskId) {  // ✅ 清楚表達意圖
    return hasProject(projectId) &&
           getProject(projectId).hasTask(taskId);
}
```

**症狀**：Lambda 表達式過長難讀、邏輯重複、難以維護、測試困難。

### 🔍 檢查清單

- [ ] 每個 ensure() 只檢查一個概念
- [ ] Lambda 表達式少於 3 行
- [ ] 複雜邏輯提取到私有方法
- [ ] 私有方法名稱清楚表達意圖
- [ ] 避免在 ensure() 中有複雜的型別轉換
- [ ] 避免在 ensure() 中有多層巢狀的條件判斷

---

## 建構子的 Postconditions

### 🎯 設計意圖 (Why)

**問題**：建構子執行後如何確保物件狀態正確初始化？

**答案**：在建構子結尾使用 ensure() 驗證所有欄位、事件和版本。

### ✅ 正確範例

```java
public Tag(TagId tagId, PlanId planId, String name, String color) {
    super();

    // Preconditions
    requireNotNull("Tag id", tagId);
    requireNotNull("Plan id", planId);
    requireNotNull("Tag name", name);
    requireNotNull("Tag color", color);
    require("Tag name is not empty", () -> !name.trim().isEmpty());
    require("Tag name length is valid", () ->
        name.trim().length() >= 1 && name.trim().length() <= 100);
    require("Tag color is valid HEX", () -> isValidHexColor(color));

    // 執行業務邏輯
    apply(new TagEvents.TagCreated(
        tagId,
        planId,
        name.trim(),
        color.toUpperCase(),
        UUID.randomUUID(),
        DateProvider.now()
    ));

    // ✅ Postconditions: 驗證所有欄位
    ensure(format("Tag id is '%s'", tagId), () -> getId().equals(tagId));
    ensure(format("Tag belongs to plan '%s'", planId), () -> getPlanId().equals(planId));
    ensure(format("Tag name is '%s'", name.trim()), () -> getName().equals(name.trim()));
    ensure(format("Tag color is '%s'", color.toUpperCase()),
        () -> getColor().equals(color.toUpperCase()));
    ensure("Tag is not deleted", () -> !isDeleted());

    // ✅ Postconditions: 驗證事件
    ensure("A TagCreated event is generated", () -> getDomainEvents().size() == 1);
    ensure("The generated event is TagCreated",
        () -> getLastDomainEvent() instanceof TagEvents.TagCreated);
    ensure("TagCreated event contains correct data", () -> {
        var event = (TagEvents.TagCreated) getLastDomainEvent();
        return event.tagId().equals(tagId) &&
               event.planId().equals(planId) &&
               event.name().equals(name.trim()) &&
               event.color().equals(color.toUpperCase());
    });

    // ✅ Postconditions: 驗證版本（Event Sourcing）
    ensure("Tag version is 0 after creation", () -> getVersion() == 0);
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：建構子沒有後置條件
public Tag(TagId tagId, PlanId planId, String name, String color) {
    requireNotNull("Tag id", tagId);
    apply(new TagEvents.TagCreated(...));
    // ❌ 缺少 ensure，無法確認初始化是否正確
}

// ❌ 錯誤：只檢查部分欄位
public Tag(TagId tagId, PlanId planId, String name, String color) {
    requireNotNull("Tag id", tagId);
    apply(new TagEvents.TagCreated(...));
    ensure("Tag id is correct", () -> getId().equals(tagId));
    // ❌ 沒有檢查其他欄位、事件、版本
}

// ✅ 正確：完整的後置條件
public Tag(TagId tagId, PlanId planId, String name, String color) {
    requireNotNull("Tag id", tagId);
    requireNotNull("Plan id", planId);
    requireNotNull("Tag name", name);
    requireNotNull("Tag color", color);

    apply(new TagEvents.TagCreated(...));

    // ✅ 檢查所有欄位
    ensure("Tag id is correct", () -> getId().equals(tagId));
    ensure("Plan id is correct", () -> getPlanId().equals(planId));
    ensure("Name is correct", () -> getName().equals(name.trim()));
    ensure("Color is correct", () -> getColor().equals(color.toUpperCase()));
    ensure("Not deleted", () -> !isDeleted());

    // ✅ 檢查事件
    ensure("Event generated", () -> getDomainEvents().size() == 1);
    ensure("Event is TagCreated", () ->
        getLastDomainEvent() instanceof TagEvents.TagCreated);

    // ✅ 檢查版本
    ensure("Version is 0", () -> getVersion() == 0);
}
```

**症狀**：物件初始化不完整、欄位值錯誤、事件缺失、版本號異常。

### 🔍 檢查清單

- [ ] 檢查所有必要欄位是否正確設定
- [ ] 檢查事件數量（應該是 1）
- [ ] 檢查事件類型是 ConstructionEvent
- [ ] 檢查事件資料正確
- [ ] 檢查版本號為 0（Event Sourcing）
- [ ] 檢查 isDeleted() 為 false
- [ ] 檢查集合欄位已初始化（非 null）

---

## 總結

Postconditions 的設計要點：
1. **ensure() 檢查狀態**：驗證欄位、事件、版本
2. **Objects.equals() 比較**：null-safe 的物件比較
3. **Lambda 重構**：保持簡潔，提取複雜邏輯
4. **建構子驗證**：完整檢查初始化狀態