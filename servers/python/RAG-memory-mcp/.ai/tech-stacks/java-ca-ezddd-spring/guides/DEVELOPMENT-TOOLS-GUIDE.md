# 開發工具與常用命令指南

## 📋 概述
本指南提供 AI-Plan 專案開發中常用的工具和命令參考。

## 🛠️ Maven 命令

### 基本命令
```bash
# 編譯專案
mvn compile

# 執行所有測試
mvn test

# 執行特定測試類
mvn test -Dtest=CreatePlanUseCaseTest

# 執行特定測試方法
mvn test -Dtest=CreatePlanUseCaseTest#testCreatePlan

# 跳過測試進行打包
mvn package -DskipTests

# 清理並重新編譯
mvn clean compile

# 檢查依賴更新
mvn versions:display-dependency-updates
```

### 依賴管理
```bash
# 顯示依賴樹
mvn dependency:tree

# 分析未使用的依賴
mvn dependency:analyze

# 下載所有依賴的源碼
mvn dependency:sources

# 解決依賴衝突
mvn dependency:resolve
```

## 🔍 Git 命令

### 基本操作
```bash
# 查看狀態
git status

# 添加所有變更
git add -A

# 提交變更
git commit -m "feat: Add new feature"

# 推送到遠端
git push origin main
```

### 分支管理
```bash
# 創建新分支
git checkout -b feature/new-feature

# 切換分支
git checkout main

# 合併分支
git merge feature/new-feature

# 刪除本地分支
git branch -d feature/new-feature

# 刪除遠端分支
git push origin --delete feature/new-feature
```

### 提交規範
```
feat: 新功能
fix: 修復錯誤
docs: 文檔更新
style: 格式調整（不影響程式碼運行）
refactor: 重構（不新增功能或修復錯誤）
perf: 效能改進
test: 新增測試
chore: 構建過程或輔助工具的變動
```

## 🐳 Docker 命令

### PostgreSQL 本地開發
```bash
# 啟動 PostgreSQL
docker run --name postgres-dev \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=aiplan \
  -p 5432:5432 \
  -d postgres:15

# 進入 PostgreSQL 命令行
docker exec -it postgres-dev psql -U postgres -d aiplan

# 停止容器
docker stop postgres-dev

# 啟動已存在的容器
docker start postgres-dev

# 查看容器日誌
docker logs postgres-dev
```

### Redis 本地開發
```bash
# 啟動 Redis
docker run --name redis-dev \
  -p 6379:6379 \
  -d redis:7

# 進入 Redis CLI
docker exec -it redis-dev redis-cli

# 監控 Redis 命令
docker exec -it redis-dev redis-cli monitor
```

## 📊 資料庫命令

### PostgreSQL 常用命令
```sql
-- 列出所有表
\dt

-- 查看表結構
\d table_name

-- 查看表資料
SELECT * FROM plan LIMIT 10;

-- 清空表資料（保留結構）
TRUNCATE TABLE plan CASCADE;

-- 查看當前連接
SELECT * FROM pg_stat_activity;

-- 查看表大小
SELECT pg_size_pretty(pg_total_relation_size('plan'));
```

### 資料庫遷移
```bash
# Flyway 遷移（如果使用）
mvn flyway:migrate

# 查看遷移歷史
mvn flyway:info

# 修復遷移
mvn flyway:repair
```

## 🔧 開發工具

### IntelliJ IDEA 快捷鍵
```
Cmd + Shift + F    # 全域搜尋
Cmd + Shift + R    # 全域替換
Cmd + O            # 查找類
Cmd + Shift + O    # 查找文件
Cmd + E            # 最近打開的文件
Cmd + B            # 跳轉到定義
Cmd + Alt + B      # 跳轉到實現
Cmd + F12          # 文件結構
Alt + F7           # 查找使用
Cmd + Shift + T    # 創建/跳轉測試
```

### VS Code 快捷鍵
```
Cmd + P            # 快速打開文件
Cmd + Shift + P    # 命令面板
Cmd + Shift + F    # 全域搜尋
Cmd + D            # 選擇下一個相同的詞
Cmd + /            # 註釋/取消註釋
F12                # 跳轉到定義
Shift + F12        # 查找所有引用
```

## 🐛 調試技巧

### 遠端調試
```bash
# 啟動應用程式時加入調試參數
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar
```

### 日誌級別調整
```properties
# application.properties
logging.level.tw.teddysoft.example=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type=TRACE
```

### 查看 JVM 資訊
```bash
# 查看 Java 進程
jps -l

# 查看堆疊資訊
jstack <pid>

# 查看堆記憶體
jmap -heap <pid>

# 生成堆轉儲
jmap -dump:live,format=b,file=heap.bin <pid>
```

## 🚀 效能分析

### Apache Bench (ab)
```bash
# 簡單壓測
ab -n 1000 -c 10 http://localhost:8080/api/v1/plans

# POST 請求壓測
ab -n 1000 -c 10 -p post.json -T application/json http://localhost:8080/api/v1/plans
```

### curl 測試
```bash
# GET 請求
curl -X GET http://localhost:8080/api/v1/users/user123/plans

# POST 請求
curl -X POST http://localhost:8080/api/v1/plans \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123","planName":"My Plan"}'

# 查看響應時間
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8080/api/v1/plans
```

### curl-format.txt
```
time_namelookup:  %{time_namelookup}s\n
time_connect:  %{time_connect}s\n
time_appconnect:  %{time_appconnect}s\n
time_pretransfer:  %{time_pretransfer}s\n
time_redirect:  %{time_redirect}s\n
time_starttransfer:  %{time_starttransfer}s\n
time_total:  %{time_total}s\n
```

## 📝 實用腳本

### 批量重命名
```bash
# 將所有 .java 檔案中的舊類名替換為新類名
find . -name "*.java" -exec sed -i '' 's/OldClassName/NewClassName/g' {} +
```

### 查找未使用的程式碼
```bash
# 查找可能未使用的 private 方法
grep -r "private.*(" --include="*.java" | grep -v Test
```

### 統計程式碼行數
```bash
# 統計 Java 程式碼行數
find . -name "*.java" -exec wc -l {} + | tail -1

# 按文件類型統計
find . -name "*.java" -o -name "*.md" -o -name "*.yml" | xargs wc -l
```

## 🔗 相關資源
- [Maven 官方文檔](https://maven.apache.org/guides/)
- [Git 官方文檔](https://git-scm.com/doc)
- [Docker 官方文檔](https://docs.docker.com/)
- [PostgreSQL 官方文檔](https://www.postgresql.org/docs/)