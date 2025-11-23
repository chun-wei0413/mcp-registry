package tw.teddysoft.example.plan.usecase.port.out.projection;

import tw.teddysoft.example.plan.usecase.port.TaskDueTodayDto;

import java.time.LocalDate;
import java.util.List;

public interface TasksByDateProjection {
    
    List<TaskDueTodayDto> query(TasksByDateProjectionInput input);
    
    class TasksByDateProjectionInput {
        public final String userId;
        public final LocalDate targetDate;
        
        public TasksByDateProjectionInput(String userId, LocalDate targetDate) {
            this.userId = userId;
            this.targetDate = targetDate;
        }
    }
}