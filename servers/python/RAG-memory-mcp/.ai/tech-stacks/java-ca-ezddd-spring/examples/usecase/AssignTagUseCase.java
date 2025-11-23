package tw.teddysoft.example.plan.usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

public interface AssignTagUseCase extends Command<AssignTagUseCase.AssignTagInput, CqrsOutput> {
    
    class AssignTagInput implements Input {
        public String planId;
        public String projectName;
        public String taskId;
        public String tagId;
        
        public static AssignTagInput create() {
            return new AssignTagInput();
        }
    }
}