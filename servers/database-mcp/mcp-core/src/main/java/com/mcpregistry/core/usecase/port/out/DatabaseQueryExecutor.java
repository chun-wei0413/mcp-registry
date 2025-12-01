package com.mcpregistry.core.usecase.port.out;

import com.mcpregistry.core.entity.ConnectionId;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 資料庫查詢執行器輸出端口
 *
 * 定義實際執行資料庫操作的抽象接口
 * 使用反應式程式設計模式 (Reactor)
 */
public interface DatabaseQueryExecutor {

    /**
     * 執行查詢並返回結果
     *
     * @param connectionId 連線識別碼
     * @param query SQL 查詢語句
     * @param parameters 查詢參數
     * @return 查詢結果的 Mono
     */
    Mono<QueryResult> executeQuery(ConnectionId connectionId, String query, List<Object> parameters);

    /**
     * 執行更新操作（INSERT、UPDATE、DELETE）
     *
     * @param connectionId 連線識別碼
     * @param query SQL 語句
     * @param parameters 參數
     * @return 受影響的行數
     */
    Mono<Integer> executeUpdate(ConnectionId connectionId, String query, List<Object> parameters);

    /**
     * 執行事務
     *
     * @param connectionId 連線識別碼
     * @param queries 事務中的查詢列表
     * @return 事務執行結果
     */
    Mono<TransactionResult> executeTransaction(ConnectionId connectionId, List<TransactionQuery> queries);

    /**
     * 測試連線
     *
     * @param connectionId 連線識別碼
     * @return 連線測試結果
     */
    Mono<ConnectionTestResult> testConnection(ConnectionId connectionId);

    /**
     * 獲取表結構資訊
     *
     * @param connectionId 連線識別碼
     * @param tableName 表名
     * @param schemaName 結構名（可選）
     * @return 表結構資訊
     */
    Mono<TableSchema> getTableSchema(ConnectionId connectionId, String tableName, String schemaName);

    /**
     * 查詢結果的值對象
     */
    class QueryResult {
        public final List<Map<String, Object>> rows;
        public final List<String> columns;
        public final int rowCount;
        public final long executionTimeMs;

        public QueryResult(List<Map<String, Object>> rows, List<String> columns,
                         int rowCount, long executionTimeMs) {
            this.rows = rows;
            this.columns = columns;
            this.rowCount = rowCount;
            this.executionTimeMs = executionTimeMs;
        }
    }

    /**
     * 事務查詢的值對象
     */
    class TransactionQuery {
        public final String query;
        public final List<Object> parameters;

        public TransactionQuery(String query, List<Object> parameters) {
            this.query = query;
            this.parameters = parameters;
        }
    }

    /**
     * 事務執行結果的值對象
     */
    class TransactionResult {
        public final boolean success;
        public final List<Object> results;
        public final String errorMessage;
        public final long executionTimeMs;

        public TransactionResult(boolean success, List<Object> results,
                               String errorMessage, long executionTimeMs) {
            this.success = success;
            this.results = results;
            this.errorMessage = errorMessage;
            this.executionTimeMs = executionTimeMs;
        }
    }

    /**
     * 連線測試結果的值對象
     */
    class ConnectionTestResult {
        public final boolean success;
        public final String message;
        public final long responseTimeMs;

        public ConnectionTestResult(boolean success, String message, long responseTimeMs) {
            this.success = success;
            this.message = message;
            this.responseTimeMs = responseTimeMs;
        }
    }

    /**
     * 表結構資訊的值對象
     */
    class TableSchema {
        public final String tableName;
        public final String schemaName;
        public final List<ColumnInfo> columns;

        public TableSchema(String tableName, String schemaName, List<ColumnInfo> columns) {
            this.tableName = tableName;
            this.schemaName = schemaName;
            this.columns = columns;
        }
    }

    /**
     * 欄位資訊的值對象
     */
    class ColumnInfo {
        public final String name;
        public final String dataType;
        public final boolean nullable;
        public final String defaultValue;
        public final boolean primaryKey;

        public ColumnInfo(String name, String dataType, boolean nullable,
                        String defaultValue, boolean primaryKey) {
            this.name = name;
            this.dataType = dataType;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
            this.primaryKey = primaryKey;
        }
    }
}