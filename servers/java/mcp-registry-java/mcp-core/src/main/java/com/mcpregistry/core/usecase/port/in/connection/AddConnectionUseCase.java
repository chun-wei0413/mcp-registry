package com.mcpregistry.core.usecase.port.in.connection;

import com.mcpregistry.core.usecase.port.common.UseCaseOutput;

/**
 * 新增資料庫連線用例接口
 *
 * 此接口定義了新增資料庫連線的操作契約
 * 遵循 Clean Architecture 的依賴反轉原則
 */
public interface AddConnectionUseCase {

    /**
     * 執行新增連線操作
     *
     * @param input 連線配置資料
     * @return 操作結果，包含成功狀態和連線資訊
     */
    UseCaseOutput execute(AddConnectionInput input);
}