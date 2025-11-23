# AiScrum AI 協作開發框架

這是 AiScrum 專案的 AI 協作開發框架，幫助開發團隊與 AI 助手高效協作，實作 Domain-Driven Design (DDD)、Clean Architecture 和 CQRS 架構。

## 🎯 框架特色

- **DDD + Clean Architecture**：完整實作領域驅動設計與清潔架構
- **Sub-agent Workflow System**：專門的 AI sub-agents 處理不同類型任務
- **Event Sourcing**：使用 ezddd 框架實作事件溯源
- **Profile-Based Testing**：支援 test-inmemory 和 test-outbox 雙 profile 測試
- **完整的編碼規範**：詳細的 coding standards 和 code review checklists

## 📚 核心文檔

### 必讀文檔
- **[../CLAUDE.md](../CLAUDE.md)** - 專案記憶文檔（主要參考）
- **[INDEX.md](INDEX.md)** - 完整文檔索引
- **[SUB-AGENT-SYSTEM.md](SUB-AGENT-SYSTEM.md)** - Sub-agent 工作流程系統
- **[../dev/ADR-INDEX.md](../dev/ADR-INDEX.md)** - 架構決策記錄索引

### 學習路徑
- **[guides/LEARNING-PATH.md](guides/LEARNING-PATH.md)** - DDD + CA + CQRS 學習路徑
- **[guides/NEW-PROJECT-GUIDE.md](guides/NEW-PROJECT-GUIDE.md)** - AiScrum 專案結構指南
- **[guides/PROFILE-BASED-TESTING-GUIDE.md](guides/PROFILE-BASED-TESTING-GUIDE.md)** - Profile 測試架構
- **[guides/TEST-DATA-PREPARATION-GUIDE.md](guides/TEST-DATA-PREPARATION-GUIDE.md)** - 測試資料準備指南
- 🔴 **[guides/FRAMEWORK-API-INTEGRATION-GUIDE.md](guides/FRAMEWORK-API-INTEGRATION-GUIDE.md)** - ezddd 框架 API 整合完整指南 🆕

### 經驗教訓
- **[lessons/FAILURE-CASES.md](lessons/FAILURE-CASES.md)** - 實際錯誤案例與教訓
- **[lessons/CRITICAL-LESSONS.md](lessons/CRITICAL-LESSONS.md)** - 絕對不能再犯的錯誤
- **[lessons/CODE-REVIEW-LESSONS.md](lessons/CODE-REVIEW-LESSONS.md)** - Code Review 經驗
- **[lessons/FRONTEND-DEBUGGING-LESSONS.md](lessons/FRONTEND-DEBUGGING-LESSONS.md)** - 前端除錯教訓
- **[lessons/JUNIT-SUITE-PROFILE-SWITCHING.md](lessons/JUNIT-SUITE-PROFILE-SWITCHING.md)** - JUnit Profile 切換突破

## 📁 目錄結構

```
.ai/
├── README.md                   # 本文件
├── INDEX.md                    # 完整文檔索引
├── SUB-AGENT-SYSTEM.md       # Sub-agent 系統說明
├── ADR-INDEX.md                # ADR 快速索引
│
├── checklists/                 # 檢查清單
│   ├── AGGREGATE-IDENTIFICATION-CHECKLIST.md
│   ├── AGGREGATE-IMPLEMENTATION-CHECKLIST.md
│   ├── AI-TASK-EXECUTION-CHECKLIST.md
│   ├── CONSISTENCY-CHECK.md
│   ├── TEST-VERIFICATION-GUIDE.md
│   └── VALIDATION-CHECKLIST.md
│
├── config/                     # 配置文檔
│   ├── AI-INIT-COMMANDS.md    # 專案初始化指令
│   └── VERSION-CONTROL.md     # 版本控制（注意：以 .dev/project-config.json 為準）
│
├── guides/                     # 指導文檔
│   ├── LEARNING-PATH.md       # 學習路徑
│   ├── NEW-PROJECT-GUIDE.md   # 新專案指南
│   ├── PROFILE-BASED-TESTING-GUIDE.md
│   └── TEST-DATA-PREPARATION-GUIDE.md
│
├── prompts/                    # AI Prompts
│   ├── aggregate-sub-agent-prompt.md
│   ├── command-sub-agent-prompt.md
│   ├── query-sub-agent-prompt.md
│   ├── reactor-sub-agent-prompt.md
│   ├── controller-code-generation-prompt.md
│   ├── outbox-sub-agent-prompt.md
│   └── mutation-testing-sub-agent-prompt.md
│
├── schemas/                    # 結構定義
│   ├── ai-config-schema.json
│   ├── project-config-schema.json
│   └── workflow-schema.json
│
├── scripts/                    # 自動化腳本
│   ├── check-coding-standards.sh
│   ├── check-jpa-projection-config.sh
│   ├── check-mapper-compliance.sh
│   ├── check-mutation-coverage.sh
│   ├── check-repository-compliance.sh
│   └── check-spec-compliance.sh
│
├── tech-stacks/                # 技術棧文檔
│   ├── java-ca-ezddd-spring/  # Java 後端技術棧
│   │   ├── coding-standards/  # 編碼規範
│   │   ├── examples/          # 範例程式碼
│   │   └── guides/            # 技術指南
│   └── react-typescript/      # React 前端技術棧
│
└── workflows/                  # 工作流程
    ├── project-initialization.md
    ├── feature-implementation.md
    ├── tdd-implementation.md
    ├── mutation-testing-workflow.md
    └── code-generation/
```

## 🚀 快速開始

### 使用 Sub-agent Workflow
```bash
# Command Use Case
請使用 command-sub-agent workflow 實作 [create-product]

# Query Use Case  
請使用 query-sub-agent workflow 實作 [get-product]

# Reactor
請使用 reactor-sub-agent workflow 實作 [notify-sprint-to-select-backlog-item]

# Aggregate
請使用 aggregate-sub-agent workflow 實作 [ProductBacklogItem 狀態機]

# Outbox Pattern
請使用 outbox-sub-agent workflow 為 [Product] 實作 Outbox Pattern

# Mutation Testing
請使用 mutation-testing-sub-agent workflow 為 [Product] 提升 mutation coverage
```

### 執行測試
```bash
# 後端測試
/opt/homebrew/bin/mvn test -q                    # 執行所有測試
/opt/homebrew/bin/mvn test -Dtest=ClassName -q   # 執行特定測試
```

### 自動化檢查
```bash
# Coding Standards 完整性檢查
.ai/scripts/check-coding-standards.sh

# Repository 規範檢查
.ai/scripts/check-repository-compliance.sh

# Spec 完整性檢查
.ai/scripts/check-spec-compliance.sh
```

## ⚠️ 重要原則

1. **版本管理**：所有版本號以 `.dev/project-config.json` 為準（正式來源）
2. **Profile 測試**：支援 test-inmemory 和 test-outbox 雙 profile
3. **測試規範**：Use Case 測試必須使用 ezSpec BDD 風格
4. **Repository 規範**：絕對不要創建自定義 Repository 介面
5. **Reactor 規範**：必須繼承 `Reactor<DomainEventData>`
6. **Outbox 規範**：OutboxMapper 必須是內部類別

## 📖 技術棧版本

- **Spring Boot**: 3.5.3
- **ezddd-core**: 3.0.1
- **uContract**: 2.0.0（注意：`reject()` 已改為 `ignore()`）
- **ezSpec**: 0.0.5
- **Java**: 21

詳細版本資訊請查看 `.dev/project-config.json`

## 💡 提示

- 使用 `INDEX.md` 快速導航到所需文檔
- 查看 `lessons/` 資料夾了解常見錯誤和解決方案
- 執行自動化腳本確保程式碼符合規範
- 遵循 Sub-agent Workflow 提高開發效率

---

**注意**：本框架是 AiScrum 專案的一部分，專門用於實作 Scrum 管理系統的領域模型和業務邏輯。