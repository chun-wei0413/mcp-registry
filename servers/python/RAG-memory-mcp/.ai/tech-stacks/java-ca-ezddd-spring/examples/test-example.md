# 測試範例 - 基礎概念與進階實作

> **🚀 快速導航**
> - **現代化實作**: 參考 [use-case-test-example.md](./use-case-test-example.md) - Spring Boot + Profile-based 測試
> - **基礎概念**: 繼續閱讀本文件了解 ezSpec 核心概念

## 概述

本文件展示 ezSpec BDD 測試的**基礎概念**，包含：
- ezSpec 框架的核心用法
- Domain Entity 測試模式  
- Test Data Builder 模式
- 手動管理 Repository 和 MessageBus 的方式

**適合閱讀情境**：
- 學習 ezSpec 基礎概念
- 了解 Domain Entity 的測試方式
- 需要手動控制測試環境（非 Spring Boot 專案）

## 重要規則

**絕對規則：所有測試必須使用 `GenericInMemoryRepository`，禁止直接實現 InMemory[Entity]Repository**

```java
// ❌ 錯誤
repository = new InMemoryPlanRepository();

// ✅ 正確
repository = new GenericInMemoryRepository<>(messageBus);
```

## 1. Feature 測試 - 完整場景

```java
package tw.teddysoft.example.plan.usecase;

import tw.teddysoft.ezspec.EzFeature;
import tw.teddysoft.ezspec.extension.junit5.EzScenario;
import tw.teddysoft.ezspec.keyword.Feature;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.*;

@EzFeature
public class PlanManagementFeatureTest {
    
    static Feature feature = Feature.New(
        "Plan Management",
        "As a user",
        "I want to manage my plans and tasks",
        "So that I can organize my work effectively"
    );
    
    private CreatePlanUseCase createPlanUseCase;
    private CreateTaskUseCase createTaskUseCase;
    private Repository<Plan, PlanId> repository;
    private MessageBus messageBus;
    private List<DomainEvent> publishedEvents;
    
    @BeforeEach
    void setUp() {
        publishedEvents = new ArrayList<>();
        messageBus = new BlockingMessageBus();
        
        // Register a reactor to capture domain events
        messageBus.register(event -> {
            if (event instanceof DomainEvent) {
                publishedEvents.add((DomainEvent) event);
            }
        });
        
        repository = new GenericInMemoryRepository<>(messageBus);
        createPlanUseCase = new CreatePlanService(repository);
        createTaskUseCase = new CreateTaskService(repository);
    }
    
    @EzScenario
    public void complete_plan_workflow() {
        feature.newScenario("Complete plan creation and task management workflow")
            .Given("a user wants to create a new plan", env -> {
                env.put("userId", "user123");
                env.put("planName", "Q4 Planning");
            })
            .When("the user creates a plan", env -> {
                var input = new CreatePlanInput(
                    env.get("planName"),
                    env.get("userId")
                );
                var output = createPlanUseCase.execute(input);
                env.put("planId", output.getData().getId());
                env.put("createPlanOutput", output);
            })
            .Then("the plan should be created successfully", env -> {
                var output = env.get("createPlanOutput", CqrsOutput.class);
                assertThat(output.isSuccessful()).isTrue();
                assertThat(output.getData().getName()).isEqualTo("Q4 Planning");
            })
            .And("a PlanCreated event should be published", env -> {
                var events = publishedEvents;
                assertThat(events).hasSize(1);
                assertThat(events.get(0)).isInstanceOf(PlanCreated.class);
                
                var event = (PlanCreated) events.get(0);
                assertThat(event.getName().getValue()).isEqualTo("Q4 Planning");
            })
            .When("the user creates a project in the plan", env -> {
                // Note: Should use CreateProjectUseCase here
                // This is a simplified example - in real tests, always use Use Case interfaces
                var createProjectInput = new CreateProjectInput();
                createProjectInput.planId = PlanId.of(env.get("planId", String.class));
                createProjectInput.projectName = "Backend Development";
                
                // Assuming we have createProjectUseCase injected
                // var output = createProjectUseCase.execute(createProjectInput);
                // env.put("projectId", output.getId());
                
                // For now, using direct aggregate interaction (NOT RECOMMENDED)
                var plan = repository.findById(PlanId.of(env.get("planId"))).orElseThrow();
                var projectId = plan.createProject("Backend Development");
                repository.save(plan);
                env.put("projectId", projectId.getValue());
            })
            .And("the user creates a task in the project", env -> {
                var input = CreateTaskInput.builder()
                    .planId(env.get("planId"))
                    .projectId(env.get("projectId"))
                    .taskName("Implement user authentication")
                    .description("OAuth2 integration")
                    .userId(env.get("userId"))
                    .build();
                
                var output = createTaskUseCase.execute(input);
                env.put("taskId", output.getData().getId());
                env.put("createTaskOutput", output);
            })
            .Then("the task should be created with correct details", env -> {
                var output = env.get("createTaskOutput", CqrsOutput.class);
                var task = output.getData();
                
                assertThat(task.getName()).isEqualTo("Implement user authentication");
                assertThat(task.getDescription()).isEqualTo("OAuth2 integration");
                assertThat(task.getStatus()).isEqualTo("PENDING");
            })
            .And("all events should be published in order", env -> {
                var allEvents = publishedEvents;
                assertThat(allEvents).hasSize(3);
                
                assertThat(allEvents.get(0)).isInstanceOf(PlanCreated.class);
                assertThat(allEvents.get(1)).isInstanceOf(ProjectCreated.class);
                assertThat(allEvents.get(2)).isInstanceOf(TaskCreated.class);
            })
            .Execute();
    }
}
```

## 2. Use Case 單元測試

```java
@EzFeature
public class CreatePlanUseCaseTest {
    
    static Feature feature = Feature.New("Create Plan Use Case");
    
    private CreatePlanService useCase;
    private Repository<Plan, PlanId> repository;
    private MessageBus messageBus;
    private List<DomainEvent> publishedEvents;
    
    @BeforeEach
    void setUp() {
        publishedEvents = new ArrayList<>();
        messageBus = new BlockingMessageBus();
        
        // Register a reactor to capture domain events
        messageBus.register(event -> {
            if (event instanceof DomainEvent) {
                publishedEvents.add((DomainEvent) event);
            }
        });
        
        repository = new GenericInMemoryRepository<>(messageBus);
        useCase = new CreatePlanService(repository);
    }
    
    @EzScenario
    public void successfully_create_plan_with_valid_input() {
        feature.newScenario("Successfully create a plan with valid input")
            .Given("valid plan creation input", env -> {
                var input = CreatePlanInput.builder()
                    .name("Sprint Planning")
                    .userId("user456")
                    .build();
                env.put("input", input);
            })
            .When("the use case is executed", env -> {
                var input = env.get("input", CreatePlanInput.class);
                var output = useCase.execute(input);
                env.put("output", output);
            })
            .Then("a plan should be created and persisted", env -> {
                var output = env.get("output", CqrsOutput.class);
                
                assertThat(output.isSuccessful()).isTrue();
                assertThat(output.getData()).isNotNull();
                
                var planId = PlanId.of(output.getData().getId());
                var savedPlan = repository.findById(planId);
                
                assertThat(savedPlan).isPresent();
                assertThat(savedPlan.get().getName()).isEqualTo("Sprint Planning");
            })
            .And("domain events should be published", env -> {
                var events = publishedEvents;
                
                assertThat(events).hasSize(1);
                assertThat(events.get(0)).isInstanceOf(PlanCreated.class);
                
                var event = (PlanCreated) events.get(0);
                assertThat(event.getName().getValue()).isEqualTo("Sprint Planning");
                assertThat(event.getOwnerId().getValue()).isEqualTo("user456");
            })
            .Execute();
    }
    
    @EzScenario
    public void fail_to_create_plan_with_invalid_input() {
        feature.newScenario("Fail to create plan with null name")
            .Given("invalid plan creation input with null name", env -> {
                // Input with null name
                env.put("invalidInput", () -> 
                    CreatePlanInput.builder()
                        .name(null)
                        .userId("user123")
                        .build()
                );
            })
            .When("the use case is executed", env -> {
                var inputSupplier = env.get("invalidInput", Supplier.class);
                env.put("exception", catchThrowable(() -> {
                    var input = inputSupplier.get();
                    useCase.execute(input);
                }));
            })
            .Then("a validation exception should be thrown", env -> {
                var exception = env.get("exception", Throwable.class);
                
                assertThat(exception)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("name");
            })
            .And("no plan should be saved", env -> {
                assertThat(repository.findAll()).isEmpty();
            })
            .And("no events should be published", env -> {
                assertThat(publishedEvents).isEmpty();
            })
            .Execute();
    }
}
```

## 3. Domain Entity 測試

```java
@EzFeature
public class PlanEntityTest {
    
    static Feature feature = Feature.New("Plan Entity Behavior");
    
    @EzScenario
    public void plan_lifecycle_scenario() {
        feature.newScenario("Complete plan lifecycle from creation to task management")
            .Given("a new plan is created", env -> {
                var plan = new Plan(PlanId.create(), "Development Plan", UserId.of("dev123"));
                env.put("plan", plan);
                env.put("originalEventCount", plan.getEvents().size());
            })
            .Then("the plan should have correct initial state", env -> {
                var plan = env.get("plan", Plan.class);
                
                assertThat(plan.getId()).isNotNull();
                assertThat(plan.getName()).isEqualTo("Development Plan");
                assertThat(plan.getOwnerId().getValue()).isEqualTo("dev123");
                assertThat(plan.getProjects()).isEmpty();
            })
            .And("a PlanCreated event should be generated", env -> {
                var plan = env.get("plan", Plan.class);
                var events = plan.getEvents();
                
                assertThat(events).hasSize(1);
                assertThat(events.get(0)).isInstanceOf(PlanCreated.class);
            })
            .When("a project is added to the plan", env -> {
                var plan = env.get("plan", Plan.class);
                plan.clearEvents(); // Clear previous events for testing
                
                var projectId = plan.createProject("API Development");
                env.put("projectId", projectId);
            })
            .Then("the project should be added successfully", env -> {
                var plan = env.get("plan", Plan.class);
                var projectId = env.get("projectId", ProjectId.class);
                
                assertThat(plan.getProjects()).hasSize(1);
                
                var project = plan.findProject(projectId);
                assertThat(project).isPresent();
                assertThat(project.get().getName()).isEqualTo("API Development");
            })
            .When("a task is added to the project", env -> {
                var plan = env.get("plan", Plan.class);
                var projectId = env.get("projectId", ProjectId.class);
                plan.clearEvents();
                
                var taskId = plan.createTask(
                    projectId.getValue(),
                    "Design REST endpoints",
                    UserId.of("dev123")
                );
                env.put("taskId", taskId);
            })
            .Then("the task should be created in the correct project", env -> {
                var plan = env.get("plan", Plan.class);
                var projectId = env.get("projectId", ProjectId.class);
                var taskId = env.get("taskId", TaskId.class);
                
                var task = plan.findTask(projectId.getValue(), taskId);
                assertThat(task).isPresent();
                assertThat(task.get().getName()).isEqualTo("Design REST endpoints");
                assertThat(task.get().getStatus()).isEqualTo(TaskStatus.PENDING);
            })
            .And("appropriate events should be generated", env -> {
                var plan = env.get("plan", Plan.class);
                var events = plan.getEvents();
                
                assertThat(events).hasSize(1);
                assertThat(events.get(0)).isInstanceOf(TaskCreated.class);
                
                var event = (TaskCreated) events.get(0);
                assertThat(event.getTaskName()).isEqualTo("Design REST endpoints");
            })
            .Execute();
    }
}
```

## 4. 測試輔助類別

```java
// 使用 BlockingMessageBus 的測試範例
// BlockingMessageBus 是 ezddd 框架內建的類別
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;

@Test
public void test_with_blocking_message_bus() {
    // BlockingMessageBus 適用於需要同步處理事件的測試場景
    BlockingMessageBus messageBus = new BlockingMessageBus();
    Repository<Plan, PlanId> repository = new GenericInMemoryRepository<>(messageBus);
    
    // 創建並保存 aggregate
    Plan plan = new Plan(PlanId.create(), "Test Plan", UserId.of("user123"));
    repository.save(plan);
    
    // 驗證事件已發布 - BlockingMessageBus 會同步處理事件
    // 使用 register() 方法註冊 reactor 來捕獲事件
    // 請參考 ezddd 框架文檔了解具體使用方法
}

// Test Data Builder
public class PlanTestDataBuilder {
    private String name = "Test Plan";
    private UserId ownerId = UserId.of("test-user");
    private List<String> projectNames = new ArrayList<>();
    
    public static PlanTestDataBuilder aPlan() {
        return new PlanTestDataBuilder();
    }
    
    public PlanTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public PlanTestDataBuilder withOwner(String userId) {
        this.ownerId = UserId.of(userId);
        return this;
    }
    
    public PlanTestDataBuilder withProject(String projectName) {
        this.projectNames.add(projectName);
        return this;
    }
    
    public Plan build() {
        var plan = new Plan(PlanId.create(), name, ownerId.toString());
        projectNames.forEach(plan::createProject);
        return plan;
    }
}

// Usage
var plan = PlanTestDataBuilder.aPlan()
    .withName("Q1 Planning")
    .withOwner("manager123")
    .withProject("Frontend")
    .withProject("Backend")
    .build();
```

## 5. 整合測試範例

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EzFeature
public class PlanManagementIntegrationTest {
    
    static Feature feature = Feature.New("Plan Management Integration");
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private PlanRepository planRepository;
    
    @EzScenario
    public void end_to_end_plan_creation() {
        feature.newScenario("End-to-end plan creation through REST API")
            .Given("a valid plan creation request", env -> {
                var request = """
                    {
                        "name": "Integration Test Plan"
                    }
                    """;
                env.put("request", request);
            })
            .When("the request is sent to the API", env -> {
                var request = env.get("request", String.class);
                
                var result = mockMvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header("X-User-Id", "integration-test-user"))
                    .andReturn();
                
                env.put("response", result.getResponse());
            })
            .Then("the plan should be created successfully", env -> {
                var response = env.get("response", MockHttpServletResponse.class);
                
                assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());
                assertThat(response.getHeader("Location")).isNotNull();
                
                var responseBody = new ObjectMapper()
                    .readValue(response.getContentAsString(), PlanDto.class);
                
                assertThat(responseBody.getName()).isEqualTo("Integration Test Plan");
                env.put("planId", responseBody.getId());
            })
            .And("the plan should be persisted in the database", env -> {
                var planId = env.get("planId", String.class);
                var plan = planRepository.findById(PlanId.of(planId));
                
                assertThat(plan).isPresent();
                assertThat(plan.get().getName()).isEqualTo("Integration Test Plan");
            })
            .Execute();
    }
}
```

## 關鍵測試原則

1. **Given-When-Then 結構**：清晰的測試場景描述
2. **環境變數傳遞**：使用 `env` 在步驟間共享數據
3. **完整的斷言**：驗證狀態、事件和副作用
4. **測試隔離**：每個測試獨立，不依賴外部狀態
5. **可讀性優先**：測試即文檔