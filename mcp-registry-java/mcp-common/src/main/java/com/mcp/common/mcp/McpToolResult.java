package com.mcp.common.mcp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MCP Tool 執行結果
 *
 * 標準化的工具執行結果格式
 */
public class McpToolResult {

    private final boolean success;
    private final String content;
    private final Object data;
    private final String error;
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;

    private McpToolResult(boolean success, String content, Object data, String error, Map<String, Object> metadata) {
        this.success = success;
        this.content = content;
        this.data = data;
        this.error = error;
        this.timestamp = LocalDateTime.now();
        this.metadata = metadata;
    }

    public static McpToolResult success(String content, Object data) {
        return new McpToolResult(true, content, data, null, null);
    }

    public static McpToolResult success(String content, Object data, Map<String, Object> metadata) {
        return new McpToolResult(true, content, data, null, metadata);
    }

    public static McpToolResult error(String error) {
        return new McpToolResult(false, null, null, error, null);
    }

    public static McpToolResult error(String error, Map<String, Object> metadata) {
        return new McpToolResult(false, null, null, error, metadata);
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getContent() {
        return content;
    }

    public Object getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "McpToolResult{" +
                "success=" + success +
                ", content='" + content + '\'' +
                ", error='" + error + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}