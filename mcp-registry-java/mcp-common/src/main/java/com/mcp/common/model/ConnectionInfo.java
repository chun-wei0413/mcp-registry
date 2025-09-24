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
 * Database connection information model
 */
@Data
@Builder
@Jacksonized
public class ConnectionInfo {

    /**
     * Connection unique identifier
     */
    @NotBlank(message = "Connection ID cannot be blank")
    private final String connectionId;

    /**
     * Database host address
     */
    @NotBlank(message = "Host cannot be blank")
    private final String host;

    /**
     * Database port number
     */
    @NotNull(message = "Port cannot be null")
    @Min(value = 1, message = "Port must be greater than 0")
    @Max(value = 65535, message = "Port must be less than 65536")
    private final Integer port;

    /**
     * Database name
     */
    @NotBlank(message = "Database name cannot be blank")
    private final String database;

    /**
     * Username
     */
    @NotBlank(message = "Username cannot be blank")
    private final String username;

    /**
     * Password
     */
    @NotBlank(message = "Password cannot be blank")
    private final String password;

    /**
     * Connection pool size
     */
    @Builder.Default
    private final Integer poolSize = 10;

    /**
     * Whether it is read-only mode
     */
    @Builder.Default
    private final Boolean readOnly = false;

    /**
     * Connection creation time
     */
    @Builder.Default
    private final Instant createdAt = Instant.now();

    /**
     * Connection status
     */
    @Builder.Default
    private final ConnectionStatus status = ConnectionStatus.DISCONNECTED;

    /**
     * Connection status enumeration
     */
    public enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
        ERROR,
        CONNECTING
    }
}