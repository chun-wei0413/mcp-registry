package tw.teddysoft.example.plan.usecase.port.out.projection;

import tw.teddysoft.example.plan.usecase.port.TaskDueTodayDto;

import java.util.List;

public interface TasksSortedByDeadlineProjection {
    
    List<TaskDueTodayDto> query(TasksSortedByDeadlineProjectionInput input);
    
    class TasksSortedByDeadlineProjectionInput {
        private final String userId;
        private final String sortOrder;
        
        public TasksSortedByDeadlineProjectionInput(String userId, String sortOrder) {
            this.userId = userId;
            this.sortOrder = sortOrder;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getSortOrder() {
            return sortOrder;
        }
    }
}