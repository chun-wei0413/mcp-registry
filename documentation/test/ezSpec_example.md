
## dependency
    請使用以下工具
```

        <ezspec.version>2.0.2</ezspec.version>
        
        <!-- Testing library versions -->
        <junit.version>5.10.2</junit.version>
        <junit-platform.version>1.10.2</junit-platform.version>
        <mockito.version>5.11.0</mockito.version>

```

## 以下為ezSpec使用範例

```
package tw.teddysoft.aiplan.plan.usecase;

import tw.teddysoft.aiplan.plan.entity.Plan;
import tw.teddysoft.aiplan.plan.entity.PlanEvents;
import tw.teddysoft.aiplan.plan.entity.PlanId;
import tw.teddysoft.aiplan.plan.entity.ProjectId;
import tw.teddysoft.aiplan.plan.entity.ProjectName;
import tw.teddysoft.aiplan.plan.usecase.port.in.CreatePlanUseCase;
import tw.teddysoft.aiplan.plan.usecase.port.in.CreatePlanUseCase.CreatePlanInput;
import tw.teddysoft.aiplan.plan.usecase.port.in.CreateProjectUseCase;
import tw.teddysoft.aiplan.plan.usecase.port.in.CreateProjectUseCase.CreateProjectInput;
import tw.teddysoft.aiplan.plan.usecase.service.CreatePlanService;
import tw.teddysoft.aiplan.plan.usecase.service.CreateProjectService;
import tw.teddysoft.aiplan.adapter.out.repository.GenericInMemoryRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
public class CreateProjectUseCaseTest {

    static String FEATURE_NAME = "Create Project";
    static Feature feature;
    
    // === Structure Rules ===
    static final String PROJECT_IDENTIFICATION = "Project must have valid and unique identifier within plan";
    static final String PROJECT_REQUIREMENTS = "Project must have complete information and belong to existing plan";
    static final String PROJECT_HIERARCHY = "Project exists within plan and can contain multiple tasks";
    
    // === Behavior Rules ===
    static final String PROJECT_CREATION_PROCESS = "Project creation modifies plan state and triggers events";
    static final String PROJECT_DUPLICATION = "Plans can have multiple projects with same name but different IDs";

    @BeforeAll
    static void beforeAll() {
        feature = Feature.New(FEATURE_NAME);
        feature.initialize();
        
        // Create rules
        feature.NewRule(PROJECT_IDENTIFICATION);
        feature.NewRule(PROJECT_REQUIREMENTS);
        feature.NewRule(PROJECT_HIERARCHY);
        feature.NewRule(PROJECT_CREATION_PROCESS);
        feature.NewRule(PROJECT_DUPLICATION);
    }

    @BeforeEach
    void setUp() {
        TestContext.reset();
    }

    @EzScenario
    public void create_a_project_in_a_plan() {

        feature.newScenario(PROJECT_CREATION_PROCESS)
                .Given("a plan exists", env -> {
                    String planId = UUID.randomUUID().toString();
                    String userId = UUID.randomUUID().toString();
                    String planName = "My Study Plan";

                    // Create a plan first
                    CreatePlanUseCase createPlanUseCase = getContext().newCreatePlanUseCase();
                    CreatePlanInput planInput = CreatePlanInput.create();
                    planInput.id = planId;
                    planInput.name = planName;
                    planInput.userId = userId;
                    
                    createPlanUseCase.execute(planInput);
                    getContext().clearPublishedEvents(); // Clear events from plan creation

                    env.put("planId", planId)
                            .put("userId", userId)
                            .put("planName", planName);
                })
                .And("I want to add a project to the plan", env -> {
                    String projectId = UUID.randomUUID().toString();
                    String projectName = "Backend Development";

                    env.put("projectId", projectId)
                            .put("projectName", projectName);
                })
                .When("I create a project", env -> {
                    CreateProjectUseCase createProjectUseCase = getContext().newCreateProjectUseCase();
                    CreateProjectInput input = CreateProjectInput.create();
                    input.planId = PlanId.valueOf(env.gets("planId"));
                    input.id = ProjectId.valueOf(env.gets("projectId"));
                    input.name = ProjectName.valueOf(env.gets("projectName"));

                    var output = createProjectUseCase.execute(input);
                    env.put("output", output)
                            .put("input", input);
                })
                .ThenSuccess(env -> {
                    var output = env.get("output", CqrsOutput.class);
                    assertNotNull(output.getId());
                    assertEquals(ExitCode.SUCCESS, output.getExitCode());
                })
                .And("the project should be added to the plan", env -> {
                    var output = env.get("output", CqrsOutput.class);
                    var input = env.get("input", CreateProjectInput.class);
                    Plan plan = getContext().planRepository().findById(input.planId).get();
                    assertTrue(plan.hasProject(input.id));
                    var project = plan.getProject(input.id);
                    assertEquals(input.id, project.getId());
                    assertEquals(input.name, project.getName());
                })
                .And("a project created event should be published", env -> {
                    List<DomainEvent> events = getContext().getPublishedEvents();
                    assertEquals(1, events.size());
                    assertTrue(events.get(0) instanceof PlanEvents.ProjectCreated);
                    PlanEvents.ProjectCreated event = (PlanEvents.ProjectCreated) events.get(0);
                    var input = env.get("input", CreateProjectInput.class);
                    assertEquals(input.planId.value(), event.planId().value());
                    assertEquals(input.id.value(), event.projectId().value());
                    assertEquals(input.name.value(), event.projectName().value());
                })
                .Execute();
    }

    @EzScenario
    public void create_project_fails_when_plan_not_found() {
        
        feature.newScenario(PROJECT_REQUIREMENTS)
                .Given("a plan ID that doesn't exist", env -> {
                    String nonExistentPlanId = UUID.randomUUID().toString();
                    String projectId = UUID.randomUUID().toString();
                    String projectName = "Backend Development";

                    env.put("planId", nonExistentPlanId)
                            .put("projectId", projectId)
                            .put("projectName", projectName);
                })
                .When("I try to create a project", env -> {
                    CreateProjectUseCase createProjectUseCase = getContext().newCreateProjectUseCase();
                    CreateProjectInput input = CreateProjectInput.create();
                    input.planId = PlanId.valueOf(env.gets("planId"));
                    input.id = ProjectId.valueOf(env.gets("projectId"));
                    input.name = ProjectName.valueOf(env.gets("projectName"));

                    var output = createProjectUseCase.execute(input);
                    env.put("output", output)
                            .put("input", input);
                })
                .ThenFailure(env -> {
                    var output = env.get("output", CqrsOutput.class);
                    assertEquals(ExitCode.FAILURE, output.getExitCode());
                    assertTrue(output.getMessage().contains("plan not found"));
                })
                .And("no events should be published", env -> {
                    List<DomainEvent> events = getContext().getPublishedEvents();
                    assertEquals(0, events.size());
                })
                .Execute();
    }

    @EzScenario
    public void create_project_fails_with_duplicate_id() {
        
        feature.newScenario(PROJECT_IDENTIFICATION)
                .Given("a plan with an existing project", env -> {
                    String planId = UUID.randomUUID().toString();
                    String userId = UUID.randomUUID().toString();
                    String planName = "My Study Plan";
                    String projectId = UUID.randomUUID().toString();
                    String projectName = "Backend Development";

                    // Create a plan
                    CreatePlanUseCase createPlanUseCase = getContext().newCreatePlanUseCase();
                    CreatePlanInput planInput = CreatePlanInput.create();
                    planInput.id = planId;
                    planInput.name = planName;
                    planInput.userId = userId;
                    createPlanUseCase.execute(planInput);

                    // Create first project
                    CreateProjectUseCase createProjectUseCase = getContext().newCreateProjectUseCase();
                    CreateProjectInput projectInput = CreateProjectInput.create();
                    projectInput.planId = PlanId.valueOf(planId);
                    projectInput.id = ProjectId.valueOf(projectId);
                    projectInput.name = ProjectName.valueOf(projectName);
                    createProjectUseCase.execute(projectInput);

                    getContext().clearPublishedEvents();

                    env.put("planId", planId)
                            .put("existingProjectId", projectId)
                            .put("newProjectName", "Frontend Development");
                })
                .When("I try to create a project with the same ID", env -> {
                    CreateProjectUseCase createProjectUseCase = getContext().newCreateProjectUseCase();
                    CreateProjectInput input = CreateProjectInput.create();
                    input.planId = PlanId.valueOf(env.gets("planId"));
                    input.id = ProjectId.valueOf(env.gets("existingProjectId")); // Same ID
                    input.name = ProjectName.valueOf(env.gets("newProjectName"));

                    try {
                        var output = createProjectUseCase.execute(input);
                        env.put("output", output);
                    } catch (Exception e) {
                        env.put("exception", e);
                    }
                    env.put("input", input);
                })
                .ThenFailure(env -> {
                    var exception = env.get("exception", Exception.class);
                    assertNotNull(exception);
                    assertTrue(exception.getMessage().contains("Project id must be unique"));
                })
                .And("no new events should be published", env -> {
                    List<DomainEvent> events = getContext().getPublishedEvents();
                    assertEquals(0, events.size());
                })
                .Execute();
    }

    @EzScenario
    public void create_multiple_projects_in_same_plan() {
        
        feature.newScenario(PROJECT_HIERARCHY)
                .Given("a plan exists", env -> {
                    String planId = UUID.randomUUID().toString();
                    String userId = UUID.randomUUID().toString();
                    String planName = "My Study Plan";

                    // Create a plan
                    CreatePlanUseCase createPlanUseCase = getContext().newCreatePlanUseCase();
                    CreatePlanInput planInput = CreatePlanInput.create();
                    planInput.id = planId;
                    planInput.name = planName;
                    planInput.userId = userId;
                    createPlanUseCase.execute(planInput);

                    getContext().clearPublishedEvents();

                    env.put("planId", planId);
                })
                .When("I create three different projects", env -> {
                    CreateProjectUseCase createProjectUseCase = getContext().newCreateProjectUseCase();
                    
                    List<String> projectIds = new ArrayList<>();
                    List<String> projectNames = List.of(
                        "Backend Development",
                        "Frontend Development",
                        "Database Design"
                    );

                    for (String projectName : projectNames) {
                        CreateProjectInput input = CreateProjectInput.create();
                        input.planId = PlanId.valueOf(env.gets("planId"));
                        input.id = ProjectId.valueOf(UUID.randomUUID().toString());
                        input.name = ProjectName.valueOf(projectName);

                        var output = createProjectUseCase.execute(input);
                        projectIds.add(input.id.value());
                    }

                    env.put("projectIds", projectIds)
                            .put("projectNames", projectNames);
                })
                .ThenSuccess(env -> {
                    List<String> projectIds = env.get("projectIds", List.class);
                    assertEquals(3, projectIds.size());
                })
                .And("all projects should be in the plan", env -> {
                    Plan plan = getContext().planRepository().findById(PlanId.valueOf(env.gets("planId"))).get();
                    List<String> projectIds = env.get("projectIds", List.class);
                    List<String> projectNames = env.get("projectNames", List.class);
                    
                    for (int i = 0; i < projectIds.size(); i++) {
                        ProjectId projectId = ProjectId.valueOf(projectIds.get(i));
                        assertTrue(plan.hasProject(projectId));
                        assertEquals(projectNames.get(i), plan.getProject(projectId).getName().value());
                    }
                })
                .And("three project created events should be published", env -> {
                    List<DomainEvent> events = getContext().getPublishedEvents();
                    assertEquals(3, events.size());
                    
                    for (DomainEvent event : events) {
                        assertTrue(event instanceof PlanEvents.ProjectCreated);
                    }
                })
                .Execute();
    }

    @EzScenario
    public void create_project_with_empty_name_fails() {
        
        feature.newScenario(PROJECT_REQUIREMENTS)
                .Given("a plan exists", env -> {
                    String planId = UUID.randomUUID().toString();
                    String userId = UUID.randomUUID().toString();
                    String planName = "My Study Plan";

                    // Create a plan
                    CreatePlanUseCase createPlanUseCase = getContext().newCreatePlanUseCase();
                    CreatePlanInput planInput = CreatePlanInput.create();
                    planInput.id = planId;
                    planInput.name = planName;
                    planInput.userId = userId;
                    createPlanUseCase.execute(planInput);

                    getContext().clearPublishedEvents();

                    env.put("planId", planId)
                            .put("projectId", UUID.randomUUID().toString());
                })
                .When("I try to create a project with empty name", env -> {
                    CreateProjectUseCase createProjectUseCase = getContext().newCreateProjectUseCase();
                    CreateProjectInput input = CreateProjectInput.create();
                    input.planId = PlanId.valueOf(env.gets("planId"));
                    input.id = ProjectId.valueOf(env.gets("projectId"));
                    
                    try {
                        input.name = ProjectName.valueOf("   "); // Empty or whitespace only
                        env.put("failed", false);
                    } catch (Exception e) {
                        env.put("exception", e);
                        env.put("failed", true);
                    }
                })
                .ThenFailure(env -> {
                    assertTrue(env.get("failed", Boolean.class));
                    var exception = env.get("exception", Exception.class);
                    assertNotNull(exception);
                    assertTrue(exception.getMessage().contains("cannot be empty"));
                })
                .Execute();
    }

    @EzScenario
    public void create_projects_with_same_name_but_different_ids() {
        
        feature.newScenario(PROJECT_DUPLICATION)
                .Given("a plan exists", env -> {
                    String planId = UUID.randomUUID().toString();
                    String userId = UUID.randomUUID().toString();
                    String planName = "My Study Plan";

                    // Create a plan
                    CreatePlanUseCase createPlanUseCase = getContext().newCreatePlanUseCase();
                    CreatePlanInput planInput = CreatePlanInput.create();
                    planInput.id = planId;
                    planInput.name = planName;
                    planInput.userId = userId;
                    createPlanUseCase.execute(planInput);

                    getContext().clearPublishedEvents();

                    env.put("planId", planId)
                            .put("projectName", "Backend Development");
                })
                .When("I create two projects with same name but different IDs", env -> {
                    CreateProjectUseCase createProjectUseCase = getContext().newCreateProjectUseCase();
                    String projectName = env.gets("projectName");
                    
                    // Create first project
                    CreateProjectInput input1 = CreateProjectInput.create();
                    input1.planId = PlanId.valueOf(env.gets("planId"));
                    input1.id = ProjectId.valueOf(UUID.randomUUID().toString());
                    input1.name = ProjectName.valueOf(projectName);
                    var output1 = createProjectUseCase.execute(input1);

                    // Create second project with same name
                    CreateProjectInput input2 = CreateProjectInput.create();
                    input2.planId = PlanId.valueOf(env.gets("planId"));
                    input2.id = ProjectId.valueOf(UUID.randomUUID().toString());
                    input2.name = ProjectName.valueOf(projectName);
                    var output2 = createProjectUseCase.execute(input2);

                    env.put("projectId1", input1.id)
                            .put("projectId2", input2.id)
                            .put("output1", output1)
                            .put("output2", output2);
                })
                .ThenSuccess(env -> {
                    var output1 = env.get("output1", CqrsOutput.class);
                    var output2 = env.get("output2", CqrsOutput.class);
                    assertEquals(ExitCode.SUCCESS, output1.getExitCode());
                    assertEquals(ExitCode.SUCCESS, output2.getExitCode());
                })
                .And("both projects should exist in the plan", env -> {
                    Plan plan = getContext().planRepository().findById(PlanId.valueOf(env.gets("planId"))).get();
                    var projectId1 = env.get("projectId1", ProjectId.class);
                    var projectId2 = env.get("projectId2", ProjectId.class);
                    String projectName = env.gets("projectName");
                    
                    assertTrue(plan.hasProject(projectId1));
                    assertTrue(plan.hasProject(projectId2));
                    assertEquals(projectName, plan.getProject(projectId1).getName().value());
                    assertEquals(projectName, plan.getProject(projectId2).getName().value());
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
        private GenericInMemoryRepository<Plan, PlanId> planRepository;
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
            planRepository = new GenericInMemoryRepository<>(messageBus);
        }

        public static TestContext getInstance() {
            if (instance == null) {
                instance = new TestContext();
            }
            return instance;
        }

        public static void reset() {
            instance = null;
        }

        public CreatePlanUseCase newCreatePlanUseCase() {
            return new CreatePlanService(planRepository);
        }

        public CreateProjectUseCase newCreateProjectUseCase() {
            return new CreateProjectService(planRepository);
        }

        public GenericInMemoryRepository<Plan, PlanId> planRepository() {
            return planRepository;
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