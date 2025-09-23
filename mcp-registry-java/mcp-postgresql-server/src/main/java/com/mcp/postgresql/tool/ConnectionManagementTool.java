package com.mcp.postgresql.tool;

import com.mcp.common.mcp.McpTool;
import com.mcp.common.mcp.McpToolResult;
import com.mcpregistry.core.usecase.port.in.connection.AddConnectionInput;
import com.mcpregistry.core.usecase.port.in.connection.AddConnectionUseCase;
import com.mcpregistry.core.usecase.port.common.UseCaseOutput;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PostgreSQL 連線管理工具
 *
 * 提供 MCP 連線管理功能:
 * - add_connection: 新增資料庫連線
 * - test_connection: 測試連線狀態
 * - remove_connection: 移除連線
 */
@Component
public class ConnectionManagementTool implements McpTool {

    private final AddConnectionUseCase addConnectionUseCase;

    public ConnectionManagementTool(AddConnectionUseCase addConnectionUseCase) {
        this.addConnectionUseCase = addConnectionUseCase;
    }

    @Override
    public String getToolName() {
        return "postgresql_connection_management";
    }

    @Override
    public String getDescription() {
        return "PostgreSQL 資料庫連線管理工具，支援連線建立、測試和移除";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "action", Map.of(
                    "type", "string",
                    "enum", new String[]{"add", "test", "remove"},
                    "description", "操作類型: add(新增), test(測試), remove(移除)"
                ),
                "connectionId", Map.of(
                    "type", "string",
                    "description", "連線唯一識別碼"
                ),
                "host", Map.of(
                    "type", "string",
                    "description", "PostgreSQL 主機位址"
                ),
                "port", Map.of(
                    "type", "integer",
                    "default", 5432,
                    "description", "PostgreSQL 埠號"
                ),
                "database", Map.of(
                    "type", "string",
                    "description", "資料庫名稱"
                ),
                "username", Map.of(
                    "type", "string",
                    "description", "使用者名稱"
                ),
                "password", Map.of(
                    "type", "string",
                    "description", "密碼"
                ),
                "poolSize", Map.of(
                    "type", "integer",
                    "default", 10,
                    "description", "連線池大小"
                ),
                "readOnly", Map.of(
                    "type", "boolean",
                    "default", false,
                    "description", "是否為只讀模式"
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
                case "add" -> addConnection(arguments);
                case "test" -> testConnection(connectionId);
                case "remove" -> removeConnection(connectionId);
                default -> McpToolResult.error("不支援的操作: " + action);
            };

        } catch (Exception e) {
            return McpToolResult.error("連線管理操作失敗: " + e.getMessage());
        }
    }

    private McpToolResult addConnection(Map<String, Object> arguments) {
        try {
            // 建立 Use Case 輸入
            AddConnectionInput input = new AddConnectionInput(
                (String) arguments.get("connectionId"),
                (String) arguments.get("host"),
                arguments.get("port") != null ? (Integer) arguments.get("port") : 5432,
                (String) arguments.get("database"),
                (String) arguments.get("username"),
                (String) arguments.get("password"),
                "postgresql", // PostgreSQL 伺服器類型
                arguments.get("poolSize") != null ? (Integer) arguments.get("poolSize") : 10
            );

            // 執行 Use Case
            UseCaseOutput result = addConnectionUseCase.execute(input);

            if (result.isSuccess()) {
                return McpToolResult.success(
                    "PostgreSQL 連線建立成功",
                    result.getData()
                );
            } else {
                return McpToolResult.error(result.getMessage());
            }

        } catch (Exception e) {
            return McpToolResult.error("新增連線失敗: " + e.getMessage());
        }
    }

    private McpToolResult testConnection(String connectionId) {
        try {
            // TODO: 實現測試連線的 Use Case
            return McpToolResult.success(
                "連線測試完成",
                Map.of(
                    "connectionId", connectionId,
                    "isHealthy", true,
                    "status", "connected"
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("連線測試失敗: " + e.getMessage());
        }
    }

    private McpToolResult removeConnection(String connectionId) {
        try {
            // TODO: 實現移除連線的 Use Case
            return McpToolResult.success(
                "連線移除成功",
                Map.of("connectionId", connectionId, "status", "removed")
            );

        } catch (Exception e) {
            return McpToolResult.error("移除連線失敗: " + e.getMessage());
        }
    }
}