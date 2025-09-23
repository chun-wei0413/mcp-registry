# PostgreSQL MCP Server - 客戶端整合範例 🔌

本文件提供各種 MCP 客戶端與 PostgreSQL MCP Server 的整合範例。

## 📋 目錄

- [Python 客戶端](#python-客戶端)
- [Claude Desktop 整合](#claude-desktop-整合)
- [Node.js 客戶端](#nodejs-客戶端)
- [實際應用場景](#實際應用場景)

## 🐍 Python 客戶端

### 安裝依賴

```bash
pip install mcp anthropic-mcp-client asyncio asyncpg
```

### 基本連線和查詢

```python
# examples/python_client.py
import asyncio
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def main():
    # 連接到 MCP Server
    server_params = StdioServerParameters(
        command="docker",
        args=["exec", "-i", "postgresql-mcp-server", "python", "-m", "postgresql_mcp_server"]
    )

    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            # 初始化
            await session.initialize()

            # 1. 建立資料庫連線
            result = await session.call_tool(
                "add_connection",
                {
                    "connection_id": "main_db",
                    "host": "localhost",
                    "port": 5432,
                    "database": "testdb",
                    "user": "postgres",
                    "password": "password"
                }
            )
            print("連線結果:", result.content)

            # 2. 測試連線
            health = await session.call_tool("test_connection", {"connection_id": "main_db"})
            print("連線測試:", health.content)

            # 3. 列出所有表
            tables = await session.call_tool("list_tables", {"connection_id": "main_db"})
            print("資料庫表:", tables.content)

            # 4. 執行查詢
            query_result = await session.call_tool(
                "execute_query",
                {
                    "connection_id": "main_db",
                    "query": "SELECT current_database(), current_user, version()"
                }
            )
            print("查詢結果:", query_result.content)

if __name__ == "__main__":
    asyncio.run(main())
```

### 資料遷移範例

```python
# examples/data_migration.py
import asyncio
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def migrate_user_data():
    """將舊用戶表資料遷移到新用戶表的範例"""

    server_params = StdioServerParameters(
        command="docker",
        args=["exec", "-i", "postgresql-mcp-server", "python", "-m", "postgresql_mcp_server"]
    )

    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()

            # 連線到資料庫
            await session.call_tool("add_connection", {
                "connection_id": "migration_db",
                "host": "localhost",
                "database": "company_db",
                "user": "admin",
                "password": "admin_password"
            })

            print("🔍 分析來源表結構...")
            old_schema = await session.call_tool(
                "get_table_schema",
                {"connection_id": "migration_db", "table_name": "old_users"}
            )
            print("舊表結構:", old_schema.content)

            print("🔍 分析目標表結構...")
            new_schema = await session.call_tool(
                "get_table_schema",
                {"connection_id": "migration_db", "table_name": "new_users"}
            )
            print("新表結構:", new_schema.content)

            print("📊 提取待遷移資料...")
            source_data = await session.call_tool(
                "execute_query",
                {
                    "connection_id": "migration_db",
                    "query": """
                        SELECT id, name, email, created_at
                        FROM old_users
                        WHERE active = true AND created_at > $1
                    """,
                    "params": ["2024-01-01"]
                }
            )

            users = source_data.content["rows"]
            print(f"找到 {len(users)} 筆需要遷移的資料")

            print("🔄 開始批次遷移...")
            migration_queries = []
            for user in users:
                migration_queries.append({
                    "query": """
                        INSERT INTO new_users (legacy_id, full_name, email_address, registration_date)
                        VALUES ($1, $2, $3, $4)
                    """,
                    "params": [user["id"], user["name"], user["email"], user["created_at"]]
                })

            # 使用事務確保資料一致性
            result = await session.call_tool(
                "execute_transaction",
                {
                    "connection_id": "migration_db",
                    "queries": migration_queries
                }
            )

            if result.content["success"]:
                print("✅ 遷移成功完成!")
                print(f"已處理 {result.content['affected_rows']} 筆資料")
            else:
                print("❌ 遷移失敗:", result.content["error"])

if __name__ == "__main__":
    asyncio.run(migrate_user_data())
```

### 效能監控範例

```python
# examples/monitoring.py
import asyncio
import time
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def monitor_database_performance():
    """資料庫效能監控範例"""

    server_params = StdioServerParameters(
        command="docker",
        args=["exec", "-i", "postgresql-mcp-server", "python", "-m", "postgresql_mcp_server"]
    )

    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()

            # 連線到監控目標資料庫
            await session.call_tool("add_connection", {
                "connection_id": "monitor_db",
                "host": "production-db.company.com",
                "database": "main_app",
                "user": "monitor_user",
                "password": "monitor_pass"
            })

            print("📊 開始效能監控...")

            while True:
                try:
                    # 1. 檢查 MCP Server 健康狀態
                    health = await session.call_tool("health_check")
                    print(f"🏥 MCP Server 狀態: {health.content['status']}")

                    # 2. 取得系統指標
                    metrics = await session.call_tool("get_metrics")
                    conn_info = metrics.content["connections"]
                    print(f"🔌 活躍連線: {conn_info['active']}/{conn_info['total']}")

                    # 3. 檢查資料庫效能
                    performance_query = await session.call_tool(
                        "execute_query",
                        {
                            "connection_id": "monitor_db",
                            "query": """
                                SELECT
                                    datname,
                                    numbackends as active_connections,
                                    xact_commit + xact_rollback as total_transactions,
                                    blks_read + blks_hit as total_blocks_accessed,
                                    round((blks_hit * 100.0) / (blks_hit + blks_read), 2) as cache_hit_ratio
                                FROM pg_stat_database
                                WHERE datname = current_database()
                            """
                        }
                    )

                    db_stats = performance_query.content["rows"][0]
                    print(f"📈 資料庫指標:")
                    print(f"  - 活躍連線: {db_stats['active_connections']}")
                    print(f"  - 總交易數: {db_stats['total_transactions']}")
                    print(f"  - 快取命中率: {db_stats['cache_hit_ratio']}%")

                    # 4. 檢查慢查詢
                    slow_queries = await session.call_tool(
                        "execute_query",
                        {
                            "connection_id": "monitor_db",
                            "query": """
                                SELECT
                                    query,
                                    calls,
                                    total_time,
                                    mean_time,
                                    rows
                                FROM pg_stat_statements
                                WHERE mean_time > 1000  -- 超過1秒的查詢
                                ORDER BY mean_time DESC
                                LIMIT 5
                            """
                        }
                    )

                    if slow_queries.content["rows"]:
                        print("🐌 發現慢查詢:")
                        for query in slow_queries.content["rows"]:
                            print(f"  - 平均耗時: {query['mean_time']:.2f}ms")
                            print(f"    查詢: {query['query'][:100]}...")

                    print("-" * 50)

                    # 每30秒檢查一次
                    await asyncio.sleep(30)

                except KeyboardInterrupt:
                    print("監控已停止")
                    break
                except Exception as e:
                    print(f"監控錯誤: {e}")
                    await asyncio.sleep(10)

if __name__ == "__main__":
    asyncio.run(monitor_database_performance())
```

## 🖥️ Claude Desktop 整合

### MCP 配置檔案

建立或修改 `~/.config/claude-desktop/mcp.json`:

```json
{
  "mcpServers": {
    "postgresql-mcp": {
      "command": "docker",
      "args": [
        "exec", "-i", "postgresql-mcp-server",
        "python", "-m", "postgresql_mcp_server"
      ],
      "env": {
        "MCP_LOG_LEVEL": "INFO"
      }
    }
  }
}
```

### Claude 對話範例

```
用戶: 請幫我連接到公司的 PostgreSQL 資料庫，並分析銷售表的結構

Claude: 我來幫你連接資料庫並分析銷售表。首先讓我建立連線：

[使用 add_connection 工具]
- connection_id: "company_db"
- host: "db.company.com"
- database: "sales_analytics"
- user: "analyst"
- password: [請提供密碼]

連線成功後，我會使用 get_table_schema 工具分析銷售表結構，並提供詳細的欄位說明和索引資訊。

用戶: 密碼是 "analytics2024"，資料庫中有個 sales_data 表

Claude: 好的，現在我建立連線並分析 sales_data 表結構：

[呼叫 MCP 工具執行...]

連線成功！以下是 sales_data 表的結構分析：

📊 **表基本資訊**
- 表名: sales_data
- 總行數: 1,234,567 行
- 表大小: 89.2 MB

🏗️ **欄位結構**
1. id (SERIAL PRIMARY KEY) - 主鍵，自動遞增
2. product_id (INTEGER) - 產品ID，有外鍵約束
3. sales_date (DATE) - 銷售日期，有索引
4. amount (DECIMAL(10,2)) - 銷售金額
5. customer_id (INTEGER) - 客戶ID
6. region (VARCHAR(50)) - 銷售區域

📈 **索引資訊**
- PRIMARY KEY: id
- INDEX: sales_date_idx (sales_date)
- INDEX: product_customer_idx (product_id, customer_id)

需要我進一步分析資料或執行特定查詢嗎？
```

## 🟢 Node.js 客戶端

### 安裝依賴

```bash
npm install @modelcontextprotocol/sdk ws
```

### 基本客戶端實現

```javascript
// examples/nodejs_client.js
const { Client } = require('@modelcontextprotocol/sdk/client/index.js');
const { StdioClientTransport } = require('@modelcontextprotocol/sdk/client/stdio.js');

class PostgreSQLMCPClient {
    constructor() {
        this.client = null;
        this.transport = null;
    }

    async connect() {
        // 連接到 Docker 容器中的 MCP Server
        this.transport = new StdioClientTransport({
            command: 'docker',
            args: ['exec', '-i', 'postgresql-mcp-server', 'python', '-m', 'postgresql_mcp_server']
        });

        this.client = new Client({
            name: "nodejs-postgresql-client",
            version: "1.0.0"
        }, {
            capabilities: {}
        });

        await this.client.connect(this.transport);
        console.log('✅ 已連接到 PostgreSQL MCP Server');
    }

    async addConnection(connectionId, config) {
        const result = await this.client.request({
            method: 'tools/call',
            params: {
                name: 'add_connection',
                arguments: {
                    connection_id: connectionId,
                    host: config.host,
                    port: config.port || 5432,
                    database: config.database,
                    user: config.user,
                    password: config.password
                }
            }
        });
        return result.content;
    }

    async executeQuery(connectionId, query, params = []) {
        const result = await this.client.request({
            method: 'tools/call',
            params: {
                name: 'execute_query',
                arguments: {
                    connection_id: connectionId,
                    query: query,
                    params: params
                }
            }
        });
        return result.content;
    }

    async getTableSchema(connectionId, tableName, schema = 'public') {
        const result = await this.client.request({
            method: 'tools/call',
            params: {
                name: 'get_table_schema',
                arguments: {
                    connection_id: connectionId,
                    table_name: tableName,
                    schema: schema
                }
            }
        });
        return result.content;
    }

    async healthCheck() {
        const result = await this.client.request({
            method: 'tools/call',
            params: {
                name: 'health_check',
                arguments: {}
            }
        });
        return result.content;
    }

    async disconnect() {
        if (this.client) {
            await this.client.close();
        }
    }
}

// 使用範例
async function main() {
    const mcpClient = new PostgreSQLMCPClient();

    try {
        // 連接到 MCP Server
        await mcpClient.connect();

        // 健康檢查
        const health = await mcpClient.healthCheck();
        console.log('健康狀態:', health);

        // 建立資料庫連線
        await mcpClient.addConnection('test_db', {
            host: 'localhost',
            database: 'testdb',
            user: 'postgres',
            password: 'password'
        });
        console.log('✅ 資料庫連線已建立');

        // 執行查詢
        const result = await mcpClient.executeQuery(
            'test_db',
            'SELECT table_name FROM information_schema.tables WHERE table_schema = $1',
            ['public']
        );
        console.log('查詢結果:', result);

        // 分析表結構 (如果有表的話)
        if (result.rows && result.rows.length > 0) {
            const tableName = result.rows[0].table_name;
            const schema = await mcpClient.getTableSchema('test_db', tableName);
            console.log(`表 ${tableName} 的結構:`, schema);
        }

    } catch (error) {
        console.error('錯誤:', error);
    } finally {
        await mcpClient.disconnect();
    }
}

// 執行範例
if (require.main === module) {
    main().catch(console.error);
}

module.exports = PostgreSQLMCPClient;
```

### Express.js Web 服務範例

```javascript
// examples/web_service.js
const express = require('express');
const PostgreSQLMCPClient = require('./nodejs_client.js');

const app = express();
app.use(express.json());

let mcpClient = null;

// 初始化 MCP 客戶端
async function initializeMCP() {
    mcpClient = new PostgreSQLMCPClient();
    await mcpClient.connect();
    console.log('MCP 客戶端已初始化');
}

// API 端點：建立資料庫連線
app.post('/api/connections', async (req, res) => {
    try {
        const { connection_id, host, database, user, password, port } = req.body;

        const result = await mcpClient.addConnection(connection_id, {
            host, database, user, password, port
        });

        res.json({ success: true, data: result });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// API 端點：執行查詢
app.post('/api/query', async (req, res) => {
    try {
        const { connection_id, query, params } = req.body;

        const result = await mcpClient.executeQuery(connection_id, query, params || []);

        res.json({ success: true, data: result });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// API 端點：獲取表結構
app.get('/api/schema/:connection_id/:table_name', async (req, res) => {
    try {
        const { connection_id, table_name } = req.params;
        const { schema = 'public' } = req.query;

        const result = await mcpClient.getTableSchema(connection_id, table_name, schema);

        res.json({ success: true, data: result });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// API 端點：健康檢查
app.get('/api/health', async (req, res) => {
    try {
        const result = await mcpClient.healthCheck();
        res.json({ success: true, data: result });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// 啟動伺服器
const PORT = process.env.PORT || 3001;

async function startServer() {
    try {
        await initializeMCP();

        app.listen(PORT, () => {
            console.log(`🚀 Web 服務已啟動在 http://localhost:${PORT}`);
            console.log('可用端點:');
            console.log('  POST /api/connections - 建立資料庫連線');
            console.log('  POST /api/query - 執行查詢');
            console.log('  GET /api/schema/:connection_id/:table_name - 獲取表結構');
            console.log('  GET /api/health - 健康檢查');
        });

    } catch (error) {
        console.error('伺服器啟動失敗:', error);
        process.exit(1);
    }
}

startServer();
```

## 🎯 實際應用場景

### 場景 1: 資料庫遷移自動化

```python
# examples/migration_automation.py
"""
自動化資料庫遷移腳本
功能：從舊系統遷移用戶、訂單、產品資料到新系統
"""

class DatabaseMigrationTool:
    def __init__(self, mcp_session):
        self.session = mcp_session

    async def setup_connections(self):
        """建立來源和目標資料庫連線"""
        # 舊系統資料庫
        await self.session.call_tool("add_connection", {
            "connection_id": "legacy_db",
            "host": "old-db.company.com",
            "database": "legacy_system",
            "user": "migration_user",
            "password": "legacy_pass"
        })

        # 新系統資料庫
        await self.session.call_tool("add_connection", {
            "connection_id": "new_db",
            "host": "new-db.company.com",
            "database": "new_system",
            "user": "migration_user",
            "password": "new_pass"
        })

    async def migrate_users(self):
        """遷移用戶資料"""
        print("🔄 開始遷移用戶資料...")

        # 從舊系統提取用戶
        users = await self.session.call_tool("execute_query", {
            "connection_id": "legacy_db",
            "query": """
                SELECT user_id, username, email, first_name, last_name,
                       phone, created_date, last_login
                FROM users
                WHERE active = 1 AND created_date > '2023-01-01'
            """
        })

        # 批次插入新系統
        migration_queries = []
        for user in users.content["rows"]:
            migration_queries.append({
                "query": """
                    INSERT INTO user_profiles (
                        legacy_user_id, username, email, full_name,
                        contact_phone, registration_date, last_activity
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7)
                """,
                "params": [
                    user["user_id"],
                    user["username"],
                    user["email"],
                    f"{user['first_name']} {user['last_name']}",
                    user["phone"],
                    user["created_date"],
                    user["last_login"]
                ]
            })

        result = await self.session.call_tool("execute_transaction", {
            "connection_id": "new_db",
            "queries": migration_queries
        })

        print(f"✅ 用戶遷移完成，處理了 {len(users.content['rows'])} 筆記錄")
        return result
```

### 場景 2: 即時資料分析儀表板

```python
# examples/analytics_dashboard.py
"""
即時資料分析儀表板
功能：連接多個資料源，提供即時業務指標分析
"""

class AnalyticsDashboard:
    def __init__(self, mcp_session):
        self.session = mcp_session

    async def setup_data_sources(self):
        """設定多個資料源"""
        connections = [
            {
                "id": "sales_db",
                "host": "sales-db.company.com",
                "database": "sales_analytics"
            },
            {
                "id": "crm_db",
                "host": "crm-db.company.com",
                "database": "customer_data"
            },
            {
                "id": "inventory_db",
                "host": "inventory-db.company.com",
                "database": "warehouse_mgmt"
            }
        ]

        for conn in connections:
            await self.session.call_tool("add_connection", {
                "connection_id": conn["id"],
                "host": conn["host"],
                "database": conn["database"],
                "user": "analytics_reader",
                "password": "readonly_pass"
            })

    async def get_sales_metrics(self):
        """獲取銷售指標"""
        return await self.session.call_tool("execute_query", {
            "connection_id": "sales_db",
            "query": """
                SELECT
                    DATE(sale_date) as date,
                    COUNT(*) as total_orders,
                    SUM(amount) as total_revenue,
                    AVG(amount) as avg_order_value,
                    COUNT(DISTINCT customer_id) as unique_customers
                FROM sales_transactions
                WHERE sale_date >= CURRENT_DATE - INTERVAL '7 days'
                GROUP BY DATE(sale_date)
                ORDER BY date DESC
            """
        })

    async def get_customer_insights(self):
        """獲取客戶洞察"""
        return await self.session.call_tool("execute_query", {
            "connection_id": "crm_db",
            "query": """
                SELECT
                    customer_segment,
                    COUNT(*) as customer_count,
                    AVG(lifetime_value) as avg_ltv,
                    AVG(satisfaction_score) as avg_satisfaction
                FROM customer_profiles
                WHERE status = 'active'
                GROUP BY customer_segment
                ORDER BY avg_ltv DESC
            """
        })
```

### 場景 3: 資料品質監控

```python
# examples/data_quality_monitor.py
"""
資料品質監控工具
功能：自動檢查資料完整性、一致性和準確性
"""

class DataQualityMonitor:
    def __init__(self, mcp_session):
        self.session = mcp_session
        self.issues_found = []

    async def check_data_completeness(self, connection_id, table_name):
        """檢查資料完整性"""
        print(f"🔍 檢查 {table_name} 表的資料完整性...")

        # 檢查空值
        null_check = await self.session.call_tool("execute_query", {
            "connection_id": connection_id,
            "query": f"""
                SELECT
                    column_name,
                    COUNT(*) as total_rows,
                    COUNT(*) - COUNT(CAST(column_name AS TEXT)) as null_count,
                    ROUND(
                        (COUNT(*) - COUNT(CAST(column_name AS TEXT))) * 100.0 / COUNT(*),
                        2
                    ) as null_percentage
                FROM {table_name}
                CROSS JOIN information_schema.columns
                WHERE table_name = '{table_name}'
                GROUP BY column_name
                HAVING COUNT(*) - COUNT(CAST(column_name AS TEXT)) > 0
            """
        })

        for row in null_check.content["rows"]:
            if row["null_percentage"] > 5:  # 超過5%的空值率
                self.issues_found.append({
                    "type": "high_null_rate",
                    "table": table_name,
                    "column": row["column_name"],
                    "null_rate": row["null_percentage"]
                })

    async def check_data_consistency(self, connection_id):
        """檢查資料一致性"""
        print("🔍 檢查跨表資料一致性...")

        # 檢查外鍵完整性
        fk_check = await self.session.call_tool("execute_query", {
            "connection_id": connection_id,
            "query": """
                -- 檢查訂單表中的客戶ID是否在客戶表中存在
                SELECT COUNT(*) as orphaned_orders
                FROM orders o
                LEFT JOIN customers c ON o.customer_id = c.customer_id
                WHERE c.customer_id IS NULL
            """
        })

        orphaned = fk_check.content["rows"][0]["orphaned_orders"]
        if orphaned > 0:
            self.issues_found.append({
                "type": "referential_integrity",
                "description": f"發現 {orphaned} 筆訂單沒有對應的客戶記錄"
            })

    async def generate_report(self):
        """生成資料品質報告"""
        print("\n📊 資料品質檢查報告")
        print("=" * 50)

        if not self.issues_found:
            print("✅ 沒有發現資料品質問題")
        else:
            print(f"⚠️  發現 {len(self.issues_found)} 個問題:")
            for i, issue in enumerate(self.issues_found, 1):
                print(f"{i}. {issue['type']}: {issue.get('description', '')}")
                if 'table' in issue:
                    print(f"   表: {issue['table']}, 欄位: {issue.get('column', 'N/A')}")

        return self.issues_found
```

## 📚 更多資源

- **📖 完整文件**: [MCP Server 使用手冊](../MCP_SERVER_HANDBOOK.md)
- **🚀 快速開始**: [快速開始指南](../../QUICK_START.md)
- **🐳 Docker 部署**: [Docker Hub 指南](../DOCKER_HUB_GUIDE.md)
- **❓ 常見問題**: [FAQ 文件](../../QA.md)

## 📞 取得協助

- 📧 **Email**: a910413frank@gmail.com
- 🐛 **問題回報**: [GitHub Issues](https://github.com/your-repo/postgresql-mcp-server/issues)
- 💬 **功能討論**: [GitHub Discussions](https://github.com/your-repo/postgresql-mcp-server/discussions)

---

> **💡 提示**: 這些範例提供了完整的 MCP 客戶端整合模式，你可以根據自己的需求修改和擴展。建議先在測試環境中驗證功能，再部署到生產環境。