package tw.teddysoft.example.plan.entity;


import tw.teddysoft.ezddd.entity.Entity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Project implements Entity<ProjectId> {
    private final ProjectId id;
    private final ProjectName name;
    private final PlanId planId;
    private final Map<TaskId, Task> tasks;

    public Project(ProjectId id, ProjectName name, PlanId planId) {
        Objects.requireNonNull(id, "Project id cannot be null");
        Objects.requireNonNull(name, "Project name cannot be null");
        Objects.requireNonNull(planId, "Plan id cannot be null");
        
        this.id = id;
        this.name = name;
        this.planId = planId;
        this.tasks = new HashMap<>();
    }

    public ProjectId getId() {
        return id;
    }

    public ProjectName getName() {
        return name;
    }

    public PlanId getPlanId() {
        return planId;
    }
    
    public void createTask(TaskId taskId, String taskName) {
        if (taskName == null || taskName.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be empty");
        }
        
        Task task = new Task(taskId, taskName, this.name);
        tasks.put(taskId, task);
    }
    
    public boolean hasTask(TaskId taskId) {
        return tasks.containsKey(taskId);
    }
    
    public Task getTask(TaskId taskId) {
        return tasks.get(taskId);
    }
    
    public void checkTask(TaskId taskId) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.markAsDone();
        }
    }
    
    public void uncheckTask(TaskId taskId) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.unmarkAsDone();
        }
    }
    
    public void deleteTask(TaskId taskId) {
        tasks.remove(taskId);
    }
    
    public void setTaskDeadline(TaskId taskId, LocalDate deadline) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.setDeadline(deadline);
        }
    }
    
    public void renameTask(TaskId taskId, String newName) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.rename(newName);
        }
    }
    
    void addTask(Task task) {
        tasks.put(task.getId(), task);
    }
    
    public Map<TaskId, Task> getTasks() {
        return new HashMap<>(tasks);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}