package com.mcp.mysql.tool;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.mysql.service.DatabaseConnectionService;
import com.mcpregistry.core.entity.*;
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
 * MySQL Schema 管理工具測試
 */
@ExtendWith(MockitoExtension.class)
class SchemaManagementToolTest {

    @Mock
    private DatabaseConnectionService connectionService;

    private SchemaManagementTool schemaTool;

    @BeforeEach
    void setUp() {
        schemaTool = new SchemaManagementTool(connectionService);
    }

    @Test
    void shouldReturnCorrectToolName() {
        assertEquals("mysql_schema_management", schemaTool.getToolName());
    }

    @Test
    void shouldReturnCorrectDescription() {
        String description = schemaTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("MySQL"));
        assertTrue(description.contains("Schema"));
    }

    @Test
    void shouldReturnValidParameterSchema() {
        Map<String, Object> schema = schemaTool.getParameterSchema();
        assertNotNull(schema);
        assertTrue(schema.containsKey("type"));
        assertTrue(schema.containsKey("properties"));
        assertTrue(schema.containsKey("required"));
    }

    @Test
    void shouldGetTableSchemaSuccessfully() {
        // Arrange
        List<ColumnInfo> columns = List.of(
            ColumnInfo.builder()
                .columnName("id")
                .dataType("int(11)")
                .isNullable(false)
                .columnDefault(null)
                .isPrimaryKey(true)
                .isAutoIncrement(true)
                .build(),
            ColumnInfo.builder()
                .columnName("name")
                .dataType("varchar(255)")
                .isNullable(false)
                .build(),
            ColumnInfo.builder()
                .columnName("email")
                .dataType("varchar(255)")
                .isNullable(true)
                .isUnique(true)
                .build(),
            ColumnInfo.builder()
                .columnName("age")
                .dataType("int(11)")
                .isNullable(true)
                .build(),
            ColumnInfo.builder()
                .columnName("created_at")
                .dataType("datetime")
                .isNullable(false)
                .columnDefault("CURRENT_TIMESTAMP")
                .build()
        );

        List<IndexInfo> indexes = List.of(
            IndexInfo.builder()
                .indexName("PRIMARY")
                .columnNames(List.of("id"))
                .isUnique(true)
                .indexType("BTREE")
                .build(),
            IndexInfo.builder()
                .indexName("email")
                .columnNames(List.of("email"))
                .isUnique(true)
                .indexType("BTREE")
                .build(),
            IndexInfo.builder()
                .indexName("idx_name_age")
                .columnNames(List.of("name", "age"))
                .isUnique(false)
                .indexType("BTREE")
                .build()
        );

        TableSchema mockSchema = TableSchema.builder()
            .tableName("users")
            .schemaName("testdb")
            .tableType("BASE TABLE")
            .columns(columns)
            .indexes(indexes)
            .rowCount(2500L)
            .engine("InnoDB")
            .collation("utf8mb4_unicode_ci")
            .build();

        when(connectionService.getTableSchema(anyString(), anyString(), anyString()))
            .thenReturn(mockSchema);

        Map<String, Object> arguments = Map.of(
            "action", "getTableSchema",
            "connectionId", "test-mysql-conn",
            "tableName", "users",
            "schemaName", "testdb"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 表格結構獲取完成", result.getContent());
    }

    @Test
    void shouldListTablesSuccessfully() {
        // Arrange
        List<TableInfo> tables = List.of(
            TableInfo.builder()
                .tableName("users")
                .tableType("BASE TABLE")
                .schemaName("testdb")
                .rowCount(2500L)
                .engine("InnoDB")
                .collation("utf8mb4_unicode_ci")
                .comment("用戶主表")
                .build(),
            TableInfo.builder()
                .tableName("orders")
                .tableType("BASE TABLE")
                .schemaName("testdb")
                .rowCount(8000L)
                .engine("InnoDB")
                .collation("utf8mb4_unicode_ci")
                .comment("訂單表")
                .build(),
            TableInfo.builder()
                .tableName("user_stats")
                .tableType("VIEW")
                .schemaName("testdb")
                .comment("用戶統計視圖")
                .build()
        );

        when(connectionService.listTables(anyString(), anyString()))
            .thenReturn(tables);

        Map<String, Object> arguments = Map.of(
            "action", "listTables",
            "connectionId", "test-mysql-conn",
            "schemaName", "testdb"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 表格列表獲取完成", result.getContent());
    }

    @Test
    void shouldListDatabasesSuccessfully() {
        // Arrange
        List<String> databases = List.of("testdb", "information_schema", "performance_schema", "mysql", "sys", "production_db");

        when(connectionService.listSchemas(anyString()))
            .thenReturn(databases);

        Map<String, Object> arguments = Map.of(
            "action", "listDatabases",
            "connectionId", "test-mysql-conn"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 資料庫列表獲取完成", result.getContent());
    }

    @Test
    void shouldExplainQuerySuccessfully() {
        // Arrange
        QueryResult explainResult = QueryResult.builder()
            .queryId("mysql-explain-1")
            .columnNames(List.of("id", "select_type", "table", "partitions", "type", "possible_keys", "key", "key_len", "ref", "rows", "filtered", "Extra"))
            .rows(List.of(
                Map.of(
                    "id", 1,
                    "select_type", "SIMPLE",
                    "table", "users",
                    "partitions", null,
                    "type", "range",
                    "possible_keys", "idx_age",
                    "key", "idx_age",
                    "key_len", "5",
                    "ref", null,
                    "rows", 500,
                    "filtered", 100.0,
                    "Extra", "Using index condition"
                )
            ))
            .executionTimeMs(10)
            .build();

        when(connectionService.explainQuery(anyString(), anyString(), anyBoolean()))
            .thenReturn(explainResult);

        Map<String, Object> arguments = Map.of(
            "action", "explainQuery",
            "connectionId", "test-mysql-conn",
            "query", "SELECT * FROM users WHERE age > 25",
            "analyze", false
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 查詢計畫分析完成", result.getContent());
    }

    @Test
    void shouldExplainAnalyzeQuerySuccessfully() {
        // Arrange
        QueryResult explainResult = QueryResult.builder()
            .queryId("mysql-explain-analyze-1")
            .columnNames(List.of("EXPLAIN"))
            .rows(List.of(
                Map.of("EXPLAIN", "-> Filter: (users.age > 25)  (cost=250.25 rows=500) (actual time=0.085..2.123 rows=300 loops=1)"),
                Map.of("EXPLAIN", "    -> Table scan on users  (cost=250.25 rows=2500) (actual time=0.075..1.890 rows=2500 loops=1)")
            ))
            .executionTimeMs(25)
            .build();

        when(connectionService.explainQuery(anyString(), anyString(), anyBoolean()))
            .thenReturn(explainResult);

        Map<String, Object> arguments = Map.of(
            "action", "explainQuery",
            "connectionId", "test-mysql-conn",
            "query", "SELECT * FROM users WHERE age > 25",
            "analyze", true
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 查詢計畫分析完成", result.getContent());
    }

    @Test
    void shouldGetTableIndexesSuccessfully() {
        // Arrange
        List<IndexInfo> indexes = List.of(
            IndexInfo.builder()
                .indexName("PRIMARY")
                .tableName("users")
                .columnNames(List.of("id"))
                .isUnique(true)
                .indexType("BTREE")
                .build(),
            IndexInfo.builder()
                .indexName("email")
                .tableName("users")
                .columnNames(List.of("email"))
                .isUnique(true)
                .indexType("BTREE")
                .build(),
            IndexInfo.builder()
                .indexName("idx_name_age")
                .tableName("users")
                .columnNames(List.of("name", "age"))
                .isUnique(false)
                .indexType("BTREE")
                .build(),
            IndexInfo.builder()
                .indexName("idx_created_at")
                .tableName("users")
                .columnNames(List.of("created_at"))
                .isUnique(false)
                .indexType("BTREE")
                .build()
        );

        when(connectionService.getTableIndexes(anyString(), anyString(), anyString()))
            .thenReturn(indexes);

        Map<String, Object> arguments = Map.of(
            "action", "getTableIndexes",
            "connectionId", "test-mysql-conn",
            "tableName", "users",
            "schemaName", "testdb"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 索引資訊獲取完成", result.getContent());
    }

    @Test
    void shouldGetDatabaseSizeSuccessfully() {
        // Arrange
        Map<String, Object> sizeInfo = Map.of(
            "database_name", "testdb",
            "size_bytes", 104857600L,
            "size_human", "100 MB",
            "table_count", 25,
            "view_count", 5
        );

        when(connectionService.getDatabaseSize(anyString()))
            .thenReturn(sizeInfo);

        Map<String, Object> arguments = Map.of(
            "action", "getDatabaseSize",
            "connectionId", "test-mysql-conn"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 資料庫大小資訊獲取完成", result.getContent());
    }

    @Test
    void shouldGetTableStatsSuccessfully() {
        // Arrange
        Map<String, Object> tableStats = Map.of(
            "table_name", "users",
            "engine", "InnoDB",
            "row_count", 2500L,
            "data_length", 2097152L,
            "data_size_human", "2 MB",
            "index_length", 1048576L,
            "index_size_human", "1 MB",
            "total_size_bytes", 3145728L,
            "total_size_human", "3 MB",
            "avg_row_length", 838L,
            "auto_increment", 2501L
        );

        when(connectionService.getTableStats(anyString(), anyString(), anyString()))
            .thenReturn(tableStats);

        Map<String, Object> arguments = Map.of(
            "action", "getTableStats",
            "connectionId", "test-mysql-conn",
            "tableName", "users",
            "schemaName", "testdb"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 表格統計資訊獲取完成", result.getContent());
    }

    @Test
    void shouldGetForeignKeysSuccessfully() {
        // Arrange
        List<ForeignKeyInfo> foreignKeys = List.of(
            ForeignKeyInfo.builder()
                .constraintName("fk_orders_user_id")
                .tableName("orders")
                .columnName("user_id")
                .referencedTableName("users")
                .referencedColumnName("id")
                .onDelete("CASCADE")
                .onUpdate("RESTRICT")
                .build(),
            ForeignKeyInfo.builder()
                .constraintName("fk_order_items_order_id")
                .tableName("order_items")
                .columnName("order_id")
                .referencedTableName("orders")
                .referencedColumnName("id")
                .onDelete("CASCADE")
                .onUpdate("CASCADE")
                .build()
        );

        when(connectionService.getTableForeignKeys(anyString(), anyString(), anyString()))
            .thenReturn(foreignKeys);

        Map<String, Object> arguments = Map.of(
            "action", "getForeignKeys",
            "connectionId", "test-mysql-conn",
            "tableName", "orders",
            "schemaName", "testdb"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 外鍵資訊獲取完成", result.getContent());
    }

    @Test
    void shouldShowCreateTableSuccessfully() {
        // Arrange
        String createTableSql = """
            CREATE TABLE `users` (
              `id` int(11) NOT NULL AUTO_INCREMENT,
              `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
              `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
              `age` int(11) DEFAULT NULL,
              `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (`id`),
              UNIQUE KEY `email` (`email`),
              KEY `idx_name_age` (`name`,`age`)
            ) ENGINE=InnoDB AUTO_INCREMENT=2501 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶主表'
            """;

        when(connectionService.showCreateTable(anyString(), anyString(), anyString()))
            .thenReturn(createTableSql);

        Map<String, Object> arguments = Map.of(
            "action", "showCreateTable",
            "connectionId", "test-mysql-conn",
            "tableName", "users",
            "schemaName", "testdb"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 建表語句獲取完成", result.getContent());
    }

    @Test
    void shouldGetTableConstraintsSuccessfully() {
        // Arrange
        List<ConstraintInfo> constraints = List.of(
            ConstraintInfo.builder()
                .constraintName("PRIMARY")
                .constraintType("PRIMARY KEY")
                .tableName("users")
                .columnName("id")
                .build(),
            ConstraintInfo.builder()
                .constraintName("email")
                .constraintType("UNIQUE")
                .tableName("users")
                .columnName("email")
                .build(),
            ConstraintInfo.builder()
                .constraintName("users_chk_1")
                .constraintType("CHECK")
                .tableName("users")
                .columnName("age")
                .checkClause("(age >= 0 AND age <= 150)")
                .build()
        );

        when(connectionService.getTableConstraints(anyString(), anyString(), anyString()))
            .thenReturn(constraints);

        Map<String, Object> arguments = Map.of(
            "action", "getConstraints",
            "connectionId", "test-mysql-conn",
            "tableName", "users",
            "schemaName", "testdb"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MySQL 約束資訊獲取完成", result.getContent());
    }

    @Test
    void shouldFailWhenConnectionIdIsEmpty() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "action", "listTables",
            "connectionId", ""
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("Connection ID 不能為空", result.getError());
    }

    @Test
    void shouldFailWhenActionIsUnsupported() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "action", "unsupportedAction",
            "connectionId", "test-mysql-conn"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("不支援的操作"));
    }

    @Test
    void shouldFailWhenTableNameIsEmpty() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "action", "getTableSchema",
            "connectionId", "test-mysql-conn",
            "tableName", ""
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("表格名稱不能為空", result.getError());
    }

    @Test
    void shouldFailWhenConnectionNotFound() {
        // Arrange
        when(connectionService.getTableSchema(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Connection 'non-existent' not found"));

        Map<String, Object> arguments = Map.of(
            "action", "getTableSchema",
            "connectionId", "non-existent",
            "tableName", "users",
            "schemaName", "testdb"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not found"));
    }

    @Test
    void shouldFailWhenTableNotFound() {
        // Arrange
        when(connectionService.getTableSchema(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Table 'testdb.non_existent' doesn't exist"));

        Map<String, Object> arguments = Map.of(
            "action", "getTableSchema",
            "connectionId", "test-mysql-conn",
            "tableName", "non_existent",
            "schemaName", "testdb"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("doesn't exist"));
    }

    @Test
    void shouldHandleEmptySchemaName() {
        // Arrange
        List<TableInfo> tables = List.of(
            TableInfo.builder()
                .tableName("users")
                .tableType("BASE TABLE")
                .schemaName("testdb")
                .rowCount(2500L)
                .build()
        );

        when(connectionService.listTables(anyString(), anyString()))
            .thenReturn(tables);

        Map<String, Object> arguments = Map.of(
            "action", "listTables",
            "connectionId", "test-mysql-conn"
            // schemaName 未提供，應該使用當前資料庫
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void shouldHandleAccessDeniedError() {
        // Arrange
        when(connectionService.listTables(anyString(), anyString()))
            .thenThrow(new RuntimeException("Access denied for user 'testuser'@'localhost' to database 'restricted_db'"));

        Map<String, Object> arguments = Map.of(
            "action", "listTables",
            "connectionId", "test-mysql-conn",
            "schemaName", "restricted_db"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Access denied"));
    }
}