package com.mcp.postgresql.tool;

import com.mcp.common.mcp.McpTool;
import com.mcp.common.mcp.McpToolResult;
import com.mcp.common.model.QueryResult;
import com.mcp.postgresql.service.DatabaseQueryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * PostgreSQL 查詢執行工具
 *
 * 提供 MCP 查詢執行功能:
 * - execute_query: 執行 SELECT 查詢
 * - execute_update: 執行 INSERT/UPDATE/DELETE
 * - execute_transaction: 執行事務操作
 * - batch_execute: 批次執行查詢
 */
@Component
public class QueryExecutionTool implements McpTool {

    private final DatabaseQueryService queryService;

    public QueryExecutionTool(DatabaseQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public String getToolName() {
        return "postgresql_query_execution";
    }

    @Override
    public String getDescription() {
        return "PostgreSQL 查詢執行工具，支援 SELECT、UPDATE、事務和批次操作";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "action", Map.of(
                    "type", "string",
                    "enum", new String[]{"query", "update", "transaction", "batch"},
                    "description", "操作類型: query(查詢), update(更新), transaction(事務), batch(批次)"
                ),
                "connectionId", Map.of(
                    "type", "string",
                    "description", "連線識別碼"
                ),
                "sql", Map.of(
                    "type", "string",
                    "description", "SQL 語句"
                ),
                "parameters", Map.of(
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "description", "SQL 參數列表"
                ),
                "queries", Map.of(
                    "type", "array",
                    "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "sql", Map.of("type", "string"),
                            "parameters", Map.of("type", "array", "items", Map.of("type", "string"))
                        )
                    ),
                    "description", "批次查詢列表"
                ),
                "fetchSize", Map.of(
                    "type", "integer",
                    "default", 1000,
                    "description", "每次提取的記錄數"
                ),
                "timeout", Map.of(
                    "type", "integer",
                    "default", 30,
                    "description", "查詢超時時間（秒）"
                )
            ),
            "required", new String[]{"action", "connectionId"}
        );
    }

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        try {
            String action = (String) arguments.get("action");
            String connectionId = (String) arguments.get("connectionId");

            if (connectionId == null || connectionId.trim().isEmpty()) {
                return McpToolResult.error("Connection ID 不能為空");
            }

            return switch (action) {
                case "query" -> executeQuery(arguments);
                case "update" -> executeUpdate(arguments);
                case "transaction" -> executeTransaction(arguments);
                case "batch" -> executeBatch(arguments);
                default -> McpToolResult.error("不支援的操作: " + action);
            };

        } catch (Exception e) {
            return McpToolResult.error("查詢執行失敗: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private McpToolResult executeQuery(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connectionId");
            String sql = (String) arguments.get("sql");
            List<Object> parameters = (List<Object>) arguments.get("parameters");
            Integer fetchSize = (Integer) arguments.get("fetchSize");

            if (sql == null || sql.trim().isEmpty()) {
                return McpToolResult.error("SQL 語句不能為空");
            }

            QueryResult result = queryService.executeQuery(
                connectionId,
                sql,
                parameters,
                fetchSize != null ? fetchSize : 1000
            );

            return McpToolResult.success(
                "查詢執行成功",
                Map.of(
                    "result", result,
                    "rowCount", result.getRowCount(),
                    "executionTimeMs", result.getExecutionTimeMs()
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("執行查詢失敗: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private McpToolResult executeUpdate(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connectionId");
            String sql = (String) arguments.get("sql");
            List<Object> parameters = (List<Object>) arguments.get("parameters");

            if (sql == null || sql.trim().isEmpty()) {
                return McpToolResult.error("SQL 語句不能為空");
            }

            int affectedRows = queryService.executeUpdate(connectionId, sql, parameters);

            return McpToolResult.success(
                "更新執行成功",
                Map.of(
                    "affectedRows", affectedRows,
                    "sql", sql
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("執行更新失敗: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private McpToolResult executeTransaction(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connectionId");
            List<Map<String, Object>> queries = (List<Map<String, Object>>) arguments.get("queries");

            if (queries == null || queries.isEmpty()) {
                return McpToolResult.error("事務查詢列表不能為空");
            }

            List<Object> results = queryService.executeTransaction(connectionId, queries);

            return McpToolResult.success(
                "事務執行成功",
                Map.of(
                    "results", results,
                    "queryCount", queries.size()
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("執行事務失敗: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private McpToolResult executeBatch(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connectionId");
            String sql = (String) arguments.get("sql");
            List<List<Object>> parametersList = (List<List<Object>>) arguments.get("parameters");

            if (sql == null || sql.trim().isEmpty()) {
                return McpToolResult.error("SQL 語句不能為空");
            }

            if (parametersList == null || parametersList.isEmpty()) {
                return McpToolResult.error("批次參數列表不能為空");
            }

            int[] results = queryService.executeBatch(connectionId, sql, parametersList);

            return McpToolResult.success(
                "批次執行成功",
                Map.of(
                    "batchResults", results,
                    "batchSize", results.length,
                    "totalAffectedRows", java.util.Arrays.stream(results).sum()
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("執行批次操作失敗: " + e.getMessage());
        }
    }
}