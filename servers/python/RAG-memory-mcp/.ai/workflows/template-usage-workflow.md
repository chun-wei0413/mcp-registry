# Template Usage Workflow

## 目的
指導如何有效使用 templates 目錄中的各種模板來提升專案品質和一致性。

## 模板使用場景

### 1. 架構決策記錄 (ADR)

#### 觸發時機
- 需要做重要技術選擇時
- 改變既有架構時
- 選擇新工具或框架時

#### 使用方式
```bash
# 直接指令
請使用 ADR 模板記錄我們選擇 PostgreSQL 而非 MongoDB 的決定

# 具體範例
請創建 ADR：為什麼我們選擇微服務架構而非單體架構
```

#### AI 執行步驟
1. 複製 `templates/adr-template.md`
2. 創建新文件 `.ai/decisions/ADR-XXX-[決策名稱].md`
3. 根據討論內容填充模板
4. 生成決策編號和日期

### 2. Commit 訊息模板

#### 使用方式
```bash
# 設定 Git 使用模板
git config commit.template .ai/templates/commit-message-template.md

# 或請 AI 幫助
請幫我為這次的修改寫一個符合規範的 commit 訊息
```

#### AI 執行步驟
1. 分析修改內容
2. 判斷 commit 類型 (feat/fix/docs 等)
3. 生成符合模板的訊息
4. 包含相關 issue 編號

### 3. Issue 模板

#### Bug 報告
```bash
# 使用指令
請使用 bug 模板幫我報告登入功能的問題

# AI 會詢問
- 重現步驟是什麼？
- 預期行為是什麼？
- 實際發生了什麼？
- 環境資訊？
```

#### 功能需求
```bash
# 使用指令
請使用 feature 模板提出新增深色模式的需求

# AI 會詢問
- 功能描述
- 使用場景
- 接受條件
- 相關 mockup 或參考
```

### 4. Pull Request 模板

#### 使用方式
```bash
# 創建 PR
請使用 PR 模板為我的認證功能分支創建 Pull Request

# 自動 PR 描述
根據 commit 歷史自動生成符合模板的 PR 描述
```

### 5. 架構文檔模板

#### 使用方式
```bash
# 生成系統架構
使用 system-architecture-template 為我的專案生成架構圖

# 生成領域模型
使用 domain-model-template 生成領域模型圖
```

## 進階用法

### 批量應用模板
```bash
# 初始化專案文檔結構
請為新專案初始化所有必要的文檔模板

AI 會：
1. 創建 .ai/decisions/ 目錄
2. 設定 git commit 模板
3. 創建 .github/ISSUE_TEMPLATE/ 目錄
4. 創建 .github/pull_request_template.md
5. 生成初始架構文檔
```

### 模板驗證
```bash
# 檢查是否符合模板
請檢查我的 commit 訊息是否符合規範

# 驗證 PR 完整性
請確認這個 PR 描述包含所有必要資訊
```

### 自定義模板變數
```bash
# 使用專案特定變數
請使用 ADR 模板，專案名稱是 "EZ-Commerce"，團隊是 "Backend Team"
```

## 與其他工作流程整合

### 與 Code Review 工作流程結合
```bash
進行 code review 時，確保 PR 符合模板要求
```

### 與 Documentation 工作流程結合
```bash
更新文檔時，使用適當的模板保持一致性
```

## 自動化建議

### Git Hooks
```bash
# pre-commit hook
#!/bin/bash
# 檢查 commit 訊息格式
# TODO: 需要實作 validate-commit-message.sh
# 暫時可手動檢查 commit 訊息是否符合規範
```

### CI/CD 整合
```yaml
# GitHub Actions
- name: Validate PR Template
  run: |
    # TODO: 需要實作 check-pr-template.py
    # 暫時由 reviewer 手動檢查
```

### IDE 整合
```json
// VS Code settings.json
{
  "files.associations": {
    "*.adr.md": "markdown"
  },
  "markdown.template.adr": ".ai/templates/adr-template.md"
}
```

## 模板客製化

### 專案特定調整
```bash
# 創建專案特定版本
請基於通用 ADR 模板，創建包含安全考量section的版本

# 添加欄位
請在 bug 模板中加入 "影響範圍" 欄位
```

### 團隊慣例
```bash
# 設定團隊規範
我們團隊的 commit 都需要包含 JIRA 編號，請調整模板
```

## 使用範例

### 完整流程範例
```bash
User: 我剛完成了用戶認證功能，幫我處理相關文檔

AI:
1. 先為您生成 commit 訊息：
   feat(auth): implement JWT-based user authentication
   
   - Add login endpoint with email/password
   - Implement JWT token generation and validation
   - Add refresh token mechanism
   - Include rate limiting for login attempts
   
   Closes #156

2. 創建 ADR-004-authentication-method.md 記錄選擇 JWT 的決策

3. 準備 PR 描述：
   ## Summary
   Implements JWT-based authentication system
   
   ## Changes
   - Login/logout endpoints
   - Token refresh mechanism
   - Rate limiting
   - Unit tests
   
   ## Testing
   - [x] Unit tests pass
   - [x] Manual testing completed
   - [x] Security review done
```

## 注意事項

1. **保持模板更新**：根據專案演進調整模板
2. **不要過度使用**：模板是輔助工具，不是枷鎖
3. **團隊共識**：確保團隊都了解模板使用方式
4. **靈活調整**：根據具體情況修改模板內容