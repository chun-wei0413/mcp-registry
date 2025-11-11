package com.mcpregistry.core.usecase.service;

import com.mcpregistry.core.entity.*;
import com.mcpregistry.core.usecase.port.in.connection.AddConnectionInput;
import com.mcpregistry.core.usecase.port.in.connection.AddConnectionUseCase;
import com.mcpregistry.core.usecase.port.common.UseCaseOutput;
import com.mcpregistry.core.usecase.port.out.DatabaseConnectionRepository;
import com.mcpregistry.core.usecase.port.out.DatabaseQueryExecutor;

import java.util.Objects;

/**
 * 新增資料庫連線應用服務
 *
 * 實現 Clean Architecture 的 Use Case 層
 * 協調領域實體和輸出端口來完成業務操作
 */
public class AddConnectionService implements AddConnectionUseCase {

    private final DatabaseConnectionRepository connectionRepository;
    private final DatabaseQueryExecutor queryExecutor;

    public AddConnectionService(DatabaseConnectionRepository connectionRepository,
                              DatabaseQueryExecutor queryExecutor) {
        this.connectionRepository = Objects.requireNonNull(connectionRepository, "連線儲存庫不能為空");
        this.queryExecutor = Objects.requireNonNull(queryExecutor, "查詢執行器不能為空");
    }

    @Override
    public UseCaseOutput execute(AddConnectionInput input) {
        // 1. 輸入驗證
        var validationResult = validateInput(input);
        if (validationResult.isFailure()) {
            return validationResult;
        }

        try {
            // 2. 檢查連線是否已存在
            ConnectionId connectionId = ConnectionId.of(input.connectionId);
            if (connectionRepository.existsById(connectionId)) {
                return UseCaseOutput.businessRuleViolation("連線 ID 已存在: " + input.connectionId);
            }

            // 3. 創建連線資訊值對象
            ServerType serverType = ServerType.fromDriverName(input.serverType);
            ConnectionInfo connectionInfo = ConnectionInfo.builder()
                    .host(input.host)
                    .port(input.port)
                    .database(input.database)
                    .username(input.username)
                    .password(input.password)
                    .serverType(serverType)
                    .poolSize(input.poolSize)
                    .build();

            // 4. 創建資料庫連線聚合根
            DatabaseConnection connection = new DatabaseConnection(connectionId, connectionInfo);

            // 5. 測試連線
            var testResult = queryExecutor.testConnection(connectionId).block();
            if (testResult != null && testResult.success) {
                connection.markConnected();
            } else {
                connection.markConnectionFailed(testResult != null ? testResult.message : "連線測試失敗");
            }

            // 6. 保存連線
            connectionRepository.save(connection);

            // 7. 返回成功結果
            return UseCaseOutput.success("連線創建成功", new ConnectionResult(
                    connectionId.getValue(),
                    connection.getStatus().getDisplayName(),
                    connection.getDisplayInfo()
            ));

        } catch (IllegalArgumentException e) {
            return UseCaseOutput.validationFailure("輸入資料無效: " + e.getMessage());
        } catch (Exception e) {
            return UseCaseOutput.failure("創建連線時發生錯誤: " + e.getMessage());
        }
    }

    private UseCaseOutput validateInput(AddConnectionInput input) {
        if (input == null) {
            return UseCaseOutput.validationFailure("輸入資料不能為空");
        }
        if (isNullOrEmpty(input.connectionId)) {
            return UseCaseOutput.validationFailure("連線 ID 不能為空");
        }
        if (isNullOrEmpty(input.host)) {
            return UseCaseOutput.validationFailure("主機名稱不能為空");
        }
        if (input.port <= 0 || input.port > 65535) {
            return UseCaseOutput.validationFailure("埠號必須在 1-65535 之間");
        }
        if (isNullOrEmpty(input.database)) {
            return UseCaseOutput.validationFailure("資料庫名稱不能為空");
        }
        if (isNullOrEmpty(input.username)) {
            return UseCaseOutput.validationFailure("使用者名稱不能為空");
        }
        if (isNullOrEmpty(input.password)) {
            return UseCaseOutput.validationFailure("密碼不能為空");
        }
        if (isNullOrEmpty(input.serverType)) {
            return UseCaseOutput.validationFailure("伺服器類型不能為空");
        }
        if (input.poolSize <= 0 || input.poolSize > 100) {
            return UseCaseOutput.validationFailure("連線池大小必須在 1-100 之間");
        }

        return UseCaseOutput.success();
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 連線創建結果的資料傳輸對象
     */
    public static class ConnectionResult {
        public final String connectionId;
        public final String status;
        public final String displayInfo;

        public ConnectionResult(String connectionId, String status, String displayInfo) {
            this.connectionId = connectionId;
            this.status = status;
            this.displayInfo = displayInfo;
        }
    }
}