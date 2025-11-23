# Examples 目錄

本目錄包含各種程式碼範例和設計模式，供 AI 編碼助手參考。

## 🔥 重要：Verified Templates (Single Source of Truth)

### [pom/](./pom/)
**經過驗證的 Maven POM 模板**：
- [pom.xml](./pom/pom.xml) - 包含正確的 EZDDD 依賴（ezapp-starter）
- ⚠️ **重要**：使用佔位符系統，AI 會自動從 `.dev/project-config.json` 替換版本號
- 📖 **佔位符說明**：[VERSION-PLACEHOLDER-GUIDE.md](../../guides/VERSION-PLACEHOLDER-GUIDE.md)

### [spring/](./spring/)
**經過驗證的 Spring Boot 配置模板**：
- [application.properties](./spring/application.properties) - 主配置檔案
- [application-inmemory.properties](./spring/application-inmemory.properties) - InMemory profile（純 Java，無資料庫）
- [application-outbox.properties](./spring/application-outbox.properties) - Outbox profile（PostgreSQL）
- [AiScrumApp.java](./spring/AiScrumApp.java) - Spring Boot 主程式範本
- [UseCaseConfiguration.java](./spring/UseCaseConfiguration.java) - Use Case 層 Bean 配置範本
- [InMemoryRepositoryConfig.java](./spring/InMemoryRepositoryConfig.java) - InMemory Repository 配置範本

### [generation-templates/](./generation-templates/)
**Java 類別模板**：
- [local-utils.md](./generation-templates/local-utils.md) - 共用工具類別（DateProvider, GenericInMemoryRepository 等）

## 目錄結構

### [contract/](./contract/)
Contract（契約）設計的完整指南和範例：
- [CONTRACT-GUIDE.md](./contract/CONTRACT-GUIDE.md) - Contract 設計指南
- [aggregate-contract-example.md](./contract/aggregate-contract-example.md) - Aggregate Contract 範例
- [usecase-contract-example.md](./contract/usecase-contract-example.md) - Use Case Contract 範例
- [value-object-contract-example.md](./contract/value-object-contract-example.md) - Value Object Contract 範例

## 使用方式

1. **學習 Contract 基礎**：先閱讀 CONTRACT-GUIDE.md 了解基本概念
2. **參考具體範例**：根據需要查看不同類型的 Contract 範例
3. **應用到實際程式碼**：在編寫程式碼時遵循這些模式

## Contract 快速參考

### Preconditions（前置條件）
```java
requireNotNull("Parameter name", parameter);
require("Condition description", () -> condition);
```

### Postconditions（後置條件）
```java
ensure("What should be true", () -> condition);
```

### Invariants（不變條件）
```java
invariant("What must always be true", () -> condition);
invariantNotNull("Field name", field);
```

## 重要提醒

- Contract 是程式正確性的保證
- 好的 Contract 能防止 bug、提升可維護性
- 避免過度設計，只檢查必要的條件
- 提供清晰的錯誤訊息幫助除錯