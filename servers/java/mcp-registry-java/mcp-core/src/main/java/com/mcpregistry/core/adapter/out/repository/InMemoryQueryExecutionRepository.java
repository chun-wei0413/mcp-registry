package com.mcpregistry.core.adapter.out.repository;

import com.mcpregistry.core.entity.ConnectionId;
import com.mcpregistry.core.entity.QueryExecution;
import com.mcpregistry.core.entity.QueryId;
import com.mcpregistry.core.entity.QueryStatus;
import com.mcpregistry.core.usecase.port.out.QueryExecutionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 記憶體內查詢執行儲存庫實現
 *
 * Output Adapter 實現，提供記憶體內的查詢執行記錄管理
 */
public class InMemoryQueryExecutionRepository implements QueryExecutionRepository {

    private final Map<String, QueryExecution> queryExecutions = new ConcurrentHashMap<>();

    @Override
    public void save(QueryExecution queryExecution) {
        if (queryExecution == null) {
            throw new IllegalArgumentException("QueryExecution 不能為空");
        }
        queryExecutions.put(queryExecution.getId().getValue(), queryExecution);
    }

    @Override
    public Optional<QueryExecution> findById(QueryId queryId) {
        if (queryId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(queryExecutions.get(queryId.getValue()));
    }

    @Override
    public List<QueryExecution> findByConnectionId(ConnectionId connectionId) {
        if (connectionId == null) {
            return List.of();
        }

        return queryExecutions.values().stream()
                .filter(qe -> connectionId.equals(qe.getConnectionId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<QueryExecution> findByStatus(QueryStatus status) {
        if (status == null) {
            return List.of();
        }

        return queryExecutions.values().stream()
                .filter(qe -> status == qe.getStatus())
                .collect(Collectors.toList());
    }

    @Override
    public List<QueryExecution> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return List.of();
        }

        return queryExecutions.values().stream()
                .filter(qe -> !qe.getStartedAt().isBefore(startTime) &&
                             !qe.getStartedAt().isAfter(endTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<QueryExecution> findRecent(int limit) {
        return queryExecutions.values().stream()
                .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt())) // 最新的在前
                .limit(Math.max(0, limit))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteOlderThan(LocalDateTime cutoffTime) {
        if (cutoffTime == null) {
            return;
        }

        var toDelete = queryExecutions.values().stream()
                .filter(qe -> qe.getStartedAt().isBefore(cutoffTime))
                .map(qe -> qe.getId().getValue())
                .collect(Collectors.toList());

        toDelete.forEach(queryExecutions::remove);
    }

    @Override
    public QueryExecutionStats getStatsForConnection(ConnectionId connectionId) {
        if (connectionId == null) {
            return new QueryExecutionStats(0, 0, 0, 0.0);
        }

        var executions = findByConnectionId(connectionId);

        long totalQueries = executions.size();
        long successfulQueries = executions.stream()
                .mapToLong(qe -> qe.isSuccessful() ? 1 : 0)
                .sum();
        long failedQueries = totalQueries - successfulQueries;

        double averageExecutionTimeMs = executions.stream()
                .filter(QueryExecution::isCompleted)
                .mapToLong(QueryExecution::getExecutionTimeMs)
                .average()
                .orElse(0.0);

        return new QueryExecutionStats(totalQueries, successfulQueries, failedQueries, averageExecutionTimeMs);
    }

    /**
     * 清空所有查詢執行記錄（測試用）
     */
    public void clear() {
        queryExecutions.clear();
    }

    /**
     * 獲取查詢執行記錄數量
     */
    public int size() {
        return queryExecutions.size();
    }
}