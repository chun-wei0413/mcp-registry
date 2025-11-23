# Projection 設計模式與實作範例

## 概述

Projection 是 CQRS 模式中的查詢端實作，用於建立針對特定查詢需求優化的資料視圖。在 ezddd 框架中，Projection 提供了一個標準化的查詢介面，將查詢邏輯與資料存取細節分離。

## 📁 檔案結構

```
projection/
├── README.md                              # 本文件
│
├── Projection Interfaces (定義查詢契約)
├── PlanDtosProjection.java               # 查詢計畫列表
├── TasksByDateProjection.java            # 按日期查詢任務
├── TasksDueTodayProjection.java          # 查詢今日到期任務
├── TasksSortedByDeadlineProjection.java  # 按截止日期排序任務
├── AllTagsProjection.java                # 查詢所有標籤
│
└── JPA Implementations (實作查詢邏輯)
    ├── JpaPlanDtosProjection.java         # 計畫列表查詢實作
    ├── JpaTasksByDateProjection.java      # 按日期查詢實作
    ├── JpaTasksDueTodayProjection.java    # 今日到期查詢實作
    └── JpaAllTagsProjection.java          # 標籤查詢實作
```

## 核心概念

### 1. Projection 在 CQRS 中的角色

```
Command Side (Write)          Query Side (Read)
      ↓                            ↑
Domain Model → Events →    Event Handler → Projection
      ↓                            ↓
Event Store                   Read Database
```

### 2. Projection 定義

- **Projection Interface**: 定義查詢契約，在 `usecase.port.out.projection` 套件
- **JPA Projection Implementation**: 實作查詢邏輯，在 `adapter.out.projection` 套件
- **ProjectionInput**: 查詢參數封裝，作為 Projection interface 的內部類別
- **Output**: 通常是 DTO 或 DTO 列表

## ⚠️ 重要提醒

**產生 Projection 時必須同時產生兩個檔案**：
1. **Projection Interface** - 定義查詢契約
2. **JPA Projection Implementation** - 實作查詢邏輯

**絕對不要只產生 interface 而忘記產生 JPA 實作！**

## 實作模式

### 1. Projection Interface 模式

```java
package [rootPackage].[aggregate].usecase.port.out.projection;

import tw.teddysoft.ezddd.cqrs.usecase.query.Projection;
import tw.teddysoft.ezddd.cqrs.usecase.query.ProjectionInput;

import java.util.List;

public interface [Name]Projection extends Projection<[Name]Projection.[Name]ProjectionInput, [OutputType]> {

    // ProjectionInput 作為內部類別
    class [Name]ProjectionInput implements ProjectionInput {
        public String userId;       // 使用 public fields
        public LocalDate date;      // 簡化存取
        // 其他查詢參數...
    }
}
```

### 2. JPA Projection Implementation 模式

```java
package [rootPackage].[aggregate].adapter.out.projection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import [rootPackage].[aggregate].usecase.port.[Aggregate]Dto;
import [rootPackage].[aggregate].usecase.port.[Aggregate]Mapper;
import [rootPackage].[aggregate].usecase.port.out.[Aggregate]Data;
import [rootPackage].[aggregate].usecase.port.out.projection.[Name]Projection;

import java.util.List;

@Repository
public interface Jpa[Name]Projection extends JpaRepository<[Aggregate]Data, String>, [Name]Projection {
    
    @Override
    default [OutputType] query([Name]ProjectionInput input) {
        // 使用 Mapper 轉換查詢結果
        return [Aggregate]Mapper.toDto(findBy[Criteria](input.userId, input.date));
    }
    
    // JPA 查詢方法
    @Query("""
        SELECT DISTINCT p
        FROM [Aggregate]Data p
        LEFT JOIN FETCH p.childEntities
        WHERE p.userId = :userId
        AND p.date = :date
        ORDER BY p.createdAt DESC
        """)
    List<[Aggregate]Data> findBy[Criteria](@Param("userId") String userId, 
                                           @Param("date") LocalDate date);
}
```

## 實際範例

### 1. TasksByDateProjection - 按日期查詢任務

參見 [JpaTasksByDateProjection.java](./JpaTasksByDateProjection.java)

重點特性：
- 使用 JPQL 進行複雜查詢
- 返回特定 DTO 而非完整實體
- 支援多種查詢條件

### 2. PlanDtosProjection - 查詢計畫列表

```java
// Projection Interface
public interface PlanDtosProjection extends Projection<PlanDtosProjection.PlanDtosProjectionInput, List<PlanDto>> {
    
    class PlanDtosProjectionInput implements ProjectionInput {
        public String userId;
    }
}

// JPA Implementation
@Repository
public interface JpaPlanDtosProjection extends JpaRepository<PlanData, String>, PlanDtosProjection {
    
    @Override
    default List<PlanDto> query(PlanDtosProjectionInput input) {
        return PlanMapper.toDto(getPlans(input.userId));
    }
    
    @Query(value = "SELECT * FROM plan WHERE user_id = :userId AND is_deleted = false", 
           nativeQuery = true)
    List<PlanData> getPlans(@Param("userId") String userId);
}
```

## 在 Query Service 中使用 Projection

### 1. 注入 Projection

```java
@Service
public class GetTasksByDateService implements GetTasksByDateUseCase {

    private final TasksByDateProjection projection;

    public GetTasksByDateService(TasksByDateProjection projection) {
        this.projection = projection;
    }
    
    @Override
    public TasksByDateOutput execute(GetTasksByDateInput input) {
        try {
            var output = TasksByDateOutput.create();
            
            // 準備 Projection Input
            var projectionInput = new TasksByDateProjection.TasksByDateProjectionInput();
            projectionInput.userId = input.userId;
            projectionInput.targetDate = LocalDate.parse(input.date);
            
            // 執行查詢
            List<TaskDto> tasks = projection.query(projectionInput);
            
            // 設定輸出
            output.setTasks(tasks)
                  .setExitCode(ExitCode.SUCCESS);
            
            return output;
        } catch (Exception e) {
            throw new UseCaseFailureException(e);
        }
    }
}
```

### 2. Projection vs Repository 使用時機

| 使用 Repository | 使用 Projection |
|----------------|-----------------|
| 查詢單一 Aggregate | 跨 Aggregate 查詢 |
| 需要完整的領域物件 | 只需要部分資料 |
| 簡單的 CRUD 操作 | 複雜的查詢條件 |
| 需要修改資料 | 純粹讀取資料 |

## 設計要點

### 1. 單一介面設計
- JPA Projection 直接繼承 Projection interface 和 `JpaRepository`
- 不需要額外的實作類別
- 使用 `default` method 實作 `query` 方法

### 2. 命名規範
- Projection Interface: `[Feature]Projection`
- JPA Implementation: `Jpa[Feature]Projection`
- ProjectionInput: `[Feature]ProjectionInput`
- 查詢方法: `findBy[Criteria]`

### 3. 查詢優化
```java
// 使用 JOIN FETCH 避免 N+1 問題
@Query("""
    SELECT DISTINCT p
    FROM PlanData p
    LEFT JOIN FETCH p.projectDatas proj
    LEFT JOIN FETCH proj.taskDatas
    WHERE p.userId = :userId
    """)
List<PlanData> findPlansWithDetails(@Param("userId") String userId);

// 使用 DTO Projection 減少資料傳輸
@Query("""
    SELECT new tw.teddysoft.example.plan.usecase.port.TaskDueTodayDto(
        t.taskId,
        t.name,
        t.done,
        CAST(t.deadline AS string),
        p.id,
        p.name,
        proj.name
    )
    FROM PlanData p
    JOIN p.projectDatas proj
    JOIN proj.taskDatas t
    WHERE p.userId = :userId
    AND t.deadline = :targetDate
    """)
List<TaskDueTodayDto> findTasksDueToday(@Param("userId") String userId, 
                                        @Param("targetDate") LocalDate targetDate);
```

### 4. Mapper 整合
- 確保 Mapper 提供批次轉換方法
- 處理 null 值情況
- 支援巢狀結構轉換

```java
// Mapper 必須提供的方法
public static List<PlanDto> toDto(List<PlanData> planDatas) {
    requireNotNull("PlanData list", planDatas);
    return planDatas.stream()
        .map(PlanMapper::toDto)
        .collect(Collectors.toList());
}
```

## 最佳實踐

### 1. ProjectionInput 設計
- 作為 Projection interface 的內部類別
- 使用 public fields 簡化存取
- 包含所有查詢需要的參數

### 2. 錯誤處理
- 在 Service 層處理例外
- 使用 `UseCaseFailureException` 包裝
- 提供有意義的錯誤訊息

### 3. 效能考量
- 使用適當的索引
- 避免 SELECT *
- 考慮分頁需求
- 使用 DTO Projection 減少資料傳輸

### 4. 測試策略
```java
// 測試用 Mock 實作
public class MockTasksByDateProjection implements TasksByDateProjection {
    private final List<TaskDto> tasks = new ArrayList<>();
    
    @Override
    public List<TaskDto> query(TasksByDateProjectionInput input) {
        return tasks.stream()
            .filter(t -> t.getUserId().equals(input.userId))
            .filter(t -> t.getDeadline().equals(input.targetDate))
            .collect(Collectors.toList());
    }
    
    public void addTask(TaskDto task) {
        tasks.add(task);
    }
}
```

## 常見錯誤

### 1. 只產生 Interface 忘記實作
```java
// ❌ 錯誤：只有 interface，沒有 JPA 實作
public interface PlanDtosProjection extends Projection<...> { }

// ✅ 正確：必須同時有 JPA 實作
@Repository
public interface JpaPlanDtosProjection extends JpaRepository<...>, PlanDtosProjection { }
```

### 2. 錯誤的套件位置
```java
// ❌ 錯誤：放在 service 套件
package [rootPackage].[aggregate].usecase.service;

// ✅ 正確：Interface 在 port.out.projection
package [rootPackage].[aggregate].usecase.port.out.projection;

// ✅ 正確：Implementation 在 adapter.out.projection
package [rootPackage].[aggregate].adapter.out.projection;
```

### 3. 忘記使用 Mapper
```java
// ❌ 錯誤：直接返回 Data 物件
default List<PlanData> query(PlanDtosProjectionInput input) {
    return getPlans(input.userId);
}

// ✅ 正確：使用 Mapper 轉換為 DTO
default List<PlanDto> query(PlanDtosProjectionInput input) {
    return PlanMapper.toDto(getPlans(input.userId));
}
```

## 檢查清單

產生 Projection 時，確保完成以下項目：

- [ ] **Projection Interface** 已創建
  - [ ] 繼承 `Projection<Input, Output>`
  - [ ] 包含 ProjectionInput 內部類別
  - [ ] ProjectionInput 實作 `ProjectionInput` 介面

- [ ] **JPA Projection Implementation** 已創建
  - [ ] 同時繼承 Projection interface 和 `JpaRepository`
  - [ ] 使用 `default` method 實作 `query`
  - [ ] 包含 `@Query` 註解的查詢方法
  - [ ] 使用 Mapper 進行資料轉換

- [ ] **Mapper 支援**
  - [ ] 確認 Mapper 有批次轉換方法
  - [ ] 處理 null 值情況

- [ ] **Service 整合**
  - [ ] Query Service 正確注入 Projection
  - [ ] 正確建立 ProjectionInput
  - [ ] 適當的錯誤處理

## 相關資源

- [Query Pattern 說明](../usecase/README.md#query-pattern)
- [Mapper 模式](../mapper/README.md)
- [Repository vs Projection](../repository/README.md#projection-比較)