# AI Scripts Collection

這個目錄包含各種自動化檢查和驗證腳本，用於確保程式碼品質和規範遵守。

## 🚀 快速開始

### Code Review（最常用）
```bash
# Review 當前變更
./.ai/scripts/code-review.sh

# Review 特定 commit 範圍
./.ai/scripts/code-review.sh HEAD~3..HEAD

# Review staged changes
./.ai/scripts/code-review.sh staged
```

### 執行所有檢查（完整檢查）
```bash
# 執行所有檢查
./.ai/scripts/check-all.sh

# 只執行快速的重要檢查  
./.ai/scripts/check-all.sh --quick

# 只執行最關鍵的檢查
./.ai/scripts/check-all.sh --critical
```

## 📁 目錄結構

```
.ai/scripts/
├── code-review.sh                      # 智能 Code Review 檢查 🔥
├── check-all.sh                        # 執行所有檢查的整合腳本 🆕
├── add-adr.sh                          # 自動建立 ADR 並更新索引 ✅
├── update-adr-index.sh                 # 更新 ADR 索引檔案 ✅
├── check-adr-cross-references.sh       # 檢查 ADR 交叉引用
├── check-aggregate.sh                  # 檢查 Aggregate 實作規範
├── check-archive.sh                    # 檢查 Archive Pattern 實作
├── check-coding-standards.sh           # 檢查 coding standards 文件完整性 ✅
├── check-controller.sh                 # 檢查 Controller 實作規範
├── check-dependencies.sh               # 檢查 Maven 依賴項相容性和完整性 ✅
├── check-event-interfaces.sh           # 檢查 Domain Event 介面實作
├── check-jpa-projection-config.sh      # 檢查 JPA Projection 配置完整性 ✅
├── check-mapper.sh                     # 檢查 Mapper 設計規範
├── check-mutation-coverage.sh          # 檢查 Mutation Testing 覆蓋率
├── check-outbox-mapper-completeness.sh # 檢查 Outbox Mapper 完整性
├── check-projection.sh                 # 檢查 Projection 實作規範
├── check-repository.sh                 # 檢查 Repository Pattern 合規性 ✅
├── check-spec-compliance.sh            # 檢查 spec 實作完整性
├── check-test.sh                       # 檢查測試實作規範
├── check-usecase.sh                    # 檢查 Use Case 實作規範
├── generate-check-scripts-from-md.sh   # 從 MD 檔案生成檢查腳本
└── README.md                           # 本文件
```

## 🔍 可用腳本

### 1. code-review.sh 🔥

**用途**: 智能分析變更並執行相關的 code review 檢查

**使用方式**:
```bash
# Review 與 main 分支的差異（預設）
.ai/scripts/code-review.sh

# Review 特定 commit 範圍
.ai/scripts/code-review.sh HEAD~3..HEAD

# Review staged 檔案
.ai/scripts/code-review.sh staged

# Review 特定分支差異
.ai/scripts/code-review.sh feature-branch..main
```

**功能特色**:
- ✅ 智能分析變更檔案類型
- ✅ 只執行相關的檢查（效率最高）
- ✅ 提供 Pattern-Specific 檢查清單
- ✅ 顯示變更檔案統計
- ✅ 適合 PR review 和 commit 前檢查

**智能檢測規則**:
- Repository 變更 → 執行 Repository Pattern 檢查
- Mapper 變更 → 執行 Mapper 合規性檢查
- JPA Projection 變更 → 執行 JPA 配置檢查
- Aggregate/UseCase/Controller/Reactor 變更 → 顯示對應檢查清單

**執行時機**:
- **Code Review 時（推薦）**
- Commit 前的最終檢查
- PR 提交前
- 與同事討論程式碼時

### 2. check-all.sh 🆕

**用途**: 一次執行所有檢查腳本，提供完整的專案健康報告

**使用方式**:
```bash
# 執行所有檢查（預設）
.ai/scripts/check-all.sh

# 只執行快速檢查（跳過耗時的檢查）
.ai/scripts/check-all.sh --quick

# 只執行關鍵檢查（coding standards、repository、mapper）
.ai/scripts/check-all.sh --critical

# 顯示說明
.ai/scripts/check-all.sh --help
```

**功能特色**:
- ✅ 三種執行模式：full、quick、critical
- ✅ 彩色結果輸出和統計摘要
- ✅ 智能跳過不適用的檢查
- ✅ 詳細的錯誤報告和建議
- ✅ 適當的退出碼（0=成功，1=失敗）

**執行時機**:
- 大型重構後的完整檢查
- PR 提交前的品質確認
- 定期的專案健康檢查
- CI/CD pipeline 整合

### 2. add-adr.sh ✅

**用途**: 自動建立新的 Architecture Decision Record (ADR)

**使用方式**:
```bash
# 建立新 ADR
.ai/scripts/add-adr.sh "Database Selection"

# 建立並設定狀態
.ai/scripts/add-adr.sh --status Accepted "API Versioning Strategy"

# 建立核心決策（會更新 CLAUDE.md）
.ai/scripts/add-adr.sh --core "Authentication Architecture"

# 顯示說明
.ai/scripts/add-adr.sh --help
```

**功能特色**:
- ✅ 自動分配下一個 ADR 編號
- ✅ 從模板建立 ADR 檔案
- ✅ 自動更新 README.md 索引
- ✅ 可選更新 CLAUDE.md（核心決策）
- ✅ 標題自動轉換為 kebab-case

**執行時機**:
- 需要記錄架構決策時
- 技術選型決定時
- 重要設計模式確定時

### 2. update-adr-index.sh ✅

**用途**: 掃描 ADR 目錄並更新所有索引

**使用方式**:
```bash
# 更新所有 ADR 索引
.ai/scripts/update-adr-index.sh

# 預覽變更（不實際修改）
.ai/scripts/update-adr-index.sh --dry-run

# 顯示說明
.ai/scripts/update-adr-index.sh --help
```

**功能特色**:
- ✅ 掃描所有 ADR 檔案
- ✅ 自動重建 README.md 索引
- ✅ 檢查 CLAUDE.md 遺漏項目
- ✅ 驗證一致性（孤立引用、重複編號）
- ✅ 建立備份檔案

**執行時機**:
- 手動建立 ADR 後
- 修復索引不一致時
- CI/CD 檢查流程

### 3. check-dependencies.sh ✅

**用途**: 檢查 Maven 專案的依賴項配置和相容性

**使用方式**:
```bash
# 基本檢查 (預設)
.ai/scripts/check-dependencies.sh

# 完整分析 (包含傳遞依賴)
.ai/scripts/check-dependencies.sh --full

# 安全漏洞檢查 (需要網路連接)
.ai/scripts/check-dependencies.sh --security

# 顯示詳細輸出
.ai/scripts/check-dependencies.sh --verbose

# 顯示說明
.ai/scripts/check-dependencies.sh --help
```

**檢查項目**:
- ✅ ezSpec 相關依賴 (ezspec-core, ezspec-report)
- ✅ JUnit 版本相容性 (ezSpec 需要 JUnit 5)
- ✅ ezddd 框架依賴 (ezapp-starter, uContract, ezdoc)
- ✅ Spring Boot 版本和 starter 依賴
- ✅ Persistence API 相容性 (Java EE vs Jakarta EE)
- ✅ 測試相關依賴 (Mockito, ByteBuddy, REST Assured)
- ✅ 版本衝突檢測
- ✅ 已知安全漏洞檢查

**輸出特色**:
- 🟢 彩色輸出 (綠色=通過，黃色=警告，紅色=錯誤)
- 📊 結構化報告和修復建議
- 🎯 適當的退出碼 (0=成功，1=失敗，2=警告)

**執行時機**:
- 專案初始化後
- 新增或更新依賴項時
- 升級 Spring Boot 或框架版本時
- CI/CD pipeline 中的品質檢查

### 5. check-repository-compliance.sh ✅

**用途**: 檢查 Repository Pattern 實作是否符合規範

**使用方式**:
```bash
.ai/scripts/check-repository-compliance.sh
```

**檢查項目**:
- ✅ 禁止自定義 Repository 介面（必須使用泛型 `Repository<T, ID>`）
- ✅ Repository 只能有三個方法：findById()、save()、delete()
- ✅ 檢測違規的自定義介面
- ✅ 提供修正建議（使用 Projection、Inquiry 或 Archive）

**執行時機**:
- 新增或修改 Repository 相關程式碼後
- Code Review 時檢查 Repository Pattern 合規性
- 重構資料存取層時

**常見違規範例**:
```java
// ❌ 錯誤：自定義 Repository 介面
interface ProductRepository extends Repository<Product, ProductId> {
    List<Product> findByState(State state);  // 違規！
}

// ✅ 正確：使用 Projection 查詢
interface JpaProductProjection extends JpaRepository<ProductData, String> {
    List<ProductData> findByState(String state);
}
```

### 6. check-spec-compliance.sh

**用途**: 檢查實作是否符合 spec 規格要求

**使用方式**:
```bash
.ai/scripts/check-spec-compliance.sh <spec-file> <task-name>
```

**檢查項目**:
- ✅ 所有 spec 要求的元件是否都已實作
- ✅ 檔案是否存在於正確位置
- ✅ Input/Output 結構是否正確
- ✅ Domain Events 是否包含 metadata

**執行時機**:
- 完成 Use Case 實作後
- 執行 task 的 postChecks 時

### 7. check-mapper-compliance.sh

**用途**: 檢查 Mapper 是否符合設計規範

**使用方式**:
```bash
.ai/scripts/check-mapper-compliance.sh
```

**檢查項目**:
- ✅ 一個 DTO 一個 Mapper 原則
- ✅ Mapper 是否為靜態工具類
- ✅ 是否使用靜態方法
- ✅ 是否正確處理 null 值

**執行時機**:
- 新增或修改 Mapper 後
- Code Review 時

### 8. check-coding-standards.sh

**用途**: 檢查 coding standards 檔案的完整性和一致性

**使用方式**:
```bash
.ai/scripts/check-coding-standards.sh
```

**檢查項目**:
- ✅ 主檔案 `coding-standards.md` 的必要章節
- ✅ 5個專門規範檔案的存在性和完整性
- ✅ Sub-agent prompts 是否正確引用規範檔案
- ✅ 檔案之間的交叉引用
- ✅ 內容重複性檢查
- ✅ 檔案統計和大小檢查

**執行時機**:
- 新增或修改 coding standards 規則前後
- 更新 sub-agent prompts 時
- 分拆或重組規範檔案時

### 9. check-jpa-projection-config.sh ✅

**用途**: 檢查所有 JPA Projection 是否正確配置在 Spring JpaConfiguration 中

**使用方式**:
```bash
.ai/scripts/check-jpa-projection-config.sh
```

**檢查項目**:
- ✅ 尋找所有 Jpa*Projection.java 檔案
- ✅ 檢查每個 JPA Projection 的套件是否在 `@EnableJpaRepositories` 中
- ✅ 驗證 @Repository 註解存在
- ✅ 確認是否繼承 JpaRepository 介面
- ✅ 偵測潛在的 JPA repository 檔案

**執行時機**:
- **新增 JPA Projection 後（必須執行）**
- Spring Boot 啟動失敗，提示找不到 bean 時
- 重構 Projection 套件結構後
- CI/CD pipeline 中的驗證步驟

**常見問題修復**:
```bash
# 錯誤：Field xxx required a bean of type 'JpaXxxProjection' that could not be found
# 解決：執行腳本找出缺少的配置，然後加入 JpaConfiguration
.ai/scripts/check-jpa-projection-config.sh
```

## 🎯 使用建議

### 在 AI 對話中使用

當進行以下操作時，AI 助手會自動提醒執行相關腳本：

1. **專案初始化或依賴項變更後**
   ```
   AI: 依賴項配置完成，執行相容性檢查：
   .ai/scripts/check-dependencies.sh
   ```

2. **實作新功能後**
   ```
   AI: 實作完成，現在執行 spec 合規性檢查：
   .ai/scripts/check-spec-compliance.sh specs/create-product.md create-product
   ```

3. **修改 Mapper 後**
   ```
   AI: Mapper 修改完成，執行合規性檢查：
   .ai/scripts/check-mapper-compliance.sh
   ```

4. **更新 coding standards 後**
   ```
   AI: 規範更新完成，執行完整性檢查：
   .ai/scripts/check-coding-standards.sh
   ```

5. **新增 JPA Projection 後**
   ```
   AI: JPA Projection 實作完成，執行配置檢查：
   .ai/scripts/check-jpa-projection-config.sh
   ```

### 手動執行

你也可以隨時手動執行這些腳本來驗證：

```bash
# 檢查所有項目
.ai/scripts/check-dependencies.sh        # 依賴項檢查
.ai/scripts/check-spec-compliance.sh specs/my-spec.md my-task
.ai/scripts/check-mapper-compliance.sh  # Mapper 合規性
.ai/scripts/check-coding-standards.sh   # Coding standards
```

## 📊 輸出說明

所有腳本使用統一的輸出格式：
- 🟢 **綠色勾選 (✓)**: 檢查通過
- 🟡 **黃色警告 (⚠)**: 建議改進但不影響功能
- 🔴 **紅色錯誤 (✗)**: 必須修正的問題

## 🔧 擴展計畫

未來可能新增的腳本：
- `check-test-coverage.sh` - 檢查測試覆蓋率
- ~~`check-dependency-security.sh` - 依賴項安全性檢查~~ ✅ (已整合到 check-dependencies.sh)
- `generate-spec-from-code.sh` - 從程式碼反向生成 spec
- `check-aggregate-boundaries.sh` - 檢查 Aggregate 邊界
- `check-api-compatibility.sh` - 檢查 API 向後相容性
- `generate-migration-guide.sh` - 生成版本升級指南

## 💡 開發新腳本的準則

1. **命名規範**: `check-[target].sh` 或 `generate-[output].sh`
2. **使用顏色**: 使用統一的顏色變數 (RED, GREEN, YELLOW, BLUE)
3. **錯誤處理**: 使用 `set -e` 確保錯誤時停止
4. **輸出格式**: 清晰的標題和分組
5. **退出碼**: 成功=0, 警告=0, 錯誤=1

## 🔗 相關文件

- [Coding Standards](./../tech-stacks/java-ca-ezddd-spring/coding-standards.md)
- [Sub-agent Workflow](./../SUB-AGENT-SYSTEM.md)
- [CLAUDE.md](../../CLAUDE.md) - 專案記憶體