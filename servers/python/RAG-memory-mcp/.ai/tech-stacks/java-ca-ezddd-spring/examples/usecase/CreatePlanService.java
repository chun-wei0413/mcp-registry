package tw.teddysoft.example.plan.usecase.service;

import tw.teddysoft.example.plan.entity.Plan;
import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.example.plan.usecase.port.in.CreatePlanUseCase;
import tw.teddysoft.example.plan.usecase.port.in.CreatePlanUseCase.CreatePlanInput;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;
import tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

import static tw.teddysoft.ucontract.Contract.requireNotNull;

public class CreatePlanService implements CreatePlanUseCase {

  private final Repository<Plan, PlanId> planRepository;

  public CreatePlanService(Repository<Plan, PlanId> planRepository) {
    requireNotNull("PlanRepository", planRepository);

    this.planRepository = planRepository;
  }

  @Override
  public CqrsOutput execute(CreatePlanInput input) {
    try {
      var output = CqrsOutput.create();
      
      // Check if plan name is empty or whitespace only (but not null - let Plan constructor handle null)
      if (input.name != null && input.name.trim().isEmpty()) {
        output.setExitCode(ExitCode.FAILURE);
        output.setMessage("Plan name cannot be empty");
        return output;
      }
      
      // *** 重要：創建新聚合根時要檢查 ID 是否已存在 ***
      // Check if plan with same ID already exists
      PlanId planId = PlanId.valueOf(input.id);
      if (planRepository.findById(planId).isPresent()) {
        throw new IllegalArgumentException("Plan with id " + input.id + " already exists");
      }

      // *** 直接創建新的 Aggregate Root ***
      Plan plan = new Plan(planId, input.name, input.userId);

      // *** 儲存新創建的聚合根 ***
      planRepository.save(plan);

      output.setId(input.id);
      output.setExitCode(ExitCode.SUCCESS);
      return output;
    } catch (Exception e) {
      throw new UseCaseFailureException(e);
    }
  }
}