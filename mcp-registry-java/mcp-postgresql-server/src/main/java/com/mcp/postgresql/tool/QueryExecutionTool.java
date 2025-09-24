package com.mcp.postgresql.tool;

import com.mcp.common.mcp.McpTool;
import com.mcp.common.mcp.McpToolResult;
import com.mcp.common.model.QueryResult;
import com.mcp.postgresql.service.DatabaseQueryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * PostgreSQL query execution tool
 *
 * Provides MCP query execution functionality:
 * - execute_query: Execute SELECT queries
 * - execute_update: Execute INSERT/UPDATE/DELETE
 * - execute_transaction: Execute transaction operations
 * - batch_execute: Execute batch queries
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
        return "PostgreSQL query execution tool, supports SELECT, UPDATE, transactions and batch operations";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "action", Map.of(
                    "type", "string",
                    "enum", new String[]{"query", "update", "transaction", "batch"},
                    "description", "Operation type: query(select), update(modify), transaction(atomic), batch(bulk)"
                ),
                "connectionId", Map.of(
                    "type", "string",
                    "description", "Connection identifier"
                ),
                "sql", Map.of(
                    "type", "string",
                    "description", "SQL statement"
                ),
                "parameters", Map.of(
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "description", "SQL parameter list"
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
                    "description", "Batch query list"
                ),
                "fetchSize", Map.of(
                    "type", "integer",
                    "default", 1000,
                    "description", "Number of records to fetch per request"
                ),
                "timeout", Map.of(
                    "type", "integer",
                    "default", 30,
                    "description", "Query timeout in seconds"
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
                return McpToolResult.error("Connection ID cannot be empty");
            }

            return switch (action) {
                case "query" -> executeQuery(arguments);
                case "update" -> executeUpdate(arguments);
                case "transaction" -> executeTransaction(arguments);
                case "batch" -> executeBatch(arguments);
                default -> McpToolResult.error("Unsupported operation: " + action);
            };

        } catch (Exception e) {
            return McpToolResult.error("Query execution failed: " + e.getMessage());
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
                return McpToolResult.error("SQL statement cannot be empty");
            }

            QueryResult result = queryService.executeQuery(
                connectionId,
                sql,
                parameters,
                fetchSize != null ? fetchSize : 1000
            );

            return McpToolResult.success(
                "Query executed successfully",
                Map.of(
                    "result", result,
                    "rowCount", result.getRowCount(),
                    "executionTimeMs", result.getExecutionTimeMs()
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("Query execution failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private McpToolResult executeUpdate(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connectionId");
            String sql = (String) arguments.get("sql");
            List<Object> parameters = (List<Object>) arguments.get("parameters");

            if (sql == null || sql.trim().isEmpty()) {
                return McpToolResult.error("SQL statement cannot be empty");
            }

            int affectedRows = queryService.executeUpdate(connectionId, sql, parameters);

            return McpToolResult.success(
                "Update executed successfully",
                Map.of(
                    "affectedRows", affectedRows,
                    "sql", sql
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("Update execution failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private McpToolResult executeTransaction(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connectionId");
            List<Map<String, Object>> queries = (List<Map<String, Object>>) arguments.get("queries");

            if (queries == null || queries.isEmpty()) {
                return McpToolResult.error("Transaction query list cannot be empty");
            }

            List<Object> results = queryService.executeTransaction(connectionId, queries);

            return McpToolResult.success(
                "Transaction executed successfully",
                Map.of(
                    "results", results,
                    "queryCount", queries.size()
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("Transaction execution failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private McpToolResult executeBatch(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connectionId");
            String sql = (String) arguments.get("sql");
            List<List<Object>> parametersList = (List<List<Object>>) arguments.get("parameters");

            if (sql == null || sql.trim().isEmpty()) {
                return McpToolResult.error("SQL statement cannot be empty");
            }

            if (parametersList == null || parametersList.isEmpty()) {
                return McpToolResult.error("Batch parameter list cannot be empty");
            }

            int[] results = queryService.executeBatch(connectionId, sql, parametersList);

            return McpToolResult.success(
                "Batch executed successfully",
                Map.of(
                    "batchResults", results,
                    "batchSize", results.length,
                    "totalAffectedRows", java.util.Arrays.stream(results).sum()
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("Batch execution failed: " + e.getMessage());
        }
    }
}