package com.mcp.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * 資料庫連線資訊模型
 */
@Data
@Builder
@Jacksonized
public class ConnectionInfo {

    /**
     * 連線唯一識別碼
     */
    @NotBlank(message = "Connection ID cannot be blank")
    private final String connectionId;

    /**
     * 資料庫主機位址
     */
    @NotBlank(message = "Host cannot be blank")
    private final String host;

    /**
     * 資料庫埠號
     */
    @NotNull(message = "Port cannot be null")
    @Min(value = 1, message = "Port must be greater than 0")
    @Max(value = 65535, message = "Port must be less than 65536")
    private final Integer port;

    /**
     * 資料庫名稱
     */
    @NotBlank(message = "Database name cannot be blank")
    private final String database;

    /**
     * 使用者名稱
     */
    @NotBlank(message = "Username cannot be blank")
    private final String username;

    /**
     * 密碼
     */
    @NotBlank(message = "Password cannot be blank")
    private final String password;

    /**
     * 連線池大小
     */
    @Builder.Default
    private final Integer poolSize = 10;

    /**
     * 是否為只讀模式
     */
    @Builder.Default
    private final Boolean readOnly = false;

    /**
     * 連線建立時間
     */
    @Builder.Default
    private final Instant createdAt = Instant.now();

    /**
     * 連線狀態
     */
    @Builder.Default
    private final ConnectionStatus status = ConnectionStatus.DISCONNECTED;

    /**
     * 連線狀態枚舉
     */
    public enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
        ERROR,
        CONNECTING
    }
}