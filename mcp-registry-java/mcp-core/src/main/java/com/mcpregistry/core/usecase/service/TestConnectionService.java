package com.mcpregistry.core.usecase.service;

import com.mcpregistry.core.entity.ConnectionId;
import com.mcpregistry.core.entity.DatabaseConnection;
import com.mcpregistry.core.usecase.port.in.connection.TestConnectionUseCase;
import com.mcpregistry.core.usecase.port.common.UseCaseOutput;
import com.mcpregistry.core.usecase.port.out.DatabaseConnectionRepository;
import com.mcpregistry.core.usecase.port.out.DatabaseQueryExecutor;

import java.util.Objects;
import java.util.Optional;

/**
 * 測試資料庫連線應用服務
 *
 * 實現 Clean Architecture 的 Use Case 層
 * 測試指定的資料庫連線並返回狀態
 */
public class TestConnectionService implements TestConnectionUseCase {

    private final DatabaseConnectionRepository connectionRepository;
    private final DatabaseQueryExecutor queryExecutor;

    public TestConnectionService(DatabaseConnectionRepository connectionRepository,
                               DatabaseQueryExecutor queryExecutor) {
        this.connectionRepository = Objects.requireNonNull(connectionRepository, "連線儲存庫不能為空");
        this.queryExecutor = Objects.requireNonNull(queryExecutor, "查詢執行器不能為空");
    }

    @Override
    public UseCaseOutput execute(String connectionIdString) {
        // 1. 輸入驗證
        if (connectionIdString == null || connectionIdString.trim().isEmpty()) {
            return UseCaseOutput.validationFailure("連線 ID 不能為空");
        }

        try {
            // 2. 檢查連線是否存在
            ConnectionId connectionId = ConnectionId.of(connectionIdString.trim());
            Optional<DatabaseConnection> connectionOpt = connectionRepository.findById(connectionId);

            if (connectionOpt.isEmpty()) {
                return UseCaseOutput.failure("連線不存在: " + connectionIdString);
            }

            DatabaseConnection connection = connectionOpt.get();

            // 3. 執行連線測試
            var testResult = queryExecutor.testConnection(connectionId).block();

            if (testResult == null) {
                return UseCaseOutput.failure("連線測試超時或失敗");
            }

            // 4. 更新連線狀態
            if (testResult.success) {
                connection.markConnected();
                connectionRepository.save(connection);

                return UseCaseOutput.success("連線測試成功", new TestConnectionResult(
                    connectionIdString,
                    "CONNECTED",
                    testResult.message,
                    connection.getDisplayInfo()
                ));
            } else {
                connection.markConnectionFailed(testResult.message);
                connectionRepository.save(connection);

                return UseCaseOutput.failure("連線測試失敗: " + testResult.message);
            }

        } catch (IllegalArgumentException e) {
            return UseCaseOutput.validationFailure("無效的連線 ID: " + e.getMessage());
        } catch (Exception e) {
            return UseCaseOutput.failure("測試連線時發生錯誤: " + e.getMessage());
        }
    }

    /**
     * 連線測試結果的資料傳輸對象
     */
    public static class TestConnectionResult {
        public final String connectionId;
        public final String status;
        public final String message;
        public final String displayInfo;

        public TestConnectionResult(String connectionId, String status, String message, String displayInfo) {
            this.connectionId = connectionId;
            this.status = status;
            this.message = message;
            this.displayInfo = displayInfo;
        }
    }
}