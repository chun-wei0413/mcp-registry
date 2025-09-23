package com.mcp.postgresql.tool;

import com.mcp.common.mcp.McpTool;
import com.mcp.common.mcp.McpToolResult;
import com.mcp.postgresql.service.DatabaseSchemaService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PostgreSQL Schema 管理工具
 *
 * 提供 MCP Schema 管理功能:
 * - get_table_schema: 獲取表結構
 * - list_tables: 列出所有表
 * - list_schemas: 列出所有 Schema
 * - explain_query: 分析查詢執行計畫
 */
@Component
public class SchemaManagementTool implements McpTool {

    private final DatabaseSchemaService schemaService;

    public SchemaManagementTool(DatabaseSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @Override
    public String getToolName() {
        return "postgresql_schema_management";
    }

    @Override
    public String getDescription() {
        return "PostgreSQL Schema 管理工具，支援表結構查詢、列表和執行計畫分析";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "action", Map.of(
                    "type", "string",
                    "enum", new String[]{"get_table_schema", "list_tables", "list_schemas", "explain_query"},
                    "description", "操作類型"
                ),
                "connectionId", Map.of(
                    "type", "string",
                    "description", "連線識別碼"
                ),
                "tableName", Map.of(
                    "type", "string",
                    "description", "表名稱"
                ),
                "schemaName", Map.of(
                    "type", "string",
                    "default", "public",
                    "description", "Schema 名稱"
                ),
                "sql", Map.of(
                    "type", "string",
                    "description", "要分析的 SQL 語句"
                ),
                "analyze", Map.of(
                    "type", "boolean",
                    "default", false,
                    "description", "是否執行 ANALYZE (實際執行查詢)"
                )
            ),
            "required", new String[]{"action", "connectionId"}
        );
    }

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        try {
            String action = (String) arguments.get("action");
            String connectionId = (String) arguments.get("connectionId");

            if (connectionId == null || connectionId.trim().isEmpty()) {
                return McpToolResult.error("Connection ID 不能為空");
            }

            return switch (action) {
                case "get_table_schema" -> getTableSchema(arguments);
                case "list_tables" -> listTables(arguments);
                case "list_schemas" -> listSchemas(connectionId);
                case "explain_query" -> explainQuery(arguments);
                default -> McpToolResult.error("不支援的操作: " + action);
            };

        } catch (Exception e) {
            return McpToolResult.error("Schema 管理操作失敗: " + e.getMessage());
        }
    }

    private McpToolResult getTableSchema(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connectionId");
            String tableName = (String) arguments.get("tableName");
            String schemaName = arguments.get("schemaName") != null ?
                (String) arguments.get("schemaName") : "public";

            if (tableName == null || tableName.trim().isEmpty()) {
                return McpToolResult.error("表名稱不能為空");
            }

            Map<String, Object> schema = schemaService.getTableSchema(connectionId, tableName, schemaName);

            return McpToolResult.success(
                "表結構獲取成功",
                Map.of(
                    "tableName", tableName,
                    "schemaName", schemaName,
                    "schema", schema
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("獲取表結構失敗: " + e.getMessage());
        }
    }

    private McpToolResult listTables(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connectionId");
            String schemaName = arguments.get("schemaName") != null ?
                (String) arguments.get("schemaName") : "public";

            var tables = schemaService.listTables(connectionId, schemaName);

            return McpToolResult.success(
                "表列表獲取成功",
                Map.of(
                    "schemaName", schemaName,
                    "tables", tables,
                    "tableCount", tables.size()
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("獲取表列表失敗: " + e.getMessage());
        }
    }

    private McpToolResult listSchemas(String connectionId) {
        try {
            var schemas = schemaService.listSchemas(connectionId);

            return McpToolResult.success(
                "Schema 列表獲取成功",
                Map.of(
                    "schemas", schemas,
                    "schemaCount", schemas.size()
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("獲取 Schema 列表失敗: " + e.getMessage());
        }
    }

    private McpToolResult explainQuery(Map<String, Object> arguments) {
        try {
            String connectionId = (String) arguments.get("connectionId");
            String sql = (String) arguments.get("sql");
            Boolean analyze = arguments.get("analyze") != null ?
                (Boolean) arguments.get("analyze") : false;

            if (sql == null || sql.trim().isEmpty()) {
                return McpToolResult.error("SQL 語句不能為空");
            }

            Map<String, Object> executionPlan = schemaService.explainQuery(connectionId, sql, analyze);

            return McpToolResult.success(
                "執行計畫分析完成",
                Map.of(
                    "sql", sql,
                    "analyze", analyze,
                    "executionPlan", executionPlan
                )
            );

        } catch (Exception e) {
            return McpToolResult.error("分析執行計畫失敗: " + e.getMessage());
        }
    }
}