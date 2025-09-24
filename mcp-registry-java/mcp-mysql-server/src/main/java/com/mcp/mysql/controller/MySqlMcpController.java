package com.mcp.mysql.controller;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.mysql.tool.ConnectionManagementTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MySQL MCP Server main controller
 *
 * Coordinates all MCP Tools and Resources
 * Provides unified MCP protocol interface
 */
@RestController
@RequestMapping("/mcp/mysql")
public class MySqlMcpController {

    private static final Logger log = LoggerFactory.getLogger(MySqlMcpController.class);

    private final ConnectionManagementTool connectionTool;

    public MySqlMcpController(ConnectionManagementTool connectionTool) {
        this.connectionTool = connectionTool;
    }

    /**
     * List all available tools
     */
    @GetMapping("/tools")
    public Map<String, Object> listTools() {
        log.info("Listing all MySQL MCP tools");

        return Map.of(
            "tools", List.of(
                Map.of(
                    "name", connectionTool.getToolName(),
                    "description", connectionTool.getDescription(),
                    "schema", connectionTool.getParameterSchema()
                )
            ),
            "serverType", "MySQL",
            "version", "1.0.0",
            "timestamp", java.time.LocalDateTime.now().toString()
        );
    }

    /**
     * Execute tool operation
     */
    @PostMapping("/tools/{toolName}")
    public McpToolResult executeTool(@PathVariable String toolName,
                                   @RequestBody Map<String, Object> arguments) {
        log.info("Executing tool: {} with arguments: {}", toolName, arguments.keySet());

        try {
            return switch (toolName) {
                case "mysql_connection_management" -> connectionTool.execute(arguments);
                default -> McpToolResult.error("Unknown tool: " + toolName);
            };

        } catch (Exception e) {
            log.error("Tool execution failed: " + toolName, e);
            return McpToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "serverType", "MySQL MCP Server",
            "version", "1.0.0",
            "timestamp", java.time.LocalDateTime.now().toString()
        );
    }

    /**
     * Service information endpoint
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
            "name", "MySQL MCP Server",
            "description", "Model Context Protocol Server for MySQL operations",
            "version", "1.0.0",
            "protocol", "MCP",
            "database", "MySQL",
            "capabilities", Map.of(
                "tools", List.of("connection_management"),
                "features", List.of("connection_pooling", "health_check")
            ),
            "timestamp", java.time.LocalDateTime.now().toString()
        );
    }
}