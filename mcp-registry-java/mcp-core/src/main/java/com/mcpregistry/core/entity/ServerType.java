package com.mcpregistry.core.entity;

/**
 * MCP Server 類型枚舉
 */
public enum ServerType {
    POSTGRESQL("PostgreSQL", "postgresql", 5432),
    MYSQL("MySQL", "mysql", 3306);

    private final String displayName;
    private final String driverName;
    private final int defaultPort;

    ServerType(String displayName, String driverName, int defaultPort) {
        this.displayName = displayName;
        this.driverName = driverName;
        this.defaultPort = defaultPort;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDriverName() {
        return driverName;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getR2dbcUrl(String host, int port, String database) {
        return String.format("r2dbc:%s://%s:%d/%s", driverName, host, port, database);
    }

    public static ServerType fromDriverName(String driverName) {
        for (ServerType type : values()) {
            if (type.driverName.equalsIgnoreCase(driverName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支援的驅動程式類型: " + driverName);
    }
}