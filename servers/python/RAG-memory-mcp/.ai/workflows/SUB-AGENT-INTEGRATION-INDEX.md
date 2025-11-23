# Sub-agent Integration Index

> 本索引按 Sub-agent 整合程度分類所有 workflows，幫助快速找到適合的工作流程。

## 🤖 完全整合 Workflows (使用 3+ Sub-agents)

### feature-implementation.md
- **用途**: 端到端的功能實現
- **整合 Sub-agents**:
  - Code Generation Agent (產生 Use Case)
  - Test Generation Agent (產生測試)
  - Code Review Agent (品質審查)
- **適用場景**: 新功能開發、完整的 CRUD 操作
- **優勢**: 全程自動化、品質保證

### code-generation/template-based-generation-workflow.md
- **用途**: 基於模板的程式碼生成
- **整合 Sub-agents**:
  - Code Generation Agent (產生程式碼)
  - Test Generation Agent (產生測試)
  - Code Review Agent (審查品質)
- **適用場景**: 重複性元件、標準化程式碼
- **優勢**: 快速生成、符合規範

## 🔧 部分整合 Workflows (使用 1-2 Sub-agents)

### tdd-implementation.md
- **用途**: 測試驅動開發
- **整合 Sub-agents**:
  - Test Generation Agent (先寫測試)
  - Code Generation Agent (實作功能)
- **適用場景**: 需要高測試覆蓋率的功能
- **優勢**: 確保測試先行、減少 bugs

### codebase-improvement.md
- **用途**: 改進既有程式碼
- **整合 Sub-agents**:
  - Code Review Agent (識別問題)
  - Test Generation Agent (補充測試)
- **適用場景**: 重構、性能優化、技術債清理
- **優勢**: 系統性改進、維持品質

## 🌟 專門化 Sub-agent Workflows

### mutation-testing-workflow.md
- **用途**: 提升 mutation testing 覆蓋率
- **整合 Sub-agents**:
  - Mutation Testing Sub-agent
- **適用場景**: 強化測試品質、增加 contracts
- **優勢**: 漸進式改進、保持相容性

## 📋 純流程 Workflows (無 Sub-agent)

### 專案管理類
- **project-initialization.md** - 專案初始設置
- **collaborative-documentation.md** - 協作編寫文檔
- **sync-templates.md** - 同步模板檔案
- **template-usage-workflow.md** - 使用現有模板

### 自動化執行類
- **command-execution-workflow.md** - 執行命令
- **script-automation-workflow.md** - 腳本自動化
- **architecture-generation-workflow.md** - 生成架構文檔

### 品質追蹤類
- **quality-tracking-workflow.md** - 追蹤品質指標

## 📊 選擇指南

### 何時使用「完全整合」Workflows？
✅ 新功能從零開始開發
✅ 需要完整的測試和審查
✅ 團隊要求高品質標準
✅ 想要最大化自動化

### 何時使用「部分整合」Workflows？
✅ 特定任務（只寫測試或只改進程式碼）
✅ 已有部分程式碼，需要補充
✅ TDD 開發模式
✅ 漸進式改進

### 何時使用「純流程」Workflows？
✅ 一次性設置任務
✅ 簡單的檔案操作
✅ 不涉及程式碼生成
✅ 協調性質的工作

## 🚀 快速開始範例

### 實作新的 REST API
```bash
# 使用完全整合的 workflow
"請按照 feature-implementation workflow 實作用戶註冊 API"
```

### 為既有功能補充測試
```bash
# 使用部分整合的 workflow
"請按照 codebase-improvement workflow 為 UserService 補充測試"
```

### 初始化新專案
```bash
# 使用純流程 workflow
"請按照 project-initialization workflow 設置新專案"
```

## 📈 整合統計

| 整合程度 | 數量 | 百分比 | 主要用途 |
|---------|------|--------|---------|
| 🤖 完全整合 | 3 | 21% | 完整功能開發 |
| 🔧 部分整合 | 1 | 7% | 特定任務改進 |
| 🌟 專門化 | 1 | 7% | 特殊需求 |
| 📋 純流程 | 9 | 65% | 輔助任務 |

## 🔗 相關資源

- [SUB-AGENT-SYSTEM.md](../SUB-AGENT-SYSTEM.md) - Sub-agent System 詳細說明
- [workflows/README.md](README.md) - 所有 workflows 清單
- [prompts/](../prompts/) - Sub-agent prompts 目錄