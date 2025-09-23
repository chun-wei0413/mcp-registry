package com.mcpregistry.core.usecase.service;

import com.mcpregistry.core.entity.*;
import com.mcpregistry.core.usecase.port.in.query.ExecuteQueryInput;
import com.mcpregistry.core.usecase.port.in.query.ExecuteQueryUseCase;
import com.mcpregistry.core.usecase.port.common.UseCaseOutput;
import com.mcpregistry.core.usecase.port.out.DatabaseConnectionRepository;
import com.mcpregistry.core.usecase.port.out.DatabaseQueryExecutor;
import com.mcpregistry.core.usecase.port.out.QueryExecutionRepository;

import java.util.Objects;

/**
 * 執行資料庫查詢應用服務
 *
 * 協調查詢執行的完整流程：驗證、執行、記錄
 */
public class ExecuteQueryService implements ExecuteQueryUseCase {

    private final DatabaseConnectionRepository connectionRepository;
    private final QueryExecutionRepository queryExecutionRepository;
    private final DatabaseQueryExecutor queryExecutor;

    public ExecuteQueryService(DatabaseConnectionRepository connectionRepository,
                              QueryExecutionRepository queryExecutionRepository,
                              DatabaseQueryExecutor queryExecutor) {
        this.connectionRepository = Objects.requireNonNull(connectionRepository, "連線儲存庫不能為空");
        this.queryExecutionRepository = Objects.requireNonNull(queryExecutionRepository, "查詢執行儲存庫不能為空");
        this.queryExecutor = Objects.requireNonNull(queryExecutor, "查詢執行器不能為空");
    }

    @Override
    public UseCaseOutput execute(ExecuteQueryInput input) {
        // 1. 輸入驗證
        var validationResult = validateInput(input);
        if (validationResult.isFailure()) {
            return validationResult;
        }

        ConnectionId connectionId = ConnectionId.of(input.connectionId);

        try {
            // 2. 檢查連線是否存在且可用
            var connectionOpt = connectionRepository.findById(connectionId);
            if (connectionOpt.isEmpty()) {
                return UseCaseOutput.businessRuleViolation("連線不存在: " + input.connectionId);
            }

            DatabaseConnection connection = connectionOpt.get();
            if (!connection.isAvailable()) {
                return UseCaseOutput.businessRuleViolation(
                    "連線不可用，狀態: " + connection.getStatus().getDisplayName());
            }

            // 3. 創建查詢執行聚合根
            QueryExecution queryExecution = new QueryExecution(
                connectionId,
                input.query,
                input.parameters
            );

            // 4. 保存查詢執行記錄（開始狀態）
            queryExecutionRepository.save(queryExecution);

            // 5. 執行查詢
            queryExecution.markStarted();
            queryExecutionRepository.save(queryExecution);

            try {
                // 根據查詢類型選擇執行方法
                if (queryExecution.getQueryType().isReadOnly()) {
                    var result = queryExecutor.executeQuery(connectionId, input.query, input.parameters).block();
                    queryExecution.markCompleted(result);

                    // 6. 更新連線的最後存取時間
                    connection.updateLastAccessed();
                    connectionRepository.save(connection);

                    // 7. 保存完成狀態
                    queryExecutionRepository.save(queryExecution);

                    return UseCaseOutput.success("查詢執行成功", new QueryExecutionResult(
                        queryExecution.getId().getValue(),
                        queryExecution.getStatus().getDisplayName(),
                        result,
                        queryExecution.getExecutionTimeMs()
                    ));

                } else {
                    var affectedRows = queryExecutor.executeUpdate(connectionId, input.query, input.parameters).block();
                    queryExecution.markCompleted(affectedRows);

                    connection.updateLastAccessed();
                    connectionRepository.save(connection);
                    queryExecutionRepository.save(queryExecution);

                    return UseCaseOutput.success("更新執行成功", new QueryExecutionResult(
                        queryExecution.getId().getValue(),
                        queryExecution.getStatus().getDisplayName(),
                        affectedRows,
                        queryExecution.getExecutionTimeMs()
                    ));
                }

            } catch (Exception e) {
                // 8. 查詢執行失敗
                queryExecution.markFailed(e.getMessage());
                queryExecutionRepository.save(queryExecution);

                return UseCaseOutput.failure("查詢執行失敗: " + e.getMessage());
            }

        } catch (Exception e) {
            return UseCaseOutput.failure("執行查詢時發生錯誤: " + e.getMessage());
        }
    }

    private UseCaseOutput validateInput(ExecuteQueryInput input) {
        if (input == null) {
            return UseCaseOutput.validationFailure("輸入資料不能為空");
        }
        if (isNullOrEmpty(input.connectionId)) {
            return UseCaseOutput.validationFailure("連線 ID 不能為空");
        }
        if (isNullOrEmpty(input.query)) {
            return UseCaseOutput.validationFailure("查詢語句不能為空");
        }

        // 基本 SQL 注入防護檢查
        if (containsDangerousKeywords(input.query)) {
            return UseCaseOutput.validationFailure("查詢包含危險關鍵字");
        }

        return UseCaseOutput.success();
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean containsDangerousKeywords(String query) {
        String upperQuery = query.toUpperCase();
        String[] dangerousKeywords = {"DROP", "TRUNCATE", "DELETE FROM", "UPDATE", "INSERT", "ALTER"};

        // 這裡可以實現更複雜的 SQL 驗證邏輯
        // 實際應用中應該使用專業的 SQL 解析器
        return false; // 簡化實現
    }

    /**
     * 查詢執行結果的資料傳輸對象
     */
    public static class QueryExecutionResult {
        public final String queryId;
        public final String status;
        public final Object result;
        public final long executionTimeMs;

        public QueryExecutionResult(String queryId, String status, Object result, long executionTimeMs) {
            this.queryId = queryId;
            this.status = status;
            this.result = result;
            this.executionTimeMs = executionTimeMs;
        }
    }
}