package com.mcp.contextcore.usecase;

import com.mcp.contextcore.domain.entity.Log;
import com.mcp.contextcore.domain.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * List Log Summaries Use Case
 *
 * Business logic for listing log summaries with optional filters.
 * Returns logs sorted by timestamp (newest first).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListLogSummariesUseCase {

    private final LogRepository logRepository;

    /**
     * Executes the list log summaries use case
     *
     * @param tags filter by tags (null for no filter)
     * @param module filter by module (null for no filter)
     * @param type filter by type (null for no filter)
     * @param limit maximum number of results (default 50)
     * @return list of logs
     */
    public Flux<Log> execute(List<String> tags, String module, String type, Integer limit) {
        log.info("Listing log summaries: tags={}, module={}, type={}, limit={}",
                tags, module, type, limit);

        int resultLimit = (limit != null && limit > 0) ? limit : 50;

        Log.LogType logType = null;
        if (type != null && !type.trim().isEmpty()) {
            logType = Log.LogType.fromValue(type);
        }

        return logRepository.findAll(tags, module, logType, resultLimit)
                .doOnComplete(() -> log.info("Listed log summaries"))
                .doOnError(error -> log.error("Failed to list logs: {}", error.getMessage()));
    }
}
