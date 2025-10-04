package io.github.frankli.mcp.contextcore.controller;

import io.github.frankli.mcp.contextcore.controller.dto.*;
import io.github.frankli.mcp.contextcore.domain.entity.Log;
import io.github.frankli.mcp.contextcore.usecase.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * MCP Controller for ContextCore
 *
 * Provides MCP Tools for development log management
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp/contextcore")
@RequiredArgsConstructor
public class ContextCoreMCPController {

    private final AddLogUseCase addLogUseCase;
    private final SearchLogsUseCase searchLogsUseCase;
    private final GetLogUseCase getLogUseCase;
    private final ListLogSummariesUseCase listLogSummariesUseCase;
    private final GetProjectContextUseCase getProjectContextUseCase;

    /**
     * MCP Tool: add_log
     *
     * Adds a new development log entry
     */
    @PostMapping("/logs")
    public Mono<LogResponse> addLog(@RequestBody AddLogRequest request) {
        log.info("MCP Tool: add_log - title={}", request.getTitle());

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
     * MCP Tool: search_logs
     *
     * Performs semantic search on logs
     */
    @PostMapping("/logs/search")
    public Flux<SearchResultResponse> searchLogs(@RequestBody SearchLogsRequest request) {
        log.info("MCP Tool: search_logs - query={}", request.getQuery());

        Instant dateFrom = request.getDateFrom() != null ?
                Instant.parse(request.getDateFrom()) : null;
        Instant dateTo = request.getDateTo() != null ?
                Instant.parse(request.getDateTo()) : null;

        return searchLogsUseCase.execute(
                request.getQuery(),
                request.getLimit(),
                request.getTags(),
                request.getModule(),
                request.getType(),
                dateFrom,
                dateTo
        ).map(SearchResultResponse::from);
    }

    /**
     * MCP Tool: get_log
     *
     * Retrieves a specific log by ID
     */
    @GetMapping("/logs/{id}")
    public Mono<LogResponse> getLog(@PathVariable String id) {
        log.info("MCP Tool: get_log - id={}", id);

        return getLogUseCase.execute(id)
                .map(LogResponse::from);
    }

    /**
     * MCP Tool: list_log_summaries
     *
     * Lists log summaries with optional filters
     */
    @GetMapping("/logs/summaries")
    public Flux<LogResponse> listLogSummaries(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String type) {

        log.info("MCP Tool: list_log_summaries - limit={}, tags={}, module={}, type={}",
                limit, tags, module, type);

        Log.LogType logType = type != null ? Log.LogType.fromValue(type) : null;

        return listLogSummariesUseCase.execute(limit, tags, module, logType)
                .map(LogResponse::from);
    }

    /**
     * MCP Tool: get_project_context
     *
     * Retrieves important project context (decisions and recent logs)
     */
    @GetMapping("/context")
    public Flux<LogResponse> getProjectContext(
            @RequestParam(required = false) List<String> modules,
            @RequestParam(required = false) Integer limit) {

        log.info("MCP Tool: get_project_context - modules={}, limit={}", modules, limit);

        return getProjectContextUseCase.execute(modules, limit)
                .map(LogResponse::from);
    }
}
