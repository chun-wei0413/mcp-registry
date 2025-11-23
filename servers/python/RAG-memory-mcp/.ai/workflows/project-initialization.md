# Workflow: 專案初始化 (Project Initialization)

**標籤**: `#pure-workflow` `#project-setup` `#configuration`  
**整合狀態**: 📋 純流程 (無 Sub-agent)

## 概述

此工作流程指導 AI 完成新專案的初始化設置，包括產生 pom.xml、共用程式、基礎配置等。

## 目標

1. 快速建立可編譯的專案結構
2. 產生所有必要的共用程式
3. 設置正確的測試環境
4. 確保遵循框架規範

## 適用場景

- 新專案開始時
- 將框架整合到現有專案
- 需要重新產生基礎設施

## 工作流程

### 階段 1：檢查專案配置

**AI 行動**：
1. 讀取 `.dev/project-config.json`
2. 驗證必要欄位是否完整
3. 確認 rootPackage 設定正確
4. 檢查技術棧是否為 java-ca-ezddd-spring

**人類輸入**：
- 確認專案名稱和套件結構
- 提供缺少的配置資訊

**產出**：
- 完整的專案配置
- 確認的套件結構

### 階段 2：產生 Maven 配置

**AI 行動**：
1. 根據 `.dev/project-config.json` 產生 pom.xml
2. 包含所有必要的依賴
3. 設定正確的 plugin 配置
4. 所有 tw.teddysoft 依賴都已在 Maven Central，無需私有 repository

🚨 **強制使用正確的 Maven 依賴配置** 🚨
- ezddd-core: `<groupId>tw.teddysoft.ezddd</groupId><artifactId>ezddd-core</artifactId>`
- ezddd-gateway: `<groupId>tw.teddysoft.ezddd-gateway</groupId><artifactId>ez-esdb</artifactId>`
- ucontract: `<groupId>tw.teddysoft.ucontract</groupId><artifactId>uContract</artifactId>`
- ezspec: 
  - ezspec-core: `<groupId>tw.teddysoft.ezspec</groupId><artifactId>ezspec-core</artifactId>`
  - ezspec-report: `<groupId>tw.teddysoft.ezspec</groupId><artifactId>ezspec-report</artifactId>`

**⛔ 執行檢查點 1：Maven 配置驗證**
```
🔍 驗證清單：
□ pom.xml 已產生
□ ezddd-core 使用 groupId: tw.teddysoft.ezddd
□ ezddd-core 使用 artifactId: ezddd-core
□ 沒有使用錯誤的 artifactId (如 ezddd)
□ Java 版本設定為 21
□ Spring Boot 版本為 3.5.3
```

**參考文件**：
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/reference/maven-dependencies.md`
- `.dev/project-config.json`

**產出**：
- 完整的 pom.xml 檔案

### 階段 3：產生共用程式

**AI 行動**：
1. 讀取 `.ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md`
2. 根據 rootPackage 調整 package 宣告
3. 創建必要的目錄結構
4. 產生所有共用類別

🚨 **絕對禁止修改 import 語句** 🚨
- 必須完全照抄 .ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md 中的 import 語句
- 必須使用正確 import：
  - ✅ `tw.teddysoft.ezddd.entity.AggregateRoot`
  - ✅ `tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus`
  - ✅ `tw.teddysoft.ezddd.usecase.port.out.repository.Repository`

**⛔ 執行檢查點 2：Import 路徑驗證**
```
🔍 驗證清單：
□ 已讀取 local-utils.md 檔案
□ GenericInMemoryRepository 使用 tw.teddysoft.ezddd.entity.AggregateRoot
□ GenericInMemoryRepository 使用 tw.teddysoft.ezddd.usecase.port.out.repository.Repository
□ GenericInMemoryRepository 使用 tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus
□ 沒有使用任何 core package 的 import
□ 沒有使用任何錯誤的 import 路徑
```

**重要規則**：
⚠️ **只產生 .ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md 中定義的共用程式**
- ❌ 不要產生任何範例程式（如 Controller、Entity、Test）
- ❌ 不要產生任何 Domain 相關類別（如 Id、Aggregate）
- ❌ 不要自動創建 Use Case 或 Service
- ✅ 只產生 local-utils.md 中定義的共用類別

**必須產生的類別**：
```java
// 1. DateProvider - 在 common.entity package (src/main/java)
package [rootPackage].common.entity;

// 2. GenericInMemoryRepository - 在 common.adapter.out.repository package (src/main/java)
package [rootPackage].common.adapter.out.repository;

// 3. MyInMemoryMessageBroker - 在 common package (src/main/java)
package [rootPackage].common;

// 4. MyInMemoryMessageProducer - 在 common package (src/main/java)
package [rootPackage].common;
```

**⚠️ 重要**：所有類別都要放在 `src/main/java` 目錄，不是 test 目錄

**⛔ 執行檢查點 3：共用程式產生驗證**
```
🔍 驗證清單：
□ DateProvider.java 已產生在 src/main/java/[rootPackage]/common/entity/
□ GenericInMemoryRepository.java 已產生在 src/main/java/[rootPackage]/common/adapter/out/repository/
□ MyInMemoryMessageBroker.java 已產生在 src/main/java/[rootPackage]/common/
□ MyInMemoryMessageProducer.java 已產生在 src/main/java/[rootPackage]/common/
□ 所有檔案都在 src/main/java 而非 test 目錄
□ 只產生了這四個共用程式，沒有其他多餘檔案
```

**產出**：
- DateProvider.java
- GenericInMemoryRepository.java
- MyInMemoryMessageBroker.java
- MyInMemoryMessageProducer.java
- 正確的目錄結構

### 階段 4：創建 Spring Boot 基礎

**AI 行動**：
1. 產生 Application 主類（使用專案名稱，如 MyProjectApplication）
2. 創建專案套件結構：
   ```bash
   mkdir -p src/main/java/[rootPackage]/{plan,common}/{entity,usecase,adapter}
   mkdir -p src/main/java/[rootPackage]/io/springboot/config
   mkdir -p src/test/java/[rootPackage]/{plan,common}/{entity,usecase,adapter}
   ```
3. 創建 application.properties 配置（⚠️ 重要：絕對不要創建 .yml 檔案）
4. 產生測試用配置檔
5. 創建 BootstrapConfig（如使用 Event Sourcing）

**⛔ 執行檢查點 4：配置檔案驗證**
```
🔍 驗證清單：
□ 只產生了 application.properties（不是 yml）
□ 只產生了 application-test.properties（不是 yml）
□ 配置使用 PostgreSQL（不是 H2）
□ 沒有任何 .yml 檔案存在
□ Application 主類已產生
```

**配置檔案範例**：

`src/main/resources/application.properties`:
```properties
# Application Configuration
spring.application.name=[artifactId]
server.port=8080

# Database Configuration - PostgreSQL (Production)
spring.datasource.url=jdbc:postgresql://localhost:5432/[database_name]
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
```

`src/test/resources/application-test.properties`:
```properties
# Test Configuration
spring.application.name=[artifactId]-test

# PostgreSQL Test Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/[database_name]_test
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration for Test
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
```

**產出**：
- Spring Boot 啟動類
- 完整的配置檔案
- Clean Architecture 目錄結構
- 事件註冊配置（如需要）

### 階段 5：設置測試基礎設施

**AI 行動**：
1. 設置測試配置（使用 BlockingMessageBus）
2. 創建測試資料工廠
3. 設置整合測試基類

**測試相關類別**：
```java
// 使用 ezddd 框架內建的 BlockingMessageBus
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;

// TestDataFactory - 測試資料建構
package [rootPackage].testkit;

// BaseIntegrationTest - 整合測試基類
package [rootPackage].testkit;
```

**產出**：
- 完整的測試基礎設施
- 可重用的測試工具

### 階段 6：驗證和測試

**AI 行動**：
1. 執行 `mvn clean compile`
2. 檢查編譯錯誤
3. 修正任何問題（特別是 JUnit 版本衝突）
4. **🚨 執行 ezSpec 依賴完整性檢查**：
   ```bash
   # TODO: 需要實作 check-ezspec-dependencies.sh
   # 暫時手動檢查 pom.xml 中是否包含：
   # - ezspec-core
   # - ezspec-report
   ```
   - 確認 ezspec-core 和 ezspec-report 都存在
   - 若檢查失敗，立即補充缺少的依賴
5. 創建簡單的測試類別驗證設定
6. 執行 `mvn test` 確認測試環境
7. **🚨 強制執行 AI-COMPLIANCE-CHECK 驗證所有配置正確**
8. （選擇性）創建健康檢查端點

**健康檢查端點範例**：
```java
package [rootPackage].adapter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "[projectName]");
        return health;
    }
}
```

**驗證項目**：
- [ ] 專案可成功編譯
- [ ] Spring Boot 可正常啟動
- [ ] 測試環境正常運作
- [ ] 所有共用程式可用

## 執行順序建議

1. **基本初始化**（必須）
   - 產生 pom.xml
   - 產生共用程式
   - 創建主類

2. **配置設置**（必須）
   - application.properties
   - 測試配置

3. **進階設置**（選擇性）
   - Event Sourcing 配置
   - 測試基礎設施
   - 開發工具配置

## 常用指令

### 一鍵完整初始化（推薦）
```
🚨 **嚴格執行以下步驟，不得偏離** 🚨

請執行 project-initialization，特別注意：
1. **嚴格遵循 local-utils.md 中的程式碼範例**
2. **完全照抄 import 語句，不得做任何修改**  
3. **完成後執行 AI-COMPLIANCE-CHECK 驗證 import 正確性**

請直接執行 project-initialization workflow 的所有步驟，不要詢問確認：

📋 **執行清單（必須逐項完成）**：

□ 1. 讀取 .dev/project-config.json 並驗證配置
  └─ 確認 rootPackage、javaVersion=21、springBootVersion=3.5.3

□ 2. 根據配置產生完整的 pom.xml
  └─ 驗證 ezddd-core 使用正確 groupId/artifactId
  └─ 驗證 Java 版本為 21
  └─ ⚠️ **確認包含 ezspec-core 和 ezspec-report 兩個依賴**

□ 3. **必須讀取** .ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md
  └─ 確認已完整讀取檔案內容

□ 4. **必須產生** DateProvider 在 src/main/java/[rootPackage]/common/entity/DateProvider.java
  └─ 驗證檔案已產生在正確位置

□ 5. **必須產生** GenericInMemoryRepository 在 src/main/java/[rootPackage]/common/adapter/out/repository/
  └─ 驗證使用正確的 import（不是 core package）
  └─ 確認 import tw.teddysoft.ezddd.entity.AggregateRoot
  └─ 確認 import tw.teddysoft.ezddd.usecase.port.out.repository.Repository
  └─ 確認 import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus

□ 6. 創建目錄結構：src/main/java 和 src/test/java
  └─ 驗證所有必要目錄已創建

□ 7. 創建 Spring Boot 主類 Application.java
  └─ 驗證主類名稱符合專案名稱

□ 8. **絕對只能**產生 src/main/resources/application.properties
  └─ 驗證沒有產生 .yml 檔案
  └─ 確認使用 PostgreSQL 配置

□ 9. **絕對只能**產生 src/test/resources/application-test.properties
  └─ 驗證沒有產生 .yml 檔案
  └─ 確認使用 PostgreSQL 測試配置

□ 10. 如果 eventSourcing: true，創建 BootstrapConfig
  └─ 驗證 BootstrapConfig 已產生（如需要）

□ 11. 執行 mvn clean compile 驗證編譯
  └─ 確認編譯成功

□ 12. **執行 ezSpec 依賴檢查**
  └─ 執行 .ai/scripts/check-ezspec-dependencies.sh
  └─ 確認 ezspec-core 存在
  └─ 確認 ezspec-report 存在
  └─ 若有缺少，立即補充到 pom.xml

□ 13. 執行 AI-COMPLIANCE-CHECK 最終驗證
  └─ 確認所有檢查項目通過

□ 14. 報告執行結果
  └─ 列出所有產生的檔案
  └─ 報告任何問題或警告

🚨 **絕對禁止項目**：
- ❌ 產生 .yml 檔案
- ❌ 使用 H2 資料庫
- ❌ 忽略 local-utils.md
- ❌ 不產生 DateProvider 和 GenericInMemoryRepository
- ❌ 產生範例程式（Controller、Entity、Service、Test 等）
```

### 互動式初始化
```
請執行 project-initialization workflow，完成所有初始化步驟。

🚨 **執行要求**：
1. 嚴格遵循 local-utils.md 中的程式碼範例
2. 完全照抄 import 語句，不得做任何修改
3. 完成後執行 AI-COMPLIANCE-CHECK 驗證正確性

注意：只產生 .ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md 中的共用程式，不要產生任何範例程式。
```

### 只產生共用程式
```
請根據 .ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md 產生所有共用程式，使用 .dev/project-config.json 中的 rootPackage。

🚨 **執行要求**：
1. 嚴格遵循 local-utils.md 中的程式碼範例
2. 完全照抄 import 語句，不得做任何修改
3. 完成後執行 AI-COMPLIANCE-CHECK 驗證正確性

重要：只產生 DateProvider 和 GenericInMemoryRepository，不要產生其他類別。
```

### 修復編譯問題
```
專案編譯失敗，錯誤訊息：[貼上錯誤]
請檢查並修正問題
```

## 成功標準

- [ ] `mvn clean compile` 成功
- [ ] 所有共用程式都已產生
- [ ] package 結構正確
- [ ] Spring Boot 可啟動
- [ ] GenericInMemoryRepository 可用於測試

## 故障排除

### 常見問題

1. **找不到 ezddd 依賴**
   - 確認網路連線正常
   - 檢查 Maven Central 是否可以存取
   - 確認版本號碼正確（ezddd-core: 3.0.1）
   - 需要在 ezddd-core 依賴中排除舊版 junit：
   ```xml
   <dependency>
       <groupId>tw.teddysoft.ezddd</groupId>
       <artifactId>ezddd-core</artifactId>
       <version>${ezddd.version}</version>
       <exclusions>
           <exclusion>
               <groupId>junit</groupId>
               <artifactId>junit</artifactId>
           </exclusion>
       </exclusions>
   </dependency>
   ```

2. **JUnit 版本衝突**
   - Spring Boot 3.5.3 使用 JUnit 5.8.2
   - 不要明確指定 JUnit 版本，讓 Spring Boot parent 管理
   - 在 spring-boot-starter-test 中排除舊版 junit

3. **Package 不一致**
   - 檢查 .dev/project-config.json
   - 確認所有類別使用相同的 rootPackage

4. **Spring Boot 啟動失敗**
   - 檢查 application.properties（不是 yml）
   - 確認資料庫連線設定

5. **ucontract 依賴問題**
   - 確認 artifactId 是 `uContract`（C 大寫）
   - 版本: 2.0.0
   - 詳見 [依賴問題排查指南](../reference/DEPENDENCY-TROUBLESHOOTING.md)

## 相關資源

- [專案配置說明](../tech-stacks/java-ca-ezddd-spring/project-config-template.json)
- [共用程式規範](../tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md)
- [Maven 依賴說明](../tech-stacks/java-ca-ezddd-spring/examples/reference/maven-dependencies.md)

---

💡 **提示**：初始化是專案成功的第一步，確保所有基礎設施都正確設置！