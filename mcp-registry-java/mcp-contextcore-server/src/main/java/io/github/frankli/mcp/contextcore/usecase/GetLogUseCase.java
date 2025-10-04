package io.github.frankli.mcp.contextcore.usecase;

import io.github.frankli.mcp.contextcore.domain.entity.Log;
import io.github.frankli.mcp.contextcore.domain.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use Case: Get Log
 *
 * Retrieves a specific log by ID
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetLogUseCase {

    private final LogRepository logRepository;

    /**
     * Executes the get log use case
     *
     * @param id the log ID
     * @return the log if found
     */
    public Mono<Log> execute(String id) {
        log.info("Getting log: id={}", id);

        return logRepository.findById(id)
                .doOnSuccess(log -> {
                    if (log != null) {
                        log.info("Log retrieved: id={}", id);
                    } else {
                        log.warn("Log not found: id={}", id);
                    }
                })
                .doOnError(error -> log.error("Failed to get log: id={}", id, error));
    }
}
