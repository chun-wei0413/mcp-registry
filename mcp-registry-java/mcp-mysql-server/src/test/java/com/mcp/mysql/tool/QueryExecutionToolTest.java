package com.mcp.mysql.tool;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.mysql.service.DatabaseConnectionService;
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
 * MySQL 查詢執行工具測試
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
        assertEquals("mysql_query_execution", queryTool.getToolName());
    }

    @Test
    void shouldReturnCorrectDescription() {
        String description = queryTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("MySQL"));
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
    void shouldExecuteSimpleSelectQuerySuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-query-1")
            .rowCount(1)
            .columnNames(List.of("id", "name", "email"))
            .rows(List.of(Map.of("id", 1, "name", "Alice", "email", "alice@example.com")))
            .executionTimeMs(45)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "SELECT * FROM users WHERE id = ?",
            "params", List.of(1)
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 查詢執行完成", result.getContent());
    }

    @Test
    void shouldExecuteInsertQuerySuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-insert-1")
            .rowCount(0)
            .affectedRows(1)
            .executionTimeMs(35)
            .generatedKeys(List.of(Map.of("GENERATED_KEY", 123)))
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "INSERT INTO users (name, email) VALUES (?, ?)",
            "params", List.of("Bob", "bob@example.com")
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 查詢執行完成", result.getContent());
    }

    @Test
    void shouldExecuteUpdateQuerySuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-update-1")
            .rowCount(0)
            .affectedRows(2)
            .executionTimeMs(55)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "UPDATE users SET active = ? WHERE created_at < ?",
            "params", List.of(false, "2023-01-01")
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 查詢執行完成", result.getContent());
    }

    @Test
    void shouldExecuteDeleteQuerySuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-delete-1")
            .rowCount(0)
            .affectedRows(3)
            .executionTimeMs(40)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "DELETE FROM users WHERE active = ?",
            "params", List.of(false)
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 查詢執行完成", result.getContent());
    }

    @Test
    void shouldExecuteAggregateQuerySuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-aggregate-1")
            .rowCount(1)
            .columnNames(List.of("total_users", "avg_age", "max_created"))
            .rows(List.of(Map.of(
                "total_users", 150,
                "avg_age", 32.5,
                "max_created", "2024-01-15 10:30:45"
            )))
            .executionTimeMs(25)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "SELECT COUNT(*) as total_users, AVG(age) as avg_age, MAX(created_at) as max_created FROM users"
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 查詢執行完成", result.getContent());
    }

    @Test
    void shouldExecuteJoinQuerySuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-join-1")
            .rowCount(5)
            .columnNames(List.of("user_name", "order_id", "amount", "status"))
            .rows(List.of(
                Map.of("user_name", "Alice", "order_id", 1, "amount", 99.99, "status", "completed"),
                Map.of("user_name", "Alice", "order_id", 2, "amount", 149.99, "status", "pending"),
                Map.of("user_name", "Bob", "order_id", 3, "amount", 79.99, "status", "completed")
            ))
            .executionTimeMs(75)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", """
                SELECT u.name as user_name, o.id as order_id, o.amount, o.status
                FROM users u
                INNER JOIN orders o ON u.id = o.user_id
                WHERE o.created_at >= ?
                """,
            "params", List.of("2024-01-01")
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 查詢執行完成", result.getContent());
    }

    @Test
    void shouldExecuteTransactionSuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-txn-1")
            .rowCount(0)
            .executionTimeMs(120)
            .isTransactionSuccessful(true)
            .affectedRows(3)
            .build();

        when(connectionService.executeTransaction(anyString(), anyList()))
            .thenReturn(mockResult);

        List<Map<String, Object>> queries = List.of(
            Map.of("query", "INSERT INTO users (name, email) VALUES (?, ?)", "params", List.of("User1", "user1@example.com")),
            Map.of("query", "INSERT INTO users (name, email) VALUES (?, ?)", "params", List.of("User2", "user2@example.com")),
            Map.of("query", "UPDATE user_stats SET total_users = total_users + 2")
        );

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "queries", queries
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 事務執行完成", result.getContent());
    }

    @Test
    void shouldExecuteBatchQuerySuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-batch-1")
            .rowCount(0)
            .executionTimeMs(180)
            .affectedRows(5)
            .build();

        when(connectionService.executeBatch(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        List<List<Object>> paramsList = List.of(
            List.of("Product1", 19.99, "category1"),
            List.of("Product2", 29.99, "category1"),
            List.of("Product3", 39.99, "category2"),
            List.of("Product4", 49.99, "category2"),
            List.of("Product5", 59.99, "category3")
        );

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "INSERT INTO products (name, price, category) VALUES (?, ?, ?)",
            "paramsList", paramsList
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 批次執行完成", result.getContent());
    }

    @Test
    void shouldExecuteStoredProcedureSuccessfully() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-proc-1")
            .rowCount(2)
            .columnNames(List.of("user_id", "total_orders", "total_amount"))
            .rows(List.of(
                Map.of("user_id", 1, "total_orders", 5, "total_amount", 499.95),
                Map.of("user_id", 2, "total_orders", 3, "total_amount", 299.97)
            ))
            .executionTimeMs(95)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "CALL GetUserOrderStats(?)",
            "params", List.of("2024-01-01")
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 查詢執行完成", result.getContent());
    }

    @Test
    void shouldHandleQueryWithLimit() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-limit-1")
            .rowCount(10)
            .columnNames(List.of("id", "name", "created_at"))
            .executionTimeMs(30)
            .hasMoreRows(true)
            .build();

        when(connectionService.executeQueryWithLimit(anyString(), anyString(), anyList(), anyInt()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "SELECT * FROM users ORDER BY created_at DESC",
            "fetchSize", 10
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 查詢執行完成", result.getContent());
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
            "connectionId", "test-mysql-conn",
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
            .thenThrow(new RuntimeException("Connection 'non-existent' not found"));

        Map<String, Object> arguments = Map.of(
            "connectionId", "non-existent",
            "query", "SELECT 1"
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not found"));
    }

    @Test
    void shouldFailWhenSqlSyntaxError() {
        // Arrange
        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenThrow(new RuntimeException("You have an error in your SQL syntax"));

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "SELCT * FORM users"  // 故意的語法錯誤
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("SQL syntax"));
    }

    @Test
    void shouldFailWhenTableNotFound() {
        // Arrange
        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenThrow(new RuntimeException("Table 'testdb.non_existent_table' doesn't exist"));

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "SELECT * FROM non_existent_table"
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("doesn't exist"));
    }

    @Test
    void shouldFailWhenForeignKeyConstraintViolation() {
        // Arrange
        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenThrow(new RuntimeException("Cannot add or update a child row: a foreign key constraint fails"));

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "INSERT INTO orders (user_id, amount) VALUES (?, ?)",
            "params", List.of(999, 100.0)  // 不存在的 user_id
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("foreign key constraint"));
    }

    @Test
    void shouldFailWhenUniqueConstraintViolation() {
        // Arrange
        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenThrow(new RuntimeException("Duplicate entry 'alice@example.com' for key 'email'"));

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "INSERT INTO users (name, email) VALUES (?, ?)",
            "params", List.of("Alice", "alice@example.com")  // 重複的 email
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Duplicate entry"));
    }

    @Test
    void shouldHandleNullParametersCorrectly() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-null-1")
            .rowCount(2)
            .columnNames(List.of("id", "name", "description"))
            .rows(List.of(
                Map.of("id", 1, "name", "Product1", "description", null),
                Map.of("id", 2, "name", "Product2", "description", null)
            ))
            .executionTimeMs(35)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "SELECT * FROM products WHERE description IS NULL OR description = ?",
            "params", List.of((Object) null)
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void shouldHandleDateTimeParametersCorrectly() {
        // Arrange
        QueryResult mockResult = QueryResult.builder()
            .queryId("mysql-datetime-1")
            .rowCount(3)
            .columnNames(List.of("id", "name", "created_at"))
            .executionTimeMs(40)
            .build();

        when(connectionService.executeQuery(anyString(), anyString(), anyList()))
            .thenReturn(mockResult);

        Map<String, Object> arguments = Map.of(
            "connectionId", "test-mysql-conn",
            "query", "SELECT * FROM users WHERE created_at BETWEEN ? AND ?",
            "params", List.of("2024-01-01 00:00:00", "2024-12-31 23:59:59")
        );

        // Act
        McpToolResult result = queryTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }
}