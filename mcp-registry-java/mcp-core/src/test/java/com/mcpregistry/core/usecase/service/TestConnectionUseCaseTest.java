package com.mcpregistry.core.usecase.service;

import com.mcpregistry.core.entity.*;
import com.mcpregistry.core.usecase.port.in.connection.TestConnectionUseCase;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EzFeature
@EzFeatureReport
public class TestConnectionUseCaseTest {

    static String FEATURE_NAME = "Test Database Connection";
    static Feature feature;

    // === Structure Rules ===
    static final String CONNECTION_EXISTENCE = "Connection must exist before it can be tested";
    static final String CONNECTION_STATUS_UPDATE = "Connection status must be updated based on test results";
    static final String TEST_EXECUTION = "Connection test must be performed using query executor";

    // === Behavior Rules ===
    static final String SUCCESSFUL_CONNECTION_TEST = "Successful connection test marks connection as connected";
    static final String FAILED_CONNECTION_TEST = "Failed connection test marks connection as failed with error message";
    static final String NONEXISTENT_CONNECTION_HANDLING = "Testing non-existent connection returns appropriate error";

    @BeforeAll
    static void beforeAll() {
        feature = Feature.New(FEATURE_NAME);
        feature.initialize();

        // Create rules
        feature.NewRule(CONNECTION_EXISTENCE);
        feature.NewRule(CONNECTION_STATUS_UPDATE);
        feature.NewRule(TEST_EXECUTION);
        feature.NewRule(SUCCESSFUL_CONNECTION_TEST);
        feature.NewRule(FAILED_CONNECTION_TEST);
        feature.NewRule(NONEXISTENT_CONNECTION_HANDLING);
    }

    @BeforeEach
    void setUp() {
        TestContext.reset();
    }

    @EzScenario
    public void successfully_test_existing_connection() {

        feature.newScenario(SUCCESSFUL_CONNECTION_TEST)
                .Given("an existing database connection", env -> {
                    String connectionId = "test-connection-123";

                    // Create connection entity
                    ConnectionInfo connectionInfo = ConnectionInfo.builder()
                        .host("localhost")
                        .port(5432)
                        .database("test_db")
                        .username("postgres")
                        .password("password")
                        .serverType(ServerType.POSTGRESQL)
                        .poolSize(10)
                        .build();

                    DatabaseConnection connection = new DatabaseConnection(
                        ConnectionId.of(connectionId), connectionInfo);

                    var repository = getContext().mockRepository();
                    when(repository.findById(ConnectionId.of(connectionId)))
                        .thenReturn(Optional.of(connection));

                    env.put("connectionId", connectionId)
                        .put("connection", connection);
                })
                .And("the connection test will succeed", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    var testResult = new DatabaseQueryExecutor.ConnectionTestResult(true, "連線正常", 100);
                    when(queryExecutor.testConnection(any(ConnectionId.class)))
                        .thenReturn(Mono.just(testResult));
                })
                .When("I test the connection", env -> {
                    TestConnectionUseCase useCase = getContext().newTestConnectionUseCase();
                    String connectionId = env.gets("connectionId");

                    UseCaseOutput output = useCase.execute(connectionId);
                    env.put("output", output);
                })
                .ThenSuccess(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isSuccess());
                    assertEquals("連線測試成功", output.getMessage());
                    assertTrue(output.getData().isPresent());
                })
                .And("the connection should be marked as connected", env -> {
                    var repository = getContext().mockRepository();
                    verify(repository, atLeastOnce()).save(any(DatabaseConnection.class));
                })
                .And("the connection test should be executed", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    verify(queryExecutor, times(1)).testConnection(any(ConnectionId.class));
                })
                .Execute();
    }

    @EzScenario
    public void handle_failed_connection_test() {

        feature.newScenario(FAILED_CONNECTION_TEST)
                .Given("an existing database connection", env -> {
                    String connectionId = "test-connection-456";

                    ConnectionInfo connectionInfo = ConnectionInfo.builder()
                        .host("unreachable-host")
                        .port(5432)
                        .database("test_db")
                        .username("postgres")
                        .password("password")
                        .serverType(ServerType.POSTGRESQL)
                        .poolSize(10)
                        .build();

                    DatabaseConnection connection = new DatabaseConnection(
                        ConnectionId.of(connectionId), connectionInfo);

                    var repository = getContext().mockRepository();
                    when(repository.findById(ConnectionId.of(connectionId)))
                        .thenReturn(Optional.of(connection));

                    env.put("connectionId", connectionId)
                        .put("connection", connection);
                })
                .And("the connection test will fail", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    var testResult = new DatabaseQueryExecutor.ConnectionTestResult(false, "連線超時", 5000);
                    when(queryExecutor.testConnection(any(ConnectionId.class)))
                        .thenReturn(Mono.just(testResult));
                })
                .When("I test the connection", env -> {
                    TestConnectionUseCase useCase = getContext().newTestConnectionUseCase();
                    String connectionId = env.gets("connectionId");

                    UseCaseOutput output = useCase.execute(connectionId);
                    env.put("output", output);
                })
                .ThenFailure(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isFailure());
                    assertTrue(output.getMessage().contains("連線測試失敗"));
                    assertTrue(output.getMessage().contains("連線超時"));
                })
                .And("the connection should be marked as failed", env -> {
                    var repository = getContext().mockRepository();
                    verify(repository, atLeastOnce()).save(any(DatabaseConnection.class));
                })
                .Execute();
    }

    @EzScenario
    public void test_nonexistent_connection() {

        feature.newScenario(NONEXISTENT_CONNECTION_HANDLING)
                .Given("a non-existent connection ID", env -> {
                    String nonExistentId = "non-existent-connection";

                    var repository = getContext().mockRepository();
                    when(repository.findById(ConnectionId.of(nonExistentId)))
                        .thenReturn(Optional.empty());

                    env.put("connectionId", nonExistentId);
                })
                .When("I try to test the connection", env -> {
                    TestConnectionUseCase useCase = getContext().newTestConnectionUseCase();
                    String connectionId = env.gets("connectionId");

                    UseCaseOutput output = useCase.execute(connectionId);
                    env.put("output", output);
                })
                .ThenFailure(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isFailure());
                    assertTrue(output.getMessage().contains("連線不存在"));
                })
                .And("no connection test should be executed", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    verify(queryExecutor, never()).testConnection(any(ConnectionId.class));
                })
                .And("no connection should be saved", env -> {
                    var repository = getContext().mockRepository();
                    verify(repository, never()).save(any(DatabaseConnection.class));
                })
                .Execute();
    }

    @EzScenario
    public void validate_connection_id_parameter() {

        feature.newScenario(CONNECTION_EXISTENCE)
                .Given("invalid connection ID parameters", env -> {
                    // Test various invalid inputs
                    env.put("nullId", (String) null)
                            .put("emptyId", "")
                            .put("whitespaceId", "   ");
                })
                .When("I try to test connections with invalid IDs", env -> {
                    TestConnectionUseCase useCase = getContext().newTestConnectionUseCase();

                    UseCaseOutput output1 = useCase.execute(env.gets("nullId"));
                    UseCaseOutput output2 = useCase.execute(env.gets("emptyId"));
                    UseCaseOutput output3 = useCase.execute(env.gets("whitespaceId"));

                    env.put("outputs", new UseCaseOutput[]{output1, output2, output3});
                })
                .ThenFailure(env -> {
                    UseCaseOutput[] outputs = env.get("outputs", UseCaseOutput[].class);

                    for (UseCaseOutput output : outputs) {
                        assertTrue(output.isFailure());
                        assertEquals("VALIDATION_ERROR", output.getErrorCode().orElse(""));
                        assertTrue(output.getMessage().contains("連線 ID 不能為空"));
                    }
                })
                .Execute();
    }

    @EzScenario
    public void handle_query_executor_timeout() {

        feature.newScenario(TEST_EXECUTION)
                .Given("an existing database connection", env -> {
                    String connectionId = "test-connection-timeout";

                    ConnectionInfo connectionInfo = ConnectionInfo.builder()
                        .host("slow-host")
                        .port(5432)
                        .database("test_db")
                        .username("postgres")
                        .password("password")
                        .serverType(ServerType.POSTGRESQL)
                        .poolSize(10)
                        .build();

                    DatabaseConnection connection = new DatabaseConnection(
                        ConnectionId.of(connectionId), connectionInfo);

                    var repository = getContext().mockRepository();
                    when(repository.findById(ConnectionId.of(connectionId)))
                        .thenReturn(Optional.of(connection));

                    env.put("connectionId", connectionId);
                })
                .And("the query executor returns null (timeout)", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    when(queryExecutor.testConnection(any(ConnectionId.class)))
                        .thenReturn(Mono.empty());
                })
                .When("I test the connection", env -> {
                    TestConnectionUseCase useCase = getContext().newTestConnectionUseCase();
                    String connectionId = env.gets("connectionId");

                    UseCaseOutput output = useCase.execute(connectionId);
                    env.put("output", output);
                })
                .ThenFailure(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isFailure());
                    assertTrue(output.getMessage().contains("連線測試超時或失敗"));
                })
                .Execute();
    }

    @EzScenario
    public void handle_mysql_connection_test() {

        feature.newScenario(SUCCESSFUL_CONNECTION_TEST)
                .Given("an existing MySQL database connection", env -> {
                    String connectionId = "mysql-connection-123";

                    ConnectionInfo connectionInfo = ConnectionInfo.builder()
                        .host("localhost")
                        .port(3306)
                        .database("test_db")
                        .username("root")
                        .password("password")
                        .serverType(ServerType.MYSQL)
                        .poolSize(5)
                        .build();

                    DatabaseConnection connection = new DatabaseConnection(
                        ConnectionId.of(connectionId), connectionInfo);

                    var repository = getContext().mockRepository();
                    when(repository.findById(ConnectionId.of(connectionId)))
                        .thenReturn(Optional.of(connection));

                    env.put("connectionId", connectionId);
                })
                .And("the MySQL connection test will succeed", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    var testResult = new DatabaseQueryExecutor.ConnectionTestResult(true, "MySQL 連線正常", 150);
                    when(queryExecutor.testConnection(any(ConnectionId.class)))
                        .thenReturn(Mono.just(testResult));
                })
                .When("I test the MySQL connection", env -> {
                    TestConnectionUseCase useCase = getContext().newTestConnectionUseCase();
                    String connectionId = env.gets("connectionId");

                    UseCaseOutput output = useCase.execute(connectionId);
                    env.put("output", output);
                })
                .ThenSuccess(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isSuccess());
                    assertTrue(output.getData().isPresent());
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

        public TestConnectionUseCase newTestConnectionUseCase() {
            return new TestConnectionService(mockRepository, mockQueryExecutor);
        }

        public DatabaseConnectionRepository mockRepository() {
            return mockRepository;
        }

        public DatabaseQueryExecutor mockQueryExecutor() {
            return mockQueryExecutor;
        }
    }
}