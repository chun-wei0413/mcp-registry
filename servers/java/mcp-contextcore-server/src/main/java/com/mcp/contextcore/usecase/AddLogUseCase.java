package com.mcp.contextcore.usecase;

import com.mcp.contextcore.domain.entity.Log;
import com.mcp.contextcore.domain.repository.EmbeddingService;
import com.mcp.contextcore.domain.repository.LogRepository;
import com.mcp.contextcore.domain.repository.VectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Add Log Use Case
 *
 * Business logic for adding a new log entry with vector embedding.
 * This use case orchestrates the following operations:
 * 1. Validates and creates the log entity
 * 2. Persists the log to SQLite
 * 3. Generates embedding vector
 * 4. Stores the vector in Qdrant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddLogUseCase {

    private final LogRepository logRepository;
    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;

    /**
     * Executes the add log use case
     *
     * @param title the log title
     * @param content the log content
     * @param tags the log tags
     * @param module the log module
     * @param type the log type
     * @return the created log
     */
    public Mono<Log> execute(String title, String content, List<String> tags,
                            String module, Log.LogType type) {
        log.info("Adding new log: title={}, module={}, type={}", title, module, type);

        // 1. Create and validate log entity
        Log logEntity = Log.create(title, content, tags, module, type);

        try {
            logEntity.validate();
        } catch (IllegalArgumentException e) {
            log.error("Log validation failed: {}", e.getMessage());
            return Mono.error(e);
        }

        // 2. Save log to repository
        return logRepository.save(logEntity)
                .flatMap(savedLog -> {
                    log.debug("Log saved successfully: {}", savedLog.getId());

                    // 3. Generate embedding for full text
                    return embeddingService.embed(savedLog.getFullText())
                            .flatMap(embedding -> {
                                log.debug("Embedding generated for log: {}", savedLog.getId());

                                // 4. Store vector in Qdrant
                                return vectorRepository.storeVector(
                                        savedLog.getId(),
                                        embedding,
                                        savedLog.getTags(),
                                        savedLog.getModule(),
                                        savedLog.getType().getValue()
                                ).thenReturn(savedLog);
                            });
                })
                .doOnSuccess(savedLog -> log.info("Successfully added log: {}", savedLog.getId()))
                .doOnError(error -> log.error("Failed to add log: {}", error.getMessage()));
    }
}
