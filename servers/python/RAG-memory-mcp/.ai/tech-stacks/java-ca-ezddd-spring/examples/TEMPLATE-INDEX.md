# 範本索引 (Template Index)

> 最後更新: 2025-08-05
> 總計範本: 51 個檔案

## 📑 快速導航

### 按類別檢索

#### 🏛️ Core (核心模式)
- [Aggregate 聚合根](#aggregate-聚合根)
- [Value Object 值物件](#value-object-值物件)
- [Repository 儲存庫](#repository-儲存庫)

#### 🎨 Patterns (設計模式)
- [UseCase 用例](#usecase-用例)
- [Projection 投影查詢](#projection-投影查詢)
- [Mapper 轉換器](#mapper-轉換器)
- [Controller 控制器](#controller-控制器)
- [DTO 資料傳輸物件](#dto-資料傳輸物件)
- [Persistence 持久化物件](#persistence-持久化物件)

#### 🚀 Advanced (進階模式)
- [Contract 契約設計](#contract-契約設計)
- [Reactor 事件反應器](#reactor-事件反應器)
- [Test 測試模式](#test-測試模式)

### 按用途檢索
- [創建操作](#創建操作)
- [查詢操作](#查詢操作)
- [更新操作](#更新操作)
- [刪除操作](#刪除操作)
- [跨聚合操作](#跨聚合操作)

---

## 詳細清單

### Aggregate 聚合根

| 檔案 | 版本 | 描述 | 相關模式 |
|-----|------|------|---------|
| [Plan.java](aggregate/Plan.java) | 1.0.0 | 計畫聚合根範例 | Event Sourcing, DDD |
| [PlanEvents.java](aggregate/PlanEvents.java) | 1.1.0 | 領域事件定義 | Event Sourcing |
| [PlanId.java](aggregate/PlanId.java) | 1.0.0 | 聚合根識別碼 | Value Object |
| [ProjectId.java](aggregate/ProjectId.java) | 1.0.0 | 專案識別碼 | Value Object |
| [ProjectName.java](aggregate/ProjectName.java) | 1.0.0 | 專案名稱值物件 | Value Object |
| [TagId.java](aggregate/TagId.java) | 1.0.0 | 標籤識別碼 | Value Object |

### Value Object 值物件

| 檔案 | 版本 | 描述 | 使用場景 |
|-----|------|------|---------|
| [PlanId.java](aggregate/PlanId.java) | 1.0.0 | UUID 型識別碼 | 聚合根識別 |
| [ProjectName.java](aggregate/ProjectName.java) | 1.0.0 | 字串型值物件 | 業務概念封裝 |

### Repository 儲存庫

| 檔案 | 版本 | 描述 | 實作類型 |
|-----|------|------|---------|
| [GenericInMemoryRepository.java](repository/GenericInMemoryRepository.java) | 1.0.0 | 記憶體儲存庫 | 測試用 |

### UseCase 用例

#### 創建操作

| 檔案 | 版本 | 描述 | 模式類型 |
|-----|------|------|---------|
| [CreatePlanUseCase.java](usecase/CreatePlanUseCase.java) | 1.0.0 | 創建聚合根介面 | Command |
| [CreatePlanService.java](usecase/CreatePlanService.java) | 1.1.0 | 創建聚合根實作 | Command |
| [CreateTaskUseCase.java](usecase/CreateTaskUseCase.java) | 1.0.0 | 聚合內創建介面 | Command |
| [CreateTaskService.java](usecase/CreateTaskService.java) | 1.1.0 | 聚合內創建實作 | Command |

#### 查詢操作

| 檔案 | 版本 | 描述 | 模式類型 |
|-----|------|------|---------|
| [GetPlanUseCase.java](usecase/GetPlanUseCase.java) | 1.0.0 | 單一查詢介面 | Query |
| [GetPlanService.java](usecase/GetPlanService.java) | 1.0.0 | 單一查詢實作 | Query |
| [GetPlansUseCase.java](usecase/GetPlansUseCase.java) | 1.0.0 | 列表查詢介面 | Query |
| [GetPlansService.java](usecase/GetPlansService.java) | 1.0.0 | 列表查詢實作 | Query |
| [GetTasksByDateUseCase.java](usecase/GetTasksByDateUseCase.java) | 1.0.0 | 條件查詢介面 | Query |
| [GetTasksByDateService.java](usecase/GetTasksByDateService.java) | 1.0.0 | 條件查詢實作 | Query |

#### 更新操作

| 檔案 | 版本 | 描述 | 模式類型 |
|-----|------|------|---------|
| [RenameTaskUseCase.java](usecase/RenameTaskUseCase.java) | 1.0.0 | 更新操作介面 | Command |
| [RenameTaskService.java](usecase/RenameTaskService.java) | 1.0.0 | 更新操作實作 | Command |

#### 刪除操作

| 檔案 | 版本 | 描述 | 模式類型 |
|-----|------|------|---------|
| [DeleteTaskUseCase.java](usecase/DeleteTaskUseCase.java) | 1.0.0 | 刪除操作介面 | Command |
| [DeleteTaskService.java](usecase/DeleteTaskService.java) | 1.0.0 | 刪除操作實作 | Command |

#### 跨聚合操作

| 檔案 | 版本 | 描述 | 模式類型 |
|-----|------|------|---------|
| [AssignTagUseCase.java](usecase/AssignTagUseCase.java) | 1.0.0 | 跨聚合操作介面 | Command |
| [AssignTagService.java](usecase/AssignTagService.java) | 1.0.0 | 跨聚合操作實作 | Command |

### Projection 投影查詢

| 檔案 | 版本 | 描述 | 查詢類型 |
|-----|------|------|---------|
| [PlanDtosProjection.java](projection/PlanDtosProjection.java) | 1.0.0 | 計畫列表查詢介面 | 列表 |
| [JpaPlanDtosProjection.java](projection/JpaPlanDtosProjection.java) | 1.0.0 | 計畫列表查詢實作 | 列表 |
| [TasksByDateProjection.java](projection/TasksByDateProjection.java) | 1.0.0 | 日期任務查詢介面 | 條件 |
| [JpaTasksByDateProjection.java](projection/JpaTasksByDateProjection.java) | 1.0.0 | 日期任務查詢實作 | 條件 |
| [TasksDueTodayProjection.java](projection/TasksDueTodayProjection.java) | 1.0.0 | 今日到期查詢介面 | 特定 |
| [JpaTasksDueTodayProjection.java](projection/JpaTasksDueTodayProjection.java) | 1.0.0 | 今日到期查詢實作 | 特定 |
| [TasksSortedByDeadlineProjection.java](projection/TasksSortedByDeadlineProjection.java) | 1.0.0 | 截止排序查詢介面 | 排序 |
| [AllTagsProjection.java](projection/AllTagsProjection.java) | 1.0.0 | 標籤查詢介面 | 列表 |
| [JpaAllTagsProjection.java](projection/JpaAllTagsProjection.java) | 1.0.0 | 標籤查詢實作 | 列表 |

### Mapper 轉換器

| 檔案 | 版本 | 描述 | 轉換類型 |
|-----|------|------|---------|
| [PlanMapper.java](mapper/PlanMapper.java) | 1.1.0 | 計畫實體轉換 | Entity ↔ DTO |
| [TaskMapper.java](mapper/TaskMapper.java) | 1.0.0 | 任務實體轉換 | Entity ↔ DTO |

### Controller 控制器

| 檔案 | 版本 | 描述 | API 類型 |
|-----|------|------|---------|
| [CreateTaskController.java](controller/CreateTaskController.java) | 1.0.0 | 創建任務 API | REST POST |

### Contract 契約設計

| 檔案 | 版本 | 描述 | 範例類型 |
|-----|------|------|---------|
| [aggregate-contract-example.md](contract/aggregate-contract-example.md) | 1.0.0 | 聚合根契約範例 | 文檔 |
| [usecase-contract-example.md](contract/usecase-contract-example.md) | 1.0.0 | 用例契約範例 | 文檔 |
| [value-object-contract-example.md](contract/value-object-contract-example.md) | 1.0.0 | 值物件契約範例 | 文檔 |
| [ucontract-detailed-examples.md](contract/ucontract-detailed-examples.md) | 1.0.0 | 進階契約技巧 | 文檔 |

### Test 測試模式

| 檔案 | 版本 | 描述 | 測試框架 |
|-----|------|------|---------|
| [CreateTaskUseCaseTest.java](test/CreateTaskUseCaseTest.java) | 1.0.0 | 用例測試範例 | ezSpec |

### DTO 資料傳輸物件

| 檔案 | 版本 | 描述 | 設計特點 |
|-----|------|------|---------|
| [PlanDto.java](dto/PlanDto.java) | 1.0.0 | 基本 DTO 範本 | Fluent setter pattern |
| [ProjectDto.java](dto/ProjectDto.java) | 1.0.0 | 巢狀 DTO 範本 | 父子關係處理 |
| [TaskDto.java](dto/TaskDto.java) | 1.0.0 | 複雜 DTO 範本 | 多資料類型處理 |

### Persistence 持久化物件

| 檔案 | 版本 | 描述 | JPA 特性 |
|-----|------|------|---------|
| [PlanData.java](persistence/PlanData.java) | 1.0.0 | 基本 Entity 範本 | Event Sourcing 支援 |
| [ProjectData.java](persistence/ProjectData.java) | 1.0.0 | OneToMany 關聯範本 | 雙向關聯管理 |
| [TaskData.java](persistence/TaskData.java) | 1.0.0 | 複雜關聯範本 | @ElementCollection 使用 |

---

## 使用指南

### 1. 查找範本
- 使用 Ctrl+F 搜尋關鍵字
- 按類別瀏覽
- 查看相關模式連結

### 2. 版本說明
- **1.0.0** - 初始版本
- **1.1.0** - 功能更新
- **2.0.0** - 重大變更

### 3. 狀態標記
- ✅ synced - 已同步
- ⚠️ outdated - 待更新
- ❌ deprecated - 已棄用

---

## 相關資源

- [範本同步規範](./TEMPLATE-SYNC-GUIDE.md)
- [版本控制檔案](.versions.json)
- [程式碼標準](../../CODING-STANDARDS.md)
- [設計文檔](../../design.md)