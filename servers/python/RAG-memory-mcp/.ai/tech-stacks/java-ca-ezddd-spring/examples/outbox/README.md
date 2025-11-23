# Outbox Repository Pattern Implementation Guide

## 概述
本指南說明如何實作符合 ezddd 和 ezddd-gateway 規範的 Outbox Repository 模式。

## 目錄
1. [Outbox 模式介紹](#outbox-模式介紹)
2. [ezddd-gateway 整合](#ezddd-gateway-整合)
3. [實作步驟](#實作步驟)
4. [範例程式碼](#範例程式碼)
5. [測試指南](#測試指南)
6. [最佳實踐](#最佳實踐)

## Outbox 模式介紹

### 什麼是 Outbox Pattern？

Outbox Pattern 是一種確保分散式系統中事件發布可靠性的設計模式。它通過將領域事件儲存在與業務資料相同的資料庫交易中，解決了傳統事件發布的「雙寫問題」（Dual Write Problem）。

### 核心問題：雙寫問題

在傳統的事件驅動架構中，我們經常遇到這樣的場景：

```java
// ❌ 有問題的實作
@Transactional
public void createOrder(Order order) {
    // 步驟 1: 儲存訂單到資料庫
    orderRepository.save(order);
    
    // 步驟 2: 發布訂單建立事件到訊息佇列
    eventPublisher.publish(new OrderCreatedEvent(order));  // 如果這裡失敗怎麼辦？
}
```

**問題分析**：
- 如果步驟 1 成功但步驟 2 失敗，訂單已儲存但事件未發布，導致資料不一致
- 如果將步驟 2 移到交易外執行，可能因系統崩潰而遺失事件
- 無法保證資料庫操作和訊息發布的原子性

### Outbox Pattern 解決方案

Outbox Pattern 將事件先儲存到資料庫的 Outbox 表中，與業務資料在同一個交易中提交，然後由獨立的發布器非同步讀取並發布事件。

```java
// ✅ 使用 Outbox Pattern
@Transactional
public void createOrder(Order order) {
    // 步驟 1: 儲存訂單
    orderRepository.save(order);
    
    // 步驟 2: 將事件儲存到 Outbox 表（同一個交易）
    outboxRepository.save(new OutboxEvent(
        order.getId(),
        "OrderCreatedEvent",
        order.toEventData()
    ));
    // 交易提交後，訂單和事件都已安全儲存
}

// 獨立的背景服務負責發布事件，在 ezddd-gateway 中稱為 Relay
@Scheduled(fixedDelay = 5000)
public void publishOutboxEvents() {
    List<OutboxEvent> unpublished = outboxRepository.findUnpublished();
    for (OutboxEvent event : unpublished) {
        eventPublisher.publish(event);
        outboxRepository.markAsPublished(event.getId());
    }
}
```

### 運作流程

```
┌─────────────────────────────────────────────────────────────┐
│                     同一個資料庫交易                           │
│                                                             │
│  ┌──────────────┐        ┌──────────────┐                   │
│  │   業務資料    │ ──────▶ │  Outbox 表   │                   │
│  │  (Order)     │  儲存   │   (Event)    │                   │
│  └──────────────┘        └──────────────┘                   │
│                                  │                          │
└─────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
                          ┌──────────────────┐
                          │  Outbox 發布器    │ (非同步)
                          │   (Relay)        │
                          └──────────────────┘
                                   │
                                   ▼
                          ┌──────────────────┐
                          │   訊息佇列        │
                          │ (Kafka/RabbitMQ) │
                          └──────────────────┘
```

### 主要優勢

1. **交易一致性**
   - 業務資料和事件在同一個資料庫交易中提交
   - 要嘛全部成功，要嘛全部失敗，確保原子性

2. **可靠性保證**
   - 事件不會因為訊息中間件暫時不可用而遺失
   - 系統崩潰後可以從 Outbox 表恢復並繼續發布

3. **順序保證**
   - 透過序號（sequence number）確保事件按正確順序發布
   - 對於需要順序處理的業務場景特別重要

4. **冪等性支援**
   - 每個事件有唯一 ID，接收方可以去重
   - 避免網路重試導致的重複處理

5. **可觀測性**
   - 所有事件都有記錄，便於審計和除錯
   - 可以追蹤事件的發布狀態和重試次數

### 適用場景

✅ **建議使用 Outbox Pattern 的場景**：
- 微服務架構中的跨服務通訊
- 需要保證事件不遺失的關鍵業務流程
- Event Sourcing 架構
- CQRS 中的命令和查詢分離
- 需要審計追蹤的系統

❌ **不適合的場景**：
- 單體應用內部的事件處理
- 對即時性要求極高的場景（有輪詢延遲）
- 事件量極大的系統（需要考慮 Outbox 表的效能）

### 實作考量

1. **Outbox 表清理策略**
   - 定期清理已發布的舊事件（如：30 天前的）
   - 避免表無限增長影響效能

2. **失敗重試機制**
   - 實作指數退避（exponential backoff）
   - 設定最大重試次數，超過後進入死信佇列

3. **效能優化**
   - 批次讀取和發布事件
   - 為查詢欄位建立適當的索引
   - 考慮分區表（partitioning）策略

4. **監控和告警**
   - 監控未發布事件的數量
   - 追蹤發布延遲
   - 失敗事件的告警機制

## 在 ezddd-gateway 中實作 Outbox Repository for Aggregate
說明如何與 ezapp-starter 框架整合...

## 實作步驟

### 1. 定義 Outbox Entity
針對每一個 Aggregate，定義 '[Aggregate]Data implements OutboxData<String>' 的 Persistence Object。
[Aggregate]Data 放在 '[rootPackage].[aggregate].usecase.port.out' package
```java
package tw.teddysoft.example.plan.usecase.port.out;

@Entity
@Table(name = "plan")
public class PlanData implements OutboxData<String> {

    @Transient
    private List<DomainEventData> domainEventDatas;

    @Transient
    private String streamName;

    @Id
    @Column(name = "id")
    private String planId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "next_task_id", nullable = false)
    private int nextTaskId;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "planData")
    private Set<ProjectData> projectDatas;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Version
    @Column(columnDefinition = "bigint DEFAULT 0", nullable = false)
    private long version;

    public PlanData() {
        this(0L);
    }

    public PlanData(long version) {
        projectDatas = new HashSet<>();
        this.version = version;
        this.domainEventDatas = new ArrayList<>();
        this.isDeleted = false;
        this.nextTaskId = 0;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getNextTaskId() {
        return nextTaskId;
    }

    public void setNextTaskId(int nextTaskId) {
        this.nextTaskId = nextTaskId;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Set<ProjectData> getProjectDatas() {
        return projectDatas;
    }

    public void setProjectDatas(Set<ProjectData> projectDatas) {
        this.projectDatas = projectDatas;
    }

    public void addProjectData(ProjectData projectData) {
        projectData.setPlanData(this);
        this.projectDatas.add(projectData);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    @Transient
    public String getId() {
        return planId;
    }

    @Override
    @Transient
    public void setId(String id) {
        this.planId = id;
    }

    @Override
    @Transient
    public List<DomainEventData> getDomainEventDatas() {
        return this.domainEventDatas;
    }

    @Override
    @Transient
    public void setDomainEventDatas(List<DomainEventData> domainEventDatas) {
        this.domainEventDatas = domainEventDatas;
    }

    @Override
    @Transient
    public String getStreamName() {
        return streamName;
    }

    @Override
    @Transient
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }
}
```

### 2. 在 [Aggregate]Mapper 類別中實作靜態方法 toData 與 toDomain
```java
package tw.teddysoft.example.plan.usecase.port;

public class PlanMapper {

    public static PlanData toData(Plan plan) {
        requireNotNull("Plan", plan);

        PlanData planData = new PlanData(plan.getVersion());
        planData.setPlanId(plan.getId().value());
        planData.setName(plan.getName());
        planData.setUserId(plan.getUserId());
        planData.setNextTaskId(0); // This would need to be exposed by Plan entity
        planData.setDeleted(plan.isDeleted());
        // Set timestamps based on domain events
        if (!plan.getDomainEvents().isEmpty()) {
            // Set createdAt to the timestamp of the first event (should be PlanCreated)
            planData.setCreatedAt(plan.getDomainEvents().get(0).occurredOn());
            // Set lastUpdated to the timestamp of the most recent event
            planData.setLastUpdated(plan.getDomainEvents().get(plan.getDomainEvents().size() - 1).occurredOn());
        } else {
            // Fallback to current time if no events (shouldn't happen in normal flow)
            planData.setCreatedAt(DateProvider.now());
            planData.setLastUpdated(DateProvider.now());
        }
        planData.setStreamName(plan.getStreamName());
        planData.setDomainEventDatas(plan.getDomainEvents().stream()
                .map(DomainEventMapper::toData)
                .collect(Collectors.toList()));

        // Synchronize projects with the domain model
        // Remove projects that no longer exist in the domain
        planData.getProjectDatas().removeIf(projectData ->
                !plan.getProjects().containsKey(ProjectId.valueOf(projectData.getProjectId()))
        );

        // Add or update projects from the domain
        for (Project project : plan.getProjects().values()) {
            // Check if project already exists in planData
            ProjectData existingProjectData = planData.getProjectDatas().stream()
                    .filter(pd -> pd.getProjectId().equals(project.getId().value()))
                    .findFirst()
                    .orElse(null);

            if (existingProjectData == null) {
                // Add new project
                ProjectData projectData = ProjectMapper.toData(project);
                planData.addProjectData(projectData);
            } else {
                // Update existing project's tasks
                // Remove tasks that no longer exist in the domain
                existingProjectData.getTaskDatas().removeIf(taskData ->
                        !project.getTasks().containsKey(TaskId.valueOf(taskData.getTaskId()))
                );

                // Add or update tasks from the domain
                for (Task task : project.getTasks().values()) {
                    // Check if task already exists
                    TaskData existingTaskData = existingProjectData.getTaskDatas().stream()
                            .filter(td -> td.getTaskId().equals(task.getId().value()))
                            .findFirst()
                            .orElse(null);

                    if (existingTaskData == null) {
                        // Add new task
                        TaskData taskData = TaskMapper.toData(task, project.getId().value());
                        existingProjectData.addTaskData(taskData);
                    } else {
                        // Update existing task
                        existingTaskData.setName(task.getName());
                        existingTaskData.setDone(task.isDone());

                        // Update tag IDs from domain model
                        existingTaskData.getTagIds().clear();
                        for (tw.teddysoft.example.tag.entity.TagId tagId : task.getTags()) {
                            existingTaskData.getTagIds().add(tagId.value());
                        }
                    }
                }
            }
        }

        return planData;
    }

    public static List<PlanData> toData(List<Plan> plans) {
        List<PlanData> result = new ArrayList<>();
        plans.forEach(x -> result.add(toData(x)));
        return result;
    }

    public static Plan toDomain(PlanData planData) {
        requireNotNull("PlanData", planData);

        // Reconstruct Plan from events if available
        if (planData.getDomainEventDatas() != null && !planData.getDomainEventDatas().isEmpty()) {
            // Convert domain event data back to domain events
            var domainEvents = planData.getDomainEventDatas().stream()
                    .map(DomainEventMapper::toDomain)
                    .map(event -> (PlanEvents) event)
                    .collect(Collectors.toList());

            // Create Plan from events
            Plan plan = new Plan(domainEvents);
            plan.setVersion(planData.getVersion());
            plan.clearDomainEvents();
            return plan;
        } else {
            // Create Plan and reconstruct its state from PlanData
            List<PlanEvents> events = new ArrayList<>();

            // Create PlanCreated event
            events.add(new PlanEvents.PlanCreated(
                    PlanId.valueOf(planData.getPlanId()),
                    planData.getName(),
                    planData.getUserId(),
                    UUID.randomUUID(),
                    planData.getCreatedAt()
            ));

            // Create ProjectCreated events for each project
            for (ProjectData projectData : planData.getProjectDatas()) {
                events.add(new PlanEvents.ProjectCreated(
                        PlanId.valueOf(planData.getPlanId()),
                        ProjectId.valueOf(projectData.getProjectId()),
                        ProjectName.valueOf(projectData.getName()),
                        UUID.randomUUID(),
                        planData.getCreatedAt()
                ));

                // Create TaskCreated events for each task in the project
                for (TaskData taskData : projectData.getTaskDatas()) {
                    events.add(new PlanEvents.TaskCreated(
                            PlanId.valueOf(planData.getPlanId()),
                            ProjectId.valueOf(projectData.getProjectId()),
                            TaskId.valueOf(taskData.getTaskId()),
                            taskData.getName(),
                            UUID.randomUUID(),
                            planData.getCreatedAt()
                    ));

                    // If task is done, create TaskChecked event
                    if (taskData.isDone()) {
                        events.add(new PlanEvents.TaskChecked(
                                PlanId.valueOf(planData.getPlanId()),
                                ProjectId.valueOf(projectData.getProjectId()),
                                TaskId.valueOf(taskData.getTaskId()),
                                UUID.randomUUID(),
                                planData.getLastUpdated()
                        ));
                    }

                    // If task has deadline, create TaskDeadlineSet event
                    if (taskData.getDeadline() != null) {
                        events.add(new PlanEvents.TaskDeadlineSet(
                                PlanId.valueOf(planData.getPlanId()),
                                ProjectId.valueOf(projectData.getProjectId()),
                                TaskId.valueOf(taskData.getTaskId()),
                                taskData.getDeadline().toString(),
                                UUID.randomUUID(),
                                planData.getLastUpdated()
                        ));
                    }

                    // Create TagAssigned events for existing tags
                    for (String tagId : taskData.getTagIds()) {
                        events.add(new PlanEvents.TagAssigned(
                                PlanId.valueOf(planData.getPlanId()),
                                ProjectId.valueOf(projectData.getProjectId()),
                                TaskId.valueOf(taskData.getTaskId()),
                                tw.teddysoft.example.tag.entity.TagId.valueOf(tagId),
                                UUID.randomUUID(),
                                planData.getLastUpdated()
                        ));
                    }
                }
            }

            // If plan is deleted, add PlanDeleted event
            if (planData.isDeleted()) {
                events.add(new PlanEvents.PlanDeleted(
                        PlanId.valueOf(planData.getPlanId()),
                        UUID.randomUUID(),
                        planData.getLastUpdated()
                ));
            }

            // Create Plan from reconstructed events
            Plan plan = new Plan(events);
            plan.setVersion(planData.getVersion());
            plan.clearDomainEvents();
            return plan;
        }
    }

    public static List<Plan> toDomain(List<PlanData> planDatas) {
        requireNotNull("PlanData list", planDatas);

        List<Plan> result = new ArrayList<>();
        planDatas.forEach(x -> result.add(toDomain(x)));
        return result;
    }
}
```

### 3. 在 [Aggregate]Mapper 類別中實作 inner class Mapper 與 static OutboxMapper newMapper () method
```java
package tw.teddysoft.example.plan.usecase.port;

public class PlanMapper {

    // Only for aggregate mappers
    private static OutboxMapper mapper = new PlanMapper.Mapper();

    // Only for aggregate mappers
    public static OutboxMapper newMapper () {
        return mapper;
    }

    // Only for aggregate mappers
    static class Mapper implements OutboxMapper<Plan, PlanData> {

        @Override
        public Plan toDomain(PlanData data) {
            return PlanMapper.toDomain(data);
        }

        @Override
        public PlanData toData(Plan aggregateRoot) {
            return PlanMapper.toData(aggregateRoot);
        }
    }
}
```

### 4. 在 [rootPackage].io.springboot.config.orm package 新增 [Aggregate]OrmClient.java interface
```java
import tw.teddysoft.example.plan.usecase.port.out.PlanData;
import tw.teddysoft.ezddd.data.io.ezoutbox.SpringJpaClient;

/**
 * Interface to generate bean for JPA CRUDRepository
 * This will be used by PostgresOutboxStoreClient for Plan aggregate
 */
public interface PlanOrmClient extends SpringJpaClient<PlanData, String> {
}
```

### 5. 在 SpringBoot BootstrapConfig.java 檔案宣告 PgMessageDbClient
```java

@Bean(name = "entityManagerIn[BoundedContextName]")
public EntityManager getEntityManager(final @Qualifier("[BoundedContextName]EntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
    return entityManagerFactoryBean.getObject().createEntityManager();
}
@Bean(name = "pgMessageDbClientIn[BoundedContextName]")
public PgMessageDbClient pgMessageDbClient(final @Qualifier("entityManagerIn[BoundedContextName]") EntityManager entityManager) {
    RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
    return factory.getRepository(PgMessageDbClient.class);
}
```

### 6. 在 [rootPackage].io.springboot.config package 的 RepositoryConfig.java 檔案宣告 PgMessageDbClient 與 [Aggregate]OrmClient
```java
@Configuration("PlanRepositoryInjection")
@EnableConfigurationProperties(value = ConfigProperty.class)
@AutoConfigureAfter({DataSourceConfig.class, BootstrapConfiguration.class})
public class RepositoryConfig {

    private PgMessageDbClient pgMessageDbClient;
    private PlanOrmClient planOrmStoreClient;

    @Autowired
    public RepositoryConfig(
            @Qualifier("domainEventTypeMapperInPlan") DomainEventTypeMapper domainEventTypeMapper,
            PlanOrmClient planOrmStoreClient,
            @Qualifier("pgMessageDbClientInPlan") PgMessageDbClient pgMessageDbClient
            ) {
    
        this.domainEventTypeMapper = domainEventTypeMapper;
        this.pgMessageDbClient = pgMessageDbClient;
        this.planOrmStoreClient = planOrmStoreClient;
    }
}
```

### 7. 在 [rootPackage].io.springboot.config package 的 RepositoryConfig.java 檔案宣告 Aggregate 使用的 OutboxStore
```java
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxStore;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxClient;

@Bean
public OutboxStore<PlanData, String> planOutboxStore() {
    return EzOutboxStoreAdapter.createOutboxStore(new EzOutboxClient<>(planOrmStoreClient, pgMessageDbClient));
}
```

## 範例程式碼
[完整範例程式碼連結]

## Spring Profile 配置

### 使用 @Profile 進行條件啟用

Outbox 模式可以透過 Spring Profile 機制進行條件啟用，這允許在不同環境中靈活切換 Repository 實作。

#### 配置類別設定
```java
@Configuration
@Profile("outbox")  // 只有當 'outbox' profile 啟用時才載入此配置
public class OutboxRepositoryConfig {
    
    @Bean
    public Repository<Product, ProductId> productRepository(
            OutboxStore<ProductData, String> productOutboxStore,
            MessageBus<DomainEvent> messageBus) {
        return new OutboxRepository<>(
            new OutboxRepositoryPeerAdapter<>(productOutboxStore), 
            ProductMapper.newMapper()
        );
    }
}
```

#### 啟用方式

1. **透過 application.properties**
```properties
spring.profiles.active=outbox
```

2. **透過命令列參數**
```bash
java -jar app.jar --spring.profiles.active=outbox
```

3. **透過測試註解**
```java
@SpringBootTest
@ActiveProfiles("outbox")
public class OutboxIntegrationTest {
    // 測試程式碼
}
```

#### Profile 策略建議

- **開發環境**: 使用 `default` profile，使用 In-Memory Repository
- **測試環境**: 使用 `outbox` profile，測試 Outbox 功能
- **生產環境**: 使用 `outbox,production` profile 組合

## Jakarta EE 註解升級

### 從 javax.persistence 到 jakarta.persistence

從 Spring Boot 3.x 開始，JPA 相關註解已從 `javax.persistence` 套件遷移到 `jakarta.persistence` 套件。

#### 註解對照表

| 舊版 (javax.persistence) | 新版 (jakarta.persistence) | 說明 |
|-------------------------|---------------------------|------|
| `@javax.persistence.Entity` | `@jakarta.persistence.Entity` | 標記實體類別 |
| `@javax.persistence.Table` | `@jakarta.persistence.Table` | 指定資料表名稱 |
| `@javax.persistence.Id` | `@jakarta.persistence.Id` | 標記主鍵欄位 |
| `@javax.persistence.Column` | `@jakarta.persistence.Column` | 定義欄位屬性 |
| `@javax.persistence.Version` | `@jakarta.persistence.Version` | 樂觀鎖版本欄位 |
| `@javax.persistence.Transient` | `@jakarta.persistence.Transient` | 標記非持久化欄位 |
| `@javax.persistence.OneToMany` | `@jakarta.persistence.OneToMany` | 一對多關聯 |
| `@javax.persistence.ManyToOne` | `@jakarta.persistence.ManyToOne` | 多對一關聯 |

#### 更新範例

**舊版 (javax.persistence)**:
```java
import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class ProductData implements OutboxData<String> {
    @Id
    private String productId;
    
    @Version
    private long version;
    
    @Transient
    private List<DomainEventData> domainEventDatas;
}
```

**新版 (jakarta.persistence)**:
```java
import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class ProductData implements OutboxData<String> {
    @Id
    private String productId;
    
    @Version
    private long version;
    
    @Transient
    private List<DomainEventData> domainEventDatas;
}
```

#### 相依性配置

**Maven pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <version>3.5.3</version>  <!-- Spring Boot 3.x 自動包含 Jakarta EE -->
</dependency>
```

#### 注意事項

1. **完全替換**: 確保專案中所有 `javax.persistence` 都替換為 `jakarta.persistence`
2. **IDE 支援**: 現代 IDE（IntelliJ IDEA, Eclipse）都支援自動遷移
3. **第三方套件**: 檢查第三方套件是否支援 Jakarta EE
4. **Hibernate 版本**: 需要 Hibernate 6.x 或更高版本

## 測試指南

### 🔴 重要：Outbox Repository 測試要求

**每個 Aggregate 的 OutboxRepository 都必須包含完整的整合測試**。這不是選擇性的，而是強制要求。

#### 必要測試案例
所有 OutboxRepository 實作都**必須**包含以下標準測試案例：

1. **資料持久化測試** - 驗證所有欄位正確儲存到資料庫
2. **資料讀取測試** - 驗證從資料庫讀取的完整性
3. **軟刪除測試** - 驗證使用 `save()` 而非 `delete()` 執行軟刪除
4. **版本控制測試** - 驗證樂觀鎖機制

#### 標準測試範例
**ProductOutboxRepositoryTest** 是所有 OutboxRepository 測試的標準範本：
- 📄 [查看完整範例](./ProductOutboxRepositoryTest.java)

每個新的 OutboxRepository 都應該參考此範例建立對應的測試案例。

### 完整測試配置
詳細的測試配置和範例請參考：[📘 Outbox 測試配置指南](./OUTBOX-TEST-CONFIGURATION.md)

### 快速開始
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test-outbox")
@EzFeature
@EzFeatureReport
public class YourOutboxRepositoryTest {
    // 參考 ProductOutboxRepositoryTest.java 實作標準測試案例
}
```

### 測試檢查清單
實作 OutboxRepository 測試時，確保：
- [ ] 包含所有 4 個必要測試案例
- [ ] 使用 PostgreSQL 測試資料庫（port 5800）
- [ ] 使用 `test-outbox` profile
- [ ] 使用 ezSpec BDD 測試框架
- [ ] 軟刪除測試使用 `save()` 而非 `delete()`
- [ ] 版本號驗證接受 >= 0

## 最佳實踐
- 交易一致性保證
- 事件順序處理
- 錯誤處理策略
- 效能優化建議

## 相關文件
- [ezddd Repository 模式](../repository/README.md)
- [ezddd-gateway 文件](https://github.com/teddy-chen/ezddd-gateway)

---
*此文件為 AI-SCRUM 專案的一部分*