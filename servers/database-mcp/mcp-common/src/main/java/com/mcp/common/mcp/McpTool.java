package com.mcp.common.mcp;

import java.util.Map;

/**
 * MCP Tool base interface
 *
 * Defines the basic structure of all MCP tools
 * Follows Model Context Protocol specifications
 */
public interface McpTool {

    /**
     * Tool name (tool name in MCP protocol)
     */
    String getToolName();

    /**
     * Tool description
     */
    String getDescription();

    /**
     * Tool parameter Schema (JSON Schema format)
     */
    Map<String, Object> getParameterSchema();

    /**
     * Execute tool operation
     *
     * @param arguments Tool parameters
     * @return Execution result
     */
    McpToolResult execute(Map<String, Object> arguments);
}