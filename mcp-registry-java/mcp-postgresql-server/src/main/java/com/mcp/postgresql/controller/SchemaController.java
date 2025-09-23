package com.mcp.postgresql.controller;

import com.mcp.postgresql.service.SchemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.server.annotation.McpTool;
import org.springframework.ai.mcp.server.annotation.ToolFunction;
import org.springframework.ai.mcp.server.annotation.ToolParameter;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Schema 管理 MCP 工具控制器
 */
@Controller
@McpTool
@RequiredArgsConstructor
@Slf4j
public class SchemaController {

    private final SchemaService schemaService;

    /**
     * 取得表結構詳情
     */
    @ToolFunction(
        name = "get_table_schema",
        description = "取得 PostgreSQL 表結構詳情"
    )
    public Mono<Map<String, Object>> getTableSchema(
            @ToolParameter(name = "connection_id", description = "連線識別碼")
            String connectionId,

            @ToolParameter(name = "table_name", description = "表名稱")
            String tableName,

            @ToolParameter(name = "schema_name", description = "Schema 名稱", required = false)
            String schemaName) {

        log.info("Getting PostgreSQL table schema: {}.{} on connection: {}",
                schemaName != null ? schemaName : "public", tableName, connectionId);

        return schemaService.getTableSchema(connectionId, tableName, schemaName)
                .map(schema -> Map.of(
                    "success", true,
                    "connection_id", connectionId,
                    "table_name", tableName,
                    "schema_name", schemaName != null ? schemaName : "public",
                    "schema", schema
                ))
                .onErrorResume(error -> {
                    log.error("Failed to get table schema", error);
                    return Mono.just(Map.of(
                        "success", false,
                        "error_message", error.getMessage(),
                        "connection_id", connectionId,
                        "table_name", tableName
                    ));
                });
    }

    /**
     * 列出所有表
     */
    @ToolFunction(
        name = "list_tables",
        description = "列出 PostgreSQL 資料庫中的所有表"
    )
    public Mono<Map<String, Object>> listTables(
            @ToolParameter(name = "connection_id", description = "連線識別碼")
            String connectionId,

            @ToolParameter(name = "schema_name", description = "Schema 名稱", required = false)
            String schemaName) {

        log.info("Listing PostgreSQL tables in schema: {} on connection: {}",
                schemaName != null ? schemaName : "public", connectionId);

        return schemaService.listTables(connectionId, schemaName)
                .collectList()
                .map(tables -> Map.of(
                    "success", true,
                    "connection_id", connectionId,
                    "schema_name", schemaName != null ? schemaName : "public",
                    "tables", tables,
                    "count", tables.size()
                ))
                .onErrorResume(error -> {
                    log.error("Failed to list tables", error);
                    return Mono.just(Map.of(
                        "success", false,
                        "error_message", error.getMessage(),
                        "connection_id", connectionId
                    ));
                });
    }

    /**
     * 列出所有 Schema
     */
    @ToolFunction(
        name = "list_schemas",
        description = "列出 PostgreSQL 資料庫中的所有 Schema"
    )
    public Mono<Map<String, Object>> listSchemas(
            @ToolParameter(name = "connection_id", description = "連線識別碼")
            String connectionId) {

        log.info("Listing PostgreSQL schemas on connection: {}", connectionId);

        return schemaService.listSchemas(connectionId)
                .collectList()
                .map(schemas -> Map.of(
                    "success", true,
                    "connection_id", connectionId,
                    "schemas", schemas,
                    "count", schemas.size()
                ))
                .onErrorResume(error -> {
                    log.error("Failed to list schemas", error);
                    return Mono.just(Map.of(
                        "success", false,
                        "error_message", error.getMessage(),
                        "connection_id", connectionId
                    ));
                });
    }

    /**
     * 取得資料庫統計資訊
     */
    @ToolFunction(
        name = "get_database_stats",
        description = "取得 PostgreSQL 資料庫統計資訊"
    )
    public Mono<Map<String, Object>> getDatabaseStats(
            @ToolParameter(name = "connection_id", description = "連線識別碼")
            String connectionId) {

        log.info("Getting PostgreSQL database statistics on connection: {}", connectionId);

        return schemaService.getDatabaseStats(connectionId)
                .map(stats -> Map.of(
                    "success", true,
                    "connection_id", connectionId,
                    "statistics", stats
                ))
                .onErrorResume(error -> {
                    log.error("Failed to get database statistics", error);
                    return Mono.just(Map.of(
                        "success", false,
                        "error_message", error.getMessage(),
                        "connection_id", connectionId
                    ));
                });
    }
}