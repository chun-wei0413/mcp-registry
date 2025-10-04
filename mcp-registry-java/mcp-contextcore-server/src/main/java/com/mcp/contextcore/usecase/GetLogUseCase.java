package com.mcp.contextcore.usecase;

import com.mcp.contextcore.domain.entity.Log;
import com.mcp.contextcore.domain.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Get Log Use Case
 *
 * Business logic for retrieving a specific log entry by ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetLogUseCase {

    private final LogRepository logRepository;

    /**
     * Executes the get log use case
     *
     * @param logId the log ID
     * @return the log if found
     */
    public Mono<Log> execute(String logId) {
        log.info("Getting log: {}", logId);

        if (logId == null || logId.trim().isEmpty()) {
            log.error("Log ID cannot be empty");
            return Mono.error(new IllegalArgumentException("Log ID cannot be empty"));
        }

        return logRepository.findById(logId)
                .doOnSuccess(log -> {
                    if (log != null) {
                        log.debug("Successfully retrieved log: {}", logId);
                    } else {
                        log.debug("Log not found: {}", logId);
                    }
                })
                .doOnError(error -> log.error("Failed to get log {}: {}", logId, error.getMessage()));
    }
}
