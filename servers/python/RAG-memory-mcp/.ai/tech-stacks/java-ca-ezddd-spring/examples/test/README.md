# 測試範例與模式

本目錄包含各種測試模式和最佳實踐，使用 ezSpec BDD 測試框架。

## 📋 概述

測試是確保程式碼品質的關鍵。本框架使用 ezSpec 進行 BDD（行為驅動開發）風格的測試，讓測試更易讀、更貼近業務需求。

## 🎯 核心概念

### ezSpec BDD 框架
- **@EzFeature**：標記測試類別為 Feature
- **@EzScenario**：標記測試方法為 Scenario
- **Given-When-Then**：結構化的測試步驟
- **Rule-based**：基於業務規則的測試組織

### 測試層級
1. **單元測試**：測試單一類別或方法
2. **整合測試**：測試多個組件的協作
3. **端到端測試**：測試完整的業務流程

## 📁 檔案結構

```
test/
├── README.md                     # 本文件
├── CreateTaskUseCaseTest.java    # Use Case 測試範例
├── PlanAggregateTest.java        # Aggregate 測試範例
└── TestDataBuilder.java          # 測試資料建構器
```

## 🔧 實作要點

### 1. Feature 測試結構

```java
@EzFeature
@EzFeatureReport
public class [Feature]Test {
    
    static String FEATURE_NAME = "[Feature Name]";
    static Feature feature;
    
    // === 業務規則定義 ===
    static final String RULE_1 = "業務規則描述1";
    static final String RULE_2 = "業務規則描述2";
    
    @BeforeAll
    static void beforeAll() {
        feature = Feature.New(FEATURE_NAME);
        feature.initialize();
        
        // 創建規則
        feature.NewRule(RULE_1);
        feature.NewRule(RULE_2);
    }
    
    @BeforeEach
    void setUp() {
        // 每個測試前的設置
    }
    
    @AfterAll
    static void afterAll() {
        PlainTextReport report = new PlainTextReport();
        System.out.println(report.generate(feature));
    }
}
```

### 2. Scenario 測試範例

```java
@EzScenario
public void test_創建任務_成功() {
    
    feature.newScenario(TASK_CREATION_RULE)
        .Given("存在一個包含專案的計畫", env -> {
            // 準備測試資料
            Plan plan = new Plan(
                PlanId.generate(), 
                "My Plan", 
                "user123"
            );
            plan.createProject(
                ProjectId.generate(), 
                ProjectName.valueOf("Backend")
            );
            
            // 保存到測試環境
            env.put("plan", plan);
            env.put("repository", new GenericInMemoryRepository<>());
            env.get("repository", Repository.class).save(plan);
        })
        .When("使用者創建一個新任務", env -> {
            // 執行測試動作
            CreateTaskUseCase useCase = new CreateTaskService(
                env.get("repository", Repository.class)
            );
            
            CreateTaskInput input = CreateTaskInput.create();
            input.planId = env.get("plan", Plan.class).getId();
            input.projectName = ProjectName.valueOf("Backend");
            input.taskName = "實作 API";
            
            CqrsOutput output = useCase.execute(input);
            env.put("output", output);
        })
        .Then("任務應該被成功創建", env -> {
            // 驗證結果
            CqrsOutput output = env.get("output", CqrsOutput.class);
            assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
            assertThat(output.getId()).isNotNull();
            
            // 驗證聚合根狀態
            Plan plan = env.get("repository", Repository.class)
                .findById(env.get("plan", Plan.class).getId())
                .orElseThrow();
            
            assertThat(plan.hasTask(TaskId.valueOf(output.getId()))).isTrue();
        })
        .And("應該產生 TaskCreated 事件", env -> {
            Plan plan = env.get("repository", Repository.class)
                .findById(env.get("plan", Plan.class).getId())
                .orElseThrow();
            
            assertThat(plan.getUncommittedChanges())
                .hasSize(1)
                .first()
                .isInstanceOf(PlanEvents.TaskCreated.class);
        });
}
```

### 3. Aggregate 測試

```java
@EzScenario
public void test_重新命名計畫() {
    
    feature.newScenario(PLAN_RENAME_RULE)
        .Given("一個已存在的計畫", env -> {
            Plan plan = new Plan(
                PlanId.generate(),
                "Original Name",
                "user123"
            );
            env.put("plan", plan);
            env.put("originalName", plan.getName());
        })
        .When("重新命名計畫", env -> {
            Plan plan = env.get("plan", Plan.class);
            plan.rename("New Name");
        })
        .Then("計畫名稱應該被更新", env -> {
            Plan plan = env.get("plan", Plan.class);
            assertThat(plan.getName()).isEqualTo("New Name");
        })
        .And("應該產生 PlanRenamed 事件", env -> {
            Plan plan = env.get("plan", Plan.class);
            PlanEvents lastEvent = plan.getLastDomainEvent();
            
            assertThat(lastEvent).isInstanceOf(PlanEvents.PlanRenamed.class);
            PlanEvents.PlanRenamed event = (PlanEvents.PlanRenamed) lastEvent;
            assertThat(event.oldName()).isEqualTo("Original Name");
            assertThat(event.newName()).isEqualTo("New Name");
        });
}
```

### 4. 測試資料建構器

```java
public class TestDataBuilder {
    
    public static Plan givenPlanWithProject() {
        Plan plan = new Plan(
            PlanId.generate(),
            "Test Plan",
            "test-user"
        );
        
        plan.createProject(
            ProjectId.generate(),
            ProjectName.valueOf("Test Project")
        );
        
        return plan;
    }
    
    public static Plan givenPlanWithTask() {
        Plan plan = givenPlanWithProject();
        
        plan.createTask(
            ProjectName.valueOf("Test Project"),
            "Test Task"
        );
        
        return plan;
    }
    
    public static Repository<Plan, PlanId> givenRepositoryWithPlan(Plan plan) {
        Repository<Plan, PlanId> repository = new GenericInMemoryRepository<>();
        repository.save(plan);
        return repository;
    }
}
```

## 💡 測試原則

### 1. 3A 模式
- **Arrange**：準備測試資料（Given）
- **Act**：執行測試動作（When）
- **Assert**：驗證結果（Then）

### 2. 測試獨立性
- 每個測試應該獨立運行
- 不依賴其他測試的執行順序
- 使用 @BeforeEach 重置狀態

### 3. 清晰的命名
- 使用中文描述測試情境
- 格式：`test_[操作]_[預期結果]`

### 4. 測試覆蓋率
- 正常路徑（Happy Path）
- 異常情況（Error Cases）
- 邊界條件（Edge Cases）

## 📝 常見測試場景

### Use Case 測試
```java
@EzScenario
public void test_找不到計畫時創建任務失敗() {
    feature.newScenario(ERROR_HANDLING_RULE)
        .Given("不存在的計畫ID", env -> {
            env.put("planId", PlanId.generate());
            env.put("repository", new GenericInMemoryRepository<>());
        })
        .When("嘗試創建任務", env -> {
            CreateTaskUseCase useCase = new CreateTaskService(
                env.get("repository", Repository.class)
            );
            
            CreateTaskInput input = CreateTaskInput.create();
            input.planId = env.get("planId", PlanId.class);
            input.projectName = ProjectName.valueOf("Any");
            input.taskName = "Any Task";
            
            CqrsOutput output = useCase.execute(input);
            env.put("output", output);
        })
        .Then("應該返回失敗結果", env -> {
            CqrsOutput output = env.get("output", CqrsOutput.class);
            assertThat(output.getExitCode()).isEqualTo(ExitCode.FAILURE);
            assertThat(output.getMessage()).contains("plan not found");
        });
}
```

### Repository 測試
```java
@EzScenario
public void test_儲存和查詢聚合根() {
    feature.newScenario(REPOSITORY_RULE)
        .Given("一個聚合根和 Repository", env -> {
            Plan plan = TestDataBuilder.givenPlanWithProject();
            Repository<Plan, PlanId> repository = new GenericInMemoryRepository<>();
            
            env.put("plan", plan);
            env.put("repository", repository);
        })
        .When("儲存聚合根", env -> {
            Repository<Plan, PlanId> repository = env.get("repository", Repository.class);
            Plan plan = env.get("plan", Plan.class);
            repository.save(plan);
        })
        .Then("應該能夠查詢到", env -> {
            Repository<Plan, PlanId> repository = env.get("repository", Repository.class);
            Plan originalPlan = env.get("plan", Plan.class);
            
            Optional<Plan> found = repository.findById(originalPlan.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(originalPlan.getId());
            assertThat(found.get().getName()).isEqualTo(originalPlan.getName());
        });
}
```

## ⚠️ 注意事項

1. **避免過度 Mock**
   - 優先使用真實物件（如 GenericInMemoryRepository）
   - 只在必要時使用 Mock（外部服務、網路呼叫）

2. **測試資料管理**
   - 使用 Builder 模式創建測試資料
   - 避免在測試中硬編碼大量資料

3. **效能考量**
   - 單元測試應該快速執行（< 100ms）
   - 整合測試可以稍慢但應該 < 1s

4. **測試報告**
   - 使用 @EzFeatureReport 生成測試報告
   - 定期檢視測試覆蓋率

## 🔗 相關資源

- [ezSpec 文檔](https://github.com/teddysoft/ezspec)
- [Aggregate 範例](../aggregate/)
- [Use Case 範例](../usecase/)
- [測試最佳實踐](../../standards/testing/)