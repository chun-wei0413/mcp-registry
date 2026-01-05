# State Tracking 設計指南

本文件說明如何使用 old() 和 ensureAssignable() 追蹤和驗證狀態變更。

---

## old() 捕獲舊狀態

### 🎯 設計意圖 (Why)

**問題**：如何在方法執行前後比較狀態變化？

**答案**：使用 `old()` 在方法執行前捕獲物件狀態的深層複製，讓你能在 postcondition 中比較狀態變化：
1. **追蹤變更**：知道哪些欄位被修改
2. **驗證差異**：確保只有預期的欄位改變
3. **深層複製**：確保原始狀態不被修改
4. **靈活捕獲**：可以捕獲整個物件或特定屬性

### 📋 實作規範 (How)

```java
// 捕獲整個物件
var oldState = old(() -> state);

// 捕獲特定欄位
var oldName = old(() -> this.name);
var oldVersion = old(() -> getVersion());
```

**使用時機**：
- 在任何狀態修改**之前**呼叫
- 需要比較前後狀態時
- 需要驗證只有特定欄位改變時

### ✅ 正確範例

```java
public void changeEmail(String newEmail) {
    requireNotNull("New email", newEmail);
    require("Email format is valid", () -> isValidEmail(newEmail));

    // ✅ 在狀態修改前捕獲舊狀態
    var oldEmail = old(() -> this.email);
    var oldVersion = old(() -> getVersion());

    // 執行業務邏輯
    apply(new EmailChanged(userId, newEmail, UUID.randomUUID(), DateProvider.now()));

    // ✅ 使用舊狀態比較
    ensure("Email changed", () -> !this.email.equals(oldEmail));
    ensure("Email is new value", () -> this.email.equals(newEmail));
    ensure("Version incremented", () -> getVersion() == oldVersion + 1);
}

// ✅ 捕獲整個物件狀態
public void updateProfile(String nickname, String bio, String avatarUrl) {
    requireNotNull("Nickname", nickname);
    requireNotNull("Bio", bio);

    // ✅ 捕獲整個狀態物件
    var oldState = old(() -> state);

    // 執行業務邏輯
    if (nickname != null) {
        apply(new NicknameChanged(userId, nickname, UUID.randomUUID(), DateProvider.now()));
    }
    if (bio != null) {
        apply(new BioUpdated(userId, bio, UUID.randomUUID(), DateProvider.now()));
    }
    if (avatarUrl != null) {
        apply(new AvatarUrlUpdated(userId, avatarUrl, UUID.randomUUID(), DateProvider.now()));
    }

    // ✅ 確保只有允許的欄位改變
    ensureAssignable(state, oldState, ".*nickname", ".*bio", ".*avatarUrl", "version", "lastModified");
}

// ✅ 捕獲集合大小
public void addTask(TaskId taskId, String taskName) {
    requireNotNull("Task id", taskId);
    requireNotNull("Task name", taskName);

    // ✅ 捕獲舊的任務數量
    var oldTaskCount = old(() -> tasks.size());

    apply(new TaskAdded(projectId, taskId, taskName, UUID.randomUUID(), DateProvider.now()));

    // ✅ 驗證任務數量增加
    ensure("Task count increased by 1", () -> tasks.size() == oldTaskCount + 1);
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：在狀態修改後才捕獲
public void changeEmail(String newEmail) {
    apply(new EmailChanged(...));

    var oldEmail = old(() -> this.email);  // ❌ 太晚了！email 已經被修改
    ensure("Email changed", () -> !this.email.equals(oldEmail));  // ❌ 永遠是 false
}

// ✅ 正確：在修改前捕獲
public void changeEmail(String newEmail) {
    var oldEmail = old(() -> this.email);  // ✅ 在修改前捕獲
    apply(new EmailChanged(...));
    ensure("Email changed", () -> !this.email.equals(oldEmail));  // ✅ 正確比較
}

// ❌ 錯誤：直接使用欄位值（不是深層複製）
public void updateAddress(Address newAddress) {
    Address oldAddress = this.address;  // ❌ 淺複製，如果 address 是 mutable 會有問題

    apply(new AddressUpdated(...));

    ensure("Address changed", () -> !this.address.equals(oldAddress));  // ❌ 可能不正確
}

// ✅ 正確：使用 old() 深層複製
public void updateAddress(Address newAddress) {
    var oldAddress = old(() -> this.address);  // ✅ 深層複製

    apply(new AddressUpdated(...));

    ensure("Address changed", () -> !this.address.equals(oldAddress));  // ✅ 正確
}

// ❌ 錯誤：捕獲了不必要的狀態
public void rename(String newName) {
    var oldState = old(() -> state);  // ❌ 只需要 name，不需要整個 state
    var oldProjects = old(() -> projects);  // ❌ 不必要
    var oldTasks = old(() -> tasks);  // ❌ 不必要

    apply(new Renamed(...));
}

// ✅ 正確：只捕獲需要的欄位
public void rename(String newName) {
    var oldName = old(() -> this.name);  // ✅ 只捕獲需要的欄位

    apply(new Renamed(...));

    ensure("Name changed", () -> !this.name.equals(oldName));
}
```

**症狀**：狀態比較錯誤、記憶體浪費、效能問題、無法正確驗證變更。

### 🔍 檢查清單

- [ ] 在狀態修改**之前**呼叫 old()
- [ ] 只捕獲需要比較的欄位
- [ ] 使用 old() 而非直接賦值（確保深層複製）
- [ ] 考慮效能影響（避免捕獲過大的物件）
- [ ] 變數命名清楚（如 oldName, oldVersion）

---

## ensureAssignable() 驗證欄位變更

### 🎯 設計意圖 (Why)

**問題**：如何確保只有指定的欄位被修改，其他欄位保持不變？

**答案**：使用 `ensureAssignable()` 驗證只有允許的欄位改變：
1. **白名單機制**：明確列出允許改變的欄位
2. **防止意外修改**：確保其他欄位不被改變
3. **正則表達式支援**：靈活匹配欄位名稱
4. **深層比較**：自動比較物件的所有欄位

### 📋 實作規範 (How)

```java
ensureAssignable(newState, oldState, "允許欄位1", "允許欄位2", ...);
```

**參數說明**：
- 第一個參數：新狀態（當前狀態）
- 第二個參數：舊狀態（用 old() 捕獲）
- 後續參數：允許改變的欄位名稱模式（支援正則表達式）

**常用模式**：
- `"fieldName"` - 精確匹配欄位名稱
- `".*fieldName"` - 匹配包含 fieldName 的欄位
- `"version"` - 通常版本號會改變
- `"lastModified"` - 通常最後修改時間會改變

### ✅ 正確範例

```java
// ✅ 範例 1：單一欄位更新
public void changeEmail(String newEmail) {
    requireNotNull("New email", newEmail);
    require("Email format is valid", () -> isValidEmail(newEmail));

    // 捕獲舊狀態
    var oldState = old(() -> state);

    // 執行業務邏輯
    apply(new EmailChanged(userId, newEmail, UUID.randomUUID(), DateProvider.now()));

    // ✅ 確保只有 email、version、lastModified 改變
    ensureAssignable(state, oldState, ".*email", "version", "lastModified");
}

// ✅ 範例 2：多個欄位更新
public void updateProfile(String nickname, String bio) {
    var oldState = old(() -> state);

    // 執行業務邏輯
    if (nickname != null) {
        apply(new NicknameChanged(userId, nickname, UUID.randomUUID(), DateProvider.now()));
    }
    if (bio != null) {
        apply(new BioUpdated(userId, bio, UUID.randomUUID(), DateProvider.now()));
    }

    // ✅ 確保只有 nickname、bio、version、lastModified 改變
    ensureAssignable(state, oldState, ".*nickname", ".*bio", "version", "lastModified");
}

// ✅ 範例 3：嵌套物件的欄位
public void updateShippingAddress(Address newAddress) {
    requireNotNull("New address", newAddress);

    var oldState = old(() -> state);

    apply(new ShippingAddressUpdated(
        orderId,
        newAddress,
        UUID.randomUUID(),
        DateProvider.now()
    ));

    // ✅ 使用正則表達式匹配嵌套物件的欄位
    ensureAssignable(state, oldState, "shippingAddress.*", "version", "lastModifiedAt");
}

// ✅ 範例 4：複雜的業務邏輯
public void processOrder() {
    var oldState = old(() -> state);

    // 多個狀態變更
    apply(new OrderValidated(...));
    apply(new PaymentProcessed(...));
    apply(new InventoryReserved(...));

    // ✅ 允許多個相關欄位改變
    ensureAssignable(
        state,
        oldState,
        "status",
        ".*payment.*",
        ".*inventory.*",
        "processedAt",
        "version",
        "lastModified"
    );
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：沒有使用 ensureAssignable
public void changeEmail(String newEmail) {
    var oldState = old(() -> state);
    apply(new EmailChanged(...));
    // ❌ 沒有驗證其他欄位是否保持不變
}

// ✅ 正確：使用 ensureAssignable
public void changeEmail(String newEmail) {
    var oldState = old(() -> state);
    apply(new EmailChanged(...));
    ensureAssignable(state, oldState, ".*email", "version", "lastModified");  // ✅
}

// ❌ 錯誤：忘記包含 version 和 lastModified
public void changeEmail(String newEmail) {
    var oldState = old(() -> state);
    apply(new EmailChanged(...));
    ensureAssignable(state, oldState, ".*email");  // ❌ version 和 lastModified 也會改變
}

// ✅ 正確：包含所有會改變的欄位
public void changeEmail(String newEmail) {
    var oldState = old(() -> state);
    apply(new EmailChanged(...));
    ensureAssignable(state, oldState, ".*email", "version", "lastModified");  // ✅
}

// ❌ 錯誤：正則表達式錯誤
public void updateShippingAddress(Address newAddress) {
    var oldState = old(() -> state);
    apply(new ShippingAddressUpdated(...));
    ensureAssignable(state, oldState, "shippingAddress");  // ❌ 只匹配欄位名稱，不包括嵌套屬性
}

// ✅ 正確：使用正則表達式匹配嵌套屬性
public void updateShippingAddress(Address newAddress) {
    var oldState = old(() -> state);
    apply(new ShippingAddressUpdated(...));
    ensureAssignable(state, oldState, "shippingAddress.*", "version");  // ✅ 使用 .* 匹配所有嵌套屬性
}

// ❌ 錯誤：參數順序錯誤
public void changeEmail(String newEmail) {
    var oldState = old(() -> state);
    apply(new EmailChanged(...));
    ensureAssignable(oldState, state, ".*email", "version");  // ❌ 順序反了
}

// ✅ 正確：正確的參數順序
public void changeEmail(String newEmail) {
    var oldState = old(() -> state);
    apply(new EmailChanged(...));
    ensureAssignable(state, oldState, ".*email", "version");  // ✅ 新狀態在前，舊狀態在後
}
```

**症狀**：意外欄位被修改、業務規則被破壞、資料完整性問題。

### 🔍 檢查清單

- [ ] 使用 old() 捕獲舊狀態
- [ ] 參數順序正確（新狀態, 舊狀態, 允許欄位...）
- [ ] 列出所有會改變的欄位（包括 version, lastModified）
- [ ] 使用正則表達式匹配嵌套物件（如 "address.*"）
- [ ] 考慮 Event Sourcing 中 version 欄位會自動遞增
- [ ] 訊息清楚描述允許改變的欄位

---

## old() + ensureAssignable() 組合模式

### 🎯 設計意圖 (Why)

**問題**：如何完整驗證方法只修改了預期的欄位？

**答案**：組合使用 `old()` 和 `ensureAssignable()`，形成強大的狀態變更驗證機制。

### ✅ 正確範例

```java
// ✅ 完整的狀態變更驗證
public void updateShippingAddress(Address newAddress) {
    // Preconditions
    requireNotNull("New address", newAddress);
    require("Order not shipped", () -> status != OrderStatus.SHIPPED);
    require("Order not cancelled", () -> status != OrderStatus.CANCELLED);

    // ✅ Step 1: 捕獲舊狀態（用於比較和驗證）
    var oldState = old(() -> state);
    var oldAddress = old(() -> shippingAddress);
    var oldVersion = old(() -> getVersion());

    // ✅ Step 2: Early Exit 檢查
    if (ignore("Address unchanged", () -> shippingAddress.equals(newAddress))) {
        return;
    }

    // ✅ Step 3: 執行業務邏輯
    apply(new ShippingAddressUpdated(
        orderId,
        newAddress,
        UUID.randomUUID(),
        DateProvider.now()
    ));

    // ✅ Step 4: 驗證特定欄位變更
    ensure("New address is set", () -> shippingAddress.equals(newAddress));
    ensure("Address changed from old value", () -> !shippingAddress.equals(oldAddress));
    ensure("Version incremented", () -> getVersion() == oldVersion + 1);

    // ✅ Step 5: 確保只有允許的欄位改變
    ensureAssignable(state, oldState, "shippingAddress", "version", "lastModifiedAt");
}

// ✅ 複雜業務邏輯的完整驗證
public void completeOrder(PaymentInfo paymentInfo, ShippingOption shippingOption) {
    requireNotNull("Payment info", paymentInfo);
    requireNotNull("Shipping option", shippingOption);
    require("Order is pending", () -> status == OrderStatus.PENDING);

    // ✅ 捕獲舊狀態
    var oldState = old(() -> state);
    var oldStatus = old(() -> status);
    var oldVersion = old(() -> getVersion());

    // ✅ 執行多個業務邏輯
    apply(new PaymentInfoRecorded(orderId, paymentInfo, UUID.randomUUID(), DateProvider.now()));
    apply(new ShippingOptionSelected(orderId, shippingOption, UUID.randomUUID(), DateProvider.now()));
    apply(new OrderCompleted(orderId, UUID.randomUUID(), DateProvider.now()));

    // ✅ 驗證特定變更
    ensure("Status changed to completed", () -> status == OrderStatus.COMPLETED);
    ensure("Status changed from pending", () -> oldStatus == OrderStatus.PENDING);
    ensure("Payment info is recorded", () -> this.paymentInfo.equals(paymentInfo));
    ensure("Shipping option is selected", () -> this.shippingOption.equals(shippingOption));
    ensure("Completion time is set", () -> completedAt != null);

    // ✅ 確保只有預期的欄位改變
    ensureAssignable(
        state,
        oldState,
        "status",
        "paymentInfo.*",
        "shippingOption.*",
        "completedAt",
        "version",
        "lastModifiedAt"
    );
}
```

### 🔍 檢查清單

- [ ] 在狀態修改前使用 old() 捕獲
- [ ] 捕獲需要比較的特定欄位
- [ ] 使用 ensure() 驗證特定欄位變更
- [ ] 使用 ensureAssignable() 驗證只有允許的欄位改變
- [ ] 考慮 Event Sourcing 中的 version 欄位
- [ ] 考慮 lastModified 或 lastModifiedAt 欄位

---

## 總結

State Tracking 的設計要點：
1. **old() 捕獲**：在修改前捕獲舊狀態
2. **深層複製**：確保原始狀態不被修改
3. **ensureAssignable()**：驗證只有允許的欄位改變
4. **組合使用**：old() + ensure() + ensureAssignable() 形成完整驗證