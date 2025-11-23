package tw.teddysoft.example.plan.usecase.port.out;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * OneToMany 關聯範本 - 展示父子關係的 JPA 映射
 * 
 * 設計重點：
 * 1. 作為子實體的設計模式
 * 2. ManyToOne 反向關聯的處理
 * 3. 複合主鍵的使用（可選）
 * 4. 級聯操作的傳遞
 * 5. 雙向關聯的維護
 * 
 * 關聯結構：
 * PlanData (1) --> (*) ProjectData (1) --> (*) TaskData
 */
@Entity
@Table(name = "project")
public class ProjectData {

    // === 主鍵 ===
    @Id
    @Column(name = "id")
    private String projectId;

    // === 基本欄位 ===
    @Column(name = "name", nullable = false)
    private String name;

    // === 父層關聯 (ManyToOne) ===
    /**
     * ManyToOne 關聯重點：
     * - JoinColumn 指定外鍵欄位名稱
     * - nullable = false 確保參照完整性
     * - fetch 預設為 EAGER（對 ManyToOne 是合理的）
     * 
     * 注意：這是雙向關聯的「擁有方」
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanData planData;

    // === 子層關聯 (OneToMany) ===
    /**
     * OneToMany 關聯設定：
     * - 級聯所有操作到子實體
     * - EAGER loading（禁止 LAZY）
     * - 自動刪除孤兒記錄
     * - mappedBy 指向子實體的關聯欄位
     */
    @OneToMany(
        cascade = CascadeType.ALL,
        fetch = FetchType.EAGER,  // 重要：永遠使用 EAGER
        orphanRemoval = true,
        mappedBy = "projectData"
    )
    private Set<TaskData> taskDatas;

    // === 時間戳記 ===
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    // === 版本控制（可選） ===
    /**
     * 子實體通常不需要獨立的版本控制
     * 但在某些場景下可能有用
     */
    @Version
    @Column(columnDefinition = "bigint DEFAULT 0", nullable = false)
    private long version;

    // === 建構子 ===

    /**
     * JPA 需要的無參數建構子
     */
    public ProjectData() {
        this.taskDatas = new HashSet<>();
        this.version = 0L;
    }

    /**
     * 便利建構子
     */
    public ProjectData(String projectId, String name) {
        this();
        this.projectId = projectId;
        this.name = name;
    }

    // === Getter/Setter Methods ===

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlanData getPlanData() {
        return planData;
    }

    /**
     * 設定父層關聯
     * 注意：通常由父層的 addProjectData() 方法調用
     */
    public void setPlanData(PlanData planData) {
        this.planData = planData;
    }

    public Set<TaskData> getTaskDatas() {
        return taskDatas;
    }

    public void setTaskDatas(Set<TaskData> taskDatas) {
        this.taskDatas = taskDatas;
    }

    /**
     * 便利方法：新增任務
     * 維護雙向關聯
     */
    public void addTaskData(TaskData taskData) {
        taskData.setProjectData(this);  // 設定反向關聯
        this.taskDatas.add(taskData);
    }

    /**
     * 便利方法：移除任務
     * 維護雙向關聯
     */
    public void removeTaskData(TaskData taskData) {
        taskData.setProjectData(null);  // 清除反向關聯
        this.taskDatas.remove(taskData);
    }

    /**
     * 便利方法：根據 ID 查找任務
     */
    public TaskData findTaskById(String taskId) {
        return taskDatas.stream()
            .filter(task -> task.getTaskId().equals(taskId))
            .findFirst()
            .orElse(null);
    }

    /**
     * 便利方法：清空所有任務
     */
    public void clearTasks() {
        // 先清除所有反向關聯
        taskDatas.forEach(task -> task.setProjectData(null));
        taskDatas.clear();
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
    /**
     * 重要：基於業務主鍵實作
     * 不要使用 Hibernate 生成的 ID
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectData)) return false;
        
        ProjectData that = (ProjectData) o;
        return projectId != null && projectId.equals(that.projectId);
    }

    @Override
    public int hashCode() {
        return projectId != null ? projectId.hashCode() : 0;
    }

    // === 設計考量 ===
    // 1. 雙向關聯需要小心維護一致性
    // 2. 使用便利方法封裝關聯操作
    // 3. equals/hashCode 基於業務主鍵
    // 4. 生命週期回調自動管理時間戳記
    // 5. 所有集合使用 EAGER loading
}