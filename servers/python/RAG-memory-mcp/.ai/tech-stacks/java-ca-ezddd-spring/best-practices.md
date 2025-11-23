# Java DDD Spring 最佳實踐

## 概述

本文件總結了使用 Java DDD Spring 技術棧的最佳實踐，這些經驗來自實際專案的成功應用。

## 領域建模最佳實踐

### 1. ✅ 小而聚焦的 Aggregate
```java
// 好的設計：Plan Aggregate 只包含核心數據
public class Plan extends EsAggregateRoot<PlanId, PlanEvent> {
    private PlanId id;
    private PlanName name;
    private UserId ownerId;
    private List<Project> projects;  // 少量的嵌套實體
    
    // 業務行為集中在 Aggregate 中
    public ProjectId createProject(String name) {
        Contract.requireNotNull(name, "Project name");
        var projectId = ProjectId.generate();
        apply(new ProjectCreated(id, projectId, name));
        return projectId;
    }
}
```

### 2. ✅ 豐富的 Value Object
```java
// 不只是數據容器，包含驗證和行為
public class Email {
    private final String value;
    
    public Email(String value) {
        Contract.requireNotNull(value, "Email");
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.value = value.toLowerCase();
    }
    
    private static boolean isValid(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
    
    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }
}
```

### 3. ✅ 明確的領域事件
```java
// 使用內部類別組織相關事件
public interface PlanEvents {

    // 事件名稱清晰表達業務含義
    record TaskMovedToProject(
        PlanId planId,
        TaskId taskId,
        ProjectId fromProjectId,
        ProjectId toProjectId,
        Map<String, String> metadata
    ) implements DomainEvent {

        @Override
        public String aggregateId() {
            return planId.value();
        }

        @Override
        public String source() {
            return "Plan";
        }
    }

    // 使用 TypeMapper 註冊所有事件
    class TypeMapper {
        static {
            DomainEventTypeMapper.registerType("TaskMovedToProject", TaskMovedToProject.class);
        }
    }
}
```

## 應用層最佳實踐

### 4. ✅ 薄的 Use Case 層
```java
public class CreatePlanService implements CreatePlanUseCase {
    private final Repository<Plan, PlanId> repository;
    private final MessageBus messageBus;
    
    @Override
    public CqrsOutput<PlanDto> execute(CreatePlanInput input) {
        // Use Case 只做協調工作
        var plan = new Plan(PlanId.create(), input.getName(), input.getUserId());
        
        repository.save(plan);
        messageBus.publish(plan.getEvents());
        
        return CqrsOutput.of(PlanMapper.toDto(plan));
    }
}
```

### 5. ✅ 清晰的輸入輸出 DTO
```java
public interface CreateTaskUseCase extends Command<CreateTaskInput, TaskDto> {
    
    class CreateTaskInput implements Input {
        public String planId;
        public String projectId;
        public String taskName;
        public String description;
        public LocalDate deadline;
        
        // 提供便利的建構方法
        public static CreateTaskInput of(String planId, String projectId, String name) {
            return CreateTaskInput.builder()
                .planId(planId)
                .projectId(projectId)
                .taskName(name)
                .build();
        }
    }
}
```

### 6. ✅ 使用 Reactor 處理跨 Aggregate 操作
```java
public class NotifyTaskToUnassignTagService implements NotifyTaskToUnassignTagReactor {
    private final Repository<Plan, PlanId> planRepository;
    private final MessageBus messageBus;
    
    public void handle(TagDeleted event) {
        // 查找所有受影響的 Plans
        var affectedPlans = planRepository.findByTagId(event.getTagId());
        
        // 通知每個 Plan 移除 Tag
        affectedPlans.forEach(plan -> {
            plan.removeTagFromAllTasks(event.getTagId());
            planRepository.save(plan);
            messageBus.publish(plan.getEvents());
        });
    }
}
```

## 測試最佳實踐

### 7. ✅ BDD 風格的測試
```java
@EzFeature
public class CreatePlanUseCaseTest {
    static Feature feature = Feature.New("Create Plan", 
        "As a user",
        "I want to create a plan",
        "So that I can organize my tasks");
    
    @EzScenario
    public void successfully_create_plan_with_valid_name() {
        feature.newScenario("Successfully create a plan")
            .Given("user provides a valid plan name", env -> {
                env.put("planName", "My Todo List");
                env.put("userId", "user123");
            })
            .When("the create plan use case is executed", env -> {
                var input = new CreatePlanInput(
                    env.get("planName"),
                    env.get("userId")
                );
                var output = useCase.execute(input);
                env.put("output", output);
            })
            .Then("a new plan should be created", env -> {
                var output = env.get("output", CqrsOutput.class);
                assertThat(output.isSuccessful()).isTrue();
                assertThat(output.getData().getName()).isEqualTo("My Todo List");
            })
            .And("plan created event should be published", env -> {
                var events = messageBus.getPublishedEvents();
                assertThat(events).hasSize(1);
                assertThat(events.get(0)).isInstanceOf(PlanCreated.class);
            })
            .Execute();
    }
}
```

### 8. ✅ 使用 Test Data Builder
```java
public class PlanTestDataBuilder {
    private String name = "Default Plan";
    private String userId = "user123";
    private List<Project> projects = new ArrayList<>();
    
    public static PlanTestDataBuilder aPlan() {
        return new PlanTestDataBuilder();
    }
    
    public PlanTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public PlanTestDataBuilder withProject(String projectName) {
        projects.add(new Project(projectName));
        return this;
    }
    
    public Plan build() {
        var plan = new Plan(PlanId.create(), name, userId);
        projects.forEach(p -> plan.createProject(p.getName()));
        return plan;
    }
}

// 使用方式
var plan = PlanTestDataBuilder.aPlan()
    .withName("Sprint Planning")
    .withProject("Backend")
    .withProject("Frontend")
    .build();
```

## 持久化最佳實踐

### 9. ✅ 嚴格遵守 Repository 限制規則
```java
// 正確：Repository 只包含三個基本方法
public interface PlanRepository extends Repository<Plan, PlanId> {
    // 只繼承 findById, save, delete
    // 不添加任何自定義方法
}

// 錯誤：添加自定義查詢方法
public interface PlanRepository extends Repository<Plan, PlanId> {
    List<Plan> findByOwnerId(UserId ownerId);  // ❌ 違反規則！
    Optional<Plan> findByName(PlanName name);  // ❌ 違反規則！
}

// 查詢需求使用 Query Service
public class PlanQueryService {
    private final PlanDataJpaRepository jpaRepository;
    private final PlanMapper mapper;
    
    public List<PlanDto> findByOwnerId(String ownerId) {
        return jpaRepository.findByOwnerId(ownerId)
            .stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }
}

// 或使用 Projection
public interface PlanProjectionRepository extends JpaRepository<PlanProjection, String> {
    List<PlanProjection> findByOwnerId(String ownerId);
    Page<PlanProjection> searchByKeyword(String keyword, Pageable pageable);
}
```

**原因**：
- 強制 CQRS 分離
- 保持 Repository 純粹性
- 避免領域層污染
- 提高系統可維護性

### 10. ✅ 明確的數據映射
```java
public class PlanMapper {
    
    public static PlanData toData(Plan plan) {
        var data = new PlanData();
        data.setId(plan.getId().getValue());
        data.setStreamName(plan.getStreamName());
        data.setName(plan.getName().getValue());
        
        // 明確處理嵌套實體
        var projectDataList = plan.getProjects().stream()
            .map(ProjectMapper::toData)
            .collect(Collectors.toList());
        data.setProjects(projectDataList);
        
        return data;
    }
    
    public static PlanDto toDto(Plan plan) {
        return PlanDto.builder()
            .id(plan.getId().getValue())
            .name(plan.getName().getValue())
            .projectCount(plan.getProjects().size())
            .build();
    }
}
```

### 11. ✅ 使用 Projection 優化查詢
```java
// Repository 介面
public interface PlanSummaryProjection {
    @Query("SELECT new tw.teddysoft.example.product.usecase.port.ProductSummaryDto(" +
           "p.id, p.name, SIZE(p.projects), SIZE(p.tasks)) " +
           "FROM PlanData p WHERE p.userId = :userId")
    List<PlanSummaryDto> findSummariesByUserId(@Param("userId") String userId);
}

// DTO 建構子
public class PlanSummaryDto {
    public PlanSummaryDto(String id, String name, int projectCount, int taskCount) {
        // ...
    }
}
```

## 架構最佳實踐

### 12. ✅ 依賴注入使用建構子
```java
public class GetPlanService implements GetPlanUseCase {
    private final Repository<Plan, PlanId> repository;
    private final PlanProjection projection;
    
    // 不使用 @Autowired 字段注入
}
```

### 13. ✅ 使用 DateProvider 產生時間戳記

**所有 Domain Events 和需要時間戳記的地方都必須使用 `DateProvider.now()`**：

```java
// ✅ 正確：Domain Event 使用 DateProvider
apply(new PlanCreated(
    planId,
    name,
    userId,
    UUID.randomUUID(),
    DateProvider.now()  // 必須使用 DateProvider
));

// ✅ 正確：測試中設定固定時間
@Test
void should_create_plan_with_specific_time() {
    // Given
    DateProvider.setDate("2024-01-01T10:00:00Z");
    
    // When
    var plan = new Plan(PlanId.create(), "Test Plan", userId);
    
    // Then
    assertThat(plan.getCreatedAt()).isEqualTo("2024-01-01T10:00:00Z");
    
    // Cleanup
    DateProvider.resetDate();
}

// ❌ 錯誤：直接使用時間 API
Instant.now()              // 不可測試
LocalDateTime.now()        // 不可測試
System.currentTimeMillis() // 不可測試
```

**為什麼這很重要**：
- **可測試性**：固定時間讓測試結果可預測
- **一致性**：整個系統使用相同的時間來源
- **除錯容易**：可以重現特定時間點的行為

### 14. ✅ 統一的錯誤處理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AggregateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AggregateNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        var error = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message(e.getMessage())
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.badRequest().body(error);
    }
}
```

## 效能最佳實踐

### 15. ✅ 合理使用快取
```java
public class GetFrequentlyAccessedDataService {
    private final LoadingCache<String, PlanSummaryDto> cache;
    
    public GetFrequentlyAccessedDataService() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(this::loadPlanSummary);
    }
    
    private PlanSummaryDto loadPlanSummary(String planId) {
        return projection.findSummaryById(planId);
    }
}
```

### 16. ✅ 批量操作優化
```java
// 批量保存減少資料庫往返
public void createMultipleTasks(List<CreateTaskInput> inputs) {
    var plans = new HashMap<PlanId, Plan>();
    
    // 批量載入所需的 Plans
    var planIds = inputs.stream()
        .map(i -> new PlanId(i.getPlanId()))
        .distinct()
        .collect(Collectors.toList());
    
    repository.findAllById(planIds)
        .forEach(plan -> plans.put(plan.getId(), plan));
    
    // 批量創建任務
    inputs.forEach(input -> {
        var plan = plans.get(new PlanId(input.getPlanId()));
        plan.createTask(input.getProjectId(), input.getTaskName());
    });
    
    // 批量保存
    repository.saveAll(plans.values());
}
```

## 開發流程最佳實踐

### 17. ✅ 遵循 TDD 循環
```
1. 寫失敗的測試（Red）
2. 寫最少的代碼使測試通過（Green）
3. 重構代碼保持測試通過（Refactor）
4. 重複循環
```

### 18. ✅ 持續更新文檔
```markdown
# 在 .dev/specs/ 中維護領域規格
- 每次修改領域模型時更新規格
- 記錄業務規則的變更原因
- 保持範例代碼的時效性
```

### 19. ✅ ezSpec 測試規格保護

**ezSpec 測試代表業務規格，必須謹慎處理**：

```java
// ✅ 正確：將 ezSpec 測試視為業務規格
@EzScenario
public void process_valid_order() {
    feature.newScenario("Process a valid order")
        .Given("a valid order", env -> {
            // 這些條件代表業務規則，不能隨意修改
            var order = Order.builder()
                .customerId("valid-customer")
                .amount(Money.of(100))
                .build();
            env.put("order", order);
        })
        .When("processing the order", env -> {
            var order = env.get("order", Order.class);
            var result = orderService.process(order);
            env.put("result", result);
        })
        .Then("order should be processed successfully", env -> {
            // 這些期望代表業務預期結果
            var result = env.get("result", OrderResult.class);
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getOrderId()).isNotNull();
        });
}
```

**測試失敗時的處理原則**：
1. **不要修改 Given-When-Then**：這些是業務規格
2. **分析失敗原因**：是規格錯誤還是實現錯誤？
3. **尋求人類確認**：業務規格的變更需要明確授權
4. **優先修改實現**：假設規格是正確的，調整 production code

### 20. ✅ Code Review 檢查清單
- [ ] 業務邏輯在 Domain 層？
- [ ] Use Case 是否足夠薄？
- [ ] 是否正確使用 Value Object？
- [ ] 事件命名是否表達業務含義？
- [ ] 測試是否覆蓋主要場景？
- [ ] ezSpec 測試是否保持業務規格的完整性？
- [ ] 是否遵循既定的編碼規範？

## 總結

這些最佳實踐的核心原則：
1. **領域優先**：業務邏輯集中在領域層
2. **簡單明確**：代碼意圖清晰，避免過度設計
3. **測試驅動**：通過測試確保質量
4. **持續改進**：根據實際需求調整實踐