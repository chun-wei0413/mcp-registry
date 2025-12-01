package com.mcp.common.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Query result model
 */
@Data
@Builder
@Jacksonized
public class QueryResult {

    /**
     * Whether the query was successful
     */
    private final Boolean success;

    /**
     * Number of rows in result set
     */
    private final Integer rowCount;

    /**
     * Result data
     */
    private final List<Map<String, Object>> rows;

    /**
     * Column information
     */
    private final List<ColumnInfo> columns;

    /**
     * Execution time (milliseconds)
     */
    private final Long executionTimeMs;

    /**
     * Query start time
     */
    @Builder.Default
    private final Instant startTime = Instant.now();

    /**
     * Error message (if any)
     */
    private final String errorMessage;

    /**
     * Execution plan (if enabled)
     */
    private final String executionPlan;

    /**
     * Whether there are more results
     */
    @Builder.Default
    private final Boolean hasMore = false;

    /**
     * Column information model
     */
    @Data
    @Builder
    @Jacksonized
    public static class ColumnInfo {
        private final String name;
        private final String type;
        private final Boolean nullable;
        private final Integer precision;
        private final Integer scale;
    }

    /**
     * Create successful result
     */
    public static QueryResult success(List<Map<String, Object>> rows,
                                    List<ColumnInfo> columns,
                                    long executionTimeMs) {
        return QueryResult.builder()
                .success(true)
                .rows(rows)
                .rowCount(rows != null ? rows.size() : 0)
                .columns(columns)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    /**
     * Create failure result
     */
    public static QueryResult failure(String errorMessage, long executionTimeMs) {
        return QueryResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .executionTimeMs(executionTimeMs)
                .build();
    }
}