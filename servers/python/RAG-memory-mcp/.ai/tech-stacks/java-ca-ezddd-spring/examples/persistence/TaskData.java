package tw.teddysoft.example.plan.usecase.port.out;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

/**
 * ManyToMany 關聯範本 - 展示複雜關聯的最佳實踐
 * 
 * 設計重點：
 * 1. 使用 @ElementCollection 替代 @ManyToMany
 * 2. 多種資料類型的映射
 * 3. 列舉類型的持久化
 * 4. Map 集合的映射
 * 5. 日期時間的處理
 * 
 * 重要洞察：
 * - 當只需要儲存 ID 時，@ElementCollection 比 @ManyToMany 更簡單高效
 * - 避免真正的多對多關聯，考慮使用中間實體
 */
@Entity
@Table(name = "task")
public class TaskData {

    // === 主鍵 ===
    @Id
    @Column(name = "id")
    private String taskId;

    // === 基本欄位 ===
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_done", nullable = false)
    private boolean done;

    // === 列舉類型 ===
    /**
     * 列舉持久化策略：
     * - EnumType.STRING: 儲存列舉名稱（推薦）
     * - EnumType.ORDINAL: 儲存序號（不推薦）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private TaskPriority priority;

    // === 日期時間欄位 ===
    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    // === 父層關聯 ===
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectData projectData;

    // === 集合映射方案一：@ElementCollection（推薦） ===
    /**
     * 使用 @ElementCollection 儲存簡單值集合
     * 優點：
     * - 不需要創建額外的實體類
     * - JPA 自動管理集合表的 CRUD
     * - 避免複雜的多對多關聯
     * 
     * 適用場景：只需要儲存 ID 或簡單值
     */
    @ElementCollection(fetch = FetchType.EAGER)  // 重要：使用 EAGER
    @CollectionTable(
        name = "task_tag",
        joinColumns = @JoinColumn(name = "task_id")
    )
    @Column(name = "tag_id")
    private Set<String> tagIds = new HashSet<>();

    // === 集合映射方案二：@ElementCollection with Map ===
    /**
     * 使用 @ElementCollection 儲存鍵值對
     * 適用於儲存元資料或設定
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "task_metadata",
        joinColumns = @JoinColumn(name = "task_id")
    )
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> metadata = new HashMap<>();

    // === 集合映射方案三：避免使用的 @ManyToMany ===
    /**
     * 不推薦的做法（僅作示範）：
     * @ManyToMany(fetch = FetchType.EAGER)
     * @JoinTable(
     *     name = "task_tag",
     *     joinColumns = @JoinColumn(name = "task_id"),
     *     inverseJoinColumns = @JoinColumn(name = "tag_id")
     * )
     * private Set<TagData> tags;
     * 
     * 問題：
     * - 需要載入完整的 Tag 實體
     * - 增加複雜性和性能開銷
     * - 難以管理雙向關聯
     */

    // === 時間戳記 ===
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    // === 版本控制 ===
    @Version
    @Column(columnDefinition = "bigint DEFAULT 0", nullable = false)
    private long version;

    // === 列舉定義 ===
    
    public enum TaskStatus {
        TODO, IN_PROGRESS, DONE, CANCELLED
    }

    public enum TaskPriority {
        LOW, MEDIUM, HIGH, URGENT
    }

    // === 建構子 ===

    public TaskData() {
        this.done = false;
        this.status = TaskStatus.TODO;
        this.priority = TaskPriority.MEDIUM;
        this.tagIds = new HashSet<>();
        this.metadata = new HashMap<>();
        this.version = 0L;
    }

    public TaskData(String taskId, String name) {
        this();
        this.taskId = taskId;
        this.name = name;
    }

    // === Getter/Setter Methods ===

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
        if (done && this.completedDate == null) {
            this.completedDate = LocalDate.now();
        }
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }

    public ProjectData getProjectData() {
        return projectData;
    }

    public void setProjectData(ProjectData projectData) {
        this.projectData = projectData;
    }

    public Set<String> getTagIds() {
        return tagIds;
    }

    /**
     * 重要：直接設定集合時要小心
     * JPA 可能需要追蹤變更
     */
    public void setTagIds(Set<String> tagIds) {
        this.tagIds.clear();
        this.tagIds.addAll(tagIds);
    }

    /**
     * 便利方法：新增標籤
     */
    public void addTag(String tagId) {
        this.tagIds.add(tagId);
    }

    /**
     * 便利方法：移除標籤
     */
    public void removeTag(String tagId) {
        this.tagIds.remove(tagId);
    }

    /**
     * 便利方法：檢查是否有特定標籤
     */
    public boolean hasTag(String tagId) {
        return this.tagIds.contains(tagId);
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata.clear();
        this.metadata.putAll(metadata);
    }

    /**
     * 便利方法：新增或更新元資料
     */
    public void putMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    /**
     * 便利方法：取得元資料
     */
    public String getMetadata(String key) {
        return this.metadata.get(key);
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

    public void setVersion(long version) {
        this.version = version;
    }

    // === 生命週期回調 ===

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        lastUpdated = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }

    // === equals 和 hashCode ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskData)) return false;
        
        TaskData that = (TaskData) o;
        return taskId != null && taskId.equals(that.taskId);
    }

    @Override
    public int hashCode() {
        return taskId != null ? taskId.hashCode() : 0;
    }

    // === 設計考量 ===
    // 1. 使用 @ElementCollection 而非 @ManyToMany 簡化關聯
    // 2. 列舉使用 STRING 類型持久化，避免序號問題
    // 3. 集合操作提供便利方法，隱藏實作細節
    // 4. 時間欄位使用適當的 Java 8 時間類型
    // 5. 所有集合都使用 EAGER loading
}