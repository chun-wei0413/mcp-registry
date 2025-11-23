# Use Case 範例與模式

本目錄包含 Use Case（用例）的設計模式說明與實作範例，遵循 Clean Architecture 原則。

## 📋 概述

Use Case 代表系統的業務邏輯，是應用層的核心。每個 Use Case 封裝了一個特定的業務操作，協調 Domain 層和基礎設施層的互動。

## 🎯 核心概念

### 什麼是 Use Case？
- **業務邏輯封裝**：一個完整的業務操作
- **協調者角色**：協調 Domain 和 Infrastructure
- **單一職責**：每個 Use Case 只做一件事
- **與框架無關**：不依賴特定技術框架

### CQRS 模式中的 Use Case 類型
在 ezddd 框架中，Use Case 分為兩種類型：

1. **Command** - 修改系統狀態的操作
   - 繼承 `Command<Input, CqrsOutput>` 介面
   - 返回 `CqrsOutput`（只包含操作結果）
   - 例如：CreatePlan、UpdateTask、DeleteProject

2. **Query** - 查詢資料的操作
   - 繼承 `Query<Input, Output>` 介面
   - 返回自定義的 Output（包含查詢結果）
   - 例如：GetPlan、ListTasks、SearchProjects

### Clean Architecture 層次
```
Controller → UseCase → Domain
    ↓           ↓         ↓
  Request    Service   Aggregate
```

## 📁 檔案結構

```
usecase/
├── README.md                    # 本文件
│
├── Command 範例
├── CreatePlanUseCase.java       # 創建聚合根 - 創建新的 Plan Aggregate
├── CreatePlanService.java       # 創建聚合根 - Service 實作（含 ID 檢查）
├── CreateTaskUseCase.java       # 聚合內創建 - 在既有 Plan 內創建 Task
├── CreateTaskService.java       # 聚合內創建 - Service 實作（含載入聚合）
├── DeleteTaskUseCase.java       # 刪除 - 刪除任務的 Use Case 介面
├── DeleteTaskService.java       # 刪除 - 刪除任務的 Service 實作
├── RenameTaskUseCase.java       # 更新 - 重新命名任務的 Use Case 介面
├── RenameTaskService.java       # 更新 - 重新命名任務的 Service 實作
├── AssignTagUseCase.java        # 跨聚合 - 指派標籤的 Use Case 介面
├── AssignTagService.java        # 跨聚合 - 指派標籤的 Service 實作
│
└── Query 範例
    ├── GetPlansUseCase.java     # 列表查詢 - 查詢計畫列表的 Use Case 介面
    ├── GetPlansService.java     # 列表查詢 - 查詢計畫列表的 Service 實作
    ├── GetPlanUseCase.java      # 單一查詢 - 查詢單一計畫的 Use Case 介面
    ├── GetPlanService.java      # 單一查詢 - 查詢單一計畫的 Service 實作
    ├── GetTasksByDateUseCase.java  # 條件查詢 - 依日期查詢任務的 Use Case 介面
    └── GetTasksByDateService.java  # 條件查詢 - 依日期查詢任務的 Service 實作
```

## 🔧 實作要點

### 1. Use Case 介面定義 (Command)

```java
package [package].usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

public interface [Operation]UseCase extends Command<[Operation]UseCase.[Operation]Input, CqrsOutput> {
    
    // 輸入參數類別
    class [Operation]Input implements Input {
        public [Aggregate]Id aggregateId;
        public String parameter1;
        public String parameter2;
        
        public static [Operation]Input create() {
            return new [Operation]Input();
        }
    }
}
```

### 2. Use Case 實作

```java
package [package].usecase.service;

import static tw.teddysoft.ucontract.Contract.requireNotNull;

public class [Operation]Service implements [Operation]UseCase {
    
    private final Repository<[Aggregate], [Aggregate]Id> repository;
    
    public [Operation]Service(Repository<[Aggregate], [Aggregate]Id> repository) {
        requireNotNull("Repository", repository);
        this.repository = repository;
    }
    
    @Override
    public CqrsOutput execute([Operation]Input input) {
        try {
            var output = CqrsOutput.create();
            
            // 1. 載入聚合根
            [Aggregate] aggregate = repository.findById(input.aggregateId)
                    .orElse(null);
            if (null == aggregate) {
                output.setId(input.aggregateId.value())
                      .setExitCode(ExitCode.FAILURE)
                      .setMessage("[Operation] failed: aggregate not found");
                return output;
            }
            
            // 2. 執行業務邏輯
            aggregate.doSomething(input.parameter1, input.parameter2);
            
            // 3. 儲存聚合根
            repository.save(aggregate);
            
            // 4. 返回結果
            output.setId(aggregate.getId().value());
            output.setExitCode(ExitCode.SUCCESS);
            return output;
            
        } catch (Exception e) {
            throw new UseCaseFailureException(e);
        }
    }
}
```

### 3. 具體範例：CreateTaskUseCase

```java
// Interface 定義
package tw.teddysoft.example.plan.usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

public interface CreateTaskUseCase extends Command<CreateTaskUseCase.CreateTaskInput, CqrsOutput> {
    
    class CreateTaskInput implements Input {
        public PlanId planId;
        public ProjectName projectName;
        public String taskName;
        
        public static CreateTaskInput create() {
            return new CreateTaskInput();
        }
    }
}

// Service 實作
public class CreateTaskService implements CreateTaskUseCase {

    private final Repository<Plan, PlanId> planRepository;

    public CreateTaskService(Repository<Plan, PlanId> planRepository) {
        requireNotNull("PlanRepository", planRepository);
        this.planRepository = planRepository;
    }

    @Override
    public CqrsOutput execute(CreateTaskInput input) {
        try {
            var output = CqrsOutput.create();

            // 載入 Plan 聚合根
            Plan plan = planRepository.findById(input.planId).orElse(null);
            if (null == plan) {
                output.setId(input.planId.value())
                      .setExitCode(ExitCode.FAILURE)
                      .setMessage("Create task failed: plan not found");
                return output;
            }

            // 執行業務操作 - 創建任務
            TaskId taskId = plan.createTask(input.projectName, input.taskName);

            // 儲存聚合根（包含新產生的事件）
            planRepository.save(plan);

            // 返回新創建的任務 ID
            output.setId(taskId.value());
            output.setExitCode(ExitCode.SUCCESS);
            return output;
            
        } catch (Exception e) {
            throw new UseCaseFailureException(e);
        }
    }
}
```

## 💡 設計原則

### 1. 單一職責原則
- 一個 Use Case 只處理一個業務操作
- 不要在一個 Use Case 中混合多個業務邏輯

### 2. 依賴倒置原則
- Use Case 依賴抽象（Repository 介面）
- 不依賴具體實作（JPA、MongoDB 等）

### 3. 錯誤處理
- 使用 CqrsOutput 統一返回格式
- 明確的錯誤訊息
- 適當的異常包裝

### 4. 事務管理
- 一個 Use Case 就是一個事務邊界
- Repository.save() 應該包含完整的事務

## 📝 使用範例

### 在 Controller 中使用

```java
@RestController
@RequestMapping("/api/tasks")
public class CreateTaskController {
    
    private final CreateTaskUseCase createTaskUseCase;
    
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody CreateTaskRequest request) {
        CreateTaskInput input = CreateTaskInput.create();
        input.planId = PlanId.valueOf(request.getPlanId());
        input.projectName = ProjectName.valueOf(request.getProjectName());
        input.taskName = request.getTaskName();
        
        CqrsOutput output = createTaskUseCase.execute(input);
        
        if (output.getExitCode() == ExitCode.SUCCESS) {
            return ResponseEntity.ok(Map.of("taskId", output.getId()));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", output.getMessage()));
        }
    }
}
```

### 在測試中使用

```java
@EzScenario
public void test_create_task_successfully() {
    // Given
    Plan plan = givenPlanWithProject();
    Repository<Plan, PlanId> repository = new GenericInMemoryRepository<>();
    repository.save(plan);
    
    CreateTaskUseCase useCase = new CreateTaskService(repository);
    CreateTaskInput input = CreateTaskInput.create();
    input.planId = plan.getId();
    input.projectName = ProjectName.valueOf("Backend");
    input.taskName = "Implement API";
    
    // When
    CqrsOutput output = useCase.execute(input);
    
    // Then
    assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
    assertThat(output.getId()).isNotNull();
    
    Plan updatedPlan = repository.findById(plan.getId()).orElseThrow();
    assertThat(updatedPlan.hasTask(TaskId.valueOf(output.getId()))).isTrue();
}
```

## ⚠️ 注意事項

1. **不要跨聚合根操作**
   - 一個 Use Case 應該只修改一個聚合根
   - 如需協調多個聚合根，考慮使用 Saga 或 Process Manager

2. **避免貧血模型**
   - 業務邏輯應該在 Domain 層（Aggregate）
   - Use Case 只負責協調

3. **保持簡單**
   - 如果 Use Case 變得複雜，考慮拆分
   - 使用組合而非繼承

4. **測試友好**
   - 使用依賴注入
   - 使用 In-Memory Repository 進行單元測試

## Query Pattern

### Query 設計原則

Query 是 CQRS 中的查詢操作，不會改變系統狀態，只負責讀取資料並回傳結果。

### Query Use Case Interface

```java
package [package].usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.query.Query;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;
import [package].usecase.port.[Aggregate]Dto;
import java.util.List;

public interface Get[Aggregate]sUseCase extends Query<Get[Aggregate]sUseCase.Get[Aggregate]sInput, Get[Aggregate]sUseCase.Get[Aggregate]sOutput> {
    
    class Get[Aggregate]sInput implements Input {
        public String userId;
        public String sortBy;
        public String sortOrder;
        
        public static Get[Aggregate]sInput create() {
            return new Get[Aggregate]sInput();
        }
    }
    
    class Get[Aggregate]sOutput extends CqrsOutput {
        public List<[Aggregate]Dto> [aggregate]s;
        
        public static Get[Aggregate]sOutput create() {
            return new Get[Aggregate]sOutput();
        }
        
        public List<[Aggregate]Dto> get[Aggregate]s() {
            return [aggregate]s;
        }
        
        public Get[Aggregate]sOutput set[Aggregate]s(List<[Aggregate]Dto> [aggregate]s) {
            this.[aggregate]s = [aggregate]s;
            return this;
        }
    }
}
```

### Query Service Implementation

```java
@Service
public class GetPlanService implements GetPlanUseCase {

    private final Repository<Plan, PlanId> planRepository;

    public GetPlanService(Repository<Plan, PlanId> planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public PlanOutput execute(GetPlanInput input) {
        try {
            var output = PlanOutput.create();
            
            Plan plan = planRepository.findById(PlanId.valueOf(input.planId))
                .orElse(null);
            
            if (plan == null) {
                output.setId(input.planId)
                      .setExitCode(ExitCode.FAILURE)
                      .setMessage("Plan not found with id: " + input.planId);
                return output;
            }
            
            output.setPlanDto(PlanMapper.toDto(plan))
                  .setId(plan.getId().value())
                  .setExitCode(ExitCode.SUCCESS);
            
            return output;
        } catch (Exception e) {
            throw new UseCaseFailureException(e);
        }
    }
}
```

### 具體範例：GetPlansUseCase (Query with Projection)

當需要複雜查詢時，使用 Projection 取代 Repository。這是完整的 GetPlansUseCase 實作範例：

#### GetPlansUseCase Interface
```java
package tw.teddysoft.example.plan.usecase.port.in;

import tw.teddysoft.example.plan.usecase.port.PlanDto;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.query.Query;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

import java.util.List;

public interface GetPlansUseCase extends Query<GetPlansUseCase.GetPlansInput, GetPlansUseCase.GetPlansOutput> {
    
    class GetPlansInput implements Input {
        public String userId;
        public String sortBy;
        public String sortOrder;
        
        public static GetPlansInput create() {
            return new GetPlansInput();
        }
    }
    
    class GetPlansOutput extends CqrsOutput {
        public List<PlanDto> plans;
        
        public static GetPlansOutput create() {
            return new GetPlansOutput();
        }
        
        public List<PlanDto> getPlans() {
            return plans;
        }
        
        public GetPlansOutput setPlans(List<PlanDto> plans) {
            this.plans = plans;
            return this;
        }
    }
}
```

#### GetPlansService Implementation
```java
package tw.teddysoft.example.plan.usecase.service;

import tw.teddysoft.example.plan.usecase.port.PlanDto;
import tw.teddysoft.example.plan.usecase.port.in.GetPlansUseCase;
import tw.teddysoft.example.plan.usecase.port.in.GetPlansUseCase.GetPlansInput;
import tw.teddysoft.example.plan.usecase.port.in.GetPlansUseCase.GetPlansOutput;
import tw.teddysoft.example.plan.usecase.port.out.PlanDtosProjection;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;
import tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetPlansService implements GetPlansUseCase {

    private final PlanDtosProjection planDtosProjection;

    public GetPlansService(PlanDtosProjection planDtosProjection) {
        this.planDtosProjection = planDtosProjection;
    }

    @Override
    public GetPlansOutput execute(GetPlansInput input) {
        try {
            var output = GetPlansOutput.create();
            var projectionInput = new PlanDtosProjection.PlanDtosProjectionInput();
            projectionInput.userId = input.userId;
            projectionInput.sortBy = input.sortBy;
            projectionInput.sortOrder = input.sortOrder;

            List<PlanDto> plans = planDtosProjection.query(projectionInput);

            output.setPlans(plans);
            output.setExitCode(ExitCode.SUCCESS);
            
            return output;
        } catch (Exception e) {
            throw new UseCaseFailureException(e);
        }
    }
}
```

### Projection vs Repository 使用時機

- **使用 Repository**：查詢單一 Aggregate 或需要完整的領域物件
- **使用 Projection**：跨 Aggregate 查詢、複雜過濾條件，或只需要部分資料

## 🔗 相關資源

- [Aggregate 範例](../aggregate/)
- [Repository 範例](../repository/)
- [Projection 範例](../projection/)
- [Controller 範例](../controller/)
- [測試範例](../test/)