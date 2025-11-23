package tw.teddysoft.example.plan.usecase.service;

import tw.teddysoft.example.plan.entity.Plan;
import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.example.plan.usecase.port.in.DeleteTaskUseCase;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;

import static tw.teddysoft.ucontract.Contract.*;

public class DeleteTaskService implements DeleteTaskUseCase {
    private final Repository<Plan, PlanId> repository;

    public DeleteTaskService(Repository<Plan, PlanId> repository) {
        requireNotNull("Repository", repository);
        this.repository = repository;
    }

    @Override
    public CqrsOutput execute(DeleteTaskInput input) {
        // Preconditions
        requireNotNull("Input", input);
        requireNotNull("Plan id", input.planId);
        requireNotNull("Project name", input.projectName);
        requireNotNull("Task id", input.taskId);

        // Load aggregate
        Plan plan = repository.findById(PlanId.valueOf(input.planId))
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + input.planId));

        // Business logic verification
        require("Project exists", () -> plan.hasProject(input.projectName));
        require("Task exists", () -> plan.getProject(input.projectName).hasTask(input.taskId));

        // Execute business logic
        plan.deleteTask(input.projectName, input.taskId);

        // Save changes
        repository.save(plan);

        // Create output
        return CqrsOutput.create()
                .setExitCode(ExitCode.SUCCESS);
    }
}