# Archive 編碼規範

本文件定義 Archive Pattern 的編碼標準，負責處理 Query Model 的寫入資料庫需求。

## 📌 核心概念

**Archive** 是一種資料庫寫入模式，在 CQRS 架構中，專門用於 「Query Model」：
- 介面與 Write Model 的 Repository 相同，差別在於 Archive 負責 Query Model 的寫入資料庫需求，Repository 只限定在 Command Model 寫入單一 Aggregate 使用
- 可寫入單表格或跨表格
- Use Cases Layer 的 Reactor 物件收到 Domain Event 時呼叫 Archive，將資料寫入資料庫
 
## 🔴 必須遵守的規則 (MUST FOLLOW)

### 1. Archive Interface 設計

#### 套件位置
```java
// ✅ 正確：Archive 介面定義在 usecase.port.out.archive 套件
package tw.teddysoft.aiscrum.product.usecase.port.out.archive;

// ❌ 錯誤：不要放在其他位置
package tw.teddysoft.aiscrum.product.usecase.port.out;  // 缺少 archive
package tw.teddysoft.aiscrum.product.adapter.out;       // 不應在 adapter 層
```

#### 介面命名規範
```java
// ✅ 正確：使用 XxxArchive 命名（單數形）
public interface  UserArchive { }

// ❌ 錯誤：不要使用其他命名模式
public interface UserRepository { }  // 在 Read Model 不要用 Repository
public interface IUserArchive { }    // 不要加 I 前綴
public interface UserDtoArchive { }  // 舊規範，不要用 DtoArchive
```
#### 介面繼承規範
```java
import tw.teddysoft.ezddd.cqrs.usecase.query.Archive;

// ✅ 正確：繼承 Archive<I, ID> 介面
public interface UserArchive extends Archive<UserData, String> {
    // findById, save, delete 方法由 Archive 介面定義，不需要重複宣告
}

// ❌ 錯誤：不繼承 Archive 介面
public interface UserArchive<T, ID> {
    Optional<T> findById(ID id);
    void save(T data);
    void delete(T data);
}
```

#### 方法設計原則
```java
import tw.teddysoft.ezddd.cqrs.usecase.query.Archive;

public interface UserArchive extends Archive<UserData, String> {
}

```

#### 返回類型規範
```java
public class RdbUserArchive implements UserArchive {
    
    // ✅ 正確：返回 DATA (Persistence Object) 物件
    public Optional<UserData> findById(String userId) {
    }

    // ❌ 錯誤：不要返回領域物件
    Optional<User> findById(String userId);
    
    // ❌ 錯誤：不要返回 DTO（Use Case 層負責轉換）
    Optional<UserDto> findById(String userId);
}


```

### 2. Archive 實作

#### 實作位置
```java
// ✅ 正確：實作放在 adapter.out.database.springboot.archive 套件
package tw.teddysoft.aiscrum.product.adapter.out.database.springboot.archive;
```

#### JPA Archive 實作範例

先宣告 OrmClient
```java
package tw.teddysoft.aiscrum.io.springboot.config.orm;

import tw.teddysoft.aiscrum.user.usecase.port.out.UserData;
import org.springframework.data.repository.CrudRepository;

public interface UserOrmClient extends CrudRepository<UserData, String> {
}
```

實作 JapArchive
```java
package tw.teddysoft.aiscrum.product.adapter.out.database.springboot.archive;

import java.util.Optional;

// ⚠️ 重要：不要加 @Repository 註解，Spring Data JPA 會自動產生 bean
public class JapUserArchive implements UserArchive {

    private UserOrmClient userOrmClient;

    public RdbUserDtoArchive(UserOrmClient userOrmClient) {
        Objects.requireNonNull(userOrmClient, "userOrmClient cannot be null");
        this.userOrmClient = userOrmClient;
    }

    @Override
    public Optional<UserData> findById(String userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        return userOrmClient.findById(userId);
    }

    @Override
    public void save(UserData user) {
        Objects.requireNonNull(user, "user cannot be null");

        userOrmClient.save(user);
    }

    @Override
    public void delete(UserData user) {
        Objects.requireNonNull(user, "user cannot be null");
        userOrmClient.delete(UserData);
    }
}
```

#### ⚠️ 重要：JPA Archive Bean 管理方式

JPA Archive 有兩種 bean 管理方式：

##### 明確宣告 Bean
```java
@Configuration
@Profile("outbox")
public class OutboxArchiveConfig {

    private UserOrmClient userOrmClient;

    @Autowired
    public OutboxArchiveConfig( UserOrmClient userOrmClient){
        this.userOrmClient = userOrmClient;
    }

    @Bean(name = "userArchive")
    public UserArchive userArchive() {
        return new JapUserArchive(userDtoOrmClientInBoard);
    }
}
```

**常見錯誤**：
- ❌ 在 JPA Archive 介面上加 `@Repository` 註解（不需要）
- ❌ 嘗試手動實例化 JPA interface（如 `new JapUserArchive()`）

#### InMemory Archive 實作範例
```java
// TODO
```

### 3. Spring Configuration

#### Profile-based 配置
```java
@Configuration
@Profile("outbox")
public class OutboxArchiveConfig {

    private UserOrmClient userOrmClient;

    @Autowired
    public OutboxArchiveConfig( UserOrmClient userOrmClient){
        this.userOrmClient = userOrmClient;
    }

    @Bean(name = "userArchive")
    public UserArchive userArchive() {
        return new JapUserArchive(userDtoOrmClientInBoard);
    }
}
```

## 🎯 使用場景指南

### 1. 何時使用 Archive
- ✅ Query Model 的 CRUD
- ❌ Write Model 的 CRUD 操作（使用 Repository）

### 2. 與 Repository 的區別
- **Archive**: 用於 Read Model 的 CRUD 操作
- **Repository**: 用於 Write Model 的 CRUD 操作

## 🔍 檢查清單

### Archive Interface
- [ ] 定義在 `usecase.port.out.archive` 套件
- [ ] 使用 `XxxArchive` 命名（單數形）
- [ ] 繼承 `Archive<T, ID>` 介面
- [ ] T 類別實作 `Data` 物件，例如 UserData
- [ ] 只依賴繼承的 `findById`, `save`, `deelte` 方法，不自行宣告其他方法
- [ ] 返回 Data (Persistence Object) 而非領域物件或 DTO

### Archive 實作
- [ ] 實作在 `adapter.out.database.springboot.archive` 套件
- [ ] **JPA Archive 不要加 `@Repository` 註解**（宣告 @Bean）
- [ ] 處理 null 值和空集合

### Spring Configuration
- [ ] 使用 @Profile 區分不同環境
- [ ] 使用 @ConditionalOnMissingBean 避免衝突
- [ ] 正確配置 Bean 優先順序
- [ ] **JPA Archive: 選擇合適的 bean 管理方式（明確宣告）**

## 📚 相關文件
- [Repository 規範](./repository-standards.md)
- [Use Case 規範](./usecase-standards.md)
- [Inquiry Pattern 指南](../examples/inquiry-archive/README.md)
- [Query Use Case 實作指引](../../prompts/query-sub-agent-prompt.md)