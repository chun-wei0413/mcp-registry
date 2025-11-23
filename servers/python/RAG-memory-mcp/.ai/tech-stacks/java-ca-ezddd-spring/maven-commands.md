# Maven 命令指南

## 依賴管理

### 添加 Maven 依賴
```
參考 maven-dependencies.md, 添加依賴到 pom.xml
```

### 更新依賴版本
```
更新 pom.xml 中的 spring-boot 版本為 3.2.0
```

### 依賴分析
```
執行 mvn dependency:tree 分析依賴關係
```

## 常用 Maven 命令

### 建置命令
- `mvn clean compile` - 清理並編譯
- `mvn test` - 執行測試
- `mvn package` - 打包專案
- `mvn install` - 安裝到本地倉庫

### 測試命令
- `mvn test -Dtest=TestClassName` - 執行特定測試
- `mvn verify` - 執行整合測試
- `mvn test -DskipTests` - 跳過測試

### 報告命令
- `mvn site` - 生成專案網站
- `mvn javadoc:javadoc` - 生成 JavaDoc
- `mvn surefire-report:report` - 生成測試報告

## 專案初始化

### 創建新專案
```
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=project-name \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

### 創建 Spring Boot 專案
```
參考 spring-initializr 配置, 創建 Spring Boot 專案結構
```

## 依賴配置範例

### 版本屬性配置
```xml
<properties>
    <!-- Core library versions -->
    <ezddd.version>3.0.1</ezddd.version>
    <ezddd-gateway.version>1.0.0</ezddd-gateway.version>
    <ezspec.version>2.0.3</ezspec.version>
    <ucontract.version>2.0.0</ucontract.version>
    
    <!-- Spring Boot version -->
    <spring-boot.version>3.5.3</spring-boot.version>
    
    <!-- Jakarta EE -->
    <jakarta.persistence-api.version>3.1.0</jakarta.persistence-api.version>
</properties>
```

### 完整依賴結構 (tw.teddysoft.*)
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- EZDDD Core Framework -->
    <!-- ⚠️ 重要：ezddd-core 是 aggregator POM，實際類別在子模組中 -->
    <!-- 使用時 import tw.teddysoft.ezddd.entity.* 而非 tw.teddysoft.ezddd.core.* -->
    <dependency>
        <groupId>tw.teddysoft.ezddd</groupId>
        <artifactId>ezddd-core</artifactId>
        <version>${ezddd.version}</version>
    </dependency>
    
    <dependency>
        <groupId>tw.teddysoft.ezddd</groupId>
        <artifactId>ezcqrs</artifactId>
        <version>${ezddd.version}</version>
    </dependency>
    
    <!-- EZDDD Gateway Components (Outbox Pattern & Event Store) -->
    <dependency>
        <groupId>tw.teddysoft.ezddd-gateway</groupId>
        <artifactId>ez-outbox</artifactId>
        <version>${ezddd-gateway.version}</version>
    </dependency>
    
    <dependency>
        <groupId>tw.teddysoft.ezddd-gateway</groupId>
        <artifactId>ez-esdb</artifactId>
        <version>${ezddd-gateway.version}</version>
    </dependency>
    
    <dependency>
        <groupId>tw.teddysoft.ezddd-gateway</groupId>
        <artifactId>ez-message</artifactId>
        <version>${ezddd-gateway.version}</version>
    </dependency>
    
    <dependency>
        <groupId>tw.teddysoft.ezddd-gateway</groupId>
        <artifactId>ez-es</artifactId>
        <version>${ezddd-gateway.version}</version>
    </dependency>
    
    <!-- Design by Contract -->
    <dependency>
        <groupId>tw.teddysoft.ucontract</groupId>
        <artifactId>uContract</artifactId>
        <version>${ucontract.version}</version>
    </dependency>
    
    <!-- Jakarta Persistence API -->
    <dependency>
        <groupId>jakarta.persistence</groupId>
        <artifactId>jakarta.persistence-api</artifactId>
        <version>${jakarta.persistence-api.version}</version>
    </dependency>
    
    <!-- Testing Dependencies -->
    <dependency>
        <groupId>tw.teddysoft.ezspec</groupId>
        <artifactId>ezspec-core</artifactId>
        <version>${ezspec.version}</version>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>tw.teddysoft.ezspec</groupId>
        <artifactId>ezspec-report</artifactId>
        <version>${ezspec.version}</version>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Maven Central 配置

所有 tw.teddysoft 的依賴都已在 Maven Central 上提供，無需配置私有倉庫。

### ⚠️ 極其重要的注意事項
**ezddd-core 是 aggregator POM，不包含實際類別！**
- 雖然在 pom.xml 中宣告 `ezddd-core`
- 但 import 時**絕對不要使用** `tw.teddysoft.ezddd.core.*`
- 正確的 import 路徑參見：`.ai/tech-stacks/java-ca-ezddd-spring/examples/reference/ezddd-import-mapping.md`

### 最新可用版本 (2025-09-04)
- ezddd-core (3.0.1) - DDD 核心框架
- ezcqrs (3.0.1) - CQRS 支援模組
- ezddd-gateway 系列 (1.0.0) - Outbox Pattern 與 Event Store
  - ez-outbox - Outbox Pattern 實作
  - ez-esdb - Event Store 資料庫連接
  - ez-message - 訊息處理
  - ez-es - Event Sourcing 支援
- ezspec (2.0.3) - BDD 測試框架
- ucontract (2.0.0) - Design by Contract

## 常見問題處理

### 依賴下載失敗
1. 檢查網路連接
2. 確認倉庫配置
3. 清理本地快取：`mvn clean -U`

### 版本衝突
1. 使用 `mvn dependency:tree` 查看衝突
2. 使用 `<exclusions>` 排除衝突依賴
3. 統一版本管理使用 `<dependencyManagement>`

## 與 CI/CD 整合

### GitHub Actions
```yaml
- name: Build with Maven
  run: mvn clean verify
  
- name: Generate reports
  run: mvn site
```

### 快取依賴
```yaml
- uses: actions/cache@v3
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
```