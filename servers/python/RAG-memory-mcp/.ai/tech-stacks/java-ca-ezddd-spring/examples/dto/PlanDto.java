package tw.teddysoft.example.plan.usecase.port;

import java.util.List;
import java.util.ArrayList;

/**
 * 基本 DTO 範本 - Data Transfer Object
 * 
 * 設計重點：
 * 1. 純粹的資料容器，不包含業務邏輯
 * 2. 使用 Fluent Setter Pattern
 * 3. 集合欄位初始化避免 null
 * 4. 私有欄位配合 getter/setter
 * 
 * 使用場景：
 * - Controller 回傳給前端的資料
 * - UseCase 的輸出結果
 * - 跨層資料傳輸
 */
public class PlanDto {
    // 私有欄位 - 基本資料類型
    private String id;
    private String name;
    private String userId;
    
    // 集合欄位 - 永不為 null
    private List<ProjectDto> projects;
    
    /**
     * 預設建構子
     * 重要：初始化所有集合欄位，避免 NullPointerException
     */
    public PlanDto() {
        this.projects = new ArrayList<>();
    }
    
    // === Getter Methods ===
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public List<ProjectDto> getProjects() {
        return projects;
    }
    
    // === Fluent Setter Methods ===
    // 返回 this 支援鏈式調用
    
    public PlanDto setId(String id) {
        this.id = id;
        return this;
    }
    
    public PlanDto setName(String name) {
        this.name = name;
        return this;
    }
    
    public PlanDto setUserId(String userId) {
        this.userId = userId;
        return this;
    }
    
    public PlanDto setProjects(List<ProjectDto> projects) {
        this.projects = projects;
        return this;
    }
    
    // === 便利方法 ===
    
    /**
     * 新增單一 project 到集合中
     * 避免外部需要先取得集合再操作
     */
    public PlanDto addProject(ProjectDto project) {
        this.projects.add(project);
        return this;
    }
    
    /**
     * 清空所有 projects
     */
    public PlanDto clearProjects() {
        this.projects.clear();
        return this;
    }
    
    // === 注意事項 ===
    // 1. 不要加入業務邏輯方法（如 isValid(), canDelete() 等）
    // 2. 不要依賴領域物件（如 Plan entity, PlanId value object）
    // 3. 保持簡單，只做資料傳輸
    // 4. 如需驗證，使用 Bean Validation 註解（如 @NotNull, @Size）
}