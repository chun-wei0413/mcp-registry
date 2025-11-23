package tw.teddysoft.example.plan.usecase.service;

import tw.teddysoft.example.plan.usecase.port.in.GetPlanUseCase;
import tw.teddysoft.example.plan.usecase.port.out.projection.PlanProjection;
import tw.teddysoft.example.plan.usecase.port.PlanDto;

import static tw.teddysoft.ucontract.Contract.*;

public class GetPlanService implements GetPlanUseCase {
    private final PlanProjection projection;

    public GetPlanService(PlanProjection projection) {
        requireNotNull("Projection", projection);
        this.projection = projection;
    }

    @Override
    public PlanDto execute(GetPlanInput input) {
        // Preconditions
        requireNotNull("Input", input);
        requireNotNull("Plan id", input.planId);
        require("Plan id is not empty", () -> !input.planId.trim().isEmpty());

        // Query data
        PlanDto plan = projection.findById(input.planId);

        // Postconditions
        ensure("Plan found or null", () -> 
            plan == null || plan.planId().equals(input.planId));

        return plan;
    }
}