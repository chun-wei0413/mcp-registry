# Java DDD Spring 快速設置指南

## 新專案設置

### 1. 初始化專案結構
```bash
# 創建基本目錄結構
mkdir -p src/main/java/com/example/myapp/{common,io/springboot}
mkdir -p src/test/java/com/example/myapp
mkdir -p .ai .dev
```

### 2. 複製必要檔案
```bash
# 從 AI-Plan 專案複製核心配置
cp -r /path/to/ai-plan/.ai/* .ai/
cp -r /path/to/ai-plan/.dev/adr .dev/
cp /path/to/ai-plan/pom.xml .
```

### 3. 調整專案特定內容
```xml
<!-- 更新 pom.xml -->
<groupId>com.example</groupId>
<artifactId>myapp</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

### 4. 創建第一個 Aggregate

使用 AI 指令創建：

```
請使用 feature-implementation workflow 創建 User aggregate
需要包含：
- userId (AggregateId)
- email (唯一)
- name
- 基本的 CRUD 操作
```

或者生成特定組件：

```
"請生成 UserRepository 介面"
"請生成 UserController 並包含基本的 REST endpoints"
"請為 User aggregate 生成完整的測試案例"
```

## 從現有專案遷移

### 1. 評估現有架構
- 識別現有的 Entity 和 Service
- 評估哪些適合轉換為 Aggregate
- 確定邊界上下文

### 2. 漸進式遷移
```
Phase 1: 建立新的包結構
Phase 2: 遷移簡單的 CRUD 操作
Phase 3: 引入 Domain Events
Phase 4: 實現 Event Sourcing
Phase 5: 優化查詢 (CQRS)
```

### 3. 保持兼容性
- 使用 Facade 模式包裝新實現
- 維護舊 API 直到完全遷移
- 逐步廢棄舊代碼

## 團隊入門培訓

### Day 1: DDD 基礎
- Aggregate、Entity、Value Object 概念
- Domain Event 設計
- Repository 模式

### Day 2: 技術實踐
- ezSpec 測試編寫
- Event Sourcing 實現
- Spring Boot 集成

### Day 3: 實戰練習
- 實現一個完整的 Use Case
- Code Review
- 最佳實踐分享

## 常用命令

```bash
# 編譯專案
mvn clean compile

# 運行測試
mvn test

# 運行特定測試
mvn test -Dtest=CreateUserUseCaseTest

# 生成測試報告
mvn surefire-report:report

# 啟動應用
mvn spring-boot:run
```

## 故障排除

### 問題：找不到 ezddd 依賴
所有 tw.teddysoft 依賴都已在 Maven Central，無需配置私有倉庫。
如果仍有問題，請檢查：
- 網路連線是否正常
- 版本號碼是否正確（如 ezddd-core: 3.0.0）

### 問題：Event 未註冊
```java
// 在 BootstrapConfig.java 添加
DomainEventTypeRegistry.registerType(UserCreated.class);
```

### 問題：JPA LazyInitializationException
```java
// 改為 EAGER loading
@OneToMany(fetch = FetchType.EAGER)
private Set<Task> tasks;
```

## 相關資源

- [完整編碼指南](./coding-guide.md)
- [設計模式與範例](./examples/)
- [完整範例索引](./examples/INDEX.md)
- [AI-Plan 原始專案](https://github.com/example/ai-plan)