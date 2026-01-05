# Control Flow 設計指南

本文件說明如何使用 ignore() 和 check() 控制程式流程。

**重要**：在 uContract 2.0.0 中，`reject()` 已改名為 `ignore()`。

---

## ignore() 提前退出

### 🎯 設計意圖 (Why)

**問題**：如何避免產生不必要的 domain event？

**答案**：使用 `ignore()` 檢查是否需要執行操作，返回 true 時提前退出。

### 📋 實作規範 (How)

```java
if (ignore("Reason", () -> condition)) {
    return;  // 提前退出，不產生 event
}
```

### ✅ 正確範例

```java
// ✅ 正確：避免不必要的 domain event
public void rename(String newName) {
    requireNotNull("New name", newName);
    require("Name must not be empty", () -> !newName.isBlank());

    // 使用 ignore 避免產生不必要的 Renamed event
    if (ignore("New name is the same as current name",
                () -> this.name.equals(newName))) {
        return; // 不產生 event，直接返回
    }

    apply(new ProductRenamed(this.id, newName, ...));

    ensure("Name is updated", () -> this.name.equals(newName));
}

// ✅ 正確：避免重複指派
public void assignTag(TagId tagId) {
    requireNotNull("Tag id", tagId);

    // 如果標籤已經存在，避免重複指派
    if (ignore("Tag already assigned", () -> tags.contains(tagId))) {
        return;
    }

    apply(new TagAssigned(taskId, tagId, UUID.randomUUID(), DateProvider.now()));
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：混淆 ignore 和 require
public void deleteTask(TaskId taskId) {
    // ❌ 錯誤：task 不存在應該拋異常，不是「忽略操作」
    if (ignore("Task not found", () -> !hasTask(taskId))) {
        return; // 這會默默地什麼都不做！
    }

    // ✅ 正確：應該用 require
    require("Task must exist", () -> hasTask(taskId));
}
```

**症狀**：業務異常被默默吞掉，使用者不知道發生了什麼。

### 🔍 檢查清單

- [ ] 用於避免不必要操作（如值相同、已存在）
- [ ] 返回 true 時使用 return 提前退出
- [ ] 不用於錯誤處理（錯誤應該用 require）
- [ ] 訊息清楚描述「為什麼不需要執行」

---

## check() 執行中斷言

### 🎯 設計意圖 (Why)

**問題**：如何在方法執行過程中驗證條件？

**答案**：使用 `check()` 在執行過程中斷言條件，類似於 assert 但提供更好的錯誤訊息。

### 📋 實作規範 (How)

```java
check("Description", () -> condition);
```

### ✅ 正確範例

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
        check("Item quantity is valid", () ->
            item.getQuantity() > 0 && item.getQuantity() <= 100);

        processItem(item);
    }

    // 最終狀態檢查
    check("Operation completed successfully", () ->
        getProcessedCount() == validItems.size());
}
```

### 🔍 檢查清單

- [ ] 用於執行過程中的斷言
- [ ] 驗證中間狀態的正確性
- [ ] 不用於前置條件（應該用 require）
- [ ] 不用於後置條件（應該用 ensure）

---

## ignore vs require vs check 對比

### 🎯 使用時機

| 方法 | 用途 | 失敗行為 | 典型場景 |
|------|------|---------|---------|
| **require()** | 前置條件驗證 | 拋出異常 | 參數為 null、狀態無效 |
| **ignore()** | 避免不必要操作 | 提前 return | 值相同、已存在 |
| **check()** | 執行中斷言 | 拋出異常 | 中間狀態驗證 |
| **ensure()** | 後置條件驗證 | 拋出異常 | 狀態已更新、事件已產生 |

### ✅ 正確範例

```java
public void updateShippingAddress(Address newAddress) {
    // Step 1: Preconditions（require）
    requireNotNull("New address", newAddress);
    require("Order not shipped", () -> status != OrderStatus.SHIPPED);

    // Step 2: Early Exit（ignore）
    if (ignore("Address unchanged", () -> shippingAddress.equals(newAddress))) {
        return;
    }

    // Step 3: Action
    var oldAddress = old(() -> shippingAddress);
    this.shippingAddress = newAddress;

    // 執行中檢查（check）
    check("Address is updated", () -> !shippingAddress.equals(oldAddress));

    apply(new ShippingAddressUpdated(orderId, newAddress, ...));

    // Step 4: Postconditions（ensure）
    ensure("New address is set", () -> shippingAddress.equals(newAddress));
}
```

### 🔍 檢查清單

- [ ] require 用於方法開頭的前置條件
- [ ] ignore 用於提前退出避免不必要操作
- [ ] check 用於執行過程中的斷言
- [ ] ensure 用於方法結尾的後置條件

---

## 總結

Control Flow 的設計要點：
1. **ignore()**：避免不必要的 domain event
2. **check()**：執行過程中的斷言
3. **清楚區分**：require（前置）、ignore（優化）、check（中間）、ensure（後置）

**重要提醒**：在 uContract 2.0.0+ 版本中，使用 `ignore()` 而非 `reject()`。
