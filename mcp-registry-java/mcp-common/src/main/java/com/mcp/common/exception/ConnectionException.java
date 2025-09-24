package com.mcp.common.exception;

/**
 * Database connection-related exceptions
 */
public class ConnectionException extends McpException {

    public ConnectionException(String message) {
        super(message, "CONNECTION_ERROR");
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, "CONNECTION_ERROR", cause);
    }

    /**
     * Connection not found exception
     */
    public static class ConnectionNotFound extends ConnectionException {
        public ConnectionNotFound(String connectionId) {
            super("Connection not found: " + connectionId);
        }
    }

    /**
     * Connection already exists exception
     */
    public static class ConnectionAlreadyExists extends ConnectionException {
        public ConnectionAlreadyExists(String connectionId) {
            super("Connection already exists: " + connectionId);
        }
    }

    /**
     * Connection establishment failure exception
     */
    public static class ConnectionFailure extends ConnectionException {
        public ConnectionFailure(String connectionId, Throwable cause) {
            super("Failed to connect to database: " + connectionId, cause);
        }
    }
}