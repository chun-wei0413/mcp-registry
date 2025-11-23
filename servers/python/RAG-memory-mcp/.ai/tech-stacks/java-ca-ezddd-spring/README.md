# Java Clean Architecture ezddd Spring 技術棧

## 概述

這是一套成熟的企業級 Java 開發技術棧，結合了 Clean Architecture、領域驅動設計（DDD）、事件溯源（Event Sourcing）和命令查詢責任分離（CQRS）模式，使用 ezddd 框架簡化實現。

## 技術組合

- **語言**: Java 21
- **框架**: Spring Boot 2.7.x / 3.x
- **架構**: Clean Architecture (分層架構)
- **DDD框架**: ezddd (Event-Driven DDD Framework)
- **模式**: DDD + Event Sourcing + CQRS
- **測試**: ezSpec (BDD) + JUnit 5
- **持久化**: JPA + PostgreSQL
- **契約**: ucontract (Design by Contract)

## 核心特性

### 1. Clean Architecture
- 業務邏輯獨立於框架
- 依賴方向由外向內
- 各層職責明確分離
- 易於測試和維護

### 2. 領域驅動設計 (DDD)
- Aggregate Root 作為一致性邊界
- Domain Events 記錄所有狀態變更
- Value Objects 表達業務概念
- Repository 模式封裝持久化

### 3. Event Sourcing (ezddd)
- 所有狀態變更通過事件記錄
- 支持完整的審計追踪
- 可重建任意時間點的狀態
- 使用 ezddd 框架簡化實現

### 4. CQRS 模式
- Command 負責狀態修改
- Query 負責數據讀取
- Projection 優化查詢性能
- 讀寫模型分離

## 重要規則

### 1. Repository 方法限制
**所有 concrete Repository 必須只能有 `tw.teddysoft.ezddd.usecase.port.out.repository.Repository` 身上的三個方法**：
- `findById(ID id)`
- `save(T entity)`  
- `delete(T entity)`

不允許添加任何自定義查詢方法！查詢需求請使用 Query Service 或 Projection。

### 2. 測試必須使用 GenericInMemoryRepository
**禁止直接實現 InMemory[Entity]Repository**，所有測試必須使用框架提供的 `GenericInMemoryRepository`。

```java
// ✅ 正確
repository = new GenericInMemoryRepository<>(messageBus);

// ❌ 錯誤
repository = new InMemoryPlanRepository();
```

### 3. Domain Events 必須使用 DateProvider.now()
**所有 Domain Events 的時間戳記必須使用 `DateProvider.now()`**，禁止直接使用 `Instant.now()` 或其他時間 API。

```java
// ✅ 正確
apply(new PlanCreated(
    planId, name, userId,
    UUID.randomUUID(),
    DateProvider.now()  // 必須使用 DateProvider
));

// ❌ 錯誤
Instant.now()       // 不可測試
LocalDateTime.now() // 不可測試
```

### 4. ezSpec 測試失敗時必須尋求人類確認
**測試失敗時絕對不要直接修改 Given-When-Then 內容**，ezSpec 測試代表業務規格。

```java
// 🚨 測試失敗時 AI 必須停止並詢問：
// 1. 分析失敗原因並報告
// 2. 確認是業務規格錯誤還是實現錯誤？
// 3. 等待人類明確指示如何處理
```

## 適用場景

✅ **推薦使用**：
- 複雜業務邏輯的企業應用
- 需要完整審計追踪的系統
- 金融、電商、ERP 等領域
- 微服務架構中的核心服務

❌ **不推薦使用**：
- 簡單的 CRUD 應用
- 快速原型開發
- 小型團隊無 DDD 經驗
- 性能要求極高的即時系統

## 快速開始

### 1. 專案配置

使用 Java 專屬的專案配置模板：

```bash
# 複製 Java 技術棧專屬模板
cp .ai/tech-stacks/java-ca-ezddd-spring/project-config-template.json .dev/project-config.json

# 編輯配置填入你的專案資訊
vim .dev/project-config.json
```

配置包含：
- Maven 座標（groupId、artifactId、version）
- Java 和 Spring Boot 版本
- 所有相關依賴版本（ezddd、ezspec、JUnit 等）
- Maven 插件版本
- 資料庫和功能設定

### 2. 專案結構
```
src/main/java/[package]/
├── [aggregate]/          # 聚合根目錄
│   ├── entity/          # 領域實體
│   ├── usecase/         # 用例層
│   └── adapter/         # 適配器層
├── common/              # 共用組件
└── io/                  # 應用程式入口
```

### 2. 開發流程
1. 設計領域模型（Aggregate、Entity、Value Object）
2. 定義領域事件（Domain Events）
3. 實現用例（Use Cases）
4. 創建適配器（Controllers、Repositories）
5. 編寫測試（BDD style）

### 3. 關鍵檔案
- `coding-guide.md` - 詳細的 AI 編碼指南
- `TEMPLATE-USAGE-GUIDE.md` - 範本選擇決策指南
- `CODE-REVIEW-CHECKLIST.md` - 程式碼審查檢查清單
- `COMMON-MISTAKES-GUIDE.md` - 常見錯誤與解決方案
- `examples/` - 各種設計模式的實際範例代碼（已移除過時的 level1-3 範例）

## 與 EZ-AI 整合

當使用此技術棧時，Workflows 會自動調整：

- **feature-implementation-workflow**: 遵循 DDD tactical patterns
- **refactoring-workflow**: 保持 Aggregate 邊界
- **test-workflow**: 使用 ezSpec BDD 風格

## 必讀資源

1. [UBIQUITOUS-LANGUAGE.md](../../UBIQUITOUS-LANGUAGE.md) - 統一語言和術語定義
2. [coding-guide.md](./coding-guide.md) - AI 專用編碼指南
3. 🔴 **[Framework API Integration Guide](../../guides/FRAMEWORK-API-INTEGRATION-GUIDE.md)** - ezddd 框架 API 整合完整指南 🆕
4. [TEMPLATE-USAGE-GUIDE.md](./TEMPLATE-USAGE-GUIDE.md) - 範本使用指南（何時使用哪個範本）🆕
5. [CODE-REVIEW-CHECKLIST.md](./CODE-REVIEW-CHECKLIST.md) - 程式碼審查檢查清單 🆕
6. [COMMON-MISTAKES-GUIDE.md](./COMMON-MISTAKES-GUIDE.md) - 常見錯誤與解決方案 🆕
7. [project-structure.md](./project-structure.md) - 專案結構與檔案組織
8. [examples/TEMPLATE-INDEX.md](./examples/TEMPLATE-INDEX.md) - 範本索引（快速查找）
9. [examples/aggregate/](./examples/aggregate/README.md) - Aggregate 設計模式與範例
9. [examples/aggregate/Project.java](./examples/aggregate/Project.java) - Entity 設計模式與範例（Project Entity）
10. [examples/usecase/](./examples/usecase/README.md) - Use Case 設計模式與範例
11. [examples/controller/](./examples/controller/README.md) - Controller 設計模式與範例
12. [examples/repository/](./examples/repository/README.md) - Repository 模式與範例
13. [examples/projection/](./examples/projection/README.md) - CQRS Projection 模式與範例
14. [examples/dto/](./examples/dto/README.md) - DTO 設計模式與範例 🆕
15. [examples/persistence/](./examples/persistence/README.md) - JPA Entity 設計模式與範例 🆕
16. [examples/test/](./examples/test/README.md) - 測試模式與範例
17. [examples/projection-example.md](./examples/projection-example.md) - Projection 實作範例
18. [examples/test-example.md](./examples/test-example.md) - 測試範例

## 常用 AI 指令

### 生成組件
```
"請生成 [Name] Aggregate 包含 properties: [列表]"
"請為 [Aggregate] 生成 Repository 介面和 JPA 實現"
"請生成 [Aggregate]Controller 實現 REST API"
"請為 [UseCase] 生成單元測試"
```

### 事件相關
```
"請生成 BootstrapConfig 來註冊這些 events: [列表]"
"請為 [Event] 生成對應的 Reactor"
"請生成 [Aggregate] 的 Projection"
```

### 架構和文檔
```
"請為這個模組生成架構圖"
"請使用 ADR 模板記錄 [決策]"
"請生成這個 API 的 Swagger 文檔"
```

## 常見問題

查看 [FAQ.md](./FAQ.md) 了解常見問題和解決方案。

## 常見陷阱

1. **過度設計**: 不是所有東西都需要 Event Sourcing
2. **Aggregate 太大**: 保持 Aggregate 小而聚焦
3. **忽視性能**: Event Sourcing 可能影響查詢性能
4. **測試不足**: 事件驅動系統更需要完整測試

## 相關資源

- ezddd 框架文檔
- Spring Boot 官方指南
- Domain-Driven Design (Eric Evans)
- Implementing Domain-Driven Design (Vaughn Vernon)