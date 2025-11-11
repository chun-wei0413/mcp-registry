package com.mcpregistry.core.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Query execution aggregate root
 * Manages the lifecycle and results of a single query execution
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
        this.connectionId = Objects.requireNonNull(connectionId, "ConnectionId cannot be null");
        this.query = Objects.requireNonNull(query, "Query cannot be null");
        this.parameters = parameters != null ? List.copyOf(parameters) : List.of();
        this.queryType = determineQueryType(query);
        this.status = QueryStatus.PENDING;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * Mark query as started
     */
    public void markStarted() {
        if (this.status != QueryStatus.PENDING) {
            throw new IllegalStateException("Only pending queries can be started");
        }
        this.status = QueryStatus.EXECUTING;
    }

    /**
     * Mark query execution as successful
     */
    public void markCompleted(Object result) {
        if (this.status != QueryStatus.EXECUTING) {
            throw new IllegalStateException("Only executing queries can be marked as completed");
        }
        this.status = QueryStatus.COMPLETED;
        this.result = result;
        this.completedAt = LocalDateTime.now();
        this.executionTimeMs = calculateExecutionTime();
    }

    /**
     * Mark query execution as failed
     */
    public void markFailed(String errorMessage) {
        if (this.status != QueryStatus.EXECUTING) {
            throw new IllegalStateException("Only executing queries can be marked as failed");
        }
        this.status = QueryStatus.FAILED;
        this.errorMessage = Objects.requireNonNull(errorMessage, "Error message cannot be null");
        this.completedAt = LocalDateTime.now();
        this.executionTimeMs = calculateExecutionTime();
    }

    /**
     * Cancel query execution
     */
    public void cancel() {
        if (this.status == QueryStatus.COMPLETED || this.status == QueryStatus.FAILED) {
            throw new IllegalStateException("Completed or failed queries cannot be cancelled");
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
     * Check if query is completed (successful or failed)
     */
    public boolean isCompleted() {
        return status == QueryStatus.COMPLETED || status == QueryStatus.FAILED || status == QueryStatus.CANCELLED;
    }

    /**
     * Check if query is successful
     */
    public boolean isSuccessful() {
        return status == QueryStatus.COMPLETED;
    }

    /**
     * Get safe query summary (truncate long queries)
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