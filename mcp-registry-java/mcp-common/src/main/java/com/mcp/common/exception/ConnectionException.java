package com.mcp.common.exception;

/**
 * 資料庫連線相關例外
 */
public class ConnectionException extends McpException {

    public ConnectionException(String message) {
        super(message, "CONNECTION_ERROR");
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, "CONNECTION_ERROR", cause);
    }

    /**
     * 連線不存在例外
     */
    public static class ConnectionNotFound extends ConnectionException {
        public ConnectionNotFound(String connectionId) {
            super("Connection not found: " + connectionId);
        }
    }

    /**
     * 連線已存在例外
     */
    public static class ConnectionAlreadyExists extends ConnectionException {
        public ConnectionAlreadyExists(String connectionId) {
            super("Connection already exists: " + connectionId);
        }
    }

    /**
     * 連線建立失敗例外
     */
    public static class ConnectionFailure extends ConnectionException {
        public ConnectionFailure(String connectionId, Throwable cause) {
            super("Failed to connect to database: " + connectionId, cause);
        }
    }
}