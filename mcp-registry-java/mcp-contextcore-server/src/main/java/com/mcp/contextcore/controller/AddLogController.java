package com.mcp.contextcore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.contextcore.domain.entity.Log;
import com.mcp.contextcore.usecase.AddLogUseCase;
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
 * MCP Controller for adding development logs.
 * Exposes the addLog tool to LLM agents via Model Context Protocol.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddLogController {

    private final AddLogUseCase addLogUseCase;
    private final ObjectMapper objectMapper;

    /**
     * Adds a new development log entry with optional metadata.
     *
     * @param title   The title/subject of the log entry
     * @param content The detailed content of the log entry
     * @param tags    Optional comma-separated tags for categorization
     * @param module  Optional module/component name this log relates to
     * @param type    Log type: FEATURE, BUG, DECISION, or NOTE (default: NOTE)
     * @return JSON string containing the created log with ID and metadata
     */
    @Tool(description = "Add a new development log entry with semantic indexing. " +
                       "Use this to record features, bugs, decisions, or general notes during development. " +
                       "Parameters: title (required), content (required), tags (optional, comma-separated), " +
                       "module (optional), type (optional: FEATURE, BUG, DECISION, NOTE)")
    public String addLog(String title, String content, String tags, String module, String type) {
        try {
            log.info("MCP Tool: addLog - title={}", title);

            List<String> tagsList = tags != null && !tags.isBlank()
                    ? List.of(tags.split(",")).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList())
                    : null;

            Log.LogType logType = type != null && !type.isBlank()
                    ? Log.LogType.fromValue(type)
                    : Log.LogType.NOTE;

            Log result = addLogUseCase.execute(title, content, tagsList, module, logType)
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(Duration.ofSeconds(30));

            if (result == null) {
                return "{\"error\": \"Failed to create log - timeout or null result\"}";
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
            log.error("Error adding log", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
