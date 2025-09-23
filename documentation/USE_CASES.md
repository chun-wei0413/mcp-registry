# PostgreSQL MCP Server - 常見使用場景 🎯

本文件展示 PostgreSQL MCP Server 在實際工作中的典型應用場景和最佳實務。

## 📋 場景分類

- [資料遷移與轉換](#-資料遷移與轉換)
- [資料分析與報表](#-資料分析與報表)
- [資料庫管理與維運](#-資料庫管理與維運)
- [開發與測試支援](#-開發與測試支援)
- [監控與警報](#-監控與警報)
- [商業智慧應用](#-商業智慧應用)

## 🔄 資料遷移與轉換

### 場景 1.1: 系統升級資料遷移

**背景**: 公司要從舊的 ERP 系統遷移到新系統，需要將 10 萬筆客戶資料和 50 萬筆交易紀錄遷移過去。

**挑戰**:
- 資料結構不同
- 需要資料清理和轉換
- 確保資料完整性
- 最小化停機時間

**解決方案**:

```python
# 使用 MCP 工具進行智能資料遷移
async def migrate_erp_data():
    # 1. 分析來源和目標表結構
    old_customers = await get_table_schema("old_erp", "customers")
    new_customers = await get_table_schema("new_erp", "customer_profiles")

    # 2. 批次提取和轉換資料
    batch_size = 1000
    offset = 0

    while True:
        # 分批提取資料
        source_data = await execute_query(
            "old_erp",
            """SELECT customer_id, company_name, contact_person,
                      phone, email, address, registration_date
               FROM customers
               ORDER BY customer_id
               LIMIT $1 OFFSET $2""",
            [batch_size, offset]
        )

        if not source_data["rows"]:
            break

        # 資料轉換和清理
        transformed_data = []
        for customer in source_data["rows"]:
            transformed_data.append({
                "query": """
                    INSERT INTO customer_profiles (
                        legacy_id, business_name, primary_contact,
                        contact_phone, email_address, business_address,
                        onboarding_date, data_source
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7, 'ERP_MIGRATION')
                """,
                "params": [
                    customer["customer_id"],
                    customer["company_name"].strip().title(),
                    customer["contact_person"],
                    clean_phone(customer["phone"]),
                    customer["email"].lower(),
                    customer["address"],
                    customer["registration_date"]
                ]
            })

        # 事務性批次插入
        result = await execute_transaction("new_erp", transformed_data)
        print(f"✅ 已遷移 {len(transformed_data)} 筆客戶資料")

        offset += batch_size
```

**優勢**:
- ✅ 自動化資料轉換
- ✅ 事務保證資料一致性
- ✅ 批次處理提升效能
- ✅ LLM 智能處理資料品質問題

### 場景 1.2: 多資料源資料整合

**背景**: 將來自 CRM、ERP、電商平台的客戶資料整合到統一的客戶資料平台。

```python
async def integrate_customer_data():
    # 連接多個資料源
    data_sources = {
        "crm": {"host": "crm-db.company.com", "database": "salesforce"},
        "erp": {"host": "erp-db.company.com", "database": "sap_data"},
        "ecommerce": {"host": "shop-db.company.com", "database": "magento"}
    }

    for source_id, config in data_sources.items():
        await add_connection(source_id, **config)

    # 統一客戶資料格式
    unified_customers = []

    # 從 CRM 提取潛在客戶
    crm_leads = await execute_query("crm", """
        SELECT id, first_name, last_name, email, phone, company,
               created_date, lead_source, status
        FROM leads WHERE status IN ('qualified', 'converted')
    """)

    # 從 ERP 提取現有客戶
    erp_customers = await execute_query("erp", """
        SELECT customer_code, company_name, contact_email,
               contact_phone, industry, annual_revenue
        FROM customers WHERE active = true
    """)

    # 從電商平台提取線上客戶
    ecommerce_users = await execute_query("ecommerce", """
        SELECT user_id, email, first_name, last_name,
               total_orders, total_spent, last_order_date
        FROM customer_summary WHERE total_orders > 0
    """)

    # 智能資料去重和整合（由 LLM 處理複雜邏輯）
    # LLM 可以基於 email、phone、company name 進行智能匹配
```

## 📊 資料分析與報表

### 場景 2.1: 即時業務儀表板

**背景**: 管理團隊需要即時監控銷售績效、客戶活動和庫存狀況。

```python
async def generate_business_dashboard():
    # 銷售績效指標
    sales_metrics = await execute_query("analytics", """
        WITH daily_sales AS (
            SELECT DATE(order_date) as sale_date,
                   COUNT(*) as orders_count,
                   SUM(total_amount) as revenue,
                   COUNT(DISTINCT customer_id) as unique_customers
            FROM orders
            WHERE order_date >= CURRENT_DATE - INTERVAL '30 days'
            GROUP BY DATE(order_date)
        )
        SELECT
            sale_date,
            orders_count,
            revenue,
            unique_customers,
            revenue / NULLIF(orders_count, 0) as avg_order_value,
            LAG(revenue) OVER (ORDER BY sale_date) as prev_day_revenue
        FROM daily_sales
        ORDER BY sale_date DESC
        LIMIT 30
    """)

    # 客戶分析
    customer_insights = await execute_query("analytics", """
        SELECT
            customer_segment,
            COUNT(*) as customer_count,
            AVG(lifetime_value) as avg_ltv,
            SUM(CASE WHEN last_order_date > CURRENT_DATE - INTERVAL '30 days'
                THEN 1 ELSE 0 END) as active_customers,
            AVG(satisfaction_score) as avg_satisfaction
        FROM customer_profiles
        GROUP BY customer_segment
        ORDER BY avg_ltv DESC
    """)

    # 庫存警報
    inventory_alerts = await execute_query("warehouse", """
        SELECT
            product_code,
            product_name,
            current_stock,
            reorder_level,
            supplier_name,
            lead_time_days,
            CASE
                WHEN current_stock <= 0 THEN 'OUT_OF_STOCK'
                WHEN current_stock <= reorder_level THEN 'LOW_STOCK'
                ELSE 'NORMAL'
            END as stock_status
        FROM inventory_view
        WHERE current_stock <= reorder_level * 1.2
        ORDER BY
            CASE stock_status
                WHEN 'OUT_OF_STOCK' THEN 1
                WHEN 'LOW_STOCK' THEN 2
                ELSE 3
            END,
            current_stock ASC
    """)

    return {
        "sales_performance": sales_metrics["rows"],
        "customer_insights": customer_insights["rows"],
        "inventory_alerts": inventory_alerts["rows"],
        "generated_at": datetime.utcnow().isoformat()
    }
```

### 場景 2.2: 深度資料探索分析

**背景**: 行銷團隊想了解不同客戶群體的購買行為模式，以優化行銷策略。

```python
async def customer_behavior_analysis():
    # RFM 分析 (Recency, Frequency, Monetary)
    rfm_analysis = await execute_query("marketing", """
        WITH customer_rfm AS (
            SELECT
                customer_id,
                EXTRACT(DAYS FROM CURRENT_DATE - MAX(order_date)) as recency_days,
                COUNT(DISTINCT order_id) as frequency,
                SUM(total_amount) as monetary_value
            FROM orders
            WHERE order_date >= CURRENT_DATE - INTERVAL '365 days'
            GROUP BY customer_id
        ),
        rfm_scores AS (
            SELECT *,
                NTILE(5) OVER (ORDER BY recency_days DESC) as recency_score,
                NTILE(5) OVER (ORDER BY frequency) as frequency_score,
                NTILE(5) OVER (ORDER BY monetary_value) as monetary_score
            FROM customer_rfm
        )
        SELECT
            recency_score,
            frequency_score,
            monetary_score,
            COUNT(*) as customer_count,
            AVG(monetary_value) as avg_value,
            CASE
                WHEN recency_score >= 4 AND frequency_score >= 4 THEN 'Champions'
                WHEN recency_score >= 3 AND frequency_score >= 3 THEN 'Loyal Customers'
                WHEN recency_score >= 4 AND frequency_score <= 2 THEN 'New Customers'
                WHEN recency_score <= 2 AND frequency_score >= 3 THEN 'At Risk'
                WHEN recency_score <= 2 AND frequency_score <= 2 THEN 'Lost Customers'
                ELSE 'Regular Customers'
            END as customer_segment
        FROM rfm_scores
        GROUP BY recency_score, frequency_score, monetary_score
        ORDER BY recency_score DESC, frequency_score DESC, monetary_score DESC
    """)

    # 產品關聯分析
    product_affinity = await execute_query("marketing", """
        WITH product_pairs AS (
            SELECT
                oi1.product_id as product_a,
                oi2.product_id as product_b,
                COUNT(DISTINCT oi1.order_id) as cooccurrence_count
            FROM order_items oi1
            JOIN order_items oi2 ON oi1.order_id = oi2.order_id
                AND oi1.product_id < oi2.product_id
            JOIN orders o ON oi1.order_id = o.order_id
            WHERE o.order_date >= CURRENT_DATE - INTERVAL '180 days'
            GROUP BY oi1.product_id, oi2.product_id
            HAVING COUNT(DISTINCT oi1.order_id) >= 10
        )
        SELECT
            pa.name as product_a_name,
            pb.name as product_b_name,
            pp.cooccurrence_count,
            ROUND(
                pp.cooccurrence_count * 100.0 /
                (SELECT COUNT(DISTINCT order_id) FROM order_items WHERE product_id = pp.product_a),
                2
            ) as lift_percentage
        FROM product_pairs pp
        JOIN products pa ON pp.product_a = pa.product_id
        JOIN products pb ON pp.product_b = pb.product_id
        ORDER BY pp.cooccurrence_count DESC
        LIMIT 20
    """)
```

## 🛠️ 資料庫管理與維運

### 場景 3.1: 自動化資料庫健康檢查

**背景**: DBA 團隊需要每日自動檢查多個生產資料庫的健康狀況。

```python
async def database_health_monitoring():
    databases = [
        {"id": "prod_app", "host": "prod-db-01.company.com"},
        {"id": "prod_analytics", "host": "analytics-db.company.com"},
        {"id": "prod_warehouse", "host": "dw-db.company.com"}
    ]

    health_report = {"timestamp": datetime.utcnow(), "databases": []}

    for db_config in databases:
        db_id = db_config["id"]

        try:
            # 連線測試
            conn_test = await test_connection(db_id)

            # 系統指標檢查
            system_metrics = await execute_query(db_id, """
                SELECT
                    current_database(),
                    pg_database_size(current_database()) / 1024 / 1024 / 1024.0 as db_size_gb,
                    (SELECT count(*) FROM pg_stat_activity WHERE state = 'active') as active_connections,
                    (SELECT count(*) FROM pg_stat_activity) as total_connections,
                    (SELECT setting::int FROM pg_settings WHERE name = 'max_connections') as max_connections
            """)

            # 鎖等待檢查
            lock_waits = await execute_query(db_id, """
                SELECT
                    blocked_locks.pid AS blocked_pid,
                    blocked_activity.usename AS blocked_user,
                    blocking_locks.pid AS blocking_pid,
                    blocking_activity.usename AS blocking_user,
                    blocked_activity.query AS blocked_statement,
                    blocking_activity.query AS current_statement_in_blocking_process
                FROM pg_catalog.pg_locks blocked_locks
                JOIN pg_catalog.pg_stat_activity blocked_activity
                    ON blocked_activity.pid = blocked_locks.pid
                JOIN pg_catalog.pg_locks blocking_locks
                    ON blocking_locks.locktype = blocked_locks.locktype
                    AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
                    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
                    AND blocking_locks.pid != blocked_locks.pid
                JOIN pg_catalog.pg_stat_activity blocking_activity
                    ON blocking_activity.pid = blocking_locks.pid
                WHERE NOT blocked_locks.granted
            """)

            # 慢查詢檢查
            slow_queries = await execute_query(db_id, """
                SELECT
                    query,
                    calls,
                    total_time,
                    mean_time,
                    stddev_time,
                    rows
                FROM pg_stat_statements
                WHERE mean_time > 1000
                ORDER BY mean_time DESC
                LIMIT 10
            """)

            db_health = {
                "database": db_id,
                "status": "healthy" if conn_test["status"] == "connected" else "unhealthy",
                "metrics": system_metrics["rows"][0],
                "lock_waits": len(lock_waits["rows"]),
                "slow_queries_count": len(slow_queries["rows"]),
                "issues": []
            }

            # 健康檢查邏輯
            metrics = system_metrics["rows"][0]
            if metrics["active_connections"] > metrics["max_connections"] * 0.8:
                db_health["issues"].append("High connection usage")

            if metrics["db_size_gb"] > 100:  # 超過 100GB
                db_health["issues"].append("Large database size")

            if len(lock_waits["rows"]) > 0:
                db_health["issues"].append(f"{len(lock_waits['rows'])} lock waits detected")

            health_report["databases"].append(db_health)

        except Exception as e:
            health_report["databases"].append({
                "database": db_id,
                "status": "error",
                "error": str(e)
            })

    return health_report
```

### 場景 3.2: 自動化資料庫維護

**背景**: 定期執行資料庫維護任務，如統計資訊更新、索引重建、資料清理。

```python
async def automated_maintenance():
    maintenance_tasks = []

    # 更新統計資訊
    stats_update = await execute_query("maintenance_db", """
        SELECT schemaname, tablename, n_dead_tup, n_live_tup,
               CASE WHEN n_live_tup > 0
                    THEN n_dead_tup::float / n_live_tup::float
                    ELSE 0 END as dead_ratio
        FROM pg_stat_user_tables
        WHERE n_dead_tup > 1000 AND
              (n_dead_tup::float / GREATEST(n_live_tup, 1)::float) > 0.1
    """)

    # 對需要的表執行 VACUUM ANALYZE
    for table_info in stats_update["rows"]:
        schema = table_info["schemaname"]
        table = table_info["tablename"]

        await execute_query("maintenance_db",
            f"VACUUM ANALYZE {schema}.{table}")

        maintenance_tasks.append(f"Vacuumed {schema}.{table}")

    # 檢查並重建碎片化索引
    fragmented_indexes = await execute_query("maintenance_db", """
        SELECT
            schemaname,
            tablename,
            indexname,
            pg_relation_size(indexrelid) as index_size,
            pg_stat_get_blocks_fetched(indexrelid) -
            pg_stat_get_blocks_hit(indexrelid) as blocks_read
        FROM pg_stat_user_indexes
        WHERE pg_relation_size(indexrelid) > 100 * 1024 * 1024  -- 100MB+
        ORDER BY blocks_read DESC
        LIMIT 10
    """)

    # 清理過期資料
    cleanup_result = await execute_transaction("maintenance_db", [
        {
            "query": "DELETE FROM audit_logs WHERE created_at < $1",
            "params": [datetime.utcnow() - timedelta(days=90)]
        },
        {
            "query": "DELETE FROM session_tokens WHERE expires_at < $1",
            "params": [datetime.utcnow()]
        }
    ])

    maintenance_tasks.append(f"Cleaned up old data: {cleanup_result['affected_rows']} rows")

    return maintenance_tasks
```

## 🚀 開發與測試支援

### 場景 4.1: 測試資料生成

**背景**: 開發團隊需要生成擬真的測試資料來進行功能測試和效能測試。

```python
async def generate_test_data():
    # 生成測試客戶
    test_customers = []
    for i in range(1000):
        test_customers.append({
            "query": """
                INSERT INTO customers (name, email, phone, company, industry, created_at)
                VALUES ($1, $2, $3, $4, $5, $6)
            """,
            "params": [
                f"Test Customer {i+1}",
                f"customer{i+1}@testdomain.com",
                f"+1-555-{1000+i:04d}",
                f"Test Company {i+1}",
                random.choice(["Technology", "Healthcare", "Finance", "Retail", "Manufacturing"]),
                datetime.utcnow() - timedelta(days=random.randint(1, 365))
            ]
        })

    # 批次插入客戶資料
    await execute_transaction("test_db", test_customers)

    # 生成測試訂單
    customers = await execute_query("test_db", "SELECT customer_id FROM customers")

    test_orders = []
    for _ in range(5000):
        customer_id = random.choice(customers["rows"])["customer_id"]
        order_date = datetime.utcnow() - timedelta(days=random.randint(1, 180))

        test_orders.append({
            "query": """
                INSERT INTO orders (customer_id, order_date, total_amount, status)
                VALUES ($1, $2, $3, $4)
            """,
            "params": [
                customer_id,
                order_date,
                round(random.uniform(50, 5000), 2),
                random.choice(["pending", "processing", "shipped", "delivered", "cancelled"])
            ]
        })

    await execute_transaction("test_db", test_orders)

    print("✅ 測試資料生成完成：1000 個客戶，5000 筆訂單")
```

### 場景 4.2: 資料庫 Schema 比較

**背景**: 確保開發、測試和生產環境的資料庫結構一致性。

```python
async def compare_database_schemas():
    environments = ["dev_db", "test_db", "prod_db"]
    schema_comparison = {}

    for env in environments:
        # 獲取所有表的結構
        tables_info = await execute_query(env, """
            SELECT
                table_name,
                column_name,
                data_type,
                is_nullable,
                column_default,
                ordinal_position
            FROM information_schema.columns
            WHERE table_schema = 'public'
            ORDER BY table_name, ordinal_position
        """)

        # 獲取索引資訊
        indexes_info = await execute_query(env, """
            SELECT
                tablename,
                indexname,
                indexdef
            FROM pg_indexes
            WHERE schemaname = 'public'
            ORDER BY tablename, indexname
        """)

        schema_comparison[env] = {
            "tables": tables_info["rows"],
            "indexes": indexes_info["rows"]
        }

    # 比較差異（這裡 LLM 可以智能分析差異）
    differences = []

    # 比較表結構
    dev_tables = {(t["table_name"], t["column_name"]): t for t in schema_comparison["dev_db"]["tables"]}
    prod_tables = {(t["table_name"], t["column_name"]): t for t in schema_comparison["prod_db"]["tables"]}

    # 找出缺失的欄位
    missing_in_prod = set(dev_tables.keys()) - set(prod_tables.keys())
    missing_in_dev = set(prod_tables.keys()) - set(dev_tables.keys())

    if missing_in_prod:
        differences.append(f"Production missing columns: {missing_in_prod}")
    if missing_in_dev:
        differences.append(f"Development has extra columns: {missing_in_dev}")

    return {
        "schemas": schema_comparison,
        "differences": differences,
        "sync_required": len(differences) > 0
    }
```

## 📈 監控與警報

### 場景 5.1: 即時效能監控

**背景**: 監控資料庫效能指標，在出現異常時及時警報。

```python
async def performance_monitoring():
    # 收集效能指標
    performance_metrics = await execute_query("monitor_db", """
        SELECT
            -- 連線資訊
            (SELECT count(*) FROM pg_stat_activity WHERE state = 'active') as active_connections,
            (SELECT count(*) FROM pg_stat_activity WHERE state = 'idle in transaction') as idle_in_transaction,

            -- 查詢效能
            (SELECT avg(query_start - xact_start) FROM pg_stat_activity
             WHERE state = 'active' AND query_start IS NOT NULL) as avg_query_duration,

            -- 鎖資訊
            (SELECT count(*) FROM pg_locks WHERE NOT granted) as waiting_locks,

            -- I/O 統計
            (SELECT sum(blks_read) FROM pg_stat_database) as total_blocks_read,
            (SELECT sum(blks_hit) FROM pg_stat_database) as total_blocks_hit,

            -- 資料庫大小
            pg_database_size(current_database()) / 1024 / 1024 / 1024.0 as db_size_gb
    """)

    metrics = performance_metrics["rows"][0]

    # 警報閾值檢查
    alerts = []

    if metrics["active_connections"] > 80:
        alerts.append({
            "level": "warning",
            "message": f"High active connections: {metrics['active_connections']}"
        })

    if metrics["idle_in_transaction"] > 10:
        alerts.append({
            "level": "critical",
            "message": f"Many idle in transaction: {metrics['idle_in_transaction']}"
        })

    if metrics["waiting_locks"] > 0:
        alerts.append({
            "level": "warning",
            "message": f"Waiting locks detected: {metrics['waiting_locks']}"
        })

    # 計算快取命中率
    cache_hit_ratio = (metrics["total_blocks_hit"] /
                      (metrics["total_blocks_hit"] + metrics["total_blocks_read"]) * 100)

    if cache_hit_ratio < 95:
        alerts.append({
            "level": "warning",
            "message": f"Low cache hit ratio: {cache_hit_ratio:.2f}%"
        })

    return {
        "timestamp": datetime.utcnow(),
        "metrics": metrics,
        "cache_hit_ratio": cache_hit_ratio,
        "alerts": alerts
    }
```

### 場景 5.2: 資料品質監控

**背景**: 持續監控資料品質，確保資料的準確性和完整性。

```python
async def data_quality_monitoring():
    quality_checks = []

    # 檢查1: 資料完整性
    completeness_check = await execute_query("quality_db", """
        SELECT
            'customers' as table_name,
            'email' as column_name,
            COUNT(*) as total_rows,
            COUNT(email) as non_null_rows,
            COUNT(*) - COUNT(email) as null_rows,
            ROUND((COUNT(*) - COUNT(email)) * 100.0 / COUNT(*), 2) as null_percentage
        FROM customers

        UNION ALL

        SELECT
            'orders' as table_name,
            'customer_id' as column_name,
            COUNT(*) as total_rows,
            COUNT(customer_id) as non_null_rows,
            COUNT(*) - COUNT(customer_id) as null_rows,
            ROUND((COUNT(*) - COUNT(customer_id)) * 100.0 / COUNT(*), 2) as null_percentage
        FROM orders
    """)

    # 檢查2: 資料一致性
    consistency_check = await execute_query("quality_db", """
        -- 檢查訂單表中的客戶ID是否都存在於客戶表中
        SELECT
            'referential_integrity' as check_type,
            'orders.customer_id -> customers.customer_id' as check_description,
            COUNT(*) as violations
        FROM orders o
        LEFT JOIN customers c ON o.customer_id = c.customer_id
        WHERE c.customer_id IS NULL

        UNION ALL

        -- 檢查電子郵件格式
        SELECT
            'email_format' as check_type,
            'customers.email format validation' as check_description,
            COUNT(*) as violations
        FROM customers
        WHERE email NOT LIKE '%@%' OR email NOT LIKE '%.%'
    """)

    # 檢查3: 資料新鮮度
    freshness_check = await execute_query("quality_db", """
        SELECT
            table_name,
            date_column,
            MAX(date_value) as latest_record,
            EXTRACT(HOURS FROM CURRENT_TIMESTAMP - MAX(date_value)) as hours_since_last_update
        FROM (
            SELECT 'orders' as table_name, 'order_date' as date_column, order_date as date_value FROM orders
            UNION ALL
            SELECT 'customers' as table_name, 'created_at' as date_column, created_at as date_value FROM customers
        ) data_dates
        GROUP BY table_name, date_column
    """)

    # 生成品質報告
    quality_report = {
        "timestamp": datetime.utcnow(),
        "completeness": completeness_check["rows"],
        "consistency": consistency_check["rows"],
        "freshness": freshness_check["rows"],
        "overall_score": 100  # 基於檢查結果計算分數
    }

    # 計算整體品質分數
    issues = 0
    for check in completeness_check["rows"]:
        if check["null_percentage"] > 5:
            issues += 1

    for check in consistency_check["rows"]:
        if check["violations"] > 0:
            issues += 2  # 一致性問題權重較高

    quality_report["overall_score"] = max(0, 100 - (issues * 10))

    return quality_report
```

## 💼 商業智慧應用

### 場景 6.1: 客戶價值分析

**背景**: 行銷團隊需要識別高價值客戶並制定針對性的行銷策略。

```python
async def customer_value_analysis():
    # 客戶生命週期價值計算
    clv_analysis = await execute_query("bi_db", """
        WITH customer_metrics AS (
            SELECT
                c.customer_id,
                c.registration_date,
                EXTRACT(DAYS FROM CURRENT_DATE - c.registration_date) as customer_age_days,
                COUNT(DISTINCT o.order_id) as total_orders,
                SUM(o.total_amount) as total_spent,
                AVG(o.total_amount) as avg_order_value,
                MAX(o.order_date) as last_order_date,
                EXTRACT(DAYS FROM CURRENT_DATE - MAX(o.order_date)) as days_since_last_order,
                MIN(o.order_date) as first_order_date,
                EXTRACT(DAYS FROM MAX(o.order_date) - MIN(o.order_date)) as customer_lifespan_days
            FROM customers c
            LEFT JOIN orders o ON c.customer_id = o.customer_id
            GROUP BY c.customer_id, c.registration_date
        ),
        clv_segments AS (
            SELECT *,
                CASE
                    WHEN total_spent >= 10000 AND days_since_last_order <= 30 THEN 'VIP_Active'
                    WHEN total_spent >= 5000 AND days_since_last_order <= 60 THEN 'High_Value'
                    WHEN total_spent >= 1000 AND days_since_last_order <= 90 THEN 'Medium_Value'
                    WHEN days_since_last_order > 180 THEN 'At_Risk'
                    WHEN total_orders = 0 THEN 'Never_Purchased'
                    ELSE 'Regular'
                END as value_segment,

                -- 預測未來價值（簡化模型）
                CASE
                    WHEN customer_lifespan_days > 0 THEN
                        (total_spent / GREATEST(customer_lifespan_days, 1)) * 365 * 2
                    ELSE avg_order_value * 4
                END as predicted_annual_value

            FROM customer_metrics
        )
        SELECT
            value_segment,
            COUNT(*) as customer_count,
            AVG(total_spent) as avg_total_spent,
            AVG(total_orders) as avg_total_orders,
            AVG(avg_order_value) as avg_order_value,
            AVG(predicted_annual_value) as avg_predicted_annual_value,
            SUM(total_spent) as segment_total_revenue
        FROM clv_segments
        GROUP BY value_segment
        ORDER BY avg_predicted_annual_value DESC
    """)

    # 產品親和性分析
    product_affinity = await execute_query("bi_db", """
        WITH product_cooccurrence AS (
            SELECT
                oi1.product_id as product_a,
                oi2.product_id as product_b,
                COUNT(DISTINCT oi1.order_id) as cooccurrence_count,
                COUNT(DISTINCT oi1.order_id) * 1.0 /
                    (SELECT COUNT(DISTINCT order_id) FROM order_items WHERE product_id = oi1.product_id) as confidence
            FROM order_items oi1
            JOIN order_items oi2 ON oi1.order_id = oi2.order_id AND oi1.product_id != oi2.product_id
            GROUP BY oi1.product_id, oi2.product_id
            HAVING COUNT(DISTINCT oi1.order_id) >= 5
        )
        SELECT
            pa.name as product_a_name,
            pb.name as product_b_name,
            pc.cooccurrence_count,
            ROUND(pc.confidence * 100, 2) as confidence_percentage
        FROM product_cooccurrence pc
        JOIN products pa ON pc.product_a = pa.product_id
        JOIN products pb ON pc.product_b = pb.product_id
        WHERE pc.confidence > 0.3
        ORDER BY pc.confidence DESC
        LIMIT 20
    """)

    return {
        "customer_segments": clv_analysis["rows"],
        "product_recommendations": product_affinity["rows"],
        "analysis_date": datetime.utcnow()
    }
```

### 場景 6.2: 銷售預測分析

**背景**: 基於歷史資料預測未來銷售趨勢，協助庫存規劃和業務決策。

```python
async def sales_forecasting():
    # 歷史銷售趨勢分析
    historical_sales = await execute_query("forecast_db", """
        WITH monthly_sales AS (
            SELECT
                DATE_TRUNC('month', order_date) as month,
                COUNT(DISTINCT order_id) as orders_count,
                SUM(total_amount) as monthly_revenue,
                COUNT(DISTINCT customer_id) as unique_customers,
                AVG(total_amount) as avg_order_value
            FROM orders
            WHERE order_date >= CURRENT_DATE - INTERVAL '24 months'
            GROUP BY DATE_TRUNC('month', order_date)
        ),
        sales_with_growth AS (
            SELECT *,
                LAG(monthly_revenue, 1) OVER (ORDER BY month) as prev_month_revenue,
                LAG(monthly_revenue, 12) OVER (ORDER BY month) as same_month_last_year,
                (monthly_revenue - LAG(monthly_revenue, 1) OVER (ORDER BY month)) /
                    NULLIF(LAG(monthly_revenue, 1) OVER (ORDER BY month), 0) * 100 as mom_growth,
                (monthly_revenue - LAG(monthly_revenue, 12) OVER (ORDER BY month)) /
                    NULLIF(LAG(monthly_revenue, 12) OVER (ORDER BY month), 0) * 100 as yoy_growth
            FROM monthly_sales
        )
        SELECT
            month,
            monthly_revenue,
            orders_count,
            unique_customers,
            avg_order_value,
            ROUND(mom_growth, 2) as mom_growth_pct,
            ROUND(yoy_growth, 2) as yoy_growth_pct,
            -- 簡單的移動平均預測
            AVG(monthly_revenue) OVER (
                ORDER BY month ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
            ) as ma3_forecast
        FROM sales_with_growth
        ORDER BY month
    """)

    # 季節性分析
    seasonal_analysis = await execute_query("forecast_db", """
        SELECT
            EXTRACT(MONTH FROM order_date) as month,
            EXTRACT(QUARTER FROM order_date) as quarter,
            COUNT(*) as orders_count,
            SUM(total_amount) as total_revenue,
            AVG(total_amount) as avg_order_value,
            -- 計算相對於年平均的季節性指數
            SUM(total_amount) / (
                SELECT SUM(total_amount) / 12.0
                FROM orders
                WHERE EXTRACT(YEAR FROM order_date) = EXTRACT(YEAR FROM order_date)
            ) as seasonal_index
        FROM orders
        WHERE order_date >= CURRENT_DATE - INTERVAL '36 months'
        GROUP BY EXTRACT(MONTH FROM order_date), EXTRACT(QUARTER FROM order_date)
        ORDER BY month
    """)

    # 產品銷售預測
    product_forecast = await execute_query("forecast_db", """
        WITH product_monthly_sales AS (
            SELECT
                p.product_id,
                p.name as product_name,
                p.category,
                DATE_TRUNC('month', o.order_date) as month,
                SUM(oi.quantity) as units_sold,
                SUM(oi.quantity * oi.unit_price) as revenue
            FROM products p
            JOIN order_items oi ON p.product_id = oi.product_id
            JOIN orders o ON oi.order_id = o.order_id
            WHERE o.order_date >= CURRENT_DATE - INTERVAL '12 months'
            GROUP BY p.product_id, p.name, p.category, DATE_TRUNC('month', o.order_date)
        ),
        product_trends AS (
            SELECT
                product_id,
                product_name,
                category,
                SUM(units_sold) as total_units_12m,
                SUM(revenue) as total_revenue_12m,
                AVG(units_sold) as avg_monthly_units,
                STDDEV(units_sold) as stddev_monthly_units,
                -- 簡單線性趨勢
                CORR(EXTRACT(EPOCH FROM month), units_sold) as trend_correlation
            FROM product_monthly_sales
            GROUP BY product_id, product_name, category
        )
        SELECT
            product_name,
            category,
            total_units_12m,
            total_revenue_12m,
            avg_monthly_units,
            ROUND(avg_monthly_units * 1.1, 0) as forecasted_next_month,  -- 假設10%成長
            CASE
                WHEN trend_correlation > 0.7 THEN 'Strong Upward'
                WHEN trend_correlation > 0.3 THEN 'Moderate Upward'
                WHEN trend_correlation < -0.7 THEN 'Strong Downward'
                WHEN trend_correlation < -0.3 THEN 'Moderate Downward'
                ELSE 'Stable'
            END as trend_direction
        FROM product_trends
        WHERE total_units_12m > 0
        ORDER BY total_revenue_12m DESC
        LIMIT 50
    """)

    return {
        "historical_trends": historical_sales["rows"],
        "seasonal_patterns": seasonal_analysis["rows"],
        "product_forecasts": product_forecast["rows"],
        "forecast_date": datetime.utcnow(),
        "forecast_horizon": "1_month",
        "model_type": "simple_statistical"
    }
```

## 🎯 最佳實務建議

### 1. 連線管理
- 使用連線池避免頻繁建立連線
- 為不同用途設立不同的連線（讀取、寫入、分析）
- 定期測試連線健康狀況

### 2. 查詢優化
- 使用參數化查詢防止 SQL 注入
- 對大量資料使用分頁或批次處理
- 利用 `explain_query` 分析查詢效能

### 3. 安全性
- 生產環境啟用只讀模式
- 設定適當的操作白名單
- 記錄所有重要操作的審計日誌

### 4. 監控與維護
- 建立自動化健康檢查
- 設定效能指標警報
- 定期執行資料庫維護任務

### 5. 錯誤處理
- 使用事務確保資料一致性
- 實現重試機制處理暫時性錯誤
- 記錄詳細錯誤日誌便於除錯

## 📚 更多資源

- **📖 完整文件**: [MCP Server 使用手冊](MCP_SERVER_HANDBOOK.md)
- **🔌 客戶端範例**: [MCP 客戶端整合範例](examples/MCP_CLIENT_EXAMPLES.md)
- **🚀 快速開始**: [快速開始指南](../QUICK_START.md)
- **🐳 Docker 部署**: [Docker Hub 指南](DOCKER_HUB_GUIDE.md)
- **❓ 常見問題**: [FAQ 文件](../QA.md)

---

> **💡 提示**: 這些使用場景展示了 PostgreSQL MCP Server 的強大功能和靈活性。你可以根據自己的業務需求，調整和組合這些範例來解決實際問題。建議從簡單場景開始，逐步擴展到複雜的應用。