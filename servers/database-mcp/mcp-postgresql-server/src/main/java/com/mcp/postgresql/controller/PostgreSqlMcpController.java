package com.mcp.postgresql.controller;

import com.mcp.common.mcp.McpTool;
import com.mcp.common.mcp.McpToolResult;
import com.mcp.common.mcp.McpResource;
import com.mcp.common.mcp.McpResourceResult;
import com.mcp.postgresql.tool.ConnectionManagementTool;
import com.mcp.postgresql.tool.QueryExecutionTool;
import com.mcp.postgresql.tool.SchemaManagementTool;
import com.mcp.postgresql.resource.ConnectionResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * PostgreSQL MCP Server main controller
 *
 * Coordinates all MCP Tools and Resources
 * Provides unified MCP protocol interface
 */
@RestController
@RequestMapping("/mcp/postgresql")
public class PostgreSqlMcpController {

    private static final Logger log = LoggerFactory.getLogger(PostgreSqlMcpController.class);

    private final ConnectionManagementTool connectionTool;
    private final QueryExecutionTool queryTool;
    private final SchemaManagementTool schemaTool;
    private final ConnectionResource connectionResource;

    public PostgreSqlMcpController(ConnectionManagementTool connectionTool,
                                 QueryExecutionTool queryTool,
                                 SchemaManagementTool schemaTool,
                                 ConnectionResource connectionResource) {
        this.connectionTool = connectionTool;
        this.queryTool = queryTool;
        this.schemaTool = schemaTool;
        this.connectionResource = connectionResource;
    }

    /**
     * List all available tools
     */
    @GetMapping("/tools")
    public Map<String, Object> listTools() {
        log.info("Listing all PostgreSQL MCP tools");

        return Map.of(
            "tools", List.of(
                createToolInfo(connectionTool),
                createToolInfo(queryTool),
                createToolInfo(schemaTool)
            ),
            "serverType", "PostgreSQL",
            "version", "1.0.0",
            "timestamp", java.time.LocalDateTime.now().toString()
        );
    }

    /**
     * List all available resources
     */
    @GetMapping("/resources")
    public Map<String, Object> listResources() {
        log.info("Listing all PostgreSQL MCP resources");

        return Map.of(
            "resources", List.of(
                createResourceInfo(connectionResource)
            ),
            "serverType", "PostgreSQL",
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
                case "postgresql_connection_management" -> connectionTool.execute(arguments);
                case "postgresql_query_execution" -> queryTool.execute(arguments);
                case "postgresql_schema_management" -> schemaTool.execute(arguments);
                default -> McpToolResult.error("Unknown tool: " + toolName);
            };

        } catch (Exception e) {
            log.error("Tool execution failed: " + toolName, e);
            return McpToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * Get resource content
     */
    @GetMapping("/resources/{resourceType}")
    public McpResourceResult getResource(@PathVariable String resourceType,
                                       @RequestParam Map<String, Object> parameters) {
        log.info("Getting resource: {} with parameters: {}", resourceType, parameters.keySet());

        try {
            // Add resource type to parameters
            parameters.put("type", resourceType);

            return switch (resourceType) {
                case "connections", "healthy_connections", "connection_details" ->
                    connectionResource.getContent(parameters);
                default -> McpResourceResult.error("Unknown resource type: " + resourceType);
            };

        } catch (Exception e) {
            log.error("Resource retrieval failed: " + resourceType, e);
            return McpResourceResult.error("Resource retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "serverType", "PostgreSQL MCP Server",
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
            "name", "PostgreSQL MCP Server",
            "description", "Model Context Protocol Server for PostgreSQL operations",
            "version", "1.0.0",
            "protocol", "MCP",
            "database", "PostgreSQL",
            "capabilities", Map.of(
                "tools", List.of("connection_management", "query_execution", "schema_management"),
                "resources", List.of("connections", "healthy_connections", "connection_details"),
                "features", List.of("transactions", "batch_operations", "query_explain", "schema_introspection")
            ),
            "timestamp", java.time.LocalDateTime.now().toString()
        );
    }

    /**
     * Create tool information
     */
    private Map<String, Object> createToolInfo(McpTool tool) {
        return Map.of(
            "name", tool.getToolName(),
            "description", tool.getDescription(),
            "schema", tool.getParameterSchema()
        );
    }

    /**
     * Create resource information
     */
    private Map<String, Object> createResourceInfo(McpResource resource) {
        return Map.of(
            "uri", resource.getResourceUri(),
            "description", resource.getDescription(),
            "mimeType", resource.getMimeType()
        );
    }
}