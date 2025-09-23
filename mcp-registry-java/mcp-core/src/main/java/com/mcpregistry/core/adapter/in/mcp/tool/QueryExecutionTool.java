package com.mcpregistry.core.adapter.in.mcp.tool;

import com.mcpregistry.core.usecase.port.in.query.ExecuteQueryInput;
import com.mcpregistry.core.usecase.port.in.query.ExecuteQueryUseCase;
import com.mcpregistry.core.usecase.port.common.UseCaseOutput;

import java.util.Map;
import java.util.List;
import java.util.Objects;

/**
 * MCP Tool: 查詢執行
 *
 * 提供 MCP 協議的資料庫查詢工具
 */
public class QueryExecutionTool {

    private final ExecuteQueryUseCase executeQueryUseCase;

    public QueryExecutionTool(ExecuteQueryUseCase executeQueryUseCase) {
        this.executeQueryUseCase = Objects.requireNonNull(executeQueryUseCase, "ExecuteQueryUseCase 不能為空");
    }

    /**
     * MCP Tool: execute_query
     * 執行資料庫查詢
     *
     * @param arguments MCP 工具參數，包含：
     *                 - connection_id: 連線識別碼
     *                 - query: SQL 查詢語句
     *                 - parameters: 查詢參數（可選）
     *                 - fetch_size: 獲取行數限制（可選）
     */
    public ConnectionManagementTool.McpToolResult executeQuery(Map<String, Object> arguments) {
        try {
            // 1. 解析 MCP 參數
            ExecuteQueryInput input = parseExecuteQueryArguments(arguments);

            // 2. 執行 Use Case
            UseCaseOutput result = executeQueryUseCase.execute(input);

            // 3. 轉換為 MCP 工具結果
            return convertToMcpResult(result);

        } catch (Exception e) {
            return ConnectionManagementTool.McpToolResult.error("查詢執行失敗: " + e.getMessage());
        }
    }

    /**
     * MCP Tool: execute_transaction
     * 執行事務操作
     */
    public ConnectionManagementTool.McpToolResult executeTransaction(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connection_id");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> queries = (List<Map<String, Object>>) arguments.get("queries");

            if (connectionId == null || connectionId.trim().isEmpty()) {
                return ConnectionManagementTool.McpToolResult.error("連線 ID 不能為空");
            }

            if (queries == null || queries.isEmpty()) {
                return ConnectionManagementTool.McpToolResult.error("查詢列表不能為空");
            }

            // TODO: 實現事務執行邏輯
            // 這裡需要實現 ExecuteTransactionUseCase
            return ConnectionManagementTool.McpToolResult.success("事務執行功能尚未實現");

        } catch (Exception e) {
            return ConnectionManagementTool.McpToolResult.error("事務執行失敗: " + e.getMessage());
        }
    }

    /**
     * MCP Tool: batch_execute
     * 批次執行查詢
     */
    public ConnectionManagementTool.McpToolResult batchExecute(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connection_id");
            String query = (String) arguments.get("query");
            @SuppressWarnings("unchecked")
            List<List<Object>> parametersList = (List<List<Object>>) arguments.get("parameters_list");

            if (connectionId == null || connectionId.trim().isEmpty()) {
                return ConnectionManagementTool.McpToolResult.error("連線 ID 不能為空");
            }

            if (query == null || query.trim().isEmpty()) {
                return ConnectionManagementTool.McpToolResult.error("查詢語句不能為空");
            }

            // TODO: 實現批次執行邏輯
            // 這裡需要實現 BatchExecuteUseCase
            return ConnectionManagementTool.McpToolResult.success("批次執行功能尚未實現");

        } catch (Exception e) {
            return ConnectionManagementTool.McpToolResult.error("批次執行失敗: " + e.getMessage());
        }
    }

    private ExecuteQueryInput parseExecuteQueryArguments(Map<String, Object> arguments) {
        String connectionId = (String) arguments.get("connection_id");
        String query = (String) arguments.get("query");
        @SuppressWarnings("unchecked")
        List<Object> parameters = (List<Object>) arguments.get("parameters");
        Integer fetchSize = null;

        // 解析可選的 fetch_size 參數
        Object fetchSizeObj = arguments.get("fetch_size");
        if (fetchSizeObj instanceof Number) {
            fetchSize = ((Number) fetchSizeObj).intValue();
        }

        return new ExecuteQueryInput(connectionId, query, parameters, fetchSize);
    }

    private ConnectionManagementTool.McpToolResult convertToMcpResult(UseCaseOutput result) {
        if (result.isSuccess()) {
            return ConnectionManagementTool.McpToolResult.success(
                result.getMessage(),
                result.getData().orElse(null)
            );
        } else {
            return ConnectionManagementTool.McpToolResult.error(result.getMessage());
        }
    }
}