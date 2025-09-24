package com.mcp.common.mcp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MCP Resource result
 *
 * Standardized resource content format
 */
public class McpResourceResult {

    private final boolean success;
    private final String content;
    private final Object data;
    private final String mimeType;
    private final String error;
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;

    private McpResourceResult(boolean success, String content, Object data, String mimeType, String error, Map<String, Object> metadata) {
        this.success = success;
        this.content = content;
        this.data = data;
        this.mimeType = mimeType;
        this.error = error;
        this.timestamp = LocalDateTime.now();
        this.metadata = metadata;
    }

    public static McpResourceResult success(String content, Object data, String mimeType) {
        return new McpResourceResult(true, content, data, mimeType, null, null);
    }

    public static McpResourceResult success(String content, Object data, String mimeType, Map<String, Object> metadata) {
        return new McpResourceResult(true, content, data, mimeType, null, metadata);
    }

    public static McpResourceResult error(String error) {
        return new McpResourceResult(false, null, null, null, error, null);
    }

    public static McpResourceResult error(String error, Map<String, Object> metadata) {
        return new McpResourceResult(false, null, null, null, error, metadata);
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

    public String getMimeType() {
        return mimeType;
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
        return "McpResourceResult{" +
                "success=" + success +
                ", content='" + content + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", error='" + error + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}