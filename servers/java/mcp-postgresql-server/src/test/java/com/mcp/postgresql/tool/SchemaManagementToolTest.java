package com.mcp.postgresql.tool;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.postgresql.service.DatabaseConnectionService;
import com.mcpregistry.core.entity.TableSchema;
import com.mcpregistry.core.entity.ColumnInfo;
import com.mcpregistry.core.entity.IndexInfo;
import com.mcpregistry.core.entity.TableInfo;
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
 * PostgreSQL Schema 管理工具測試
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
        assertEquals("postgresql_schema_management", schemaTool.getToolName());
    }

    @Test
    void shouldReturnCorrectDescription() {
        String description = schemaTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("PostgreSQL"));
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
                .dataType("integer")
                .isNullable(false)
                .columnDefault("nextval('users_id_seq'::regclass)")
                .isPrimaryKey(true)
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
                .build()
        );

        List<IndexInfo> indexes = List.of(
            IndexInfo.builder()
                .indexName("users_pkey")
                .columnNames(List.of("id"))
                .isUnique(true)
                .indexType("btree")
                .build(),
            IndexInfo.builder()
                .indexName("idx_users_email")
                .columnNames(List.of("email"))
                .isUnique(false)
                .indexType("btree")
                .build()
        );

        TableSchema mockSchema = TableSchema.builder()
            .tableName("users")
            .schemaName("public")
            .tableType("BASE TABLE")
            .columns(columns)
            .indexes(indexes)
            .rowCount(1500L)
            .build();

        when(connectionService.getTableSchema(anyString(), anyString(), anyString()))
            .thenReturn(mockSchema);

        Map<String, Object> arguments = Map.of(
            "action", "getTableSchema",
            "connectionId", "test-conn",
            "tableName", "users",
            "schemaName", "public"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("表格結構獲取完成", result.getContent());
    }

    @Test
    void shouldListTablesSuccessfully() {
        // Arrange
        List<TableInfo> tables = List.of(
            TableInfo.builder()
                .tableName("users")
                .tableType("BASE TABLE")
                .schemaName("public")
                .rowCount(1500L)
                .comment("用戶表")
                .build(),
            TableInfo.builder()
                .tableName("orders")
                .tableType("BASE TABLE")
                .schemaName("public")
                .rowCount(5000L)
                .comment("訂單表")
                .build(),
            TableInfo.builder()
                .tableName("user_view")
                .tableType("VIEW")
                .schemaName("public")
                .comment("用戶視圖")
                .build()
        );

        when(connectionService.listTables(anyString(), anyString()))
            .thenReturn(tables);

        Map<String, Object> arguments = Map.of(
            "action", "listTables",
            "connectionId", "test-conn",
            "schemaName", "public"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("表格列表獲取完成", result.getContent());
    }

    @Test
    void shouldListSchemasSuccessfully() {
        // Arrange
        List<String> schemas = List.of("public", "information_schema", "pg_catalog", "app_schema");

        when(connectionService.listSchemas(anyString()))
            .thenReturn(schemas);

        Map<String, Object> arguments = Map.of(
            "action", "listSchemas",
            "connectionId", "test-conn"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("Schema 列表獲取完成", result.getContent());
    }

    @Test
    void shouldExplainQuerySuccessfully() {
        // Arrange
        QueryResult explainResult = QueryResult.builder()
            .queryId("explain-1")
            .columnNames(List.of("QUERY PLAN"))
            .rows(List.of(
                Map.of("QUERY PLAN", "Seq Scan on users  (cost=0.00..22.50 rows=1250 width=68)"),
                Map.of("QUERY PLAN", "  Filter: (id > 100)")
            ))
            .executionTimeMs(15)
            .build();

        when(connectionService.explainQuery(anyString(), anyString(), anyBoolean()))
            .thenReturn(explainResult);

        Map<String, Object> arguments = Map.of(
            "action", "explainQuery",
            "connectionId", "test-conn",
            "query", "SELECT * FROM users WHERE id > 100",
            "analyze", false
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("查詢計畫分析完成", result.getContent());
    }

    @Test
    void shouldExplainAnalyzeQuerySuccessfully() {
        // Arrange
        QueryResult explainResult = QueryResult.builder()
            .queryId("explain-analyze-1")
            .columnNames(List.of("QUERY PLAN"))
            .rows(List.of(
                Map.of("QUERY PLAN", "Seq Scan on users  (cost=0.00..22.50 rows=1250 width=68) (actual time=0.015..0.125 rows=750 loops=1)"),
                Map.of("QUERY PLAN", "  Filter: (id > 100)"),
                Map.of("QUERY PLAN", "  Rows Removed by Filter: 500"),
                Map.of("QUERY PLAN", "Planning Time: 0.089 ms"),
                Map.of("QUERY PLAN", "Execution Time: 0.156 ms")
            ))
            .executionTimeMs(25)
            .build();

        when(connectionService.explainQuery(anyString(), anyString(), anyBoolean()))
            .thenReturn(explainResult);

        Map<String, Object> arguments = Map.of(
            "action", "explainQuery",
            "connectionId", "test-conn",
            "query", "SELECT * FROM users WHERE id > 100",
            "analyze", true
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("查詢計畫分析完成", result.getContent());
    }

    @Test
    void shouldGetTableIndexesSuccessfully() {
        // Arrange
        List<IndexInfo> indexes = List.of(
            IndexInfo.builder()
                .indexName("users_pkey")
                .tableName("users")
                .columnNames(List.of("id"))
                .isUnique(true)
                .indexType("btree")
                .build(),
            IndexInfo.builder()
                .indexName("idx_users_email")
                .tableName("users")
                .columnNames(List.of("email"))
                .isUnique(false)
                .indexType("btree")
                .build(),
            IndexInfo.builder()
                .indexName("idx_users_name_email")
                .tableName("users")
                .columnNames(List.of("name", "email"))
                .isUnique(false)
                .indexType("btree")
                .build()
        );

        when(connectionService.getTableIndexes(anyString(), anyString(), anyString()))
            .thenReturn(indexes);

        Map<String, Object> arguments = Map.of(
            "action", "getTableIndexes",
            "connectionId", "test-conn",
            "tableName", "users",
            "schemaName", "public"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("索引資訊獲取完成", result.getContent());
    }

    @Test
    void shouldGetDatabaseSizeSuccessfully() {
        // Arrange
        Map<String, Object> sizeInfo = Map.of(
            "database_name", "test_db",
            "size_bytes", 52428800L,
            "size_human", "50 MB",
            "table_count", 15,
            "schema_count", 3
        );

        when(connectionService.getDatabaseSize(anyString()))
            .thenReturn(sizeInfo);

        Map<String, Object> arguments = Map.of(
            "action", "getDatabaseSize",
            "connectionId", "test-conn"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("資料庫大小資訊獲取完成", result.getContent());
    }

    @Test
    void shouldGetTableStatsSuccessfully() {
        // Arrange
        Map<String, Object> tableStats = Map.of(
            "table_name", "users",
            "row_count", 1500L,
            "table_size_bytes", 1048576L,
            "table_size_human", "1 MB",
            "index_size_bytes", 524288L,
            "index_size_human", "512 KB",
            "total_size_bytes", 1572864L,
            "total_size_human", "1.5 MB"
        );

        when(connectionService.getTableStats(anyString(), anyString(), anyString()))
            .thenReturn(tableStats);

        Map<String, Object> arguments = Map.of(
            "action", "getTableStats",
            "connectionId", "test-conn",
            "tableName", "users",
            "schemaName", "public"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("表格統計資訊獲取完成", result.getContent());
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
            "connectionId", "test-conn"
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
            "connectionId", "test-conn",
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
            .thenThrow(new RuntimeException("Connection not found"));

        Map<String, Object> arguments = Map.of(
            "action", "getTableSchema",
            "connectionId", "non-existent",
            "tableName", "users",
            "schemaName", "public"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Connection not found"));
    }

    @Test
    void shouldFailWhenTableNotFound() {
        // Arrange
        when(connectionService.getTableSchema(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Table 'non_existent' doesn't exist"));

        Map<String, Object> arguments = Map.of(
            "action", "getTableSchema",
            "connectionId", "test-conn",
            "tableName", "non_existent",
            "schemaName", "public"
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
                .schemaName("public")
                .rowCount(1500L)
                .build()
        );

        when(connectionService.listTables(anyString(), anyString()))
            .thenReturn(tables);

        Map<String, Object> arguments = Map.of(
            "action", "listTables",
            "connectionId", "test-conn"
            // schemaName 未提供，應該使用預設值 "public"
        );

        // Act
        McpToolResult result = schemaTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }
}