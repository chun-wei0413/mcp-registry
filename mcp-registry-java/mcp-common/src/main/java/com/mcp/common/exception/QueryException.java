package com.mcp.common.exception;

/**
 * Query execution-related exceptions
 */
public class QueryException extends McpException {

    public QueryException(String message) {
        super(message, "QUERY_ERROR");
    }

    public QueryException(String message, Throwable cause) {
        super(message, "QUERY_ERROR", cause);
    }

    /**
     * SQL syntax error
     */
    public static class SqlSyntaxError extends QueryException {
        public SqlSyntaxError(String message, Throwable cause) {
            super("SQL syntax error: " + message, cause);
        }
    }

    /**
     * Query timeout exception
     */
    public static class QueryTimeout extends QueryException {
        public QueryTimeout(int timeoutSeconds) {
            super("Query timeout after " + timeoutSeconds + " seconds");
        }
    }

    /**
     * Operation not allowed exception
     */
    public static class OperationNotAllowed extends QueryException {
        public OperationNotAllowed(String operation) {
            super("Operation not allowed: " + operation);
        }
    }

    /**
     * SQL injection detection exception
     */
    public static class SqlInjectionDetected extends QueryException {
        public SqlInjectionDetected(String reason) {
            super("Potential SQL injection detected: " + reason);
        }
    }
}