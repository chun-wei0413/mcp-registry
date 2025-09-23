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
 * PostgreSQL MCP Server 主控制器
 *
 * 協調所有 MCP Tools 和 Resources
 * 提供統一的 MCP 協議介面
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
     * 列出所有可用的工具
     */
    @GetMapping("/tools")
    public Map<String, Object> listTools() {
        log.info("列出所有 PostgreSQL MCP 工具");

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
     * 列出所有可用的資源
     */
    @GetMapping("/resources")
    public Map<String, Object> listResources() {
        log.info("列出所有 PostgreSQL MCP 資源");

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
     * 執行工具操作
     */
    @PostMapping("/tools/{toolName}")
    public McpToolResult executeTool(@PathVariable String toolName,
                                   @RequestBody Map<String, Object> arguments) {
        log.info("執行工具: {} with arguments: {}", toolName, arguments.keySet());

        try {
            return switch (toolName) {
                case "postgresql_connection_management" -> connectionTool.execute(arguments);
                case "postgresql_query_execution" -> queryTool.execute(arguments);
                case "postgresql_schema_management" -> schemaTool.execute(arguments);
                default -> McpToolResult.error("未知的工具: " + toolName);
            };

        } catch (Exception e) {
            log.error("工具執行失敗: " + toolName, e);
            return McpToolResult.error("工具執行失敗: " + e.getMessage());
        }
    }

    /**
     * 獲取資源內容
     */
    @GetMapping("/resources/{resourceType}")
    public McpResourceResult getResource(@PathVariable String resourceType,
                                       @RequestParam Map<String, Object> parameters) {
        log.info("獲取資源: {} with parameters: {}", resourceType, parameters.keySet());

        try {
            // 添加資源類型到參數中
            parameters.put("type", resourceType);

            return switch (resourceType) {
                case "connections", "healthy_connections", "connection_details" ->
                    connectionResource.getContent(parameters);
                default -> McpResourceResult.error("未知的資源類型: " + resourceType);
            };

        } catch (Exception e) {
            log.error("資源獲取失敗: " + resourceType, e);
            return McpResourceResult.error("資源獲取失敗: " + e.getMessage());
        }
    }

    /**
     * 健康檢查端點
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
     * 服務資訊端點
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
     * 建立工具資訊
     */
    private Map<String, Object> createToolInfo(McpTool tool) {
        return Map.of(
            "name", tool.getToolName(),
            "description", tool.getDescription(),
            "schema", tool.getParameterSchema()
        );
    }

    /**
     * 建立資源資訊
     */
    private Map<String, Object> createResourceInfo(McpResource resource) {
        return Map.of(
            "uri", resource.getResourceUri(),
            "description", resource.getDescription(),
            "mimeType", resource.getMimeType()
        );
    }
}