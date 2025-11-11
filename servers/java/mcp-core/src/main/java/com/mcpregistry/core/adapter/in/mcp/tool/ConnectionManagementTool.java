package com.mcpregistry.core.adapter.in.mcp.tool;

import com.mcpregistry.core.usecase.port.in.connection.AddConnectionInput;
import com.mcpregistry.core.usecase.port.in.connection.AddConnectionUseCase;
import com.mcpregistry.core.usecase.port.in.connection.TestConnectionUseCase;
import com.mcpregistry.core.usecase.port.common.UseCaseOutput;

import java.util.Map;
import java.util.List;
import java.util.Objects;

/**
 * MCP Tool: 資料庫連線管理
 *
 * 提供 MCP 協議的連線管理工具
 * 這是 Clean Architecture 的 Input Adapter
 */
public class ConnectionManagementTool {

    private final AddConnectionUseCase addConnectionUseCase;
    private final TestConnectionUseCase testConnectionUseCase;

    public ConnectionManagementTool(AddConnectionUseCase addConnectionUseCase,
                                   TestConnectionUseCase testConnectionUseCase) {
        this.addConnectionUseCase = Objects.requireNonNull(addConnectionUseCase, "AddConnectionUseCase 不能為空");
        this.testConnectionUseCase = Objects.requireNonNull(testConnectionUseCase, "TestConnectionUseCase 不能為空");
    }

    /**
     * MCP Tool: add_connection
     * 新增資料庫連線
     *
     * @param arguments MCP 工具參數
     * @return MCP 工具執行結果
     */
    public McpToolResult addConnection(Map<String, Object> arguments) {
        try {
            // 1. 解析 MCP 參數
            AddConnectionInput input = parseAddConnectionArguments(arguments);

            // 2. 呼叫 Use Case
            UseCaseOutput result = addConnectionUseCase.execute(input);

            // 3. 轉換為 MCP 回應格式
            return convertToMcpResult(result);

        } catch (Exception e) {
            return McpToolResult.error("新增連線失敗: " + e.getMessage());
        }
    }

    /**
     * MCP Tool: test_connection
     * 測試資料庫連線
     */
    public McpToolResult testConnection(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connection_id");
            if (connectionId == null || connectionId.trim().isEmpty()) {
                return McpToolResult.error("連線 ID 不能為空");
            }

            UseCaseOutput result = testConnectionUseCase.execute(connectionId);
            return convertToMcpResult(result);

        } catch (Exception e) {
            return McpToolResult.error("測試連線失敗: " + e.getMessage());
        }
    }

    private AddConnectionInput parseAddConnectionArguments(Map<String, Object> arguments) {
        return new AddConnectionInput(
            (String) arguments.get("connection_id"),
            (String) arguments.get("host"),
            getIntegerArgument(arguments, "port"),
            (String) arguments.get("database"),
            (String) arguments.get("username"),
            (String) arguments.get("password"),
            (String) arguments.get("server_type"),
            getIntegerArgument(arguments, "pool_size", 10) // 預設值
        );
    }

    private Integer getIntegerArgument(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new IllegalArgumentException("參數 " + key + " 必須是數字");
    }

    private Integer getIntegerArgument(Map<String, Object> arguments, String key, int defaultValue) {
        try {
            return getIntegerArgument(arguments, key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private McpToolResult convertToMcpResult(UseCaseOutput result) {
        if (result.isSuccess()) {
            return McpToolResult.success(
                result.getMessage(),
                result.getData().orElse(null)
            );
        } else {
            return McpToolResult.error(result.getMessage());
        }
    }

    /**
     * MCP Tool 執行結果的值對象
     */
    public static class McpToolResult {
        public final boolean isError;
        public final String content;
        public final Object data;

        private McpToolResult(boolean isError, String content, Object data) {
            this.isError = isError;
            this.content = content;
            this.data = data;
        }

        public static McpToolResult success(String message, Object data) {
            return new McpToolResult(false, message, data);
        }

        public static McpToolResult success(String message) {
            return new McpToolResult(false, message, null);
        }

        public static McpToolResult error(String message) {
            return new McpToolResult(true, message, null);
        }
    }
}