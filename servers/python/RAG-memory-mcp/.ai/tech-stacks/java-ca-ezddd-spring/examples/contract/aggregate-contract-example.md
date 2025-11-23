# Aggregate Contract 範例

## 概述

在 Event Sourcing 和 DDD 架構中，Aggregate 的 Contract 設計特別重要。本文提供具體的範例，展示如何為 Aggregate 的各種方法設計良好的 Contract。

## 建構子 Contract

```java
public Tag(TagId tagId, PlanId planId, String name, String color) {
    super();
    
    // Preconditions - 驗證所有輸入
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
    
    // Postconditions - 驗證物件狀態
    ensure(format("Tag id is '%s'", tagId), () -> getId().equals(tagId));
    ensure(format("Tag belongs to plan '%s'", planId), () -> getPlanId().equals(planId));
    ensure(format("Tag name is '%s'", name.trim()), () -> getName().equals(name.trim()));
    ensure(format("Tag color is '%s'", color.toUpperCase()), 
        () -> getColor().equals(color.toUpperCase()));
    ensure("Tag is not deleted", () -> !isDeleted());
    
    // 驗證事件產生
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
    
    // 驗證版本
    ensure("Tag version is 0 after creation", () -> getVersion() == 0);
}
```

## 命令方法 Contract

### 範例 1：Rename 方法

```java
public void rename(String newName) {
    // Preconditions
    requireNotNull("New tag name", newName);
    require("New tag name is not empty", () -> !newName.trim().isEmpty());
    require("New tag name length is valid", () -> 
        newName.trim().length() >= 1 && newName.trim().length() <= 100);
    require("Tag is not deleted", () -> !isDeleted());
    
    // 儲存舊狀態（只儲存會改變的）
    String oldName = this.name;
    long oldVersion = getVersion();
    int oldEventCount = getDomainEvents().size();
    
    // 最佳化：如果名稱沒變就跳過
    if (ignore("New name equals current name", () -> name.equals(newName.trim()))) {
        return;
    }
    
    // 執行業務邏輯
    apply(new TagEvents.TagRenamed(
        tagId,
        newName.trim(),
        UUID.randomUUID(),
        DateProvider.now()
    ));
    
    // Postconditions - 只檢查真正改變的東西
    ensure(format("Tag name changed from '%s' to '%s'", oldName, newName.trim()), 
        () -> getName().equals(newName.trim()));
    ensure("Tag name is different from old name", 
        () -> !getName().equals(oldName));
    
    // 驗證事件
    ensure("Exactly one new event was generated", 
        () -> getDomainEvents().size() == oldEventCount + 1);
    ensure("The last event is TagRenamed", 
        () -> getLastDomainEvent() instanceof TagEvents.TagRenamed);
    ensure("TagRenamed event contains correct data", () -> {
        var event = (TagEvents.TagRenamed) getLastDomainEvent();
        return event.tagId().equals(tagId) &&
               event.newName().equals(newName.trim());
    });
    
    // 驗證版本
    ensure("Version was incremented", () -> getVersion() == oldVersion + 1);
}
```

### 範例 2：Delete 方法

```java
public void delete() {
    // Preconditions
    require("Tag is not already deleted", () -> !isDeleted());
    
    // 儲存舊狀態
    long oldVersion = getVersion();
    int oldEventCount = getDomainEvents().size();
    
    // 執行業務邏輯
    apply(new TagEvents.TagDeleted(
        tagId,
        UUID.randomUUID(),
        DateProvider.now()
    ));
    
    // Postconditions
    ensure("Tag is marked as deleted", () -> isDeleted());
    
    // 驗證事件
    ensure("Exactly one new event was generated", 
        () -> getDomainEvents().size() == oldEventCount + 1);
    ensure("The last event is TagDeleted", 
        () -> getLastDomainEvent() instanceof TagEvents.TagDeleted);
    
    // 驗證版本
    ensure("Version was incremented", () -> getVersion() == oldVersion + 1);
}
```

## 複雜業務邏輯的 Contract

### 範例：分配標籤給任務

```java
public void assignTag(ProjectId projectId, TaskId taskId, TagId tagId) {
    // Preconditions
    requireNotNull("Project id", projectId);
    requireNotNull("Task id", taskId);
    requireNotNull("Tag id", tagId);
    require("Plan is not deleted", () -> !isDeleted());
    require("Project exists", () -> hasProject(projectId));
    require("Task exists in project", () -> getProject(projectId).hasTask(taskId));
    require("Tag not already assigned", () -> 
        !getProject(projectId).getTask(taskId).hasTag(tagId));
    
    // 儲存舊狀態
    int oldTagCount = getProject(projectId).getTask(taskId).getTags().size();
    long oldVersion = getVersion();
    int oldEventCount = getDomainEvents().size();
    
    // 執行業務邏輯
    apply(new PlanEvents.TagAssigned(
        planId,
        projectId,
        taskId,
        tagId,
        UUID.randomUUID(),
        DateProvider.now()
    ));
    
    // Postconditions
    ensure("Tag is assigned to task", () -> 
        getProject(projectId).getTask(taskId).hasTag(tagId));
    ensure("Task tag count increased by 1", () -> 
        getProject(projectId).getTask(taskId).getTags().size() == oldTagCount + 1);
    
    // 驗證事件
    ensure("Exactly one new event was generated", 
        () -> getDomainEvents().size() == oldEventCount + 1);
    ensure("The last event is TagAssigned", 
        () -> getLastDomainEvent() instanceof PlanEvents.TagAssigned);
    ensure("TagAssigned event contains correct data", () -> {
        var event = (PlanEvents.TagAssigned) getLastDomainEvent();
        return event.planId().equals(planId) &&
               event.projectId().equals(projectId) &&
               event.taskId().equals(taskId) &&
               event.tagId().equals(tagId);
    });
    
    // 驗證版本
    ensure("Version was incremented", () -> getVersion() == oldVersion + 1);
}
```

## Invariant 設計

```java
@Override
public void ensureInvariant() {
    // 基本屬性不變條件
    invariantNotNull("Tag id", tagId);
    invariantNotNull("Plan id", planId);
    invariantNotNull("Tag name", name);
    invariantNotNull("Tag color", color);
    
    // 業務規則不變條件
    invariant("Tag name is not empty", () -> !name.trim().isEmpty());
    invariant("Tag name length is valid", () -> 
        name.length() >= 1 && name.length() <= 100);
    invariant("Tag color is valid HEX", () -> isValidHexColor(color));
    
    // 狀態一致性不變條件
    if (!isDeleted()) {
        invariant("Active tag has valid properties", () -> 
            tagId != null && planId != null && name != null && color != null);
    }
}
```

## Contract 設計要點

### 1. 前置條件的層次
1. **Null 檢查**：最基本的驗證
2. **格式驗證**：確保資料格式正確
3. **狀態檢查**：確保物件處於正確狀態
4. **業務規則**：確保業務邏輯的前提條件

### 2. 後置條件的重點
1. **狀態變更**：驗證預期的狀態改變
2. **事件產生**：確保正確的事件被產生
3. **版本更新**：Event Sourcing 中的版本管理
4. **副作用**：任何其他的副作用

### 3. 避免的陷阱
- 不要檢查不會改變的屬性
- 不要做 getter == field 的檢查
- 不要在 postcondition 重複 precondition
- 不要忘記檢查事件的產生

## 總結

良好的 Aggregate Contract 設計能：
- 防止無效狀態的產生
- 確保業務規則的執行
- 提供清晰的錯誤訊息
- 使測試更容易撰寫
- 提升程式碼的可維護性