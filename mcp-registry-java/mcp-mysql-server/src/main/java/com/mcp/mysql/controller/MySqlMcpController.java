package com.mcp.mysql.controller;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.mysql.tool.ConnectionManagementTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MySQL MCP Server 主控制器
 *
 * 協調所有 MCP Tools 和 Resources
 * 提供統一的 MCP 協議介面
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
     * 列出所有可用的工具
     */
    @GetMapping("/tools")
    public Map<String, Object> listTools() {
        log.info("列出所有 MySQL MCP 工具");

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
     * 執行工具操作
     */
    @PostMapping("/tools/{toolName}")
    public McpToolResult executeTool(@PathVariable String toolName,
                                   @RequestBody Map<String, Object> arguments) {
        log.info("執行工具: {} with arguments: {}", toolName, arguments.keySet());

        try {
            return switch (toolName) {
                case "mysql_connection_management" -> connectionTool.execute(arguments);
                default -> McpToolResult.error("未知的工具: " + toolName);
            };

        } catch (Exception e) {
            log.error("工具執行失敗: " + toolName, e);
            return McpToolResult.error("工具執行失敗: " + e.getMessage());
        }
    }

    /**
     * 健康檢查端點
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
     * 服務資訊端點
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