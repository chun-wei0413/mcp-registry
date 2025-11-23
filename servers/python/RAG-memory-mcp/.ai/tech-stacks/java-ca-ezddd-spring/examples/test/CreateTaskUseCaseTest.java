package tw.teddysoft.example.plan.usecase;

import tw.teddysoft.example.plan.entity.Plan;
import tw.teddysoft.example.plan.entity.PlanEvents;
import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.example.plan.entity.Project;
import tw.teddysoft.example.plan.entity.ProjectId;
import tw.teddysoft.example.plan.entity.ProjectName;
import tw.teddysoft.example.plan.entity.Task;
import tw.teddysoft.example.plan.entity.TaskId;
import tw.teddysoft.example.plan.usecase.port.in.CreatePlanUseCase;
import tw.teddysoft.example.plan.usecase.port.in.CreatePlanUseCase.CreatePlanInput;
import tw.teddysoft.example.plan.usecase.port.in.CreateProjectUseCase;
import tw.teddysoft.example.plan.usecase.port.in.CreateProjectUseCase.CreateProjectInput;
import tw.teddysoft.example.plan.usecase.port.in.CreateTaskUseCase;
import tw.teddysoft.example.plan.usecase.port.in.CreateTaskUseCase.CreateTaskInput;
import tw.teddysoft.example.plan.usecase.service.CreatePlanService;
import tw.teddysoft.example.plan.usecase.service.CreateProjectService;
import tw.teddysoft.example.plan.usecase.service.CreateTaskService;
import tw.teddysoft.example.adapter.out.repository.GenericInMemoryRepository;
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
public class CreateTaskUseCaseTest {

    static String FEATURE_NAME = "Create Task";
    static Feature feature;
    
    // === Structure Rules ===
    static final String TASK_IDENTIFICATION = "Task must have auto-incremented ID and valid name";
    static final String TASK_REQUIREMENTS = "Task must belong to existing project in plan";
    static final String TASK_HIERARCHY = "Task exists within project with hierarchical relationship";
    
    // === Behavior Rules ===
    static final String TASK_CREATION_PROCESS = "Task creation modifies plan state and triggers events";
    static final String TASK_ID_GENERATION = "Task IDs are globally unique within plan with incremental sequence";

    @BeforeAll
    static void beforeAll() {
        feature = Feature.New(FEATURE_NAME);
        feature.initialize();
        
        // Create rules
        feature.NewRule(TASK_IDENTIFICATION);
        feature.NewRule(TASK_REQUIREMENTS);
        feature.NewRule(TASK_HIERARCHY);
        feature.NewRule(TASK_CREATION_PROCESS);
        feature.NewRule(TASK_ID_GENERATION);
    }

    @BeforeEach
    void setUp() {
        TestContext.reset();
    }

    @EzScenario
    public void create_a_task_in_a_project() {

        feature.newScenario(TASK_CREATION_PROCESS)
                .Given("a plan with a project exists", env -> {
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

                    // Create a project
                    CreateProjectUseCase createProjectUseCase = getContext().newCreateProjectUseCase();
                    CreateProjectInput projectInput = CreateProjectInput.create();
                    projectInput.planId = PlanId.valueOf(planId);
                    projectInput.id = ProjectId.valueOf(projectId);
                    projectInput.name = ProjectName.valueOf(projectName);
                    createProjectUseCase.execute(projectInput);

                    getContext().clearPublishedEvents(); // Clear events from plan and project creation

                    env.put("planId", planId)
                            .put("userId", userId)
                            .put("planName", planName)
                            .put("projectId", projectId)
                            .put("projectName", projectName);
                })
                .And("I want to add a task to the project", env -> {
                    String taskName = "Implement User Authentication";

                    env.put("taskName", taskName);
                })
                .When("I create a task", env -> {
                    CreateTaskUseCase createTaskUseCase = getContext().newCreateTaskUseCase();
                    CreateTaskInput input = CreateTaskInput.create();
                    input.planId = PlanId.valueOf(env.gets("planId"));
                    input.projectName = ProjectName.valueOf(env.gets("projectName"));
                    input.taskName = env.gets("taskName");

                    var output = createTaskUseCase.execute(input);
                    env.put("output", output)
                            .put("input", input);
                })
                .ThenSuccess(env -> {
                    var output = env.get("output", CqrsOutput.class);
                    assertNotNull(output.getId());
                    assertEquals(ExitCode.SUCCESS, output.getExitCode());
                })
                .And("the task should be added to the project", env -> {
                    var output = env.get("output", CqrsOutput.class);
                    var input = env.get("input", CreateTaskInput.class);
                    Plan plan = getContext().planRepository().findById(input.planId).get();
                    
                    // Get the task by its ID
                    TaskId taskId = TaskId.valueOf(output.getId());
                    var task = plan.getTask(taskId);
                    assertNotNull(task);
                    assertEquals(taskId, task.getId());
                    assertEquals(input.taskName, task.getName());
                    assertEquals(input.projectName, task.getProjectName());
                })
                .And("a task created event should be published", env -> {
                    List<DomainEvent> events = getContext().getPublishedEvents();
                    assertEquals(1, events.size());
                    assertTrue(events.get(0) instanceof PlanEvents.TaskCreated);
                    PlanEvents.TaskCreated event = (PlanEvents.TaskCreated) events.get(0);
                    var input = env.get("input", CreateTaskInput.class);
                    var output = env.get("output", CqrsOutput.class);
                    assertEquals(input.planId.value(), event.planId().value());
                    assertEquals(input.projectName.value(), event.projectName());
                    assertEquals(output.getId(), event.taskId());
                    assertEquals(input.taskName, event.taskName());
                })
                .Execute();
    }

    @EzScenario
    public void create_task_fails_when_plan_not_found() {
        
        feature.newScenario(TASK_REQUIREMENTS)
                .Given("a plan ID that doesn't exist", env -> {
                    String nonExistentPlanId = UUID.randomUUID().toString();
                    String projectName = "Backend Development";
                    String taskName = "Implement User Authentication";

                    env.put("planId", nonExistentPlanId)
                            .put("projectName", projectName)
                            .put("taskName", taskName);
                })
                .When("I try to create a task", env -> {
                    CreateTaskUseCase createTaskUseCase = getContext().newCreateTaskUseCase();
                    CreateTaskInput input = CreateTaskInput.create();
                    input.planId = PlanId.valueOf(env.gets("planId"));
                    input.projectName = ProjectName.valueOf(env.gets("projectName"));
                    input.taskName = env.gets("taskName");

                    var output = createTaskUseCase.execute(input);
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
    public void create_task_fails_when_project_not_found() {
        
        feature.newScenario(TASK_REQUIREMENTS)
                .Given("a plan exists without the specified project", env -> {
                    String planId = UUID.randomUUID().toString();
                    String userId = UUID.randomUUID().toString();
                    String planName = "My Study Plan";
                    String nonExistentProjectName = "Frontend Development";
                    String taskName = "Implement User Authentication";

                    // Create a plan
                    CreatePlanUseCase createPlanUseCase = getContext().newCreatePlanUseCase();
                    CreatePlanInput planInput = CreatePlanInput.create();
                    planInput.id = planId;
                    planInput.name = planName;
                    planInput.userId = userId;
                    createPlanUseCase.execute(planInput);

                    getContext().clearPublishedEvents();

                    env.put("planId", planId)
                            .put("projectName", nonExistentProjectName)
                            .put("taskName", taskName);
                })
                .When("I try to create a task", env -> {
                    CreateTaskUseCase createTaskUseCase = getContext().newCreateTaskUseCase();
                    CreateTaskInput input = CreateTaskInput.create();
                    input.planId = PlanId.valueOf(env.gets("planId"));
                    input.projectName = ProjectName.valueOf(env.gets("projectName"));
                    input.taskName = env.gets("taskName");

                    try {
                        var output = createTaskUseCase.execute(input);
                        env.put("output", output);
                        env.put("exceptionThrown", false);
                    } catch (Exception e) {
                        env.put("exception", e);
                        env.put("exceptionThrown", true);
                    }
                })
                .Then("it should throw an exception", env -> {
                    assertTrue(env.getb("exceptionThrown"));
                    Exception e = env.get("exception", Exception.class);
                    assertTrue(e.getMessage().contains("Project must exist"));
                })
                .And("no events should be published", env -> {
                    List<DomainEvent> events = getContext().getPublishedEvents();
                    assertEquals(0, events.size());
                })
                .Execute();
    }

    @AfterAll
    static void afterAll() {
        PlainTextReport report = new PlainTextReport();
        System.out.println(report.generate(feature));
    }

    private static TestContext getContext() {
        return TestContext.getInstance();
    }

    static class TestContext {
        private static TestContext instance;
        private Repository<Plan, PlanId> planRepository;
        private MessageBus messageBus;
        private List<DomainEvent> publishedEvents;

        private TestContext() {
            reset();
        }

        public static TestContext getInstance() {
            if (instance == null) {
                instance = new TestContext();
            }
            return instance;
        }

        public static void reset() {
            getInstance().planRepository = new GenericInMemoryRepository<>();
            getInstance().messageBus = new BlockingMessageBus();
            getInstance().publishedEvents = new ArrayList<>();
            
            // Subscribe to all events
            getInstance().messageBus.subscribe(DomainEvent.class, event -> {
                getInstance().publishedEvents.add(event);
            });
        }

        public Repository<Plan, PlanId> planRepository() {
            return planRepository;
        }

        public MessageBus messageBus() {
            return messageBus;
        }

        public List<DomainEvent> getPublishedEvents() {
            return new ArrayList<>(publishedEvents);
        }

        public void clearPublishedEvents() {
            publishedEvents.clear();
        }

        public CreatePlanUseCase newCreatePlanUseCase() {
            return new CreatePlanService(planRepository);
        }

        public CreateProjectUseCase newCreateProjectUseCase() {
            return new CreateProjectService(planRepository);
        }

        public CreateTaskUseCase newCreateTaskUseCase() {
            return new CreateTaskService(planRepository);
        }
    }
}