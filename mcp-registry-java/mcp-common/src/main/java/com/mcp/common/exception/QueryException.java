package com.mcp.common.exception;

/**
 * 查詢執行相關例外
 */
public class QueryException extends McpException {

    public QueryException(String message) {
        super(message, "QUERY_ERROR");
    }

    public QueryException(String message, Throwable cause) {
        super(message, "QUERY_ERROR", cause);
    }

    /**
     * SQL 語法錯誤
     */
    public static class SqlSyntaxError extends QueryException {
        public SqlSyntaxError(String message, Throwable cause) {
            super("SQL syntax error: " + message, cause);
        }
    }

    /**
     * 查詢超時例外
     */
    public static class QueryTimeout extends QueryException {
        public QueryTimeout(int timeoutSeconds) {
            super("Query timeout after " + timeoutSeconds + " seconds");
        }
    }

    /**
     * 不允許的操作例外
     */
    public static class OperationNotAllowed extends QueryException {
        public OperationNotAllowed(String operation) {
            super("Operation not allowed: " + operation);
        }
    }

    /**
     * SQL 注入檢測例外
     */
    public static class SqlInjectionDetected extends QueryException {
        public SqlInjectionDetected(String reason) {
            super("Potential SQL injection detected: " + reason);
        }
    }
}