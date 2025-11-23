# ezSpec Rule Migration Guide

## 將現有測試遷移到使用 Rules

本指南幫助您將現有的 ezSpec 測試遷移到使用 Rule 功能。

## 遷移步驟

### 1. 分析現有測試場景

首先，分析您的測試類別中的所有測試方法，將它們按業務邏輯分類：

```java
// 原始測試類別
public class YourUseCaseTest {
    // 測試方法 1: 成功創建
    // 測試方法 2: 重複 ID 驗證
    // 測試方法 3: 空值驗證
    // 測試方法 4: 無效格式驗證
    // 測試方法 5: 權限檢查
}
```

### 2. 定義 Rule 常量

根據分析結果，定義相應的 Rule：

```java
// 添加 Rule 定義
static final String SUCCESSFUL_OPERATIONS = "Successful Operations";
static final String DUPLICATE_VALIDATION = "Duplicate Validation";
static final String INPUT_VALIDATION = "Input Validation";
static final String AUTHORIZATION = "Authorization Checks";
```

### 3. 修改 Feature 初始化

更新 `beforeAll()` 方法：

```java
// 原始代碼
@BeforeAll
static void beforeAll() {
    feature.initialize();
}

// 遷移後
@BeforeAll
static void beforeAll() {
    feature = Feature.New(FEATURE_NAME);
    feature.initialize();
    
    // 創建 Rules
    feature.NewRule(SUCCESSFUL_OPERATIONS);
    feature.NewRule(DUPLICATE_VALIDATION);
    feature.NewRule(INPUT_VALIDATION);
    feature.NewRule(AUTHORIZATION);
}
```

### 4. 為測試方法添加 Rule

為每個 `@EzScenario` 添加 rule 參數：

```java
// 原始代碼
@EzScenario
public void create_successfully() {
    // ...
}

// 遷移後
@EzScenario(rule = SUCCESSFUL_OPERATIONS)
public void create_successfully() {
    // ...
}
```

## 實際遷移範例

### Before (無 Rules)

```java
@EzFeature
@EzFeatureReport
public class CreateOrderUseCaseTest {
    
    static String FEATURE_NAME = "Create Order";
    static Feature feature = Feature.New(FEATURE_NAME);
    
    @BeforeAll
    static void beforeAll() {
        feature.initialize();
    }
    
    @EzScenario
    public void create_order_successfully() {
        // 成功創建訂單的測試
    }
    
    @EzScenario
    public void reject_duplicate_order_number() {
        // 拒絕重複訂單號的測試
    }
    
    @EzScenario
    public void reject_empty_customer_id() {
        // 拒絕空客戶 ID 的測試
    }
    
    @EzScenario
    public void reject_negative_amount() {
        // 拒絕負金額的測試
    }
}
```

### After (使用 Rules)

```java
@EzFeature
@EzFeatureReport
public class CreateOrderUseCaseTest {
    
    static String FEATURE_NAME = "Create Order";
    static Feature feature;
    
    // Define Rules
    static final String SUCCESSFUL_CREATION = "Successful Order Creation";
    static final String DUPLICATE_VALIDATION = "Duplicate Order Validation";
    static final String INPUT_VALIDATION = "Input Validation";
    
    @BeforeAll
    static void beforeAll() {
        feature = Feature.New(FEATURE_NAME);
        feature.initialize();
        
        // Create Rules
        feature.NewRule(SUCCESSFUL_CREATION);
        feature.NewRule(DUPLICATE_VALIDATION);
        feature.NewRule(INPUT_VALIDATION);
    }
    
    @EzScenario(rule = SUCCESSFUL_CREATION)
    public void create_order_successfully() {
        // 成功創建訂單的測試
    }
    
    @EzScenario(rule = DUPLICATE_VALIDATION)
    public void reject_duplicate_order_number() {
        // 拒絕重複訂單號的測試
    }
    
    @EzScenario(rule = INPUT_VALIDATION)
    public void reject_empty_customer_id() {
        // 拒絕空客戶 ID 的測試
    }
    
    @EzScenario(rule = INPUT_VALIDATION)
    public void reject_negative_amount() {
        // 拒絕負金額的測試
    }
}
```

## Rule 分類建議

### 基本分類模式

1. **按操作結果分類**
   - Successful Operations
   - Failed Operations
   - Partial Success

2. **按驗證類型分類**
   - Input Validation
   - Business Rule Validation
   - Security Validation

3. **按功能模塊分類**
   - Core Features
   - Advanced Features
   - Integration Points

### 領域特定分類

根據您的領域，可能有特定的分類方式：

**電商領域**
- Order Processing
- Payment Validation
- Inventory Management
- Customer Management

**任務管理領域**
- Task Creation
- Task Assignment
- Task Completion
- Collaboration Features

## 遷移檢查清單

- [ ] 分析所有測試方法並分類
- [ ] 定義有意義的 Rule 名稱
- [ ] 將 `Feature feature = Feature.New()` 改為 `Feature feature;`
- [ ] 在 `beforeAll()` 中初始化 feature 並創建 Rules
- [ ] 為每個 `@EzScenario` 添加 rule 參數
- [ ] 執行測試確保功能正常
- [ ] 查看生成的報告確認 Rule 分組正確

## 注意事項

1. **Rule 必須在 `initialize()` 之後創建**
2. **Rule 名稱必須唯一**
3. **如果不指定 Rule，測試會歸類到 "default" Rule**
4. **可以使用 `withRule()` 方法動態指定 Rule**

## 遷移的好處

1. **更好的測試組織**: 相關測試分組在一起
2. **更清晰的報告**: 按業務邏輯分類的測試結果
3. **更容易維護**: 快速找到特定類型的測試
4. **更好的文檔**: Rule 名稱提供測試意圖的文檔