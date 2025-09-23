package com.mcp.common.mcp;

import java.util.Map;

/**
 * MCP Resource 基礎介面
 *
 * 定義所有 MCP 資源的基本結構
 * 遵循 Model Context Protocol 規範
 */
public interface McpResource {

    /**
     * 資源 URI
     */
    String getResourceUri();

    /**
     * 資源描述
     */
    String getDescription();

    /**
     * 資源類型
     */
    String getMimeType();

    /**
     * 獲取資源內容
     *
     * @param parameters 查詢參數
     * @return 資源內容
     */
    McpResourceResult getContent(Map<String, Object> parameters);
}