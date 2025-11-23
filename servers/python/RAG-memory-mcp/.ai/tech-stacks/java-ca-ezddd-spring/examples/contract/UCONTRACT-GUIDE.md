# uContract 進階功能指南

## 概述

uContract 是專為 Domain-Driven Design (DDD) 中的事件溯源聚合根 (Event-Sourced Aggregate Roots) 設計的 Design By Contract (DBC) 工具。除了基本的 require/ensure 功能外，uContract 提供了許多進階功能，能幫助撰寫更精確、更易維護的契約。

## 核心進階功能

### 1. old() - 捕獲舊狀態

`old()` 用於在方法執行前捕獲物件狀態的深層複製，讓你能在 postcondition 中比較狀態變化。

```java
public void changeEmail(String newEmail) {
    requireNotNull("New email", newEmail);
    require("Email format is valid", () -> isValidEmail(newEmail));
    
    // 捕獲修改前的狀態
    var oldState = old(() -> state);
    var oldEmail = old(() -> state.getEmail());
    var oldVersion = old(() -> getVersion());
    
    // 執行業務邏輯
    apply(new EmailChanged(userId, newEmail, UUID.randomUUID(), DateProvider.now()));
    
    // 驗證只有 email 改變了
    ensure("Email changed", () -> !state.getEmail().equals(oldEmail));
    ensure("Version incremented", () -> getVersion() == oldVersion + 1);
    
    // 確保只有允許的欄位被修改
    ensureAssignable(state, oldState, ".*email");
}
```

**重要概念**：
- `old()` 會建立深層複製，確保原始狀態不被修改
- 應該在任何狀態修改之前呼叫
- 可以捕獲整個物件或特定屬性

### 2. ensureAssignable() - 驗證狀態變更

`ensureAssignable()` 確保只有指定的欄位可以被修改，其他欄位必須保持不變。

```java
public void updateProfile(String nickname, String bio) {
    var oldState = old(() -> state);
    
    // 執行業務邏輯
    if (nickname != null) {
        apply(new NicknameChanged(userId, nickname, UUID.randomUUID(), DateProvider.now()));
    }
    if (bio != null) {
        apply(new BioUpdated(userId, bio, UUID.randomUUID(), DateProvider.now()));
    }
    
    // 確保只有 nickname 和 bio 相關的欄位可以改變
    ensureAssignable(state, oldState, ".*nickname", ".*bio", "version", "lastModified");
}
```

**使用模式**：
- 第一個參數：新狀態
- 第二個參數：舊狀態（通常用 `old()` 捕獲）
- 後續參數：允許改變的欄位名稱模式（支援正則表達式）

### 3. ensureResult() - 驗證返回值

`ensureResult()` 用於驗證方法的返回值符合特定條件。

```java
public TaskDto findTaskById(String taskId) {
    TaskDto task = taskRepository.findById(taskId);
    
    // 驗證返回的任務符合條件
    return ensureResult("Task must be valid and not deleted", task, t -> 
        t != null && 
        !t.isDeleted() && 
        t.getTaskId().equals(taskId)
    );
}

public List<ProjectDto> getActiveProjects() {
    List<ProjectDto> projects = projectRepository.findActive();
    
    // 驗證所有返回的專案都是活躍的
    return ensureResult("All projects must be active", projects, list ->
        list.stream().allMatch(p -> p.isActive() && !p.isArchived())
    );
}
```

### 4. ensureImmutableCollection() - 確保集合不可變

`ensureImmutableCollection()` 確保返回的集合是不可變的，防止外部修改。

```java
public List<Tag> getTags() {
    // 確保返回的集合不能被外部修改
    return ensureImmutableCollection(Collections.unmodifiableList(new ArrayList<>(tags)));
}

public Set<String> getPermissions() {
    // 對於 Set 也同樣適用
    return ensureImmutableCollection(Collections.unmodifiableSet(new HashSet<>(permissions)));
}

public Map<String, Task> getTaskMap() {
    // Map 也可以使用
    return ensureImmutableCollection(Collections.unmodifiableMap(new HashMap<>(taskMap)));
}
```

### 5. ignore() - 防止不必要的操作 (uContract 2.0.0+)

`ignore()` 用於提前退出方法，避免產生不必要的領域事件。

**注意**: 在 uContract 2.0.0 版本中，`reject()` 方法已改名為 `ignore()`。

```java
public void rename(String newName) {
    requireNotNull("New name", newName);
    require("New name is not empty", () -> !newName.trim().isEmpty());
    
    // 如果名稱沒有改變，提前返回，避免產生事件
    if (ignore("Name unchanged", () -> name.equals(newName.trim()))) {
        return;
    }
    
    // 只有在名稱真的改變時才執行
    apply(new TaskRenamed(taskId, newName.trim(), UUID.randomUUID(), DateProvider.now()));
}

public void assignTag(TagId tagId) {
    requireNotNull("Tag id", tagId);
    
    // 如果標籤已經存在，避免重複指派
    if (ignore("Tag already assigned", () -> tags.contains(tagId))) {
        return;
    }
    
    apply(new TagAssigned(taskId, tagId, UUID.randomUUID(), DateProvider.now()));
}
```

### 6. check() - 執行中斷言

`check()` 用於在方法執行過程中驗證條件，類似於 assert 但提供更好的錯誤訊息。

```java
public void processComplexOperation(List<Item> items) {
    requireNotNull("Items", items);
    require("Items not empty", () -> !items.isEmpty());
    
    // 第一階段處理
    List<Item> validItems = items.stream()
        .filter(Item::isValid)
        .collect(Collectors.toList());
    
    // 執行中檢查
    check("At least one valid item", () -> !validItems.isEmpty());
    
    // 第二階段處理
    for (Item item : validItems) {
        // 每個項目的處理前檢查
        check("Item price is positive", () -> item.getPrice() > 0);
        check("Item quantity is valid", () -> item.getQuantity() > 0 && item.getQuantity() <= 100);
        
        processItem(item);
    }
    
    // 最終狀態檢查
    check("Operation completed successfully", () -> getProcessedCount() == validItems.size());
}
```

## 進階模式組合

### 完整的聚合根方法範例

```java
public class Order extends EsAggregateRoot<OrderId, OrderEvents> {
    
    public void updateShippingAddress(Address newAddress) {
        // Preconditions
        requireNotNull("New address", newAddress);
        require("Order not shipped", () -> status != OrderStatus.SHIPPED);
        require("Order not cancelled", () -> status != OrderStatus.CANCELLED);
        
        // 捕獲舊狀態
        var oldState = old(() -> state);
        var oldAddress = old(() -> shippingAddress);
        var oldVersion = old(() -> getVersion());
        
        // 提前退出檢查
        if (ignore("Address unchanged", () -> shippingAddress.equals(newAddress))) {
            return;
        }
        
        // 執行業務邏輯
        apply(new ShippingAddressUpdated(
            orderId, 
            newAddress, 
            UUID.randomUUID(), 
            DateProvider.now()
        ));
        
        // 執行中檢查
        check("Address is updated", () -> !shippingAddress.equals(oldAddress));
        
        // Postconditions
        ensure("New address is set", () -> shippingAddress.equals(newAddress));
        ensure("Version incremented", () -> getVersion() == oldVersion + 1);
        
        // 確保只有允許的欄位改變
        ensureAssignable(state, oldState, "shippingAddress", "version", "lastModifiedAt");
    }
    
    public OrderSummary getSummary() {
        // 建立摘要
        List<OrderItem> items = getOrderItems();
        Money total = calculateTotal();
        
        OrderSummary summary = new OrderSummary(
            orderId,
            customerName,
            items,
            total,
            status
        );
        
        // 驗證返回值
        return ensureResult("Summary is valid", summary, s -> 
            s.getOrderId().equals(orderId) &&
            s.getTotal().equals(total) &&
            s.getItems().size() == items.size()
        );
    }
    
    public List<OrderItem> getOrderItems() {
        // 返回不可變集合
        return ensureImmutableCollection(
            Collections.unmodifiableList(new ArrayList<>(orderItems))
        );
    }
}
```

## 最佳實踐

### 1. 使用 old() 的時機
- 在任何狀態修改之前捕獲
- 只捕獲需要比較的狀態
- 考慮效能影響（深層複製的成本）

### 2. ensureAssignable() 的模式
- 使用正則表達式匹配欄位名稱
- 記得包含版本和時間戳記欄位
- 對於嵌套物件，使用適當的模式（如 "address\\..*"）

### 3. ignore() vs require() (uContract 2.0.0+)
- `require()`: 驗證前置條件，失敗時拋出異常
- `ignore()`: 檢查是否需要執行，返回 true 時提前退出
- 使用 `ignore()` 避免產生不必要的事件
- **注意**: `reject()` 在 uContract 2.0.0 中已改名為 `ignore()`

### 4. 組合使用
```java
public void complexOperation(Input input) {
    // 1. 前置條件
    requireNotNull("Input", input);
    require("Valid input", () -> input.isValid());
    
    // 2. 捕獲舊狀態
    var oldState = old(() -> state);
    
    // 3. 提前退出檢查
    if (ignore("No change needed", () -> !needsUpdate(input))) {
        return;
    }
    
    // 4. 執行業務邏輯
    performUpdate(input);
    
    // 5. 執行中檢查
    check("Update successful", () -> isUpdateSuccessful());
    
    // 6. 後置條件
    ensure("State updated", () -> stateUpdated());
    ensureAssignable(state, oldState, "allowedField1", "allowedField2");
}
```

## 錯誤處理範例

```java
public void riskyOperation() {
    var oldState = old(() -> state);
    
    try {
        // 危險操作
        performRiskyUpdate();
        
        // 成功時的檢查
        check("Operation succeeded", () -> isSuccessful());
        ensure("State is consistent", () -> isConsistent());
        
    } catch (Exception e) {
        // 失敗時確保狀態回滾
        ensure("State rolled back on failure", () -> state.equals(oldState));
        throw new OperationFailedException("Risky operation failed", e);
    }
}
```

## 總結

uContract 的進階功能提供了強大的工具來：
1. **old()** - 追蹤狀態變化
2. **ensureAssignable()** - 控制可變更的欄位
3. **ensureResult()** - 驗證返回值
4. **ensureImmutableCollection()** - 防止集合被修改
5. **ignore()** - 避免不必要的操作 (uContract 2.0.0+)
6. **check()** - 執行中的斷言

這些功能結合使用，能幫助你寫出更安全、更可維護的領域模型。