# AI Task 執行檢查清單

## ⚠️ 強制執行順序 - 防止遺漏步驟

### 🛑 STEP 0: STOP AND CHECK
```markdown
問題：我是否直接開始寫程式碼了？
✅ 正確：先找 task 檔案
❌ 錯誤：直接寫程式碼
```

### 📋 STEP 1: FIND TASK FILE
```bash
# 必須執行
Glob: **/task-{name}*.json

# 如果找到
→ 必須讀取 task 檔案
→ 繼續 STEP 2

# 如果沒找到
→ 詢問使用者是否有 task 檔案
→ 或確認可以直接執行
```

### 📖 STEP 2: READ AND ANALYZE
```markdown
從 task 檔案提取：
□ description - 完整理解要求
□ spec - 找到並讀取 spec 檔案
□ pipeline.steps - 記錄必須執行的步驟
□ postChecks - 記錄必須執行的檢查
```

### 📝 STEP 3: CREATE TODO LIST
```markdown
使用 TodoWrite 建立計畫，必須包含：
□ 讀取 spec 檔案
□ 執行 workflow 所有步驟
  - codeGeneration
  - testGeneration  
  - codeReview
□ 執行所有 postChecks
□ 產生報告（如果需要）
□ 更新 task results
```

### 🚀 STEP 4: EXECUTE WITH TRACKING
```markdown
執行規則：
1. 按照 TodoWrite 順序執行
2. 每完成一項就標記 completed
3. 不可跳過任何項目
4. 如果卡住，標記 blocked 並說明
```

### ✅ STEP 5: VERIFY COMPLETION
```markdown
完成檢查清單：
□ 所有 TodoWrite 項目都是 completed？
□ 所有 postChecks 都執行了？
□ 報告產生了（如果需要）？
□ task results 更新了？
□ status 更新為 done？
```

### 🔴 STEP 6: UPDATE TASK FILE (強制步驟)
```markdown
必須更新 task 檔案：
1. 將 status 從 "todo" 改為 "done"
2. 在 results 陣列新增執行記錄：
   {
     "completionDateTime": "2024-MM-DDTHH:MM:SS+08:00",  // ISO 8601 格式
     "totalImplementationTime": "XX minutes",              // 實際執行時間
     "status": "done",
     "summary": "執行摘要",
     "outputFiles": ["產生的檔案列表"],
     "testResults": "測試結果",
     "postChecksResults": { "審查結果" },
     "changes": ["具體變更說明列表"]
   }
3. 記錄已修正的問題和待處理的問題
4. 確認結果已寫入檔案：grep "results" task-xxx.json
```

## 🔴 常見錯誤模式

### 錯誤 1：看到關鍵字就行動
```markdown
❌ 看到 "controller" → 直接寫 controller
✅ 看到 "controller" → 先找 task-*controller*.json
```

### 錯誤 2：忽略 sub-agent workflow
```markdown
❌ 只產生程式碼
✅ 執行完整 workflow：
   1. Code Generation (讀取 prompt)
   2. Test Generation (讀取 prompt)
   3. Code Review (讀取 prompt)
```

### 錯誤 3：忘記更新 task
```markdown
❌ 完成工作就結束
✅ 必須更新：
   - results 陣列
   - status 欄位
   - timestamp
```

## 📊 執行範例

### 正確執行流程：
```markdown
使用者：實作 CreateProductController

AI：
1. 搜尋 task 檔案
   Glob: **/task-*create-product-controller*.json
   找到：.dev/tasks/feature/product/adapter/task-create-product-controller.json

2. 讀取 task 檔案
   要求：controller sub-agent workflow
   需要：codeGeneration, testGeneration, codeReview

3. 建立 TodoWrite
   □ 讀取 spec 檔案 (.dev/specs/product/create-product.json)
   □ 產生 controller (controller-code-generation-prompt.md)
   □ 產生測試 (controller-test-generation-prompt.md)
   □ 執行 code review (controller-code-review-prompt.md)
   □ 執行 postChecks
   □ 更新 task results

4. 依序執行並標記完成狀態
5. 驗證所有項目都是 completed
6. 更新 task 檔案的 status 和 results
```

## 🎯 關鍵提醒

1. **永遠先找 task 檔案**
2. **永遠使用 TodoWrite**
3. **永遠執行所有步驟**
4. **永遠更新 task results**

## 💡 自我檢查問題

在開始任何工作前，問自己：
1. 我找過 task 檔案了嗎？
2. 我建立 TodoWrite 了嗎？
3. 我知道所有必須執行的步驟嗎？
4. 我準備好更新 task results 了嗎？

---

**記住**：寧可多花時間確認，也不要遺漏步驟後補救！