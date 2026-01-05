# Value Objects 設計指南

本文件說明如何使用 Java record 設計 Value Objects。

---

## Record 使用

### 🎯 設計意圖

Value Object 是**不可變的值物件**，使用 Java record 可以簡潔地定義。

### 📋 實作規範

```java
public record PlanId(String value) implements ValueObject {
    public PlanId {
        Objects.requireNonNull(value, "PlanId cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("PlanId cannot be empty");
        }
    }

    public static PlanId create() {
        return new PlanId(UUID.randomUUID().toString());
    }

    public static PlanId valueOf(String value) {
        return new PlanId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

### ✅ 正確範例

```java
// ✅ 帶驗證的 Value Object
public record Email(String value) implements ValueObject {
    public Email {
        Objects.requireNonNull(value, "Email cannot be null");
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private static boolean isValid(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：沒有驗證
public record PlanId(String value) implements ValueObject {
    // 缺少驗證邏輯
}

// ❌ 錯誤：使用 Contract.requireNotNull
public record PlanId(String value) {
    public PlanId {
        requireNotNull("Plan id", value);  // ❌ 應該用 Objects.requireNonNull
    }
}
```

### 🔍 檢查清單

- [ ] 使用 record 定義
- [ ] implements ValueObject
- [ ] 使用 Objects.requireNonNull() 驗證（不是 Contract.requireNotNull）
- [ ] 提供 toString() 返回純值
- [ ] 提供靜態工廠方法（create, valueOf）

---

## 類型安全

### 🎯 設計意圖

避免使用原始類型（如 String, Long），使用 Value Object 提供類型安全。

### ✅ 正確範例

```java
// ✅ 使用 Value Object
public void assignTask(TaskId taskId, UserId userId) {
    // 編譯器會檢查類型，不會混淆
}

// 使用
assignTask(taskId, userId);  // ✅ 正確
assignTask(userId, taskId);  // ❌ 編譯錯誤！
```

### ❌ 常見錯誤

```java
// ❌ 使用原始類型
public void assignTask(String taskId, String userId) {
    // 容易混淆參數順序
}

assignTask(userId, taskId);  // ❌ 編譯通過但邏輯錯誤！
```

---

## 總結

Value Objects 設計要點：
1. 使用 record 定義
2. 驗證使用 Objects.requireNonNull()
3. 提供類型安全