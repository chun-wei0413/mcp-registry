package com.mcpregistry.core.usecase.port.in.query;

import com.mcpregistry.core.usecase.port.common.UseCaseOutput;

/**
 * 執行資料庫查詢用例接口
 */
public interface ExecuteQueryUseCase {

    /**
     * 執行資料庫查詢
     *
     * @param input 查詢輸入資料，包含連線 ID、查詢語句和參數
     * @return 查詢結果，包含資料或錯誤資訊
     */
    UseCaseOutput execute(ExecuteQueryInput input);
}