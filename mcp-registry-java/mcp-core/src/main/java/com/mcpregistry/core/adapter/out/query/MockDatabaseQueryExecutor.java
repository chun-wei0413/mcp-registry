package com.mcpregistry.core.adapter.out.query;

import com.mcpregistry.core.entity.ConnectionId;
import com.mcpregistry.core.usecase.port.out.DatabaseQueryExecutor;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Mock 資料庫查詢執行器
 *
 * Output Adapter 實現，提供模擬的資料庫操作
 * 適用於開發和測試環境
 */
public class MockDatabaseQueryExecutor implements DatabaseQueryExecutor {

    @Override
    public Mono<QueryResult> executeQuery(ConnectionId connectionId, String query, List<Object> parameters) {
        return Mono.delay(Duration.ofMillis(100)) // 模擬查詢延遲
                .map(delay -> createMockQueryResult(query));
    }

    @Override
    public Mono<Integer> executeUpdate(ConnectionId connectionId, String query, List<Object> parameters) {
        return Mono.delay(Duration.ofMillis(50))
                .map(delay -> determineMockAffectedRows(query));
    }

    @Override
    public Mono<TransactionResult> executeTransaction(ConnectionId connectionId, List<TransactionQuery> queries) {
        return Mono.delay(Duration.ofMillis(200))
                .map(delay -> new TransactionResult(
                    true,
                    queries.stream().map(q -> (Object) ("執行成功: " + q.query)).toList(),
                    null,
                    200L
                ));
    }

    @Override
    public Mono<ConnectionTestResult> testConnection(ConnectionId connectionId) {
        return Mono.delay(Duration.ofMillis(10))
                .map(delay -> new ConnectionTestResult(
                    true,
                    "連線測試成功 - 模擬環境",
                    10L
                ));
    }

    @Override
    public Mono<TableSchema> getTableSchema(ConnectionId connectionId, String tableName, String schemaName) {
        return Mono.delay(Duration.ofMillis(30))
                .map(delay -> createMockTableSchema(tableName, schemaName));
    }

    private QueryResult createMockQueryResult(String query) {
        // 根據查詢類型創建不同的模擬結果
        String upperQuery = query.toUpperCase().trim();

        if (upperQuery.startsWith("SELECT")) {
            return createMockSelectResult();
        } else if (upperQuery.contains("VERSION")) {
            return createMockVersionResult();
        } else {
            return createMockGenericResult();
        }
    }

    private QueryResult createMockSelectResult() {
        List<Map<String, Object>> rows = List.of(
            Map.of("id", 1, "name", "測試資料 1", "status", "active"),
            Map.of("id", 2, "name", "測試資料 2", "status", "inactive"),
            Map.of("id", 3, "name", "測試資料 3", "status", "active")
        );

        List<String> columns = List.of("id", "name", "status");

        return new QueryResult(rows, columns, rows.size(), 100L);
    }

    private QueryResult createMockVersionResult() {
        List<Map<String, Object>> rows = List.of(
            Map.of("version", "PostgreSQL 14.5 (Mock Version)")
        );

        List<String> columns = List.of("version");

        return new QueryResult(rows, columns, 1, 50L);
    }

    private QueryResult createMockGenericResult() {
        List<Map<String, Object>> rows = List.of(
            Map.of("result", "查詢執行成功", "timestamp", java.time.LocalDateTime.now().toString())
        );

        List<String> columns = List.of("result", "timestamp");

        return new QueryResult(rows, columns, 1, 75L);
    }

    private Integer determineMockAffectedRows(String query) {
        String upperQuery = query.toUpperCase().trim();

        if (upperQuery.startsWith("INSERT")) {
            return 1; // 插入通常影響 1 行
        } else if (upperQuery.startsWith("UPDATE")) {
            return 3; // 更新可能影響多行
        } else if (upperQuery.startsWith("DELETE")) {
            return 2; // 刪除可能影響多行
        } else {
            return 0; // 其他操作
        }
    }

    private TableSchema createMockTableSchema(String tableName, String schemaName) {
        List<ColumnInfo> columns = List.of(
            new ColumnInfo("id", "SERIAL", false, "nextval('seq')", true),
            new ColumnInfo("name", "VARCHAR(255)", false, null, false),
            new ColumnInfo("status", "VARCHAR(50)", true, "'active'", false),
            new ColumnInfo("created_at", "TIMESTAMP", false, "now()", false)
        );

        return new TableSchema(tableName, schemaName != null ? schemaName : "public", columns);
    }
}