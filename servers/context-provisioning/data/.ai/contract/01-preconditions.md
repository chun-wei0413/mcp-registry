# Preconditions 設計指南

本文件說明如何使用 requireNotNull() 和 require() 設計前置條件。

---

## requireNotNull() 使用

### 🎯 設計意圖

驗證參數不為 null，提供清晰的錯誤訊息。

### 📋 實作規範

```java
requireNotNull("Parameter name", parameter);
```

### ✅ 正確範例

```java
public void rename(String newName) {
    requireNotNull("New name", newName);  // ✅ 清楚指出參數名稱
    // ...
}
```

### ❌ 常見錯誤

```java
requireNotNull("Invalid", newName);  // ❌ 訊息不清楚
Objects.requireNonNull(newName);  // ❌ Aggregate 應用 Contract.requireNotNull
```

### 🔍 檢查清單

- [ ] 所有參數都經過 requireNotNull()
- [ ] 錯誤訊息清楚描述參數名稱
- [ ] Aggregate 使用 Contract.requireNotNull（不是 Objects.requireNonNull）

---

## require() 使用

### 🎯 設計意圖

驗證業務規則，失敗時拋出 PreconditionViolationException。

### 📋 實作規範

```java
require("Rule description", () -> boolean條件);
```

### ✅ 正確範例

```java
public void createProject(ProjectId projectId, String name) {
    requireNotNull("Project id", projectId);
    requireNotNull("Project name", name);

    // 業務規則驗證
    require("Plan not deleted", () -> !isDeleted);
    require("Project not exists", () -> !hasProject(projectId));
    require("Name not empty", () -> !name.isBlank());
    require("Name length valid", () -> name.length() <= 100);
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：應該用 requireNotNull
require("Name not null", () -> name != null);

// ✅ 正確
requireNotNull("Name", name);
```

### 🔍 檢查清單

- [ ] 用於業務規則驗證
- [ ] Lambda 表達式簡潔清晰
- [ ] 錯誤訊息描述業務規則

---

## 總結

Preconditions 設計要點：
1. null 檢查使用 requireNotNull()
2. 業務規則使用 require()
3. 清晰的錯誤訊息