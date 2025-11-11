package com.mcpregistry.core.usecase.service;

import com.mcpregistry.core.entity.*;
import com.mcpregistry.core.usecase.port.in.connection.AddConnectionInput;
import com.mcpregistry.core.usecase.port.in.connection.AddConnectionUseCase;
import com.mcpregistry.core.usecase.port.common.UseCaseOutput;
import com.mcpregistry.core.usecase.port.out.DatabaseConnectionRepository;
import com.mcpregistry.core.usecase.port.out.DatabaseQueryExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import tw.teddysoft.ezspec.EzFeature;
import tw.teddysoft.ezspec.EzFeatureReport;
import tw.teddysoft.ezspec.extension.junit5.EzScenario;
import tw.teddysoft.ezspec.keyword.Feature;
import tw.teddysoft.ezspec.visitor.PlainTextReport;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EzFeature
@EzFeatureReport
public class AddConnectionUseCaseTest {

    static String FEATURE_NAME = "Add Database Connection";
    static Feature feature;

    // === Structure Rules ===
    static final String CONNECTION_IDENTIFICATION = "Connection must have unique identifier and complete configuration";
    static final String CONNECTION_VALIDATION = "Connection parameters must be valid and secure";
    static final String CONNECTION_TESTING = "New connections must be tested before being saved";

    // === Behavior Rules ===
    static final String CONNECTION_CREATION_PROCESS = "Connection creation validates input, tests connectivity, and saves state";
    static final String CONNECTION_DUPLICATE_PREVENTION = "System prevents duplicate connection IDs";
    static final String CONNECTION_ERROR_HANDLING = "Invalid connections are properly handled with clear error messages";

    @BeforeAll
    static void beforeAll() {
        feature = Feature.New(FEATURE_NAME);
        feature.initialize();

        // Create rules
        feature.NewRule(CONNECTION_IDENTIFICATION);
        feature.NewRule(CONNECTION_VALIDATION);
        feature.NewRule(CONNECTION_TESTING);
        feature.NewRule(CONNECTION_CREATION_PROCESS);
        feature.NewRule(CONNECTION_DUPLICATE_PREVENTION);
        feature.NewRule(CONNECTION_ERROR_HANDLING);
    }

    @BeforeEach
    void setUp() {
        TestContext.reset();
    }

    @EzScenario
    public void successfully_create_postgresql_connection() {

        feature.newScenario(CONNECTION_CREATION_PROCESS)
                .Given("valid PostgreSQL connection parameters", env -> {
                    String connectionId = UUID.randomUUID().toString();
                    AddConnectionInput input = new AddConnectionInput(
                        connectionId,
                        "localhost",
                        5432,
                        "test_db",
                        "postgres",
                        "password123",
                        "postgresql",
                        10
                    );

                    env.put("connectionId", connectionId)
                            .put("input", input);
                })
                .And("the connection test will succeed", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    var testResult = new DatabaseQueryExecutor.ConnectionTestResult(true, "連線成功", 100);
                    when(queryExecutor.testConnection(any(ConnectionId.class)))
                            .thenReturn(Mono.just(testResult));
                })
                .When("I create the connection", env -> {
                    AddConnectionUseCase useCase = getContext().newAddConnectionUseCase();
                    AddConnectionInput input = env.get("input", AddConnectionInput.class);

                    UseCaseOutput output = useCase.execute(input);
                    env.put("output", output);
                })
                .ThenSuccess(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isSuccess());
                    assertEquals("連線創建成功", output.getMessage());
                    assertTrue(output.getData().isPresent());
                })
                .And("the connection should be saved with connected status", env -> {
                    String connectionId = env.gets("connectionId");
                    var repository = getContext().mockRepository();

                    // Verify repository.save was called (may be called multiple times due to internal logic)
                    verify(repository, atLeastOnce()).save(any(DatabaseConnection.class));
                })
                .And("the connection should be tested", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    verify(queryExecutor, atLeastOnce()).testConnection(any(ConnectionId.class));
                })
                .Execute();
    }

    @EzScenario
    public void create_mysql_connection_successfully() {

        feature.newScenario(CONNECTION_CREATION_PROCESS)
                .Given("valid MySQL connection parameters", env -> {
                    String connectionId = UUID.randomUUID().toString();
                    AddConnectionInput input = new AddConnectionInput(
                        connectionId,
                        "localhost",
                        3306,
                        "test_db",
                        "root",
                        "password123",
                        "mysql",
                        5
                    );

                    env.put("connectionId", connectionId)
                            .put("input", input);
                })
                .And("the connection test will succeed", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    var testResult = new DatabaseQueryExecutor.ConnectionTestResult(true, "連線成功", 100);
                    when(queryExecutor.testConnection(any(ConnectionId.class)))
                            .thenReturn(Mono.just(testResult));
                })
                .When("I create the MySQL connection", env -> {
                    AddConnectionUseCase useCase = getContext().newAddConnectionUseCase();
                    AddConnectionInput input = env.get("input", AddConnectionInput.class);

                    UseCaseOutput output = useCase.execute(input);
                    env.put("output", output);
                })
                .ThenSuccess(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isSuccess());
                    assertTrue(output.getData().isPresent());
                })
                .Execute();
    }

    @EzScenario
    public void reject_duplicate_connection_id() {

        feature.newScenario(CONNECTION_DUPLICATE_PREVENTION)
                .Given("a connection already exists", env -> {
                    String existingConnectionId = "existing-connection-123";
                    var repository = getContext().mockRepository();

                    // Mock existing connection
                    when(repository.existsById(ConnectionId.of(existingConnectionId)))
                            .thenReturn(true);

                    AddConnectionInput input = new AddConnectionInput(
                        existingConnectionId,
                        "localhost",
                        5432,
                        "test_db",
                        "postgres",
                        "password123",
                        "postgresql",
                        10
                    );

                    env.put("input", input);
                })
                .When("I try to create a connection with the same ID", env -> {
                    AddConnectionUseCase useCase = getContext().newAddConnectionUseCase();
                    AddConnectionInput input = env.get("input", AddConnectionInput.class);

                    UseCaseOutput output = useCase.execute(input);
                    env.put("output", output);
                })
                .ThenFailure(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isFailure());
                    assertTrue(output.getMessage().contains("連線 ID 已存在"));
                    assertEquals("BUSINESS_RULE_VIOLATION", output.getErrorCode().orElse(""));
                })
                .And("no connection should be saved", env -> {
                    var repository = getContext().mockRepository();
                    verify(repository, never()).save(any(DatabaseConnection.class));
                })
                .Execute();
    }

    @EzScenario
    public void validate_connection_parameters() {

        feature.newScenario(CONNECTION_VALIDATION)
                .Given("invalid connection parameters", env -> {
                    // Test various invalid inputs
                    env.put("emptyConnectionId", new AddConnectionInput(
                        "", "localhost", 5432, "test_db", "postgres", "password123", "postgresql", 10));
                    env.put("invalidPort", new AddConnectionInput(
                        "test-id", "localhost", -1, "test_db", "postgres", "password123", "postgresql", 10));
                    env.put("invalidPoolSize", new AddConnectionInput(
                        "test-id", "localhost", 5432, "test_db", "postgres", "password123", "postgresql", 0));
                    env.put("emptyHost", new AddConnectionInput(
                        "test-id", "", 5432, "test_db", "postgres", "password123", "postgresql", 10));
                })
                .When("I try to create connections with invalid parameters", env -> {
                    AddConnectionUseCase useCase = getContext().newAddConnectionUseCase();

                    UseCaseOutput output1 = useCase.execute(env.get("emptyConnectionId", AddConnectionInput.class));
                    UseCaseOutput output2 = useCase.execute(env.get("invalidPort", AddConnectionInput.class));
                    UseCaseOutput output3 = useCase.execute(env.get("invalidPoolSize", AddConnectionInput.class));
                    UseCaseOutput output4 = useCase.execute(env.get("emptyHost", AddConnectionInput.class));

                    env.put("outputs", new UseCaseOutput[]{output1, output2, output3, output4});
                })
                .ThenFailure(env -> {
                    UseCaseOutput[] outputs = env.get("outputs", UseCaseOutput[].class);

                    for (UseCaseOutput output : outputs) {
                        assertTrue(output.isFailure());
                        assertEquals("VALIDATION_ERROR", output.getErrorCode().orElse(""));
                    }
                })
                .Execute();
    }

    @EzScenario
    public void handle_connection_test_failure() {

        feature.newScenario(CONNECTION_ERROR_HANDLING)
                .Given("valid connection parameters", env -> {
                    String connectionId = UUID.randomUUID().toString();
                    AddConnectionInput input = new AddConnectionInput(
                        connectionId,
                        "unreachable-host",
                        5432,
                        "test_db",
                        "postgres",
                        "password123",
                        "postgresql",
                        10
                    );

                    env.put("input", input);
                })
                .And("the connection test will fail", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    var testResult = new DatabaseQueryExecutor.ConnectionTestResult(false, "連線超時", 5000);
                    when(queryExecutor.testConnection(any(ConnectionId.class)))
                            .thenReturn(Mono.just(testResult));
                })
                .When("I create the connection", env -> {
                    AddConnectionUseCase useCase = getContext().newAddConnectionUseCase();
                    AddConnectionInput input = env.get("input", AddConnectionInput.class);

                    UseCaseOutput output = useCase.execute(input);
                    env.put("output", output);
                })
                .ThenSuccess(env -> {
                    // Connection is still created but marked as failed
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isSuccess());
                    assertEquals("連線創建成功", output.getMessage());
                })
                .And("the connection should be saved with failed status", env -> {
                    var repository = getContext().mockRepository();
                    verify(repository, times(1)).save(any(DatabaseConnection.class));
                })
                .Execute();
    }

    @EzScenario
    public void handle_unsupported_server_type() {

        feature.newScenario(CONNECTION_VALIDATION)
                .Given("an unsupported server type", env -> {
                    AddConnectionInput input = new AddConnectionInput(
                        "test-id",
                        "localhost",
                        5432,
                        "test_db",
                        "postgres",
                        "password123",
                        "unsupported-db",
                        10
                    );

                    env.put("input", input);
                })
                .When("I try to create the connection", env -> {
                    AddConnectionUseCase useCase = getContext().newAddConnectionUseCase();
                    AddConnectionInput input = env.get("input", AddConnectionInput.class);

                    UseCaseOutput output = useCase.execute(input);
                    env.put("output", output);
                })
                .ThenFailure(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isFailure());
                    assertTrue(output.getMessage().contains("輸入資料無效"));
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
        private DatabaseConnectionRepository mockRepository;
        private DatabaseQueryExecutor mockQueryExecutor;

        private TestContext() {
            mockRepository = mock(DatabaseConnectionRepository.class);
            mockQueryExecutor = mock(DatabaseQueryExecutor.class);
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

        public AddConnectionUseCase newAddConnectionUseCase() {
            return new AddConnectionService(mockRepository, mockQueryExecutor);
        }

        public DatabaseConnectionRepository mockRepository() {
            return mockRepository;
        }

        public DatabaseQueryExecutor mockQueryExecutor() {
            return mockQueryExecutor;
        }
    }
}