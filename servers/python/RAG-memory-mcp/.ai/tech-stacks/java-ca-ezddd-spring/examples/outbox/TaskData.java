package tw.teddysoft.example.plan.usecase.port.out;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "task")
public class TaskData {

    @Id
    @Column(name = "id")
    private String taskId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectData projectData;

    @Column(name = "is_done", nullable = false)
    private boolean done;

    @Column(name = "deadline")
    private LocalDate deadline;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_tag", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tag_id")
    private Set<String> tagIds = new HashSet<>();

    public TaskData() {
        this.done = false;
    }

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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectId() {
        return projectData != null ? projectData.getProjectId() : null;
    }

    public void setProjectId(String projectId) {
        // This method is kept for backward compatibility
        // The actual relationship is managed through setProjectData
    }

    public ProjectData getProjectData() {
        return projectData;
    }

    public void setProjectData(ProjectData projectData) {
        this.projectData = projectData;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Set<String> getTagIds() {
        return tagIds;
    }

    public void setTagIds(Set<String> tagIds) {
        this.tagIds = tagIds;
    }
}