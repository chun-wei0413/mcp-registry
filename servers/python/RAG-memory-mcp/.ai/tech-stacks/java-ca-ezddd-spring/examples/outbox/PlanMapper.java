package tw.teddysoft.example.plan.usecase.port;

import tw.teddysoft.example.common.DateProvider;
import tw.teddysoft.example.plan.entity.Plan;
import tw.teddysoft.example.plan.entity.PlanEvents;
import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.example.plan.entity.Project;
import tw.teddysoft.example.plan.entity.ProjectId;
import tw.teddysoft.example.plan.entity.ProjectName;
import tw.teddysoft.example.plan.entity.Task;
import tw.teddysoft.example.plan.entity.TaskId;
import tw.teddysoft.example.plan.usecase.port.out.PlanData;
import tw.teddysoft.example.plan.usecase.port.out.ProjectData;
import tw.teddysoft.example.plan.usecase.port.out.TaskData;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import java.util.Objects;

public class PlanMapper {
    
    public static PlanData toData(Plan plan) {
        Objects.requireNonNull(plan, "Plan cannot be null");
        
        PlanData planData = new PlanData(plan.getVersion());
        planData.setPlanId(plan.getId().value());
        planData.setName(plan.getName());
        planData.setUserId(plan.getUserId());
        planData.setNextTaskId(0); // This would need to be exposed by Plan entity
        planData.setDeleted(plan.isDeleted());
        // Set timestamps based on domain events
        if (!plan.getDomainEvents().isEmpty()) {
            // Set createdAt to the timestamp of the first event (should be PlanCreated)
            planData.setCreatedAt(plan.getDomainEvents().get(0).occurredOn());
            // Set lastUpdated to the timestamp of the most recent event
            planData.setLastUpdated(plan.getDomainEvents().get(plan.getDomainEvents().size() - 1).occurredOn());
        } else {
            // Fallback to current time if no events (shouldn't happen in normal flow)
            planData.setCreatedAt(DateProvider.now());
            planData.setLastUpdated(DateProvider.now());
        }
        planData.setStreamName(plan.getStreamName());
        planData.setDomainEventDatas(plan.getDomainEvents().stream()
                .map(DomainEventMapper::toData)
                .collect(Collectors.toList()));
        
        // Synchronize projects with the domain model
        // Remove projects that no longer exist in the domain
        planData.getProjectDatas().removeIf(projectData -> 
            !plan.getProjects().containsKey(ProjectId.valueOf(projectData.getProjectId()))
        );
        
        // Add or update projects from the domain
        for (Project project : plan.getProjects().values()) {
            // Check if project already exists in planData
            ProjectData existingProjectData = planData.getProjectDatas().stream()
                .filter(pd -> pd.getProjectId().equals(project.getId().value()))
                .findFirst()
                .orElse(null);
                
            if (existingProjectData == null) {
                // Add new project
                ProjectData projectData = ProjectMapper.toData(project);
                planData.addProjectData(projectData);
            } else {
                // Update existing project's tasks
                // Remove tasks that no longer exist in the domain
                existingProjectData.getTaskDatas().removeIf(taskData -> 
                    !project.getTasks().containsKey(TaskId.valueOf(taskData.getTaskId()))
                );
                
                // Add or update tasks from the domain
                for (Task task : project.getTasks().values()) {
                    // Check if task already exists
                    TaskData existingTaskData = existingProjectData.getTaskDatas().stream()
                        .filter(td -> td.getTaskId().equals(task.getId().value()))
                        .findFirst()
                        .orElse(null);
                        
                    if (existingTaskData == null) {
                        // Add new task
                        TaskData taskData = TaskMapper.toData(task, project.getId().value());
                        existingProjectData.addTaskData(taskData);
                    } else {
                        // Update existing task
                        existingTaskData.setName(task.getName());
                        existingTaskData.setDone(task.isDone());
                        
                        // Update tag IDs from domain model
                        existingTaskData.getTagIds().clear();
                        for (tw.teddysoft.example.tag.entity.TagId tagId : task.getTags()) {
                            existingTaskData.getTagIds().add(tagId.value());
                        }
                    }
                }
            }
        }
        
        return planData;
    }
    
    public static List<PlanData> toData(List<Plan> plans) {
        List<PlanData> result = new ArrayList<>();
        plans.forEach(x -> result.add(toData(x)));
        return result;
    }
    
    public static PlanDto toDto(Plan plan) {
        Objects.requireNonNull(plan, "Plan cannot be null");
        
        PlanDto planDto = new PlanDto()
                .setId(plan.getId().value())
                .setName(plan.getName())
                .setUserId(plan.getUserId());
        
        // Convert projects to DTOs
        List<ProjectDto> projectDtos = new ArrayList<>();
        for (Project project : plan.getProjects().values()) {
            projectDtos.add(ProjectMapper.toDto(project));
        }
        planDto.setProjects(projectDtos);
        
        return planDto;
    }
    
    public static Plan toDomain(PlanData planData) {
        Objects.requireNonNull(planData, "PlanData cannot be null");
        
        // Reconstruct Plan from events if available
        if (planData.getDomainEventDatas() != null && !planData.getDomainEventDatas().isEmpty()) {
            // Convert domain event data back to domain events
            var domainEvents = planData.getDomainEventDatas().stream()
                    .map(DomainEventMapper::toDomain)
                    .map(event -> (PlanEvents) event)
                    .collect(Collectors.toList());
            
            // Create Plan from events
            Plan plan = new Plan(domainEvents);
            plan.setVersion(planData.getVersion());
            plan.clearDomainEvents();
            return plan;
        } else {
            // Create Plan and reconstruct its state from PlanData
            List<PlanEvents> events = new ArrayList<>();
            
            // Create PlanCreated event
            events.add(new PlanEvents.PlanCreated(
                    PlanId.valueOf(planData.getPlanId()),
                    planData.getName(),
                    planData.getUserId(),
                    UUID.randomUUID(),
                    planData.getCreatedAt()
            ));
            
            // Create ProjectCreated events for each project
            for (ProjectData projectData : planData.getProjectDatas()) {
                events.add(new PlanEvents.ProjectCreated(
                        PlanId.valueOf(planData.getPlanId()),
                        ProjectId.valueOf(projectData.getProjectId()),
                        ProjectName.valueOf(projectData.getName()),
                        UUID.randomUUID(),
                        planData.getCreatedAt()
                ));
                
                // Create TaskCreated events for each task in the project
                for (TaskData taskData : projectData.getTaskDatas()) {
                    events.add(new PlanEvents.TaskCreated(
                            PlanId.valueOf(planData.getPlanId()),
                            ProjectId.valueOf(projectData.getProjectId()),
                            TaskId.valueOf(taskData.getTaskId()),
                            taskData.getName(),
                            UUID.randomUUID(),
                            planData.getCreatedAt()
                    ));
                    
                    // If task is done, create TaskChecked event
                    if (taskData.isDone()) {
                        events.add(new PlanEvents.TaskChecked(
                                PlanId.valueOf(planData.getPlanId()),
                                ProjectId.valueOf(projectData.getProjectId()),
                                TaskId.valueOf(taskData.getTaskId()),
                                UUID.randomUUID(),
                                planData.getLastUpdated()
                        ));
                    }
                    
                    // If task has deadline, create TaskDeadlineSet event
                    if (taskData.getDeadline() != null) {
                        events.add(new PlanEvents.TaskDeadlineSet(
                                PlanId.valueOf(planData.getPlanId()),
                                ProjectId.valueOf(projectData.getProjectId()),
                                TaskId.valueOf(taskData.getTaskId()),
                                taskData.getDeadline().toString(),
                                UUID.randomUUID(),
                                planData.getLastUpdated()
                        ));
                    }
                    
                    // Create TagAssigned events for existing tags
                    for (String tagId : taskData.getTagIds()) {
                        events.add(new PlanEvents.TagAssigned(
                                PlanId.valueOf(planData.getPlanId()),
                                ProjectId.valueOf(projectData.getProjectId()),
                                TaskId.valueOf(taskData.getTaskId()),
                                tw.teddysoft.example.tag.entity.TagId.valueOf(tagId),
                                UUID.randomUUID(),
                                planData.getLastUpdated()
                        ));
                    }
                }
            }
            
            // If plan is deleted, add PlanDeleted event
            if (planData.isDeleted()) {
                events.add(new PlanEvents.PlanDeleted(
                        PlanId.valueOf(planData.getPlanId()),
                        UUID.randomUUID(),
                        planData.getLastUpdated()
                ));
            }
            
            // Create Plan from reconstructed events
            Plan plan = new Plan(events);
            plan.setVersion(planData.getVersion());
            plan.clearDomainEvents();
            return plan;
        }
    }
    
    public static List<Plan> toDomain(List<PlanData> planDatas) {
        Objects.requireNonNull(planDatas, "PlanData list cannot be null");
        
        List<Plan> result = new ArrayList<>();
        planDatas.forEach(x -> result.add(toDomain(x)));
        return result;
    }
    
    public static PlanDto toDto(PlanData planData) {
        Objects.requireNonNull(planData, "PlanData cannot be null");
        
        PlanDto dto = new PlanDto();
        dto.setId(planData.getPlanId());
        dto.setName(planData.getName());
        dto.setUserId(planData.getUserId());
        
        List<ProjectDto> projectDtos = new ArrayList<>();
        for (ProjectData projectData : planData.getProjectDatas()) {
            ProjectDto projectDto = ProjectMapper.toDto(projectData);
            projectDtos.add(projectDto);
        }
        dto.setProjects(projectDtos);
        
        return dto;
    }
    
    public static List<PlanDto> toDto(List<PlanData> planDatas) {
        Objects.requireNonNull(planDatas, "PlanData list cannot be null");
        
        List<PlanDto> result = new ArrayList<>();
        planDatas.forEach(x -> result.add(toDto(x)));
        return result;
    }
    
    // Only for aggregate mappers
    private static OutboxMapper mapper = new PlanMapper.Mapper();
    
    // Only for aggregate mappers
    public static OutboxMapper newMapper() {
        return mapper;
    }
    
    // Only for aggregate mappers
    static class Mapper implements OutboxMapper<Plan, PlanData> {
        
        @Override
        public Plan toDomain(PlanData data) {
            return PlanMapper.toDomain(data);
        }
        
        @Override
        public PlanData toData(Plan aggregateRoot) {
            return PlanMapper.toData(aggregateRoot);
        }
    }
}