package com.mcp.contextcore.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.contextcore.domain.entity.Log;
import com.mcp.contextcore.domain.entity.LogSearchResult;
import com.mcp.contextcore.usecase.*;
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
 * ContextCore MCP Tools
 *
 * MCP tools for managing development logs with semantic search capabilities.
 * These tools are exposed to LLM agents via the Model Context Protocol.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextCoreTools {

    private final AddLogUseCase addLogUseCase;
    private final SearchLogsUseCase searchLogsUseCase;
    private final GetLogUseCase getLogUseCase;
    private final ListLogSummariesUseCase listLogSummariesUseCase;
    private final GetProjectContextUseCase getProjectContextUseCase;
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

    /**
     * Searches development logs using semantic similarity.
     *
     * @param query  Natural language search query
     * @param limit  Maximum number of results to return (default: 10)
     * @param tags   Optional comma-separated tags to filter by
     * @param module Optional module name to filter by
     * @param type   Optional log type to filter by
     * @return JSON array of matching log entries with similarity scores
     */
    @Tool(description = "Search development logs using semantic similarity. " +
                       "Returns logs ranked by relevance to the query. " +
                       "Parameters: query (required), limit (optional, default 10), " +
                       "tags (optional, comma-separated), module (optional), type (optional)")
    public String searchLogs(String query, Integer limit, String tags, String module, String type) {
        try {
            log.info("MCP Tool: searchLogs - query={}", query);

            List<String> tagsList = tags != null && !tags.isBlank()
                    ? List.of(tags.split(",")).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList())
                    : null;

            int resultLimit = limit != null ? limit : 10;

            List<LogSearchResult> results = searchLogsUseCase
                    .execute(query, resultLimit, tagsList, module, type)
                    .collectList()
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(Duration.ofSeconds(30));

            if (results == null) {
                return "[]";
            }

            List<Map<String, Object>> jsonResults = results.stream()
                    .map(result -> Map.of(
                            "id", result.getLog().getId(),
                            "title", result.getLog().getTitle(),
                            "content", result.getLog().getContent(),
                            "tags", result.getLog().getTags() != null ? result.getLog().getTags() : List.of(),
                            "module", result.getLog().getModule() != null ? result.getLog().getModule() : "",
                            "type", result.getLog().getType().getValue(),
                            "score", result.getScore(),
                            "createdAt", result.getLog().getCreatedAt().toString()
                    ))
                    .collect(Collectors.toList());

            return objectMapper.writeValueAsString(jsonResults);
        } catch (Exception e) {
            log.error("Error searching logs", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

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
                    .map(log -> Map.of(
                            "id", log.getId(),
                            "title", log.getTitle(),
                            "content", log.getContent(),
                            "tags", log.getTags() != null ? log.getTags() : List.of(),
                            "module", log.getModule() != null ? log.getModule() : "",
                            "type", log.getType().getValue(),
                            "createdAt", log.getCreatedAt().toString()
                    ))
                    .collect(Collectors.toList());

            return objectMapper.writeValueAsString(jsonResults);
        } catch (Exception e) {
            log.error("Error listing logs", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

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
