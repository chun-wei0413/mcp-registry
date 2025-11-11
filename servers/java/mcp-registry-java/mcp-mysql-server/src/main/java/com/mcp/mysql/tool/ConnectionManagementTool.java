package com.mcp.mysql.tool;

import com.mcp.common.mcp.McpTool;
import com.mcp.common.mcp.McpToolResult;
import com.mcp.common.model.ConnectionInfo;
import com.mcp.mysql.service.DatabaseConnectionService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MySQL connection management tool
 *
 * Provides MCP connection management functionality:
 * - add_connection: Add database connection
 * - test_connection: Test connection status
 * - remove_connection: Remove connection
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
        return "MySQL database connection management tool, supports connection creation, testing and removal";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "action", Map.of(
                    "type", "string",
                    "enum", new String[]{"add", "test", "remove"},
                    "description", "Operation type: add(create), test(verify), remove(delete)"
                ),
                "connectionId", Map.of(
                    "type", "string",
                    "description", "Unique connection identifier"
                ),
                "host", Map.of(
                    "type", "string",
                    "description", "MySQL host address"
                ),
                "port", Map.of(
                    "type", "integer",
                    "default", 3306,
                    "description", "MySQL port number"
                ),
                "database", Map.of(
                    "type", "string",
                    "description", "Database name"
                ),
                "username", Map.of(
                    "type", "string",
                    "description", "Username"
                ),
                "password", Map.of(
                    "type", "string",
                    "description", "Password"
                ),
                "poolSize", Map.of(
                    "type", "integer",
                    "default", 10,
                    "description", "Connection pool size"
                ),
                "readOnly", Map.of(
                    "type", "boolean",
                    "default", false,
                    "description", "Whether to enable read-only mode"
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
                case "add" -> addConnection(arguments);
                case "test" -> testConnection(connectionId);
                case "remove" -> removeConnection(connectionId);
                default -> McpToolResult.error("Unsupported operation: " + action);
            };

        } catch (Exception e) {
            return McpToolResult.error("Connection management operation failed: " + e.getMessage());
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
                    "MySQL connection established successfully",
                    Map.of(
                        "connectionId", connectionInfo.getConnectionId(),
                        "host", connectionInfo.getHost(),
                        "database", connectionInfo.getDatabase(),
                        "status", "connected"
                    )
                );
            } else {
                return McpToolResult.error("Connection establishment failed");
            }

        } catch (Exception e) {
            return McpToolResult.error("Adding connection failed: " + e.getMessage());
        }
    }

    private McpToolResult testConnection(String connectionId) {
        try {
            boolean isHealthy = connectionService.testConnection(connectionId);

            return McpToolResult.success(
                "Connection test completed",
                Map.of(
                    "connectionId", connectionId,
                    "isHealthy", isHealthy,
                    "status", isHealthy ? "connected" : "disconnected"
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("Connection test failed: " + e.getMessage());
        }
    }

    private McpToolResult removeConnection(String connectionId) {
        try {
            boolean success = connectionService.removeConnection(connectionId);

            if (success) {
                return McpToolResult.success(
                    "Connection removed successfully",
                    Map.of("connectionId", connectionId, "status", "removed")
                );
            } else {
                return McpToolResult.error("Connection does not exist or removal failed");
            }

        } catch (Exception e) {
            return McpToolResult.error("Connection removal failed: " + e.getMessage());
        }
    }
}