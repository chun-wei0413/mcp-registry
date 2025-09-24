package com.mcp.common.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Query request model
 */
@Data
@Builder
@Jacksonized
public class QueryRequest {

    /**
     * Connection identifier
     */
    @NotBlank(message = "Connection ID cannot be blank")
    private final String connectionId;

    /**
     * SQL query statement
     */
    @NotBlank(message = "Query cannot be blank")
    @Size(max = 50000, message = "Query length cannot exceed 50000 characters")
    private final String query;

    /**
     * Query parameters
     */
    private final List<Object> params;

    /**
     * Query timeout duration (seconds)
     */
    @Builder.Default
    private final Integer timeoutSeconds = 30;

    /**
     * Maximum result set size
     */
    @Builder.Default
    private final Integer maxRows = 10000;

    /**
     * Whether to execute execution plan analysis
     */
    @Builder.Default
    private final Boolean explain = false;
}