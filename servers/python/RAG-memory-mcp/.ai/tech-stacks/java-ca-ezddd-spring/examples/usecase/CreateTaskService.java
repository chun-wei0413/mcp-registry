package tw.teddysoft.example.plan.usecase.service;

import tw.teddysoft.example.plan.entity.Plan;
import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.example.plan.entity.TaskId;
import tw.teddysoft.example.plan.usecase.port.in.CreateTaskUseCase;
import tw.teddysoft.example.plan.usecase.port.in.CreateTaskUseCase.CreateTaskInput;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;
import tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

import static tw.teddysoft.ucontract.Contract.requireNotNull;

public class CreateTaskService implements CreateTaskUseCase {

    private final Repository<Plan, PlanId> planRepository;

    public CreateTaskService(Repository<Plan, PlanId> planRepository) {
        requireNotNull("PlanRepository", planRepository);
        this.planRepository = planRepository;
    }

    @Override
    public CqrsOutput execute(CreateTaskInput input) {
        try {
            var output = CqrsOutput.create();

            // 載入聚合根
            Plan plan = planRepository.findById(input.planId).orElse(null);
            if (null == plan) {
                output.setId(input.planId.value())
                        .setExitCode(ExitCode.FAILURE)
                        .setMessage("Create task failed: plan not found, plan id = " + input.planId.value());
                return output;
            }

            // 執行業務邏輯
            TaskId taskId = plan.createTask(input.projectName, input.taskName);

            // 保存聚合根
            planRepository.save(plan);

            output.setId(taskId.value());
            output.setExitCode(ExitCode.SUCCESS);
            return output;
        } catch (Exception e) {
            throw new UseCaseFailureException(e);
        }
    }
}