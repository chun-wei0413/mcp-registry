package com.mcp.postgresql.tool;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.postgresql.service.DatabaseConnectionService;
import com.mcpregistry.core.entity.*;
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
 * PostgreSQL Schema 管理工具 BDD 測試
 *
 * 功能：作為資料庫架構師，我希望能夠管理和分析 PostgreSQL 資料庫的 Schema 結構
 * 以便我可以了解表結構、索引配置、約束關係，並進行查詢性能分析
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostgreSQL Schema 管理工具")
class SchemaManagementToolBDDTest {

    @Mock
    private DatabaseConnectionService connectionService;

    private SchemaManagementTool schemaTool;

    @BeforeEach
    void setUp() {
        schemaTool = new SchemaManagementTool(connectionService);
    }

    @Nested
    @DisplayName("工具基本資訊")
    class ToolBasicInformation {

        @Test
        @DisplayName("場景：驗證 PostgreSQL Schema 工具名稱")
        void shouldReturnCorrectToolName() {
            // Given: PostgreSQL Schema 管理工具已初始化
            // When: 請求工具名稱
            String toolName = schemaTool.getToolName();

            // Then: 應該返回 PostgreSQL 專用的 Schema 工具名稱
            assertEquals("postgresql_schema_management", toolName);
        }

        @Test
        @DisplayName("場景：驗證工具描述包含關鍵資訊")
        void shouldReturnInformativeDescription() {
            // Given: PostgreSQL Schema 管理工具已初始化
            // When: 請求工具描述
            String description = schemaTool.getDescription();

            // Then: 描述應該包含 PostgreSQL 和 Schema 管理關鍵字
            assertNotNull(description);
            assertTrue(description.contains("PostgreSQL"));
            assertTrue(description.contains("Schema"));
        }

        @Test
        @DisplayName("場景：驗證參數結構定義完整")
        void shouldReturnValidParameterSchema() {
            // Given: PostgreSQL Schema 管理工具已初始化
            // When: 請求參數結構定義
            Map<String, Object> schema = schemaTool.getParameterSchema();

            // Then: 應該返回完整的 JSON Schema 結構
            assertNotNull(schema);
            assertTrue(schema.containsKey("type"));
            assertTrue(schema.containsKey("properties"));
            assertTrue(schema.containsKey("required"));
        }
    }

    @Nested
    @DisplayName("表結構分析")
    class TableSchemaAnalysis {

        @Test
        @DisplayName("場景：分析完整的表結構資訊")
        void shouldAnalyzeCompleteTableSchema() {
            // Given: PostgreSQL 資料庫中存在一個具有完整結構的用戶表
            List<ColumnInfo> userTableColumns = List.of(
                ColumnInfo.builder()
                    .columnName("id")
                    .dataType("integer")
                    .isNullable(false)
                    .columnDefault("nextval('users_id_seq'::regclass)")
                    .isPrimaryKey(true)
                    .isAutoIncrement(true)
                    .build(),
                ColumnInfo.builder()
                    .columnName("username")
                    .dataType("varchar(50)")
                    .isNullable(false)
                    .isUnique(true)
                    .build(),
                ColumnInfo.builder()
                    .columnName("email")
                    .dataType("varchar(255)")
                    .isNullable(false)
                    .isUnique(true)
                    .build(),
                ColumnInfo.builder()
                    .columnName("created_at")
                    .dataType("timestamp")
                    .isNullable(false)
                    .columnDefault("CURRENT_TIMESTAMP")
                    .build(),
                ColumnInfo.builder()
                    .columnName("last_login")
                    .dataType("timestamp")
                    .isNullable(true)
                    .build()
            );

            List<IndexInfo> userTableIndexes = List.of(
                IndexInfo.builder()
                    .indexName("users_pkey")
                    .columnNames(List.of("id"))
                    .isUnique(true)
                    .indexType("btree")
                    .build(),
                IndexInfo.builder()
                    .indexName("users_username_key")
                    .columnNames(List.of("username"))
                    .isUnique(true)
                    .indexType("btree")
                    .build(),
                IndexInfo.builder()
                    .indexName("idx_users_email")
                    .columnNames(List.of("email"))
                    .isUnique(true)
                    .indexType("btree")
                    .build(),
                IndexInfo.builder()
                    .indexName("idx_users_created_at")
                    .columnNames(List.of("created_at"))
                    .isUnique(false)
                    .indexType("btree")
                    .build()
            );

            TableSchema mockUserSchema = TableSchema.builder()
                .tableName("users")
                .schemaName("public")
                .tableType("BASE TABLE")
                .columns(userTableColumns)
                .indexes(userTableIndexes)
                .rowCount(15000L)
                .comment("系統用戶主表")
                .build();

            when(connectionService.getTableSchema(anyString(), anyString(), anyString()))
                .thenReturn(mockUserSchema);

            // And: 提供表結構分析請求
            Map<String, Object> arguments = Map.of(
                "action", "getTableSchema",
                "connectionId", "prod-postgres",
                "tableName", "users",
                "schemaName", "public"
            );

            // When: 執行表結構分析
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該成功返回完整的表結構資訊
            assertTrue(result.isSuccess());
            assertEquals("表格結構獲取完成", result.getContent());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：分析具有外鍵約束的表結構")
        void shouldAnalyzeTableWithForeignKeyConstraints() {
            // Given: PostgreSQL 資料庫中存在具有外鍵約束的訂單表
            List<ColumnInfo> orderTableColumns = List.of(
                ColumnInfo.builder()
                    .columnName("id")
                    .dataType("integer")
                    .isNullable(false)
                    .isPrimaryKey(true)
                    .isAutoIncrement(true)
                    .build(),
                ColumnInfo.builder()
                    .columnName("user_id")
                    .dataType("integer")
                    .isNullable(false)
                    .isForeignKey(true)
                    .referencedTable("users")
                    .referencedColumn("id")
                    .build(),
                ColumnInfo.builder()
                    .columnName("total_amount")
                    .dataType("numeric(12,2)")
                    .isNullable(false)
                    .build()
            );

            TableSchema mockOrderSchema = TableSchema.builder()
                .tableName("orders")
                .schemaName("public")
                .tableType("BASE TABLE")
                .columns(orderTableColumns)
                .rowCount(45000L)
                .comment("訂單資料表")
                .build();

            when(connectionService.getTableSchema(anyString(), anyString(), anyString()))
                .thenReturn(mockOrderSchema);

            // And: 提供外鍵表的分析請求
            Map<String, Object> arguments = Map.of(
                "action", "getTableSchema",
                "connectionId", "prod-postgres",
                "tableName", "orders",
                "schemaName", "public"
            );

            // When: 執行外鍵表結構分析
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該成功返回包含外鍵資訊的表結構
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：分析視圖結構")
        void shouldAnalyzeViewStructure() {
            // Given: PostgreSQL 資料庫中存在業務視圖
            List<ColumnInfo> viewColumns = List.of(
                ColumnInfo.builder()
                    .columnName("user_id")
                    .dataType("integer")
                    .isNullable(false)
                    .build(),
                ColumnInfo.builder()
                    .columnName("username")
                    .dataType("varchar(50)")
                    .isNullable(false)
                    .build(),
                ColumnInfo.builder()
                    .columnName("total_orders")
                    .dataType("bigint")
                    .isNullable(true)
                    .build(),
                ColumnInfo.builder()
                    .columnName("total_spent")
                    .dataType("numeric")
                    .isNullable(true)
                    .build()
            );

            TableSchema mockViewSchema = TableSchema.builder()
                .tableName("user_order_summary")
                .schemaName("public")
                .tableType("VIEW")
                .columns(viewColumns)
                .comment("用戶訂單統計視圖")
                .build();

            when(connectionService.getTableSchema(anyString(), anyString(), anyString()))
                .thenReturn(mockViewSchema);

            // And: 提供視圖分析請求
            Map<String, Object> arguments = Map.of(
                "action", "getTableSchema",
                "connectionId", "analytics-postgres",
                "tableName", "user_order_summary",
                "schemaName", "public"
            );

            // When: 執行視圖結構分析
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該成功返回視圖結構資訊
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }
    }

    @Nested
    @DisplayName("Schema 探索")
    class SchemaExploration {

        @Test
        @DisplayName("場景：列出 Schema 中的所有表格")
        void shouldListAllTablesInSchema() {
            // Given: PostgreSQL 資料庫的 public schema 中存在多個表格
            List<TableInfo> mockTables = List.of(
                TableInfo.builder()
                    .tableName("users")
                    .tableType("BASE TABLE")
                    .schemaName("public")
                    .rowCount(15000L)
                    .comment("系統用戶表")
                    .build(),
                TableInfo.builder()
                    .tableName("orders")
                    .tableType("BASE TABLE")
                    .schemaName("public")
                    .rowCount(45000L)
                    .comment("訂單資料表")
                    .build(),
                TableInfo.builder()
                    .tableName("products")
                    .tableType("BASE TABLE")
                    .schemaName("public")
                    .rowCount(2500L)
                    .comment("產品目錄表")
                    .build(),
                TableInfo.builder()
                    .tableName("user_order_summary")
                    .tableType("VIEW")
                    .schemaName("public")
                    .comment("用戶訂單統計視圖")
                    .build()
            );

            when(connectionService.listTables(anyString(), anyString()))
                .thenReturn(mockTables);

            // And: 提供列表查詢請求
            Map<String, Object> arguments = Map.of(
                "action", "listTables",
                "connectionId", "prod-postgres",
                "schemaName", "public"
            );

            // When: 執行表格列表查詢
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該成功返回所有表格和視圖的資訊
            assertTrue(result.isSuccess());
            assertEquals("表格列表獲取完成", result.getContent());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：列出資料庫中的所有 Schema")
        void shouldListAllSchemasInDatabase() {
            // Given: PostgreSQL 資料庫中存在多個 Schema
            List<String> mockSchemas = List.of(
                "public",
                "information_schema",
                "pg_catalog",
                "business_analytics",
                "user_management",
                "order_processing"
            );

            when(connectionService.listSchemas(anyString()))
                .thenReturn(mockSchemas);

            // And: 提供 Schema 列表查詢請求
            Map<String, Object> arguments = Map.of(
                "action", "listSchemas",
                "connectionId", "enterprise-postgres"
            );

            // When: 執行 Schema 列表查詢
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該成功返回所有 Schema 名稱
            assertTrue(result.isSuccess());
            assertEquals("Schema 列表獲取完成", result.getContent());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：按類型篩選表格")
        void shouldFilterTablesByType() {
            // Given: 需要只查看特定類型的資料庫物件
            List<TableInfo> mockViews = List.of(
                TableInfo.builder()
                    .tableName("monthly_sales_report")
                    .tableType("VIEW")
                    .schemaName("analytics")
                    .comment("月度銷售報告視圖")
                    .build(),
                TableInfo.builder()
                    .tableName("customer_insights")
                    .tableType("VIEW")
                    .schemaName("analytics")
                    .comment("客戶洞察分析視圖")
                    .build()
            );

            when(connectionService.listTables(anyString(), anyString()))
                .thenReturn(mockViews);

            // And: 提供視圖篩選請求
            Map<String, Object> arguments = Map.of(
                "action", "listTables",
                "connectionId", "analytics-postgres",
                "schemaName", "analytics",
                "tableType", "VIEW"
            );

            // When: 執行視圖篩選查詢
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該只返回視圖類型的物件
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }
    }

    @Nested
    @DisplayName("索引分析")
    class IndexAnalysis {

        @Test
        @DisplayName("場景：分析表格的所有索引")
        void shouldAnalyzeAllTableIndexes() {
            // Given: PostgreSQL 表格具有多種類型的索引
            List<IndexInfo> mockIndexes = List.of(
                IndexInfo.builder()
                    .indexName("users_pkey")
                    .tableName("users")
                    .columnNames(List.of("id"))
                    .isUnique(true)
                    .indexType("btree")
                    .indexSize("64 kB")
                    .build(),
                IndexInfo.builder()
                    .indexName("idx_users_email")
                    .tableName("users")
                    .columnNames(List.of("email"))
                    .isUnique(true)
                    .indexType("btree")
                    .indexSize("128 kB")
                    .build(),
                IndexInfo.builder()
                    .indexName("idx_users_created_at")
                    .tableName("users")
                    .columnNames(List.of("created_at"))
                    .isUnique(false)
                    .indexType("btree")
                    .indexSize("96 kB")
                    .build(),
                IndexInfo.builder()
                    .indexName("idx_users_fulltext")
                    .tableName("users")
                    .columnNames(List.of("username", "email"))
                    .isUnique(false)
                    .indexType("gin")
                    .indexSize("256 kB")
                    .build()
            );

            when(connectionService.getTableIndexes(anyString(), anyString(), anyString()))
                .thenReturn(mockIndexes);

            // And: 提供索引分析請求
            Map<String, Object> arguments = Map.of(
                "action", "getTableIndexes",
                "connectionId", "prod-postgres",
                "tableName", "users",
                "schemaName", "public"
            );

            // When: 執行索引分析
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該成功返回所有索引的詳細資訊
            assertTrue(result.isSuccess());
            assertEquals("索引資訊獲取完成", result.getContent());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：識別複合索引結構")
        void shouldIdentifyCompositeIndexStructure() {
            // Given: 表格具有複合索引用於查詢優化
            List<IndexInfo> mockCompositeIndexes = List.of(
                IndexInfo.builder()
                    .indexName("idx_orders_user_date")
                    .tableName("orders")
                    .columnNames(List.of("user_id", "created_at"))
                    .isUnique(false)
                    .indexType("btree")
                    .indexSize("512 kB")
                    .build(),
                IndexInfo.builder()
                    .indexName("idx_orders_status_amount")
                    .tableName("orders")
                    .columnNames(List.of("status", "total_amount"))
                    .isUnique(false)
                    .indexType("btree")
                    .indexSize("384 kB")
                    .build()
            );

            when(connectionService.getTableIndexes(anyString(), anyString(), anyString()))
                .thenReturn(mockCompositeIndexes);

            // And: 提供複合索引分析請求
            Map<String, Object> arguments = Map.of(
                "action", "getTableIndexes",
                "connectionId", "prod-postgres",
                "tableName", "orders",
                "schemaName", "public"
            );

            // When: 執行複合索引分析
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該正確識別複合索引的結構
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
        }
    }

    @Nested
    @DisplayName("查詢性能分析")
    class QueryPerformanceAnalysis {

        @Test
        @DisplayName("場景：分析查詢執行計畫")
        void shouldAnalyzeQueryExecutionPlan() {
            // Given: 需要分析複雜查詢的執行效率
            QueryResult mockExplainResult = QueryResult.builder()
                .queryId("explain-complex-1")
                .columnNames(List.of("QUERY PLAN"))
                .rows(List.of(
                    Map.of("QUERY PLAN", "Nested Loop  (cost=0.29..856.45 rows=100 width=68)"),
                    Map.of("QUERY PLAN", "  ->  Seq Scan on orders o  (cost=0.00..22.50 rows=100 width=36)"),
                    Map.of("QUERY PLAN", "        Filter: (created_at >= '2024-01-01'::date)"),
                    Map.of("QUERY PLAN", "  ->  Index Scan using users_pkey on users u  (cost=0.29..8.31 rows=1 width=32)"),
                    Map.of("QUERY PLAN", "        Index Cond: (id = o.user_id)")
                ))
                .executionTimeMs(12)
                .build();

            when(connectionService.explainQuery(anyString(), anyString(), anyBoolean()))
                .thenReturn(mockExplainResult);

            // And: 提供查詢計畫分析請求
            Map<String, Object> arguments = Map.of(
                "action", "explainQuery",
                "connectionId", "prod-postgres",
                "query", """
                    SELECT u.username, COUNT(o.id) as order_count
                    FROM users u
                    INNER JOIN orders o ON u.id = o.user_id
                    WHERE o.created_at >= '2024-01-01'
                    GROUP BY u.id, u.username
                    """,
                "analyze", false
            );

            // When: 執行查詢計畫分析
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該成功返回查詢執行計畫
            assertTrue(result.isSuccess());
            assertEquals("查詢計畫分析完成", result.getContent());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：進行查詢性能實測分析")
        void shouldPerformQueryPerformanceAnalysis() {
            // Given: 需要進行實際執行的性能分析
            QueryResult mockAnalyzeResult = QueryResult.builder()
                .queryId("explain-analyze-1")
                .columnNames(List.of("QUERY PLAN"))
                .rows(List.of(
                    Map.of("QUERY PLAN", "Nested Loop  (cost=0.29..856.45 rows=100 width=68) (actual time=0.125..15.234 rows=150 loops=1)"),
                    Map.of("QUERY PLAN", "  ->  Seq Scan on orders o  (cost=0.00..22.50 rows=100 width=36) (actual time=0.015..2.345 rows=150 loops=1)"),
                    Map.of("QUERY PLAN", "        Filter: (created_at >= '2024-01-01'::date)"),
                    Map.of("QUERY PLAN", "        Rows Removed by Filter: 50"),
                    Map.of("QUERY PLAN", "  ->  Index Scan using users_pkey on users u  (cost=0.29..8.31 rows=1 width=32) (actual time=0.025..0.028 rows=1 loops=150)"),
                    Map.of("QUERY PLAN", "        Index Cond: (id = o.user_id)"),
                    Map.of("QUERY PLAN", "Planning Time: 0.234 ms"),
                    Map.of("QUERY PLAN", "Execution Time: 15.567 ms")
                ))
                .executionTimeMs(45)
                .build();

            when(connectionService.explainQuery(anyString(), anyString(), anyBoolean()))
                .thenReturn(mockAnalyzeResult);

            // And: 提供實測性能分析請求
            Map<String, Object> arguments = Map.of(
                "action", "explainQuery",
                "connectionId", "prod-postgres",
                "query", """
                    SELECT u.username, COUNT(o.id) as order_count, SUM(o.total_amount) as total_spent
                    FROM users u
                    INNER JOIN orders o ON u.id = o.user_id
                    WHERE o.created_at >= '2024-01-01'
                    GROUP BY u.id, u.username
                    HAVING COUNT(o.id) > 5
                    ORDER BY total_spent DESC
                    """,
                "analyze", true
            );

            // When: 執行實測性能分析
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該返回包含實際執行時間的詳細分析
            assertTrue(result.isSuccess());
            assertEquals("查詢計畫分析完成", result.getContent());
            assertNotNull(result.getData());
        }
    }

    @Nested
    @DisplayName("資料庫統計資訊")
    class DatabaseStatistics {

        @Test
        @DisplayName("場景：獲取資料庫整體大小資訊")
        void shouldGetDatabaseSizeInformation() {
            // Given: PostgreSQL 資料庫包含多個表格和索引
            Map<String, Object> mockSizeInfo = Map.of(
                "database_name", "production_app",
                "size_bytes", 2147483648L,  // 2GB
                "size_human", "2.0 GB",
                "table_count", 45,
                "index_count", 128,
                "schema_count", 6
            );

            when(connectionService.getDatabaseSize(anyString()))
                .thenReturn(mockSizeInfo);

            // And: 提供資料庫大小查詢請求
            Map<String, Object> arguments = Map.of(
                "action", "getDatabaseSize",
                "connectionId", "prod-postgres"
            );

            // When: 執行資料庫大小分析
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該返回詳細的資料庫大小統計
            assertTrue(result.isSuccess());
            assertEquals("資料庫大小資訊獲取完成", result.getContent());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：分析特定表格的統計資訊")
        void shouldAnalyzeSpecificTableStatistics() {
            // Given: 需要了解核心業務表格的統計資訊
            Map<String, Object> mockTableStats = Map.of(
                "table_name", "orders",
                "schema_name", "public",
                "row_count", 45000L,
                "table_size_bytes", 12582912L,  // 12MB
                "table_size_human", "12 MB",
                "index_size_bytes", 8388608L,   // 8MB
                "index_size_human", "8 MB",
                "total_size_bytes", 20971520L,  // 20MB
                "total_size_human", "20 MB",
                "last_vacuum", "2024-01-15 10:30:00",
                "last_analyze", "2024-01-15 10:35:00"
            );

            when(connectionService.getTableStats(anyString(), anyString(), anyString()))
                .thenReturn(mockTableStats);

            // And: 提供表格統計分析請求
            Map<String, Object> arguments = Map.of(
                "action", "getTableStats",
                "connectionId", "prod-postgres",
                "tableName", "orders",
                "schemaName", "public"
            );

            // When: 執行表格統計分析
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該返回詳細的表格統計資訊
            assertTrue(result.isSuccess());
            assertEquals("表格統計資訊獲取完成", result.getContent());
            assertNotNull(result.getData());
        }
    }

    @Nested
    @DisplayName("Schema 管理錯誤處理")
    class SchemaManagementErrorHandling {

        @Test
        @DisplayName("場景：連線 ID 為空時應該失敗")
        void shouldFailWhenConnectionIdIsEmpty() {
            // Given: 提供空的連線 ID
            Map<String, Object> arguments = Map.of(
                "action", "listTables",
                "connectionId", ""
            );

            // When: 嘗試執行 Schema 查詢
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該失敗並返回連線 ID 錯誤
            assertFalse(result.isSuccess());
            assertEquals("Connection ID 不能為空", result.getError());
        }

        @Test
        @DisplayName("場景：不支援的操作類型應該失敗")
        void shouldFailWithUnsupportedAction() {
            // Given: 提供不支援的操作類型
            Map<String, Object> arguments = Map.of(
                "action", "createTable",  // 不支援的操作
                "connectionId", "prod-postgres"
            );

            // When: 嘗試執行不支援的 Schema 操作
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該失敗並返回操作不支援錯誤
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("不支援的操作"));
        }

        @Test
        @DisplayName("場景：表格名稱為空時應該失敗")
        void shouldFailWhenTableNameIsEmpty() {
            // Given: 提供空的表格名稱
            Map<String, Object> arguments = Map.of(
                "action", "getTableSchema",
                "connectionId", "prod-postgres",
                "tableName", "",
                "schemaName", "public"
            );

            // When: 嘗試查詢空表格名稱的結構
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該失敗並返回表格名稱錯誤
            assertFalse(result.isSuccess());
            assertEquals("表格名稱不能為空", result.getError());
        }

        @Test
        @DisplayName("場景：表格不存在時應該失敗")
        void shouldFailWhenTableNotFound() {
            // Given: 資料庫連線服務拋出表格不存在錯誤
            when(connectionService.getTableSchema(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Table 'non_existent_table' doesn't exist"));

            // And: 提供不存在的表格名稱
            Map<String, Object> arguments = Map.of(
                "action", "getTableSchema",
                "connectionId", "prod-postgres",
                "tableName", "non_existent_table",
                "schemaName", "public"
            );

            // When: 嘗試查詢不存在的表格結構
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該失敗並包含表格不存在的錯誤訊息
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("doesn't exist"));
        }

        @Test
        @DisplayName("場景：連線失效時應該失敗")
        void shouldFailWhenConnectionIsInvalid() {
            // Given: 資料庫連線已失效或不可用
            when(connectionService.listTables(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection 'invalid-postgres' not found"));

            // And: 提供失效的連線 ID
            Map<String, Object> arguments = Map.of(
                "action", "listTables",
                "connectionId", "invalid-postgres",
                "schemaName", "public"
            );

            // When: 嘗試使用失效連線查詢表格列表
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該失敗並包含連線錯誤訊息
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("not found"));
        }

        @Test
        @DisplayName("場景：權限不足時應該失敗")
        void shouldFailWhenInsufficientPermissions() {
            // Given: 使用者沒有足夠權限存取特定 Schema
            when(connectionService.listTables(anyString(), anyString()))
                .thenThrow(new RuntimeException("permission denied for schema restricted_schema"));

            // And: 提供受限制的 Schema 查詢
            Map<String, Object> arguments = Map.of(
                "action", "listTables",
                "connectionId", "limited-user-postgres",
                "schemaName", "restricted_schema"
            );

            // When: 嘗試存取受限制的 Schema
            McpToolResult result = schemaTool.execute(arguments);

            // Then: 應該失敗並包含權限錯誤訊息
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("permission denied"));
        }
    }
}