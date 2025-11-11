package com.mcp.postgresql.service;

import com.mcpregistry.core.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL 資料庫連線服務整合測試
 * 使用 TestContainers 進行真實資料庫測試
 */
@ExtendWith(MockitoExtension.class)
@Testcontainers
class DatabaseConnectionServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("test-data.sql");

    private DatabaseConnectionService connectionService;
    private String connectionId;

    @BeforeEach
    void setUp() throws Exception {
        connectionService = new DatabaseConnectionService();
        connectionId = "test-postgres";

        // 設置測試連線
        ConnectionConfig config = ConnectionConfig.builder()
                .connectionId(connectionId)
                .host(postgres.getHost())
                .port(postgres.getFirstMappedPort())
                .database(postgres.getDatabaseName())
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .maxPoolSize(5)
                .build();

        boolean added = connectionService.addConnection(config);
        assertTrue(added, "應該成功建立連線");

        // 準備測試資料
        setupTestData();
    }

    private void setupTestData() throws Exception {
        String jdbcUrl = postgres.getJdbcUrl();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            // 建立測試表格
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255) UNIQUE,
                    age INTEGER,
                    active BOOLEAN DEFAULT true,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER REFERENCES users(id),
                    amount DECIMAL(10,2),
                    status VARCHAR(50) DEFAULT 'pending',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // 插入測試資料
            stmt.execute("""
                INSERT INTO users (name, email, age) VALUES
                ('Alice', 'alice@example.com', 25),
                ('Bob', 'bob@example.com', 30),
                ('Charlie', 'charlie@example.com', 35)
                """);

            stmt.execute("""
                INSERT INTO orders (user_id, amount, status) VALUES
                (1, 100.50, 'completed'),
                (1, 75.25, 'pending'),
                (2, 200.00, 'completed'),
                (3, 150.75, 'pending')
                """);

            // 建立索引
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id)");
        }
    }

    @Test
    void shouldTestConnectionSuccessfully() {
        // Act
        boolean isConnected = connectionService.testConnection(connectionId);

        // Assert
        assertTrue(isConnected, "連線測試應該成功");
    }

    @Test
    void shouldExecuteSelectQuerySuccessfully() {
        // Arrange
        String query = "SELECT * FROM users WHERE age > ?";
        List<Object> params = List.of(25);

        // Act
        QueryResult result = connectionService.executeQuery(connectionId, query, params);

        // Assert
        assertNotNull(result);
        assertTrue(result.getRowCount() >= 2);
        assertEquals(List.of("id", "name", "email", "age", "active", "created_at"), result.getColumnNames());
        assertNotNull(result.getRows());
        assertTrue(result.getExecutionTimeMs() > 0);
    }

    @Test
    void shouldExecuteCountQuerySuccessfully() {
        // Arrange
        String query = "SELECT COUNT(*) as total FROM users";

        // Act
        QueryResult result = connectionService.executeQuery(connectionId, query, List.of());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getRowCount());
        assertEquals(List.of("total"), result.getColumnNames());

        Map<String, Object> row = result.getRows().get(0);
        assertTrue(((Number) row.get("total")).intValue() >= 3);
    }

    @Test
    void shouldExecuteUpdateQuerySuccessfully() {
        // Arrange
        String query = "UPDATE users SET age = ? WHERE name = ?";
        List<Object> params = List.of(26, "Alice");

        // Act
        QueryResult result = connectionService.executeQuery(connectionId, query, params);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getAffectedRows());
        assertTrue(result.getExecutionTimeMs() > 0);
    }

    @Test
    void shouldExecuteInsertQuerySuccessfully() {
        // Arrange
        String query = "INSERT INTO users (name, email, age) VALUES (?, ?, ?)";
        List<Object> params = List.of("David", "david@example.com", 28);

        // Act
        QueryResult result = connectionService.executeQuery(connectionId, query, params);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getAffectedRows());
        assertTrue(result.getExecutionTimeMs() > 0);
    }

    @Test
    void shouldExecuteTransactionSuccessfully() {
        // Arrange
        List<QueryRequest> queries = List.of(
            QueryRequest.builder()
                .query("INSERT INTO users (name, email, age) VALUES (?, ?, ?)")
                .params(List.of("Eve", "eve@example.com", 32))
                .build(),
            QueryRequest.builder()
                .query("INSERT INTO orders (user_id, amount, status) VALUES (?, ?, ?)")
                .params(List.of(4, 99.99, "pending"))
                .build()
        );

        // Act
        QueryResult result = connectionService.executeTransaction(connectionId, queries);

        // Assert
        assertNotNull(result);
        assertTrue(result.isTransactionSuccessful());
        assertTrue(result.getExecutionTimeMs() > 0);
    }

    @Test
    void shouldRollbackTransactionOnError() {
        // Arrange
        List<QueryRequest> queries = List.of(
            QueryRequest.builder()
                .query("INSERT INTO users (name, email, age) VALUES (?, ?, ?)")
                .params(List.of("Frank", "frank@example.com", 40))
                .build(),
            QueryRequest.builder()
                .query("INVALID SQL STATEMENT")
                .params(List.of())
                .build()
        );

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            connectionService.executeTransaction(connectionId, queries);
        });

        // 驗證第一個插入被回滾
        QueryResult checkResult = connectionService.executeQuery(
            connectionId,
            "SELECT COUNT(*) as count FROM users WHERE name = ?",
            List.of("Frank")
        );
        assertEquals(0, ((Number) checkResult.getRows().get(0).get("count")).intValue());
    }

    @Test
    void shouldExecuteBatchQuerySuccessfully() {
        // Arrange
        String query = "INSERT INTO users (name, email, age) VALUES (?, ?, ?)";
        List<List<Object>> paramsList = List.of(
            List.of("User1", "user1@example.com", 20),
            List.of("User2", "user2@example.com", 21),
            List.of("User3", "user3@example.com", 22)
        );

        // Act
        QueryResult result = connectionService.executeBatch(connectionId, query, paramsList);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getAffectedRows());
        assertTrue(result.getExecutionTimeMs() > 0);
    }

    @Test
    void shouldGetTableSchemaSuccessfully() {
        // Act
        TableSchema schema = connectionService.getTableSchema(connectionId, "users", "public");

        // Assert
        assertNotNull(schema);
        assertEquals("users", schema.getTableName());
        assertEquals("public", schema.getSchemaName());
        assertEquals("BASE TABLE", schema.getTableType());

        List<ColumnInfo> columns = schema.getColumns();
        assertNotNull(columns);
        assertTrue(columns.size() >= 6);

        // 檢查主鍵欄位
        ColumnInfo idColumn = columns.stream()
            .filter(col -> "id".equals(col.getColumnName()))
            .findFirst()
            .orElse(null);
        assertNotNull(idColumn);
        assertTrue(idColumn.isPrimaryKey());
        assertFalse(idColumn.isNullable());
    }

    @Test
    void shouldListTablesSuccessfully() {
        // Act
        List<TableInfo> tables = connectionService.listTables(connectionId, "public");

        // Assert
        assertNotNull(tables);
        assertTrue(tables.size() >= 2);

        List<String> tableNames = tables.stream()
            .map(TableInfo::getTableName)
            .toList();
        assertTrue(tableNames.contains("users"));
        assertTrue(tableNames.contains("orders"));
    }

    @Test
    void shouldListSchemasSuccessfully() {
        // Act
        List<String> schemas = connectionService.listSchemas(connectionId);

        // Assert
        assertNotNull(schemas);
        assertTrue(schemas.contains("public"));
        assertTrue(schemas.contains("information_schema"));
        assertTrue(schemas.contains("pg_catalog"));
    }

    @Test
    void shouldExplainQuerySuccessfully() {
        // Arrange
        String query = "SELECT * FROM users WHERE age > 25";

        // Act
        QueryResult explainResult = connectionService.explainQuery(connectionId, query, false);

        // Assert
        assertNotNull(explainResult);
        assertTrue(explainResult.getRowCount() > 0);
        assertEquals(List.of("QUERY PLAN"), explainResult.getColumnNames());

        String planText = (String) explainResult.getRows().get(0).get("QUERY PLAN");
        assertTrue(planText.contains("Scan"));
    }

    @Test
    void shouldExplainAnalyzeQuerySuccessfully() {
        // Arrange
        String query = "SELECT * FROM users WHERE age > 25";

        // Act
        QueryResult explainResult = connectionService.explainQuery(connectionId, query, true);

        // Assert
        assertNotNull(explainResult);
        assertTrue(explainResult.getRowCount() > 0);

        String planText = explainResult.getRows().stream()
            .map(row -> (String) row.get("QUERY PLAN"))
            .reduce("", (a, b) -> a + " " + b);
        assertTrue(planText.contains("actual time"));
    }

    @Test
    void shouldGetTableIndexesSuccessfully() {
        // Act
        List<IndexInfo> indexes = connectionService.getTableIndexes(connectionId, "users", "public");

        // Assert
        assertNotNull(indexes);
        assertTrue(indexes.size() >= 2);

        List<String> indexNames = indexes.stream()
            .map(IndexInfo::getIndexName)
            .toList();
        assertTrue(indexNames.contains("users_pkey"));
        assertTrue(indexNames.contains("idx_users_email"));
    }

    @Test
    void shouldGetDatabaseSizeSuccessfully() {
        // Act
        Map<String, Object> sizeInfo = connectionService.getDatabaseSize(connectionId);

        // Assert
        assertNotNull(sizeInfo);
        assertTrue(sizeInfo.containsKey("database_name"));
        assertTrue(sizeInfo.containsKey("size_bytes"));
        assertTrue(sizeInfo.containsKey("size_human"));

        Long sizeBytes = (Long) sizeInfo.get("size_bytes");
        assertTrue(sizeBytes > 0);
    }

    @Test
    void shouldGetTableStatsSuccessfully() {
        // Act
        Map<String, Object> tableStats = connectionService.getTableStats(connectionId, "users", "public");

        // Assert
        assertNotNull(tableStats);
        assertTrue(tableStats.containsKey("table_name"));
        assertTrue(tableStats.containsKey("row_count"));
        assertTrue(tableStats.containsKey("table_size_bytes"));

        assertEquals("users", tableStats.get("table_name"));
        Long rowCount = (Long) tableStats.get("row_count");
        assertTrue(rowCount >= 3);
    }

    @Test
    void shouldExecuteQueryWithLimitSuccessfully() {
        // Arrange
        String query = "SELECT * FROM users ORDER BY id";
        int fetchSize = 2;

        // Act
        QueryResult result = connectionService.executeQueryWithLimit(connectionId, query, List.of(), fetchSize);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getRowCount());
        assertTrue(result.isHasMoreRows());
    }

    @Test
    void shouldFailWithInvalidConnection() {
        // Arrange
        String invalidConnectionId = "invalid-connection";
        String query = "SELECT 1";

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            connectionService.executeQuery(invalidConnectionId, query, List.of());
        });
    }

    @Test
    void shouldFailWithInvalidSql() {
        // Arrange
        String invalidQuery = "INVALID SQL STATEMENT";

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            connectionService.executeQuery(connectionId, invalidQuery, List.of());
        });
    }

    @Test
    void shouldFailWithInvalidParameters() {
        // Arrange
        String query = "SELECT * FROM users WHERE id = ?";
        List<Object> invalidParams = List.of("not_a_number");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            connectionService.executeQuery(connectionId, query, invalidParams);
        });
    }

    @Test
    void shouldRemoveConnectionSuccessfully() {
        // Act
        boolean removed = connectionService.removeConnection(connectionId);

        // Assert
        assertTrue(removed);
        assertFalse(connectionService.testConnection(connectionId));
    }
}