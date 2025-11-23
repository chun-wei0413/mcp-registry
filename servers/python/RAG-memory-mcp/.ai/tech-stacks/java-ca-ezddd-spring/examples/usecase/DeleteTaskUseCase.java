package tw.teddysoft.example.plan.usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

public interface DeleteTaskUseCase extends Command<DeleteTaskUseCase.DeleteTaskInput, CqrsOutput> {
    
    class DeleteTaskInput implements Input {
        public String planId;
        public String projectName;
        public String taskId;
    }
}