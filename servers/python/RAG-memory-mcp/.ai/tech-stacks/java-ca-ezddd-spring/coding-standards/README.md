# Java CA-ezddd-Spring 編碼規範

本目錄包含所有編碼規範文件，每個文件專注於特定領域的標準和最佳實踐。

## 📚 規範文件索引

### 核心領域規範
- **[aggregate-standards.md](./aggregate-standards.md)** - Aggregate、Entity、Value Object 和 Domain Event 規範
  - Aggregate Root 設計原則
  - Domain Event 結構與處理
  - Value Object 不可變性設計
  - 軟刪除 (Soft Delete) 實作要求
  - 📋 包含完整程式碼模板

- **[repository-standards.md](./repository-standards.md)** - Repository 模式規範
  - Generic Repository 使用原則
  - 禁止自定義 Repository 介面
  - Event Sourcing 與 State-based 實作
  - 軟刪除過濾機制

- **[usecase-standards.md](./usecase-standards.md)** - Use Case 層規範
  - Command vs Query 分離原則
  - Input/Output 設計模式
  - Service 實作與依賴注入
  - 事務管理與錯誤處理
  - 📋 包含 Command/Query 完整模板

### 資料存取規範
- **[projection-standards.md](./projection-standards.md)** - Projection 查詢模式規範
  - Read Model 設計原則
  - JPA Projection 實作
  - 複雜查詢處理

- **[archive-standards.md](./archive-standards.md)** - Archive 模式規範
  - Query Model CRUD 操作
  - 跨 Bounded Context 參考資料
  - 歷史資料管理

- **[mapper-standards.md](./mapper-standards.md)** - Mapper 設計規範
  - Domain 與 Data 物件轉換
  - Outbox Pattern 整合
  - 靜態方法設計原則

### API 與控制層規範
- **[controller-standards.md](./controller-standards.md)** - REST Controller 規範
  - HTTP 狀態碼使用
  - 請求/回應格式設計
  - 驗證與錯誤處理
  - API 版本管理

### 測試規範
- **[test-standards.md](./test-standards.md)** - 測試編碼規範
  - ezSpec BDD 測試框架
  - Use Case 測試模式
  - Assertion-free 測試
  - Mutation Testing 整合
  - 📋 包含各種測試模板

## 🔴 關鍵原則摘要

### 必須遵守的核心規則

1. **Repository 規範**
   - ❌ 絕對不要創建自定義 Repository 介面
   - ✅ 直接使用 `Repository<Aggregate, AggregateId>`
   - ✅ Repository 只能有三個方法: findById(), save(), delete()

2. **Aggregate 設計**
   - ✅ 每個 Aggregate 必須支援軟刪除 (isDeleted)
   - ✅ 使用公開建構子，不用 static factory method
   - ✅ Command method 必須有 ensure 後置條件檢查

3. **Use Case 設計**
   - ✅ Input/Output 必須是 UseCase interface 的 inner class
   - ✅ Command 修改狀態，Query 只讀取
   - ✅ 使用構造函數注入，不用 @Autowired field injection

4. **測試要求**
   - ✅ 使用 ezSpec BDD 框架
   - ✅ 支援 test-inmemory 和 test-outbox 雙 profile
   - ✅ 包含 uContract 的 Design by Contract 驗證

## 🛠️ 自動化檢查

這些規範文件是自動化檢查腳本的來源 (Single Source of Truth)：

```bash
# 生成檢查腳本
../../scripts/generate-check-scripts-from-md.sh

# 執行所有檢查
../../scripts/check-all.sh

# 執行特定檢查
../../scripts/check-repository-compliance.sh
../../scripts/check-aggregate-compliance.sh
```

檢查腳本會自動從這些 Markdown 文件中提取規則，確保文件與檢查邏輯永遠同步。

## 📋 快速導航

### 當你要...
- **創建新的 Aggregate** → 查看 [aggregate-standards.md](./aggregate-standards.md)
- **實作 Use Case** → 查看 [usecase-standards.md](./usecase-standards.md)
- **設計 REST API** → 查看 [controller-standards.md](./controller-standards.md)
- **撰寫測試** → 查看 [test-standards.md](./test-standards.md)
- **處理查詢** → 查看 [projection-standards.md](./projection-standards.md)
- **管理歷史資料** → 查看 [archive-standards.md](./archive-standards.md)

## 🔄 更新流程

1. **修改規範文件** - 編輯對應的 `.md` 檔案
2. **重新生成腳本** - 執行 `generate-check-scripts-from-md.sh`
3. **執行檢查** - 運行 `check-all.sh` 驗證現有程式碼
4. **修復違規** - 根據檢查結果調整程式碼

## 📚 相關文件

- [編碼指南](../coding-guide.md) - 實作指引和範例
- [範例程式碼](../examples/) - 各種模式的實作範例
- [ADR 索引](../../../ADR-INDEX.md) - 架構決策記錄
- [Sub-agent Workflow](../../../SUB-AGENT-SYSTEM.md) - AI 輔助開發流程