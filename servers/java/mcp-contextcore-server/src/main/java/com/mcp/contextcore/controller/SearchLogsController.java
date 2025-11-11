package com.mcp.contextcore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.contextcore.domain.entity.LogSearchResult;
import com.mcp.contextcore.usecase.SearchLogsUseCase;
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
 * MCP Controller for searching development logs using semantic similarity.
 * Exposes the searchLogs tool to LLM agents via Model Context Protocol.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogsController {

    private final SearchLogsUseCase searchLogsUseCase;
    private final ObjectMapper objectMapper;

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
}
