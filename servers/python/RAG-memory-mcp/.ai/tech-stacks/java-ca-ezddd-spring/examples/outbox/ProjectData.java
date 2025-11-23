package tw.teddysoft.example.plan.usecase.port.out;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "project")
public class ProjectData {

    @Id
    @Column(name = "id")
    private String projectId;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanData planData;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "projectData")
    private Set<TaskData> taskDatas;

    public ProjectData() {
        this.taskDatas = new HashSet<>();
    }

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

    public void setPlanData(PlanData planData) {
        this.planData = planData;
    }
    
    public String getPlanId() {
        return planData != null ? planData.getPlanId() : null;
    }

    public void setPlanId(String planId) {
        // This method is kept for backward compatibility
        // The actual relationship is managed through setPlanData
    }

    public Set<TaskData> getTaskDatas() {
        return taskDatas;
    }

    public void setTaskDatas(Set<TaskData> taskDatas) {
        this.taskDatas = taskDatas;
    }

    public void addTaskData(TaskData taskData) {
        taskData.setProjectData(this);
        this.taskDatas.add(taskData);
    }
}