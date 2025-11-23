# AI 提示範本 (AI Prompts)

本目錄包含預先設計的 AI 提示範本，幫助開發者更有效地與 AI 編碼助手協作。

## 📁 目錄內容

- **add-feature-prompt.md** - 新增功能提示範本
  - 結構化的功能需求描述
  - 包含必要的上下文資訊
  - 明確的預期輸出格式

- **create-use-case-prompt.md** - 建立用例提示範本
  - Use Case 規格定義
  - 輸入輸出參數說明
  - 業務規則和約束條件

## 🎯 為什麼需要提示範本？

### 優點
1. **一致性** - 確保團隊使用相同的溝通方式
2. **完整性** - 不遺漏重要資訊
3. **效率** - 減少來回澄清的時間
4. **品質** - 獲得更準確的程式碼生成

### 使用場景
- 實作新功能
- 重構現有程式碼
- 解決技術問題
- 生成測試案例

## 📝 提示範本結構

### 標準結構
```markdown
# [任務類型]

## 背景資訊
- 專案：[專案名稱]
- 技術棧：[使用的技術]
- 相關模組：[受影響的模組]

## 需求描述
[詳細的需求說明]

## 技術規格
- 輸入：[輸入參數]
- 輸出：[預期輸出]
- 約束：[限制條件]

## 參考資料
- [相關文檔連結]
- [範例程式碼]

## 預期產出
1. [產出項目 1]
2. [產出項目 2]
```

## 🚀 如何使用提示範本

### 步驟 1: 選擇合適的範本
根據任務類型選擇對應的提示範本：
- 新功能 → `add-feature-prompt.md`
- Use Case → `create-use-case-prompt.md`

### 步驟 2: 填寫範本
```markdown
# 範例：使用 add-feature-prompt.md

## 背景資訊
- 專案：AI Todo List
- 技術棧：Java 21, Spring Boot, DDD
- 相關模組：Plan Aggregate

## 需求描述
為 Plan 新增「設定截止日期」功能，允許用戶為整個計畫設定截止日期，
並在接近截止日期時發出提醒。

## 技術規格
- 輸入：PlanId, LocalDate deadline
- 輸出：CqrsOutput
- 約束：截止日期不能早於今天

## 參考資料
- Plan.java
- SetDeadlineUseCase 介面定義

## 預期產出
1. SetDeadlineUseCase 和 SetDeadlineService
2. 相關的單元測試
3. Plan Aggregate 的修改
```

### 步驟 3: 提交給 AI
將填寫完成的提示提交給 AI 編碼助手，獲得程式碼生成。

## 🔧 建立新的提示範本

### 範本設計原則
1. **明確性** - 清楚定義任務目標
2. **結構化** - 使用一致的格式
3. **可重用** - 參數化常變的部分
4. **範例驅動** - 提供具體例子

### 範本模板
```markdown
# [範本名稱] 提示範本

## 用途
[說明這個範本的使用場景]

## 範本
---
# [任務標題]

## 背景資訊
- 專案：{PROJECT_NAME}
- 模組：{MODULE_NAME}
- 相關檔案：{RELATED_FILES}

## 任務說明
{TASK_DESCRIPTION}

## 技術要求
{TECHNICAL_REQUIREMENTS}

## 預期結果
{EXPECTED_OUTPUTS}
---

## 使用範例
[提供一個填寫完成的範例]

## 注意事項
[使用時的注意事項]
```

## 📊 常用提示範本列表

### 開發類
- `add-feature-prompt.md` - 新增功能
- `create-use-case-prompt.md` - 建立用例
- `refactor-code-prompt.md` - 重構程式碼（待新增）
- `fix-bug-prompt.md` - 修復錯誤（待新增）

### 測試類
- `write-unit-test-prompt.md` - 撰寫單元測試（待新增）
- `create-integration-test-prompt.md` - 建立整合測試（待新增）

### 文檔類
- `generate-api-doc-prompt.md` - 生成 API 文檔（待新增）
- `update-readme-prompt.md` - 更新 README（待新增）

## 📚 最佳實踐

### DO ✅
- 提供充分的上下文
- 使用具體的例子
- 明確預期輸出
- 引用相關文檔

### DON'T ❌
- 模糊的需求描述
- 遺漏技術約束
- 假設 AI 知道背景
- 一次要求太多

## 🔗 相關資源
- [AI 行為指南](../../../AI-BEHAVIOR-GUIDE.md)
- [程式碼生成工作流程](../../workflows/code-generation/)
- [範例程式碼](../examples/)