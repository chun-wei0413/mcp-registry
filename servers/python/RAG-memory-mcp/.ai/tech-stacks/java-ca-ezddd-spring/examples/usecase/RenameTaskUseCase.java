package tw.teddysoft.example.plan.usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

public interface RenameTaskUseCase extends Command<RenameTaskUseCase.RenameTaskInput, CqrsOutput> {
    
    class RenameTaskInput implements Input {
        public String planId;
        public String projectName;
        public String taskId;
        public String newTaskName;
        
        public static RenameTaskInput create() {
            return new RenameTaskInput();
        }
    }
}