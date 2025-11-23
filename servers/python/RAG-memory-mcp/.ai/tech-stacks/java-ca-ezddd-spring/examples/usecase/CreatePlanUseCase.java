package tw.teddysoft.example.plan.usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

public interface CreatePlanUseCase extends Command<CreatePlanUseCase.CreatePlanInput, CqrsOutput> {
    
    class CreatePlanInput implements Input {
        public String id;
        public String name;
        public String userId;
        
        public static CreatePlanInput create() {
            return new CreatePlanInput();
        }
    }
}