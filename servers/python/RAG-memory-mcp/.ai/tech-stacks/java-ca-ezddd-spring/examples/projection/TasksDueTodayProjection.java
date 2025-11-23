package tw.teddysoft.example.plan.usecase.port.out.projection;

import tw.teddysoft.example.plan.usecase.port.TaskDueTodayDto;
import java.util.List;

public interface TasksDueTodayProjection {
    
    List<TaskDueTodayDto> query(TasksDueTodayProjectionInput input);
    
    class TasksDueTodayProjectionInput {
        public String userId;
        
        public TasksDueTodayProjectionInput(String userId) {
            this.userId = userId;
        }
    }
}