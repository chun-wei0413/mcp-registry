package com.mcp.contextcore.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Search Logs Request DTO
 *
 * Request object for semantic search of log entries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchLogsRequest {

    /**
     * Search query text
     */
    @JsonProperty("query")
    private String query;

    /**
     * Maximum number of results (default: 10)
     */
    @JsonProperty("limit")
    private Integer limit;

    /**
     * Filter by tags (optional)
     */
    @JsonProperty("tags")
    private List<String> tags;

    /**
     * Filter by module (optional)
     */
    @JsonProperty("module")
    private String module;

    /**
     * Filter by type (optional): feature | bug | decision | note
     */
    @JsonProperty("type")
    private String type;
}
