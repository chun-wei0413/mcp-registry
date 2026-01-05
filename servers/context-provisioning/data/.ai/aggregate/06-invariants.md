# Invariants 設計指南

本文件說明如何設計和實作 Aggregate 的不變式（Invariant）。

---

## ensureInvariant() 設計

### 🎯 設計意圖

Invariant 是 Aggregate 必須始終滿足的業務規則。

### 📋 實作規範

```java
@Override
public void ensureInvariant() {
    // 1. 基本屬性檢查
    invariant("Category correct", () -> getCategory().equals(CATEGORY));
    invariantNotNull("Plan Id", planId);

    // 2. 業務狀態檢查（考慮刪除狀態）
    if (!isDeleted) {
        invariantNotNull("Plan name", name);
        invariantNotNull("User Id", userId);
        invariant("Projects not null", () -> projects != null);
    }
}
```

### ✅ 正確範例

```java
@Override
public void ensureInvariant() {
    invariant("Category is 'User'", () -> getCategory().equals(CATEGORY));
    invariantNotNull("User Id", userId);

    if (!isDeleted) {
        invariantNotNull("Username", username);
        invariantNotNull("Password", password);
        invariantNotNull("Email", email);
        invariantNotNull("Nickname", nickname);
        invariantNotNull("Role", role);
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：對已刪除的 Aggregate 檢查業務欄位
@Override
public void ensureInvariant() {
    invariantNotNull("Name", name);  // 如果已刪除，name 可能是 null
}

// ✅ 正確：考慮刪除狀態
@Override
public void ensureInvariant() {
    if (!isDeleted) {
        invariantNotNull("Name", name);
    }
}
```

### 🔍 檢查清單

- [ ] 實作 ensureInvariant() 方法
- [ ] 檢查 Category 正確
- [ ] 檢查 ID 不為 null
- [ ] 考慮 isDeleted 狀態
- [ ] 驗證業務規則

---

## 總結

Invariants 設計要點：
1. 所有 Aggregate 必須實作 ensureInvariant()
2. 考慮 isDeleted 狀態
3. 驗證業務規則始終成立