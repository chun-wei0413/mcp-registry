# Contract 設計指南

## 概述

Contract（契約）是一種程式設計技術，透過明確定義前置條件（Preconditions）、後置條件（Postconditions）和不變條件（Invariants）來確保程式的正確性。在 DDD 和 Event Sourcing 架構中，良好的 Contract 設計能大幅提升程式碼品質和可維護性。

## Contract 基本概念

### 1. Preconditions（前置條件）
- 方法執行前必須滿足的條件
- 檢查輸入參數的有效性
- 驗證物件的當前狀態
- 使用 `require()` 和 `requireNotNull()`

### 2. Postconditions（後置條件）
- 方法執行後必須滿足的條件
- 驗證方法的執行結果
- 確保狀態變更的正確性
- 使用 `ensure()`

### 3. Invariants（不變條件）
- 物件在整個生命週期中必須保持的條件
- 在每個公開方法執行後都應該成立
- 定義在 `ensureInvariant()` 方法中
- 使用 `invariant()` 和 `invariantNotNull()`

## Contract 設計原則

### 1. 精確性原則
- Contract 應該精確描述方法的行為
- 避免模糊或過於寬鬆的條件
- 不要檢查永遠為真的條件

### 2. 完整性原則
- Preconditions 應涵蓋所有必要的輸入驗證
- Postconditions 應驗證所有重要的狀態變更
- 不要遺漏關鍵的檢查

### 3. 最小化原則
- 只檢查必要的條件
- 避免重複或冗餘的檢查
- 不要檢查不會改變的屬性

### 4. 可讀性原則
- 使用清晰的錯誤訊息
- Contract 應該易於理解
- 適當使用註解說明複雜的條件

## 常見反模式

### 1. 同義反覆檢查
```java
// ❌ 錯誤：檢查 getter 是否返回 field
ensure("Color unchanged", () -> getColor().equals(color));

// ✅ 正確：如果屬性不應該改變，就不需要檢查
// （移除不必要的檢查）
```

### 2. 重複的狀態檢查
```java
// ❌ 錯誤：postcondition 重複檢查 precondition 已驗證的狀態
public void rename(String newName) {
    require("Not deleted", () -> !isDeleted());
    // ... 執行邏輯 ...
    ensure("Still not deleted", () -> !isDeleted()); // 多餘！
}

// ✅ 正確：只檢查方法真正改變的東西
public void rename(String newName) {
    require("Not deleted", () -> !isDeleted());
    // ... 執行邏輯 ...
    ensure("Name changed", () -> getName().equals(newName));
}
```

### 3. 檢查不可變屬性
```java
// ❌ 錯誤：檢查構造後就不會改變的屬性
ensure("ID unchanged", () -> getId().equals(id));

// ✅ 正確：不需要檢查不可變屬性
// （移除不必要的檢查）
```

## uContract 進階功能

除了基本的 require/ensure/invariant，uContract 提供了更強大的進階功能：

### 1. old() - 捕獲舊狀態
使用 `old()` 可以更優雅地捕獲方法執行前的狀態：
```java
public void updateBalance(Money amount) {
    // 使用 old() 捕獲舊狀態
    var oldBalance = old(() -> this.balance);
    var oldVersion = old(() -> getVersion());
    
    // 執行業務邏輯
    this.balance = balance.add(amount);
    
    // 驗證狀態變更
    ensure("Balance updated correctly", 
        () -> balance.equals(oldBalance.add(amount)));
    ensure("Version incremented", 
        () -> getVersion() == oldVersion + 1);
}
```

### 2. ensureAssignable() - 驗證欄位變更
確保只有指定的欄位被修改：
```java
public void updateEmail(String newEmail) {
    var oldUser = old(() -> this.clone());
    
    this.email = newEmail;
    this.lastModified = DateProvider.now();
    
    // 確保只有 email 和 lastModified 改變
    ensureAssignable(this, oldUser, "email", "lastModified");
}
```

### 3. ensureResult() - 驗證返回值
```java
public User findActiveUser(String userId) {
    User user = userRepository.findById(userId);
    
    // 驗證返回值符合條件
    return ensureResult("User must be active", user, u ->
        u != null && u.isActive() && !u.isDeleted()
    );
}
```

### 4. ensureImmutableCollection() - 確保集合不可變
```java
public List<Tag> getTags() {
    return ensureImmutableCollection(
        Collections.unmodifiableList(new ArrayList<>(tags))
    );
}
```

### 5. reject() - 防止不必要的操作
```java
public void rename(String newName) {
    requireNotNull("New name", newName);
    
    // 如果名稱沒變，提前返回
    if (reject("Name unchanged", () -> name.equals(newName))) {
        return;
    }
    
    apply(new NameChanged(id, newName));
}
```

### 6. check() - 執行中斷言
```java
public void processPayment(Payment payment) {
    // 步驟 1
    boolean validated = validatePayment(payment);
    check("Payment validated", () -> validated);
    
    // 步驟 2
    PaymentResult result = executePayment(payment);
    check("Payment successful", () -> result.isSuccessful());
}
```

## 最佳實踐

### 2. 使用描述性的錯誤訊息
```java
// ❌ 錯誤：模糊的錯誤訊息
require("Invalid", () -> amount > 0);

// ✅ 正確：清晰的錯誤訊息
require("Amount must be positive", () -> amount > 0);
require(format("Amount %s exceeds limit %s", amount, limit), 
    () -> amount.compareTo(limit) <= 0);
```

### 3. 參數驗證順序
```java
public void transfer(Account target, Money amount) {
    // 1. 先檢查 null
    requireNotNull("Target account", target);
    requireNotNull("Amount", amount);
    
    // 2. 再檢查值的有效性
    require("Amount is positive", () -> amount.isPositive());
    require("Different accounts", () -> !this.equals(target));
    
    // 3. 最後檢查業務規則
    require("Sufficient balance", () -> balance.compareTo(amount) >= 0);
    require("Account is active", () -> !isClosed());
}
```

## Contract 檢查清單

使用此清單確保你的 Contract 設計正確：

- [ ] **Preconditions 完整性**
  - [ ] 所有參數都有 null 檢查（如果不允許 null）
  - [ ] 參數值在有效範圍內
  - [ ] 物件處於正確狀態
  - [ ] 業務規則得到滿足

- [ ] **舊狀態儲存**
  - [ ] 只儲存會改變的值
  - [ ] 不儲存 immutable 的值
  - [ ] 在執行業務邏輯前儲存

- [ ] **Postconditions 有效性**
  - [ ] 驗證所有重要的狀態變更
  - [ ] 不檢查 getter == field
  - [ ] 不重複 precondition 的檢查
  - [ ] 專注於方法的實際效果

- [ ] **錯誤訊息品質**
  - [ ] 訊息清晰明確
  - [ ] 包含足夠的上下文資訊
  - [ ] 有助於除錯

## 延伸閱讀

- [Aggregate Contract 範例](./aggregate-contract-example.md)
- [Use Case Contract 範例](./usecase-contract-example.md)
- [Value Object Contract 範例](./value-object-contract-example.md)
- [uContract 進階功能指南](./UCONTRACT-GUIDE.md)
- [uContract 詳細範例集](./ucontract-detailed-examples.md)