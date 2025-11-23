# AI 程式碼驗證檢查清單

> 每次 AI 產生程式碼後，使用此清單逐項檢查

## ✅ Level 1：語法層面檢查（自動化）

### Import 檢查
```bash
# 執行這個命令檢查是否有錯誤的 import
grep -r "import.*Repository;" . | grep -v "tw.teddysoft.ezddd"
grep -r "extends.*Repository" . | grep -v "interface"
```

- [ ] 沒有自定義的 Repository 實作類別
- [ ] 沒有自定義的 DomainEvent 類別
- [ ] 沒有自定義的 ValueObject 基礎類別

### 註解檢查
```bash
# 檢查 Service 實作是否誤用註解
grep -r "@Service" . | grep "implements.*UseCase"
grep -r "@Transactional" . | grep "Service"
```

- [ ] Service 實作沒有 @Service 註解
- [ ] Service 實作沒有 @Transactional 註解
- [ ] Service 實作沒有 @AllArgsConstructor

## ✅ Level 2：Domain 層驗證方式檢查（重要）

### Contract vs Objects 使用檢查
```bash
# 檢查 Value Objects 是否錯誤使用 Contract
grep -r "implements ValueObject" . -A 10 | grep "Contract\."
grep -r "implements Entity" . -A 10 | grep "Contract\."
```

- [ ] **Aggregate Root** 必須使用 `Contract.requireNotNull()` ✅
- [ ] **Entity** 必須使用 `Objects.requireNonNull()` ✅ 
- [ ] **Value Object** 必須使用 `Objects.requireNonNull()` ✅
- [ ] 違反以上規則必須標記為 **MUST FIX**（不是建議）

### 正確範例對照
```java
// ✅ Aggregate Root (AiScrum 專案)
public class Product extends EsAggregateRoot<ProductId, ProductEvents> {
    public Product(ProductId productId, String name, String userId) {
        super(productId);
        requireNotNull("productId", productId);  // Contract
        requireNotNull("name", name);  // Contract
        requireNotNull("userId", userId);  // Contract
        apply(ProductEvents.ProductCreated.create(productId, name, userId));
    }
}

// ✅ Value Object (AiScrum 專案)
public record ProductId(String value) implements ValueObject {
    public ProductId {
        Objects.requireNonNull(value, "ProductId value cannot be null");  // Objects
    }
}

// ✅ Entity (AiScrum 專案)
public class ProductGoal implements Entity<ProductGoalId> {
    private final ProductGoalId id;
    private String name;
    
    public ProductGoal(ProductGoalId id, String name) {
        this.id = Objects.requireNonNull(id, "ProductGoalId cannot be null");  // Objects
        this.name = Objects.requireNonNull(name, "name cannot be null");  // Objects
    }
}
```

## ✅ Level 3：結構層面檢查（半自動化）

### Package 結構檢查
```
正確結構：
[aggregate]/
├── entity/           # Aggregate, Events, Value Objects
├── usecase/
│   ├── port/
│   │   ├── in/      # Use Case interfaces (含 Input inner class)
│   │   └── out/     # Projections, Repositories
│   └── service/     # Use Case 實作
└── adapter/         # Controllers, JPA entities
```

- [ ] Input 類別是 UseCase interface 的 inner class
- [ ] 沒有獨立的 Input 檔案
- [ ] Service 實作在 usecase/service 包

### 命名規範檢查（AiScrum 專案）
- [ ] Use Case：`[Operation][Aggregate]UseCase` (如 `CreateProductUseCase`)
- [ ] Service：`[Operation][Aggregate]Service` (如 `CreateProductService`)
- [ ] Events：`[Aggregate]Events` (如 `ProductEvents`, `SprintEvents`)
- [ ] Value Object：`[Concept]Id` (如 `ProductId`, `SprintId`, `PbiId`)
- [ ] Projection：`[Aggregate][View]Projection` (如 `ProductListProjection`)

## ✅ Level 3：設計模式檢查（人工）

### Aggregate 檢查
- [ ] 繼承 `EsAggregateRoot<ID, Events>`
- [ ] when() 方法使用 switch expression
- [ ] 使用公開建構函數（不用 static factory method）
- [ ] 使用 Contract.requireNotNull 驗證
- [ ] 實作 ensureInvariant()
- [ ] 集合欄位在宣告時初始化（避免事件重播問題）

### Domain Events 檢查（2024-08-12 更新）

#### 🚨 新規範必須檢查項目
- [ ] **MUST**: 包含 `Map<String, String> metadata` 欄位（在 UUID id 之前）
- [ ] **MUST**: Override `metadata()` 方法返回 metadata 欄位（不是 Map.of()）
- [ ] **MUST**: Override `source()` 方法返回 Aggregate 名稱
- [ ] **MUST**: 在 compact constructor 中包含 `Objects.requireNonNull(metadata)`
- [ ] **MUST**: 創建事件時使用 `new HashMap<>()` 而非 `Map.of()`（保證可變性）
- [ ] **MUST**: 確認 Use Case 可以修改 metadata（冪等性需求）

#### 原有檢查項目
- [ ] 使用 sealed interface 並 extends InternalDomainEvent
- [ ] 每個 event 是 record
- [ ] 使用 Value Objects（非 primitive types）
- [ ] 包含 UUID id 和 Instant occurredOn
- [ ] 有 static DomainEventTypeMapper mapper() 方法
- [ ] TypeMapper 使用新的實作方式（見 CODE-TEMPLATES.md）

### Use Case 檢查
- [ ] Interface 繼承 Command 或 Query
- [ ] Input 是 inner class 並 implements Input
- [ ] Service 手動建構函數 + requireNotNull
- [ ] execute() 方法有 try-catch
- [ ] 遵循四步驟：載入→業務邏輯→保存→返回

### 測試檢查
- [ ] Use Case 測試使用 @EzFeature
- [ ] 測試方法使用 @EzScenario
- [ ] 遵循 Given-When-Then 格式

### Mutation Testing 檢查 🆕
- [ ] POM 已配置 PIT mutation testing plugin
- [ ] 配置排除 uContract：`<avoidCallsTo>tw.teddysoft.ucontract</avoidCallsTo>`
- [ ] Aggregate 有適當的 Contracts（preconditions, postconditions, invariants）
- [ ] 新加入的 Contracts 不破壞既有測試（100% 相容性）
- [ ] 考慮建立 Assertion-Free Tests 來搭配 Contracts
- [ ] Mutation Coverage 目標：> 80%（排除 uContract 後）
- [ ] 測試使用正確的 Repository，不直接操作 Aggregate
- [ ] 避免在 test-outbox profile 執行（會超時）

## ✅ Level 4：業務邏輯檢查

### 一致性檢查
- [ ] 一個 Use Case 只修改一個 Aggregate
- [ ] Domain Event 包含所有必要資訊
- [ ] 業務規則在 Aggregate 中驗證
- [ ] 錯誤訊息清晰明確

### 效能考量
- [ ] 沒有使用 JPA Lazy Loading
- [ ] 使用 Projection 進行複雜查詢
- [ ] 避免 N+1 查詢問題

## 📊 評分標準

### 計算方式
- Level 1 (25%): 基礎語法正確性
- Level 2 (25%): 結構符合規範
- Level 3 (35%): 設計模式正確
- Level 4 (15%): 業務邏輯品質

### 品質等級
- 95-100%: 🟢 優秀 - 可直接使用
- 85-94%:  🟡 良好 - 小幅調整
- 70-84%:  🟠 及格 - 需要修改
- <70%:    🔴 不及格 - 需要重寫

## 🔧 自動化驗證腳本

```bash
#!/bin/bash
# validate-ai-code.sh

echo "🔍 開始驗證 AI 產生的程式碼..."

# Level 1: Import 檢查
echo "Level 1: Import 檢查"
errors=0

if grep -r "implements Repository" . --include="*.java" | grep -v "interface"; then
    echo "❌ 發現自定義 Repository 實作"
    ((errors++))
fi

if grep -r "@Service" . --include="*Service.java" | grep -q "implements.*UseCase"; then
    echo "❌ Service 實作使用了 @Service 註解"
    ((errors++))
fi

# Level 2: 結構檢查
echo "Level 2: 結構檢查"
if find . -name "*Input.java" -not -path "*/test/*"; then
    echo "❌ 發現獨立的 Input 檔案"
    ((errors++))
fi

# 評分
if [ $errors -eq 0 ]; then
    echo "✅ 通過所有自動化檢查"
else
    echo "❌ 發現 $errors 個問題"
fi
```

## 💡 使用建議

### 1. 整合到工作流程
```markdown
1. AI 產生程式碼
2. 執行自動化檢查（Level 1-2）
3. 人工檢查（Level 3-4）
4. 修正問題
5. 重複直到通過
```

### 2. 建立習慣
- 每次 code review 都使用此清單
- 發現新問題就更新清單
- 定期統計常見問題

### 3. 持續改進
- 將常見錯誤加入 FAILURE-CASES.md
- 更新 tech-stacks/java-ca-ezddd-spring/anti-patterns.md
- 優化自動化腳本