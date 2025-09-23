package com.mcp.mysql.tool;

import com.mcp.common.mcp.McpTool;
import com.mcp.common.mcp.McpToolResult;
import com.mcp.common.model.ConnectionInfo;
import com.mcp.mysql.service.DatabaseConnectionService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MySQL 連線管理工具
 *
 * 提供 MCP 連線管理功能:
 * - add_connection: 新增資料庫連線
 * - test_connection: 測試連線狀態
 * - remove_connection: 移除連線
 */
@Component
public class ConnectionManagementTool implements McpTool {

    private final DatabaseConnectionService connectionService;

    public ConnectionManagementTool(DatabaseConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public String getToolName() {
        return "mysql_connection_management";
    }

    @Override
    public String getDescription() {
        return "MySQL 資料庫連線管理工具，支援連線建立、測試和移除";
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
                    "description", "MySQL 主機位址"
                ),
                "port", Map.of(
                    "type", "integer",
                    "default", 3306,
                    "description", "MySQL 埠號"
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
            ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .connectionId((String) arguments.get("connectionId"))
                .host((String) arguments.get("host"))
                .port(arguments.get("port") != null ? (Integer) arguments.get("port") : 3306)
                .database((String) arguments.get("database"))
                .username((String) arguments.get("username"))
                .password((String) arguments.get("password"))
                .poolSize(arguments.get("poolSize") != null ? (Integer) arguments.get("poolSize") : 10)
                .readOnly(arguments.get("readOnly") != null ? (Boolean) arguments.get("readOnly") : false)
                .build();

            boolean success = connectionService.addConnection(connectionInfo);

            if (success) {
                return McpToolResult.success(
                    "MySQL 連線建立成功",
                    Map.of(
                        "connectionId", connectionInfo.getConnectionId(),
                        "host", connectionInfo.getHost(),
                        "database", connectionInfo.getDatabase(),
                        "status", "connected"
                    )
                );
            } else {
                return McpToolResult.error("連線建立失敗");
            }

        } catch (Exception e) {
            return McpToolResult.error("新增連線失敗: " + e.getMessage());
        }
    }

    private McpToolResult testConnection(String connectionId) {
        try {
            boolean isHealthy = connectionService.testConnection(connectionId);

            return McpToolResult.success(
                "連線測試完成",
                Map.of(
                    "connectionId", connectionId,
                    "isHealthy", isHealthy,
                    "status", isHealthy ? "connected" : "disconnected"
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("連線測試失敗: " + e.getMessage());
        }
    }

    private McpToolResult removeConnection(String connectionId) {
        try {
            boolean success = connectionService.removeConnection(connectionId);

            if (success) {
                return McpToolResult.success(
                    "連線移除成功",
                    Map.of("connectionId", connectionId, "status", "removed")
                );
            } else {
                return McpToolResult.error("連線不存在或移除失敗");
            }

        } catch (Exception e) {
            return McpToolResult.error("移除連線失敗: " + e.getMessage());
        }
    }
}