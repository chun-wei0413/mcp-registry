package io.github.frankli.mcp.contextcore.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Log Search Result
 *
 * Represents a search result with similarity score
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSearchResult {

    /**
     * The log entry
     */
    private Log log;

    /**
     * Similarity score (0.0 to 1.0)
     */
    private float score;

    /**
     * Creates a search result
     */
    public static LogSearchResult of(Log log, float score) {
        return LogSearchResult.builder()
                .log(log)
                .score(score)
                .build();
    }
}
