package tw.teddysoft.example.plan.usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.query.Query;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;
import tw.teddysoft.example.plan.usecase.port.TaskDto;

import java.time.LocalDate;
import java.util.List;

public interface GetTasksByDateUseCase extends Query<GetTasksByDateUseCase.GetTasksByDateInput, GetTasksByDateUseCase.GetTasksByDateOutput> {
    
    class GetTasksByDateInput implements Input {
        public String userId;
        public LocalDate targetDate;
        
        public static GetTasksByDateInput create() {
            return new GetTasksByDateInput();
        }
    }
    
    class GetTasksByDateOutput extends CqrsOutput {
        public List<TaskDto> tasks;
        
        public static GetTasksByDateOutput create() {
            return new GetTasksByDateOutput();
        }
    }
}