package com.mcpregistry.core.usecase.port.in.connection;

/**
 * 新增資料庫連線的輸入 DTO
 */
public class AddConnectionInput {
    public String connectionId;
    public String host;
    public int port;
    public String database;
    public String username;
    public String password;
    public String serverType; // "postgresql" or "mysql"
    public int poolSize;

    // 預設建構子 - 供序列化框架使用
    public AddConnectionInput() {}

    public AddConnectionInput(String connectionId, String host, int port, String database,
                             String username, String password, String serverType, int poolSize) {
        this.connectionId = connectionId;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.serverType = serverType;
        this.poolSize = poolSize;
    }
}