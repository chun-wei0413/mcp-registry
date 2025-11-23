package tw.teddysoft.example.plan.usecase.port;

import java.util.List;
import java.util.ArrayList;

/**
 * 巢狀 DTO 範本 - 展示 DTO 之間的組合關係
 * 
 * 設計重點：
 * 1. 作為父 DTO (PlanDto) 的子元素
 * 2. 包含自己的子元素集合 (TaskDto)
 * 3. 處理父子關係的資料傳輸
 * 4. 維持資料的層次結構
 * 
 * 使用場景：
 * - 表達樹狀資料結構
 * - RESTful API 的巢狀資源
 * - 複雜查詢結果的組織
 */
public class ProjectDto {
    // 基本識別資訊
    private String id;
    private String name;
    
    // 父層關聯 ID（不直接包含父物件）
    private String planId;
    
    // 子元素集合
    private List<TaskDto> tasks;
    
    // 統計資訊（衍生資料）
    private int completedTaskCount;
    private int totalTaskCount;
    
    /**
     * 預設建構子
     * 初始化所有集合，設定預設值
     */
    public ProjectDto() {
        this.tasks = new ArrayList<>();
        this.completedTaskCount = 0;
        this.totalTaskCount = 0;
    }
    
    /**
     * 便利建構子 - 快速建立實例
     */
    public ProjectDto(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }
    
    // === Getter Methods ===
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getPlanId() {
        return planId;
    }
    
    public List<TaskDto> getTasks() {
        return tasks;
    }
    
    public int getCompletedTaskCount() {
        return completedTaskCount;
    }
    
    public int getTotalTaskCount() {
        return totalTaskCount;
    }
    
    // === Fluent Setter Methods ===
    
    public ProjectDto setId(String id) {
        this.id = id;
        return this;
    }
    
    public ProjectDto setName(String name) {
        this.name = name;
        return this;
    }
    
    public ProjectDto setPlanId(String planId) {
        this.planId = planId;
        return this;
    }
    
    public ProjectDto setTasks(List<TaskDto> tasks) {
        this.tasks = tasks;
        updateTaskCounts();
        return this;
    }
    
    public ProjectDto setCompletedTaskCount(int completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
        return this;
    }
    
    public ProjectDto setTotalTaskCount(int totalTaskCount) {
        this.totalTaskCount = totalTaskCount;
        return this;
    }
    
    // === 便利方法 ===
    
    /**
     * 新增任務並自動更新計數
     */
    public ProjectDto addTask(TaskDto task) {
        this.tasks.add(task);
        this.totalTaskCount++;
        if (task.isDone()) {
            this.completedTaskCount++;
        }
        return this;
    }
    
    /**
     * 移除任務並自動更新計數
     */
    public ProjectDto removeTask(String taskId) {
        tasks.removeIf(task -> {
            if (task.getId().equals(taskId)) {
                this.totalTaskCount--;
                if (task.isDone()) {
                    this.completedTaskCount--;
                }
                return true;
            }
            return false;
        });
        return this;
    }
    
    /**
     * 根據現有任務更新計數
     * 用於批次設定任務後
     */
    private void updateTaskCounts() {
        this.totalTaskCount = tasks.size();
        this.completedTaskCount = (int) tasks.stream()
            .filter(TaskDto::isDone)
            .count();
    }
    
    // === 設計考量 ===
    // 1. planId 只存 ID，不存整個 PlanDto（避免循環引用）
    // 2. 統計資訊可以即時計算或預先儲存
    // 3. 提供便利方法簡化操作，但不加入業務邏輯
    // 4. 保持 DTO 的純粹性，只做資料傳輸
}