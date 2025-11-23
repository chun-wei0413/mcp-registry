# Generation Templates 目錄

## 📋 概述

本目錄包含完整的程式碼生成模板，用於協助 AI 助手快速生成符合專案規範的完整功能模組。

## 🎯 與 examples 其他目錄的區別

- **examples/[pattern]/** - 展示單一模式的範例程式碼，用於學習和參考
- **generation-templates/** - 提供完整的多檔案生成模板，用於快速產生整個功能模組

## 📁 模板清單

### aggregate-usecase-full.md
完整的 Aggregate 和 Use Case 生成模板，包含：
- Value Object (ID)
- Domain Events (含 TypeMapper)
- Aggregate Root
- Use Case Interface
- Input DTO
- Service Implementation
- 測試案例參考

使用時機：需要創建新的 Aggregate 及其相關 Use Case

### reactor-full.md
完整的 Reactor 生成模板，包含：
- Reactor Interface
- Reactor Implementation
- Event Handler 配置
- 測試案例

使用時機：需要實作 Domain Event 的處理邏輯

### complex-aggregate-spec.md
複雜聚合根的規格定義模板，使用 YAML 格式描述：
- 實體層次結構（含繼承關係）
- 業務不變量和規則
- 領域事件定義
- 命令和業務操作
- 實體間關係

使用時機：在實作前定義複雜的聚合根結構（如包含多個子實體的情況）

### test-case-full.md
完整的測試案例生成模板，包含：
- TestContext 單例模式
- BlockingMessageBus 設置
- GenericInMemoryRepository 配置
- Domain Event 捕獲機制
- ezSpec BDD 測試結構

使用時機：需要為 Use Case 生成完整的測試類別

### local-utils.md
專案必須生成的共用類別模板，包含：
- DateProvider - 時間控制工具類別
- GenericInMemoryRepository - 記憶體儲存庫實作
- 正確的 ezapp-starter import 路徑說明

使用時機：新專案初始化時必須生成的共用元件

## 💡 使用指引

### 1. 選擇合適的模板
根據需求選擇對應的生成模板：
- 定義複雜業務結構 → `complex-aggregate-spec.md`（設計階段）
- 創建新聚合根 → `aggregate-usecase-full.md`（實作階段）
- 創建事件處理器 → `reactor-full.md`（實作階段）

### 2. 替換佔位符
模板中的佔位符說明：
- `[Aggregate]` - 大寫開頭的聚合根名稱（如 Plan, Task）
- `[aggregate]` - 小寫的聚合根名稱（如 plan, task）
- `[AGGREGATE]` - 全大寫的聚合根名稱（如 PLAN, TASK）

### 3. 生成檔案
根據模板生成所有必要的檔案，確保：
- 檔案路徑正確
- Package 名稱一致
- Import 語句完整

### 4. 驗證生成結果
- 編譯通過
- 測試案例通過
- 符合專案規範

## 📝 注意事項

1. **完整性**：這些模板生成的是完整功能模組，包含多個相關檔案
2. **一致性**：確保所有生成的檔案之間的命名和引用保持一致
3. **客製化**：根據實際需求調整模板內容，不要盲目套用
4. **測試優先**：生成程式碼後立即撰寫並執行測試

## 🔗 相關資源

- [Aggregate 範例](../aggregate/)
- [Use Case 範例](../usecase/)
- [測試範例](../test/)
- [編碼規範](../../standards/)