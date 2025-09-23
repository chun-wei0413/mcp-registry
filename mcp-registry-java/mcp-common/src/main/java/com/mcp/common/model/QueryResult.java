package com.mcp.common.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 查詢結果模型
 */
@Data
@Builder
@Jacksonized
public class QueryResult {

    /**
     * 查詢是否成功
     */
    private final Boolean success;

    /**
     * 結果集行數
     */
    private final Integer rowCount;

    /**
     * 結果資料
     */
    private final List<Map<String, Object>> rows;

    /**
     * 欄位資訊
     */
    private final List<ColumnInfo> columns;

    /**
     * 執行時間（毫秒）
     */
    private final Long executionTimeMs;

    /**
     * 查詢開始時間
     */
    @Builder.Default
    private final Instant startTime = Instant.now();

    /**
     * 錯誤訊息（如果有）
     */
    private final String errorMessage;

    /**
     * 執行計畫（如果啟用）
     */
    private final String executionPlan;

    /**
     * 是否有更多結果
     */
    @Builder.Default
    private final Boolean hasMore = false;

    /**
     * 欄位資訊模型
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
     * 建立成功結果
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
     * 建立失敗結果
     */
    public static QueryResult failure(String errorMessage, long executionTimeMs) {
        return QueryResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .executionTimeMs(executionTimeMs)
                .build();
    }
}