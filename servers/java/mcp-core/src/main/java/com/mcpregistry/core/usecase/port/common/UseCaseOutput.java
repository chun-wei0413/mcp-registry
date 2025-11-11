package com.mcpregistry.core.usecase.port.common;

import java.util.Optional;

/**
 * Use Case 執行結果的通用輸出格式
 * 採用 Result 模式，統一處理成功和失敗的情況
 */
public class UseCaseOutput {
    private final boolean success;
    private final String message;
    private final Object data;
    private final String errorCode;

    private UseCaseOutput(boolean success, String message, Object data, String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
    }

    /**
     * 創建成功結果
     */
    public static UseCaseOutput success() {
        return new UseCaseOutput(true, "操作成功", null, null);
    }

    /**
     * 創建成功結果並包含資料
     */
    public static UseCaseOutput success(Object data) {
        return new UseCaseOutput(true, "操作成功", data, null);
    }

    /**
     * 創建成功結果並包含自定義訊息
     */
    public static UseCaseOutput success(String message, Object data) {
        return new UseCaseOutput(true, message, data, null);
    }

    /**
     * 創建失敗結果
     */
    public static UseCaseOutput failure(String message) {
        return new UseCaseOutput(false, message, null, null);
    }

    /**
     * 創建失敗結果並包含錯誤代碼
     */
    public static UseCaseOutput failure(String message, String errorCode) {
        return new UseCaseOutput(false, message, null, errorCode);
    }

    /**
     * 創建驗證失敗結果
     */
    public static UseCaseOutput validationFailure(String message) {
        return new UseCaseOutput(false, message, null, "VALIDATION_ERROR");
    }

    /**
     * 創建業務規則違反結果
     */
    public static UseCaseOutput businessRuleViolation(String message) {
        return new UseCaseOutput(false, message, null, "BUSINESS_RULE_VIOLATION");
    }

    // Getters
    public boolean isSuccess() { return success; }
    public boolean isFailure() { return !success; }
    public String getMessage() { return message; }
    public Optional<Object> getData() { return Optional.ofNullable(data); }
    public Optional<String> getErrorCode() { return Optional.ofNullable(errorCode); }

    /**
     * 取得特定類型的資料
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getDataAs(Class<T> type) {
        if (data != null && type.isInstance(data)) {
            return Optional.of((T) data);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "UseCaseOutput{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", hasData=" + (data != null) +
                '}';
    }
}