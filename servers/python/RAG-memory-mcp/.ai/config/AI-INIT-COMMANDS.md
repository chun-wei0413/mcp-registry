# AI 專案初始化指令集

> 🚨 **重要**：這是 project-initialization 的執行前必讀文件

## 📋 執行前檢查清單

在執行 project-initialization 之前，請確認已理解以下所有要求：

### 1. Maven 依賴正確性
**🔥 參考模板**: `.ai/tech-stacks/java-ca-ezddd-spring/examples/pom/pom.xml`
- 使用經過驗證的完整 pom.xml 模板
- 版本號以 .dev/project-config.json 為準

### 2. Import 路徑正確性
```java
// ✅ 正確的 import
import tw.teddysoft.ezddd.entity.AggregateRoot;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
```

### 3. 配置檔案格式
**🔥 參考模板**: `.ai/tech-stacks/java-ca-ezddd-spring/examples/spring/`
- application.properties（主配置）
- application-inmemory.properties（InMemory profile）
- application-outbox.properties（Outbox profile）
- application-eventsourcing.properties（EventSourcing profile）

### 4. Repository 配置
```properties
# ✅ 正確 - 預設使用 InMemory Repository
# 單元測試不需要資料庫配置

# 當需要使用資料庫時，配置 PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
```

### 5. 必須產生的檔案
- ✅ DateProvider.java（在 src/main/java）
- ✅ GenericInMemoryRepository.java（在 src/main/java）
- ❌ 不要產生範例 Controller、Entity、Service、Test

## 🚀 標準執行指令

### 完整初始化（複製此段落執行）
```
請執行 project-initialization workflow：

1. 先讀取 AI-INIT-COMMANDS.md 理解所有要求
2. 讀取 .dev/project-config.json 取得專案配置
3. 複製 .ai/tech-stacks/java-ca-ezddd-spring/examples/pom/pom.xml 作為基礎
   - ⚠️ 必須用 .dev/project-config.json 中的版本號替換模板中的版本
4. 複製 .ai/tech-stacks/java-ca-ezddd-spring/examples/spring/ 下的所有 properties 檔案
5. 讀取 .ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md
6. 產生 DateProvider.java（從 local-utils.md）
7. 產生 GenericInMemoryRepository.java（從 local-utils.md）
8. 產生 MyInMemoryMessageBroker.java（從 local-utils.md）
9. 產生 MyInMemoryMessageProducer.java（從 local-utils.md）
10. 產生 Spring Boot Application 主類
11. 執行 mvn clean compile 驗證
12. 執行 mvn test 確認測試通過
```

## ⚠️ 常見錯誤預防

### 錯誤 1：使用錯誤的 Maven artifactId
**預防**：使用 `ezapp-starter` 包含所有 EZDDD 功能

### 錯誤 2：使用 core package 的 import
**預防**：完全照抄 local-utils.md 中的 import

### 錯誤 3：產生 yml 檔案
**預防**：只產生 .properties 檔案（yml 格式被絕對禁止）

### 錯誤 4：使用錯誤的資料庫配置
**預防**：預設使用 InMemory Repository，整合測試時使用 PostgreSQL

### 錯誤 5：GenericInMemoryRepository 放在 test 目錄
**預防**：兩個共用類別都放在 src/main/java

## 📝 執行後驗證

執行完成後，請回答以下問題：
1. pom.xml 中的 ezapp-starter 是否正確設定？
2. GenericInMemoryRepository 的 import 是否正確？
3. 是否只產生了 .properties 檔案？
4. DateProvider 和 GenericInMemoryRepository 是否都在 src/main/java？
5. 編譯是否成功？

## 🔄 錯誤修正流程

如果執行後發現錯誤：
1. 立即停止
2. 重新讀取 AI-INIT-COMMANDS.md
3. 執行 check-coding-standards.sh 找出問題
4. 修正錯誤後重新編譯

---

💡 **記住**：寧可慢一點，也要確保每個步驟都正確！