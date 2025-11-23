package tw.teddysoft.example.plan.usecase.port;

import java.time.LocalDate;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * 複雜 DTO 範本 - 包含各種資料類型的完整範例
 * 
 * 設計重點：
 * 1. 展示各種資料類型的處理
 * 2. Optional 欄位的設計模式
 * 3. 集合和 Map 的初始化
 * 4. 列舉類型的處理
 * 5. 日期時間的表達
 * 
 * 使用場景：
 * - 複雜的業務實體傳輸
 * - 包含多種資料類型的 API 回應
 * - 需要豐富元資料的查詢結果
 */
public class TaskDto {
    // === 基本欄位 ===
    private String id;
    private String name;
    private String description;  // 可為 null
    
    // === 關聯 ID ===
    private String projectId;
    private String planId;
    
    // === 狀態欄位 ===
    private boolean done;
    private TaskStatus status;  // 列舉類型
    private TaskPriority priority;
    
    // === 時間欄位 ===
    private LocalDate deadline;  // 可為 null
    private LocalDate createdDate;
    private LocalDate completedDate;  // 可為 null
    
    // === 集合欄位 ===
    private Set<String> tagIds;  // 標籤 ID 集合
    private Set<String> assigneeIds;  // 指派人員 ID 集合
    
    // === Map 欄位 ===
    private Map<String, String> metadata;  // 額外的鍵值對資料
    
    // === 統計欄位 ===
    private int commentCount;
    private int attachmentCount;
    
    /**
     * 任務狀態列舉
     */
    public enum TaskStatus {
        TODO("待辦"),
        IN_PROGRESS("進行中"),
        DONE("完成"),
        CANCELLED("取消");
        
        private final String displayName;
        
        TaskStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 任務優先級列舉
     */
    public enum TaskPriority {
        LOW(1, "低"),
        MEDIUM(2, "中"),
        HIGH(3, "高"),
        URGENT(4, "緊急");
        
        private final int level;
        private final String displayName;
        
        TaskPriority(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }
        
        public int getLevel() {
            return level;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 預設建構子
     * 初始化所有集合和必要的預設值
     */
    public TaskDto() {
        this.done = false;
        this.status = TaskStatus.TODO;
        this.priority = TaskPriority.MEDIUM;
        this.tagIds = new HashSet<>();
        this.assigneeIds = new HashSet<>();
        this.metadata = new HashMap<>();
        this.commentCount = 0;
        this.attachmentCount = 0;
        this.createdDate = LocalDate.now();
    }
    
    // === Getter Methods ===
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public String getPlanId() {
        return planId;
    }
    
    public boolean isDone() {
        return done;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public TaskPriority getPriority() {
        return priority;
    }
    
    public LocalDate getDeadline() {
        return deadline;
    }
    
    public LocalDate getCreatedDate() {
        return createdDate;
    }
    
    public LocalDate getCompletedDate() {
        return completedDate;
    }
    
    public Set<String> getTagIds() {
        return tagIds;
    }
    
    public Set<String> getAssigneeIds() {
        return assigneeIds;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public int getCommentCount() {
        return commentCount;
    }
    
    public int getAttachmentCount() {
        return attachmentCount;
    }
    
    // === Fluent Setter Methods ===
    
    public TaskDto setId(String id) {
        this.id = id;
        return this;
    }
    
    public TaskDto setName(String name) {
        this.name = name;
        return this;
    }
    
    public TaskDto setDescription(String description) {
        this.description = description;
        return this;
    }
    
    public TaskDto setProjectId(String projectId) {
        this.projectId = projectId;
        return this;
    }
    
    public TaskDto setPlanId(String planId) {
        this.planId = planId;
        return this;
    }
    
    public TaskDto setDone(boolean done) {
        this.done = done;
        if (done && this.completedDate == null) {
            this.completedDate = LocalDate.now();
        }
        return this;
    }
    
    public TaskDto setStatus(TaskStatus status) {
        this.status = status;
        return this;
    }
    
    public TaskDto setPriority(TaskPriority priority) {
        this.priority = priority;
        return this;
    }
    
    public TaskDto setDeadline(LocalDate deadline) {
        this.deadline = deadline;
        return this;
    }
    
    public TaskDto setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
        return this;
    }
    
    public TaskDto setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
        return this;
    }
    
    public TaskDto setTagIds(Set<String> tagIds) {
        this.tagIds = tagIds;
        return this;
    }
    
    public TaskDto setAssigneeIds(Set<String> assigneeIds) {
        this.assigneeIds = assigneeIds;
        return this;
    }
    
    public TaskDto setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }
    
    public TaskDto setCommentCount(int commentCount) {
        this.commentCount = commentCount;
        return this;
    }
    
    public TaskDto setAttachmentCount(int attachmentCount) {
        this.attachmentCount = attachmentCount;
        return this;
    }
    
    // === 便利方法 ===
    
    /**
     * 新增單一標籤
     */
    public TaskDto addTag(String tagId) {
        this.tagIds.add(tagId);
        return this;
    }
    
    /**
     * 移除單一標籤
     */
    public TaskDto removeTag(String tagId) {
        this.tagIds.remove(tagId);
        return this;
    }
    
    /**
     * 新增指派人員
     */
    public TaskDto addAssignee(String assigneeId) {
        this.assigneeIds.add(assigneeId);
        return this;
    }
    
    /**
     * 新增或更新 metadata
     */
    public TaskDto putMetadata(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * 檢查是否過期
     * 注意：這是純粹的資料檢查，不是業務邏輯
     */
    public boolean isOverdue() {
        return deadline != null && 
               LocalDate.now().isAfter(deadline) && 
               !done;
    }
    
    // === Optional 欄位處理 ===
    
    /**
     * 檢查是否有描述
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
    
    /**
     * 檢查是否設定了截止日期
     */
    public boolean hasDeadline() {
        return deadline != null;
    }
    
    /**
     * 檢查是否有標籤
     */
    public boolean hasTags() {
        return !tagIds.isEmpty();
    }
    
    // === 設計考量 ===
    // 1. Optional 欄位允許 null，但集合永不為 null
    // 2. 列舉提供顯示名稱，方便前端使用
    // 3. 便利方法簡化操作，但避免複雜業務邏輯
    // 4. 時間欄位使用 LocalDate 而非 String，保持類型安全
    // 5. 提供 has* 方法檢查 Optional 欄位
}