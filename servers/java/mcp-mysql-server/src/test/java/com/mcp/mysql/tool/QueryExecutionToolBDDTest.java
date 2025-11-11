package com.mcp.mysql.tool;

import com.mcp.mysql.config.MySQLConfig;
import com.mcp.mysql.service.MySQLConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MySQL 查詢執行工具 BDD 測試")
class QueryExecutionToolBDDTest {

    @Mock
    private MySQLConnectionService connectionService;

    @Mock
    private MySQLConfig config;

    private QueryExecutionTool queryExecutionTool;

    private final String connectionId = "mysql-test-connection";

    @BeforeEach
    void setUp() {
        queryExecutionTool = new QueryExecutionTool(connectionService, config);
    }

    @Nested
    @DisplayName("基本查詢操作")
    class BasicQueryOperations {

        @Test
        @DisplayName("作為資料分析師，我希望能執行 SELECT 查詢來檢視客戶資料，以便進行數據分析")
        void should_execute_select_query_successfully() {
            // Given: 設定 SELECT 查詢和預期結果
            String selectQuery = "SELECT id, name, email FROM customers WHERE status = ?";
            List<Object> params = Arrays.asList("active");
            List<Map<String, Object>> expectedRows = Arrays.asList(
                Map.of("id", 1, "name", "John Doe", "email", "john@example.com"),
                Map.of("id", 2, "name", "Jane Smith", "email", "jane@example.com")
            );

            when(connectionService.executeQuery(eq(connectionId), eq(selectQuery), eq(params)))
                .thenReturn(Flux.fromIterable(expectedRows));

            // When: 執行查詢
            Flux<Map<String, Object>> result = queryExecutionTool.executeQuery(
                connectionId, selectQuery, params);

            // Then: 驗證查詢結果
            StepVerifier.create(result)
                .expectNext(expectedRows.get(0))
                .expectNext(expectedRows.get(1))
                .verifyComplete();

            verify(connectionService).executeQuery(connectionId, selectQuery, params);
        }

        @Test
        @DisplayName("作為業務人員，我希望能執行 INSERT 查詢來新增客戶資料，以便擴展客戶基礎")
        void should_execute_insert_query_successfully() {
            // Given: 設定 INSERT 查詢
            String insertQuery = "INSERT INTO customers (name, email, phone, created_at) VALUES (?, ?, ?, ?)";
            List<Object> params = Arrays.asList(
                "Alice Johnson",
                "alice@example.com",
                "123-456-7890",
                LocalDateTime.now()
            );

            when(connectionService.executeUpdate(eq(connectionId), eq(insertQuery), eq(params)))
                .thenReturn(Mono.just(1));

            // When: 執行插入操作
            Mono<Integer> result = queryExecutionTool.executeUpdate(
                connectionId, insertQuery, params);

            // Then: 驗證插入成功
            StepVerifier.create(result)
                .expectNext(1)
                .verifyComplete();

            verify(connectionService).executeUpdate(connectionId, insertQuery, params);
        }

        @Test
        @DisplayName("作為客戶服務代表，我希望能執行 UPDATE 查詢來修改客戶資訊，以便保持資料最新")
        void should_execute_update_query_successfully() {
            // Given: 設定 UPDATE 查詢
            String updateQuery = "UPDATE customers SET email = ?, phone = ?, updated_at = ? WHERE id = ?";
            List<Object> params = Arrays.asList(
                "newemail@example.com",
                "987-654-3210",
                LocalDateTime.now(),
                1
            );

            when(connectionService.executeUpdate(eq(connectionId), eq(updateQuery), eq(params)))
                .thenReturn(Mono.just(1));

            // When: 執行更新操作
            Mono<Integer> result = queryExecutionTool.executeUpdate(
                connectionId, updateQuery, params);

            // Then: 驗證更新成功
            StepVerifier.create(result)
                .expectNext(1)
                .verifyComplete();

            verify(connectionService).executeUpdate(connectionId, updateQuery, params);
        }

        @Test
        @DisplayName("作為數據管理員，我希望能執行 DELETE 查詢來移除無效資料，以便維護資料品質")
        void should_execute_delete_query_successfully() {
            // Given: 設定 DELETE 查詢
            String deleteQuery = "DELETE FROM customers WHERE status = ? AND last_login < ?";
            List<Object> params = Arrays.asList("inactive", LocalDateTime.now().minusMonths(6));

            when(connectionService.executeUpdate(eq(connectionId), eq(deleteQuery), eq(params)))
                .thenReturn(Mono.just(3));

            // When: 執行刪除操作
            Mono<Integer> result = queryExecutionTool.executeUpdate(
                connectionId, deleteQuery, params);

            // Then: 驗證刪除成功
            StepVerifier.create(result)
                .expectNext(3)
                .verifyComplete();

            verify(connectionService).executeUpdate(connectionId, deleteQuery, params);
        }
    }

    @Nested
    @DisplayName("複雜查詢場景")
    class ComplexQueryScenarios {

        @Test
        @DisplayName("作為報表專員，我希望能執行 JOIN 查詢來產生客戶訂單報表，以便進行業務分析")
        void should_execute_join_query_successfully() {
            // Given: 設定複雜 JOIN 查詢
            String joinQuery = """
                SELECT c.id, c.name, c.email, COUNT(o.id) as order_count, SUM(o.total_amount) as total_spent
                FROM customers c
                LEFT JOIN orders o ON c.id = o.customer_id
                WHERE c.status = ? AND c.created_at >= ?
                GROUP BY c.id, c.name, c.email
                HAVING COUNT(o.id) > ?
                ORDER BY total_spent DESC
                LIMIT ?
                """;

            List<Object> params = Arrays.asList("active", LocalDateTime.now().minusYears(1), 0, 10);
            List<Map<String, Object>> expectedRows = Arrays.asList(
                Map.of("id", 1, "name", "John Doe", "email", "john@example.com",
                       "order_count", 5L, "total_spent", 1500.00),
                Map.of("id", 2, "name", "Jane Smith", "email", "jane@example.com",
                       "order_count", 3L, "total_spent", 950.00)
            );

            when(connectionService.executeQuery(eq(connectionId), eq(joinQuery), eq(params)))
                .thenReturn(Flux.fromIterable(expectedRows));

            // When: 執行複雜查詢
            Flux<Map<String, Object>> result = queryExecutionTool.executeQuery(
                connectionId, joinQuery, params);

            // Then: 驗證查詢結果
            StepVerifier.create(result)
                .expectNext(expectedRows.get(0))
                .expectNext(expectedRows.get(1))
                .verifyComplete();
        }

        @Test
        @DisplayName("作為資料科學家，我希望能執行統計查詢來分析銷售趨勢，以便制定業務策略")
        void should_execute_statistical_query_successfully() {
            // Given: 設定統計分析查詢
            String statsQuery = """
                SELECT
                    DATE_FORMAT(created_at, '%Y-%m') as month,
                    COUNT(*) as order_count,
                    AVG(total_amount) as avg_amount,
                    MIN(total_amount) as min_amount,
                    MAX(total_amount) as max_amount,
                    STDDEV(total_amount) as std_amount
                FROM orders
                WHERE created_at >= ? AND status IN (?, ?)
                GROUP BY DATE_FORMAT(created_at, '%Y-%m')
                ORDER BY month DESC
                """;

            List<Object> params = Arrays.asList(
                LocalDateTime.now().minusMonths(12), "completed", "shipped");

            List<Map<String, Object>> expectedStats = Arrays.asList(
                Map.of("month", "2024-01", "order_count", 150L, "avg_amount", 250.75,
                       "min_amount", 10.00, "max_amount", 2500.00, "std_amount", 125.30)
            );

            when(connectionService.executeQuery(eq(connectionId), eq(statsQuery), eq(params)))
                .thenReturn(Flux.fromIterable(expectedStats));

            // When: 執行統計查詢
            Flux<Map<String, Object>> result = queryExecutionTool.executeQuery(
                connectionId, statsQuery, params);

            // Then: 驗證統計結果
            StepVerifier.create(result)
                .expectNext(expectedStats.get(0))
                .verifyComplete();
        }

        @Test
        @DisplayName("作為系統管理員，我希望能執行子查詢來找出異常數據，以便進行數據清理")
        void should_execute_subquery_successfully() {
            // Given: 設定包含子查詢的複雜查詢
            String subQuery = """
                SELECT c.id, c.name, c.email,
                       (SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.id) as order_count,
                       (SELECT MAX(o.total_amount) FROM orders o WHERE o.customer_id = c.id) as max_order
                FROM customers c
                WHERE c.id IN (
                    SELECT DISTINCT customer_id
                    FROM orders
                    WHERE total_amount > (
                        SELECT AVG(total_amount) * 2 FROM orders
                    )
                )
                AND c.status = ?
                """;

            List<Object> params = Arrays.asList("active");
            List<Map<String, Object>> expectedResults = Arrays.asList(
                Map.of("id", 1, "name", "Big Spender", "email", "bigspender@example.com",
                       "order_count", 8L, "max_order", 5000.00)
            );

            when(connectionService.executeQuery(eq(connectionId), eq(subQuery), eq(params)))
                .thenReturn(Flux.fromIterable(expectedResults));

            // When: 執行子查詢
            Flux<Map<String, Object>> result = queryExecutionTool.executeQuery(
                connectionId, subQuery, params);

            // Then: 驗證查詢結果
            StepVerifier.create(result)
                .expectNext(expectedResults.get(0))
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("交易處理")
    class TransactionProcessing {

        @Test
        @DisplayName("作為會計人員，我希望能在交易中處理多個相關操作，以便保證資料一致性")
        void should_execute_transaction_successfully() {
            // Given: 設定交易中的多個操作
            List<String> queries = Arrays.asList(
                "INSERT INTO orders (customer_id, total_amount, status) VALUES (?, ?, ?)",
                "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)",
                "UPDATE inventory SET quantity = quantity - ? WHERE product_id = ?"
            );

            List<List<Object>> params = Arrays.asList(
                Arrays.asList(1, 299.99, "pending"),
                Arrays.asList(1, 101, 2, 149.99),
                Arrays.asList(2, 101)
            );

            when(connectionService.executeTransaction(eq(connectionId), eq(queries), eq(params)))
                .thenReturn(Mono.just(Arrays.asList(1, 1, 1)));

            // When: 執行交易
            Mono<List<Integer>> result = queryExecutionTool.executeTransaction(
                connectionId, queries, params);

            // Then: 驗證交易執行成功
            StepVerifier.create(result)
                .expectNext(Arrays.asList(1, 1, 1))
                .verifyComplete();

            verify(connectionService).executeTransaction(connectionId, queries, params);
        }

        @Test
        @DisplayName("作為系統管理員，我希望交易失敗時能自動回滾，以便保護資料完整性")
        void should_rollback_transaction_on_failure() {
            // Given: 設定會失敗的交易
            List<String> queries = Arrays.asList(
                "INSERT INTO orders (customer_id, total_amount, status) VALUES (?, ?, ?)",
                "INSERT INTO invalid_table (invalid_column) VALUES (?)"  // 會失敗的查詢
            );

            List<List<Object>> params = Arrays.asList(
                Arrays.asList(1, 299.99, "pending"),
                Arrays.asList("invalid_value")
            );

            when(connectionService.executeTransaction(eq(connectionId), eq(queries), eq(params)))
                .thenReturn(Mono.error(new RuntimeException("Table 'invalid_table' doesn't exist")));

            // When: 執行會失敗的交易
            Mono<List<Integer>> result = queryExecutionTool.executeTransaction(
                connectionId, queries, params);

            // Then: 驗證交易失敗並回滾
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

            verify(connectionService).executeTransaction(connectionId, queries, params);
        }
    }

    @Nested
    @DisplayName("批次處理")
    class BatchProcessing {

        @Test
        @DisplayName("作為數據導入專員，我希望能批次插入大量資料，以便提高處理效率")
        void should_execute_batch_insert_successfully() {
            // Given: 設定批次插入操作
            String batchQuery = "INSERT INTO customers (name, email, phone, status) VALUES (?, ?, ?, ?)";
            List<List<Object>> batchParams = Arrays.asList(
                Arrays.asList("Customer1", "customer1@example.com", "111-111-1111", "active"),
                Arrays.asList("Customer2", "customer2@example.com", "222-222-2222", "active"),
                Arrays.asList("Customer3", "customer3@example.com", "333-333-3333", "pending")
            );

            when(connectionService.executeBatch(eq(connectionId), eq(batchQuery), eq(batchParams)))
                .thenReturn(Mono.just(Arrays.asList(1, 1, 1)));

            // When: 執行批次插入
            Mono<List<Integer>> result = queryExecutionTool.executeBatch(
                connectionId, batchQuery, batchParams);

            // Then: 驗證批次插入成功
            StepVerifier.create(result)
                .expectNext(Arrays.asList(1, 1, 1))
                .verifyComplete();

            verify(connectionService).executeBatch(connectionId, batchQuery, batchParams);
        }

        @Test
        @DisplayName("作為數據更新專員，我希望能批次更新客戶狀態，以便處理大量狀態變更")
        void should_execute_batch_update_successfully() {
            // Given: 設定批次更新操作
            String batchQuery = "UPDATE customers SET status = ?, updated_at = ? WHERE id = ?";
            List<List<Object>> batchParams = Arrays.asList(
                Arrays.asList("inactive", LocalDateTime.now(), 1),
                Arrays.asList("inactive", LocalDateTime.now(), 2),
                Arrays.asList("suspended", LocalDateTime.now(), 3)
            );

            when(connectionService.executeBatch(eq(connectionId), eq(batchQuery), eq(batchParams)))
                .thenReturn(Mono.just(Arrays.asList(1, 1, 1)));

            // When: 執行批次更新
            Mono<List<Integer>> result = queryExecutionTool.executeBatch(
                connectionId, batchQuery, batchParams);

            // Then: 驗證批次更新成功
            StepVerifier.create(result)
                .expectNext(Arrays.asList(1, 1, 1))
                .verifyComplete();

            verify(connectionService).executeBatch(connectionId, batchQuery, batchParams);
        }
    }

    @Nested
    @DisplayName("參數處理")
    class ParameterHandling {

        @Test
        @DisplayName("作為查詢使用者，我希望能處理 null 參數，以便進行彈性查詢")
        void should_handle_null_parameters_correctly() {
            // Given: 設定包含 null 參數的查詢
            String queryWithNulls = "SELECT * FROM customers WHERE email = ? AND phone = ?";
            List<Object> paramsWithNulls = Arrays.asList("test@example.com", null);

            List<Map<String, Object>> expectedRows = Arrays.asList(
                Map.of("id", 1, "name", "Test User", "email", "test@example.com", "phone", null)
            );

            when(connectionService.executeQuery(eq(connectionId), eq(queryWithNulls), eq(paramsWithNulls)))
                .thenReturn(Flux.fromIterable(expectedRows));

            // When: 執行包含 null 參數的查詢
            Flux<Map<String, Object>> result = queryExecutionTool.executeQuery(
                connectionId, queryWithNulls, paramsWithNulls);

            // Then: 驗證能正確處理 null 參數
            StepVerifier.create(result)
                .expectNext(expectedRows.get(0))
                .verifyComplete();

            verify(connectionService).executeQuery(connectionId, queryWithNulls, paramsWithNulls);
        }

        @Test
        @DisplayName("作為查詢使用者，我希望能處理不同數據類型的參數，以便支援多樣化查詢")
        void should_handle_various_parameter_types() {
            // Given: 設定包含多種數據類型參數的查詢
            String queryWithTypes = """
                SELECT * FROM orders
                WHERE customer_id = ? AND total_amount >= ? AND created_at >= ?
                AND status IN (?, ?) AND is_priority = ?
                """;

            List<Object> mixedParams = Arrays.asList(
                1,                              // Integer
                199.99,                         // Double
                LocalDateTime.now().minusDays(7), // LocalDateTime
                "completed",                    // String
                "shipped",                      // String
                true                           // Boolean
            );

            List<Map<String, Object>> expectedRows = Arrays.asList(
                Map.of("id", 1, "customer_id", 1, "total_amount", 299.99, "status", "completed")
            );

            when(connectionService.executeQuery(eq(connectionId), eq(queryWithTypes), eq(mixedParams)))
                .thenReturn(Flux.fromIterable(expectedRows));

            // When: 執行包含多種類型參數的查詢
            Flux<Map<String, Object>> result = queryExecutionTool.executeQuery(
                connectionId, queryWithTypes, mixedParams);

            // Then: 驗證能正確處理不同類型參數
            StepVerifier.create(result)
                .expectNext(expectedRows.get(0))
                .verifyComplete();

            verify(connectionService).executeQuery(connectionId, queryWithTypes, mixedParams);
        }
    }

    @Nested
    @DisplayName("查詢錯誤處理")
    class QueryErrorHandling {

        @Test
        @DisplayName("作為系統管理員，我希望系統能妥善處理 SQL 語法錯誤，以便快速定位問題")
        void should_handle_sql_syntax_error() {
            // Given: 設定語法錯誤的 SQL
            String invalidQuery = "SELCT * FORM customers";  // 故意的語法錯誤
            List<Object> params = Arrays.asList();

            when(connectionService.executeQuery(eq(connectionId), eq(invalidQuery), eq(params)))
                .thenReturn(Flux.error(new RuntimeException("You have an error in your SQL syntax")));

            // When: 執行語法錯誤的查詢
            Flux<Map<String, Object>> result = queryExecutionTool.executeQuery(
                connectionId, invalidQuery, params);

            // Then: 驗證能正確處理語法錯誤
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

            verify(connectionService).executeQuery(connectionId, invalidQuery, params);
        }

        @Test
        @DisplayName("作為數據分析師，我希望系統能處理表不存在錯誤，以便了解資料庫結構")
        void should_handle_table_not_exists_error() {
            // Given: 設定查詢不存在的表
            String queryNonExistentTable = "SELECT * FROM non_existent_table";
            List<Object> params = Arrays.asList();

            when(connectionService.executeQuery(eq(connectionId), eq(queryNonExistentTable), eq(params)))
                .thenReturn(Flux.error(new RuntimeException("Table 'non_existent_table' doesn't exist")));

            // When: 執行查詢不存在的表
            Flux<Map<String, Object>> result = queryExecutionTool.executeQuery(
                connectionId, queryNonExistentTable, params);

            // Then: 驗證能正確處理表不存在錯誤
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

            verify(connectionService).executeQuery(connectionId, queryNonExistentTable, params);
        }

        @Test
        @DisplayName("作為開發人員，我希望系統能處理連線超時錯誤，以便進行適當的重試機制")
        void should_handle_connection_timeout_error() {
            // Given: 設定連線超時情境
            String timeoutQuery = "SELECT SLEEP(100)";  // MySQL 特有的 SLEEP 函數
            List<Object> params = Arrays.asList();

            when(connectionService.executeQuery(eq(connectionId), eq(timeoutQuery), eq(params)))
                .thenReturn(Flux.error(new RuntimeException("Connection timeout")));

            // When: 執行會超時的查詢
            Flux<Map<String, Object>> result = queryExecutionTool.executeQuery(
                connectionId, timeoutQuery, params);

            // Then: 驗證能正確處理連線超時
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

            verify(connectionService).executeQuery(connectionId, timeoutQuery, params);
        }

        @Test
        @DisplayName("作為資料維護員，我希望系統能處理約束違反錯誤，以便了解資料完整性問題")
        void should_handle_constraint_violation_error() {
            // Given: 設定會違反約束的插入操作
            String constraintViolationQuery = "INSERT INTO customers (id, name, email) VALUES (?, ?, ?)";
            List<Object> params = Arrays.asList(1, "Duplicate User", "existing@example.com");

            when(connectionService.executeUpdate(eq(connectionId), eq(constraintViolationQuery), eq(params)))
                .thenReturn(Mono.error(new RuntimeException("Duplicate entry 'existing@example.com' for key 'email'")));

            // When: 執行會違反約束的操作
            Mono<Integer> result = queryExecutionTool.executeUpdate(
                connectionId, constraintViolationQuery, params);

            // Then: 驗證能正確處理約束違反錯誤
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

            verify(connectionService).executeUpdate(connectionId, constraintViolationQuery, params);
        }
    }

    @Nested
    @DisplayName("MySQL 特有功能")
    class MySQLSpecificFeatures {

        @Test
        @DisplayName("作為 MySQL 用戶，我希望能使用 MySQL 特有的函數進行數據處理，以便發揮資料庫特性")
        void should_execute_mysql_specific_functions() {
            // Given: 使用 MySQL 特有函數的查詢
            String mysqlQuery = """
                SELECT
                    id,
                    name,
                    JSON_EXTRACT(metadata, '$.preferences') as preferences,
                    DATE_FORMAT(created_at, '%Y-%m-%d') as formatted_date,
                    IFNULL(phone, 'N/A') as phone_display
                FROM customers
                WHERE JSON_CONTAINS(metadata, '{"active": true}')
                AND MATCH(name, email) AGAINST(? IN BOOLEAN MODE)
                """;

            List<Object> params = Arrays.asList("+important +customer");
            List<Map<String, Object>> expectedRows = Arrays.asList(
                Map.of("id", 1, "name", "Important Customer", "preferences", "{\"newsletter\": true}",
                       "formatted_date", "2024-01-15", "phone_display", "555-0123")
            );

            when(connectionService.executeQuery(eq(connectionId), eq(mysqlQuery), eq(params)))
                .thenReturn(Flux.fromIterable(expectedRows));

            // When: 執行包含 MySQL 特有函數的查詢
            Flux<Map<String, Object>> result = queryExecutionTool.executeQuery(
                connectionId, mysqlQuery, params);

            // Then: 驗證 MySQL 特有功能正常工作
            StepVerifier.create(result)
                .expectNext(expectedRows.get(0))
                .verifyComplete();

            verify(connectionService).executeQuery(connectionId, mysqlQuery, params);
        }

        @Test
        @DisplayName("作為資料庫管理員，我希望能使用 MySQL 的窗口函數進行進階分析，以便產生複雜報表")
        void should_execute_mysql_window_functions() {
            // Given: 使用 MySQL 窗口函數的查詢
            String windowQuery = """
                SELECT
                    customer_id,
                    order_date,
                    total_amount,
                    ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY order_date DESC) as order_rank,
                    LAG(total_amount) OVER (PARTITION BY customer_id ORDER BY order_date) as prev_amount,
                    SUM(total_amount) OVER (PARTITION BY customer_id) as customer_total,
                    PERCENT_RANK() OVER (ORDER BY total_amount) as amount_percentile
                FROM orders
                WHERE order_date >= ?
                """;

            List<Object> params = Arrays.asList(LocalDateTime.now().minusMonths(3));
            List<Map<String, Object>> expectedRows = Arrays.asList(
                Map.of("customer_id", 1, "order_rank", 1, "prev_amount", null,
                       "customer_total", 1500.00, "amount_percentile", 0.85)
            );

            when(connectionService.executeQuery(eq(connectionId), eq(windowQuery), eq(params)))
                .thenReturn(Flux.fromIterable(expectedRows));

            // When: 執行包含窗口函數的查詢
            Flux<Map<String, Object>> result = queryExecutionTool.executeQuery(
                connectionId, windowQuery, params);

            // Then: 驗證窗口函數正常工作
            StepVerifier.create(result)
                .expectNext(expectedRows.get(0))
                .verifyComplete();

            verify(connectionService).executeQuery(connectionId, windowQuery, params);
        }
    }
}