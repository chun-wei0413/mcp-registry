package com.mcpregistry.core.usecase.port.out;

import com.mcpregistry.core.entity.ConnectionId;
import com.mcpregistry.core.entity.QueryExecution;
import com.mcpregistry.core.entity.QueryId;
import com.mcpregistry.core.entity.QueryStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 查詢執行 Repository 輸出端口
 */
public interface QueryExecutionRepository {

    /**
     * 保存查詢執行記錄
     */
    void save(QueryExecution queryExecution);

    /**
     * 根據 ID 查找查詢執行記錄
     */
    Optional<QueryExecution> findById(QueryId queryId);

    /**
     * 查找指定連線的查詢執行記錄
     */
    List<QueryExecution> findByConnectionId(ConnectionId connectionId);

    /**
     * 查找指定狀態的查詢執行記錄
     */
    List<QueryExecution> findByStatus(QueryStatus status);

    /**
     * 查找指定時間範圍內的查詢執行記錄
     */
    List<QueryExecution> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找最近的查詢執行記錄
     */
    List<QueryExecution> findRecent(int limit);

    /**
     * 刪除舊的查詢執行記錄
     */
    void deleteOlderThan(LocalDateTime cutoffTime);

    /**
     * 計算指定連線的查詢執行統計
     */
    QueryExecutionStats getStatsForConnection(ConnectionId connectionId);

    /**
     * 查詢執行統計資料的值對象
     */
    class QueryExecutionStats {
        public final long totalQueries;
        public final long successfulQueries;
        public final long failedQueries;
        public final double averageExecutionTimeMs;

        public QueryExecutionStats(long totalQueries, long successfulQueries,
                                 long failedQueries, double averageExecutionTimeMs) {
            this.totalQueries = totalQueries;
            this.successfulQueries = successfulQueries;
            this.failedQueries = failedQueries;
            this.averageExecutionTimeMs = averageExecutionTimeMs;
        }
    }
}