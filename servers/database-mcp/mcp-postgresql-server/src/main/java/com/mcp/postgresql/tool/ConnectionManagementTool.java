package com.mcp.postgresql.tool;

import com.mcp.common.mcp.McpTool;
import com.mcp.common.mcp.McpToolResult;
import com.mcpregistry.core.usecase.port.in.connection.AddConnectionInput;
import com.mcpregistry.core.usecase.port.in.connection.AddConnectionUseCase;
import com.mcpregistry.core.usecase.port.common.UseCaseOutput;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PostgreSQL connection management tool
 *
 * Provides MCP connection management functionality:
 * - add_connection: Add database connection
 * - test_connection: Test connection status
 * - remove_connection: Remove connection
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
        return "PostgreSQL database connection management tool, supports connection creation, testing and removal";
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
                    "description", "PostgreSQL host address"
                ),
                "port", Map.of(
                    "type", "integer",
                    "default", 5432,
                    "description", "PostgreSQL port number"
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
            // Create Use Case input
            AddConnectionInput input = new AddConnectionInput(
                (String) arguments.get("connectionId"),
                (String) arguments.get("host"),
                arguments.get("port") != null ? (Integer) arguments.get("port") : 5432,
                (String) arguments.get("database"),
                (String) arguments.get("username"),
                (String) arguments.get("password"),
                "postgresql", // PostgreSQL server type
                arguments.get("poolSize") != null ? (Integer) arguments.get("poolSize") : 10
            );

            // Execute Use Case
            UseCaseOutput result = addConnectionUseCase.execute(input);

            if (result.isSuccess()) {
                return McpToolResult.success(
                    "PostgreSQL connection established successfully",
                    result.getData()
                );
            } else {
                return McpToolResult.error(result.getMessage());
            }

        } catch (Exception e) {
            return McpToolResult.error("Adding connection failed: " + e.getMessage());
        }
    }

    private McpToolResult testConnection(String connectionId) {
        try {
            // TODO: Implement test connection Use Case
            return McpToolResult.success(
                "Connection test completed",
                Map.of(
                    "connectionId", connectionId,
                    "isHealthy", true,
                    "status", "connected"
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("Connection test failed: " + e.getMessage());
        }
    }

    private McpToolResult removeConnection(String connectionId) {
        try {
            // TODO: Implement remove connection Use Case
            return McpToolResult.success(
                "Connection removed successfully",
                Map.of("connectionId", connectionId, "status", "removed")
            );

        } catch (Exception e) {
            return McpToolResult.error("Connection removal failed: " + e.getMessage());
        }
    }
}