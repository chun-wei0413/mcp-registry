package com.mcp.contextcore.usecase;

import com.mcp.contextcore.domain.entity.LogSearchResult;
import com.mcp.contextcore.domain.repository.EmbeddingService;
import com.mcp.contextcore.domain.repository.LogRepository;
import com.mcp.contextcore.domain.repository.VectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Search Logs Use Case
 *
 * Business logic for semantic search of log entries.
 * This use case orchestrates the following operations:
 * 1. Generates embedding for the query text
 * 2. Searches for similar vectors in Qdrant
 * 3. Retrieves matching logs from SQLite
 * 4. Returns results sorted by similarity score
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogsUseCase {

    private final LogRepository logRepository;
    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;

    /**
     * Executes the search logs use case
     *
     * @param query the search query text
     * @param limit maximum number of results
     * @param tags filter by tags (null for no filter)
     * @param module filter by module (null for no filter)
     * @param type filter by type (null for no filter)
     * @return search results with similarity scores
     */
    public Flux<LogSearchResult> execute(String query, int limit, List<String> tags,
                                         String module, String type) {
        log.info("Searching logs: query={}, limit={}, module={}, type={}", query, limit, module, type);

        if (query == null || query.trim().isEmpty()) {
            log.error("Search query cannot be empty");
            return Flux.error(new IllegalArgumentException("Search query cannot be empty"));
        }

        // 1. Generate embedding for query
        return embeddingService.embed(query)
                .flatMapMany(queryVector -> {
                    log.debug("Generated query embedding");

                    // 2. Search for similar vectors
                    return vectorRepository.searchSimilar(queryVector, limit, tags, module, type)
                            .collectList()
                            .flatMapMany(scoredIds -> {
                                if (scoredIds.isEmpty()) {
                                    log.debug("No similar logs found");
                                    return Flux.empty();
                                }

                                log.debug("Found {} similar logs", scoredIds.size());

                                // 3. Retrieve logs by IDs
                                List<String> logIds = scoredIds.stream()
                                        .map(VectorRepository.ScoredLogId::logId)
                                        .toList();

                                return logRepository.findByIds(logIds)
                                        .map(log -> {
                                            // Find matching score
                                            float score = scoredIds.stream()
                                                    .filter(s -> s.logId().equals(log.getId()))
                                                    .findFirst()
                                                    .map(VectorRepository.ScoredLogId::score)
                                                    .orElse(0.0f);

                                            return LogSearchResult.of(log, score);
                                        })
                                        // Sort by score descending
                                        .sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
                            });
                })
                .doOnComplete(() -> log.info("Search completed"))
                .doOnError(error -> log.error("Search failed: {}", error.getMessage()));
    }
}
