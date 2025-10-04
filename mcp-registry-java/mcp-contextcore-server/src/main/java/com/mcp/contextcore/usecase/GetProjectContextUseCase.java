package com.mcp.contextcore.usecase;

import com.mcp.contextcore.domain.entity.Log;
import com.mcp.contextcore.domain.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Get Project Context Use Case
 *
 * Business logic for retrieving project context.
 * Returns a summary of all logs grouped by module.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetProjectContextUseCase {

    private final LogRepository logRepository;

    /**
     * Executes the get project context use case
     *
     * @return project context summary
     */
    public Mono<ProjectContext> execute() {
        log.info("Getting project context");

        return logRepository.findAll(null, null, null, null)
                .collectList()
                .map(logs -> {
                    log.debug("Building project context from {} logs", logs.size());
                    return buildProjectContext(logs);
                })
                .doOnSuccess(context -> log.info("Successfully built project context"))
                .doOnError(error -> log.error("Failed to get project context: {}", error.getMessage()));
    }

    /**
     * Builds project context from logs
     */
    private ProjectContext buildProjectContext(java.util.List<Log> logs) {
        // Group logs by module
        java.util.Map<String, java.util.List<Log>> logsByModule = logs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        log -> log.getModule() != null ? log.getModule() : "uncategorized"
                ));

        // Count by type
        long featureCount = logs.stream().filter(l -> l.getType() == Log.LogType.FEATURE).count();
        long bugCount = logs.stream().filter(l -> l.getType() == Log.LogType.BUG).count();
        long decisionCount = logs.stream().filter(l -> l.getType() == Log.LogType.DECISION).count();
        long noteCount = logs.stream().filter(l -> l.getType() == Log.LogType.NOTE).count();

        return new ProjectContext(
                logs.size(),
                logsByModule,
                featureCount,
                bugCount,
                decisionCount,
                noteCount
        );
    }

    /**
     * Project Context DTO
     */
    public record ProjectContext(
            int totalLogs,
            java.util.Map<String, java.util.List<Log>> logsByModule,
            long featureCount,
            long bugCount,
            long decisionCount,
            long noteCount
    ) {}
}
