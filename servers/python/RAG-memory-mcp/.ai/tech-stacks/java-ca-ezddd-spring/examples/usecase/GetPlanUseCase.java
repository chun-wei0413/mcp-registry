package tw.teddysoft.example.plan.usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.query.Query;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;
import tw.teddysoft.example.plan.usecase.port.PlanDto;

public interface GetPlanUseCase extends Query<GetPlanUseCase.GetPlanInput, PlanDto> {
    
    class GetPlanInput implements Input {
        public String planId;
        
        public static GetPlanInput create() {
            return new GetPlanInput();
        }
    }
}