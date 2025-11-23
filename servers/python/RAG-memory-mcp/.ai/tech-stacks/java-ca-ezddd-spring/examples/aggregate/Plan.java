package tw.teddysoft.example.plan.entity;

import tw.teddysoft.example.common.DateProvider;
import tw.teddysoft.example.tag.entity.TagId;
import tw.teddysoft.ezddd.entity.EsAggregateRoot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static tw.teddysoft.ucontract.Contract.*;

public class Plan extends EsAggregateRoot<PlanId, PlanEvents> {
    public final static String CATEGORY = "Plan";
    private PlanId planId;
    private String name;
    private String userId;
    private Map<ProjectId, Project> projects;
    private int nextTaskId;
    private boolean isDeleted;

    // Constructor for event sourcing framework to rebuild aggregate from events
    public Plan(List<PlanEvents> domainEvents) {
        super(domainEvents);
    }

    // Public constructor for creating new instances
    public Plan(PlanId planId, String name, String userId) {
        super();

        requireNotNull("Plan id", planId);
        requireNotNull("Plan name", name);
        requireNotNull("User id", userId);

        apply(new PlanEvents.PlanCreated(
                planId,
                name,
                userId,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure(format("Plan id is '%s'", planId), () -> getId().equals(planId));
        ensure(format("Plan name is '%s'", name), () -> getName().equals(name));
        ensure(format("User id is '%s'", userId), () -> getUserId().equals(userId));
        ensure("A PlanCreated event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.PlanCreated created &&
            created.planId().equals(planId) &&
            created.name().equals(name) &&
            created.userId().equals(userId)
        );
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public void rename(String newName) {
        requireNotNull("New name", newName);
        require("New name not empty", () -> !newName.trim().isEmpty());
        require("New name is different", () -> !name.equals(newName));

        String oldName = this.name;
        
        apply(new PlanEvents.PlanRenamed(
                planId,
                newName,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure(format("Plan name is changed to '%s'", newName), () -> getName().equals(newName));
        ensure("A PlanRenamed event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.PlanRenamed renamed &&
            renamed.planId().equals(planId) &&
            renamed.newName().equals(newName)
        );
    }

    public void createProject(ProjectId projectId, ProjectName projectName) {
        requireNotNull("Project id", projectId);
        requireNotNull("Project name", projectName);
        require("Project id must be unique", () -> !hasProject(projectId));

        apply(new PlanEvents.ProjectCreated(
                planId,
                projectId,
                projectName,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure(format("Project with id '%s' exists", projectId), () -> hasProject(projectId));
        ensure(format("Project name is '%s'", projectName), () -> getProject(projectId).getName().equals(projectName));
        ensure("A ProjectCreated event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.ProjectCreated created &&
            created.planId().equals(planId) &&
            created.projectId().equals(projectId) &&
            created.projectName().equals(projectName)
        );
    }

    public boolean hasProject(ProjectId projectId) {
        return projects.containsKey(projectId);
    }

    public Project getProject(ProjectId projectId) {
        return projects.get(projectId);
    }
    
    public boolean hasProject(ProjectName projectName) {
        return projects.values().stream()
                .anyMatch(project -> project.getName().equals(projectName));
    }
    
    public Project getProject(ProjectName projectName) {
        return projects.values().stream()
                .filter(project -> project.getName().equals(projectName))
                .findFirst()
                .orElse(null);
    }
    
    public Map<ProjectId, Project> getProjects() {
        return new HashMap<>(projects);
    }

    public TaskId createTask(ProjectName projectName, TaskId taskId, String taskName) {
        requireNotNull("Project name", projectName);
        requireNotNull("Task name", taskName);
        require("Task name not empty", () -> !taskName.trim().isEmpty());
        
        // Find project by name
        Project project = projects.values().stream()
                .filter(p -> p.getName().equals(projectName))
                .findFirst()
                .orElse(null);
        require("Project must exist", () -> project != null);
        
        // Create task in project
        project.createTask(taskId, taskName);
        
        apply(new PlanEvents.TaskCreated(
                planId,
                projectName,
                taskId,
                taskName,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure(format("Task with id '%s' exists in project '%s'", taskId, projectName), () -> project.hasTask(taskId));
        ensure(format("Task name is '%s'", taskName), () -> project.getTask(taskId).getName().equals(taskName));
        ensure("A TaskCreated event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.TaskCreated created &&
            created.planId().equals(planId) &&
            created.projectName().equals(projectName) &&
            created.taskId().equals(taskId) &&
            created.taskName().equals(taskName)
        );

        return taskId;
    }

    public boolean hasTask(TaskId taskId) {
        return projects.values().stream()
                .anyMatch(project -> project.hasTask(taskId));
    }

    public Task getTask(TaskId taskId) {
        return projects.values().stream()
                .filter(project -> project.hasTask(taskId))
                .map(project -> project.getTask(taskId))
                .findFirst()
                .orElse(null);
    }
    
    public void checkTask(ProjectName projectName, TaskId taskId) {
        // Find project by name
        Project project = projects.values().stream()
                .filter(p -> p.getName().equals(projectName))
                .findFirst()
                .orElse(null);
        require("Project must exist", () -> project != null);
        require("Task must exist in project", () -> project.hasTask(taskId));
        
        Task task = project.getTask(taskId);
        require("Task must not already be done", () -> !task.isDone());
        
        // Delegate task checking to project
        project.checkTask(taskId);
        
        apply(new PlanEvents.TaskChecked(
                planId,
                projectName,
                taskId,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure(format("Task with id '%s' is done", taskId), () -> project.getTask(taskId).isDone());
        ensure("A TaskChecked event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.TaskChecked checked &&
            checked.planId().equals(planId) &&
            checked.projectName().equals(projectName) &&
            checked.taskId().equals(taskId)
        );
    }
    
    public void uncheckTask(ProjectName projectName, TaskId taskId) {
        requireNotNull("Project name", projectName);
        requireNotNull("Task id", taskId);
        
        // Find project by name
        Project project = projects.values().stream()
                .filter(p -> p.getName().equals(projectName))
                .findFirst()
                .orElse(null);
        require("Project must exist", () -> project != null);
        require("Task must exist in project", () -> project.hasTask(taskId));
        
        Task task = project.getTask(taskId);
        require("Task must be done", () -> task.isDone());
        
        // Delegate task unchecking to project
        project.uncheckTask(taskId);
        
        apply(new PlanEvents.TaskUnchecked(
                planId,
                projectName,
                taskId,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));
        
        ensure(format("Task with id '%s' is not done", taskId), () -> !project.getTask(taskId).isDone());
        ensure("A TaskUnchecked event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.TaskUnchecked unchecked &&
            unchecked.planId().equals(planId) &&
            unchecked.projectName().equals(projectName) &&
            unchecked.taskId().equals(taskId)
        );
    }

    public void deleteProject(ProjectId projectId) {
        requireNotNull("Project id", projectId);
        require("Plan must not be deleted", () -> !isDeleted);
        require("Project must exist", () -> hasProject(projectId));
        
        // Check if project has any tasks
        Project project = getProject(projectId);
        require("Project must not have any tasks", () -> project.getTasks().isEmpty());
        
        apply(new PlanEvents.ProjectDeleted(
                planId,
                projectId,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));
        
        ensure(format("Project with id '%s' is deleted", projectId), () -> !hasProject(projectId));
        ensure("A ProjectDeleted event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.ProjectDeleted deleted &&
            deleted.planId().equals(planId) &&
            deleted.projectId().equals(projectId)
        );
    }

    public void deleteTask(ProjectName projectName, TaskId taskId) {
        requireNotNull("Project name", projectName);
        requireNotNull("Task id", taskId);
        require("Plan must not be deleted", () -> !isDeleted);
        require("Project must exist", () -> hasProject(projectName));
        
        Project project = getProject(projectName);
        require("Task must exist in project", () -> project.hasTask(taskId));
        
        // Delete the task
        project.deleteTask(taskId);
        
        apply(new PlanEvents.TaskDeleted(
                planId,
                project.getId(),
                taskId,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));
        
        ensure(format("Task with id '%s' is deleted from project '%s'", taskId, projectName), () -> !project.hasTask(taskId));
        ensure("A TaskDeleted event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.TaskDeleted deleted &&
            deleted.planId().equals(planId) &&
            deleted.projectId().equals(project.getId()) &&
            deleted.taskId().equals(taskId)
        );
    }

    public void setTaskDeadline(ProjectName projectName, TaskId taskId, LocalDate deadline) {
        requireNotNull("Project name", projectName);
        requireNotNull("Task id", taskId);
        // deadline can be null (to remove deadline)
        require("Plan must not be deleted", () -> !isDeleted);
        require("Project must exist", () -> hasProject(projectName));
        
        Project project = getProject(projectName);
        require("Task must exist in project", () -> project.hasTask(taskId));
        
        // Set the task deadline
        project.setTaskDeadline(taskId, deadline);
        
        apply(new PlanEvents.TaskDeadlineSet(
                planId,
                project.getId(),
                taskId,
                deadline != null ? deadline.toString() : null,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));
        
        ensure(format("Task deadline is set for task '%s' in project '%s'", taskId, projectName), 
               () -> Objects.equals(project.getTask(taskId).getDeadline(), deadline));
        ensure("A TaskDeadlineSet event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.TaskDeadlineSet deadlineSet &&
            deadlineSet.planId().equals(planId) &&
            deadlineSet.projectId().equals(project.getId()) &&
            deadlineSet.taskId().equals(taskId) &&
            Objects.equals(deadlineSet.deadline(), deadline != null ? deadline.toString() : null)
        );
    }

    public void renameTask(ProjectName projectName, TaskId taskId, String newName) {
        requireNotNull("Project name", projectName);
        requireNotNull("Task id", taskId);
        requireNotNull("New task name", newName);
        require("Plan must not be deleted", () -> !isDeleted);
        require("Project must exist", () -> hasProject(projectName));
        require("New task name must not be empty", () -> !newName.trim().isEmpty());
        
        Project project = getProject(projectName);
        require("Task must exist in project", () -> project.hasTask(taskId));
        
        // Rename the task
        project.renameTask(taskId, newName);
        
        apply(new PlanEvents.TaskRenamed(
                planId,
                project.getId(),
                taskId,
                newName,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));
        
        ensure(format("Task '%s' is renamed to '%s' in project '%s'", taskId, newName, projectName), 
               () -> project.getTask(taskId).getName().equals(newName));
        ensure("A TaskRenamed event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.TaskRenamed renamed &&
            renamed.planId().equals(planId) &&
            renamed.projectId().equals(project.getId()) &&
            renamed.taskId().equals(taskId) &&
            renamed.newName().equals(newName)
        );
    }

    public void delete() {
        require("Plan must not already be deleted", () -> !isDeleted);
        
        apply(new PlanEvents.PlanDeleted(
                planId,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));
        
        ensure("Plan is marked as deleted", () -> isDeleted());
        ensure("A PlanDeleted event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.PlanDeleted deleted &&
            deleted.planId().equals(planId)
        );
    }

    @Override
    public boolean isDeleted() {
        return isDeleted;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public PlanId getId() {
        return planId;
    }

    public void assignTag(ProjectId projectId, TaskId taskId, TagId tagId) {
        requireNotNull("Project id", projectId);
        requireNotNull("Task id", taskId);
        requireNotNull("Tag id", tagId);
        require("Plan is not deleted", () -> !isDeleted());
        require("Project exists", () -> hasProject(projectId));
        
        Project project = getProject(projectId);
        require("Task exists in project", () -> project.hasTask(taskId));
        
        Task task = project.getTask(taskId);
        require("Tag not already assigned", () -> !task.hasTag(tagId));
        
        apply(new PlanEvents.TagAssigned(
                planId,
                project.getId(),
                taskId,
                tagId,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure("Tag is assigned to task", () -> task.hasTag(tagId));
        ensure("A TagAssigned event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.TagAssigned assigned &&
            assigned.planId().equals(planId) &&
            assigned.projectId().equals(project.getId()) &&
            assigned.taskId().equals(taskId) &&
            assigned.tagId().equals(tagId)
        );
    }
    
    public void unassignTag(ProjectId projectId, TaskId taskId, TagId tagId) {
        requireNotNull("Project id", projectId);
        requireNotNull("Task id", taskId);
        requireNotNull("Tag id", tagId);
        require("Plan is not deleted", () -> !isDeleted());
        require("Project exists", () -> hasProject(projectId));
        
        Project project = getProject(projectId);
        require("Task exists in project", () -> project.hasTask(taskId));
        
        Task task = project.getTask(taskId);
        require("Tag is assigned", () -> task.hasTag(tagId));
        
        apply(new PlanEvents.TagUnassigned(
                planId,
                project.getId(),
                taskId,
                tagId,
                new HashMap<>(),  // metadata
                UUID.randomUUID(),
                DateProvider.now()
        ));
        
        ensure("Tag is unassigned from task", () -> !task.hasTag(tagId));
        ensure("A TagUnassigned event is generated correctly", () -> 
            getLastDomainEvent() instanceof PlanEvents.TagUnassigned unassigned &&
            unassigned.planId().equals(planId) &&
            unassigned.projectId().equals(project.getId()) &&
            unassigned.taskId().equals(taskId) &&
            unassigned.tagId().equals(tagId)
        );
    }

    @Override
    public void ensureInvariant() {
        invariant(format("Category is '%s'.", getCategory()), () -> getCategory().equals(CATEGORY));
        invariantNotNull("Plan Id", planId);
        if (!isDeleted) {
            invariantNotNull("Plan name", name);
            invariantNotNull("User Id", userId);
        }
    }

    @Override
    protected void when(PlanEvents event) {
        switch (event) {
            case PlanEvents.PlanCreated e -> {
                this.planId = e.planId();
                this.name = e.name();
                this.userId = e.userId();
                this.projects = new HashMap<>();
                this.nextTaskId = 0;
                this.isDeleted = false;
            }
            case PlanEvents.PlanRenamed e -> {
                this.name = e.newName();
            }
            case PlanEvents.ProjectCreated e -> {
                Project project = new Project( e.projectId(), e.projectName(), this.planId);
                this.projects.put(projectId, project);
            }
            case PlanEvents.TaskCreated e -> {
                Task task = new Task(e.taskId(), e.taskName(), e.projectName());
                
                // Find project and add task to it
                Project project = projects.values().stream()
                        .filter(p -> p.getName().equals(projectName))
                        .findFirst()
                        .orElse(null);
                if (project != null) {
                    project.addTask(task);
                }
                
                // Update nextTaskId to be the next available ID
                try {
                    int taskIdValue = Integer.parseInt(e.taskId());
                    if (taskIdValue >= nextTaskId) {
                        nextTaskId = taskIdValue + 1;
                    }
                } catch (NumberFormatException ex) {
                    // Ignore non-numeric task IDs
                }
            }
            case PlanEvents.TaskChecked e -> {
                // Find project and delegate task checking
                Project project = projects.values().stream()
                        .filter(p -> p.getName().equals(e.projectName()))
                        .findFirst()
                        .orElse(null);
                if (project != null) {
                    project.checkTask(e.taskId());
                }
            }
            case PlanEvents.TaskUnchecked e -> {
                // Find project and delegate task unchecking
                Project project = projects.values().stream()
                        .filter(p -> p.getName().equals(e.projectName()))
                        .findFirst()
                        .orElse(null);
                if (project != null) {
                    project.uncheckTask(e.taskId());
                }
            }
            case PlanEvents.TaskDeleted e -> {
                // Find project and delete the task
                Project project = projects.values().stream()
                        .filter(p -> p.getName().equals(e.projectName()))
                        .findFirst()
                        .orElse(null);
                if (project != null) {
                    project.deleteTask(e.taskId());
                }
            }
            case PlanEvents.TaskDeadlineSet e -> {
                LocalDate deadline = e.deadline() != null ? LocalDate.parse(e.deadline()) : null;
                
                // Find project and set the task deadline
                Project project = projects.values().stream()
                        .filter(p -> p.getName().equals(e.projectName()))
                        .findFirst()
                        .orElse(null);
                if (project != null) {
                    project.setTaskDeadline(e.taskId(), deadline);
                }
            }
            case PlanEvents.TaskRenamed e -> {
                // Find project and rename the task
                Project project = projects.values().stream()
                        .filter(p -> p.getName().equals(e.projectName()))
                        .findFirst()
                        .orElse(null);
                if (project != null) {
                    project.renameTask(e.taskId(), e.newName());
                }
            }
            case PlanEvents.ProjectDeleted e -> {
                this.projects.remove(e.projectId());
            }
            case PlanEvents.PlanDeleted e -> {
                this.isDeleted = true;
            }
            case PlanEvents.TagAssigned e -> {
                // Find project and assign tag to task
                Project project = projects.values().stream()
                        .filter(p -> p.getName().equals(e.projectName()))
                        .findFirst()
                        .orElse(null);
                if (project != null) {
                    Task task = project.getTask(e.taskId());
                    if (task != null) {
                        task.assignTag(e.tagId());
                    }
                }
            }
            case PlanEvents.TagUnassigned e -> {
                // Find project and unassign tag from task
                Project project = projects.values().stream()
                        .filter(p -> p.getName().equals(e.projectName()))
                        .findFirst()
                        .orElse(null);
                if (project != null) {
                    Task task = project.getTask(e.taskId());
                    if (task != null) {
                        task.unassignTag(e.tagId());
                    }
                }
            }
            default -> {
                // Handle unknown event types
            }
        }
    }
}