package com.mcp.contextcore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.contextcore.domain.entity.Log;
import com.mcp.contextcore.usecase.GetLogUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * MCP Controller for retrieving a specific development log by ID.
 * Exposes the getLog tool to LLM agents via Model Context Protocol.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetLogController {

    private final GetLogUseCase getLogUseCase;
    private final ObjectMapper objectMapper;

    /**
     * Retrieves a specific log entry by its ID.
     *
     * @param id The unique identifier of the log entry
     * @return JSON string containing the complete log entry
     */
    @Tool(description = "Get a specific development log entry by its ID. " +
                       "Parameters: id (required)")
    public String getLog(String id) {
        try {
            log.info("MCP Tool: getLog - id={}", id);

            Log result = getLogUseCase.execute(id)
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(Duration.ofSeconds(30));

            if (result == null) {
                return "{\"error\": \"Log not found\"}";
            }

            return objectMapper.writeValueAsString(Map.of(
                    "id", result.getId(),
                    "title", result.getTitle(),
                    "content", result.getContent(),
                    "tags", result.getTags() != null ? result.getTags() : List.of(),
                    "module", result.getModule() != null ? result.getModule() : "",
                    "type", result.getType().getValue(),
                    "createdAt", result.getCreatedAt().toString()
            ));
        } catch (Exception e) {
            log.error("Error getting log", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
