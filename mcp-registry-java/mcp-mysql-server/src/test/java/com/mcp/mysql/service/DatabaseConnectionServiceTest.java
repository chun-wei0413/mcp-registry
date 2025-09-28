package com.mcp.mysql.service;

import com.mcpregistry.core.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL 資料庫連線服務整合測試
 * 使用 TestContainers 進行真實資料庫測試
 */
@ExtendWith(MockitoExtension.class)
@Testcontainers
class DatabaseConnectionServiceTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("test-data.sql");

    private DatabaseConnectionService connectionService;
    private String connectionId;

    @BeforeEach
    void setUp() throws Exception {
        connectionService = new DatabaseConnectionService();
        connectionId = "test-mysql";

        // 設置測試連線
        ConnectionConfig config = ConnectionConfig.builder()
                .connectionId(connectionId)
                .host(mysql.getHost())
                .port(mysql.getFirstMappedPort())
                .database(mysql.getDatabaseName())
                .username(mysql.getUsername())
                .password(mysql.getPassword())
                .maxPoolSize(5)
                .build();

        boolean added = connectionService.addConnection(config);
        assertTrue(added, "應該成功建立 MySQL 連線");

        // 準備測試資料
        setupTestData();
    }

    private void setupTestData() throws Exception {
        String jdbcUrl = mysql.getJdbcUrl();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, mysql.getUsername(), mysql.getPassword());
             Statement stmt = conn.createStatement()) {

            // 建立測試表格
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255) UNIQUE,
                    age INT,
                    active BOOLEAN DEFAULT true,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶主表'
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    amount DECIMAL(10,2) NOT NULL,
                    status ENUM('pending', 'completed', 'cancelled') DEFAULT 'pending',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='訂單表'
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    category VARCHAR(100),
                    stock INT DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 插入測試資料
            stmt.execute("""
                INSERT INTO users (name, email, age) VALUES
                ('Alice', 'alice@example.com', 25),
                ('Bob', 'bob@example.com', 30),
                ('Charlie', 'charlie@example.com', 35),
                ('Diana', 'diana@example.com', 28)
                """);

            stmt.execute("""
                INSERT INTO orders (user_id, amount, status) VALUES
                (1, 99.99, 'completed'),
                (1, 149.99, 'pending'),
                (2, 79.99, 'completed'),
                (3, 199.99, 'pending'),
                (4, 299.99, 'completed')
                """);

            stmt.execute("""
                INSERT INTO products (name, price, category, stock) VALUES
                ('Laptop', 999.99, 'Electronics', 10),
                ('Mouse', 29.99, 'Electronics', 50),
                ('Book', 19.99, 'Books', 100),
                ('Chair', 199.99, 'Furniture', 5)
                """);

            // 建立額外索引
            stmt.execute("CREATE INDEX idx_users_age ON users(age)");
            stmt.execute("CREATE INDEX idx_users_name ON users(name)");
            stmt.execute("CREATE INDEX idx_orders_status ON orders(status)");
            stmt.execute("CREATE INDEX idx_products_category ON products(category)");

            // 建立視圖
            stmt.execute("""
                CREATE VIEW user_order_stats AS
                SELECT
                    u.id,
                    u.name,
                    u.email,
                    COUNT(o.id) as order_count,
                    COALESCE(SUM(o.amount), 0) as total_amount
                FROM users u
                LEFT JOIN orders o ON u.id = o.user_id
                GROUP BY u.id, u.name, u.email
                """);
        }
    }

    @Test
    void shouldTestConnectionSuccessfully() {
        // Act
        boolean isConnected = connectionService.testConnection(connectionId);

        // Assert
        assertTrue(isConnected, "MySQL 連線測試應該成功");
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
        assertTrue(result.getRowCount() >= 3);
        assertEquals(List.of("id", "name", "email", "age", "active", "created_at", "updated_at"), result.getColumnNames());
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
        assertTrue(((Number) row.get("total")).intValue() >= 4);
    }

    @Test
    void shouldExecuteInsertQueryWithGeneratedKeys() {
        // Arrange
        String query = "INSERT INTO users (name, email, age) VALUES (?, ?, ?)";
        List<Object> params = List.of("Frank", "frank@example.com", 42);

        // Act
        QueryResult result = connectionService.executeQuery(connectionId, query, params);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getAffectedRows());
        assertTrue(result.getExecutionTimeMs() > 0);
        assertNotNull(result.getGeneratedKeys());
        assertFalse(result.getGeneratedKeys().isEmpty());
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
    void shouldExecuteDeleteQuerySuccessfully() {
        // Arrange
        // 先插入一個測試用戶
        connectionService.executeQuery(connectionId,
            "INSERT INTO users (name, email, age) VALUES (?, ?, ?)",
            List.of("TempUser", "temp@example.com", 20));

        String deleteQuery = "DELETE FROM users WHERE name = ?";
        List<Object> params = List.of("TempUser");

        // Act
        QueryResult result = connectionService.executeQuery(connectionId, deleteQuery, params);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getAffectedRows());
        assertTrue(result.getExecutionTimeMs() > 0);
    }

    @Test
    void shouldExecuteJoinQuerySuccessfully() {
        // Arrange
        String query = """
            SELECT u.name, u.email, o.id as order_id, o.amount, o.status
            FROM users u
            INNER JOIN orders o ON u.id = o.user_id
            WHERE o.status = ?
            ORDER BY u.name
            """;
        List<Object> params = List.of("completed");

        // Act
        QueryResult result = connectionService.executeQuery(connectionId, query, params);

        // Assert
        assertNotNull(result);
        assertTrue(result.getRowCount() >= 3);
        assertEquals(List.of("name", "email", "order_id", "amount", "status"), result.getColumnNames());

        // 驗證所有返回的訂單狀態都是 'completed'
        for (Map<String, Object> row : result.getRows()) {
            assertEquals("completed", row.get("status"));
        }
    }

    @Test
    void shouldExecuteTransactionSuccessfully() {
        // Arrange
        List<QueryRequest> queries = List.of(
            QueryRequest.builder()
                .query("INSERT INTO users (name, email, age) VALUES (?, ?, ?)")
                .params(List.of("TxUser1", "txuser1@example.com", 25))
                .build(),
            QueryRequest.builder()
                .query("INSERT INTO products (name, price, category, stock) VALUES (?, ?, ?, ?)")
                .params(List.of("TxProduct1", 99.99, "Test", 10))
                .build()
        );

        // Act
        QueryResult result = connectionService.executeTransaction(connectionId, queries);

        // Assert
        assertNotNull(result);
        assertTrue(result.isTransactionSuccessful());
        assertTrue(result.getExecutionTimeMs() > 0);

        // 驗證資料確實插入
        QueryResult verifyUser = connectionService.executeQuery(connectionId,
            "SELECT COUNT(*) as count FROM users WHERE name = ?",
            List.of("TxUser1"));
        assertEquals(1, ((Number) verifyUser.getRows().get(0).get("count")).intValue());
    }

    @Test
    void shouldRollbackTransactionOnError() {
        // Arrange
        List<QueryRequest> queries = List.of(
            QueryRequest.builder()
                .query("INSERT INTO users (name, email, age) VALUES (?, ?, ?)")
                .params(List.of("RollbackUser", "rollback@example.com", 30))
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
            List.of("RollbackUser")
        );
        assertEquals(0, ((Number) checkResult.getRows().get(0).get("count")).intValue());
    }

    @Test
    void shouldExecuteBatchQuerySuccessfully() {
        // Arrange
        String query = "INSERT INTO products (name, price, category, stock) VALUES (?, ?, ?, ?)";
        List<List<Object>> paramsList = List.of(
            List.of("Batch Product 1", 19.99, "Batch", 5),
            List.of("Batch Product 2", 29.99, "Batch", 8),
            List.of("Batch Product 3", 39.99, "Batch", 12)
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
        TableSchema schema = connectionService.getTableSchema(connectionId, "users", "testdb");

        // Assert
        assertNotNull(schema);
        assertEquals("users", schema.getTableName());
        assertEquals("testdb", schema.getSchemaName());
        assertEquals("BASE TABLE", schema.getTableType());
        assertEquals("InnoDB", schema.getEngine());
        assertEquals("用戶主表", schema.getComment());

        List<ColumnInfo> columns = schema.getColumns();
        assertNotNull(columns);
        assertTrue(columns.size() >= 7);

        // 檢查主鍵欄位
        ColumnInfo idColumn = columns.stream()
            .filter(col -> "id".equals(col.getColumnName()))
            .findFirst()
            .orElse(null);
        assertNotNull(idColumn);
        assertTrue(idColumn.isPrimaryKey());
        assertTrue(idColumn.isAutoIncrement());
        assertFalse(idColumn.isNullable());
    }

    @Test
    void shouldListTablesSuccessfully() {
        // Act
        List<TableInfo> tables = connectionService.listTables(connectionId, "testdb");

        // Assert
        assertNotNull(tables);
        assertTrue(tables.size() >= 4);

        List<String> tableNames = tables.stream()
            .map(TableInfo::getTableName)
            .toList();
        assertTrue(tableNames.contains("users"));
        assertTrue(tableNames.contains("orders"));
        assertTrue(tableNames.contains("products"));
        assertTrue(tableNames.contains("user_order_stats"));
    }

    @Test
    void shouldListDatabasesSuccessfully() {
        // Act
        List<String> databases = connectionService.listSchemas(connectionId);

        // Assert
        assertNotNull(databases);
        assertTrue(databases.contains("testdb"));
        assertTrue(databases.contains("information_schema"));
        assertTrue(databases.contains("performance_schema"));
        assertTrue(databases.contains("mysql"));
        assertTrue(databases.contains("sys"));
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
        assertTrue(explainResult.getColumnNames().contains("table"));
        assertTrue(explainResult.getColumnNames().contains("type"));
        assertTrue(explainResult.getColumnNames().contains("rows"));

        Map<String, Object> firstRow = explainResult.getRows().get(0);
        assertEquals("users", firstRow.get("table"));
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
            .map(row -> row.values().toString())
            .reduce("", (a, b) -> a + " " + b);
        assertTrue(planText.contains("actual time") || planText.contains("cost"));
    }

    @Test
    void shouldGetTableIndexesSuccessfully() {
        // Act
        List<IndexInfo> indexes = connectionService.getTableIndexes(connectionId, "users", "testdb");

        // Assert
        assertNotNull(indexes);
        assertTrue(indexes.size() >= 4);

        List<String> indexNames = indexes.stream()
            .map(IndexInfo::getIndexName)
            .toList();
        assertTrue(indexNames.contains("PRIMARY"));
        assertTrue(indexNames.contains("email"));
        assertTrue(indexNames.contains("idx_users_age"));
        assertTrue(indexNames.contains("idx_users_name"));
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

        assertEquals("testdb", sizeInfo.get("database_name"));
        Long sizeBytes = (Long) sizeInfo.get("size_bytes");
        assertTrue(sizeBytes > 0);
    }

    @Test
    void shouldGetTableStatsSuccessfully() {
        // Act
        Map<String, Object> tableStats = connectionService.getTableStats(connectionId, "users", "testdb");

        // Assert
        assertNotNull(tableStats);
        assertTrue(tableStats.containsKey("table_name"));
        assertTrue(tableStats.containsKey("engine"));
        assertTrue(tableStats.containsKey("row_count"));
        assertTrue(tableStats.containsKey("data_length"));
        assertTrue(tableStats.containsKey("index_length"));

        assertEquals("users", tableStats.get("table_name"));
        assertEquals("InnoDB", tableStats.get("engine"));
        Long rowCount = (Long) tableStats.get("row_count");
        assertTrue(rowCount >= 4);
    }

    @Test
    void shouldGetTableForeignKeysSuccessfully() {
        // Act
        List<ForeignKeyInfo> foreignKeys = connectionService.getTableForeignKeys(connectionId, "orders", "testdb");

        // Assert
        assertNotNull(foreignKeys);
        assertFalse(foreignKeys.isEmpty());

        ForeignKeyInfo userIdFk = foreignKeys.stream()
            .filter(fk -> "user_id".equals(fk.getColumnName()))
            .findFirst()
            .orElse(null);
        assertNotNull(userIdFk);
        assertEquals("users", userIdFk.getReferencedTableName());
        assertEquals("id", userIdFk.getReferencedColumnName());
        assertEquals("CASCADE", userIdFk.getOnDelete());
    }

    @Test
    void shouldShowCreateTableSuccessfully() {
        // Act
        String createTableSql = connectionService.showCreateTable(connectionId, "users", "testdb");

        // Assert
        assertNotNull(createTableSql);
        assertTrue(createTableSql.contains("CREATE TABLE"));
        assertTrue(createTableSql.contains("users"));
        assertTrue(createTableSql.contains("AUTO_INCREMENT"));
        assertTrue(createTableSql.contains("PRIMARY KEY"));
        assertTrue(createTableSql.contains("InnoDB"));
        assertTrue(createTableSql.contains("用戶主表"));
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
        // MySQL 的分頁查詢通常會有更多行
        assertNotNull(result.getRows());
    }

    @Test
    void shouldHandleEnumTypes() {
        // Arrange
        String query = "SELECT DISTINCT status FROM orders";

        // Act
        QueryResult result = connectionService.executeQuery(connectionId, query, List.of());

        // Assert
        assertNotNull(result);
        assertTrue(result.getRowCount() >= 2);

        List<String> statuses = result.getRows().stream()
            .map(row -> (String) row.get("status"))
            .toList();
        assertTrue(statuses.contains("pending") || statuses.contains("completed"));
    }

    @Test
    void shouldHandleDecimalTypes() {
        // Arrange
        String query = "SELECT price FROM products WHERE name = ?";
        List<Object> params = List.of("Laptop");

        // Act
        QueryResult result = connectionService.executeQuery(connectionId, query, params);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getRowCount());

        Object price = result.getRows().get(0).get("price");
        assertNotNull(price);
        assertTrue(price instanceof Number);
    }

    @Test
    void shouldFailWithInvalidConnection() {
        // Arrange
        String invalidConnectionId = "invalid-mysql-connection";
        String query = "SELECT 1";

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            connectionService.executeQuery(invalidConnectionId, query, List.of());
        });
    }

    @Test
    void shouldFailWithInvalidSql() {
        // Arrange
        String invalidQuery = "SELCT * FORM users";  // 故意的語法錯誤

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            connectionService.executeQuery(connectionId, invalidQuery, List.of());
        });
    }

    @Test
    void shouldFailWithForeignKeyConstraint() {
        // Arrange
        String query = "INSERT INTO orders (user_id, amount) VALUES (?, ?)";
        List<Object> params = List.of(999, 100.0);  // 不存在的 user_id

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            connectionService.executeQuery(connectionId, query, params);
        });
    }

    @Test
    void shouldFailWithUniqueConstraintViolation() {
        // Arrange
        String query = "INSERT INTO users (name, email, age) VALUES (?, ?, ?)";
        List<Object> params = List.of("Duplicate Alice", "alice@example.com", 30);  // 重複的 email

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            connectionService.executeQuery(connectionId, query, params);
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