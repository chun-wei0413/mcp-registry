# Java DDD Spring Coding Standards

## 概述

這是 Java DDD Spring 技術棧的編碼標準主文件，統整所有專門領域的編碼規範。

## 專門領域編碼標準

本編碼標準分為以下專門領域，每個領域都有詳細的規範文件：

### 1. [Aggregate Standards](./coding-standards/aggregate-standards.md)
- DDD Aggregate 設計原則
- Event Sourcing 實作規範
- Domain Event 處理
- Value Object 設計

### 2. [UseCase Standards](./coding-standards/usecase-standards.md)
- Command/Query 分離原則
- Input/Output DTO 規範
- Service 實作模式
- Transaction 管理

### 3. [Controller Standards](./coding-standards/controller-standards.md)
- REST API 設計規範
- Request/Response 處理
- 錯誤處理機制
- Integration Test 撰寫

### 4. [Repository Standards](./coding-standards/repository-standards.md)
- Repository 介面限制（只允許三個方法）
- Event Sourcing Repository 實作
- Outbox Pattern 整合
- 查詢模式選擇指引

### 5. [Test Standards](./coding-standards/test-standards.md)
- BDD 測試風格（ezSpec）
- 測試資料準備
- Mock 與 Stub 使用
- Profile-Based Testing

### 6. [Projection Standards](./coding-standards/projection-standards.md)
- CQRS Query Model 設計
- JPA Projection 實作
- 效能優化策略

### 7. [Mapper Standards](./coding-standards/mapper-standards.md)
- Domain 與 Data 物件轉換
- DTO 映射規則
- Outbox Mapper 規範

### 8. [Archive Standards](./coding-standards/archive-standards.md)
- Archive Pattern 實作
- 軟刪除機制
- 歷史資料管理

## 核心設計原則

### 1. Domain-Driven Design (DDD)
- 業務邏輯集中在 Domain 層
- 使用 Ubiquitous Language
- Bounded Context 清晰分離

### 2. Clean Architecture
- 依賴方向由外向內
- Domain 層不依賴任何框架
- 使用 Port & Adapter 模式

### 3. Event Sourcing
- 所有狀態變更透過 Domain Event
- Event 作為 Single Source of Truth
- 支援完整的審計追蹤

### 4. CQRS (Command Query Responsibility Segregation)
- Command 負責寫入操作
- Query 負責讀取操作
- Read Model 與 Write Model 分離

## 技術棧版本

- **Java**: 21
- **Spring Boot**: 3.5.3
- **ezapp-starter**: 1.0.0
- **uContract**: 2.0.0
- **ezSpec**: 0.0.8

## 重要提醒

### ⚠️ Repository 限制規範
- **絕對不要創建自定義 Repository 介面**
- Repository 只能有三個方法：`findById()`, `save()`, `delete()`
- 查詢需求使用 Projection、Inquiry 或 Archive

### ⚠️ Profile-Based Testing
- **絕對不要在 BaseUseCaseTest 加 @ActiveProfiles**
- 所有測試必須支援 test-inmemory 和 test-outbox profiles
- 使用環境變數或配置檔案控制 profile

### ⚠️ Outbox Pattern
- OutboxMapper 必須是內部類別
- 使用 Jakarta persistence（不是 javax.persistence）
- Data 類別的 `domainEventDatas` 和 `streamName` 必須標記 @Transient

## 自動化檢查

專案提供以下自動化檢查腳本：

```bash
# 檢查編碼標準完整性
.ai/scripts/check-coding-standards.sh

# 檢查 Repository 合規性
.ai/scripts/check-repository-compliance.sh

# 檢查 Mapper 設計規範
.ai/scripts/check-mapper-compliance.sh

# 檢查規格實作完整性
.ai/scripts/check-spec-compliance.sh
```

## 相關文件

- [最佳實踐](../best-practices.md)
- [反模式](../anti-patterns.md)
- [編碼指南](../coding-guide.md)
- [程式碼審查清單](../CODE-REVIEW-CHECKLIST.md)
- **[Spring Boot 配置檢查清單](SPRING-BOOT-CONFIGURATION-CHECKLIST.md)** 🔥 - 避免常見配置錯誤