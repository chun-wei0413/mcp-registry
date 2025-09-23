package com.mcp.common.mcp;

import java.util.Map;

/**
 * MCP Tool 基礎介面
 *
 * 定義所有 MCP 工具的基本結構
 * 遵循 Model Context Protocol 規範
 */
public interface McpTool {

    /**
     * 工具名稱（MCP 協定中的 tool name）
     */
    String getToolName();

    /**
     * 工具描述
     */
    String getDescription();

    /**
     * 工具參數 Schema（JSON Schema 格式）
     */
    Map<String, Object> getParameterSchema();

    /**
     * 執行工具操作
     *
     * @param arguments 工具參數
     * @return 執行結果
     */
    McpToolResult execute(Map<String, Object> arguments);
}