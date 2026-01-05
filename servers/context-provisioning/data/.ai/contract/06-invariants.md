# Invariants 設計指南

本文件說明如何使用 invariant() 和 invariantNotNull() 實作 Aggregate 的不變式。

---

## invariant() 基本使用

### 🎯 設計意圖 (Why)

**問題**：如何確保 Aggregate 始終滿足業務規則？

**答案**：使用 `invariant()` 和 `invariantNotNull()` 在 `ensureInvariant()` 方法中定義不變式：
1. **一致性保證**：確保 Aggregate 始終處於有效狀態
2. **業務規則維護**：明確定義 Aggregate 的業務約束
3. **自動驗證**：框架自動呼叫 ensureInvariant()
4. **失敗快速**：違反不變式立即拋出異常

### 📋 實作規範 (How)

```java
@Override
public void ensureInvariant() {
    // 使用 invariant() 檢查條件
    invariant("Description", () -> boolean條件);

    // 使用 invariantNotNull() 檢查 null
    invariantNotNull("Field name", fieldValue);
}
```

**呼叫時機**：
- 框架在每次業務方法執行後自動呼叫
- 確保 Aggregate 狀態始終有效
- 違反不變式會拋出 InvariantException

### ✅ 正確範例

```java
// ✅ 範例 1：基本的不變式
@Override
public void ensureInvariant() {
    // ✅ 檢查 Category 正確
    invariant(format("Category is '%s'.", getCategory()),
        () -> getCategory().equals(CATEGORY));

    // ✅ 檢查 ID 不為 null
    invariantNotNull("User Id", userId);

    // ✅ 考慮刪除狀態
    if (!isDeleted) {
        invariantNotNull("Username", username);
        invariantNotNull("Password", password);
        invariantNotNull("Email", email);
        invariantNotNull("Nickname", nickname);
        invariantNotNull("Role", role);
    }
}

// ✅ 範例 2：包含業務規則的不變式
@Override
public void ensureInvariant() {
    invariant("Category is 'Plan'", () -> getCategory().equals(CATEGORY));
    invariantNotNull("Plan Id", planId);

    if (!isDeleted) {
        invariantNotNull("Plan name", name);
        invariantNotNull("User Id", userId);
        invariantNotNull("Projects collection", projects);

        // ✅ 業務規則：計畫名稱不能為空
        invariant("Plan name is not empty", () -> !name.trim().isEmpty());

        // ✅ 業務規則：Projects 必須是有效的 Map
        invariant("Projects is a valid map", () -> projects instanceof Map);
    }
}

// ✅ 範例 3：複雜的業務規則
@Override
public void ensureInvariant() {
    invariant("Category is 'Order'", () -> getCategory().equals(CATEGORY));
    invariantNotNull("Order Id", orderId);

    if (!isDeleted) {
        invariantNotNull("Customer name", customerName);
        invariantNotNull("Order status", status);
        invariantNotNull("Order items", orderItems);

        // ✅ 業務規則：訂單必須至少有一個項目
        invariant("Order has at least one item", () -> !orderItems.isEmpty());

        // ✅ 業務規則：所有項目的數量必須大於 0
        invariant("All items have positive quantity", () ->
            orderItems.stream().allMatch(item -> item.getQuantity() > 0));

        // ✅ 業務規則：已完成的訂單必須有完成時間
        invariant("Completed order must have completion time", () ->
            status != OrderStatus.COMPLETED || completedAt != null);

        // ✅ 業務規則：已取消的訂單不能有發貨地址
        invariant("Cancelled order cannot have shipping address", () ->
            status != OrderStatus.CANCELLED || shippingAddress == null);
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：沒有實作 ensureInvariant()
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    // ❌ 缺少 ensureInvariant() 方法
}

// ✅ 正確：實作 ensureInvariant()
public class Plan extends AggregateRoot<PlanId, PlanEvents> {
    @Override
    public void ensureInvariant() {
        invariantNotNull("Plan Id", planId);
        // ...
    }
}

// ❌ 錯誤：對已刪除的 Aggregate 檢查業務欄位
@Override
public void ensureInvariant() {
    invariantNotNull("Name", name);  // ❌ 如果已刪除，name 可能是 null
    invariantNotNull("User Id", userId);
}

// ✅ 正確：考慮刪除狀態
@Override
public void ensureInvariant() {
    if (!isDeleted) {
        invariantNotNull("Name", name);
        invariantNotNull("User Id", userId);
    }
}

// ❌ 錯誤：使用 require 或 ensure
@Override
public void ensureInvariant() {
    require("Name not null", () -> name != null);  // ❌ 應該用 invariant
    ensure("User Id not null", () -> userId != null);  // ❌ 應該用 invariant
}

// ✅ 正確：使用 invariant 和 invariantNotNull
@Override
public void ensureInvariant() {
    invariantNotNull("Name", name);
    invariantNotNull("User Id", userId);
}

// ❌ 錯誤：不變式太弱
@Override
public void ensureInvariant() {
    invariantNotNull("User Id", userId);  // ❌ 只檢查 ID，沒有檢查其他欄位
}

// ✅ 正確：完整的不變式
@Override
public void ensureInvariant() {
    invariant("Category is 'User'", () -> getCategory().equals(CATEGORY));
    invariantNotNull("User Id", userId);
    if (!isDeleted) {
        invariantNotNull("Username", username);
        invariantNotNull("Email", email);
        invariantNotNull("Role", role);
    }
}

// ❌ 錯誤：檢查可變的狀態
@Override
public void ensureInvariant() {
    invariant("User is active", () -> status == UserStatus.ACTIVE);  // ❌ 狀態會改變
}

// ✅ 正確：只檢查不變的規則
@Override
public void ensureInvariant() {
    invariantNotNull("User Id", userId);  // ✅ ID 永遠不變
    if (!isDeleted) {
        invariant("Status is valid", () ->
            status == UserStatus.ACTIVE ||
            status == UserStatus.INACTIVE ||
            status == UserStatus.SUSPENDED
        );  // ✅ 狀態必須是有效值之一
    }
}
```

**症狀**：Aggregate 處於無效狀態、業務規則被破壞、資料不一致、難以追蹤錯誤。

### 🔍 檢查清單

- [ ] 所有 Aggregate 都實作 ensureInvariant()
- [ ] 檢查 Category 正確
- [ ] 檢查 ID 不為 null
- [ ] 考慮 isDeleted 狀態
- [ ] 檢查所有必要欄位不為 null
- [ ] 定義業務規則約束
- [ ] 不檢查會改變的狀態，只檢查不變的規則
- [ ] 錯誤訊息清楚描述不變式

---

## invariantNotNull() 使用

### 🎯 設計意圖 (Why)

**問題**：如何簡潔地檢查欄位不為 null？

**答案**：使用 `invariantNotNull()` 檢查欄位，比 `invariant()` 更簡潔。

### 📋 實作規範 (How)

```java
invariantNotNull("Field name", fieldValue);

// 等同於
invariant("Field name is not null", () -> fieldValue != null);
```

### ✅ 正確範例

```java
@Override
public void ensureInvariant() {
    // ✅ 使用 invariantNotNull 檢查必要欄位
    invariantNotNull("Plan Id", planId);
    invariantNotNull("Plan name", name);
    invariantNotNull("User Id", userId);
    invariantNotNull("Projects", projects);

    // ✅ 比使用 invariant 更簡潔
    // invariant("Plan Id is not null", () -> planId != null);  // 冗長
    // invariant("Plan name is not null", () -> name != null);  // 冗長
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：使用 invariant 檢查 null（冗長）
@Override
public void ensureInvariant() {
    invariant("User Id is not null", () -> userId != null);  // ❌ 應該用 invariantNotNull
    invariant("Name is not null", () -> name != null);
}

// ✅ 正確：使用 invariantNotNull
@Override
public void ensureInvariant() {
    invariantNotNull("User Id", userId);  // ✅ 更簡潔
    invariantNotNull("Name", name);
}
```

### 🔍 檢查清單

- [ ] 檢查 null 時優先使用 invariantNotNull()
- [ ] 第一個參數是清楚的欄位名稱
- [ ] 第二個參數是要檢查的欄位值

---

## Invariant 設計模式

### 🎯 設計意圖 (Why)

**問題**：如何組織複雜的不變式？

**答案**：按照固定的順序組織不變式檢查。

### 📋 實作規範 (How)

```java
@Override
public void ensureInvariant() {
    // 1. Category 檢查
    invariant("Category is correct", () -> getCategory().equals(CATEGORY));

    // 2. ID 檢查
    invariantNotNull("Aggregate Id", aggregateId);

    // 3. 業務欄位檢查（考慮刪除狀態）
    if (!isDeleted) {
        // 3.1 必要欄位 null 檢查
        invariantNotNull("Field1", field1);
        invariantNotNull("Field2", field2);

        // 3.2 業務規則檢查
        invariant("Business rule 1", () -> ...);
        invariant("Business rule 2", () -> ...);

        // 3.3 集合檢查
        invariantNotNull("Collection", collection);
        invariant("Collection is valid", () -> collection instanceof Map);
    }
}
```

### ✅ 正確範例

```java
// ✅ 完整的不變式範例
@Override
public void ensureInvariant() {
    // Step 1: Category 檢查
    invariant(format("Category is '%s'.", getCategory()),
        () -> getCategory().equals(CATEGORY));

    // Step 2: ID 檢查
    invariantNotNull("Plan Id", planId);

    // Step 3: 業務欄位檢查
    if (!isDeleted) {
        // Step 3.1: 必要欄位 null 檢查
        invariantNotNull("Plan name", name);
        invariantNotNull("User Id", userId);
        invariantNotNull("Projects", projects);

        // Step 3.2: 業務規則檢查
        invariant("Plan name is not empty", () -> !name.trim().isEmpty());
        invariant("Plan name length is valid", () ->
            name.length() >= 1 && name.length() <= 200);
        invariant("Projects is a map", () -> projects instanceof Map);

        // Step 3.3: 集合內容檢查
        invariant("All projects have valid names", () ->
            projects.values().stream()
                .allMatch(p -> p.getName() != null && !p.getName().isEmpty())
        );
    }
}
```

### 🔍 檢查清單

- [ ] 按照固定順序組織（Category → ID → 業務欄位）
- [ ] 考慮 isDeleted 狀態
- [ ] 先檢查 null，再檢查業務規則
- [ ] 集合檢查包含類型和內容驗證
- [ ] 每個檢查都有清楚的錯誤訊息

---

## 常見的不變式模式

### ✅ 模式 1：簡單 Aggregate

```java
@Override
public void ensureInvariant() {
    invariant("Category is 'Tag'", () -> getCategory().equals(CATEGORY));
    invariantNotNull("Tag Id", tagId);

    if (!isDeleted) {
        invariantNotNull("Tag name", name);
        invariantNotNull("Tag color", color);
        invariant("Tag name is not empty", () -> !name.trim().isEmpty());
    }
}
```

### ✅ 模式 2：包含集合的 Aggregate

```java
@Override
public void ensureInvariant() {
    invariant("Category is 'Plan'", () -> getCategory().equals(CATEGORY));
    invariantNotNull("Plan Id", planId);

    if (!isDeleted) {
        invariantNotNull("Plan name", name);
        invariantNotNull("Projects", projects);
        invariant("Projects is a map", () -> projects instanceof Map);
    }
}
```

### ✅ 模式 3：複雜業務規則的 Aggregate

```java
@Override
public void ensureInvariant() {
    invariant("Category is 'Order'", () -> getCategory().equals(CATEGORY));
    invariantNotNull("Order Id", orderId);

    if (!isDeleted) {
        invariantNotNull("Customer name", customerName);
        invariantNotNull("Order status", status);
        invariantNotNull("Order items", orderItems);

        // 複雜業務規則
        invariant("Order has at least one item", () -> !orderItems.isEmpty());
        invariant("All items have positive quantity", () ->
            orderItems.stream().allMatch(item -> item.getQuantity() > 0));
        invariant("Total amount matches items", () -> {
            Money calculatedTotal = orderItems.stream()
                .map(item -> item.getPrice().multiply(item.getQuantity()))
                .reduce(Money.ZERO, Money::add);
            return totalAmount.equals(calculatedTotal);
        });
    }
}
```

---

## 總結

Invariants 的設計要點：
1. **invariant()**：檢查業務規則條件
2. **invariantNotNull()**：檢查欄位不為 null
3. **固定順序**：Category → ID → 業務欄位
4. **考慮刪除狀態**：使用 `if (!isDeleted)`
5. **清楚訊息**：描述業務規則，不只是技術細節