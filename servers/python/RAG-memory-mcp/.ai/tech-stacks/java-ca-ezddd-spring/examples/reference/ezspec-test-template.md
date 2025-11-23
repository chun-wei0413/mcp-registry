# ezSpec 測試案例規範

## 實作規則

每一個使用案例至少要有一個相對應的ezSpec測試案例，並必須遵循以下規範：

## Rule 使用指南

ezSpec 的 Rule 功能可以將測試場景按業務邏輯分類：

1. **定義 Rule 常量**：
   ```java
   static final String SUCCESS_RULE = "Successful Creation";
   static final String VALIDATION_RULE = "Input Validation";
   ```

2. **創建 Rule**（在 `feature.initialize()` 之後）：
   ```java
   @BeforeAll
   static void beforeAll() {
       feature = Feature.New(FEATURE_NAME);
       feature.initialize();
       
       // Create rules
       feature.NewRule(SUCCESS_RULE);
       feature.NewRule(VALIDATION_RULE);
   }
   ```

3. **將場景分配到 Rule**：
   ```java
   @EzScenario(rule = SUCCESS_RULE)
   public void successful_scenario() { }
   
   // 或使用 withRule()
   feature.newScenario().withRule(VALIDATION_RULE)
   ```

## 基本範例（無 Rule）

```java
//package [rootPackage].[aggregate].usecase;

import tw.teddysoft.aikanban.board.entity.Board;
import tw.teddysoft.aikanban.board.entity.BoardEvents;
import tw.teddysoft.aikanban.board.entity.BoardId;
import tw.teddysoft.aikanban.board.usecase.port.in.CreateBoardUseCase;
import tw.teddysoft.aikanban.board.usecase.port.in.CreateBoardUseCase.CreateBoardInput;
import tw.teddysoft.aikanban.board.usecase.service.CreateBoardService;
import [rootPackage].common.adapter.out.repository.GenericInMemoryRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;
import tw.teddysoft.ezspec.EzFeature;
import tw.teddysoft.ezspec.EzFeatureReport;
import tw.teddysoft.ezspec.extension.junit5.EzScenario;
import tw.teddysoft.ezspec.keyword.Feature;
import tw.teddysoft.ezspec.visitor.PlainTextReport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EzFeature
@EzFeatureReport
public class CreateBoardUseCaseTest {

    static String FEATURE_NAME = "Create Board";
    static Feature feature = Feature.New(FEATURE_NAME);

    @BeforeAll
    static void beforeAll() {
        feature.initialize();
    }

    @EzScenario
    public void create_a_board() {

        feature.newScenario()
                .Given("a user who wants to create a board", env -> {
                    String boardId = UUID.randomUUID().toString();
                    String ownerId = UUID.randomUUID().toString();
                    String boardName = "My Kanban Board";

                    env.put("boardId", boardId)
                            .put("ownerId", ownerId)
                            .put("boardName", boardName);
                })
                .When("I create a board", env -> {
                    CreateBoardUseCase createBoardUseCase = getContext().newCreateBoardUseCase();
                    CreateBoardInput input = CreateBoardInput.create();
                    input.id = env.gets("boardId");
                    input.name = env.gets("boardName");
                    input.ownerId = env.gets("ownerId");

                    var output = createBoardUseCase.execute(input);
                    env.put("output", output)
                            .put("input", input);
                })
                .ThenSuccess(env -> {
                    var output = env.get("output", CqrsOutput.class);
                    assertNotNull(output.getId());
                    assertEquals(ExitCode.SUCCESS, output.getExitCode());
                })
                .And("the board should be persisted", env -> {
                    var output = env.get("output", CqrsOutput.class);
                    var input = env.get("input", CreateBoardInput.class);
                    Board board = getContext().boardRepository().findById(BoardId.valueOf(output.getId())).get();
                    assertEquals(output.getId(), board.getId());
                    assertEquals(input.name, board.getName());
                    assertEquals(input.ownerId, board.getOwnerId());
                })
                .And("a board created event should be published", env -> {
                    List<DomainEvent> events = getContext().getPublishedEvents();
                    assertEquals(1, events.size());
                    assertTrue(events.get(0) instanceof BoardEvents.BoardCreated);
                    BoardEvents.BoardCreated event = (BoardEvents.BoardCreated) events.get(0);
                    var input = env.get("input", CreateBoardInput.class);
                    assertEquals(input.id, event.boardId());
                    assertEquals(input.name, event.name());
                    assertEquals(input.ownerId, event.ownerId());
                })
                .Execute();
    }

    @AfterAll
    static void afterAll() {
        PlainTextReport report = new PlainTextReport();
        feature.accept(report);
        System.out.println(report.toString());
    }

    private TestContext getContext() {
        return TestContext.getInstance();
    }

    static class TestContext {
        private static TestContext instance;
        private GenericInMemoryRepository<Board, BoardId> boardRepository;
        private MessageBus<DomainEvent> messageBus;
        private List<DomainEvent> publishedEvents;

        private TestContext() {
            publishedEvents = new ArrayList<>();

            // Create BlockingMessageBus
            messageBus = new BlockingMessageBus();

            // Register a reactor to capture domain events
            messageBus.register(event -> {
                if (event instanceof DomainEvent) {
                    publishedEvents.add((DomainEvent) event);
                }
            });

            // Create GenericInMemoryRepository with MessageBus
            boardRepository = new GenericInMemoryRepository<>(messageBus);
        }

        public static TestContext getInstance() {
            if (instance == null) {
                instance = new TestContext();
            }
            return instance;
        }

        public CreateBoardUseCase newCreateBoardUseCase() {
            return new CreateBoardService(boardRepository);
        }

        public GenericInMemoryRepository<Board, BoardId> boardRepository() {
            return boardRepository;
        }

        public List<DomainEvent> getPublishedEvents() {
            return new ArrayList<>(publishedEvents);
        }

        public void clearPublishedEvents() {
            publishedEvents.clear();
        }
    }
}
```

## 使用 Rule 的完整範例

```java
@EzFeature
@EzFeatureReport
public class CreatePlanUseCaseTest {

    static String FEATURE_NAME = "Create Plan";
    static Feature feature;
    
    // Define Rules
    static final String SUCCESSFUL_CREATION_RULE = "Successful Plan Creation";
    static final String INPUT_VALIDATION_RULE = "Input Validation";
    static final String DUPLICATE_VALIDATION_RULE = "Duplicate Validation";

    @BeforeAll
    static void beforeAll() {
        feature = Feature.New(FEATURE_NAME);
        feature.initialize();
        
        // Create rules after initialize()
        feature.NewRule(SUCCESSFUL_CREATION_RULE);
        feature.NewRule(INPUT_VALIDATION_RULE);
        feature.NewRule(DUPLICATE_VALIDATION_RULE);
    }

    @BeforeEach
    void setUp() {
        TestContext.reset();
    }

    @EzScenario(rule = SUCCESSFUL_CREATION_RULE)
    public void create_plan_successfully() {
        feature.newScenario()
                .Given("valid plan data", env -> {
                    env.put("planId", UUID.randomUUID().toString());
                    env.put("planName", "My Plan");
                    env.put("userId", "user123");
                })
                .When("I create the plan", env -> {
                    CreatePlanInput input = CreatePlanInput.create();
                    input.id = env.gets("planId");
                    input.name = env.gets("planName");
                    input.userId = env.gets("userId");
                    
                    var output = createPlanUseCase.execute(input);
                    env.put("output", output);
                })
                .ThenSuccess(env -> {
                    var output = env.get("output", CqrsOutput.class);
                    assertEquals(ExitCode.SUCCESS, output.getExitCode());
                })
                .Execute();
    }

    @EzScenario(rule = INPUT_VALIDATION_RULE)
    public void reject_empty_plan_name() {
        feature.newScenario()
                .Given("a plan with empty name", env -> {
                    env.put("planName", "");
                })
                .When("I try to create the plan", env -> {
                    // ... implementation
                })
                .ThenFailure(env -> {
                    // ... assertions
                })
                .Execute();
    }

    @EzScenario(rule = DUPLICATE_VALIDATION_RULE)
    public void reject_duplicate_plan_id() {
        // 使用 withRule() 方法的另一種寫法
        feature.newScenario().withRule(DUPLICATE_VALIDATION_RULE)
                .Given("an existing plan", env -> {
                    // ... setup existing plan
                })
                .When("I create a plan with same ID", env -> {
                    // ... try to create duplicate
                })
                .ThenFailure(env -> {
                    // ... verify rejection
                })
                .Execute();
    }
    
    // ... rest of the test class
}
```

## 使用時機
- Generate test cases for use cases
- 需要將測試場景按業務邏輯分類時使用 Rule

## 注意事項
- static Feature feature (是 static data member)
- **Rule 必須在 `feature.initialize()` 之後創建**
- 可以使用 `@EzScenario(rule = ruleName)` 或 `withRule(ruleName)` 來指定 Rule
- 如果不指定 Rule，場景會歸類到預設的 "default" Rule





