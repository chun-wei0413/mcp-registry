# 測試驗證指南 - 所有 Sub-agents 必讀

**最後更新**: 2024-08-15  
**重要性**: 🔴 CRITICAL - 所有涉及測試的 sub-agents 必須遵守

## 🎯 目的

確保所有 sub-agents 在執行測試時能正確判斷測試結果，避免誤判測試成功或失敗。

## 🔴 核心原則

### 1. 必須檢查完整的測試輸出
- **不要只看部分輸出**
- **不要只看開頭或結尾**
- **必須從頭到尾檢查整個輸出**

### 2. 不要被 WARN 訊息誤導
- WARN 訊息通常是預期的（測試錯誤案例）
- 重點是看最終的 BUILD 結果和測試統計

### 3. 測試失敗必須立即處理
- 不要宣稱測試成功
- 不要繼續下一步
- 立即修正問題

## ✅ 測試成功的明確標誌

測試成功必須同時滿足以下所有條件：

1. **Maven 輸出最後顯示**：
   ```
   [INFO] BUILD SUCCESS
   ```

2. **測試統計顯示**：
   ```
   Tests run: X, Failures: 0, Errors: 0, Skipped: Y
   ```
   - Failures 必須是 0
   - Errors 必須是 0
   - Skipped 可以是任何數字

3. **沒有以下錯誤訊息**：
   - `Failed to load ApplicationContext`
   - `NoSuchBeanDefinitionException`
   - `UnsatisfiedDependencyException`
   - 任何實際的 Exception stack trace（不是 WARN）

## ❌ 測試失敗的標誌

出現以下任何一個都表示測試失敗：

### 1. 明確的失敗訊息
- `[INFO] BUILD FAILURE`
- `Tests run: X, Failures: Y` (Y > 0)
- `Tests run: X, Errors: Y` (Y > 0)

### 2. Spring Context 載入失敗
```
java.lang.IllegalStateException: Failed to load ApplicationContext
```
**原因**: 通常是缺少 Bean 配置  
**解決**: 檢查 `UseCaseConfiguration.java` 並添加缺失的 Bean

### 3. Bean 注入失敗
```
NoSuchBeanDefinitionException: No qualifying bean of type 'xxx' available
```
**原因**: UseCase 或其他依賴未配置  
**解決**: 在配置類中添加 @Bean 方法

### 4. 依賴不滿足
```
UnsatisfiedDependencyException: Error creating bean with name 'xxx'
```
**原因**: 構造函數參數無法注入  
**解決**: 確認所有依賴都有對應的 Bean

### 5. 斷言失敗
```
java.lang.AssertionError: expected:<xxx> but was:<yyy>
```
**原因**: 測試邏輯錯誤或實作問題  
**解決**: 檢查測試案例和實作程式碼

## 📋 標準測試執行流程

### 1. 執行單一測試類
```bash
/opt/homebrew/bin/mvn test -Dtest=TestClassName -q
```

### 2. 執行特定測試方法
```bash
/opt/homebrew/bin/mvn test -Dtest=TestClassName#testMethodName -q
```

### 3. 執行所有測試
```bash
/opt/homebrew/bin/mvn test -q
```

### 4. 檢查輸出
1. 從頭開始讀取輸出
2. 注意任何 Exception 或 Error
3. 檢查測試統計
4. 確認最終 BUILD 狀態

## 🚨 常見誤判案例

### 案例 1: WARN 訊息誤判
```
[WARN] Resolved [org.springframework.web.bind.MethodArgumentNotValidException: ...]
```
**判斷**: ✅ 這不是測試失敗，是測試案例預期觸發的錯誤處理

### 案例 2: 只看到部分輸出
```
[INFO] Started DefineDefinitionOfDoneControllerTest in 1.069 seconds
[WARN] Resolved [...]
[WARN] Resolved [...]
```
**錯誤判斷**: 以為測試成功  
**正確做法**: 繼續往下看，可能有 `Failed to load ApplicationContext`

### 案例 3: 忽略關鍵錯誤
```
Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException
```
**錯誤判斷**: 只看到前面的 INFO 就以為成功  
**正確判斷**: ❌ 測試失敗，需要配置 Bean

## 📝 測試報告範本

### 成功報告
```
測試執行結果: ✅ 成功
- 執行測試數: 13
- 失敗數: 0
- 錯誤數: 0
- 跳過數: 0
- BUILD STATUS: SUCCESS
```

### 失敗報告
```
測試執行結果: ❌ 失敗
- 執行測試數: 13
- 失敗數: 2
- 錯誤數: 1
- 錯誤類型: NoSuchBeanDefinitionException
- 缺失 Bean: DefineDefinitionOfDoneUseCase
- 建議修正: 在 UseCaseConfiguration.java 添加 Bean 配置
```

## 🔧 快速修正指南

| 錯誤類型 | 解決方案 | 檔案位置 |
|---------|---------|---------|
| `NoSuchBeanDefinitionException` | 添加 @Bean 方法 | 配置檔案 (如 `TestInMemoryConfiguration.java`) |
| `Failed to load ApplicationContext` | 檢查 Spring 配置和 Profile | `src/test/resources/application-test.yml` |
| `AssertionError` | 修正測試或實作邏輯 | 對應的測試檔案 |
| `NullPointerException` | 檢查 null 處理 | 實作程式碼 |
| `Profile 相關錯誤` | 確認 test-inmemory 或 test-outbox | 檢查環境變數或 static initializer |

## 🎯 AiScrum 專案特定注意事項

### Profile-Based Testing
- **test-inmemory**: 使用記憶體中的 Event Store
- **test-outbox**: 使用 PostgreSQL Outbox Pattern
- 確保測試支援雙 Profile（不要硬編碼 Profile）

### ezSpec BDD 測試
- 所有 UseCase 測試必須使用 ezSpec
- 測試必須以 `.Execute()` 結尾
- 使用 `@EzScenario` 和 `@EzFeature` 註解

### Event 驗證
- 使用 `getCapturedEvents()` 取得事件
- 在 Given 階段後要 `clearCapturedEvents()`
- 驗證事件數量要用精確值，不是 `isGreaterThan(0)`

## 📌 記住

1. **永遠不要宣稱測試成功，除非看到 BUILD SUCCESS**
2. **WARN 不等於失敗，但要檢查是否有真正的錯誤**
3. **測試失敗時，提供具體的錯誤訊息和解決方案**
4. **修正問題後必須重新執行測試驗證**
5. **注意 Profile 設定，避免 PIT mutation testing 超時**