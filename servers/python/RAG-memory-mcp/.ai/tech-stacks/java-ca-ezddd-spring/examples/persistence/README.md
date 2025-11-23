# 持久化物件範本 (Persistence Object Templates)

本目錄包含 JPA 實體（Entity）設計範本，展示如何將領域模型映射到資料庫。

## 📁 目錄內容

- **PlanData.java** - 基本 Entity 範本
  - 展示基本的 JPA 註解使用
  - Event Sourcing 支援
  - 版本控制機制

- **ProjectData.java** - OneToMany 關聯範本
  - 展示父子關係的映射
  - 級聯操作設定
  - 雙向關聯管理

- **TaskData.java** - ManyToMany 關聯範本
  - 展示多對多關係的處理
  - @ElementCollection 的使用
  - 複雜關聯的最佳實踐

## 🎯 持久化設計原則

### 1. 職責分離
持久化物件只負責資料儲存，不包含業務邏輯：
```java
@Entity
public class PlanData {
    // ✅ 純粹的資料欄位和 JPA 註解
    @Id
    private String planId;
    
    // ❌ 不應包含業務邏輯
    public boolean canBeDeleted() {
        return projectDatas.isEmpty();
    }
}
```

### 2. 命名規範
- Entity 類別名稱以 `Data` 結尾
- 對應領域模型去掉 `Data` 後綴
- 表名使用底線分隔（snake_case）

### 3. 關聯映射策略

#### OneToMany 關聯
```java
@OneToMany(cascade = CascadeType.ALL, 
           fetch = FetchType.EAGER, 
           orphanRemoval = true,
           mappedBy = "planData")
private Set<ProjectData> projectDatas;
```

#### @ElementCollection（推薦用於簡單集合）
```java
@ElementCollection
@CollectionTable(name = "task_tag", 
    joinColumns = @JoinColumn(name = "task_id"))
@Column(name = "tag_id")
private Set<String> tagIds = new HashSet<>();
```

## 📊 JPA 註解使用指南

### 常用註解

| 註解 | 用途 | 範例 |
|-----|------|------|
| @Entity | 標記為 JPA 實體 | `@Entity` |
| @Table | 指定表名 | `@Table(name = "plan")` |
| @Id | 主鍵標記 | `@Id` |
| @Column | 欄位映射 | `@Column(name = "user_id", nullable = false)` |
| @Version | 樂觀鎖版本 | `@Version` |
| @Transient | 非持久化欄位 | `@Transient` |

### 關聯註解

| 註解 | 關係類型 | 建議使用場景 |
|-----|----------|------------|
| @OneToMany | 一對多 | 父子關係（如 Plan-Project） |
| @ManyToOne | 多對一 | 子指向父（如 Project-Plan） |
| @ElementCollection | 值類型集合 | 簡單集合（如 tag IDs） |
| @ManyToMany | 多對多 | 避免使用，改用中間實體 |

## 🏗️ Entity 結構模式

### 1. 基本結構
```java
@Entity
@Table(name = "entity_name")
public class EntityData {
    @Id
    private String id;
    
    @Version
    private long version;
    
    // 建構子
    public EntityData() {
        this(0L);
    }
    
    public EntityData(long version) {
        this.version = version;
    }
}
```

### 2. Event Sourcing 支援
```java
@Entity
public class AggregateData implements OutboxData<String> {
    @Transient
    private List<DomainEventData> domainEventDatas;
    
    @Transient
    private String streamName;
    
    // OutboxData 介面實作
}
```

### 3. 時間戳記
```java
@Column(name = "created_at", nullable = false)
private Instant createdAt;

@Column(name = "last_updated", nullable = false)
private Instant lastUpdated;
```

## ⚠️ 重要原則

### 1. 禁止使用 Lazy Loading
**永遠使用 EAGER fetching**：
```java
// ✅ 正確
@OneToMany(fetch = FetchType.EAGER)
private Set<ProjectData> projectDatas;

// ❌ 錯誤
@OneToMany(fetch = FetchType.LAZY)
private Set<ProjectData> projectDatas;
```

**原因**：
- 避免 LazyInitializationException
- 符合 DDD Aggregate 完整載入原則
- 簡化測試和除錯

### 2. 集合初始化
```java
// 在建構子中初始化所有集合
public PlanData() {
    this.projectDatas = new HashSet<>();
    this.domainEventDatas = new ArrayList<>();
}
```

### 3. 雙向關聯管理
```java
// 提供便利方法維護雙向關係
public void addProjectData(ProjectData projectData) {
    projectData.setPlanData(this);  // 設定反向關聯
    this.projectDatas.add(projectData);
}
```

## 💡 最佳實踐

### 1. 使用 @ElementCollection 替代 @ManyToMany
當只需要儲存 ID 集合時：
```java
// ✅ 推薦：簡單高效
@ElementCollection
@CollectionTable(name = "task_tag")
@Column(name = "tag_id")
private Set<String> tagIds;

// ❌ 避免：過度複雜
@ManyToMany
@JoinTable(name = "task_tag")
private Set<TagData> tags;
```

### 2. 版本控制
```java
@Version
@Column(columnDefinition = "bigint DEFAULT 0", nullable = false)
private long version;
```

### 3. 級聯操作設定
```java
@OneToMany(
    cascade = CascadeType.ALL,      // 級聯所有操作
    orphanRemoval = true,           // 自動刪除孤兒
    mappedBy = "parent"            // 雙向關聯
)
```

## 🚀 使用建議

### 1. Mapper 整合
- 使用專門的 Mapper 類別轉換 Domain ↔ Data
- 保持轉換邏輯的集中管理
- 處理 null 值和預設值

### 2. Repository 實作
- Repository 介面定義在 domain 層
- JPA 實作在 infrastructure 層
- 使用 Spring Data JPA 簡化實作

### 3. 測試策略
- 使用 @DataJpaTest 進行整合測試
- 測試級聯操作和關聯管理
- 驗證版本控制機制

## 📚 相關資源
- [Mapper 範本](../mapper/README.md)
- [Repository 範本](../repository/README.md)
- [Domain Model 範本](../aggregate/README.md)
- [Spring Data JPA 文檔](https://spring.io/projects/spring-data-jpa)