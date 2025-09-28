package com.mcp.postgresql.tool;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.postgresql.service.DatabaseConnectionService;
import com.mcpregistry.core.entity.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * PostgreSQL 查詢執行工具 BDD 測試
 *
 * 功能：作為開發者，我希望能夠執行各種 PostgreSQL 查詢
 * 以便我可以讀取、插入、更新和刪除資料，並執行複雜的事務操作
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostgreSQL 查詢執行工具")
class QueryExecutionToolBDDTest {

    @Mock
    private DatabaseConnectionService connectionService;

    private QueryExecutionTool queryTool;

    @BeforeEach
    void setUp() {
        queryTool = new QueryExecutionTool(connectionService);
    }

    @Nested
    @DisplayName("工具基本資訊")
    class ToolBasicInformation {

        @Test
        @DisplayName("場景：驗證工具名稱")
        void shouldReturnCorrectToolName() {
            // Given: PostgreSQL 查詢執行工具已初始化
            // When: 請求工具名稱
            String toolName = queryTool.getToolName();

            // Then: 應該返回正確的工具名稱
            assertEquals("postgresql_query_execution", toolName);
        }

        @Test
        @DisplayName("場景：驗證工具描述")
        void shouldReturnCorrectDescription() {
            // Given: PostgreSQL 查詢執行工具已初始化
            // When: 請求工具描述
            String description = queryTool.getDescription();

            // Then: 描述應該包含 PostgreSQL 和查詢執行關鍵字
            assertNotNull(description);
            assertTrue(description.contains("PostgreSQL"));
            assertTrue(description.contains("查詢執行"));
        }
    }

    @Nested
    @DisplayName("SELECT 查詢操作")
    class SelectQueryOperations {

        @Test
        @DisplayName("場景：執行簡單的 SELECT 查詢")
        void shouldExecuteSimpleSelectQuery() {
            // Given: 資料庫中存在用戶資料
            QueryResult mockResult = QueryResult.builder()
                .queryId("query-1")
                .rowCount(1)
                .columnNames(List.of("id", "name", "email"))
                .rows(List.of(Map.of("id", 1, "name", "Alice", "email", "alice@example.com")))
                .executionTimeMs(25)
                .build();

            when(connectionService.executeQuery(anyString(), anyString(), anyList()))
                .thenReturn(mockResult);

            // And: 提供有效的查詢參數
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", "SELECT * FROM users WHERE id = ?",
                "params", List.of(1)
            );

            // When: 執行 SELECT 查詢
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該成功返回查詢結果
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals("查詢執行完成", result.getContent());
        }

        @Test
        @DisplayName("場景：執行聚合查詢")
        void shouldExecuteAggregateQuery() {
            // Given: 資料庫中存在多筆用戶資料
            QueryResult mockResult = QueryResult.builder()
                .queryId("query-2")
                .rowCount(1)
                .columnNames(List.of("total_users", "avg_age"))
                .rows(List.of(Map.of("total_users", 150, "avg_age", 32.5)))
                .executionTimeMs(35)
                .build();

            when(connectionService.executeQuery(anyString(), anyString(), anyList()))
                .thenReturn(mockResult);

            // And: 提供聚合查詢
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", "SELECT COUNT(*) as total_users, AVG(age) as avg_age FROM users"
            );

            // When: 執行聚合查詢
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該成功返回聚合結果
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：執行帶分頁的查詢")
        void shouldExecuteQueryWithPagination() {
            // Given: 設定分頁查詢的模擬結果
            QueryResult mockResult = QueryResult.builder()
                .queryId("query-3")
                .rowCount(10)
                .columnNames(List.of("id", "name", "created_at"))
                .executionTimeMs(20)
                .hasMoreRows(true)
                .build();

            when(connectionService.executeQueryWithLimit(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn(mockResult);

            // And: 提供分頁查詢參數
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", "SELECT * FROM users ORDER BY created_at DESC",
                "fetchSize", 10
            );

            // When: 執行分頁查詢
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該成功返回分頁結果
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }
    }

    @Nested
    @DisplayName("INSERT 操作")
    class InsertOperations {

        @Test
        @DisplayName("場景：插入新記錄")
        void shouldInsertNewRecord() {
            // Given: 資料庫可以接受新記錄
            QueryResult mockResult = QueryResult.builder()
                .queryId("insert-1")
                .rowCount(0)
                .affectedRows(1)
                .executionTimeMs(15)
                .build();

            when(connectionService.executeQuery(anyString(), anyString(), anyList()))
                .thenReturn(mockResult);

            // And: 提供插入資料的參數
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", "INSERT INTO users (name, email, age) VALUES (?, ?, ?)",
                "params", List.of("Bob", "bob@example.com", 30)
            );

            // When: 執行插入操作
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該成功插入記錄
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：批次插入多筆記錄")
        void shouldExecuteBatchInsert() {
            // Given: 資料庫支援批次插入
            QueryResult mockResult = QueryResult.builder()
                .queryId("batch-1")
                .rowCount(0)
                .affectedRows(3)
                .executionTimeMs(45)
                .build();

            when(connectionService.executeBatch(anyString(), anyString(), anyList()))
                .thenReturn(mockResult);

            // And: 提供批次插入的資料
            List<List<Object>> paramsList = List.of(
                List.of("User1", "user1@example.com", 25),
                List.of("User2", "user2@example.com", 28),
                List.of("User3", "user3@example.com", 32)
            );

            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", "INSERT INTO users (name, email, age) VALUES (?, ?, ?)",
                "paramsList", paramsList
            );

            // When: 執行批次插入
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該成功批次插入所有記錄
            assertTrue(result.isSuccess());
            assertEquals("批次執行完成", result.getContent());
        }
    }

    @Nested
    @DisplayName("UPDATE 操作")
    class UpdateOperations {

        @Test
        @DisplayName("場景：更新現有記錄")
        void shouldUpdateExistingRecord() {
            // Given: 資料庫中存在可更新的記錄
            QueryResult mockResult = QueryResult.builder()
                .queryId("update-1")
                .rowCount(0)
                .affectedRows(1)
                .executionTimeMs(20)
                .build();

            when(connectionService.executeQuery(anyString(), anyString(), anyList()))
                .thenReturn(mockResult);

            // And: 提供更新操作的參數
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", "UPDATE users SET age = ? WHERE name = ?",
                "params", List.of(31, "Bob")
            );

            // When: 執行更新操作
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該成功更新記錄
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：批量更新多筆記錄")
        void shouldUpdateMultipleRecords() {
            // Given: 資料庫中存在多筆可更新的記錄
            QueryResult mockResult = QueryResult.builder()
                .queryId("update-2")
                .rowCount(0)
                .affectedRows(5)
                .executionTimeMs(30)
                .build();

            when(connectionService.executeQuery(anyString(), anyString(), anyList()))
                .thenReturn(mockResult);

            // And: 提供批量更新的條件
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", "UPDATE users SET active = ? WHERE created_at < ?",
                "params", List.of(false, "2023-01-01")
            );

            // When: 執行批量更新
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該成功更新多筆記錄
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }
    }

    @Nested
    @DisplayName("DELETE 操作")
    class DeleteOperations {

        @Test
        @DisplayName("場景：刪除指定記錄")
        void shouldDeleteSpecificRecord() {
            // Given: 資料庫中存在可刪除的記錄
            QueryResult mockResult = QueryResult.builder()
                .queryId("delete-1")
                .rowCount(0)
                .affectedRows(1)
                .executionTimeMs(18)
                .build();

            when(connectionService.executeQuery(anyString(), anyString(), anyList()))
                .thenReturn(mockResult);

            // And: 提供刪除操作的條件
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", "DELETE FROM users WHERE email = ?",
                "params", List.of("old_user@example.com")
            );

            // When: 執行刪除操作
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該成功刪除記錄
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }
    }

    @Nested
    @DisplayName("事務操作")
    class TransactionOperations {

        @Test
        @DisplayName("場景：執行成功的事務")
        void shouldExecuteSuccessfulTransaction() {
            // Given: 資料庫支援事務操作
            QueryResult mockResult = QueryResult.builder()
                .queryId("txn-1")
                .rowCount(0)
                .executionTimeMs(80)
                .isTransactionSuccessful(true)
                .build();

            when(connectionService.executeTransaction(anyString(), anyList()))
                .thenReturn(mockResult);

            // And: 提供一組相關的操作
            List<Map<String, Object>> queries = List.of(
                Map.of("query", "INSERT INTO users (name, email) VALUES (?, ?)",
                      "params", List.of("Alice", "alice@example.com")),
                Map.of("query", "INSERT INTO user_profiles (user_id, preferences) VALUES (?, ?)",
                      "params", List.of(1, "dark_mode"))
            );

            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "queries", queries
            );

            // When: 執行事務操作
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該成功完成整個事務
            assertTrue(result.isSuccess());
            assertEquals("事務執行完成", result.getContent());
        }

        @Test
        @DisplayName("場景：事務中的錯誤應該回滾")
        void shouldRollbackOnTransactionError() {
            // Given: 事務執行過程中發生錯誤
            when(connectionService.executeTransaction(anyString(), anyList()))
                .thenThrow(new RuntimeException("約束違反"));

            // And: 提供會導致錯誤的操作組合
            List<Map<String, Object>> queries = List.of(
                Map.of("query", "INSERT INTO users (name, email) VALUES (?, ?)",
                      "params", List.of("Bob", "bob@example.com")),
                Map.of("query", "INSERT INTO users (name, email) VALUES (?, ?)",
                      "params", List.of("Alice", "bob@example.com"))  // 重複 email
            );

            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "queries", queries
            );

            // When: 執行包含錯誤的事務
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該失敗並提供錯誤信息
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("約束違反"));
        }
    }

    @Nested
    @DisplayName("複雜查詢操作")
    class ComplexQueryOperations {

        @Test
        @DisplayName("場景：執行多表關聯查詢")
        void shouldExecuteJoinQuery() {
            // Given: 資料庫中存在多個相關聯的表
            QueryResult mockResult = QueryResult.builder()
                .queryId("join-1")
                .rowCount(3)
                .columnNames(List.of("user_name", "order_id", "amount", "status"))
                .rows(List.of(
                    Map.of("user_name", "Alice", "order_id", 1, "amount", 99.99, "status", "completed"),
                    Map.of("user_name", "Alice", "order_id", 2, "amount", 149.99, "status", "pending"),
                    Map.of("user_name", "Bob", "order_id", 3, "amount", 79.99, "status", "completed")
                ))
                .executionTimeMs(65)
                .build();

            when(connectionService.executeQuery(anyString(), anyString(), anyList()))
                .thenReturn(mockResult);

            // And: 提供複雜的關聯查詢
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", """
                    SELECT u.name as user_name, o.id as order_id, o.amount, o.status
                    FROM users u
                    INNER JOIN orders o ON u.id = o.user_id
                    WHERE o.created_at >= ?
                    """,
                "params", List.of("2024-01-01")
            );

            // When: 執行關聯查詢
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該成功返回關聯結果
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：執行包含 NULL 參數的查詢")
        void shouldHandleNullParameters() {
            // Given: 查詢可能包含 NULL 值
            QueryResult mockResult = QueryResult.builder()
                .queryId("null-query-1")
                .rowCount(2)
                .columnNames(List.of("id", "name", "description"))
                .rows(List.of(
                    Map.of("id", 1, "name", "Product1", "description", null),
                    Map.of("id", 2, "name", "Product2", "description", null)
                ))
                .executionTimeMs(25)
                .build();

            when(connectionService.executeQuery(anyString(), anyString(), anyList()))
                .thenReturn(mockResult);

            // And: 提供包含 NULL 參數的查詢
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", "SELECT * FROM products WHERE description IS NULL OR description = ?",
                "params", List.of((Object) null)
            );

            // When: 執行包含 NULL 的查詢
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該正確處理 NULL 參數
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }
    }

    @Nested
    @DisplayName("錯誤處理")
    class ErrorHandling {

        @Test
        @DisplayName("場景：連線 ID 為空時應該失敗")
        void shouldFailWhenConnectionIdIsEmpty() {
            // Given: 提供空的連線 ID
            Map<String, Object> arguments = Map.of(
                "connectionId", "",
                "query", "SELECT 1"
            );

            // When: 嘗試執行查詢
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該失敗並返回錯誤訊息
            assertFalse(result.isSuccess());
            assertEquals("Connection ID 不能為空", result.getError());
        }

        @Test
        @DisplayName("場景：查詢語句為空時應該失敗")
        void shouldFailWhenQueryIsEmpty() {
            // Given: 提供空的查詢語句
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", ""
            );

            // When: 嘗試執行空查詢
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該失敗並返回錯誤訊息
            assertFalse(result.isSuccess());
            assertEquals("查詢語句不能為空", result.getError());
        }

        @Test
        @DisplayName("場景：連線不存在時應該失敗")
        void shouldFailWhenConnectionNotFound() {
            // Given: 連線服務拋出連線不存在的錯誤
            when(connectionService.executeQuery(anyString(), anyString(), anyList()))
                .thenThrow(new RuntimeException("Connection not found"));

            // And: 提供不存在的連線 ID
            Map<String, Object> arguments = Map.of(
                "connectionId", "non-existent",
                "query", "SELECT 1"
            );

            // When: 嘗試使用不存在的連線執行查詢
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該失敗並包含錯誤訊息
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("Connection not found"));
        }

        @Test
        @DisplayName("場景：SQL 語法錯誤時應該失敗")
        void shouldFailWhenSqlSyntaxError() {
            // Given: 資料庫拋出 SQL 語法錯誤
            when(connectionService.executeQuery(anyString(), anyString(), anyList()))
                .thenThrow(new RuntimeException("SQL syntax error"));

            // And: 提供錯誤的 SQL 語法
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-conn",
                "query", "INVALID SQL STATEMENT"
            );

            // When: 嘗試執行錯誤的 SQL
            McpToolResult result = queryTool.execute(arguments);

            // Then: 應該失敗並包含語法錯誤訊息
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("SQL syntax error"));
        }
    }
}