package com.mcpregistry.core.usecase.port.in.connection;

import com.mcpregistry.core.usecase.port.common.UseCaseOutput;

/**
 * 測試資料庫連線用例接口
 */
public interface TestConnectionUseCase {

    /**
     * 測試指定的資料庫連線
     *
     * @param connectionId 連線識別碼
     * @return 測試結果，包含連線狀態和診斷資訊
     */
    UseCaseOutput execute(String connectionId);
}