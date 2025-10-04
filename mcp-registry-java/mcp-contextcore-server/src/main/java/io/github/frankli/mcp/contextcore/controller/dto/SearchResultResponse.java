package io.github.frankli.mcp.contextcore.controller.dto;

import io.github.frankli.mcp.contextcore.domain.entity.LogSearchResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for search results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {
    private LogResponse log;
    private float score;

    /**
     * Converts domain LogSearchResult to SearchResultResponse
     */
    public static SearchResultResponse from(LogSearchResult searchResult) {
        return SearchResultResponse.builder()
                .log(LogResponse.from(searchResult.getLog()))
                .score(searchResult.getScore())
                .build();
    }
}
