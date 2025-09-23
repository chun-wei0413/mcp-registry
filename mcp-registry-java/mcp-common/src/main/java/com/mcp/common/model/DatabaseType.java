package com.mcp.common.model;

/**
 * 支援的資料庫類型
 */
public enum DatabaseType {
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "postgresql"),
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "mysql");

    private final String displayName;
    private final String driverClassName;
    private final String urlPrefix;

    DatabaseType(String displayName, String driverClassName, String urlPrefix) {
        this.displayName = displayName;
        this.driverClassName = driverClassName;
        this.urlPrefix = urlPrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    /**
     * 建立 JDBC URL
     */
    public String buildJdbcUrl(String host, int port, String database) {
        return String.format("jdbc:%s://%s:%d/%s", urlPrefix, host, port, database);
    }

    /**
     * 從字串解析資料庫類型
     */
    public static DatabaseType fromString(String type) {
        for (DatabaseType dbType : values()) {
            if (dbType.name().equalsIgnoreCase(type) || dbType.displayName.equalsIgnoreCase(type)) {
                return dbType;
            }
        }
        throw new IllegalArgumentException("不支援的資料庫類型: " + type);
    }
}