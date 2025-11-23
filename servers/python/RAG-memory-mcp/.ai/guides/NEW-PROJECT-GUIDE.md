# AiScrum 專案結構與新專案設置指南

本指南說明 AiScrum 專案的結構，以及如何基於 AiScrum 模板創建新的 java-ca-ezddd-spring 專案。支援 Claude、Gemini 等多種 AI 助手。

## 📋 目錄

1. [前置準備](#前置準備)
2. [第一步：創建專案基礎結構](#第一步創建專案基礎結構)
3. [第二步：設置 EZ-AI 框架](#第二步設置-ez-ai-框架)
4. [第三步：配置專案特定資訊](#第三步配置專案特定資訊)
5. [第四步：初始化 Maven 專案](#第四步初始化-maven-專案)
6. [第五步：創建第一個功能](#第五步創建第一個功能)
7. [第六步：與 AI 協作開發](#第六步與-ai-協作開發)
8. [常見問題](#常見問題)

## 前置準備

### 環境需求
- Java 21+
- Maven 3.8+ (建議使用 `/opt/homebrew/bin/mvn`)
- Git
- PostgreSQL (測試用 port 5800)
- 你偏好的 IDE（IntelliJ IDEA 或 VS Code）

### 重要：Maven 依賴說明
**所有 tw.teddysoft 套件都在 Maven Central 上公開可用，不需要本地安裝或私有 repository。**

**必須使用的 Spring Boot 依賴：**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
這個依賴提供了 Bean Validation (JSR-303) 支援，對於驗證輸入參數是必要的。

### 知識準備
- 基本的 Domain-Driven Design (DDD) 概念
- Spring Boot 基礎
- Maven 使用經驗

## 第一步：創建專案基礎結構

```bash
# 1. 創建專案目錄
mkdir my-scrum-project
cd my-scrum-project

# 2. 初始化 Git
git init

# 3. 創建 AiScrum 標準目錄結構
mkdir -p src/main/java/tw/teddysoft/aiscrum/{common,io/springboot}
mkdir -p src/main/resources
mkdir -p src/test/java/tw/teddysoft/aiscrum/{test/base,test/suite}
mkdir -p src/test/resources

# 4. 創建 .ai 和 .dev 目錄結構
mkdir -p .ai/{config,guides,prompts,scripts,tech-stacks,workflows}
mkdir -p .dev/{adr,specs,tasks}
mkdir -p .dev/tasks/{feature,test,refactoring,frontend,main}
mkdir -p .dev/specs/{use-cases,aggregates,domain-events}

# 或者讓 AI 幫你創建：
```
請根據 AiScrum 專案結構創建標準的 Maven 專案目錄，包含 .dev/specs 規格文檔目錄
```
```

## 第二步：設置 EZ-AI 框架

### 方式一：克隆 AiScrum 模板（推薦）

```bash
# 1. 克隆 AiScrum 專案作為模板
git clone https://gitlab.com/TeddyChen/ai-plans-v2.git temp-aiscrum

# 2. 複製核心目錄結構
cp -r temp-aiscrum/aiscrum/.ai/* .ai/
cp -r temp-aiscrum/aiscrum/.dev/* .dev/

# 3. 清理不需要的內容
rm -rf .dev/tasks/old/*  # 移除舊任務
rm -rf temp-aiscrum
```

### 方式二：使用模板創建核心文件

如果無法克隆，使用提供的模板創建核心文件：

```bash
# 1. 創建 AI 記憶文件 - 根據您使用的 AI 選擇

# 建議：使用 CLAUDE.md 作為專案記憶（支援所有 AI）
cp CLAUDE.md CLAUDE.md  # 保留原樣或調整

# 更新專案特定資訊：
# - 專案名稱和描述
# - 領域模型 (Product, Sprint, ProductBacklogItem, Task)
# - Sub-agent workflows
# - 技術棧版本

# 2. 創建 EZ-AI 配置

方法一：手動創建（如左邊範例）

方法二：使用 AI 生成
```
請幫我創建 .ai/EZ-AI-CONFIG.md，專案是：
- 技術棧：java-ca-ezddd-spring
- 參與層級：medium-engagement
- 專案目的：[描述你的專案]
```
```

## 第三步：配置專案特定資訊

### 1. 創建專案配置

```json
// .dev/project-config.json 範例 (基於 AiScrum)
{
  "projectName": "MyScrum",
  "groupId": "tw.teddysoft.myscrum",
  "artifactId": "tw.teddysoft.myscrum",
  "version": "0.1.0-SNAPSHOT",
  "rootPackage": "tw.teddysoft.myscrum",
  "springBootVersion": "3.5.3",
  "dependencies": {
    "ezappStarterVersion": "1.0.0",
    "ezspecVersion": "2.0.3",
    "ucontractVersion": "2.0.0"
  },
  "database": {
    "production": "PostgreSQL",
    "test": "PostgreSQL"  // port 5800 for test
  },
  "features": {
    "eventSourcing": true,
    "cqrs": true,
    "restApi": true
  }
}
```

### 2. 創建第一個架構決策記錄

參考 AiScrum 的 ADR 結構：

```
請參考 .dev/adr/ADR-005-ai-task-execution-sop.md 創建新的 ADR：
- 編號：ADR-001
- 決策：[你的決策]
- 背景：[為什麼需要這個決策]
- 效益：[優點和缺點]
```

或手動創建：

```bash
cp .ai/templates/adr-template.md .dev/adr/ADR-001-use-ezddd.md
# 然後編輯內容
```

## 第四步：初始化 Maven 專案

### 1. 使用 AI 生成 pom.xml

**🔥 重要更新：使用驗證過的模板**

確保你已經完成第三步的專案配置後，使用以下指令讓 AI 生成完整的 pom.xml：

```
請執行 project-initialization workflow：
1. 複製 .ai/tech-stacks/java-ca-ezddd-spring/examples/pom/pom.xml 作為基礎
2. 使用 .dev/project-config.json 中的版本號替換所有 {placeholder} 佔位符
3. 複製 .ai/tech-stacks/java-ca-ezddd-spring/examples/spring/ 下的所有 properties 檔案
```

**關於佔位符系統**：
- 模板使用 `{springBootVersion}`, `{ezappStarterVersion}` 等佔位符
- AI 會自動從 project-config.json 讀取版本並替換
- 詳見 `.ai/guides/VERSION-PLACEHOLDER-GUIDE.md`

**模板包含的正確依賴**：
- ezapp-starter（包含所有 EZDDD 框架功能，包括 ezapp-starter、ucontract、ezspec）
- spring-boot-starter-validation (Bean Validation 支援)
- PostgreSQL 驅動（給 outbox profile 使用）
- JUnit Platform Suite 測試套件支援

AI 會根據：
- 驗證過的模板 `.ai/tech-stacks/java-ca-ezddd-spring/examples/pom/pom.xml`
- 你的 `.dev/project-config.json` 中的版本號
- 生成一個完整、可用的 pom.xml 檔案

### 2. 創建 Spring Boot 應用主類

同樣可以讓 AI 幫你生成：

```
請根據 .dev/project-config.json 中的 rootPackage 創建 Spring Boot 應用主類
```

AI 會自動：
- 使用正確的 package 名稱
- 創建標準的 Spring Boot 啟動類
- 建立正確的目錄結構

### 重要：正確的 Import 路徑
如果遇到 import 錯誤，請參考 `.ai/tech-stacks/java-ca-ezddd-spring/EZDDD-FRAMEWORK-REFERENCE.md`：
- ❌ 錯誤：`tw.teddysoft.ezddd.core.entity.DomainEvent`
- ✅ 正確：`tw.teddysoft.ezddd.entity.DomainEvent`

### 3. 產生共用程式

讓 AI 根據框架規範產生必要的共用類別：

```
請根據以下模板產生必要的共用程式：
1. **📖 共用類別初始化** - 參考 [Fresh Project Initialization Guide](../prompts/shared/fresh-project-init.md)
   從 .ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md 產生：
   - DateProvider - 時間管理工具
   - GenericInMemoryRepository - 測試用 Repository
   - MyInMemoryMessageBroker - 訊息傳遞機制
   - MyInMemoryMessageProducer - Outbox Pattern 支援

2. 從 .ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/base-test-classes.md 產生：
   - BaseSpringBootTest - Spring Boot 測試基礎類別
   - BaseUseCaseTest - Use Case 測試基礎類別（支援多 profile）

3. 從 .ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/test-suites.md 產生：
   - InMemoryTestSuite - 記憶體測試套件
   - OutboxTestSuite - Outbox Pattern 測試套件
   - **📖 ProfileSetter Pattern** - 參考 [Dual Profile Testing Configuration](../prompts/shared/dual-profile-testing.md)
   - UseCaseTestSuite - 只執行 UseCase 測試的套件
```

AI 會自動：
1. 根據 .dev/project-config.json 的 rootPackage 設定正確的 package
2. 創建必要的目錄結構
3. 產生完整的程式碼
4. **確保 BaseUseCaseTest 不包含 @ActiveProfiles（支援動態 profile 切換）**
5. **Test Suite 包含 ProfileSetter inner class 來強制設定 profile**

### 4. 驗證設置

```bash
# 編譯專案
/opt/homebrew/bin/mvn clean compile

# 執行測試套件（使用 ProfileSetter 設定 profile）
/opt/homebrew/bin/mvn test -Dtest=InMemoryTestSuite  # 執行記憶體測試
/opt/homebrew/bin/mvn test -Dtest=OutboxTestSuite    # 執行 Outbox 測試（需要 PostgreSQL）

# 如果成功，你應該看到 BUILD SUCCESS
```

## 第五步：創建第一個功能

### 方式一：使用規格文檔（推薦）

1. **創建 Use Case 規格**

```bash
# 複製模板
cp .ai/templates/use-case-spec-template.json .dev/specs/use-cases/product/create-product.json

# 編輯規格文件，定義業務需求
vim .dev/specs/use-cases/product/create-product.json
```

2. **使用 TDD 方式實現**

```
請根據 .dev/specs/use-cases/product/create-product.json 用 TDD 方式實現功能
```

AI 會按照 TDD 流程：
- 先生成測試（紅燈）
- 實現功能（綠燈）
- 重構優化（重構）

### 方式二：直接描述需求

1. **準備任務描述**

```json
// AiScrum 範例：創建 Product Aggregate
{
  "task": "Create Product Aggregate",
  "requirements": {
    "aggregateName": "Product",
    "properties": ["productId", "name", "goal", "definitionOfDone"],
    "commands": ["CreateProduct", "SetProductGoal", "DefineDefinitionOfDone"],
    "events": ["ProductCreated", "ProductGoalSet", "DefinitionOfDoneUpdated"]
  }
}
```

2. **與 AI 對話**

```
請使用 command-sub-agent workflow 實作 create-product use case。
需求如下：
- Aggregate: Product
- Properties: productId, name, goal, definitionOfDone
- Command: CreateProduct
- Event: ProductCreated
- 輸入：name
- 輸出：productId
```

3. **AI 將幫你生成** (依照 sub-agent workflow)
- Product Aggregate 類 (Event Sourcing pattern)
- ProductEvents (sealed interface)
- CreateProductUseCase + CreateProductService
- CreateProductUseCaseTest (ezSpec BDD style)
- Repository<Product, ProductId> interface
- CreateProductController (REST API)

### 管理規格文檔

建議將所有 Use Case 規格保存在 `.dev/specs/` 目錄：

```bash
.dev/specs/
├── use-cases/
│   ├── product/       # Product 相關功能
│   │   ├── create-product.json
│   │   ├── set-product-goal.json
│   │   └── define-definition-of-done.json
│   ├── sprint/        # Sprint 相關功能
│   │   ├── create-sprint.json
│   │   └── set-sprint-timebox.json
│   └── pbi/           # ProductBacklogItem 相關功能
│       ├── create-product-backlog-item.json
│       └── estimate-product-backlog-item.json
└── README.md          # 規格文檔說明
```

這樣做的好處：
- 需求文檔化，便於追蹤
- AI 可以批量處理相關功能
- 團隊成員清楚了解系統功能

## 第六步：與 AI 協作開發

### 1. 日常開發流程

```bash
# 每次開始開發前
1. 確保 AI 讀取了記憶文件（CLAUDE.md）
2. 告訴 AI 你要做什麼
3. AI 會根據相應的 workflow 指導你
4. 遵循測試驅動開發（TDD）
5. 定期提交代碼
```

### 1.5. 多 AI 支援

使用不同 AI 助手時：
- **Claude**: 讀取 CLAUDE.md
- **Gemini**: 讀取 CLAUDE.md 或 GEMINI.md
- **其他 AI**: 讀取 CLAUDE.md 作為通用記憶

### 2. 使用 Workflow

AiScrum 常用 Sub-agent Workflows：
- **command-sub-agent**: Command Use Case 實作
- **query-sub-agent**: Query Use Case 實作
- **reactor-sub-agent**: Reactor 事件處理器
- **aggregate-sub-agent**: DDD Aggregate 實作
- **controller-sub-agent**: REST Controller 實作
- **outbox-sub-agent**: Outbox Pattern 實作
- **mutation-testing-sub-agent**: 提升 mutation coverage

### 3. 記錄決策

重要決策記錄在：
- `.dev/adr/`: 架構決策記錄 (ADRs)
- `.dev/tasks/`: 任務執行記錄和結果
- `.dev/ADR-INDEX.md`: ADR 快速參考索引

### 4. 持續學習

- 參考 `.ai/guides/LEARNING-PATH.md` 學習 DDD + CA + CQRS
- 查看 `.ai/CODE-TEMPLATES.md` 獲取程式碼模板
- 定期回顧並優化工作流程

## 常見問題

### Q1: 找不到 ezddd 依賴怎麼辦？

重要：現在所有 EZDDD 框架功能都由 ezapp-starter 提供，請使用正確的 Maven 座標：

```xml
<!-- EZ App Starter - 包含所有 EZDDD 框架功能 -->
<dependency>
    <groupId>tw.teddysoft.ezapp</groupId>
    <artifactId>ezapp-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- ezapp-starter 已包含以下所有功能：
     - ezddd-core (DDD 核心框架)
     - ezcqrs (CQRS 支援)
     - uContract (Design by Contract)
     - ezSpec (BDD 測試框架)
     - 所有 ezddd-gateway 功能 (Outbox Pattern 等)
-->

<!-- 注意：不需要單獨引入以下依賴，它們都已包含在 ezapp-starter 中：
     - tw.teddysoft.ezddd:ezddd-core
     - tw.teddysoft.ezddd:ezcqrs
     - tw.teddysoft.ucontract:uContract
     - tw.teddysoft.ezspec:ezspec-core
     - tw.teddysoft.ezddd-gateway:* 
-->

<!-- 其他必要依賴 -->
<dependency>
    <groupId>tw.teddysoft.ezspec</groupId>
    <artifactId>ezspec-core</artifactId>
    <version>2.0.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>tw.teddysoft.ezspec</groupId>
    <artifactId>ezspec-report</artifactId>
    <version>2.0.3</version>
    <scope>test</scope>
</dependency>
```

如果仍然找不到，請檢查：
1. groupId 是否正確（不同套件使用不同的 groupId）
2. artifactId 的大小寫是否正確（如 uContract 的 C 是大寫）
3. 使用 `mvn clean compile -U` 強制更新依賴

### Q2: 如何處理 Event 未註冊的錯誤？

參考 AiScrum 的 BootstrapConfig：

```java
// 在 io.springboot.config.BootstrapConfig.java
@PostConstruct
public void initDomainEventFactory() {
    ProductEvents.registerAllEvents();
    SprintEvents.registerAllEvents();
    ProductBacklogItemEvents.registerAllEvents();
    // 加入你的 events
}
```

### Q3: 如何組織大型專案？

- 使用 Bounded Context 劃分模組
- 每個 Aggregate 有獨立的頂層套件
- 共享的內容放在 common package
- 參考 `.ai/AGGREGATE-IDENTIFICATION-CHECKLIST.md`

### Q4: 測試跑不過怎麼辦？

1. **Profile 問題**: 確認使用正確的 profile (test-inmemory 或 test-outbox)
2. **不要在 BaseUseCaseTest 加 @ActiveProfiles**（參考 ADR-021）
3. **PostgreSQL**: 測試資料庫需要在 port 5800
4. **使用 ezSpec BDD**: Use Case 測試必須用 Given-When-Then
5. **執行測試**: `/opt/homebrew/bin/mvn test -q`

## 下一步

1. **深入學習**
   - 閱讀 `CLAUDE.md` - 專案核心記憶
   - 學習 `.ai/guides/LEARNING-PATH.md` - DDD + CA + CQRS 學習路徑
   - 查看 `.dev/ADR-INDEX.md` - ADR 快速參考
   - 使用 `.ai/CODE-TEMPLATES.md` - 程式碼模板庫

2. **團隊協作**
   - 分享這份指南給團隊成員
   - 定期更新 CLAUDE.md 記錄專案知識
   - 使用 ADR 記錄重要決策

3. **持續改進**
   - 根據專案需求調整 workflows
   - 優化 AI 協作配置
   - 執行 `.ai/scripts/check-coding-standards.sh` 確保品質

---

🎉 恭喜！你已經成功設置了一個基於 AiScrum 的 java-ca-ezddd-spring 專案。現在可以開始高效的 AI 輔助開發了！

如有任何問題，請參考：
- `CLAUDE.md` - 專案核心記憶
- `.ai/guides/LEARNING-PATH.md` - DDD + CA + CQRS 學習路徑
- `.dev/ADR-INDEX.md` - ADR 快速參考
- `.ai/CODE-TEMPLATES.md` - 程式碼模板庫
- `.ai/SUB-AGENT-SYSTEM.md` - Sub-agent 使用指南