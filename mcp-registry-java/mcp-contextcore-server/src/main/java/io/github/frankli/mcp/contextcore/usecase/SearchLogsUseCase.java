package io.github.frankli.mcp.contextcore.usecase;

import io.github.frankli.mcp.contextcore.domain.entity.Log;
import io.github.frankli.mcp.contextcore.domain.entity.LogSearchResult;
import io.github.frankli.mcp.contextcore.domain.repository.EmbeddingService;
import io.github.frankli.mcp.contextcore.domain.repository.LogRepository;
import io.github.frankli.mcp.contextcore.domain.repository.VectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Use Case: Search Logs
 *
 * Performs semantic search on logs using vector similarity
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogsUseCase {

    private final LogRepository logRepository;
    private final VectorRepository vectorRepository;
    private final EmbeddingService embeddingService;

    /**
     * Executes semantic search for logs
     *
     * @param query search query text
     * @param limit maximum number of results
     * @param tags filter by tags
     * @param module filter by module
     * @param type filter by type
     * @param dateFrom filter by start date
     * @param dateTo filter by end date
     * @return flux of search results with similarity scores
     */
    public Flux<LogSearchResult> execute(String query, Integer limit, List<String> tags,
                                         String module, String type,
                                         Instant dateFrom, Instant dateTo) {

        log.info("Searching logs: query='{}', limit={}, tags={}, module={}, type={}",
                query, limit, tags, module, type);

        int searchLimit = limit != null && limit > 0 ? limit : 5;

        // 1. Generate query embedding
        return embeddingService.embed(query)
                .flatMapMany(queryVector -> {
                    // 2. Search similar vectors in Qdrant
                    return vectorRepository.searchSimilar(
                            queryVector,
                            searchLimit,
                            tags,
                            module,
                            type
                    );
                })
                .flatMap(scoredLogId -> {
                    // 3. Retrieve full log from SQLite
                    return logRepository.findById(scoredLogId.logId())
                            .map(log -> LogSearchResult.of(log, scoredLogId.score()));
                })
                .filter(result -> {
                    // 4. Apply date range filter if specified
                    if (dateFrom == null && dateTo == null) {
                        return true;
                    }

                    Instant timestamp = result.getLog().getTimestamp();

                    if (dateFrom != null && timestamp.isBefore(dateFrom)) {
                        return false;
                    }

                    if (dateTo != null && timestamp.isAfter(dateTo)) {
                        return false;
                    }

                    return true;
                })
                .doOnComplete(() -> log.info("Search completed for query: '{}'", query))
                .doOnError(error -> log.error("Search failed for query: '{}'", query, error));
    }
}
