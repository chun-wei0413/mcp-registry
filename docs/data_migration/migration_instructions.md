# Kanban 資料遷移執行指南

根據 `kanban_migration.md` 的設計，此指南提供完整的執行步驟來實現 old_kanban_data 的智能遷移。

## 🎯 執行前準備

### 1. 環境檢查

確保以下服務正常運行：
- ✅ PostgreSQL MCP Server (port 3000)
- ✅ MySQL MCP Server (port 3001)
- ✅ PostgreSQL 容器 (已部署的新系統)
- ✅ MySQL 容器 (載入 old_kanban_data)

### 2. 連線設定

```bash
# PostgreSQL 連線 (目標系統)
host: localhost
port: 5432
database: your_target_database
user: your_pg_user
password: your_pg_password

# MySQL 連線 (舊系統)
host: localhost
port: 3306
database: old_kanban_data
user: migration_user
password: migration_pass
```

## 🚀 執行流程

### Phase 1: 環境準備

使用 LLM 與兩個 MCP Server 建立連線：

```python
# 建立 PostgreSQL 連線
await pg_mcp.add_connection(
    connection_id="target_pg",
    host="localhost",
    port=5432,
    database="your_target_database",
    user="your_pg_user",
    password="your_pg_password"
)

# 建立 MySQL 連線
await mysql_mcp.add_connection(
    connection_id="old_kanban",
    host="localhost",
    port=3306,
    database="old_kanban_data",
    user="migration_user",
    password="migration_pass"
)

# 驗證連線
pg_health = await pg_mcp.health_check("target_pg")
mysql_health = await mysql_mcp.health_check("old_kanban")
```

### Phase 2: 架構分析

#### 2.1 分析舊系統架構

```python
# 獲取 old_kanban_data 所有表
mysql_tables = await mysql_mcp.list_tables("old_kanban", "old_kanban_data")

# 分析每個表的結構
mysql_schemas = {}
for table in mysql_tables["tables"]:
    schema = await mysql_mcp.get_table_schema(
        "old_kanban", table, "old_kanban_data"
    )
    mysql_schemas[table] = schema

    # 取樣資料分析
    sample_data = await mysql_mcp.execute_query(
        "old_kanban",
        f"SELECT * FROM {table} LIMIT 5"
    )
```

#### 2.2 分析新系統架構

```python
# 獲取 PostgreSQL 目標表
pg_tables = await pg_mcp.list_tables("target_pg", "public")

# 分析每個表的結構
pg_schemas = {}
for table in pg_tables["tables"]:
    schema = await pg_mcp.get_table_schema(
        "target_pg", table, "public"
    )
    pg_schemas[table] = schema
```

#### 2.3 生成架構差異報告

基於分析結果，LLM 需要生成：

1. **表映射關係**：舊表 → 新表
2. **欄位映射關係**：舊欄位 → 新欄位
3. **資料類型轉換**：MySQL → PostgreSQL
4. **業務邏輯變更**：State Sourcing 適配
5. **遷移順序**：依據外鍵關係排序

### Phase 3: 遷移執行

#### 3.1 State Sourcing 優先遷移

按照以下順序執行遷移（State Sourcing 優先）：

```python
migration_order = [
    "users",      # 基礎用戶資料
    "projects",   # 專案資料
    "boards",     # 看板資料
    "lists",      # 列表資料
    "cards",      # 卡片資料（核心業務狀態）
    "comments",   # 評論資料
    "attachments" # 附件資料
]
```

#### 3.2 批次遷移範例

```python
# 批次遷移用戶資料
async def migrate_users():
    # 獲取總數
    count_result = await mysql_mcp.execute_query(
        "old_kanban",
        "SELECT COUNT(*) as total FROM users"
    )
    total_users = count_result["rows"][0]["total"]

    batch_size = 200
    offset = 0

    while offset < total_users:
        # 讀取批次資料
        mysql_data = await mysql_mcp.execute_query(
            "old_kanban",
            f"SELECT * FROM users LIMIT {batch_size} OFFSET {offset}"
        )

        # 轉換資料格式
        transformed_users = []
        for user in mysql_data["rows"]:
            transformed_user = {
                "username": user["username"],
                "email": user["email"],
                "password_hash": user["password_hash"],
                "full_name": user["full_name"],
                "is_active": bool(user["is_active"]),
                "created_at": user["created_at"],
                "updated_at": user["updated_at"]
            }
            transformed_users.append(transformed_user)

        # 批次插入到 PostgreSQL
        if transformed_users:
            insert_queries = []
            for user in transformed_users:
                query = {
                    "query": """
                        INSERT INTO users (username, email, password_hash, full_name, is_active, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7)
                    """,
                    "params": [
                        user["username"], user["email"], user["password_hash"],
                        user["full_name"], user["is_active"],
                        user["created_at"], user["updated_at"]
                    ]
                }
                insert_queries.append(query)

            # 事務性執行
            result = await pg_mcp.execute_transaction("target_pg", insert_queries)

            if result["success"]:
                print(f"Successfully migrated {len(transformed_users)} users")
            else:
                print(f"Failed to migrate batch: {result['error']}")

        offset += batch_size
```

#### 3.3 智能轉換邏輯

LLM 需要處理以下轉換：

1. **時間戳格式轉換**
   ```python
   # MySQL TIMESTAMP → PostgreSQL TIMESTAMPTZ
   mysql_time = "2024-01-01 12:00:00"
   pg_time = f"{mysql_time}+00:00"  # 加上時區
   ```

2. **布林值轉換**
   ```python
   # MySQL TINYINT(1) → PostgreSQL BOOLEAN
   mysql_bool = 1
   pg_bool = bool(mysql_bool)
   ```

3. **外鍵關係維護**
   ```python
   # 確保參照完整性
   # 先遷移父表，再遷移子表
   ```

### Phase 4: 驗證檢查

#### 4.1 資料一致性驗證

```python
async def verify_migration():
    verification_results = {}

    for table in migration_order:
        # 比較記錄數量
        mysql_count = await mysql_mcp.execute_query(
            "old_kanban",
            f"SELECT COUNT(*) as count FROM {table}"
        )

        pg_count = await pg_mcp.execute_query(
            "target_pg",
            f"SELECT COUNT(*) as count FROM {table}"
        )

        mysql_total = mysql_count["rows"][0]["count"]
        pg_total = pg_count["rows"][0]["count"]

        verification_results[table] = {
            "mysql_records": mysql_total,
            "postgresql_records": pg_total,
            "match": mysql_total == pg_total
        }

        # 抽樣驗證資料內容
        if mysql_total > 0:
            mysql_sample = await mysql_mcp.execute_query(
                "old_kanban",
                f"SELECT * FROM {table} ORDER BY id LIMIT 3"
            )

            pg_sample = await pg_mcp.execute_query(
                "target_pg",
                f"SELECT * FROM {table} ORDER BY id LIMIT 3"
            )

            # 比較樣本資料（由 LLM 智能比較）

    return verification_results
```

#### 4.2 業務邏輯驗證

```python
# 驗證關鍵業務邏輯
async def verify_business_logic():
    # 驗證用戶-專案關係
    user_project_check = await pg_mcp.execute_query(
        "target_pg",
        """
        SELECT u.username, COUNT(p.id) as project_count
        FROM users u
        LEFT JOIN projects p ON u.id = p.owner_id
        GROUP BY u.id, u.username
        ORDER BY u.id
        LIMIT 10
        """
    )

    # 驗證卡片-列表關係
    card_list_check = await pg_mcp.execute_query(
        "target_pg",
        """
        SELECT l.name as list_name, COUNT(c.id) as card_count
        FROM lists l
        LEFT JOIN cards c ON l.id = c.list_id
        GROUP BY l.id, l.name
        ORDER BY l.id
        """
    )

    return {
        "user_projects": user_project_check["rows"],
        "list_cards": card_list_check["rows"]
    }
```

## 📊 遷移報告

### 成功指標

- ✅ **完整性**: 所有核心表 100% 遷移成功
- ✅ **準確性**: 抽樣驗證通過，資料一致
- ✅ **效率**: 批次處理，遷移時間合理
- ✅ **可靠性**: 事務回滾機制正常運作

### 報告範例

```json
{
  "migration_summary": {
    "start_time": "2024-01-01T10:00:00Z",
    "end_time": "2024-01-01T10:15:00Z",
    "duration_seconds": 900,
    "success": true
  },
  "data_summary": {
    "total_tables": 7,
    "total_records": 1395,
    "successful_records": 1390,
    "failed_records": 5,
    "success_rate_percent": 99.6
  },
  "table_details": {
    "users": {"migrated": 150, "failed": 0},
    "projects": {"migrated": 45, "failed": 0},
    "cards": {"migrated": 1200, "failed": 5}
  },
  "recommendations": [
    "Migration completed successfully!",
    "5 failed records need manual review",
    "Consider Event Sourcing migration for historical data"
  ]
}
```

## 🎯 執行建議

### 1. 準備階段
- 在非生產環境先執行測試遷移
- 備份所有相關資料
- 準備回滾計畫

### 2. 執行階段
- 監控系統資源使用情況
- 即時記錄遷移日誌
- 分批次執行，避免長時間鎖定

### 3. 驗證階段
- 執行完整的資料一致性檢查
- 進行業務邏輯功能測試
- 產生詳細的遷移報告

### 4. 後續處理
- 處理失敗的記錄
- 考慮 Event Sourcing 歷史資料遷移
- 更新應用程式配置指向新資料庫

---

**重要提醒**: 此遷移過程需要 LLM 具備對兩套系統架構的深度理解，並能智能地處理資料轉換和映射邏輯。建議在執行前充分測試遷移邏輯的正確性。