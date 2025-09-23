package com.mcp.postgresql.controller;

import com.mcp.common.model.QueryRequest;
import com.mcp.common.model.QueryResult;
import com.mcp.postgresql.service.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.server.annotation.McpTool;
import org.springframework.ai.mcp.server.annotation.ToolFunction;
import org.springframework.ai.mcp.server.annotation.ToolParameter;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 查詢執行 MCP 工具控制器
 */
@Controller
@McpTool
@RequiredArgsConstructor
@Slf4j
public class QueryController {

    private final QueryService queryService;

    /**
     * 執行 SQL 查詢
     */
    @ToolFunction(
        name = "execute_query",
        description = "執行 PostgreSQL SQL 查詢"
    )
    public Mono<Map<String, Object>> executeQuery(
            @ToolParameter(name = "connection_id", description = "連線識別碼")
            String connectionId,

            @ToolParameter(name = "query", description = "SQL 查詢語句")
            String query,

            @ToolParameter(name = "params", description = "查詢參數", required = false)
            List<Object> params) {

        log.info("Executing PostgreSQL query on connection: {}", connectionId);
        log.debug("Query: {}", query);

        QueryRequest request = QueryRequest.builder()
                .connectionId(connectionId)
                .query(query)
                .params(params)
                .build();

        return queryService.executeQuery(request)
                .map(this::mapQueryResult)
                .onErrorResume(error -> {
                    log.error("Query execution failed", error);
                    return Mono.just(Map.of(
                        "success", false,
                        "error_message", error.getMessage(),
                        "connection_id", connectionId
                    ));
                });
    }

    /**
     * 執行事務
     */
    @ToolFunction(
        name = "execute_transaction",
        description = "在 PostgreSQL 事務中執行多個查詢"
    )
    public Mono<Map<String, Object>> executeTransaction(
            @ToolParameter(name = "connection_id", description = "連線識別碼")
            String connectionId,

            @ToolParameter(name = "queries", description = "查詢列表")
            List<Map<String, Object>> queries) {

        log.info("Executing PostgreSQL transaction on connection: {}", connectionId);

        return queryService.executeTransaction(connectionId, queries)
                .map(results -> Map.of(
                    "success", true,
                    "connection_id", connectionId,
                    "results", results,
                    "queries_executed", results.size()
                ))
                .onErrorResume(error -> {
                    log.error("Transaction execution failed", error);
                    return Mono.just(Map.of(
                        "success", false,
                        "error_message", error.getMessage(),
                        "connection_id", connectionId,
                        "rolled_back", true
                    ));
                });
    }

    /**
     * 批次執行查詢
     */
    @ToolFunction(
        name = "execute_batch",
        description = "批次執行相同的 PostgreSQL 查詢，使用不同參數"
    )
    public Mono<Map<String, Object>> executeBatch(
            @ToolParameter(name = "connection_id", description = "連線識別碼")
            String connectionId,

            @ToolParameter(name = "query", description = "SQL 查詢語句")
            String query,

            @ToolParameter(name = "params_list", description = "參數列表")
            List<List<Object>> paramsList) {

        log.info("Executing PostgreSQL batch query on connection: {}", connectionId);
        log.debug("Query: {}, Batch size: {}", query, paramsList.size());

        return queryService.executeBatch(connectionId, query, paramsList)
                .map(results -> Map.of(
                    "success", true,
                    "connection_id", connectionId,
                    "results", results,
                    "batch_size", paramsList.size(),
                    "executed_count", results.size()
                ))
                .onErrorResume(error -> {
                    log.error("Batch execution failed", error);
                    return Mono.just(Map.of(
                        "success", false,
                        "error_message", error.getMessage(),
                        "connection_id", connectionId
                    ));
                });
    }

    /**
     * 解釋查詢執行計畫
     */
    @ToolFunction(
        name = "explain_query",
        description = "分析 PostgreSQL 查詢執行計畫"
    )
    public Mono<Map<String, Object>> explainQuery(
            @ToolParameter(name = "connection_id", description = "連線識別碼")
            String connectionId,

            @ToolParameter(name = "query", description = "SQL 查詢語句")
            String query,

            @ToolParameter(name = "analyze", description = "是否執行分析", required = false)
            Boolean analyze) {

        log.info("Explaining PostgreSQL query on connection: {}", connectionId);

        QueryRequest request = QueryRequest.builder()
                .connectionId(connectionId)
                .query(query)
                .explain(true)
                .build();

        return queryService.explainQuery(request, analyze != null ? analyze : false)
                .map(result -> Map.of(
                    "success", true,
                    "connection_id", connectionId,
                    "execution_plan", result.getExecutionPlan(),
                    "analyzed", analyze != null ? analyze : false
                ))
                .onErrorResume(error -> {
                    log.error("Explain query failed", error);
                    return Mono.just(Map.of(
                        "success", false,
                        "error_message", error.getMessage(),
                        "connection_id", connectionId
                    ));
                });
    }

    /**
     * 將 QueryResult 轉換為 Map
     */
    private Map<String, Object> mapQueryResult(QueryResult result) {
        Map<String, Object> response = Map.of(
            "success", result.getSuccess(),
            "row_count", result.getRowCount(),
            "rows", result.getRows() != null ? result.getRows() : List.of(),
            "columns", result.getColumns() != null ? result.getColumns() : List.of(),
            "execution_time_ms", result.getExecutionTimeMs(),
            "start_time", result.getStartTime().toString()
        );

        if (result.getErrorMessage() != null) {
            response = Map.of(
                "success", false,
                "error_message", result.getErrorMessage(),
                "execution_time_ms", result.getExecutionTimeMs()
            );
        }

        if (result.getExecutionPlan() != null) {
            response = Map.of(
                "success", result.getSuccess(),
                "execution_plan", result.getExecutionPlan(),
                "execution_time_ms", result.getExecutionTimeMs()
            );
        }

        return response;
    }
}