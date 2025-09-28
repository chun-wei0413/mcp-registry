package com.mcp.postgresql.tool;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.postgresql.service.DatabaseConnectionService;
import com.mcpregistry.core.entity.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * PostgreSQL 查詢執行工具測試
 */
@ExtendWith(MockitoExtension.class)
class QueryExecutionToolTest {

    @Mock
    private DatabaseConnectionService connectionService;

    private QueryExecutionTool queryTool;

    @BeforeEach
    void setUp() {
        queryTool = new QueryExecutionTool(connectionService);
    }

    @Test
    void shouldReturnCorrectToolName() {
        assertEquals("postgresql_query_execution", queryTool.getToolName());
    }

    @Test
    void shouldReturnCorrectDescription() {
        String description = queryTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("PostgreSQL"));
        assertTrue(description.contains("查詢執行"));
    }

    @Test
    void shouldReturnValidParameterSchema() {
        Map<String, Object> schema = queryTool.getParameterSchema();
        assertNotNull(schema);
        assertTrue(schema.containsKey("type"));
        assertTrue(schema.containsKey("properties"));
        assertTrue(schema.containsKey("required"));
    }

    @Test
    void shouldExecuteSimpleQuerySuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("query-1")
            .rowCount(1)
            .columnNames(List.of("id", "name"))
            .rows(List.of(Map.of("id", 1, "name", "test")))
            .executionTimeMs(50)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-conn",
            "query", "SELECT * FROM users WHERE id = ?",
            "params", List.of(1)
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("查詢執行完成", result.getContent());
    }

    @Test
    void shouldExecuteQueryWithoutParametersSuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("query-2")
            .rowCount(5)
            .columnNames(List.of("count"))
            .rows(List.of(Map.of("count", 5)))
            .executionTimeMs(25)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-conn",
            "query", "SELECT COUNT(*) as count FROM users"
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("查詢執行完成", result.getContent());
    }

    @Test
    void shouldExecuteUpdateQuerySuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("query-3")
            .rowCount(1)
            .executionTimeMs(75)
            .affectedRows(1)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-conn",
            "query", "UPDATE users SET name = ? WHERE id = ?",
            "params", List.of("new_name", 1)
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("查詢執行完成", result.getContent());
    }

    @Test
    void shouldExecuteTransactionSuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("txn-1")
            .rowCount(0)
            .executionTimeMs(100)
            .isTransactionSuccessful(true)
            .build();

        when(connectionService.executeTransaction(anyString(), anyList()))
            .thenReturn(mockResult);

        List<Map<String, Object>> queries = List.of(
            Map.of("query", "INSERT INTO users (name) VALUES (?)", "params", List.of("user1")),
            Map.of("query", "INSERT INTO users (name) VALUES (?)", "params", List.of("user2"))
        );

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-conn",
            "queries", queries
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("事務執行完成", result.getContent());
    }

    @Test
    void shouldExecuteBatchQuerySuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("batch-1")
            .rowCount(3)
            .executionTimeMs(150)
            .affectedRows(3)
            .build();

        when(connectionService.executeBatch(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        List<List<Object>> paramsList = List.of(
            List.of("user1"),
            List.of("user2"),
            List.of("user3")
        );

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-conn",
            "query", "INSERT INTO users (name) VALUES (?)",
            "paramsList", paramsList
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("批次執行完成", result.getContent());
    }

    @Test
    void shouldFailWhenConnectionIdIsEmpty() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "connectionId", "",
            "query", "SELECT 1"
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("Connection ID 不能為空", result.getError());
    }

    @Test
    void shouldFailWhenQueryIsEmpty() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "connectionId", "test-conn",
            "query", ""
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("查詢語句不能為空", result.getError());
    }

    @Test
    void shouldFailWhenConnectionNotFound() {
        // Arrange
        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenThrow(new RuntimeException("Connection not found"));

        Map<String, Object> arguments = Map.of(
            "connectionId", "non-existent",
            "query", "SELECT 1"
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Connection not found"));
    }

    @Test
    void shouldFailWhenSqlSyntaxError() {
        // Arrange
        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenThrow(new RuntimeException("SQL syntax error"));

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-conn",
            "query", "INVALID SQL STATEMENT"
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("SQL syntax error"));
    }

    @Test
    void shouldHandleQueryWithFetchLimit() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("query-4")
            .rowCount(10)
            .columnNames(List.of("id", "name"))
            .executionTimeMs(30)
            .hasMoreRows(true)
            .build();

        when(connectionService.executeQueryWithLimit(anyString(), anyString(), anyList(), anyInt()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-conn",
            "query", "SELECT * FROM users",
            "fetchSize", 10
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("查詢執行完成", result.getContent());
    }

    @Test
    void shouldValidateParameterTypes() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "connectionId", "test-conn",
            "query", "SELECT * FROM users WHERE id = ? AND active = ?",
            "params", List.of(1, true)
        );

        QueryResult mockResult = QueryResult.builder()
            .queryId("query-5")
            .rowCount(1)
            .columnNames(List.of("id", "name", "active"))
            .executionTimeMs(40)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldHandleNullParameters() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "connectionId", "test-conn",
            "query", "SELECT * FROM users WHERE description = ?",
            "params", List.of((Object) null)
        );

        QueryResult mockResult = QueryResult.builder()
            .queryId("query-6")
            .rowCount(2)
            .columnNames(List.of("id", "name", "description"))
            .executionTimeMs(35)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
    }
}