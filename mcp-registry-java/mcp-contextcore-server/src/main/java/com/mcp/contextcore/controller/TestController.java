package com.mcp.contextcore.controller;

import com.mcp.contextcore.mcp.ContextCoreTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * Test Controller for verifying MCP Tools functionality
 *
 * This controller provides HTTP endpoints to test the MCP Tools directly.
 * It should only be used for development and testing purposes.
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final ContextCoreTools contextCoreTools;

    /**
     * Test endpoint to verify server is running
     */
    @GetMapping("/ping")
    public Mono<Map<String, String>> ping() {
        return Mono.just(Map.of(
            "status", "ok",
            "message", "ContextCore MCP Server is running",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    /**
     * Test the addLog tool
     */
    @PostMapping("/add-log")
    public Mono<String> testAddLog(@RequestBody AddLogRequest request) {
        log.info("Testing addLog tool: title={}", request.title());

        return Mono.fromCallable(() ->
            contextCoreTools.addLog(
                request.title(),
                request.content(),
                request.tags(),
                request.module(),
                request.type()
            )
        ).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Test the searchLogs tool
     */
    @PostMapping("/search-logs")
    public Mono<String> testSearchLogs(@RequestBody SearchLogsRequest request) {
        log.info("Testing searchLogs tool: query={}", request.query());

        return Mono.fromCallable(() ->
            contextCoreTools.searchLogs(
                request.query(),
                request.limit(),
                request.tags(),
                request.module(),
                request.type()
            )
        ).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Test the getLog tool
     */
    @GetMapping("/get-log/{logId}")
    public Mono<String> testGetLog(@PathVariable String logId) {
        log.info("Testing getLog tool: logId={}", logId);

        return Mono.fromCallable(() ->
            contextCoreTools.getLog(logId)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Test the listLogs tool
     */
    @GetMapping("/list-logs")
    public Mono<String> testListLogs(
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("Testing listLogs tool: limit={}", limit);

        return Mono.fromCallable(() ->
            contextCoreTools.listLogs(tags, module, type, limit)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Test the getProjectContext tool
     */
    @GetMapping("/project-context")
    public Mono<String> testGetProjectContext() {
        log.info("Testing getProjectContext tool");

        return Mono.fromCallable(() ->
            contextCoreTools.getProjectContext()
        ).subscribeOn(Schedulers.boundedElastic());
    }

    // Request DTOs
    public record AddLogRequest(
        String title,
        String content,
        String tags,
        String module,
        String type
    ) {}

    public record SearchLogsRequest(
        String query,
        Integer limit,
        String tags,
        String module,
        String type
    ) {}
}
