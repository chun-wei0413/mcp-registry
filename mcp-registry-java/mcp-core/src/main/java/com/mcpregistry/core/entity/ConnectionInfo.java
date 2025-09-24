package com.mcpregistry.core.entity;

import java.util.Objects;

/**
 * Database connection information value object
 * Contains all configuration data required for connection
 */
public class ConnectionInfo {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password; // Should be encrypted in actual applications
    private final ServerType serverType;
    private final int poolSize;

    private ConnectionInfo(Builder builder) {
        this.host = Objects.requireNonNull(builder.host, "Host cannot be null");
        this.port = validatePort(builder.port);
        this.database = Objects.requireNonNull(builder.database, "Database name cannot be null");
        this.username = Objects.requireNonNull(builder.username, "Username cannot be null");
        this.password = Objects.requireNonNull(builder.password, "Password cannot be null");
        this.serverType = Objects.requireNonNull(builder.serverType, "Server type cannot be null");
        this.poolSize = validatePoolSize(builder.poolSize);
    }

    public static Builder builder() {
        return new Builder();
    }

    private int validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port number must be between 1-65535");
        }
        return port;
    }

    private int validatePoolSize(int poolSize) {
        if (poolSize <= 0 || poolSize > 100) {
            throw new IllegalArgumentException("Connection pool size must be between 1-100");
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
        private int poolSize = 10; // Default value

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