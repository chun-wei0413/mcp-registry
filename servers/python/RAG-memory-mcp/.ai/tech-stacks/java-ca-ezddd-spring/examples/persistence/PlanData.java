package tw.teddysoft.example.plan.usecase.port.out;

import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxData;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基本 Entity 範本 - JPA 持久化物件
 * 
 * 設計重點：
 * 1. 實作 OutboxData 介面支援 Event Sourcing
 * 2. 使用 @Version 實現樂觀鎖
 * 3. OneToMany 關聯的級聯操作
 * 4. 時間戳記的自動管理
 * 5. EAGER loading 策略
 * 
 * 重要原則：
 * - 絕對禁止使用 LAZY loading
 * - 所有集合必須初始化
 * - 不包含業務邏輯
 */
@Entity
@Table(name = "plan")
public class PlanData implements OutboxData<String> {

    // === Event Sourcing 支援欄位 ===
    @Transient
    private List<DomainEventData> domainEventDatas;

    @Transient
    private String streamName;

    // === 主鍵 ===
    @Id
    @Column(name = "id")
    private String planId;

    // === 基本欄位 ===
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "next_task_id", nullable = false)
    private int nextTaskId;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    // === 關聯欄位 ===
    /**
     * OneToMany 關聯設定重點：
     * - cascade = CascadeType.ALL: 級聯所有操作
     * - fetch = FetchType.EAGER: 永遠使用 EAGER（禁止 LAZY）
     * - orphanRemoval = true: 自動刪除孤兒記錄
     * - mappedBy: 指定雙向關聯的擁有方
     */
    @OneToMany(
        cascade = CascadeType.ALL, 
        fetch = FetchType.EAGER,  // 重要：永遠使用 EAGER
        orphanRemoval = true, 
        mappedBy = "planData"
    )
    private Set<ProjectData> projectDatas;

    // === 時間戳記 ===
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    // === 版本控制 ===
    /**
     * 樂觀鎖版本號
     * - 使用 bigint 避免溢位
     * - 預設值為 0
     * - JPA 自動管理版本遞增
     */
    @Version
    @Column(columnDefinition = "bigint DEFAULT 0", nullable = false)
    private long version;

    // === 建構子 ===
    
    /**
     * 預設建構子（JPA 需要）
     * 初始化所有集合和預設值
     */
    public PlanData() {
        this(0L);
    }

    /**
     * 帶版本號的建構子
     * 用於測試或特殊情況
     */
    public PlanData(long version) {
        this.projectDatas = new HashSet<>();
        this.version = version;
        this.domainEventDatas = new ArrayList<>();
        this.isDeleted = false;
        this.nextTaskId = 0;
    }

    // === Getter/Setter Methods ===

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

    /**
     * 便利方法：新增子項目
     * 維護雙向關聯的一致性
     */
    public void addProjectData(ProjectData projectData) {
        projectData.setPlanData(this);  // 設定反向關聯
        this.projectDatas.add(projectData);
    }

    /**
     * 便利方法：移除子項目
     * 維護雙向關聯的一致性
     */
    public void removeProjectData(ProjectData projectData) {
        projectData.setPlanData(null);  // 清除反向關聯
        this.projectDatas.remove(projectData);
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

    /**
     * 設定版本號
     * 注意：通常由 JPA 自動管理，手動設定需謹慎
     */
    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    // === OutboxData 介面實作 ===
    
    /**
     * 使用 @Transient 標記，避免持久化到資料庫
     * 這些方法支援 Event Sourcing 機制
     */
    
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

    // === 生命週期回調 ===
    
    /**
     * 在持久化前自動設定時間戳記
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        lastUpdated = Instant.now();
    }

    /**
     * 在更新前自動更新時間戳記
     */
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }

    // === 設計考量 ===
    // 1. 使用 Set 而非 List 避免重複
    // 2. 所有關聯都使用 EAGER loading
    // 3. 提供便利方法維護雙向關聯
    // 4. 實作 OutboxData 支援 Event Sourcing
    // 5. 使用生命週期回調自動管理時間戳記
}