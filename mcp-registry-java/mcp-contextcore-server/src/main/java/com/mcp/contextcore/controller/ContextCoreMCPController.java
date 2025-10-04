package com.mcp.contextcore.controller;

import com.mcp.contextcore.controller.dto.*;
import com.mcp.contextcore.domain.entity.Log;
import com.mcp.contextcore.usecase.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * ContextCore MCP Controller
 *
 * REST API controller for ContextCore MCP Server.
 * Provides endpoints for managing development logs with semantic search.
 */
@Slf4j
@RestController
@RequestMapping("/api/contextcore")
@RequiredArgsConstructor
public class ContextCoreMCPController {

    private final AddLogUseCase addLogUseCase;
    private final SearchLogsUseCase searchLogsUseCase;
    private final GetLogUseCase getLogUseCase;
    private final ListLogSummariesUseCase listLogSummariesUseCase;
    private final GetProjectContextUseCase getProjectContextUseCase;

    /**
     * Adds a new log entry
     *
     * POST /api/contextcore/logs
     */
    @PostMapping("/logs")
    public Mono<LogResponse> addLog(@RequestBody AddLogRequest request) {
        log.info("Adding new log: {}", request.getTitle());

        Log.LogType logType = request.getType() != null ?
                Log.LogType.fromValue(request.getType()) : Log.LogType.NOTE;

        return addLogUseCase.execute(
                request.getTitle(),
                request.getContent(),
                request.getTags(),
                request.getModule(),
                logType
        ).map(LogResponse::from);
    }

    /**
     * Searches logs by semantic similarity
     *
     * POST /api/contextcore/logs/search
     */
    @PostMapping("/logs/search")
    public Flux<SearchResultResponse> searchLogs(@RequestBody SearchLogsRequest request) {
        log.info("Searching logs: {}", request.getQuery());

        int limit = request.getLimit() != null ? request.getLimit() : 10;

        return searchLogsUseCase.execute(
                request.getQuery(),
                limit,
                request.getTags(),
                request.getModule(),
                request.getType()
        ).map(SearchResultResponse::from);
    }

    /**
     * Gets a specific log by ID
     *
     * GET /api/contextcore/logs/{id}
     */
    @GetMapping("/logs/{id}")
    public Mono<LogResponse> getLog(@PathVariable String id) {
        log.info("Getting log: {}", id);

        return getLogUseCase.execute(id)
                .map(LogResponse::from);
    }

    /**
     * Lists log summaries with optional filters
     *
     * GET /api/contextcore/logs
     */
    @GetMapping("/logs")
    public Flux<LogResponse> listLogs(
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        log.info("Listing logs: tags={}, module={}, type={}, limit={}", tags, module, type, limit);

        return listLogSummariesUseCase.execute(tags, module, type, limit)
                .map(LogResponse::from);
    }

    /**
     * Gets project context summary
     *
     * GET /api/contextcore/context
     */
    @GetMapping("/context")
    public Mono<ProjectContextResponse> getProjectContext() {
        log.info("Getting project context");

        return getProjectContextUseCase.execute()
                .map(ProjectContextResponse::from);
    }

    /**
     * Project Context Response DTO
     */
    public record ProjectContextResponse(
            int totalLogs,
            java.util.Map<String, Integer> logsByModule,
            long featureCount,
            long bugCount,
            long decisionCount,
            long noteCount
    ) {
        public static ProjectContextResponse from(GetProjectContextUseCase.ProjectContext context) {
            // Convert logs by module to counts
            java.util.Map<String, Integer> moduleCounts = context.logsByModule().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            java.util.Map.Entry::getKey,
                            e -> e.getValue().size()
                    ));

            return new ProjectContextResponse(
                    context.totalLogs(),
                    moduleCounts,
                    context.featureCount(),
                    context.bugCount(),
                    context.decisionCount(),
                    context.noteCount()
            );
        }
    }
}
