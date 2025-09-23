package com.mcp.common.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * 查詢請求模型
 */
@Data
@Builder
@Jacksonized
public class QueryRequest {

    /**
     * 連線識別碼
     */
    @NotBlank(message = "Connection ID cannot be blank")
    private final String connectionId;

    /**
     * SQL 查詢語句
     */
    @NotBlank(message = "Query cannot be blank")
    @Size(max = 50000, message = "Query length cannot exceed 50000 characters")
    private final String query;

    /**
     * 查詢參數
     */
    private final List<Object> params;

    /**
     * 查詢超時時間（秒）
     */
    @Builder.Default
    private final Integer timeoutSeconds = 30;

    /**
     * 最大結果集大小
     */
    @Builder.Default
    private final Integer maxRows = 10000;

    /**
     * 是否要執行計畫分析
     */
    @Builder.Default
    private final Boolean explain = false;
}