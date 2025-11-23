# Use Case Test 範例 - 現代化 Spring Boot 測試模式

## 概述

本範例展示如何使用 Spring Boot + ezSpec 框架編寫現代化的 Use Case 測試，支援 Profile-based 測試架構。

## 🔴 核心原則

### 1. Profile-Based Testing Architecture
- 所有 Use Case 測試必須繼承 `BaseUseCaseTest`
- 支援自動切換 `test-inmemory` 和 `test-outbox` profiles
- **絕對不要在 BaseUseCaseTest 加 @ActiveProfiles**

### 2. ID 生成規範
```java
// ✅ 聚合根 ID 必須使用 UUID
String productId = UUID.randomUUID().toString();
String sprintId = UUID.randomUUID().toString();
String pbiId = UUID.randomUUID().toString();

// ✅ userId 和 creatorId 可以使用固定字串（提高可讀性）
String userId = "user-123";
String creatorId = "test-creator";
```

## 完整範例：Command Use Case Test

```java
package tw.teddysoft.aiscrum.sprint.usecase;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import tw.teddysoft.aiscrum.test.base.BaseUseCaseTest;
import tw.teddysoft.aiscrum.sprint.entity.*;
import tw.teddysoft.aiscrum.sprint.usecase.port.in.CreateSprintUseCase;
import tw.teddysoft.aiscrum.sprint.usecase.port.in.CreateSprintUseCase.CreateSprintInput;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;
import tw.teddysoft.ezspec.EzFeature;
import tw.teddysoft.ezspec.EzFeatureReport;
import tw.teddysoft.ezspec.extension.junit5.EzScenario;
import tw.teddysoft.ezspec.keyword.Feature;
import tw.teddysoft.ezspec.visitor.PlainTextReport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@EzFeature
@EzFeatureReport
public class CreateSprintUseCaseTest extends BaseUseCaseTest {

    static String FEATURE_NAME = "Create Sprint";
    static Feature feature;
    
    // Rule-based 組織測試案例
    static final String SUCCESSFUL_CREATION_RULE = "Successful Sprint Creation";
    static final String VALIDATION_FAILURE_RULE = "Sprint Creation Validation";
    static final String BUSINESS_RULE = "Sprint Business Rules";
    
    @Autowired
    private CreateSprintUseCase createSprintUseCase;
    
    @Autowired
    private Repository<Sprint, SprintId> sprintRepository;

    @BeforeAll
    static void beforeAll() {
        feature = Feature.New(FEATURE_NAME);
        feature.initialize();
        feature.NewRule(SUCCESSFUL_CREATION_RULE);
        feature.NewRule(VALIDATION_FAILURE_RULE);
        feature.NewRule(BUSINESS_RULE);
    }

    @BeforeEach
    void setUp() {
        clearCapturedEvents();
    }

    @EzScenario(rule = SUCCESSFUL_CREATION_RULE)
    public void should_create_sprint_successfully() {
        feature.newScenario()
            .Given("a user wants to create a sprint", env -> {
                // 聚合根 ID 使用 UUID
                String productId = UUID.randomUUID().toString();
                String sprintId = UUID.randomUUID().toString();
                
                // userId/creatorId 可以使用固定字串
                String creatorId = "test-creator";
                
                env.put("productId", productId)
                   .put("sprintId", sprintId)
                   .put("creatorId", creatorId)
                   .put("name", "Sprint 1")
                   .put("goal", "Complete user authentication");
            })
            .When("the sprint is created", env -> {
                CreateSprintInput input = CreateSprintInput.create();
                input.productId = env.gets("productId");
                input.sprintId = env.gets("sprintId");
                input.name = env.gets("name");
                input.goal = env.gets("goal");
                input.startDateTime = LocalDateTime.of(2025, 1, 1, 9, 0);
                input.endDateTime = LocalDateTime.of(2025, 1, 14, 17, 0);
                input.zoneId = "Asia/Taipei";
                input.state = "PLANNED";
                input.creatorId = env.gets("creatorId");
                
                var output = createSprintUseCase.execute(input);
                env.put("output", output)
                   .put("input", input);
            })
            .ThenSuccess(env -> {
                var output = env.get("output", CqrsOutput.class);
                assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
                assertThat(output.getId()).isNotNull();
            })
            .And("the sprint should be persisted", env -> {
                var output = env.get("output", CqrsOutput.class);
                var input = env.get("input", CreateSprintInput.class);
                
                Sprint sprint = sprintRepository.findById(SprintId.valueOf(output.getId())).get();
                assertThat(sprint.getName().value()).isEqualTo(input.name);
                assertThat(sprint.getProductId().value()).isEqualTo(input.productId);
                assertThat(sprint.getCreatorId()).isEqualTo(input.creatorId);
            })
            .And("a SprintCreated event should be published", env -> {
                // 使用 await 處理非同步事件
                await().untilAsserted(() -> 
                    assertThat(fakeEventListener.capturedEvents.size()).isEqualTo(1)
                );
                
                List<DomainEvent> events = getCapturedEvents();
                assertTrue(events.get(0) instanceof SprintEvents.SprintCreated);
                
                SprintEvents.SprintCreated event = (SprintEvents.SprintCreated) events.get(0);
                var input = env.get("input", CreateSprintInput.class);
                assertThat(event.sprintId().value()).isEqualTo(input.sprintId);
                assertThat(event.name().value()).isEqualTo(input.name);
            })
            .Execute();
    }

    @EzScenario(rule = VALIDATION_FAILURE_RULE)
    public void should_fail_when_sprint_name_is_blank() {
        feature.newScenario()
            .Given("invalid input with blank name", env -> {
                CreateSprintInput input = CreateSprintInput.create();
                input.productId = UUID.randomUUID().toString();
                input.sprintId = UUID.randomUUID().toString();
                input.name = ""; // 空白名稱
                input.startDateTime = LocalDateTime.now();
                input.endDateTime = LocalDateTime.now().plusDays(14);
                input.creatorId = "test-creator";
                
                env.put("input", input);
            })
            .When("attempting to create sprint", env -> {
                var input = env.get("input", CreateSprintInput.class);
                
                try {
                    var output = createSprintUseCase.execute(input);
                    env.put("output", output);
                    env.put("exceptionThrown", false);
                } catch (Exception e) {
                    env.put("exception", e);
                    env.put("exceptionThrown", true);
                }
            })
            .ThenFailure(env -> {
                Boolean exceptionThrown = env.get("exceptionThrown", Boolean.class);
                
                if (exceptionThrown) {
                    Exception exception = env.get("exception", Exception.class);
                    assertNotNull(exception);
                    assertTrue(exception.getMessage().contains("name"));
                } else {
                    var output = env.get("output", CqrsOutput.class);
                    assertEquals(ExitCode.FAILURE, output.getExitCode());
                    assertTrue(output.getMessage().contains("name"));
                }
            })
            .And("no event should be published", env -> {
                List<DomainEvent> events = getCapturedEvents();
                assertEquals(0, events.size());
            })
            .Execute();
    }

    @EzScenario(rule = BUSINESS_RULE)
    public void should_not_allow_end_date_before_start_date() {
        feature.newScenario()
            .Given("a sprint with invalid date range", env -> {
                CreateSprintInput input = CreateSprintInput.create();
                input.productId = UUID.randomUUID().toString();
                input.sprintId = UUID.randomUUID().toString();
                input.name = "Invalid Sprint";
                input.startDateTime = LocalDateTime.of(2025, 2, 14, 17, 0);
                input.endDateTime = LocalDateTime.of(2025, 2, 1, 9, 0); // 結束早於開始
                input.zoneId = "Asia/Taipei";
                input.creatorId = "test-creator";
                
                env.put("input", input);
            })
            .When("attempting to create the sprint", env -> {
                var input = env.get("input", CreateSprintInput.class);
                
                try {
                    var output = createSprintUseCase.execute(input);
                    env.put("output", output);
                    env.put("exceptionThrown", false);
                } catch (Exception e) {
                    env.put("exception", e);
                    env.put("exceptionThrown", true);
                }
            })
            .ThenFailure(env -> {
                Boolean exceptionThrown = env.get("exceptionThrown", Boolean.class);
                assertTrue(exceptionThrown || 
                    env.get("output", CqrsOutput.class).getExitCode() == ExitCode.FAILURE);
            })
            .Execute();
    }

    @AfterAll
    static void afterAll() {
        PlainTextReport report = new PlainTextReport();
        feature.accept(report);
        System.out.println(report.toString());
    }
}
```

## Query Use Case Test 範例

```java
@EzFeature
@EzFeatureReport
public class GetSprintUseCaseTest extends BaseUseCaseTest {

    static Feature feature;
    static final String QUERY_SUCCESS_RULE = "Successful Sprint Query";
    static final String QUERY_NOT_FOUND_RULE = "Sprint Not Found";
    
    @Autowired
    private GetSprintUseCase getSprintUseCase;
    
    @Autowired
    private CreateSprintUseCase createSprintUseCase;
    
    @BeforeAll
    static void beforeAll() {
        feature = Feature.New("Get Sprint Use Case");
        feature.initialize();
        feature.NewRule(QUERY_SUCCESS_RULE);
        feature.NewRule(QUERY_NOT_FOUND_RULE);
    }

    @BeforeEach
    void setUp() {
        clearCapturedEvents();
    }

    @EzScenario(rule = QUERY_SUCCESS_RULE)
    public void should_get_sprint_successfully() {
        feature.newScenario()
            .Given("a sprint exists", env -> {
                // 先創建一個 Sprint
                CreateSprintInput createInput = CreateSprintInput.create();
                createInput.productId = UUID.randomUUID().toString();
                createInput.sprintId = UUID.randomUUID().toString();
                createInput.name = "Sprint for Query";
                createInput.startDateTime = LocalDateTime.now();
                createInput.endDateTime = LocalDateTime.now().plusDays(14);
                createInput.creatorId = "test-creator";
                
                createSprintUseCase.execute(createInput);
                
                // 等待事件發布後清除（避免影響後續測試）
                await().untilAsserted(() -> 
                    assertThat(fakeEventListener.capturedEvents.size()).isGreaterThanOrEqualTo(1)
                );
                clearCapturedEvents();
                
                env.put("sprintId", createInput.sprintId);
                env.put("expectedName", createInput.name);
            })
            .When("querying the sprint", env -> {
                GetSprintInput input = GetSprintInput.create();
                input.sprintId = env.gets("sprintId");
                
                var output = getSprintUseCase.execute(input);
                env.put("output", output);
            })
            .ThenSuccess(env -> {
                var output = env.get("output", GetSprintOutput.class);
                assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
                assertThat(output.getData()).isNotNull();
                assertThat(output.getData().getName()).isEqualTo(env.gets("expectedName"));
            })
            .Execute();
    }

    @EzScenario(rule = QUERY_NOT_FOUND_RULE)
    public void should_return_not_found_for_non_existent_sprint() {
        feature.newScenario()
            .Given("a non-existent sprint id", env -> {
                env.put("nonExistentId", UUID.randomUUID().toString());
            })
            .When("querying the non-existent sprint", env -> {
                GetSprintInput input = GetSprintInput.create();
                input.sprintId = env.gets("nonExistentId");
                
                var output = getSprintUseCase.execute(input);
                env.put("output", output);
            })
            .ThenFailure(env -> {
                var output = env.get("output", GetSprintOutput.class);
                assertThat(output.getExitCode()).isEqualTo(ExitCode.FAILURE);
                assertThat(output.getMessage()).contains("not found");
            })
            .Execute();
    }
}
```

## 關鍵差異與最佳實踐

### 1. Spring Boot 整合
- **使用 @Autowired** 而非手動建立 Repository
- **繼承 BaseUseCaseTest** 獲得測試基礎設施
- **支援 Profile 切換** (test-inmemory vs test-outbox)

### 2. 非同步事件處理
```java
// 使用 await 處理非同步事件
await().untilAsserted(() -> 
    assertThat(fakeEventListener.capturedEvents.size()).isEqualTo(1)
);

// 在需要時清除已捕獲的事件
clearCapturedEvents();
```

### 3. Rule-based 測試組織
```java
static final String SUCCESS_RULE = "Success scenarios";
static final String FAILURE_RULE = "Failure scenarios";

@EzScenario(rule = SUCCESS_RULE)
public void test_method() { }
```

### 4. ID 生成策略
```java
// 聚合根 ID - 必須使用 UUID
String productId = UUID.randomUUID().toString();
String sprintId = UUID.randomUUID().toString();

// 操作者 ID - 可以使用固定值
String userId = "user-123";
String creatorId = "test-creator";
```

### 5. 錯誤處理模式
```java
.When("executing use case", env -> {
    try {
        var output = useCase.execute(input);
        env.put("output", output);
        env.put("exceptionThrown", false);
    } catch (Exception e) {
        env.put("exception", e);
        env.put("exceptionThrown", true);
    }
})
.ThenFailure(env -> {
    Boolean exceptionThrown = env.get("exceptionThrown", Boolean.class);
    // 處理兩種失敗模式：異常或 FAILURE 狀態
})
```

## 測試檢查清單

- [ ] 繼承 BaseUseCaseTest
- [ ] 使用 @Autowired 注入依賴
- [ ] 聚合根 ID 使用 UUID.randomUUID().toString()
- [ ] userId/creatorId 可使用固定字串
- [ ] 使用 await() 處理非同步事件
- [ ] 適時呼叫 clearCapturedEvents()
- [ ] 使用 Rule 組織測試案例
- [ ] 包含 Given-When-Then-And 結構
- [ ] 驗證狀態、事件和副作用
- [ ] 測試成功和失敗場景

## 注意事項

1. **不要在 BaseUseCaseTest 加 @ActiveProfiles** - 讓環境決定 profile
2. **Query Use Case 需要準備測試資料** - 先創建再查詢
3. **清理捕獲的事件** - 避免測試間干擾
4. **使用 assertThat** - 更好的錯誤訊息
5. **測試隔離性** - 每個測試獨立執行