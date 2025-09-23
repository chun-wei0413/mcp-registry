package com.mcpregistry.core.entity;

import java.util.Objects;

/**
 * 資料庫連線資訊值對象
 * 包含連線所需的所有配置資料
 */
public class ConnectionInfo {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password; // 實際應用中應加密
    private final ServerType serverType;
    private final int poolSize;

    private ConnectionInfo(Builder builder) {
        this.host = Objects.requireNonNull(builder.host, "主機不能為空");
        this.port = validatePort(builder.port);
        this.database = Objects.requireNonNull(builder.database, "資料庫名稱不能為空");
        this.username = Objects.requireNonNull(builder.username, "使用者名稱不能為空");
        this.password = Objects.requireNonNull(builder.password, "密碼不能為空");
        this.serverType = Objects.requireNonNull(builder.serverType, "伺服器類型不能為空");
        this.poolSize = validatePoolSize(builder.poolSize);
    }

    public static Builder builder() {
        return new Builder();
    }

    private int validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("埠號必須在 1-65535 之間");
        }
        return port;
    }

    private int validatePoolSize(int poolSize) {
        if (poolSize <= 0 || poolSize > 100) {
            throw new IllegalArgumentException("連線池大小必須在 1-100 之間");
        }
        return poolSize;
    }

    public String getR2dbcUrl() {
        return serverType.getR2dbcUrl(host, port, database);
    }

    // Getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public ServerType getServerType() { return serverType; }
    public int getPoolSize() { return poolSize; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionInfo that = (ConnectionInfo) o;
        return port == that.port &&
               poolSize == that.poolSize &&
               Objects.equals(host, that.host) &&
               Objects.equals(database, that.database) &&
               Objects.equals(username, that.username) &&
               Objects.equals(password, that.password) &&
               serverType == that.serverType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, database, username, password, serverType, poolSize);
    }

    public static class Builder {
        private String host;
        private int port;
        private String database;
        private String username;
        private String password;
        private ServerType serverType;
        private int poolSize = 10; // 預設值

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder database(String database) {
            this.database = database;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder serverType(ServerType serverType) {
            this.serverType = serverType;
            return this;
        }

        public Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public ConnectionInfo build() {
            return new ConnectionInfo(this);
        }
    }
}