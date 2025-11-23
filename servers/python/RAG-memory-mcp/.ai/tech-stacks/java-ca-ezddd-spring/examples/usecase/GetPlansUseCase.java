package tw.teddysoft.example.plan.usecase.port.in;

import tw.teddysoft.example.plan.usecase.port.PlanDto;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.query.Query;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

import java.util.List;

public interface GetPlansUseCase extends Query<GetPlansUseCase.GetPlansInput, GetPlansUseCase.GetPlansOutput> {
    
    class GetPlansInput implements Input {
        public String userId;
        public String sortBy;
        public String sortOrder;
        
        public static GetPlansInput create() {
            return new GetPlansInput();
        }
    }
    
    class GetPlansOutput extends CqrsOutput {
        public List<PlanDto> plans;
        
        public static GetPlansOutput create() {
            return new GetPlansOutput();
        }
        
        public List<PlanDto> getPlans() {
            return plans;
        }
        
        public GetPlansOutput setPlans(List<PlanDto> plans) {
            this.plans = plans;
            return this;
        }
    }
}