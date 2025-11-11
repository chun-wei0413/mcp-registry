package com.mcp.contextcore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.contextcore.domain.entity.Log;
import com.mcp.contextcore.usecase.ListLogSummariesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP Controller for listing development log summaries with optional filtering.
 * Exposes the listLogs tool to LLM agents via Model Context Protocol.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListLogsController {

    private final ListLogSummariesUseCase listLogSummariesUseCase;
    private final ObjectMapper objectMapper;

    /**
     * Lists development log summaries with optional filtering.
     *
     * @param tags   Optional comma-separated tags to filter by
     * @param module Optional module name to filter by
     * @param type   Optional log type to filter by
     * @param limit  Maximum number of results (default: 50)
     * @return JSON array of log summaries
     */
    @Tool(description = "List development log summaries with optional filtering by tags, module, or type. " +
                       "Parameters: tags (optional, comma-separated), module (optional), " +
                       "type (optional), limit (optional, default 50)")
    public String listLogs(String tags, String module, String type, Integer limit) {
        try {
            log.info("MCP Tool: listLogs - tags={}, module={}, type={}", tags, module, type);

            List<String> tagsList = tags != null && !tags.isBlank()
                    ? List.of(tags.split(",")).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList())
                    : null;

            int resultLimit = limit != null ? limit : 50;

            List<Log> results = listLogSummariesUseCase
                    .execute(tagsList, module, type, resultLimit)
                    .collectList()
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(Duration.ofSeconds(30));

            if (results == null) {
                return "[]";
            }

            List<Map<String, Object>> jsonResults = results.stream()
                    .map(logEntry -> Map.of(
                            "id", logEntry.getId(),
                            "title", logEntry.getTitle(),
                            "content", logEntry.getContent(),
                            "tags", logEntry.getTags() != null ? logEntry.getTags() : List.of(),
                            "module", logEntry.getModule() != null ? logEntry.getModule() : "",
                            "type", logEntry.getType().getValue(),
                            "createdAt", logEntry.getCreatedAt().toString()
                    ))
                    .collect(Collectors.toList());

            return objectMapper.writeValueAsString(jsonResults);
        } catch (Exception e) {
            log.error("Error listing logs", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
