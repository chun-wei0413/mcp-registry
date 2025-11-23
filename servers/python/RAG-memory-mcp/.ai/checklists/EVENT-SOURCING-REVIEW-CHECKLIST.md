# Event Sourcing Code Review Checklist

## 🔴 必查項目（違反任一項 = 嚴重錯誤）

### 建構子檢查
- [ ] **業務建構子不可直接設定狀態**
  ```java
  // ❌ 錯誤
  public Product(ProductId id, ProductName name) {
      this.productId = id;  // 不可以！
      this.productName = name;  // 不可以！
      apply(new ProductCreated(...));
  }
  
  // ✅ 正確
  public Product(ProductId id, ProductName name) {
      apply(new ProductCreated(...));  // 只發事件
  }
  ```

- [ ] **ES 重建建構子必須呼叫 super(events)**
  ```java
  // ❌ 錯誤
  public Product(List<ProductEvents> events) {
      for (ProductEvents event : events) {
          when(event);  // 不可以自己處理！
      }
  }
  
  // ✅ 正確
  public Product(List<ProductEvents> events) {
      super(events);  // 讓框架處理
  }
  ```

### 狀態管理檢查
- [ ] **所有狀態變更必須在 when() 方法中**
- [ ] **狀態不可在 when() 之外被修改**
- [ ] **每個事件都有對應的 when() 處理**

### Single Source of Truth 檢查
- [ ] **狀態只有一個設定點（when 方法）**
- [ ] **沒有重複的狀態設定邏輯**
- [ ] **apply() 後的狀態與 when() 設定的一致**

## 🟡 重要檢查項目

### Event 設計
- [ ] Events 是 sealed interface
- [ ] Event records 包含所有必要欄位
- [ ] 有 TypeMapper 和 mapper() 方法
- [ ] ConstructionEvent 用於創建事件
- [ ] DestructionEvent 用於刪除事件

### 不變式檢查
- [ ] ensureInvariant() 包含所有業務規則
- [ ] 使用 Contract.invariant 而非 assert
- [ ] 檢查邏輯完整且正確

## 🟢 建議檢查項目

### 程式碼品質
- [ ] 沒有不必要的註解
- [ ] 使用 DateProvider.now() 而非 Instant.now()
- [ ] Value Objects 有 valueOf() 方法
- [ ] 適當的錯誤處理

## 🚨 紅旗警訊（看到這些要特別小心）

1. **建構子中有 this.xxx = xxx**
2. **when() 方法外有狀態修改**
3. **直接呼叫 when() 而非 apply()**
4. **ES 建構子有 for loop**
5. **狀態被設定多次**