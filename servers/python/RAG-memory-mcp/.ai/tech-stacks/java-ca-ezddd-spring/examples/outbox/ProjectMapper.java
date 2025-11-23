package tw.teddysoft.example.plan.usecase.port;

import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.example.plan.entity.Project;
import tw.teddysoft.example.plan.entity.ProjectId;
import tw.teddysoft.example.plan.entity.ProjectName;
import tw.teddysoft.example.plan.entity.Task;
import tw.teddysoft.example.plan.usecase.port.out.ProjectData;
import tw.teddysoft.example.plan.usecase.port.out.TaskData;

import java.util.ArrayList;
import java.util.List;

import java.util.Objects;

public class ProjectMapper {
    
    public static ProjectData toData(Project project) {
        Objects.requireNonNull(project, "Project cannot be null");
        
        ProjectData projectData = new ProjectData();
        projectData.setProjectId(project.getId().value());
        projectData.setName(project.getName().value());
        // planId is now managed through the PlanData relationship
        
        // Convert tasks to data
        for (Task task : project.getTasks().values()) {
            TaskData taskData = TaskMapper.toData(task, project.getId().value());
            projectData.addTaskData(taskData);
        }
        
        return projectData;
    }
    
    public static ProjectDto toDto(Project project) {
        Objects.requireNonNull(project, "Project cannot be null");
        
        ProjectDto projectDto = new ProjectDto()
                .setId(project.getId().value())
                .setName(project.getName().value());
        
        // Convert tasks to DTOs
        List<TaskDto> taskDtos = new ArrayList<>();
        for (Task task : project.getTasks().values()) {
            taskDtos.add(TaskMapper.toDto(task));
        }
        projectDto.setTasks(taskDtos);
        
        return projectDto;
    }
    
    public static ProjectDto toDto(ProjectData projectData) {
        Objects.requireNonNull(projectData, "ProjectData cannot be null");
        
        ProjectDto dto = new ProjectDto();
        dto.setId(projectData.getProjectId());
        dto.setName(projectData.getName());
        
        List<TaskDto> taskDtos = new ArrayList<>();
        for (TaskData taskData : projectData.getTaskDatas()) {
            TaskDto taskDto = TaskMapper.toDto(taskData);
            taskDtos.add(taskDto);
        }
        dto.setTasks(taskDtos);
        
        return dto;
    }
    
    public static Project toDomain(ProjectDto projectDto, PlanId planId) {
        Objects.requireNonNull(projectDto, "ProjectDto cannot be null");
        Objects.requireNonNull(planId, "PlanId cannot be null");
        
        // Note: Project creation requires planId
        Project project = new Project(
                ProjectId.valueOf(projectDto.getId()),
                ProjectName.valueOf(projectDto.getName()),
                planId
        );
        
        // Note: Tasks cannot be directly added due to domain rules
        // They must be created through domain methods (createTask)
        // This is a limitation of converting DTOs back to domain objects
        
        return project;
    }
}