package tw.teddysoft.example.plan.usecase.service;

import tw.teddysoft.example.plan.usecase.port.PlanDto;
import tw.teddysoft.example.plan.usecase.port.in.GetPlansUseCase;
import tw.teddysoft.example.plan.usecase.port.in.GetPlansUseCase.GetPlansInput;
import tw.teddysoft.example.plan.usecase.port.in.GetPlansUseCase.GetPlansOutput;
import tw.teddysoft.example.plan.usecase.port.out.PlanDtosProjection;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;
import tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class GetPlansService implements GetPlansUseCase {

    private final PlanDtosProjection planDtosProjection;

    @Override
    public GetPlansOutput execute(GetPlansInput input) {
        try {
            var output = GetPlansOutput.create();
            var projectionInput = new PlanDtosProjection.PlanDtosProjectionInput();
            projectionInput.userId = input.userId;
            projectionInput.sortBy = input.sortBy;
            projectionInput.sortOrder = input.sortOrder;

            List<PlanDto> plans = planDtosProjection.query(projectionInput);

            output.setPlans(plans);
            output.setExitCode(ExitCode.SUCCESS);
            
            return output;
        } catch (Exception e) {
            throw new UseCaseFailureException(e);
        }
    }
}