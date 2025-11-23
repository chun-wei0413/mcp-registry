# 契約設計範例 (Contract Design Examples)

本目錄包含使用 uContract 函式庫實作 Design by Contract (DbC) 的範例和指南。

## 📁 目錄內容

### 指南文檔
- **CONTRACT-GUIDE.md** - 契約設計基礎指南
  - DbC 基本概念
  - 前置條件、後置條件、不變條件
  - 設計原則和最佳實踐

- **UCONTRACT-GUIDE.md** - uContract 進階使用指南
  - 完整 API 參考
  - 進階功能：old()、ensureAssignable()、reject()
  - 效能考量和優化技巧

### 範例文檔
- **aggregate-contract-example.md** - 聚合根契約範例
  - DDD Aggregate 的契約設計
  - 領域不變條件的實作
  - 事件發布的契約保證

- **usecase-contract-example.md** - Use Case 契約範例
  - 應用層的契約設計
  - Input 驗證模式
  - Service 方法的契約

- **value-object-contract-example.md** - 值物件契約範例
  - 不可變性保證
  - 建構函數契約
  - equals/hashCode 契約

- **ucontract-detailed-examples.md** - uContract 詳細範例
  - 各種 API 的實際應用
  - 複雜場景的契約設計
  - 常見錯誤和解決方案

## 🎯 契約設計原則

### 1. 前置條件 (Preconditions)
```java
public void setAge(int age) {
    Contract.require(age >= 0, "Age must be non-negative");
    Contract.require(age <= 150, "Age must be reasonable");
    this.age = age;
}
```

### 2. 後置條件 (Postconditions)
```java
public TaskId createTask(String name) {
    var oldSize = old(() -> tasks.size());
    
    TaskId taskId = TaskId.newId();
    tasks.add(new Task(taskId, name));
    
    Contract.ensure(tasks.size() == oldSize + 1, "Task count increased by 1");
    Contract.ensureNotNull("Task ID", taskId);
    return taskId;
}
```

### 3. 不變條件 (Invariants)
```java
@Override
protected void checkInvariants() {
    Contract.invariant(!name.isBlank(), "Name must not be blank");
    Contract.invariantNotNull("Projects", projects);
    Contract.invariant(projects.size() <= MAX_PROJECTS, "Project limit not exceeded");
}
```

## 📝 使用指南

### 何時使用契約
1. **關鍵業務邏輯** - 保護核心領域規則
2. **公開 API** - 明確定義介面契約
3. **複雜演算法** - 驗證演算法正確性
4. **並發操作** - 確保線程安全

### 契約設計技巧
- 契約應該表達業務規則，而非技術細節
- 使用有意義的錯誤訊息
- 避免在契約中產生副作用
- 考慮效能影響，適度使用

## 🚀 快速開始

1. 引入 uContract 依賴
```xml
<dependency>
    <groupId>tw.teddysoft</groupId>
    <artifactId>ucontract</artifactId>
    <version>1.0.0</version>
</dependency>
```

2. 在類別中使用
```java
import static tw.teddysoft.ucontract.Contract.*;

public class Account {
    private BigDecimal balance;
    
    public void withdraw(BigDecimal amount) {
        requireNotNull("Amount", amount);
        require(amount.compareTo(BigDecimal.ZERO) > 0, "Amount must be positive");
        require(balance.compareTo(amount) >= 0, "Insufficient balance");
        
        var oldBalance = old(() -> balance);
        balance = balance.subtract(amount);
        
        ensure(balance.compareTo(oldBalance) < 0, "Balance decreased");
    }
}
```

## 📚 相關資源
- [uContract GitLab Repository](https://gitlab.com/teddysoft-private-projects)
- [Design by Contract - Wikipedia](https://en.wikipedia.org/wiki/Design_by_contract)
- [領域驅動設計與契約](../../best-practices.md)