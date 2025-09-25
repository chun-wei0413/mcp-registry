package com.mcpregistry.core.usecase.service;

import com.mcpregistry.core.entity.*;
import com.mcpregistry.core.usecase.port.in.query.ExecuteQueryInput;
import com.mcpregistry.core.usecase.port.in.query.ExecuteQueryUseCase;
import com.mcpregistry.core.usecase.port.common.UseCaseOutput;
import com.mcpregistry.core.usecase.port.out.DatabaseConnectionRepository;
import com.mcpregistry.core.usecase.port.out.DatabaseQueryExecutor;
import com.mcpregistry.core.usecase.port.out.QueryExecutionRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import tw.teddysoft.ezspec.EzFeature;
import tw.teddysoft.ezspec.EzFeatureReport;
import tw.teddysoft.ezspec.extension.junit5.EzScenario;
import tw.teddysoft.ezspec.keyword.Feature;
import tw.teddysoft.ezspec.visitor.PlainTextReport;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@EzFeature
@EzFeatureReport
public class ExecuteQueryUseCaseTest {

    static String FEATURE_NAME = "Execute Database Query";
    static Feature feature;

    // === Structure Rules ===
    static final String QUERY_VALIDATION = "Query must be valid and safe before execution";
    static final String CONNECTION_AVAILABILITY = "Connection must exist and be available for query execution";
    static final String QUERY_EXECUTION_TRACKING = "All query executions must be tracked and logged";

    // === Behavior Rules ===
    static final String SELECT_QUERY_EXECUTION = "SELECT queries return data and update connection access time";
    static final String UPDATE_QUERY_EXECUTION = "UPDATE queries return affected rows count and are properly logged";
    static final String QUERY_EXECUTION_FAILURE_HANDLING = "Failed queries are properly logged with error details";
    static final String INVALID_CONNECTION_HANDLING = "Invalid or unavailable connections are rejected";

    @BeforeAll
    static void beforeAll() {
        feature = Feature.New(FEATURE_NAME);
        feature.initialize();

        // Create rules
        feature.NewRule(QUERY_VALIDATION);
        feature.NewRule(CONNECTION_AVAILABILITY);
        feature.NewRule(QUERY_EXECUTION_TRACKING);
        feature.NewRule(SELECT_QUERY_EXECUTION);
        feature.NewRule(UPDATE_QUERY_EXECUTION);
        feature.NewRule(QUERY_EXECUTION_FAILURE_HANDLING);
        feature.NewRule(INVALID_CONNECTION_HANDLING);
    }

    @BeforeEach
    void setUp() {
        TestContext.reset();
    }

    @EzScenario
    public void successfully_execute_select_query() {

        feature.newScenario(SELECT_QUERY_EXECUTION)
                .Given("an available database connection", env -> {
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
                    connection.markConnected();

                    var repository = getContext().mockConnectionRepository();
                    when(repository.findById(ConnectionId.of(connectionId)))
                        .thenReturn(Optional.of(connection));

                    env.put("connectionId", connectionId)
                        .put("connection", connection);
                })
                .And("a valid SELECT query", env -> {
                    ExecuteQueryInput input = new ExecuteQueryInput(
                        env.gets("connectionId"),
                        "SELECT * FROM users WHERE id = ?",
                        List.of(1)
                    );

                    env.put("input", input);
                })
                .And("the query execution will succeed", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    List<Map<String, Object>> queryResultData = List.of(
                        Map.of("id", (Object) 1, "name", (Object) "John Doe", "email", (Object) "john@example.com")
                    );
                    var queryResult = new DatabaseQueryExecutor.QueryResult(
                        queryResultData, List.of("id", "name", "email"), 1, 100
                    );
                    when(queryExecutor.executeQuery(any(ConnectionId.class), anyString(), any(List.class)))
                        .thenReturn(Mono.just(queryResult));
                })
                .When("I execute the query", env -> {
                    ExecuteQueryUseCase useCase = getContext().newExecuteQueryUseCase();
                    ExecuteQueryInput input = env.get("input", ExecuteQueryInput.class);

                    UseCaseOutput output = useCase.execute(input);
                    env.put("output", output);
                })
                .ThenSuccess(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isSuccess());
                    assertEquals("查詢執行成功", output.getMessage());
                    assertTrue(output.getData().isPresent());
                })
                .And("the query should be executed", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    verify(queryExecutor, atLeastOnce())
                        .executeQuery(any(ConnectionId.class), anyString(), any(List.class));
                })
                .And("the query execution should be tracked", env -> {
                    var queryExecutionRepository = getContext().mockQueryExecutionRepository();
                    verify(queryExecutionRepository, atLeastOnce()).save(any(QueryExecution.class));
                })
                .And("the connection last access time should be updated", env -> {
                    var connectionRepository = getContext().mockConnectionRepository();
                    verify(connectionRepository, atLeastOnce()).save(any(DatabaseConnection.class));
                })
                .Execute();
    }

    @EzScenario
    public void successfully_execute_update_query() {

        feature.newScenario(UPDATE_QUERY_EXECUTION)
                .Given("an available database connection", env -> {
                    String connectionId = "test-connection-456";

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
                    connection.markConnected();

                    var repository = getContext().mockConnectionRepository();
                    when(repository.findById(ConnectionId.of(connectionId)))
                        .thenReturn(Optional.of(connection));

                    env.put("connectionId", connectionId)
                        .put("connection", connection);
                })
                .And("a valid UPDATE query", env -> {
                    ExecuteQueryInput input = new ExecuteQueryInput(
                        env.gets("connectionId"),
                        "UPDATE users SET email = ? WHERE id = ?",
                        List.of("new@example.com", 1)
                    );

                    env.put("input", input);
                })
                .And("the update will affect 1 row", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    when(queryExecutor.executeUpdate(any(ConnectionId.class), anyString(), any(List.class)))
                        .thenReturn(Mono.just(1));
                })
                .When("I execute the update query", env -> {
                    ExecuteQueryUseCase useCase = getContext().newExecuteQueryUseCase();
                    ExecuteQueryInput input = env.get("input", ExecuteQueryInput.class);

                    UseCaseOutput output = useCase.execute(input);
                    env.put("output", output);
                })
                .ThenSuccess(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isSuccess());
                    assertEquals("更新執行成功", output.getMessage());
                })
                .And("the update should be executed", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    verify(queryExecutor, atLeastOnce())
                        .executeUpdate(any(ConnectionId.class), anyString(), any(List.class));
                })
                .Execute();
    }

    @EzScenario
    public void reject_query_on_nonexistent_connection() {

        feature.newScenario(INVALID_CONNECTION_HANDLING)
                .Given("a non-existent connection ID", env -> {
                    String nonExistentId = "non-existent-connection";

                    var repository = getContext().mockConnectionRepository();
                    when(repository.findById(ConnectionId.of(nonExistentId)))
                        .thenReturn(Optional.empty());

                    ExecuteQueryInput input = new ExecuteQueryInput(
                        nonExistentId,
                        "SELECT * FROM users",
                        List.of()
                    );

                    env.put("input", input);
                })
                .When("I try to execute a query", env -> {
                    ExecuteQueryUseCase useCase = getContext().newExecuteQueryUseCase();
                    ExecuteQueryInput input = env.get("input", ExecuteQueryInput.class);

                    UseCaseOutput output = useCase.execute(input);
                    env.put("output", output);
                })
                .ThenFailure(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isFailure());
                    assertTrue(output.getMessage().contains("連線不存在"));
                    assertEquals("BUSINESS_RULE_VIOLATION", output.getErrorCode().orElse(""));
                })
                .And("no query should be executed", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    verify(queryExecutor, never())
                        .executeQuery(any(ConnectionId.class), anyString(), any(List.class));
                })
                .Execute();
    }

    @EzScenario
    public void reject_query_on_unavailable_connection() {

        feature.newScenario(INVALID_CONNECTION_HANDLING)
                .Given("an unavailable database connection", env -> {
                    String connectionId = "unavailable-connection";

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
                    // Mark connection as failed to make it unavailable
                    connection.markConnectionFailed("Connection failed");

                    var repository = getContext().mockConnectionRepository();
                    when(repository.findById(ConnectionId.of(connectionId)))
                        .thenReturn(Optional.of(connection));

                    ExecuteQueryInput input = new ExecuteQueryInput(
                        connectionId,
                        "SELECT * FROM users",
                        List.of()
                    );

                    env.put("input", input);
                })
                .When("I try to execute a query", env -> {
                    ExecuteQueryUseCase useCase = getContext().newExecuteQueryUseCase();
                    ExecuteQueryInput input = env.get("input", ExecuteQueryInput.class);

                    UseCaseOutput output = useCase.execute(input);
                    env.put("output", output);
                })
                .ThenFailure(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isFailure());
                    assertTrue(output.getMessage().contains("連線不可用"));
                    assertEquals("BUSINESS_RULE_VIOLATION", output.getErrorCode().orElse(""));
                })
                .Execute();
    }

    @EzScenario
    public void validate_query_input_parameters() {

        feature.newScenario(QUERY_VALIDATION)
                .Given("invalid query input parameters", env -> {
                    // Test various invalid inputs
                    ExecuteQueryInput nullInput = null;
                    ExecuteQueryInput emptyConnectionId = new ExecuteQueryInput("", "SELECT 1", List.of());
                    ExecuteQueryInput emptyQuery = new ExecuteQueryInput("conn-1", "", List.of());
                    ExecuteQueryInput nullQuery = new ExecuteQueryInput("conn-1", null, List.of());

                    env.put("inputs", new ExecuteQueryInput[]{
                        nullInput, emptyConnectionId, emptyQuery, nullQuery
                    });
                })
                .When("I try to execute queries with invalid parameters", env -> {
                    ExecuteQueryUseCase useCase = getContext().newExecuteQueryUseCase();
                    ExecuteQueryInput[] inputs = env.get("inputs", ExecuteQueryInput[].class);

                    UseCaseOutput[] outputs = new UseCaseOutput[inputs.length];
                    for (int i = 0; i < inputs.length; i++) {
                        outputs[i] = useCase.execute(inputs[i]);
                    }

                    env.put("outputs", outputs);
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
    public void handle_query_execution_failure() {

        feature.newScenario(QUERY_EXECUTION_FAILURE_HANDLING)
                .Given("an available database connection", env -> {
                    String connectionId = "test-connection-error";

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
                    connection.markConnected();

                    var repository = getContext().mockConnectionRepository();
                    when(repository.findById(ConnectionId.of(connectionId)))
                        .thenReturn(Optional.of(connection));

                    env.put("connectionId", connectionId);
                })
                .And("a query that will fail", env -> {
                    ExecuteQueryInput input = new ExecuteQueryInput(
                        env.gets("connectionId"),
                        "SELECT * FROM non_existent_table",
                        List.of()
                    );

                    env.put("input", input);
                })
                .And("the query executor will throw an exception", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    when(queryExecutor.executeQuery(any(ConnectionId.class), anyString(), any(List.class)))
                        .thenReturn(Mono.error(new RuntimeException("Table 'non_existent_table' doesn't exist")));
                })
                .When("I execute the failing query", env -> {
                    ExecuteQueryUseCase useCase = getContext().newExecuteQueryUseCase();
                    ExecuteQueryInput input = env.get("input", ExecuteQueryInput.class);

                    UseCaseOutput output = useCase.execute(input);
                    env.put("output", output);
                })
                .ThenFailure(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isFailure());
                    assertTrue(output.getMessage().contains("查詢執行失敗"));
                })
                .And("the failed query should be logged", env -> {
                    var queryExecutionRepository = getContext().mockQueryExecutionRepository();
                    // Should save: initial, started, and failed states
                    verify(queryExecutionRepository, atLeastOnce()).save(any(QueryExecution.class));
                })
                .Execute();
    }

    @EzScenario
    public void execute_parameterized_query() {

        feature.newScenario(SELECT_QUERY_EXECUTION)
                .Given("an available connection and parameterized query", env -> {
                    String connectionId = "param-query-connection";

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
                    connection.markConnected();

                    var repository = getContext().mockConnectionRepository();
                    when(repository.findById(ConnectionId.of(connectionId)))
                        .thenReturn(Optional.of(connection));

                    ExecuteQueryInput input = new ExecuteQueryInput(
                        connectionId,
                        "SELECT * FROM orders WHERE user_id = ? AND status = ? ORDER BY created_at DESC",
                        List.of(123, "COMPLETED")
                    );

                    env.put("input", input);
                })
                .And("the parameterized query will return results", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    List<Map<String, Object>> queryResultData = List.of(
                        Map.of("id", (Object) 1, "user_id", (Object) 123, "status", (Object) "COMPLETED", "amount", (Object) 99.99),
                        Map.of("id", (Object) 2, "user_id", (Object) 123, "status", (Object) "COMPLETED", "amount", (Object) 149.50)
                    );
                    var queryResult = new DatabaseQueryExecutor.QueryResult(
                        queryResultData, List.of("id", "user_id", "status", "amount"), 2, 200
                    );
                    when(queryExecutor.executeQuery(any(ConnectionId.class), anyString(), any(List.class)))
                        .thenReturn(Mono.just(queryResult));
                })
                .When("I execute the parameterized query", env -> {
                    ExecuteQueryUseCase useCase = getContext().newExecuteQueryUseCase();
                    ExecuteQueryInput input = env.get("input", ExecuteQueryInput.class);

                    UseCaseOutput output = useCase.execute(input);
                    env.put("output", output);
                })
                .ThenSuccess(env -> {
                    UseCaseOutput output = env.get("output", UseCaseOutput.class);
                    assertTrue(output.isSuccess());
                    assertTrue(output.getData().isPresent());
                })
                .And("the correct parameters should be passed", env -> {
                    var queryExecutor = getContext().mockQueryExecutor();
                    verify(queryExecutor, atLeastOnce())
                        .executeQuery(
                            any(ConnectionId.class),
                            eq("SELECT * FROM orders WHERE user_id = ? AND status = ? ORDER BY created_at DESC"),
                            eq(List.of(123, "COMPLETED"))
                        );
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
        private DatabaseConnectionRepository mockConnectionRepository;
        private QueryExecutionRepository mockQueryExecutionRepository;
        private DatabaseQueryExecutor mockQueryExecutor;

        private TestContext() {
            mockConnectionRepository = mock(DatabaseConnectionRepository.class);
            mockQueryExecutionRepository = mock(QueryExecutionRepository.class);
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

        public ExecuteQueryUseCase newExecuteQueryUseCase() {
            return new ExecuteQueryService(
                mockConnectionRepository,
                mockQueryExecutionRepository,
                mockQueryExecutor
            );
        }

        public DatabaseConnectionRepository mockConnectionRepository() {
            return mockConnectionRepository;
        }

        public QueryExecutionRepository mockQueryExecutionRepository() {
            return mockQueryExecutionRepository;
        }

        public DatabaseQueryExecutor mockQueryExecutor() {
            return mockQueryExecutor;
        }
    }
}