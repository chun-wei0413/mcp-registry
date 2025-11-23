# Examples 目錄索引 - 整合版

## 📋 概述

本目錄採用**方案一**的設計，將設計模式說明（patterns）和實作範例（examples）整合在一起，讓 AI 和開發者只需要查看一個地方就能獲得完整資訊。

## 🎯 設計理念

- **單一資訊來源**：避免在 patterns 和 examples 之間來回查找
- **理論與實踐結合**：每個目錄都包含概念說明和實際程式碼
- **減少維護成本**：避免重複內容，確保一致性
- **提升使用效率**：AI 可以快速找到所需的完整資訊

## 📁 目錄結構

```
examples/
├── INDEX.md              # 本文件
├── aggregate/           # Aggregate Root 模式與範例
│   ├── README.md       # 概念說明 + 實作要點
│   ├── Plan.java       # 完整的 Aggregate 範例
│   ├── PlanEvents.java # Domain Events 定義
│   └── PlanId.java     # Value Object 範例
│
├── usecase/            # Use Case 模式與範例
│   ├── README.md       # Clean Architecture 概念 + 實作
│   ├── CreateTaskUseCase.java    # Use Case 介面
│   └── CreateTaskService.java    # Use Case 實作
│
├── controller/         # REST Controller 模式與範例
│   ├── README.md       # Spring Boot Controller 說明
│   └── CreateTaskController.java # Controller 實作
│
├── repository/         # Repository 模式與範例
│   ├── README.md       # Repository Pattern 說明
│   └── GenericInMemoryRepository.java # 測試用實作
│
├── projection/         # CQRS Projection 模式與範例
│   ├── README.md       # 查詢優化策略
│   └── JpaTasksByDateProjection.java # Projection 實作
│
├── mapper/            # Mapper 模式與範例
│   ├── README.md       # 層級間物件轉換說明
│   ├── PlanMapper.java # Aggregate Mapper 實作
│   └── TaskMapper.java # Entity Mapper 實作
│
├── test/              # 測試模式與範例
│   ├── README.md       # ezSpec BDD 測試框架說明
│   └── CreateTaskUseCaseTest.java # 測試範例
│
├── generation-templates/  # 完整生成模板 🆕
│   ├── README.md       # 模板使用說明
│   ├── complex-aggregate-spec.md    # 複雜聚合根規格定義模板
│   ├── aggregate-usecase-full.md    # Aggregate + Use Case 完整模板
│   └── reactor-full.md              # Reactor 完整模板
│
├── inquiry-archive/    # Inquiry 與 Archive 模式與範例 🆕
│   ├── README.md       # 模式概念說明與選擇指南
│   ├── USAGE-GUIDE.md  # 詳細使用指南與最佳實踐
│   ├── FindPbisBySprintIdInquiry.java      # Inquiry 介面範例
│   ├── JpaFindPbisBySprintIdInquiry.java   # Inquiry JPA 實作
│   ├── ProductArchive.java                 # Archive 介面範例
│   └── JpaProductArchive.java              # Archive JPA 實作
│
├── outbox/             # Outbox Repository 模式與範例 🆕
│   ├── README.md       # Outbox 模式實作指南
│   ├── OUTBOX-TEST-CONFIGURATION.md       # Outbox 測試配置指南 🆕
│   ├── PlanData.java                       # Data 類別範例
│   ├── PlanMapper.java                     # Mapper 實作範例
│   └── RepositoryConfig.java               # Repository 配置範例
│
└── reference/          # 技術參考文檔 🆕
    ├── README.md       # 參考文檔說明
    ├── ezddd-import-mapping.md   # ezddd 框架 import 對照表
    ├── reactor-pattern-guide.md  # Reactor 模式完整指南
    └── ezspec-test-template.md   # ezSpec 測試框架模板
```

## 🚀 使用指南

### 對於 AI 助手

1. **產生新的 Aggregate**
   - 參考 `aggregate/README.md` 了解設計原則
   - 使用 `aggregate/Plan.java` 作為範例模板
   - 注意 Event Sourcing 的實作細節

2. **實作 Use Case**
   - 查看 `usecase/README.md` 理解 Clean Architecture
   - 參考 `usecase/CreateTaskService.java` 的實作模式
   - 遵循單一職責原則

3. **撰寫測試**
   - 閱讀 `test/README.md` 了解 ezSpec 框架
   - 使用 `test/CreateTaskUseCaseTest.java` 作為範例
   - 確保包含正常和異常情境

4. **快速生成完整模組** 🆕
   - 使用 `generation-templates/aggregate-usecase-full.md` 生成完整的 Aggregate + Use Case
   - 使用 `generation-templates/reactor-full.md` 生成 Event Handler
   - 這些模板包含所有必要檔案的完整程式碼

5. **實作進階查詢模式** 🆕
   - 查看 `inquiry-archive/README.md` 了解 Inquiry 和 Archive 模式
   - 參考 `inquiry-archive/USAGE-GUIDE.md` 的實作步驟
   - 使用 Inquiry 處理 Reactor 中的跨聚合查詢
   - 使用 Archive 實作軟刪除和審計追蹤

6. **實作 Outbox Repository 模式** 🆕
   - 查看 `outbox/README.md` 了解 Outbox 模式概念
   - 使用 Outbox 確保事件發布的可靠性
   - 整合 ezapp-starter 框架處理分散式事件
   - 確保領域事件與資料庫交易的一致性

### 對於開發者

1. **學習路徑**
   ```
   aggregate/ → usecase/ → controller/ → test/
   ```

2. **快速上手**
   - 每個目錄的 README.md 都包含完整說明
   - 範例程式碼可以直接複製修改
   - 注意調整 package 名稱和業務邏輯

## 💡 最佳實踐

### 1. 查找順序
- 先看目標目錄的 README.md 理解概念
- 再看具體的 .java 檔案了解實作
- 最後參考測試了解使用方式

### 2. 程式碼複用
- 範例程式碼都來自實際專案（ai-todo-list）
- 已經過驗證和測試
- 可以作為可靠的參考

### 3. 保持更新
- 當框架升級時同步更新範例
- 發現問題及時修正
- 持續優化說明文檔

## 🔄 從舊結構遷移

如果您之前使用分離的 patterns 和 examples 目錄：

1. **patterns/** 的內容已整合到各子目錄的 README.md
2. **examples/** 的程式碼保留在對應的子目錄中
3. 不需要再查看舊的 patterns 目錄

## ⚠️ 重要提醒

1. **這是唯一的參考來源**
   - 不要再查看舊的 patterns 目錄
   - 所有資訊都在這個 examples 目錄中

2. **保持一致性**
   - 新增範例時同時更新 README.md
   - 確保說明和程式碼同步

3. **實用優先**
   - 範例應該可以直接運行
   - 包含足夠的註釋說明
   - 涵蓋常見的使用場景

## 📚 相關文檔

- [專案結構說明](../project-structure.md)
- [編碼指南](../coding-guide.md)
- [Maven 依賴](../examples/reference/maven-dependencies.md)
- [共用程式](../examples/generation-templates/local-utils.md)

---

💡 **提示**: 這個整合的結構讓您可以在一個地方找到所有需要的資訊，大大提升開發效率！