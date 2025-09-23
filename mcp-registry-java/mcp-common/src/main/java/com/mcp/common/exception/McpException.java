package com.mcp.common.exception;

/**
 * MCP 相關例外的基礎類別
 */
public class McpException extends RuntimeException {

    private final String errorCode;

    public McpException(String message) {
        super(message);
        this.errorCode = "MCP_ERROR";
    }

    public McpException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public McpException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "MCP_ERROR";
    }

    public McpException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}