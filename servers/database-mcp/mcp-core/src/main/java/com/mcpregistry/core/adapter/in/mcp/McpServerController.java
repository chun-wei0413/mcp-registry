package com.mcpregistry.core.adapter.in.mcp;

import com.mcpregistry.core.adapter.in.mcp.tool.ConnectionManagementTool;
import com.mcpregistry.core.adapter.in.mcp.tool.QueryExecutionTool;
import com.mcpregistry.core.adapter.in.mcp.resource.ConnectionResource;

import java.util.Map;
import java.util.Objects;

/**
 * MCP Server 主控制器
 *
 * 協調所有 MCP Tools 和 Resources
 * 這是 Clean Architecture 的主要 Input Adapter
 */
public class McpServerController {

    private final ConnectionManagementTool connectionTool;
    private final QueryExecutionTool queryTool;
    private final ConnectionResource connectionResource;

    public McpServerController(ConnectionManagementTool connectionTool,
                              QueryExecutionTool queryTool,
                              ConnectionResource connectionResource) {
        this.connectionTool = Objects.requireNonNull(connectionTool, "ConnectionManagementTool 不能為空");
        this.queryTool = Objects.requireNonNull(queryTool, "QueryExecutionTool 不能為空");
        this.connectionResource = Objects.requireNonNull(connectionResource, "ConnectionResource 不能為空");
    }

    /**
     * 處理 MCP Tool 調用
     *
     * @param toolName 工具名稱
     * @param arguments 工具參數
     * @return 工具執行結果
     */
    public McpResponse handleToolCall(String toolName, Map<String, Object> arguments) {
        try {
            return switch (toolName) {
                // 連線管理工具
                case "add_connection" -> McpResponse.fromToolResult(
                    connectionTool.addConnection(arguments)
                );
                case "test_connection" -> McpResponse.fromToolResult(
                    connectionTool.testConnection(arguments)
                );

                // 查詢執行工具
                case "execute_query" -> McpResponse.fromToolResult(
                    queryTool.executeQuery(arguments)
                );
                case "execute_transaction" -> McpResponse.fromToolResult(
                    queryTool.executeTransaction(arguments)
                );
                case "batch_execute" -> McpResponse.fromToolResult(
                    queryTool.batchExecute(arguments)
                );

                default -> McpResponse.error("未知的工具: " + toolName);
            };

        } catch (Exception e) {
            return McpResponse.error("工具執行錯誤: " + e.getMessage());
        }
    }

    /**
     * 處理 MCP Resource 請求
     *
     * @param resourceUri 資源 URI
     * @return 資源內容
     */
    public McpResponse handleResourceRequest(String resourceUri) {
        try {
            return switch (resourceUri) {
                case "connections" -> McpResponse.fromResourceResult(
                    connectionResource.getConnections()
                );
                case "healthy_connections" -> McpResponse.fromResourceResult(
                    connectionResource.getHealthyConnections()
                );
                default -> {
                    // 處理動態資源路徑，如 connection_details/{id}
                    if (resourceUri.startsWith("connection_details/")) {
                        String connectionId = resourceUri.substring("connection_details/".length());
                        yield McpResponse.fromResourceResult(
                            connectionResource.getConnectionDetails(connectionId)
                        );
                    }
                    yield McpResponse.error("未知的資源: " + resourceUri);
                }
            };

        } catch (Exception e) {
            return McpResponse.error("資源訪問錯誤: " + e.getMessage());
        }
    }

    /**
     * 獲取可用的工具列表
     */
    public McpResponse listTools() {
        var tools = Map.of(
            "connection_tools", Map.of(
                "add_connection", "新增資料庫連線",
                "test_connection", "測試資料庫連線"
            ),
            "query_tools", Map.of(
                "execute_query", "執行資料庫查詢",
                "execute_transaction", "執行事務操作",
                "batch_execute", "批次執行查詢"
            )
        );

        return McpResponse.success("可用工具列表", tools);
    }

    /**
     * 獲取可用的資源列表
     */
    public McpResponse listResources() {
        var resources = Map.of(
            "connection_resources", Map.of(
                "connections", "所有資料庫連線",
                "healthy_connections", "健康的資料庫連線",
                "connection_details/{id}", "特定連線的詳細資訊"
            )
        );

        return McpResponse.success("可用資源列表", resources);
    }

    /**
     * MCP 統一回應格式
     */
    public static class McpResponse {
        public final boolean isError;
        public final String message;
        public final Object data;

        private McpResponse(boolean isError, String message, Object data) {
            this.isError = isError;
            this.message = message;
            this.data = data;
        }

        public static McpResponse success(String message, Object data) {
            return new McpResponse(false, message, data);
        }

        public static McpResponse error(String message) {
            return new McpResponse(true, message, null);
        }

        public static McpResponse fromToolResult(ConnectionManagementTool.McpToolResult result) {
            return new McpResponse(result.isError, result.content, result.data);
        }

        public static McpResponse fromResourceResult(ConnectionResource.McpResourceResult result) {
            return new McpResponse(result.isError, result.content, result.data);
        }
    }
}