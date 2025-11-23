# AiScrum 專案文檔索引

> 這是 .ai 目錄的完整文檔索引，幫助 AI 助手快速定位所需文檔。

## 📚 核心配置文檔

### 專案記憶與協作
- [CLAUDE.md](../CLAUDE.md) - 專案記憶文檔（主要參考）
- [SUB-AGENT-SYSTEM.md](SUB-AGENT-SYSTEM.md) - Sub-agent 工作流程系統 🆕
  - 包含 Mutation Testing Enhancement Agent (PIT + uContract)
  - 自動化腳本: [check-mutation-coverage.sh](scripts/check-mutation-coverage.sh)

### AI 指令集
- [AI-INIT-COMMANDS.md](config/AI-INIT-COMMANDS.md) - 專案初始化指令集

### 版本與一致性管理
- [CONSISTENCY-CHECK.md](checklists/CONSISTENCY-CHECK.md) - 一致性檢查指南

## 🔧 工作流程文檔

### 基礎工作流程
- [workflows/project-initialization.md](workflows/project-initialization.md) - 專案初始化
- [workflows/feature-implementation.md](workflows/feature-implementation.md) - 功能實現
- [workflows/tdd-implementation.md](workflows/tdd-implementation.md) - TDD 開發
- [workflows/codebase-improvement.md](workflows/codebase-improvement.md) - 程式碼改進

### 進階工作流程
- [workflows/architecture-generation-workflow.md](workflows/architecture-generation-workflow.md) - 架構生成
- [workflows/collaborative-documentation.md](workflows/collaborative-documentation.md) - 協作文檔編寫
- [workflows/command-execution-workflow.md](workflows/command-execution-workflow.md) - 指令執行
- [workflows/quality-tracking-workflow.md](workflows/quality-tracking-workflow.md) - 品質追蹤
- [workflows/script-automation-workflow.md](workflows/script-automation-workflow.md) - 腳本自動化
- [workflows/sync-templates.md](workflows/sync-templates.md) - 模板同步
- [workflows/template-usage-workflow.md](workflows/template-usage-workflow.md) - 模板使用
- [workflows/code-generation/template-based-generation-workflow.md](workflows/code-generation/template-based-generation-workflow.md) - 基於模板的代碼生成
- [workflows/mutation-testing-workflow.md](workflows/mutation-testing-workflow.md) - Mutation Testing 增強流程 🆕

## 🔍 品質管理文檔

### Code Review 與驗證
- [TEST-VERIFICATION-GUIDE.md](checklists/TEST-VERIFICATION-GUIDE.md) - 🔴 測試結果驗證指南（所有 sub-agents 必讀）
- [AGGREGATE-IDENTIFICATION-CHECKLIST.md](checklists/AGGREGATE-IDENTIFICATION-CHECKLIST.md) - Aggregate 識別檢查清單
- [VALIDATION-CHECKLIST.md](checklists/VALIDATION-CHECKLIST.md) - 程式碼品質驗證清單
- [FAILURE-CASES.md](../dev/lessons/FAILURE-CASES.md) - AI 實際產生的錯誤案例與教訓
- [CODE-REVIEW-LESSONS.md](../dev/lessons/CODE-REVIEW-LESSONS.md) - Code Review 經驗教訓
- [CRITICAL-LESSONS.md](../dev/lessons/CRITICAL-LESSONS.md) - 關鍵教訓 - 絕對不能再犯的錯誤
- [JUNIT-SUITE-PROFILE-SWITCHING.md](../dev/lessons/JUNIT-SUITE-PROFILE-SWITCHING.md) - JUnit Platform Suite Profile 動態切換突破

## 🏗️ 技術棧文檔

### 後端：Java Clean Architecture + DDD + Spring
- [tech-stacks/java-ca-ezddd-spring/README.md](tech-stacks/java-ca-ezddd-spring/README.md) - 技術棧概述
- [tech-stacks/java-ca-ezddd-spring/quick-setup.md](tech-stacks/java-ca-ezddd-spring/quick-setup.md) - 快速設置
- [tech-stacks/java-ca-ezddd-spring/coding-guide.md](tech-stacks/java-ca-ezddd-spring/coding-guide.md) - 編碼指南
- [tech-stacks/java-ca-ezddd-spring/coding-standards/](tech-stacks/java-ca-ezddd-spring/coding-standards/) - 編碼標準目錄
  - [README.md](tech-stacks/java-ca-ezddd-spring/coding-standards/README.md) - 規範總覽
  - [aggregate-standards.md](tech-stacks/java-ca-ezddd-spring/coding-standards/aggregate-standards.md) - Aggregate 規範
  - [repository-standards.md](tech-stacks/java-ca-ezddd-spring/coding-standards/repository-standards.md) - Repository 規範
  - [usecase-standards.md](tech-stacks/java-ca-ezddd-spring/coding-standards/usecase-standards.md) - Use Case 規範
  - [archive-standards.md](tech-stacks/java-ca-ezddd-spring/coding-standards/archive-standards.md) - Archive Pattern 規範 🆕
- [tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md](tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md) - 程式碼審查檢查清單
- [tech-stacks/java-ca-ezddd-spring/best-practices.md](tech-stacks/java-ca-ezddd-spring/best-practices.md) - 最佳實踐
- [tech-stacks/java-ca-ezddd-spring/FAQ.md](tech-stacks/java-ca-ezddd-spring/FAQ.md) - 常見問題

### 範例與模板
- [tech-stacks/java-ca-ezddd-spring/examples/TEMPLATE-INDEX.md](tech-stacks/java-ca-ezddd-spring/examples/TEMPLATE-INDEX.md) - 範本索引
- [tech-stacks/java-ca-ezddd-spring/TEMPLATE-USAGE-GUIDE.md](tech-stacks/java-ca-ezddd-spring/TEMPLATE-USAGE-GUIDE.md) - 範本使用指南 🆕
- [tech-stacks/java-ca-ezddd-spring/TEMPLATE-SYNC-GUIDE.md](tech-stacks/java-ca-ezddd-spring/TEMPLATE-SYNC-GUIDE.md) - 範本同步規範
- [tech-stacks/java-ca-ezddd-spring/examples/generation-templates/](tech-stacks/java-ca-ezddd-spring/examples/generation-templates/) - 代碼生成模板
- [tech-stacks/java-ca-ezddd-spring/examples/reference/](tech-stacks/java-ca-ezddd-spring/examples/reference/) - 參考實現
- [tech-stacks/java-ca-ezddd-spring/examples/reference/reactor-pattern-guide.md](tech-stacks/java-ca-ezddd-spring/examples/reference/reactor-pattern-guide.md) - Reactor 模式指南 🆕
- [tech-stacks/java-ca-ezddd-spring/examples/generation-templates/reactor-full.md](tech-stacks/java-ca-ezddd-spring/examples/generation-templates/reactor-full.md) - Reactor 完整範本 🆕

## 📖 指南文檔

### 開發指南
- [NEW-PROJECT-GUIDE.md](guides/NEW-PROJECT-GUIDE.md) - AiScrum 專案結構與新專案設置指南
- [LEARNING-PATH.md](guides/LEARNING-PATH.md) - DDD + CA + CQRS 學習路徑
- [PROFILE-BASED-TESTING-GUIDE.md](guides/PROFILE-BASED-TESTING-GUIDE.md) - Profile-Based Testing 架構指南
- [TEST-DATA-PREPARATION-GUIDE.md](guides/TEST-DATA-PREPARATION-GUIDE.md) - 測試資料準備指南
- [tech-stacks/java-ca-ezddd-spring/guides/DEVELOPMENT-TOOLS-GUIDE.md](tech-stacks/java-ca-ezddd-spring/guides/DEVELOPMENT-TOOLS-GUIDE.md) - 開發工具指南
- [tech-stacks/java-ca-ezddd-spring/guides/DATABASE-MIGRATION-GUIDE.md](tech-stacks/java-ca-ezddd-spring/guides/DATABASE-MIGRATION-GUIDE.md) - 資料庫遷移指南

### 品質保證
- [tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md](tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md) - 程式碼審查檢查清單 🆕
- [tech-stacks/java-ca-ezddd-spring/COMMON-MISTAKES-GUIDE.md](tech-stacks/java-ca-ezddd-spring/COMMON-MISTAKES-GUIDE.md) - 常見錯誤與解決方案 🆕
- 🔴 **[guides/FRAMEWORK-API-INTEGRATION-GUIDE.md](guides/FRAMEWORK-API-INTEGRATION-GUIDE.md)** - ezddd 框架 API 整合完整指南 🆕
  - PgMessageDbClient 正確創建方式
  - OutboxMapper 內部類別規範（ADR-019）
  - Jakarta persistence 使用規則
  - @Transient 註解強制要求
  - 自動檢查腳本：`scripts/check-framework-api-compliance.sh`

## 🔗 文檔依賴關係

### 初始化流程依賴
1. `config/AI-INIT-COMMANDS.md` → 參考 `.dev/project-config.json` 獲取版本號（正式來源）
2. `workflows/project-initialization.md` → 執行 `config/AI-INIT-COMMANDS.md` 中的指令
3. 所有代碼生成 → 參考 `generation-templates/` 目錄

### 一致性檢查依賴
1. `checklists/CONSISTENCY-CHECK.md` → 使用 `.dev/project-config.json` 作為版本基準，提供檢查規則和執行指令
2. 版本更新時 → 先更新 `.dev/project-config.json`，再執行一致性檢查

### 開發流程依賴
1. 功能開發 → 使用 `feature-implementation.md` 工作流程
2. TDD 開發 → 使用 `workflows/tdd-implementation.md`
3. 問題排查 → 參考 `reference/DEPENDENCY-TROUBLESHOOTING.md`

## 🚀 快速導航

### 常用指令
- 初始化專案：查看 [AI-INIT-COMMANDS.md](config/AI-INIT-COMMANDS.md)
- 檢查一致性：查看 [CONSISTENCY-CHECK.md](checklists/CONSISTENCY-CHECK.md)
- TDD 開發：查看 [tdd-implementation.md](workflows/tdd-implementation.md)

### 重要原則
1. **版本管理**：所有版本號以 `.dev/project-config.json` 為準（正式來源）
2. **配置格式**：Spring Boot 使用 application.yml 配置
3. **測試規範**：Use Case 測試必須使用 ezSpec BDD 風格
4. **文檔優先級**：CLAUDE.md > 技術棧文檔 > 其他文檔
5. **Profile 測試**：支援 test-inmemory 和 test-outbox 雙 profile

---

💡 **提示**：使用 Ctrl+F (或 Cmd+F) 快速搜尋所需文檔。