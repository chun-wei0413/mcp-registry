package com.mcp.contextcore.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcp.contextcore.domain.entity.LogSearchResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search Result Response DTO
 *
 * Response object for search results with similarity score.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {

    /**
     * The log entry
     */
    @JsonProperty("log")
    private LogResponse log;

    /**
     * Similarity score (0.0 to 1.0)
     */
    @JsonProperty("score")
    private float score;

    /**
     * Creates a SearchResultResponse from a LogSearchResult entity
     */
    public static SearchResultResponse from(LogSearchResult result) {
        return SearchResultResponse.builder()
                .log(LogResponse.from(result.getLog()))
                .score(result.getScore())
                .build();
    }
}
