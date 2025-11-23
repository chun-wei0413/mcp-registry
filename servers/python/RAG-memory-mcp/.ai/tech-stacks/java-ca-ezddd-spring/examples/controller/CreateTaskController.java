package tw.teddysoft.example.plan.adapter.in.controller.rest.springboot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.example.plan.entity.ProjectName;
import tw.teddysoft.example.plan.usecase.port.in.CreateTaskUseCase;
import tw.teddysoft.example.plan.usecase.port.in.CreateTaskUseCase.CreateTaskInput;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;

@RestController
@RequestMapping("/api/v1")
public class CreateTaskController {
    
    private final CreateTaskUseCase createTaskUseCase;
    
    @Autowired
    public CreateTaskController(CreateTaskUseCase createTaskUseCase) {
        this.createTaskUseCase = createTaskUseCase;
    }
    
    @PostMapping("/plans/{planId}/projects/{projectName}/tasks")
    public ResponseEntity<CreateTaskResponse> createTask(
            @PathVariable("planId") String planId,
            @PathVariable("projectName") String projectName,
            @RequestBody CreateTaskRequest request) {
        
        CreateTaskInput input = CreateTaskInput.create();
        input.planId = PlanId.valueOf(planId);
        input.projectName = ProjectName.valueOf(projectName);
        input.taskName = request.getTaskName();
        
        CqrsOutput output = createTaskUseCase.execute(input);
        
        if (output.getExitCode() == ExitCode.SUCCESS) {
            CreateTaskResponse response = new CreateTaskResponse();
            response.setTaskId(output.getId());
            response.setMessage("Task created successfully");
            response.setSuccess(true);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            CreateTaskResponse response = new CreateTaskResponse();
            response.setMessage(output.getMessage());
            response.setSuccess(false);
            
            // Check if it's a "not found" error
            if (output.getMessage() != null && output.getMessage().toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    // Request DTO
    public static class CreateTaskRequest {
        private String taskName;
        
        public String getTaskName() {
            return taskName;
        }
        
        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }
    }
    
    // Response DTO
    public static class CreateTaskResponse {
        private String taskId;
        private String message;
        private boolean success;
        
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
    }
}