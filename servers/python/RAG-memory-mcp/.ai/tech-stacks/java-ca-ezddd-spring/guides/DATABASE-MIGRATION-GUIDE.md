# 資料庫遷移指南

## 📋 概述
本指南詳細說明 AI-Plan 系統的資料庫遷移策略和最佳實踐。

## 🎯 遷移策略

### 遷移工具選擇
1. **Flyway** - 生產環境推薦
2. **Liquibase** - 複雜場景
3. **Hibernate DDL** - 僅開發環境

```
開發環境: Hibernate DDL Auto
測試環境: Flyway + Testcontainers
生產環境: Flyway + PostgreSQL
```

## 🛠️ Flyway 設定

### 1. Maven 依賴
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>9.22.0</version>
</dependency>

<!-- PostgreSQL 支援 -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <version>9.22.0</version>
</dependency>
```

### 2. 配置設定
```properties
# application.properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
spring.flyway.out-of-order=false
spring.flyway.clean-disabled=true  # 生產環境必須禁用
# 開發環境配置 (application-dev.properties)
spring.flyway.clean-disabled=false  # 開發環境可以清理

# 生產環境配置 (application-prod.properties)
spring.flyway.clean-disabled=true   # 生產環境禁止清理
spring.flyway.validate-on-migrate=true
```

### 3. 目錄結構
```
src/main/resources/
└── db/
    └── migration/
        ├── V1__Initial_schema.sql
        ├── V2__Add_user_table.sql
        ├── V3__Add_plan_tables.sql
        ├── V4__Add_indexes.sql
        └── V5__Add_event_store.sql
```

## 📝 遷移腳本範例

### V1__Initial_schema.sql
```sql
-- 初始化 Schema
CREATE SCHEMA IF NOT EXISTS aiplan;
SET search_path TO aiplan;

-- 建立基礎設定表
CREATE TABLE IF NOT EXISTS schema_version (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INTEGER,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INTEGER NOT NULL,
    success BOOLEAN NOT NULL,
    PRIMARY KEY (installed_rank)
);

-- 建立序列
CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START WITH 1 INCREMENT BY 1;
```

### V2__Add_user_table.sql
```sql
-- 使用者相關表格
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- 索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- 註解
COMMENT ON TABLE users IS '使用者主表';
COMMENT ON COLUMN users.id IS '使用者唯一識別碼';
COMMENT ON COLUMN users.version IS '樂觀鎖版本號';
```

### V3__Add_plan_tables.sql
```sql
-- 計畫表
CREATE TABLE plan (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 專案表
CREATE TABLE project (
    id VARCHAR(50) PRIMARY KEY,
    plan_id VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    order_index INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_plan FOREIGN KEY (plan_id) REFERENCES plan(id) ON DELETE CASCADE
);

-- 任務表
CREATE TABLE task (
    id VARCHAR(50) PRIMARY KEY,
    plan_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(50) NOT NULL,
    name VARCHAR(500) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    deadline DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_task_plan FOREIGN KEY (plan_id) REFERENCES plan(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
);

-- 標籤表（使用 @ElementCollection）
CREATE TABLE task_tag (
    task_id VARCHAR(50) NOT NULL,
    tag_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (task_id, tag_id),
    CONSTRAINT fk_task_tag_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX idx_plan_user_id ON plan(user_id);
CREATE INDEX idx_project_plan_id ON project(plan_id);
CREATE INDEX idx_task_plan_id ON task(plan_id);
CREATE INDEX idx_task_project_id ON task(project_id);
CREATE INDEX idx_task_deadline ON task(deadline) WHERE deadline IS NOT NULL;
CREATE INDEX idx_task_completed ON task(completed);
```

### V4__Add_performance_indexes.sql
```sql
-- 複合索引優化查詢效能
CREATE INDEX idx_task_plan_completed ON task(plan_id, completed);
CREATE INDEX idx_task_user_deadline ON task(plan_id, deadline) 
    WHERE deadline IS NOT NULL AND completed = FALSE;

-- 部分索引減少索引大小
CREATE INDEX idx_active_plans ON plan(user_id) WHERE status = 'ACTIVE';

-- 覆蓋索引
CREATE INDEX idx_task_covering ON task(plan_id, project_id, completed) 
    INCLUDE (name, deadline);
```

### V5__Add_event_store.sql
```sql
-- Event Store 表格
CREATE TABLE domain_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(50) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    event_version INTEGER NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

-- 索引
CREATE INDEX idx_event_aggregate ON domain_event(aggregate_id, event_version);
CREATE INDEX idx_event_type ON domain_event(event_type);
CREATE INDEX idx_event_occurred ON domain_event(occurred_at);

-- Event Snapshot 表格
CREATE TABLE event_snapshot (
    aggregate_id VARCHAR(50) PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    snapshot_data JSONB NOT NULL,
    version INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 🔄 遷移程序

### 1. 開發環境遷移
```bash
# 清理並重新建立（危險！僅開發環境）
mvn flyway:clean flyway:migrate

# 檢查遷移狀態
mvn flyway:info

# 驗證遷移
mvn flyway:validate

# 修復失敗的遷移
mvn flyway:repair
```

### 2. 生產環境遷移流程

#### 遷移前檢查清單
```bash
#!/bin/bash
# pre-migration-check.sh

echo "=== 遷移前檢查 ==="

# 1. 備份資料庫
echo "1. 執行資料庫備份..."
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. 檢查連線數
echo "2. 檢查資料庫連線..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT count(*) FROM pg_stat_activity;"

# 3. 檢查磁碟空間
echo "3. 檢查磁碟空間..."
df -h | grep postgres

# 4. 驗證遷移腳本
echo "4. 驗證遷移腳本..."
mvn flyway:validate -Dflyway.url=$DB_URL

echo "=== 檢查完成 ==="
```

#### 執行遷移
```bash
#!/bin/bash
# execute-migration.sh

set -e  # 遇到錯誤立即停止

# 設定變數
DB_URL="jdbc:postgresql://localhost:5432/aiplan"
MIGRATION_USER="migration_user"

# 1. 進入維護模式
echo "啟用維護模式..."
touch /var/www/maintenance.flag

# 2. 等待現有請求完成
echo "等待 30 秒讓現有請求完成..."
sleep 30

# 3. 執行遷移
echo "開始執行資料庫遷移..."
mvn flyway:migrate \
    -Dflyway.url=$DB_URL \
    -Dflyway.user=$MIGRATION_USER \
    -Dflyway.password=$MIGRATION_PASS

# 4. 驗證遷移結果
echo "驗證遷移結果..."
mvn flyway:info -Dflyway.url=$DB_URL

# 5. 退出維護模式
echo "退出維護模式..."
rm /var/www/maintenance.flag

echo "遷移完成！"
```

### 3. 回滾策略

#### 使用 Undo 腳本
```sql
-- U5__Add_event_store.sql (回滾腳本)
DROP TABLE IF EXISTS event_snapshot;
DROP TABLE IF EXISTS domain_event;
```

#### 手動回滾程序
```bash
#!/bin/bash
# rollback-migration.sh

# 1. 停止應用
sudo systemctl stop aiplan

# 2. 執行回滾 SQL
psql -h $DB_HOST -U $DB_USER -d $DB_NAME < rollback_V5.sql

# 3. 更新 Flyway 歷史
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "
DELETE FROM flyway_schema_history 
WHERE version = '5';
"

# 4. 重啟應用（使用舊版本）
sudo systemctl start aiplan

echo "回滾完成"
```

## 🎨 最佳實踐

### 1. 命名規範
```
V{版本號}__{描述}.sql

範例：
V1__Initial_schema.sql
V2__Add_user_table.sql
V3.1__Fix_user_constraints.sql
```

### 2. 腳本編寫原則
```sql
-- ✅ 好的做法
-- 1. 使用 IF NOT EXISTS
CREATE TABLE IF NOT EXISTS users (...);

-- 2. 明確指定 Schema
CREATE TABLE aiplan.users (...);

-- 3. 加入註解
COMMENT ON TABLE users IS '使用者主表';

-- 4. 考慮並發
CREATE INDEX CONCURRENTLY idx_users_email ON users(email);

-- ❌ 避免的做法
-- 1. 不要使用 DROP 不帶 IF EXISTS
DROP TABLE users;  -- 危險！

-- 2. 不要修改已執行的遷移
-- 3. 不要在遷移中包含資料操作（除非必要）
```

### 3. 資料遷移策略

#### 大量資料遷移
```sql
-- V6__Migrate_large_data.sql
-- 使用批次處理避免鎖表
DO $$
DECLARE
    batch_size INTEGER := 1000;
    offset_val INTEGER := 0;
    total_rows INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_rows FROM old_table;
    
    WHILE offset_val < total_rows LOOP
        INSERT INTO new_table (col1, col2)
        SELECT col1, col2 
        FROM old_table
        ORDER BY id
        LIMIT batch_size
        OFFSET offset_val;
        
        offset_val := offset_val + batch_size;
        
        -- 避免長時間鎖定
        PERFORM pg_sleep(0.1);
    END LOOP;
END $$;
```

#### 零停機遷移
```sql
-- 步驟 1: 新增欄位（允許 NULL）
ALTER TABLE users ADD COLUMN new_field VARCHAR(100);

-- 步驟 2: 回填資料（應用同時寫入兩個欄位）
UPDATE users SET new_field = old_field WHERE new_field IS NULL;

-- 步驟 3: 加入 NOT NULL 約束（確認資料完整後）
ALTER TABLE users ALTER COLUMN new_field SET NOT NULL;

-- 步驟 4: 移除舊欄位（確認應用不再使用後）
ALTER TABLE users DROP COLUMN old_field;
```

## 📊 監控和維護

### 1. 遷移監控
```sql
-- 查看遷移歷史
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;

-- 檢查失敗的遷移
SELECT * FROM flyway_schema_history WHERE success = false;

-- 統計遷移執行時間
SELECT 
    version,
    description,
    execution_time,
    installed_on
FROM flyway_schema_history
WHERE execution_time > 1000  -- 超過 1 秒
ORDER BY execution_time DESC;
```

### 2. 健康檢查
```java
@Component
public class DatabaseMigrationHealthIndicator extends AbstractHealthIndicator {
    
    @Autowired
    private Flyway flyway;
    
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            MigrationInfoService info = flyway.info();
            MigrationInfo[] pending = info.pending();
            
            if (pending.length > 0) {
                builder.status("WARNING")
                    .withDetail("pending_migrations", pending.length);
            } else {
                builder.up()
                    .withDetail("applied_migrations", info.applied().length);
            }
        } catch (Exception e) {
            builder.down(e);
        }
    }
}
```

## 🚨 緊急情況處理

### 1. 遷移失敗處理
```bash
# 1. 檢查錯誤
psql -c "SELECT * FROM flyway_schema_history WHERE success = false;"

# 2. 手動修復問題
# 修正 SQL 錯誤或資料問題

# 3. 標記修復
mvn flyway:repair

# 4. 重試遷移
mvn flyway:migrate
```

### 2. 資料損壞恢復
```bash
# 從備份恢復
psql -h localhost -U postgres -c "DROP DATABASE aiplan;"
psql -h localhost -U postgres -c "CREATE DATABASE aiplan;"
psql -h localhost -U postgres -d aiplan < backup.sql
```

## 📋 檢查清單

### 遷移前
- [ ] 完整備份資料庫
- [ ] 在測試環境驗證
- [ ] 檢查磁碟空間
- [ ] 準備回滾計畫
- [ ] 通知相關團隊

### 遷移中
- [ ] 監控執行進度
- [ ] 檢查錯誤日誌
- [ ] 驗證資料完整性

### 遷移後
- [ ] 執行健康檢查
- [ ] 驗證應用功能
- [ ] 監控效能指標
- [ ] 保留備份至少 7 天

---

⚡ **記住**：資料庫遷移是高風險操作，務必謹慎執行並做好充分準備！