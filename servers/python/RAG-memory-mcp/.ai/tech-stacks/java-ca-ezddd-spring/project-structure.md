# Java CA ezddd Spring 專案結構指南

## 建議的目錄結構

### 完整專案結構
```
project-root/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── [package]/
│   │   │       ├── [aggregate1]/        # 聚合根目錄
│   │   │       │   ├── entity/          # 領域層 (Clean Architecture)
│   │   │       │   │   ├── [Aggregate].java
│   │   │       │   │   ├── [Aggregate]Events.java
│   │   │       │   │   └── [ValueObject].java
│   │   │       │   ├── usecase/         # 應用層 (Clean Architecture)
│   │   │       │   │   ├── service/     # 所有 Service 實作
│   │   │       │   │   │   ├── Create[Aggregate]Service.java
│   │   │       │   │   │   ├── Update[Aggregate]Service.java
│   │   │       │   │   │   └── Delete[Aggregate]Service.java
│   │   │       │   │   └── port/
│   │   │       │   │       ├── in/      # UseCase interfaces
│   │   │       │   │       │   ├── Create[Aggregate]UseCase.java
│   │   │       │   │       │   ├── Update[Aggregate]UseCase.java
│   │   │       │   │       │   └── Delete[Aggregate]UseCase.java
│   │   │       │   │       ├── out/
│   │   │       │   │       │   ├── archive/
│   │   │       │   │       │   ├── inquiry/
│   │   │       │   │       │   ├── projection/
│   │   │       │   │       │   └── repository/  # DDD Repository interface
│   │   │       │   │       └── dto/
│   │   │       │   └── adapter/         # 適配器層 (Clean Architecture)
│   │   │       │       ├── in/
│   │   │       │       │   ├── acl/     # Anti-Corruption Layer
│   │   │       │       │   └── controller/
│   │   │       │       │       ├── rest/
│   │   │       │       │       └── websocket/
│   │   │       │       └── out/
│   │   │       │           ├── repository/
│   │   │       │           │   ├── es/      # Event Store
│   │   │       │           │   └── outbox/  # Outbox pattern
│   │   │       │           ├── projection/  # JPA Projections
│   │   │       │           └── websocket/
│   │   │       ├── [aggregate2]/        # 其他聚合根
│   │   │       ├── common/              # 共用組件
│   │   │       └── io/                  # 框架與主函數層 (Clean Architecture)
│   │   │           └── springboot/
│   │   │               ├── config/      # Spring 配置
│   │   │               │   ├── BootstrapConfig.java
│   │   │               │   └── orm/
│   │   │               └── Application.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/
│   │           └── migration/           # Flyway migrations
│   └── test/
│       └── java/
│           └── [package]/
│               └── [aggregate]/
│                   └── usecase/         # 測試檔案
├── .ai/                                 # AI 協作目錄
├── .dev/                                # 開發文檔
│   ├── specs/                          # 領域規格
│   └── tasks/                          # 任務記錄
├── frontend/                           # 前端專案（如果有）
├── pom.xml                             # Maven 配置
└── README.md
```

### Clean Architecture 層級對應
1. **Entity 層**: `[aggregate]/entity/` - 業務邏輯核心
2. **Use Case 層**: `[aggregate]/usecase/` - 應用邏輯
3. **Adapter 層**: `[aggregate]/adapter/` - 介面適配
4. **Framework 層**: `io/springboot/` - 框架配置

## 不同任務的關注重點

### 1. 實現新的 Use Case

需要關注的檔案：
```
# 1. 先了解領域模型
.dev/specs/[aggregate]/entity/[aggregate]-spec.md
src/main/java/.../[aggregate]/entity/[Aggregate].java

# 2. 參考現有 Use Case
src/main/java/.../[aggregate]/usecase/port/in/*UseCase.java
src/main/java/.../[aggregate]/usecase/service/*Service.java

# 3. 查看測試範例
src/test/java/.../[aggregate]/usecase/*Test.java
```

### 2. 修改 Domain Entity

需要關注的檔案：
```
# 1. 領域規格
.dev/specs/[aggregate]/entity/[aggregate]-spec.md

# 2. Entity 和 Events
src/main/java/.../[aggregate]/entity/[Aggregate].java
src/main/java/.../[aggregate]/entity/[Aggregate]Events.java

# 3. 事件註冊
src/main/java/.../io/springboot/config/BootstrapConfig.java

# 4. 相關測試
src/test/java/.../[aggregate]/entity/*Test.java
```

### 3. 除錯問題

需要檢查的位置：
```
# 配置檔案
src/main/resources/application.properties
src/main/java/.../io/springboot/config/BootstrapConfig.java

# 資料庫遷移
src/main/resources/db/migration/*.sql

# 日誌和錯誤
target/logs/
```

### 4. 新增 Projection

需要建立的檔案：
```
# 1. Projection Interface
src/main/java/.../[aggregate]/usecase/port/out/projection/[Data]Projection.java

# 2. JPA Implementation
src/main/java/.../[aggregate]/adapter/out/projection/Jpa[Data]Projection.java

# 3. 在 Service 中使用
src/main/java/.../[aggregate]/usecase/service/[Operation]Service.java
```

## 常用命令

### 編譯和測試
```bash
# 編譯專案
mvn compile

# 執行所有測試
mvn test

# 執行特定測試
mvn test -Dtest=CreateTaskUseCaseTest

# 跳過測試編譯
mvn package -DskipTests
```

### 執行應用程式
```bash
# 啟動 Spring Boot
mvn spring-boot:run

# 使用特定 profile
mvn spring-boot:run -Dspring.profiles.active=dev
```

### 資料庫操作
```bash
# 執行 Flyway migration
mvn flyway:migrate

# 清理資料庫（小心使用）
mvn flyway:clean
```

## 搜尋模式

### 尋找特定類型的檔案
```bash
# 找所有 UseCase interfaces
find . -name "*UseCase.java" -path "*/port/in/*"

# 找所有 Aggregate Roots
grep -r "extends EsAggregateRoot" --include="*.java"

# 找所有 Domain Events
grep -r "implements.*Events" --include="*.java"

# 找所有 Service 實作
find . -name "*Service.java" -path "*/usecase/service/*"
```

### 檢查常見問題
```bash
# 檢查是否有使用 Lazy Loading
grep -r "FetchType.LAZY" --include="*.java"

# 檢查未註冊的事件
grep -r "class.*implements DomainEvent" --include="*.java"

# 檢查 public fields (Input/Output)
grep -r "public.*;" --include="*Input.java" --include="*Output.java"
```

## 最佳實踐提醒

1. **包結構**: Service 類必須放在 `usecase.service` 包中
2. **事件註冊**: 新的 Domain Event 必須在 BootstrapConfig 中註冊
3. **JPA 配置**: 永遠使用 EAGER loading，避免 LAZY
4. **測試優先**: 實現功能前先寫測試
5. **規格同步**: 修改實作時同步更新 `.dev/specs/`

## 環境設置

### 系統需求
- Java 21+
- Maven 3.8+
- PostgreSQL 15+ 或 Docker
- Node.js 18+（如果有前端）

### 快速設置步驟
```bash
# 1. Clone 專案
git clone <repository-url>
cd <project-name>

# 2. 啟動資料庫（使用 Docker）
docker-compose up -d postgres

# 3. 執行資料庫遷移
mvn flyway:migrate

# 4. 執行測試確認環境
mvn test

# 5. 啟動應用程式
mvn spring-boot:run
```

### IDE 設置
1. 安裝 Lombok 插件
2. 啟用 Annotation Processing
3. 設定 Java 21 作為專案 SDK

## 開發最佳實踐

### 1. 測試驅動開發 (TDD)
```bash
# 1. 先寫失敗的測試
# 2. 執行測試確認失敗
mvn test -Dtest=CreateTaskUseCaseTest

# 3. 實作功能
# 4. 執行測試確認通過
mvn test -Dtest=CreateTaskUseCaseTest
```

### 2. 小步提交
- 每完成一個小功能就提交
- 提交訊息要清楚描述變更
- 使用 conventional commits 格式

### 3. 程式碼審查檢查點
- [ ] 遵循 Clean Architecture 層級
- [ ] Use Case 放在正確的套件
- [ ] 事件已在 BootstrapConfig 註冊
- [ ] 測試覆蓋率足夠
- [ ] 沒有使用 LAZY loading

### 4. 除錯技巧
- **編譯錯誤**：檢查 Maven 依賴和私有 repository 設定
- **測試失敗**：確認資料庫連線和測試資料
- **找不到類別**：查看 ezddd-import-mapping.md
- **事件未處理**：檢查 BootstrapConfig 註冊

## 相關資源

- [編碼指南](./coding-guide.md)
- [Aggregate 模式與範例](./examples/aggregate/README.md)
- [UseCase 模式與範例](./examples/usecase/README.md)
- [Controller 模式與範例](./examples/controller/README.md)
- [Repository 模式與範例](./examples/repository/README.md)
- [Mapper 模式與範例](./examples/mapper/README.md)
- [所有範例索引](./examples/INDEX.md)
- [FAQ](./FAQ.md)