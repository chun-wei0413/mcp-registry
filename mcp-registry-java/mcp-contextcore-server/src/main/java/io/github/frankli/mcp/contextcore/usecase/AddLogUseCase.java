package io.github.frankli.mcp.contextcore.usecase;

import io.github.frankli.mcp.contextcore.domain.entity.Log;
import io.github.frankli.mcp.contextcore.domain.repository.EmbeddingService;
import io.github.frankli.mcp.contextcore.domain.repository.LogRepository;
import io.github.frankli.mcp.contextcore.domain.repository.VectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Use Case: Add Log
 *
 * Adds a new log entry to both SQLite and Qdrant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddLogUseCase {

    private final LogRepository logRepository;
    private final VectorRepository vectorRepository;
    private final EmbeddingService embeddingService;

    /**
     * Executes the add log use case
     *
     * @param title log title
     * @param content log content
     * @param tags log tags
     * @param module log module
     * @param type log type
     * @return the created log with ID
     */
    public Mono<Log> execute(String title, String content, List<String> tags,
                             String module, Log.LogType type) {

        log.info("Adding new log: title={}, module={}, type={}", title, module, type);

        // 1. Create and validate log entity
        Log newLog = Log.create(title, content, tags, module, type);
        newLog.validate();

        // 2. Save to SQLite
        return logRepository.save(newLog)
                .flatMap(savedLog -> {
                    // 3. Generate embedding
                    return embeddingService.embed(savedLog.getFullText())
                            .flatMap(vector -> {
                                // 4. Store vector in Qdrant
                                return vectorRepository.storeVector(
                                        savedLog.getId(),
                                        vector,
                                        savedLog.getTags(),
                                        savedLog.getModule(),
                                        savedLog.getType() != null ? savedLog.getType().getValue() : null
                                ).thenReturn(savedLog);
                            });
                })
                .doOnSuccess(log -> log.info("Log added successfully: id={}", log.getId()))
                .doOnError(error -> log.error("Failed to add log: title={}", title, error));
    }
}
