package com.mcp.postgresql.service;

import com.mcp.common.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * PostgreSQL 查詢執行服務
 *
 * 負責執行各種類型的 SQL 查詢
 * 支援參數化查詢、事務處理和批次操作
 */
@Service
public class DatabaseQueryService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseQueryService.class);

    private final DatabaseConnectionService connectionService;

    public DatabaseQueryService(DatabaseConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * 執行 SELECT 查詢
     */
    public QueryResult executeQuery(String connectionId, String sql, List<Object> parameters, int fetchSize) {
        long startTime = System.currentTimeMillis();

        try (Connection connection = connectionService.getConnection(connectionId);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 設定參數
            setParameters(statement, parameters);

            // 設定 fetch size
            statement.setFetchSize(fetchSize);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                List<String> columnNames = new ArrayList<>();

                // 獲取列資訊
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metaData.getColumnName(i));
                }

                // 讀取資料
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
                    }
                    rows.add(row);
                }

                long executionTime = System.currentTimeMillis() - startTime;

                log.info("查詢執行成功: {} 行, {}ms", rows.size(), executionTime);

                return QueryResult.builder()
                    .success(true)
                    .rows(rows)
                    .rowCount(rows.size())
                    .columns(createColumnInfoList(metaData))
                    .executionTimeMs(executionTime)
                    .build();
            }

        } catch (SQLException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("查詢執行失敗: " + sql, e);

            return QueryResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .executionTimeMs(executionTime)
                .build();
        }
    }

    /**
     * 執行 UPDATE/INSERT/DELETE
     */
    public int executeUpdate(String connectionId, String sql, List<Object> parameters) throws SQLException {
        try (Connection connection = connectionService.getConnection(connectionId);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, parameters);

            int affectedRows = statement.executeUpdate();
            log.info("更新執行成功: {} 行受影響", affectedRows);

            return affectedRows;
        }
    }

    /**
     * 執行事務操作
     */
    public List<Object> executeTransaction(String connectionId, List<Map<String, Object>> queries) throws SQLException {
        List<Object> results = new ArrayList<>();

        try (Connection connection = connectionService.getConnection(connectionId)) {
            connection.setAutoCommit(false);

            try {
                for (Map<String, Object> queryInfo : queries) {
                    String sql = (String) queryInfo.get("sql");
                    @SuppressWarnings("unchecked")
                    List<Object> parameters = (List<Object>) queryInfo.get("parameters");

                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        setParameters(statement, parameters);

                        boolean isQuery = statement.execute();
                        if (isQuery) {
                            // SELECT 查詢
                            try (ResultSet resultSet = statement.getResultSet()) {
                                List<Map<String, Object>> rows = extractRows(resultSet);
                                results.add(rows);
                            }
                        } else {
                            // UPDATE/INSERT/DELETE
                            int updateCount = statement.getUpdateCount();
                            results.add(updateCount);
                        }
                    }
                }

                connection.commit();
                log.info("事務執行成功: {} 個查詢", queries.size());

                return results;

            } catch (SQLException e) {
                connection.rollback();
                log.error("事務執行失敗，已回滾", e);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * 批次執行相同 SQL
     */
    public int[] executeBatch(String connectionId, String sql, List<List<Object>> parametersList) throws SQLException {
        try (Connection connection = connectionService.getConnection(connectionId);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (List<Object> parameters : parametersList) {
                setParameters(statement, parameters);
                statement.addBatch();
            }

            int[] results = statement.executeBatch();
            log.info("批次執行成功: {} 個語句", results.length);

            return results;
        }
    }

    /**
     * 設定 PreparedStatement 參數
     */
    private void setParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                Object parameter = parameters.get(i);
                statement.setObject(i + 1, parameter);
            }
        }
    }

    /**
     * 從 ResultSet 提取行資料
     */
    private List<Map<String, Object>> extractRows(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(i);
                row.put(columnName, value);
            }
            rows.add(row);
        }

        return rows;
    }

    /**
     * 建立列資訊列表
     */
    private List<QueryResult.ColumnInfo> createColumnInfoList(ResultSetMetaData metaData) throws SQLException {
        List<QueryResult.ColumnInfo> columns = new ArrayList<>();

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            QueryResult.ColumnInfo columnInfo = QueryResult.ColumnInfo.builder()
                .name(metaData.getColumnName(i))
                .type(metaData.getColumnTypeName(i))
                .nullable(metaData.isNullable(i) == ResultSetMetaData.columnNullable)
                .precision(metaData.getPrecision(i))
                .scale(metaData.getScale(i))
                .build();

            columns.add(columnInfo);
        }

        return columns;
    }
}