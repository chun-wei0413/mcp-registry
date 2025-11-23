package tw.teddysoft.example.plan.usecase.port;

import tw.teddysoft.example.plan.entity.ProjectName;
import tw.teddysoft.example.plan.entity.Task;
import tw.teddysoft.example.plan.entity.TaskId;
import tw.teddysoft.example.plan.usecase.port.out.TaskData;
import tw.teddysoft.example.tag.entity.TagId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

public class TaskMapper {
    
    public static TaskData toData(Task task, String projectId) {
        Objects.requireNonNull(task, "Task cannot be null");
        Objects.requireNonNull(projectId, "ProjectId cannot be null");
        
        TaskData taskData = new TaskData();
        taskData.setTaskId(task.getId().value());
        taskData.setName(task.getName());
        taskData.setProjectName(task.getProjectName().value());
        // projectId is now managed through the ProjectData relationship
        taskData.setDone(task.isDone());
        taskData.setDeadline(task.getDeadline());
        
        // Convert tags to tag IDs
        Set<String> tagIdSet = task.getTags().stream()
                .map(TagId::value)
                .collect(Collectors.toSet());
        taskData.setTagIds(tagIdSet);
        
        return taskData;
    }
    
    public static TaskDto toDto(Task task) {
        Objects.requireNonNull(task, "Task cannot be null");
        
        return new TaskDto()
                .setId(task.getId().value())
                .setName(task.getName())
                .setDone(task.isDone())
                .setDeadline(task.getDeadline() != null ? task.getDeadline().toString() : null)
                .setTagIds(task.getTags().stream()
                        .map(TagId::value)
                        .collect(Collectors.toList()));
    }
    
    public static TaskDto toDto(TaskData taskData) {
        Objects.requireNonNull(taskData, "TaskData cannot be null");
        
        TaskDto dto = new TaskDto();
        dto.setId(taskData.getTaskId());
        dto.setName(taskData.getName());
        dto.setDone(taskData.isDone());
        dto.setDeadline(taskData.getDeadline() != null ? taskData.getDeadline().toString() : null);
        dto.setTagIds(new ArrayList<>(taskData.getTagIds()));
        
        return dto;
    }
    
    public static Task toDomain(TaskDto taskDto, ProjectName projectName) {
        Objects.requireNonNull(taskDto, "TaskDto cannot be null");
        Objects.requireNonNull(projectName, "ProjectName cannot be null");
        
        // Note: Task creation requires projectName
        Task task = new Task(
                TaskId.valueOf(taskDto.getId()),
                taskDto.getName(),
                projectName
        );
        
        // Note: Task done status cannot be directly set in constructor
        // It must be set through domain methods (markAsDone/unmarkAsDone)
        // This is a limitation of converting DTOs back to domain objects
        
        return task;
    }
}