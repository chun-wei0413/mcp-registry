# Use Case Contract 範例

## 概述

Use Case 層的 Contract 設計著重於業務流程的正確性。本文提供各種 Use Case 的 Contract 範例。

## Command Use Case Contract

### 範例 1：CreateTagUseCase

```java
public class CreateTagService implements CreateTagUseCase {
    private final Repository<Tag, TagId> repository;
    
    public CreateTagService(Repository<Tag, TagId> repository) {
        requireNotNull("Repository", repository);
        this.repository = repository;
    }
    
    @Override
    public CqrsOutput execute(CreateTagInput input) {
        // Preconditions - 驗證輸入
        requireNotNull("Input", input);
        requireNotNull("Tag id", input.tagId);
        requireNotNull("Plan id", input.planId);
        requireNotNull("Tag name", input.name);
        requireNotNull("Tag color", input.color);
        
        require("Tag id is not empty", () -> !input.tagId.trim().isEmpty());
        require("Tag name is not empty", () -> !input.name.trim().isEmpty());
        require("Tag color is valid HEX format", () -> 
            input.color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$"));
        
        // 業務規則檢查
        require("Tag does not already exist", () -> 
            repository.findById(TagId.valueOf(input.tagId)).isEmpty());
        
        // 建立 Aggregate
        Tag tag = new Tag(
            TagId.valueOf(input.tagId),
            input.planId,
            input.name,
            input.color
        );
        
        // 儲存前的狀態
        int eventCount = tag.getDomainEvents().size();
        
        // 儲存到 Repository
        repository.save(tag);
        
        // Postconditions
        ensure("Tag was saved to repository", () -> 
            repository.findById(tag.getId()).isPresent());
        ensure("Saved tag matches created tag", () -> {
            Tag savedTag = repository.findById(tag.getId()).get();
            return savedTag.getName().equals(input.name) &&
                   savedTag.getColor().equals(input.color) &&
                   savedTag.getPlanId().equals(input.planId);
        });
        
        // 建立輸出
        CqrsOutput output = CqrsOutput.create()
            .setId(tag.getId().value())
            .setExitCode(ExitCode.SUCCESS);
            
        // 輸出驗證
        ensure("Output contains tag id", () -> 
            output.getId().equals(tag.getId().value()));
        ensure("Output indicates success", () -> 
            output.getExitCode() == ExitCode.SUCCESS);
            
        return output;
    }
}
```

### 範例 2：DeleteTaskUseCase

```java
public class DeleteTaskService implements DeleteTaskUseCase {
    private final Repository<Plan, PlanId> repository;
    
    @Override
    public CqrsOutput execute(DeleteTaskInput input) {
        // Preconditions
        requireNotNull("Input", input);
        requireNotNull("Plan id", input.planId);
        requireNotNull("Project id", input.projectId);
        requireNotNull("Task id", input.taskId);
        
        // 載入 Aggregate
        Plan plan = repository.findById(input.planId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Plan not found: " + input.planId));
        
        // 業務前置條件
        require("Project exists", () -> plan.hasProject(input.projectId));
        require("Task exists", () -> 
            plan.getProject(input.projectId).hasTask(input.taskId));
        
        // 儲存舊狀態
        long oldVersion = plan.getVersion();
        int oldTaskCount = plan.getProject(input.projectId).getTasks().size();
        boolean taskExistedBefore = plan.getProject(input.projectId).hasTask(input.taskId);
        
        // 執行業務邏輯
        plan.deleteTask(input.projectId, input.taskId);
        
        // 儲存變更
        repository.save(plan);
        
        // Postconditions
        ensure("Task no longer exists", () -> 
            !plan.getProject(input.projectId).hasTask(input.taskId));
        ensure("Task count decreased by 1", () -> 
            plan.getProject(input.projectId).getTasks().size() == oldTaskCount - 1);
        ensure("Plan version incremented", () -> 
            plan.getVersion() > oldVersion);
        
        return CqrsOutput.create()
            .setExitCode(ExitCode.SUCCESS);
    }
}
```

## Query Use Case Contract

### 範例：GetTasksByDateUseCase

```java
public class GetTasksByDateService implements GetTasksByDateUseCase {
    private final TasksByDateProjection projection;
    
    @Override
    public List<TaskDto> execute(GetTasksByDateInput input) {
        // Preconditions
        requireNotNull("Input", input);
        requireNotNull("User id", input.userId);
        requireNotNull("Date", input.date);
        require("User id is not empty", () -> !input.userId.trim().isEmpty());
        require("Date is valid", () -> input.date.matches("\\d{4}-\\d{2}-\\d{2}"));
        
        // 解析日期
        LocalDate targetDate = LocalDate.parse(input.date);
        require("Date is not in future", () -> 
            !targetDate.isAfter(LocalDate.now().plusDays(365)));
        
        // 執行查詢
        List<TaskDto> tasks = projection.findTasksByDate(input.userId, targetDate);
        
        // Postconditions
        ensure("Result is not null", () -> tasks != null);
        ensure("All tasks belong to user", () -> 
            tasks.stream().allMatch(task -> 
                // 假設 TaskDto 有方法可以驗證
                true // 實際應該檢查 userId
            ));
        ensure("All tasks have the target date", () -> 
            tasks.stream().allMatch(task -> 
                task.deadline() != null && 
                task.deadline().equals(input.date)
            ));
        
        // 額外的資料一致性檢查
        ensure("No duplicate tasks", () -> {
            Set<String> taskIds = new HashSet<>();
            return tasks.stream().allMatch(task -> taskIds.add(task.taskId()));
        });
        
        return tasks;
    }
}
```

## 複雜業務流程的 Contract

### 範例：TransferTaskUseCase

```java
public class TransferTaskService implements TransferTaskUseCase {
    private final Repository<Plan, PlanId> repository;
    
    @Override
    public CqrsOutput execute(TransferTaskInput input) {
        // Preconditions
        requireNotNull("Input", input);
        requireNotNull("Plan id", input.planId);
        requireNotNull("Source project id", input.sourceProjectId);
        requireNotNull("Target project id", input.targetProjectId);
        requireNotNull("Task id", input.taskId);
        require("Source and target are different", () -> 
            !input.sourceProjectId.equals(input.targetProjectId));
        
        // 載入 Aggregate
        Plan plan = repository.findById(input.planId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Plan not found: " + input.planId));
                
        // 業務前置條件
        require("Source project exists", () -> 
            plan.hasProject(input.sourceProjectId));
        require("Target project exists", () -> 
            plan.hasProject(input.targetProjectId));
        require("Task exists in source project", () -> 
            plan.getProject(input.sourceProjectId).hasTask(input.taskId));
        require("Task does not exist in target project", () -> 
            !plan.getProject(input.targetProjectId).hasTask(input.taskId));
            
        // 儲存舊狀態
        Project sourceProject = plan.getProject(input.sourceProjectId);
        Project targetProject = plan.getProject(input.targetProjectId);
        int sourceTaskCountBefore = sourceProject.getTasks().size();
        int targetTaskCountBefore = targetProject.getTasks().size();
        Task task = sourceProject.getTask(input.taskId);
        String taskName = task.getName();
        boolean taskDone = task.isDone();
        Set<TagId> taskTags = new HashSet<>(task.getTags());
        
        // 執行業務邏輯
        plan.transferTask(input.sourceProjectId, input.targetProjectId, input.taskId);
        
        // 儲存變更
        repository.save(plan);
        
        // Postconditions - 驗證轉移結果
        ensure("Task removed from source project", () -> 
            !plan.getProject(input.sourceProjectId).hasTask(input.taskId));
        ensure("Task added to target project", () -> 
            plan.getProject(input.targetProjectId).hasTask(input.taskId));
        ensure("Source project task count decreased", () -> 
            plan.getProject(input.sourceProjectId).getTasks().size() == 
            sourceTaskCountBefore - 1);
        ensure("Target project task count increased", () -> 
            plan.getProject(input.targetProjectId).getTasks().size() == 
            targetTaskCountBefore + 1);
            
        // 驗證任務屬性保持不變
        Task transferredTask = plan.getProject(input.targetProjectId).getTask(input.taskId);
        ensure("Task name unchanged", () -> 
            transferredTask.getName().equals(taskName));
        ensure("Task done status unchanged", () -> 
            transferredTask.isDone() == taskDone);
        ensure("Task tags unchanged", () -> 
            transferredTask.getTags().equals(taskTags));
        
        return CqrsOutput.create()
            .setExitCode(ExitCode.SUCCESS)
            .setMessage("Task transferred successfully");
    }
}
```

## 錯誤處理的 Contract

### 範例：具有補償邏輯的 Use Case

```java
public class CreateProjectWithTasksService implements CreateProjectWithTasksUseCase {
    private final Repository<Plan, PlanId> repository;
    private final TransactionManager txManager;
    
    @Override
    public CqrsOutput execute(CreateProjectWithTasksInput input) {
        // Preconditions
        requireNotNull("Input", input);
        requireNotNull("Plan id", input.planId);
        requireNotNull("Project name", input.projectName);
        requireNotNull("Task names", input.taskNames);
        require("At least one task", () -> !input.taskNames.isEmpty());
        require("All task names are valid", () -> 
            input.taskNames.stream().noneMatch(String::isBlank));
        
        Plan plan = repository.findById(input.planId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Plan not found: " + input.planId));
                
        // 儲存初始狀態
        int projectCountBefore = plan.getProjects().size();
        long versionBefore = plan.getVersion();
        
        ProjectId projectId = null;
        List<TaskId> createdTaskIds = new ArrayList<>();
        
        try {
            // 開始交易
            txManager.begin();
            
            // 建立專案
            projectId = plan.createProject(
                ProjectId.generate(), 
                ProjectName.valueOf(input.projectName)
            );
            
            // 建立所有任務
            for (String taskName : input.taskNames) {
                TaskId taskId = plan.createTask(
                    ProjectName.valueOf(input.projectName), 
                    taskName
                );
                createdTaskIds.add(taskId);
            }
            
            // 儲存變更
            repository.save(plan);
            
            // 提交交易
            txManager.commit();
            
            // Postconditions - 成功情況
            ensure("Project was created", () -> plan.hasProject(projectId));
            ensure("All tasks were created", () -> 
                createdTaskIds.size() == input.taskNames.size());
            ensure("All tasks exist in project", () -> {
                Project project = plan.getProject(projectId);
                return createdTaskIds.stream()
                    .allMatch(project::hasTask);
            });
            ensure("Project count increased by 1", () -> 
                plan.getProjects().size() == projectCountBefore + 1);
            ensure("Version increased", () -> 
                plan.getVersion() > versionBefore);
            
            return CqrsOutput.create()
                .setId(projectId.value())
                .setExitCode(ExitCode.SUCCESS);
                
        } catch (Exception e) {
            // 回滾交易
            txManager.rollback();
            
            // Postconditions - 失敗情況（補償後）
            ensure("No project was created on failure", () -> 
                plan.getProjects().size() == projectCountBefore);
            ensure("Version unchanged on failure", () -> 
                plan.getVersion() == versionBefore);
            ensure("No partial state on failure", () -> 
                projectId == null || !plan.hasProject(projectId));
            
            return CqrsOutput.create()
                .setExitCode(ExitCode.FAILURE)
                .setMessage("Failed to create project: " + e.getMessage());
        }
    }
}
```

## Contract 設計最佳實踐

### 1. 輸入驗證的完整性
- 永遠檢查 null
- 驗證字串不為空
- 檢查集合不為空
- 驗證格式（日期、email 等）

### 2. 業務規則的驗證
- 實體必須存在
- 狀態必須正確
- 權限必須足夠
- 資源必須可用

### 3. 輸出的保證
- 確保返回值不為 null
- 驗證輸出包含必要資訊
- 檢查狀態碼正確
- 確保副作用已發生

### 4. 異常情況的處理
- 定義清楚的異常情況
- 確保資源正確清理
- 驗證回滾的完整性
- 提供有意義的錯誤訊息

## 總結

Use Case 層的 Contract 設計應該：
- 完整驗證所有輸入
- 確保業務規則被遵守
- 保證輸出的正確性
- 處理所有異常情況
- 提供清晰的錯誤訊息