package io.github.frankli.mcp.contextcore.usecase;

import io.github.frankli.mcp.contextcore.domain.entity.Log;
import io.github.frankli.mcp.contextcore.domain.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Use Case: List Log Summaries
 *
 * Lists log summaries (without full content) with optional filters
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListLogSummariesUseCase {

    private final LogRepository logRepository;

    /**
     * Executes the list log summaries use case
     *
     * @param limit maximum number of results
     * @param tags filter by tags
     * @param module filter by module
     * @param type filter by type
     * @return flux of logs (full objects, but can be transformed to summaries in controller)
     */
    public Flux<Log> execute(Integer limit, List<String> tags, String module, Log.LogType type) {
        log.info("Listing log summaries: limit={}, tags={}, module={}, type={}",
                limit, tags, module, type);

        int searchLimit = limit != null && limit > 0 ? limit : 100;

        return logRepository.findAll(tags, module, type, searchLimit)
                .doOnComplete(() -> log.info("Log summaries listing completed"))
                .doOnError(error -> log.error("Failed to list log summaries", error));
    }
}
