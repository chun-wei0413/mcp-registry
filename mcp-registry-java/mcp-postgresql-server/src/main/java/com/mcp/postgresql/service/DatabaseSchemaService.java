package com.mcp.postgresql.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

/**
 * PostgreSQL Schema 管理服務
 *
 * 負責管理資料庫 Schema 相關操作
 * 包括表結構查詢、列表查詢和執行計畫分析
 */
@Service
public class DatabaseSchemaService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaService.class);

    private final DatabaseConnectionService connectionService;

    public DatabaseSchemaService(DatabaseConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * 獲取表結構詳細資訊
     */
    public Map<String, Object> getTableSchema(String connectionId, String tableName, String schemaName) throws SQLException {
        try (Connection connection = connectionService.getConnection(connectionId)) {
            Map<String, Object> tableInfo = new LinkedHashMap<>();

            // 基本表資訊
            tableInfo.put("tableName", tableName);
            tableInfo.put("schemaName", schemaName);
            tableInfo.put("tableType", getTableType(connection, tableName, schemaName));

            // 列資訊
            tableInfo.put("columns", getColumnInfo(connection, tableName, schemaName));

            // 主鍵資訊
            tableInfo.put("primaryKeys", getPrimaryKeys(connection, tableName, schemaName));

            // 外鍵資訊
            tableInfo.put("foreignKeys", getForeignKeys(connection, tableName, schemaName));

            // 索引資訊
            tableInfo.put("indexes", getIndexes(connection, tableName, schemaName));

            // 表註釋
            tableInfo.put("comment", getTableComment(connection, tableName, schemaName));

            log.info("獲取表結構成功: {}.{}", schemaName, tableName);
            return tableInfo;
        }
    }

    /**
     * 列出指定 Schema 中的所有表
     */
    public List<Map<String, Object>> listTables(String connectionId, String schemaName) throws SQLException {
        List<Map<String, Object>> tables = new ArrayList<>();

        String sql = """
            SELECT
                table_name,
                table_type,
                table_comment
            FROM information_schema.tables
            WHERE table_schema = ?
            ORDER BY table_name
            """;

        try (Connection connection = connectionService.getConnection(connectionId);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, schemaName);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> table = new LinkedHashMap<>();
                    table.put("tableName", resultSet.getString("table_name"));
                    table.put("tableType", resultSet.getString("table_type"));
                    table.put("comment", resultSet.getString("table_comment"));
                    tables.add(table);
                }
            }
        }

        log.info("列出表成功: {} 個表", tables.size());
        return tables;
    }

    /**
     * 列出所有 Schema
     */
    public List<String> listSchemas(String connectionId) throws SQLException {
        List<String> schemas = new ArrayList<>();

        String sql = """
            SELECT schema_name
            FROM information_schema.schemata
            WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
            ORDER BY schema_name
            """;

        try (Connection connection = connectionService.getConnection(connectionId);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                schemas.add(resultSet.getString("schema_name"));
            }
        }

        log.info("列出 Schema 成功: {} 個 Schema", schemas.size());
        return schemas;
    }

    /**
     * 分析查詢執行計畫
     */
    public Map<String, Object> explainQuery(String connectionId, String sql, boolean analyze) throws SQLException {
        String explainSql = analyze ? "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + sql : "EXPLAIN (FORMAT JSON) " + sql;

        try (Connection connection = connectionService.getConnection(connectionId);
             PreparedStatement statement = connection.prepareStatement(explainSql);
             ResultSet resultSet = statement.executeQuery()) {

            Map<String, Object> result = new LinkedHashMap<>();
            List<String> planLines = new ArrayList<>();

            while (resultSet.next()) {
                planLines.add(resultSet.getString(1));
            }

            result.put("query", sql);
            result.put("analyze", analyze);
            result.put("executionPlan", planLines);
            result.put("explainedAt", java.time.LocalDateTime.now().toString());

            log.info("執行計畫分析完成: analyze={}", analyze);
            return result;
        }
    }

    /**
     * 獲取表類型
     */
    private String getTableType(Connection connection, String tableName, String schemaName) throws SQLException {
        String sql = "SELECT table_type FROM information_schema.tables WHERE table_name = ? AND table_schema = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, schemaName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("table_type");
                }
            }
        }

        return "UNKNOWN";
    }

    /**
     * 獲取列資訊
     */
    private List<Map<String, Object>> getColumnInfo(Connection connection, String tableName, String schemaName) throws SQLException {
        List<Map<String, Object>> columns = new ArrayList<>();

        String sql = """
            SELECT
                column_name,
                data_type,
                character_maximum_length,
                numeric_precision,
                numeric_scale,
                is_nullable,
                column_default,
                ordinal_position
            FROM information_schema.columns
            WHERE table_name = ? AND table_schema = ?
            ORDER BY ordinal_position
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, schemaName);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> column = new LinkedHashMap<>();
                    column.put("columnName", resultSet.getString("column_name"));
                    column.put("dataType", resultSet.getString("data_type"));
                    column.put("maxLength", resultSet.getObject("character_maximum_length"));
                    column.put("precision", resultSet.getObject("numeric_precision"));
                    column.put("scale", resultSet.getObject("numeric_scale"));
                    column.put("nullable", "YES".equals(resultSet.getString("is_nullable")));
                    column.put("defaultValue", resultSet.getString("column_default"));
                    column.put("position", resultSet.getInt("ordinal_position"));
                    columns.add(column);
                }
            }
        }

        return columns;
    }

    /**
     * 獲取主鍵資訊
     */
    private List<String> getPrimaryKeys(Connection connection, String tableName, String schemaName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();

        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getPrimaryKeys(null, schemaName, tableName)) {
                while (resultSet.next()) {
                    primaryKeys.add(resultSet.getString("COLUMN_NAME"));
                }
            }
        } catch (SQLException e) {
            log.warn("獲取主鍵資訊失敗: {}.{}", schemaName, tableName, e);
        }

        return primaryKeys;
    }

    /**
     * 獲取外鍵資訊
     */
    private List<Map<String, Object>> getForeignKeys(Connection connection, String tableName, String schemaName) throws SQLException {
        List<Map<String, Object>> foreignKeys = new ArrayList<>();

        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getImportedKeys(null, schemaName, tableName)) {
                while (resultSet.next()) {
                    Map<String, Object> fk = new LinkedHashMap<>();
                    fk.put("columnName", resultSet.getString("FKCOLUMN_NAME"));
                    fk.put("referencedTable", resultSet.getString("PKTABLE_NAME"));
                    fk.put("referencedColumn", resultSet.getString("PKCOLUMN_NAME"));
                    fk.put("constraintName", resultSet.getString("FK_NAME"));
                    foreignKeys.add(fk);
                }
            }
        } catch (SQLException e) {
            log.warn("獲取外鍵資訊失敗: {}.{}", schemaName, tableName, e);
        }

        return foreignKeys;
    }

    /**
     * 獲取索引資訊
     */
    private List<Map<String, Object>> getIndexes(Connection connection, String tableName, String schemaName) throws SQLException {
        List<Map<String, Object>> indexes = new ArrayList<>();

        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getIndexInfo(null, schemaName, tableName, false, false)) {
                while (resultSet.next()) {
                    Map<String, Object> index = new LinkedHashMap<>();
                    index.put("indexName", resultSet.getString("INDEX_NAME"));
                    index.put("columnName", resultSet.getString("COLUMN_NAME"));
                    index.put("unique", !resultSet.getBoolean("NON_UNIQUE"));
                    index.put("type", resultSet.getShort("TYPE"));
                    indexes.add(index);
                }
            }
        } catch (SQLException e) {
            log.warn("獲取索引資訊失敗: {}.{}", schemaName, tableName, e);
        }

        return indexes;
    }

    /**
     * 獲取表註釋
     */
    private String getTableComment(Connection connection, String tableName, String schemaName) throws SQLException {
        String sql = """
            SELECT obj_description(c.oid) as comment
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = ? AND n.nspname = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, schemaName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("comment");
                }
            }
        } catch (SQLException e) {
            log.warn("獲取表註釋失敗: {}.{}", schemaName, tableName, e);
        }

        return null;
    }
}