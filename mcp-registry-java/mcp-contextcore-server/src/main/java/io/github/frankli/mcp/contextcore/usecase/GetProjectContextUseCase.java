package io.github.frankli.mcp.contextcore.usecase;

import io.github.frankli.mcp.contextcore.domain.entity.Log;
import io.github.frankli.mcp.contextcore.domain.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;

/**
 * Use Case: Get Project Context
 *
 * Retrieves important project context (decisions and recent logs)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetProjectContextUseCase {

    private final LogRepository logRepository;

    /**
     * Executes the get project context use case
     *
     * @param modules filter by modules (null for all)
     * @param limit maximum number of results
     * @return flux of important logs (decisions first, then recent)
     */
    public Flux<Log> execute(List<String> modules, Integer limit) {
        log.info("Getting project context: modules={}, limit={}", modules, limit);

        int searchLimit = limit != null && limit > 0 ? limit : 10;

        Flux<Log> logsFlux;

        if (modules != null && !modules.isEmpty()) {
            // Get logs from all specified modules
            logsFlux = Flux.fromIterable(modules)
                    .flatMap(module -> logRepository.findAll(null, module, null, searchLimit * 2));
        } else {
            // Get all logs
            logsFlux = logRepository.findAll(null, null, null, searchLimit * 2);
        }

        return logsFlux
                .collectList()
                .flatMapMany(logs -> {
                    // Sort: DECISION type first, then by timestamp (newest first)
                    logs.sort(Comparator
                            .comparing((Log log) -> log.getType() == Log.LogType.DECISION ? 0 : 1)
                            .thenComparing(Log::getTimestamp, Comparator.reverseOrder()));

                    // Take only the limit
                    return Flux.fromIterable(logs.stream()
                            .limit(searchLimit)
                            .toList());
                })
                .doOnComplete(() -> log.info("Project context retrieval completed"))
                .doOnError(error -> log.error("Failed to get project context", error));
    }
}
