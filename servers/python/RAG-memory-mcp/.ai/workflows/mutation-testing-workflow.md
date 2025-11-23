# Mutation Testing Enhancement Workflow

## 目的
系統化地使用 PIT mutation testing 搭配 uContract 來提升測試品質，達成高 mutation coverage。

## 前置條件
- [ ] Maven 專案配置完成
- [ ] PIT plugin 已加入 pom.xml
- [ ] uContract 依賴已配置（版本 2.0.0+）
- [ ] 目標 Entity 有基本測試

## 工作流程

### Step 1: 初始評估
```bash
# 1.1 檢查 POM 配置
grep -A 5 "pitest-maven" pom.xml

# 1.2 確認 uContract 排除
grep "avoidCallsTo.*ucontract" pom.xml

# 1.3 執行自動化檢查
.ai/scripts/check-mutation-coverage.sh [EntityName]
```

### Step 2: 基準測試
```bash
# 2.1 執行基準 mutation testing
mvn org.pitest:pitest-maven:mutationCoverage \
    -DtargetClasses="*.EntityName" \
    -DtargetTests="*.EntityName*Test" \
    -q

# 2.2 記錄基準指標
# - Line Coverage: X%
# - Mutation Score: X%  
# - Test Strength: X%
```

### Step 3: Contract 增強策略

#### 3.1 分析現有程式碼
```java
// 識別可加入 contract 的位置：
// - 方法入口（preconditions）
// - 方法出口（postconditions）
// - 類別不變式（invariants）
```

#### 3.2 漸進式實施（重要！）
```java
// 優先順序：
// 1. Postconditions（最安全）
ensure("結果驗證", () -> condition);

// 2. Invariants（資料一致性）
invariant("一致性規則", () -> condition);

// 3. Preconditions（最謹慎）
require("輸入驗證", () -> condition);
```

#### 3.3 立即驗證
```bash
# 每加一個 contract 就測試
mvn test -Dtest='EntityName*Test' -q

# 如果失敗立即回滾
git checkout -- src/main/java/.../EntityName.java
```

### Step 4: Assertion-Free Test 開發

#### 4.1 創建測試檔案
```java
public class EntityNameAssertionFreeTest {
    
    @Test
    void exerciseCompleteLifecycle() {
        // 建立物件
        Entity entity = new Entity(...);
        
        // 執行所有操作
        entity.operation1(...);
        entity.operation2(...);
        
        // 不需要 assertions
        // Contracts 會驗證一切
    }
    
    @Test
    void exerciseEdgeCases() {
        // 測試邊界情況
        // Contract violations 會自動偵測
    }
}
```

#### 4.2 執行測試
```bash
mvn test -Dtest='EntityNameAssertionFreeTest' -q
```

### Step 5: 成效評估

#### 5.1 執行最終測試
```bash
mvn org.pitest:pitest-maven:mutationCoverage \
    -DtargetClasses="*.EntityName" \
    -DtargetTests="*.EntityName*Test" \
    -q
```

#### 5.2 比較指標
```
基準 → 最終：
- Mutation Score: 10% → 40% (+30%)
- Test Strength: 60% → 85% (+25%)
```

#### 5.3 產生報告
```bash
# 開啟 HTML 報告
open target/pit-reports/*/index.html
```

## 決策點

### 何時停止加入 Contracts？
- ✅ Mutation Score > 80%
- ✅ Test Strength > 85%
- ✅ 所有既有測試通過
- ✅ 沒有破壞業務邏輯

### 何時需要寫更多測試？
- Contract 無法覆蓋的邏輯
- 複雜的業務流程
- 外部整合點

## 檢查清單

### 開始前
- [ ] POM 已配置 PIT plugin
- [ ] uContract 已排除於 mutation testing
- [ ] 基準測試已執行

### 實施中
- [ ] 使用漸進式方法加入 contracts
- [ ] 每次變更後測試
- [ ] 建立 assertion-free tests
- [ ] 不改變業務邏輯

### 完成後
- [ ] Mutation Score > 80%
- [ ] Test Strength > 85%
- [ ] 所有測試通過
- [ ] 更新 ADR 文件

## 工具與資源

### 自動化腳本
```bash
# 完整分析與建議
.ai/scripts/check-mutation-coverage.sh EntityName
```

### Sub-agent 支援
```
請使用 mutation-testing-sub-agent workflow 為 [EntityName] 提升 mutation coverage
```

### 參考文件
- `.ai/prompts/mutation-testing-sub-agent-prompt.md` - Sub-agent prompt
- `.dev/adr/ADR-025-mutation-testing-ucontract-exclusion.md` - 策略說明
- `.ai/CODE-TEMPLATES.md` - Contract 範例

## 常見陷阱

### ❌ 避免
1. 一次加入太多 contracts
2. 改變既有業務邏輯
3. 為 coverage 而加無意義 contracts
4. 忽略測試失敗

### ✅ 最佳實踐
1. 小步前進，頻繁測試
2. 優先 postconditions
3. 保持 contracts 簡單明確
4. 記錄學習與經驗

## 成功案例

### ProductBacklogItem
- 初始: 10% mutation score
- 加入 contracts: +26%
- Assertion-free tests: +3%
- 最終: 39% mutation score
- 改善: 71/71 測試全部通過

## 下一步

1. 選擇目標 Entity
2. 執行自動化檢查腳本
3. 遵循漸進式實施流程
4. 達成目標覆蓋率
5. 更新文件與分享經驗