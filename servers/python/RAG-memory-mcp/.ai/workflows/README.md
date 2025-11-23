# AI Workflows

此目錄包含 AI 編碼助手的標準化工作流程定義，與 Sub-agent System 協同運作。

## 🔄 與 Sub-agent System 的關係

### 架構層次
- **Workflows（流程層）**：定義「做什麼」和「何時做」- 端到端的開發流程
- **Sub-agents（執行層）**：負責「如何做」和「品質保證」- 專門的程式碼生成

### 協作模式
```
Workflow 定義流程
    ↓
在關鍵步驟呼叫 Sub-agents
    ↓
Sub-agents 執行專門任務
    ↓
Workflow 整合結果並繼續
```

## 📁 工作流程清單

### 👍 Sub-agent 整合狀態圖例
- 🤖 = 完全整合（使用 3+ Sub-agents）
- 🔧 = 部分整合（使用 1-2 Sub-agents）  
- 📋 = 純流程（無 Sub-agent）
- 🌟 = 專門化 Sub-agent

### 核心開發流程
- 🤖 [feature-implementation.md](feature-implementation.md) - 功能實現流程
  - 整合：Code Generation, Test Generation, Code Review Agents
- 🤖 [tdd-implementation.md](tdd-implementation.md) - TDD 開發流程
  - 整合：Test Generation, Code Generation, Code Review Agents
- 🤖 [code-generation/template-based-generation-workflow.md](code-generation/template-based-generation-workflow.md) - 範本程式碼生成
  - 整合：Code Generation, Test Generation, Code Review Agents

### 品質與維護流程
- 🔧 [codebase-improvement.md](codebase-improvement.md) - 程式碼改進流程
  - 整合：Code Review, Test Generation Agents
- 🌟 [mutation-testing-workflow.md](mutation-testing-workflow.md) - Mutation Testing 增強流程
  - 整合：Mutation Testing Sub-agent
- 📋 [quality-tracking-workflow.md](quality-tracking-workflow.md) - 品質追蹤流程

### 專案管理流程
- 📋 [project-initialization.md](project-initialization.md) - 專案初始化
- 📋 [collaborative-documentation.md](collaborative-documentation.md) - 協作文件編寫
- 📋 [sync-templates.md](sync-templates.md) - 同步範本工作流程
- 📋 [template-usage-workflow.md](template-usage-workflow.md) - 範本使用流程

### 自動化流程
- 📋 [command-execution-workflow.md](command-execution-workflow.md) - 命令執行流程
- 📋 [script-automation-workflow.md](script-automation-workflow.md) - 腳本自動化流程
- 📋 [architecture-generation-workflow.md](architecture-generation-workflow.md) - 架構生成流程

## 🎯 工作流程用途

工作流程文件定義了執行特定任務的標準化步驟，幫助：
- AI 編碼助手遵循一致的流程
- 團隊成員理解標準作業程序
- 減少重複說明相同步驟
- 確保重要步驟不被遺漏
- **與 Sub-agents 協同提高程式碼品質**

## 📝 使用方式

### 選擇使用時機

#### 使用 Workflows 當：
- 需要完整的端到端流程指導
- 涉及多個階段的複雜任務
- 需要人機協作的決策點
- 專案初始化或架構設計

#### 直接使用 Sub-agents 當：
- 單純的程式碼生成任務
- 已有明確的 spec 或需求
- 需要快速產生特定類型程式碼

### 範例指令

```bash
# 使用 Workflow（完整流程）
"請按照 feature-implementation workflow 實現用戶註冊功能"

# 直接使用 Sub-agent（快速生成）
"請使用 sub-agent workflow 實作 create-product use case"
```

## 🔧 工作流程結構

每個工作流程包含：
- **概述** - 簡要說明目的
- **目標** - 預期達成的結果
- **前置條件** - 執行前需要準備的事項
- **步驟** - 詳細的執行步驟和命令
- **執行時機** - 建議的執行頻率或觸發條件
- **成功指標** - 如何判斷流程成功完成
- **異常處理** - 常見問題和解決方案

## 🚀 建立新工作流程

新增工作流程時，請使用以下模板：

```markdown
# Workflow: [工作流程名稱]

## 📋 概述
[簡要說明此工作流程的用途]

## 🎯 目標
- [目標 1]
- [目標 2]

## 📝 前置條件
- [ ] [條件 1]
- [ ] [條件 2]

## 🔄 工作流程步驟
[詳細步驟...]

## ⏱️ 執行時機
[建議執行的時機或頻率]

## 📊 成功指標
- [ ] [指標 1]
- [ ] [指標 2]

## 🚨 異常處理
[常見問題和解決方案]

## 📚 相關資源
- [相關文件連結]
```

## 📦 Sub-agent Integration 統計

### 整合率
- **總計**：14 個 workflows（含 code-generation 子目錄）
- **完全整合** 🤖：3 個 (21%)
- **部分整合** 🔧：1 個 (7%)
- **專門化** 🌟：1 個 (7%)
- **純流程** 📋：9 個 (65%)

### 整合策略
- **需要產生程式碼** → 整合 Sub-agents
- **純操作性任務** → 不需要 Sub-agents
- **協調性質** → 不需要 Sub-agents

## 🔗 相關資源
- [SUB-AGENT-SYSTEM.md](../SUB-AGENT-SYSTEM.md) - Sub-agent System 說明
- [Sub-agent Prompts](../prompts/) - 各種 Sub-agent 的 prompts
- [範本同步指南](../tech-stacks/java-ca-ezddd-spring/TEMPLATE-SYNC-GUIDE.md)