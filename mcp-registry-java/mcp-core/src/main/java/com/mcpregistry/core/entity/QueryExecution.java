package com.mcpregistry.core.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 查詢執行聚合根
 * 管理單次查詢執行的生命週期和結果
 */
public class QueryExecution {

    private final QueryId id;
    private final ConnectionId connectionId;
    private final String query;
    private final List<Object> parameters;
    private final QueryType queryType;
    private QueryStatus status;
    private final LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Object result;
    private String errorMessage;
    private long executionTimeMs;

    public QueryExecution(ConnectionId connectionId, String query, List<Object> parameters) {
        this.id = QueryId.generate();
        this.connectionId = Objects.requireNonNull(connectionId, "ConnectionId 不能為空");
        this.query = Objects.requireNonNull(query, "查詢語句不能為空");
        this.parameters = parameters != null ? List.copyOf(parameters) : List.of();
        this.queryType = determineQueryType(query);
        this.status = QueryStatus.PENDING;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 標記查詢開始執行
     */
    public void markStarted() {
        if (this.status != QueryStatus.PENDING) {
            throw new IllegalStateException("只有待執行的查詢可以開始執行");
        }
        this.status = QueryStatus.EXECUTING;
    }

    /**
     * 標記查詢執行成功
     */
    public void markCompleted(Object result) {
        if (this.status != QueryStatus.EXECUTING) {
            throw new IllegalStateException("只有執行中的查詢可以標記為完成");
        }
        this.status = QueryStatus.COMPLETED;
        this.result = result;
        this.completedAt = LocalDateTime.now();
        this.executionTimeMs = calculateExecutionTime();
    }

    /**
     * 標記查詢執行失敗
     */
    public void markFailed(String errorMessage) {
        if (this.status != QueryStatus.EXECUTING) {
            throw new IllegalStateException("只有執行中的查詢可以標記為失敗");
        }
        this.status = QueryStatus.FAILED;
        this.errorMessage = Objects.requireNonNull(errorMessage, "錯誤訊息不能為空");
        this.completedAt = LocalDateTime.now();
        this.executionTimeMs = calculateExecutionTime();
    }

    /**
     * 取消查詢執行
     */
    public void cancel() {
        if (this.status == QueryStatus.COMPLETED || this.status == QueryStatus.FAILED) {
            throw new IllegalStateException("已完成或已失敗的查詢無法取消");
        }
        this.status = QueryStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
        this.executionTimeMs = calculateExecutionTime();
    }

    private QueryType determineQueryType(String query) {
        String trimmedQuery = query.trim().toUpperCase();
        if (trimmedQuery.startsWith("SELECT") || trimmedQuery.startsWith("WITH")) {
            return QueryType.SELECT;
        } else if (trimmedQuery.startsWith("INSERT")) {
            return QueryType.INSERT;
        } else if (trimmedQuery.startsWith("UPDATE")) {
            return QueryType.UPDATE;
        } else if (trimmedQuery.startsWith("DELETE")) {
            return QueryType.DELETE;
        } else if (trimmedQuery.startsWith("CREATE") || trimmedQuery.startsWith("ALTER") || trimmedQuery.startsWith("DROP")) {
            return QueryType.DDL;
        } else {
            return QueryType.OTHER;
        }
    }

    private long calculateExecutionTime() {
        if (completedAt == null) {
            return 0;
        }
        return java.time.Duration.between(startedAt, completedAt).toMillis();
    }

    /**
     * 檢查查詢是否已完成（成功或失敗）
     */
    public boolean isCompleted() {
        return status == QueryStatus.COMPLETED || status == QueryStatus.FAILED || status == QueryStatus.CANCELLED;
    }

    /**
     * 檢查查詢是否成功
     */
    public boolean isSuccessful() {
        return status == QueryStatus.COMPLETED;
    }

    /**
     * 獲取安全的查詢摘要（截斷長查詢）
     */
    public String getQuerySummary() {
        return query.length() > 100 ? query.substring(0, 100) + "..." : query;
    }

    // Getters
    public QueryId getId() { return id; }
    public ConnectionId getConnectionId() { return connectionId; }
    public String getQuery() { return query; }
    public List<Object> getParameters() { return parameters; }
    public QueryType getQueryType() { return queryType; }
    public QueryStatus getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public Object getResult() { return result; }
    public String getErrorMessage() { return errorMessage; }
    public long getExecutionTimeMs() { return executionTimeMs; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryExecution that = (QueryExecution) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "QueryExecution{" +
                "id=" + id +
                ", connectionId=" + connectionId +
                ", queryType=" + queryType +
                ", status=" + status +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}