package tw.teddysoft.example.plan.usecase.port.in;

import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.example.plan.entity.ProjectName;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

public interface CreateTaskUseCase extends Command<CreateTaskUseCase.CreateTaskInput, CqrsOutput> {
    
    class CreateTaskInput implements Input {
        public PlanId planId;
        public ProjectName projectName;
        public String taskName;
        
        public static CreateTaskInput create() {
            return new CreateTaskInput();
        }
    }
}