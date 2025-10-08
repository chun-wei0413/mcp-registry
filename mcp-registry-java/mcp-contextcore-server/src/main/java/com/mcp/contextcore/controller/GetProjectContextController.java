package com.mcp.contextcore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.contextcore.usecase.GetProjectContextUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP Controller for retrieving project-wide context statistics.
 * Exposes the getProjectContext tool to LLM agents via Model Context Protocol.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetProjectContextController {

    private final GetProjectContextUseCase getProjectContextUseCase;
    private final ObjectMapper objectMapper;

    /**
     * Gets project-wide context statistics.
     *
     * @return JSON string containing statistics about logs, modules, and types
     */
    @Tool(description = "Get project-wide context and statistics including total logs, " +
                       "logs by module, and counts by log type. No parameters required.")
    public String getProjectContext() {
        try {
            log.info("MCP Tool: getProjectContext");

            GetProjectContextUseCase.ProjectContext context = getProjectContextUseCase
                    .execute()
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(Duration.ofSeconds(30));

            if (context == null) {
                return "{\"error\": \"Failed to get project context\"}";
            }

            Map<String, Integer> moduleCounts = context.logsByModule().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().size()
                    ));

            return objectMapper.writeValueAsString(Map.of(
                    "totalLogs", context.totalLogs(),
                    "logsByModule", moduleCounts,
                    "featureCount", context.featureCount(),
                    "bugCount", context.bugCount(),
                    "decisionCount", context.decisionCount(),
                    "noteCount", context.noteCount()
            ));
        } catch (Exception e) {
            log.error("Error getting project context", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
