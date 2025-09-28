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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MySQL Schema 管理工具 BDD 測試")
class SchemaManagementToolBDDTest {

    @Mock
    private MySQLConnectionService connectionService;

    @Mock
    private MySQLConfig config;

    private SchemaManagementTool schemaManagementTool;

    private final String connectionId = "mysql-test-connection";

    @BeforeEach
    void setUp() {
        schemaManagementTool = new SchemaManagementTool(connectionService, config);
    }

    @Nested
    @DisplayName("表結構分析")
    class TableSchemaAnalysis {

        @Test
        @DisplayName("作為資料庫設計師，我希望能獲取完整的表結構資訊，以便了解資料模型")
        void should_get_complete_table_schema_information() {
            // Given: 設定表結構查詢結果
            String tableName = "customers";
            String schemaName = "ecommerce";

            List<Map<String, Object>> columnInfo = Arrays.asList(
                Map.of("COLUMN_NAME", "id", "DATA_TYPE", "bigint", "IS_NULLABLE", "NO",
                       "COLUMN_DEFAULT", null, "COLUMN_KEY", "PRI", "EXTRA", "auto_increment",
                       "COLUMN_COMMENT", "客戶唯一識別碼"),
                Map.of("COLUMN_NAME", "name", "DATA_TYPE", "varchar", "IS_NULLABLE", "NO",
                       "COLUMN_DEFAULT", null, "COLUMN_KEY", "", "EXTRA", "",
                       "COLUMN_COMMENT", "客戶姓名"),
                Map.of("COLUMN_NAME", "email", "DATA_TYPE", "varchar", "IS_NULLABLE", "NO",
                       "COLUMN_DEFAULT", null, "COLUMN_KEY", "UNI", "EXTRA", "",
                       "COLUMN_COMMENT", "客戶電子信箱"),
                Map.of("COLUMN_NAME", "created_at", "DATA_TYPE", "timestamp", "IS_NULLABLE", "NO",
                       "COLUMN_DEFAULT", "CURRENT_TIMESTAMP", "COLUMN_KEY", "", "EXTRA", "",
                       "COLUMN_COMMENT", "建立時間")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.COLUMNS"), any()))
                .thenReturn(Flux.fromIterable(columnInfo));

            // When: 獲取表結構
            Flux<Map<String, Object>> result = schemaManagementTool.getTableSchema(
                connectionId, tableName, schemaName);

            // Then: 驗證表結構資訊完整
            StepVerifier.create(result)
                .expectNext(columnInfo.get(0))
                .expectNext(columnInfo.get(1))
                .expectNext(columnInfo.get(2))
                .expectNext(columnInfo.get(3))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.COLUMNS"), any());
        }

        @Test
        @DisplayName("作為數據遷移專員，我希望能獲取表的外鍵約束資訊，以便規劃遷移順序")
        void should_get_foreign_key_constraints() {
            // Given: 設定外鍵約束查詢結果
            String tableName = "orders";
            String schemaName = "ecommerce";

            List<Map<String, Object>> foreignKeys = Arrays.asList(
                Map.of("CONSTRAINT_NAME", "fk_orders_customer", "COLUMN_NAME", "customer_id",
                       "REFERENCED_TABLE_SCHEMA", "ecommerce", "REFERENCED_TABLE_NAME", "customers",
                       "REFERENCED_COLUMN_NAME", "id", "UPDATE_RULE", "CASCADE", "DELETE_RULE", "RESTRICT"),
                Map.of("CONSTRAINT_NAME", "fk_orders_payment", "COLUMN_NAME", "payment_method_id",
                       "REFERENCED_TABLE_SCHEMA", "ecommerce", "REFERENCED_TABLE_NAME", "payment_methods",
                       "REFERENCED_COLUMN_NAME", "id", "UPDATE_RULE", "NO ACTION", "DELETE_RULE", "SET NULL")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("KEY_COLUMN_USAGE"), any()))
                .thenReturn(Flux.fromIterable(foreignKeys));

            // When: 獲取外鍵約束
            Flux<Map<String, Object>> result = schemaManagementTool.getForeignKeys(
                connectionId, tableName, schemaName);

            // Then: 驗證外鍵約束資訊
            StepVerifier.create(result)
                .expectNext(foreignKeys.get(0))
                .expectNext(foreignKeys.get(1))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("KEY_COLUMN_USAGE"), any());
        }

        @Test
        @DisplayName("作為性能調優專家，我希望能分析表的儲存引擎和字符集，以便最佳化配置")
        void should_analyze_table_storage_engine_and_charset() {
            // Given: 設定表儲存資訊查詢結果
            String tableName = "products";
            String schemaName = "ecommerce";

            List<Map<String, Object>> tableInfo = Arrays.asList(
                Map.of("TABLE_NAME", "products", "ENGINE", "InnoDB", "TABLE_COLLATION", "utf8mb4_unicode_ci",
                       "AUTO_INCREMENT", 1001L, "TABLE_ROWS", 50000L, "DATA_LENGTH", 15728640L,
                       "INDEX_LENGTH", 5242880L, "TABLE_COMMENT", "產品資料表")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TABLES"), any()))
                .thenReturn(Flux.fromIterable(tableInfo));

            // When: 獲取表儲存資訊
            Flux<Map<String, Object>> result = schemaManagementTool.getTableInfo(
                connectionId, tableName, schemaName);

            // Then: 驗證表儲存資訊
            StepVerifier.create(result)
                .expectNext(tableInfo.get(0))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TABLES"), any());
        }
    }

    @Nested
    @DisplayName("Schema 探索")
    class SchemaExploration {

        @Test
        @DisplayName("作為新進開發人員，我希望能列出資料庫中的所有表，以便了解整體架構")
        void should_list_all_tables_in_database() {
            // Given: 設定表列表查詢結果
            String schemaName = "ecommerce";
            List<Map<String, Object>> tables = Arrays.asList(
                Map.of("TABLE_NAME", "customers", "TABLE_TYPE", "BASE TABLE", "TABLE_COMMENT", "客戶資料表"),
                Map.of("TABLE_NAME", "products", "TABLE_TYPE", "BASE TABLE", "TABLE_COMMENT", "產品資料表"),
                Map.of("TABLE_NAME", "orders", "TABLE_TYPE", "BASE TABLE", "TABLE_COMMENT", "訂單資料表"),
                Map.of("TABLE_NAME", "customer_view", "TABLE_TYPE", "VIEW", "TABLE_COMMENT", "客戶檢視表")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TABLES"), any()))
                .thenReturn(Flux.fromIterable(tables));

            // When: 列出所有表
            Flux<Map<String, Object>> result = schemaManagementTool.listTables(connectionId, schemaName);

            // Then: 驗證表列表完整
            StepVerifier.create(result)
                .expectNext(tables.get(0))
                .expectNext(tables.get(1))
                .expectNext(tables.get(2))
                .expectNext(tables.get(3))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TABLES"), any());
        }

        @Test
        @DisplayName("作為系統管理員，我希望能列出資料庫中的所有 Schema，以便進行權限管理")
        void should_list_all_schemas() {
            // Given: 設定 Schema 列表查詢結果
            List<Map<String, Object>> schemas = Arrays.asList(
                Map.of("SCHEMA_NAME", "ecommerce", "DEFAULT_CHARACTER_SET_NAME", "utf8mb4",
                       "DEFAULT_COLLATION_NAME", "utf8mb4_unicode_ci"),
                Map.of("SCHEMA_NAME", "analytics", "DEFAULT_CHARACTER_SET_NAME", "utf8mb4",
                       "DEFAULT_COLLATION_NAME", "utf8mb4_unicode_ci"),
                Map.of("SCHEMA_NAME", "logs", "DEFAULT_CHARACTER_SET_NAME", "utf8mb4",
                       "DEFAULT_COLLATION_NAME", "utf8mb4_unicode_ci")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.SCHEMATA"), any()))
                .thenReturn(Flux.fromIterable(schemas));

            // When: 列出所有 Schema
            Flux<Map<String, Object>> result = schemaManagementTool.listSchemas(connectionId);

            // Then: 驗證 Schema 列表
            StepVerifier.create(result)
                .expectNext(schemas.get(0))
                .expectNext(schemas.get(1))
                .expectNext(schemas.get(2))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.SCHEMATA"), any());
        }

        @Test
        @DisplayName("作為 DBA，我希望能查看資料庫中的所有檢視表，以便了解查詢抽象層")
        void should_list_all_views() {
            // Given: 設定檢視表列表查詢結果
            String schemaName = "ecommerce";
            List<Map<String, Object>> views = Arrays.asList(
                Map.of("TABLE_NAME", "customer_orders_view", "VIEW_DEFINITION",
                       "SELECT c.name, COUNT(o.id) as order_count FROM customers c LEFT JOIN orders o ON c.id = o.customer_id GROUP BY c.id",
                       "CHECK_OPTION", "NONE", "IS_UPDATABLE", "NO"),
                Map.of("TABLE_NAME", "monthly_sales_view", "VIEW_DEFINITION",
                       "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, SUM(total_amount) as sales FROM orders GROUP BY month",
                       "CHECK_OPTION", "NONE", "IS_UPDATABLE", "NO")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.VIEWS"), any()))
                .thenReturn(Flux.fromIterable(views));

            // When: 列出所有檢視表
            Flux<Map<String, Object>> result = schemaManagementTool.listViews(connectionId, schemaName);

            // Then: 驗證檢視表列表
            StepVerifier.create(result)
                .expectNext(views.get(0))
                .expectNext(views.get(1))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.VIEWS"), any());
        }
    }

    @Nested
    @DisplayName("索引分析")
    class IndexAnalysis {

        @Test
        @DisplayName("作為性能調優專家，我希望能分析表的索引配置，以便最佳化查詢性能")
        void should_analyze_table_indexes() {
            // Given: 設定索引分析查詢結果
            String tableName = "customers";
            String schemaName = "ecommerce";

            List<Map<String, Object>> indexes = Arrays.asList(
                Map.of("INDEX_NAME", "PRIMARY", "COLUMN_NAME", "id", "INDEX_TYPE", "BTREE",
                       "NON_UNIQUE", 0, "SEQ_IN_INDEX", 1, "CARDINALITY", 100000L, "NULLABLE", ""),
                Map.of("INDEX_NAME", "idx_email", "COLUMN_NAME", "email", "INDEX_TYPE", "BTREE",
                       "NON_UNIQUE", 0, "SEQ_IN_INDEX", 1, "CARDINALITY", 100000L, "NULLABLE", ""),
                Map.of("INDEX_NAME", "idx_name_phone", "COLUMN_NAME", "name", "INDEX_TYPE", "BTREE",
                       "NON_UNIQUE", 1, "SEQ_IN_INDEX", 1, "CARDINALITY", 95000L, "NULLABLE", ""),
                Map.of("INDEX_NAME", "idx_name_phone", "COLUMN_NAME", "phone", "INDEX_TYPE", "BTREE",
                       "NON_UNIQUE", 1, "SEQ_IN_INDEX", 2, "CARDINALITY", 90000L, "NULLABLE", "YES")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.STATISTICS"), any()))
                .thenReturn(Flux.fromIterable(indexes));

            // When: 分析表索引
            Flux<Map<String, Object>> result = schemaManagementTool.getTableIndexes(
                connectionId, tableName, schemaName);

            // Then: 驗證索引分析結果
            StepVerifier.create(result)
                .expectNext(indexes.get(0))
                .expectNext(indexes.get(1))
                .expectNext(indexes.get(2))
                .expectNext(indexes.get(3))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.STATISTICS"), any());
        }

        @Test
        @DisplayName("作為查詢優化師，我希望能分析索引使用統計，以便識別無效索引")
        void should_analyze_index_usage_statistics() {
            // Given: 設定索引使用統計查詢結果
            String schemaName = "ecommerce";

            List<Map<String, Object>> indexStats = Arrays.asList(
                Map.of("TABLE_SCHEMA", "ecommerce", "TABLE_NAME", "customers", "INDEX_NAME", "idx_email",
                       "COUNT_STAR", 1500000L, "SUM_TIMER_WAIT", 15000000000L, "MIN_TIMER_WAIT", 5000L,
                       "AVG_TIMER_WAIT", 10000L, "MAX_TIMER_WAIT", 50000L),
                Map.of("TABLE_SCHEMA", "ecommerce", "TABLE_NAME", "orders", "INDEX_NAME", "idx_customer_id",
                       "COUNT_STAR", 2500000L, "SUM_TIMER_WAIT", 20000000000L, "MIN_TIMER_WAIT", 3000L,
                       "AVG_TIMER_WAIT", 8000L, "MAX_TIMER_WAIT", 45000L)
            );

            when(connectionService.executeQuery(eq(connectionId), contains("performance_schema.table_io_waits_summary_by_index_usage"), any()))
                .thenReturn(Flux.fromIterable(indexStats));

            // When: 分析索引使用統計
            Flux<Map<String, Object>> result = schemaManagementTool.getIndexUsageStats(
                connectionId, schemaName);

            // Then: 驗證索引使用統計
            StepVerifier.create(result)
                .expectNext(indexStats.get(0))
                .expectNext(indexStats.get(1))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("performance_schema.table_io_waits_summary_by_index_usage"), any());
        }
    }

    @Nested
    @DisplayName("查詢性能分析")
    class QueryPerformanceAnalysis {

        @Test
        @DisplayName("作為性能分析師，我希望能使用 EXPLAIN 分析查詢執行計畫，以便識別性能瓶頸")
        void should_explain_query_execution_plan() {
            // Given: 設定 EXPLAIN 查詢結果
            String query = "SELECT c.name, COUNT(o.id) FROM customers c LEFT JOIN orders o ON c.id = o.customer_id WHERE c.status = 'active' GROUP BY c.id";

            List<Map<String, Object>> explainResult = Arrays.asList(
                Map.of("id", 1, "select_type", "SIMPLE", "table", "c", "partitions", null,
                       "type", "ALL", "possible_keys", "idx_status", "key", "idx_status",
                       "key_len", "20", "ref", "const", "rows", 50000L, "filtered", 25.0,
                       "Extra", "Using where; Using temporary; Using filesort"),
                Map.of("id", 1, "select_type", "SIMPLE", "table", "o", "partitions", null,
                       "type", "ref", "possible_keys", "idx_customer_id", "key", "idx_customer_id",
                       "key_len", "8", "ref", "c.id", "rows", 5L, "filtered", 100.0,
                       "Extra", "Using index")
            );

            when(connectionService.executeQuery(eq(connectionId), startsWith("EXPLAIN"), any()))
                .thenReturn(Flux.fromIterable(explainResult));

            // When: 執行 EXPLAIN 分析
            Flux<Map<String, Object>> result = schemaManagementTool.explainQuery(
                connectionId, query);

            // Then: 驗證執行計畫分析結果
            StepVerifier.create(result)
                .expectNext(explainResult.get(0))
                .expectNext(explainResult.get(1))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), startsWith("EXPLAIN"), any());
        }

        @Test
        @DisplayName("作為性能調優專家，我希望能使用 EXPLAIN ANALYZE 獲取實際執行統計，以便進行精確調優")
        void should_analyze_query_with_execution_statistics() {
            // Given: 設定 EXPLAIN ANALYZE 查詢結果 (MySQL 8.0+ 支援)
            String query = "SELECT * FROM orders WHERE customer_id = 123 ORDER BY created_at DESC LIMIT 10";

            List<Map<String, Object>> analyzeResult = Arrays.asList(
                Map.of("EXPLAIN", "-> Limit: 10 row(s)  (cost=15.25 rows=10) (actual time=0.123..0.145 rows=10 loops=1)\n" +
                                 "    -> Sort: orders.created_at DESC  (cost=15.25 rows=25) (actual time=0.122..0.142 rows=10 loops=1)\n" +
                                 "        -> Index lookup on orders using idx_customer_id (customer_id=123)  (cost=10.25 rows=25) (actual time=0.035..0.098 rows=25 loops=1)")
            );

            when(connectionService.executeQuery(eq(connectionId), startsWith("EXPLAIN ANALYZE"), any()))
                .thenReturn(Flux.fromIterable(analyzeResult));

            // When: 執行 EXPLAIN ANALYZE
            Flux<Map<String, Object>> result = schemaManagementTool.explainAnalyzeQuery(
                connectionId, query);

            // Then: 驗證實際執行統計
            StepVerifier.create(result)
                .expectNext(analyzeResult.get(0))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), startsWith("EXPLAIN ANALYZE"), any());
        }

        @Test
        @DisplayName("作為資料庫管理員，我希望能分析慢查詢日誌，以便識別問題查詢")
        void should_analyze_slow_query_log() {
            // Given: 設定慢查詢日誌分析結果
            List<Map<String, Object>> slowQueries = Arrays.asList(
                Map.of("start_time", "2024-01-15 10:30:45", "user_host", "app_user[app_user] @ [192.168.1.100]",
                       "query_time", "00:00:05.123456", "lock_time", "00:00:00.000012",
                       "rows_sent", 1500L, "rows_examined", 250000L,
                       "sql_text", "SELECT * FROM orders o JOIN customers c ON o.customer_id = c.id WHERE o.status = 'pending'"),
                Map.of("start_time", "2024-01-15 11:15:20", "user_host", "report_user[report_user] @ [192.168.1.200]",
                       "query_time", "00:00:03.876543", "lock_time", "00:00:00.000008",
                       "rows_sent", 500L, "rows_examined", 180000L,
                       "sql_text", "SELECT COUNT(*) FROM products p WHERE p.category_id IN (SELECT id FROM categories WHERE active = 1)")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("mysql.slow_log"), any()))
                .thenReturn(Flux.fromIterable(slowQueries));

            // When: 分析慢查詢日誌
            Flux<Map<String, Object>> result = schemaManagementTool.getSlowQueries(
                connectionId, "2024-01-15", 10);

            // Then: 驗證慢查詢分析結果
            StepVerifier.create(result)
                .expectNext(slowQueries.get(0))
                .expectNext(slowQueries.get(1))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("mysql.slow_log"), any());
        }
    }

    @Nested
    @DisplayName("資料庫統計資訊")
    class DatabaseStatistics {

        @Test
        @DisplayName("作為容量規劃師，我希望能獲取資料庫大小統計，以便進行容量規劃")
        void should_get_database_size_statistics() {
            // Given: 設定資料庫大小統計查詢結果
            String schemaName = "ecommerce";

            List<Map<String, Object>> sizeStats = Arrays.asList(
                Map.of("TABLE_SCHEMA", "ecommerce", "TABLE_NAME", "customers",
                       "TABLE_ROWS", 100000L, "DATA_LENGTH", 25165824L, "INDEX_LENGTH", 8388608L,
                       "DATA_FREE", 4194304L, "ENGINE", "InnoDB"),
                Map.of("TABLE_SCHEMA", "ecommerce", "TABLE_NAME", "orders",
                       "TABLE_ROWS", 250000L, "DATA_LENGTH", 67108864L, "INDEX_LENGTH", 16777216L,
                       "DATA_FREE", 2097152L, "ENGINE", "InnoDB"),
                Map.of("TABLE_SCHEMA", "ecommerce", "TABLE_NAME", "products",
                       "TABLE_ROWS", 50000L, "DATA_LENGTH", 15728640L, "INDEX_LENGTH", 5242880L,
                       "DATA_FREE", 1048576L, "ENGINE", "InnoDB")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TABLES"), any()))
                .thenReturn(Flux.fromIterable(sizeStats));

            // When: 獲取資料庫大小統計
            Flux<Map<String, Object>> result = schemaManagementTool.getDatabaseSizeStats(
                connectionId, schemaName);

            // Then: 驗證大小統計結果
            StepVerifier.create(result)
                .expectNext(sizeStats.get(0))
                .expectNext(sizeStats.get(1))
                .expectNext(sizeStats.get(2))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TABLES"), any());
        }

        @Test
        @DisplayName("作為監控專員，我希望能獲取資料庫連線統計，以便監控系統負載")
        void should_get_connection_statistics() {
            // Given: 設定連線統計查詢結果
            List<Map<String, Object>> connectionStats = Arrays.asList(
                Map.of("VARIABLE_NAME", "Threads_connected", "VARIABLE_VALUE", "25"),
                Map.of("VARIABLE_NAME", "Threads_running", "VARIABLE_VALUE", "5"),
                Map.of("VARIABLE_NAME", "Max_connections", "VARIABLE_VALUE", "1000"),
                Map.of("VARIABLE_NAME", "Connections", "VARIABLE_VALUE", "12500"),
                Map.of("VARIABLE_NAME", "Aborted_connects", "VARIABLE_VALUE", "15")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("SHOW STATUS"), any()))
                .thenReturn(Flux.fromIterable(connectionStats));

            // When: 獲取連線統計
            Flux<Map<String, Object>> result = schemaManagementTool.getConnectionStats(connectionId);

            // Then: 驗證連線統計結果
            StepVerifier.create(result)
                .expectNext(connectionStats.get(0))
                .expectNext(connectionStats.get(1))
                .expectNext(connectionStats.get(2))
                .expectNext(connectionStats.get(3))
                .expectNext(connectionStats.get(4))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("SHOW STATUS"), any());
        }

        @Test
        @DisplayName("作為效能分析師，我希望能獲取 InnoDB 引擎統計，以便分析儲存引擎效能")
        void should_get_innodb_statistics() {
            // Given: 設定 InnoDB 統計查詢結果
            List<Map<String, Object>> innodbStats = Arrays.asList(
                Map.of("VARIABLE_NAME", "Innodb_buffer_pool_pages_total", "VARIABLE_VALUE", "8192"),
                Map.of("VARIABLE_NAME", "Innodb_buffer_pool_pages_free", "VARIABLE_VALUE", "1024"),
                Map.of("VARIABLE_NAME", "Innodb_buffer_pool_pages_data", "VARIABLE_VALUE", "7000"),
                Map.of("VARIABLE_NAME", "Innodb_buffer_pool_pages_dirty", "VARIABLE_VALUE", "200"),
                Map.of("VARIABLE_NAME", "Innodb_buffer_pool_read_requests", "VARIABLE_VALUE", "15000000"),
                Map.of("VARIABLE_NAME", "Innodb_buffer_pool_reads", "VARIABLE_VALUE", "50000")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("SHOW STATUS LIKE 'Innodb%'"), any()))
                .thenReturn(Flux.fromIterable(innodbStats));

            // When: 獲取 InnoDB 統計
            Flux<Map<String, Object>> result = schemaManagementTool.getInnoDBStats(connectionId);

            // Then: 驗證 InnoDB 統計結果
            StepVerifier.create(result)
                .expectNext(innodbStats.get(0))
                .expectNext(innodbStats.get(1))
                .expectNext(innodbStats.get(2))
                .expectNext(innodbStats.get(3))
                .expectNext(innodbStats.get(4))
                .expectNext(innodbStats.get(5))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("SHOW STATUS LIKE 'Innodb%'"), any());
        }
    }

    @Nested
    @DisplayName("MySQL 特有 Schema 功能")
    class MySQLSpecificSchemaFeatures {

        @Test
        @DisplayName("作為資料庫管理員，我希望能分析 MySQL 分割區資訊，以便最佳化大表效能")
        void should_analyze_partition_information() {
            // Given: 設定分割區資訊查詢結果
            String tableName = "order_history";
            String schemaName = "ecommerce";

            List<Map<String, Object>> partitionInfo = Arrays.asList(
                Map.of("PARTITION_NAME", "p202301", "PARTITION_EXPRESSION", "YEAR(created_at)",
                       "PARTITION_DESCRIPTION", "2023", "TABLE_ROWS", 50000L,
                       "DATA_LENGTH", 15728640L, "INDEX_LENGTH", 5242880L),
                Map.of("PARTITION_NAME", "p202302", "PARTITION_EXPRESSION", "YEAR(created_at)",
                       "PARTITION_DESCRIPTION", "2023", "TABLE_ROWS", 48000L,
                       "DATA_LENGTH", 14680064L, "INDEX_LENGTH", 4194304L),
                Map.of("PARTITION_NAME", "p202401", "PARTITION_EXPRESSION", "YEAR(created_at)",
                       "PARTITION_DESCRIPTION", "2024", "TABLE_ROWS", 75000L,
                       "DATA_LENGTH", 20971520L, "INDEX_LENGTH", 6291456L)
            );

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.PARTITIONS"), any()))
                .thenReturn(Flux.fromIterable(partitionInfo));

            // When: 分析分割區資訊
            Flux<Map<String, Object>> result = schemaManagementTool.getPartitionInfo(
                connectionId, tableName, schemaName);

            // Then: 驗證分割區資訊
            StepVerifier.create(result)
                .expectNext(partitionInfo.get(0))
                .expectNext(partitionInfo.get(1))
                .expectNext(partitionInfo.get(2))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.PARTITIONS"), any());
        }

        @Test
        @DisplayName("作為系統架構師，我希望能分析觸發器資訊，以便了解業務邏輯實現")
        void should_analyze_trigger_information() {
            // Given: 設定觸發器資訊查詢結果
            String schemaName = "ecommerce";

            List<Map<String, Object>> triggerInfo = Arrays.asList(
                Map.of("TRIGGER_NAME", "audit_customer_updates", "EVENT_MANIPULATION", "UPDATE",
                       "EVENT_OBJECT_TABLE", "customers", "ACTION_TIMING", "AFTER",
                       "ACTION_STATEMENT", "INSERT INTO audit_log (table_name, action, user, timestamp) VALUES ('customers', 'UPDATE', USER(), NOW())",
                       "CREATED", "2024-01-15 10:30:00"),
                Map.of("TRIGGER_NAME", "order_total_validation", "EVENT_MANIPULATION", "INSERT",
                       "EVENT_OBJECT_TABLE", "orders", "ACTION_TIMING", "BEFORE",
                       "ACTION_STATEMENT", "IF NEW.total_amount <= 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Total amount must be positive'; END IF",
                       "CREATED", "2024-01-10 14:20:00")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TRIGGERS"), any()))
                .thenReturn(Flux.fromIterable(triggerInfo));

            // When: 分析觸發器資訊
            Flux<Map<String, Object>> result = schemaManagementTool.getTriggerInfo(
                connectionId, schemaName);

            // Then: 驗證觸發器資訊
            StepVerifier.create(result)
                .expectNext(triggerInfo.get(0))
                .expectNext(triggerInfo.get(1))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TRIGGERS"), any());
        }

        @Test
        @DisplayName("作為安全管理員，我希望能分析使用者權限資訊，以便進行安全審查")
        void should_analyze_user_privileges() {
            // Given: 設定使用者權限查詢結果
            List<Map<String, Object>> privilegeInfo = Arrays.asList(
                Map.of("GRANTEE", "'app_user'@'%'", "TABLE_SCHEMA", "ecommerce", "TABLE_NAME", "customers",
                       "PRIVILEGE_TYPE", "SELECT", "IS_GRANTABLE", "NO"),
                Map.of("GRANTEE", "'app_user'@'%'", "TABLE_SCHEMA", "ecommerce", "TABLE_NAME", "customers",
                       "PRIVILEGE_TYPE", "INSERT", "IS_GRANTABLE", "NO"),
                Map.of("GRANTEE", "'admin_user'@'localhost'", "TABLE_SCHEMA", "ecommerce", "TABLE_NAME", null,
                       "PRIVILEGE_TYPE", "ALL PRIVILEGES", "IS_GRANTABLE", "YES")
            );

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TABLE_PRIVILEGES"), any()))
                .thenReturn(Flux.fromIterable(privilegeInfo));

            // When: 分析使用者權限
            Flux<Map<String, Object>> result = schemaManagementTool.getUserPrivileges(
                connectionId, "ecommerce");

            // Then: 驗證使用者權限資訊
            StepVerifier.create(result)
                .expectNext(privilegeInfo.get(0))
                .expectNext(privilegeInfo.get(1))
                .expectNext(privilegeInfo.get(2))
                .verifyComplete();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TABLE_PRIVILEGES"), any());
        }
    }

    @Nested
    @DisplayName("Schema 管理錯誤處理")
    class SchemaManagementErrorHandling {

        @Test
        @DisplayName("作為系統管理員，我希望系統能妥善處理表不存在的錯誤，以便快速定位問題")
        void should_handle_table_not_exists_error() {
            // Given: 設定表不存在的查詢
            String nonExistentTable = "non_existent_table";
            String schemaName = "ecommerce";

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.COLUMNS"), any()))
                .thenReturn(Flux.error(new RuntimeException("Table 'ecommerce.non_existent_table' doesn't exist")));

            // When: 查詢不存在的表結構
            Flux<Map<String, Object>> result = schemaManagementTool.getTableSchema(
                connectionId, nonExistentTable, schemaName);

            // Then: 驗證能正確處理表不存在錯誤
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.COLUMNS"), any());
        }

        @Test
        @DisplayName("作為開發人員，我希望系統能處理權限不足錯誤，以便了解存取限制")
        void should_handle_permission_denied_error() {
            // Given: 設定權限不足的查詢
            String restrictedSchema = "mysql";

            when(connectionService.executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TABLES"), any()))
                .thenReturn(Flux.error(new RuntimeException("Access denied for user 'app_user'@'%' to database 'mysql'")));

            // When: 嘗試存取受限制的 Schema
            Flux<Map<String, Object>> result = schemaManagementTool.listTables(
                connectionId, restrictedSchema);

            // Then: 驗證能正確處理權限錯誤
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

            verify(connectionService).executeQuery(eq(connectionId), contains("INFORMATION_SCHEMA.TABLES"), any());
        }

        @Test
        @DisplayName("作為監控專員，我希望系統能處理性能統計表不可用錯誤，以便提供替代方案")
        void should_handle_performance_schema_unavailable() {
            // Given: 設定 performance_schema 不可用的情境
            when(connectionService.executeQuery(eq(connectionId), contains("performance_schema"), any()))
                .thenReturn(Flux.error(new RuntimeException("Unknown database 'performance_schema'")));

            // When: 嘗試查詢 performance_schema
            Flux<Map<String, Object>> result = schemaManagementTool.getIndexUsageStats(
                connectionId, "ecommerce");

            // Then: 驗證能正確處理 performance_schema 不可用錯誤
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

            verify(connectionService).executeQuery(eq(connectionId), contains("performance_schema"), any());
        }
    }
}