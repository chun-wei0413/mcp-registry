package tw.teddysoft.example.plan.usecase.service;

import tw.teddysoft.example.plan.entity.Plan;
import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.example.plan.usecase.port.in.RenameTaskUseCase;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;

import static tw.teddysoft.ucontract.Contract.*;

public class RenameTaskService implements RenameTaskUseCase {
    private final Repository<Plan, PlanId> repository;

    public RenameTaskService(Repository<Plan, PlanId> repository) {
        requireNotNull("Repository", repository);
        this.repository = repository;
    }

    @Override
    public CqrsOutput execute(RenameTaskInput input) {
        // Preconditions
        requireNotNull("Input", input);
        requireNotNull("Plan id", input.planId);
        requireNotNull("Project name", input.projectName);
        requireNotNull("Task id", input.taskId);
        requireNotNull("New task name", input.newTaskName);
        require("New task name is not empty", () -> !input.newTaskName.trim().isEmpty());

        // Load aggregate
        Plan plan = repository.findById(PlanId.valueOf(input.planId))
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + input.planId));

        // Business logic verification
        require("Project exists", () -> plan.hasProject(input.projectName));
        require("Task exists", () -> plan.getProject(input.projectName).hasTask(input.taskId));

        // Execute business logic
        plan.renameTask(input.projectName, input.taskId, input.newTaskName);

        // Save changes
        repository.save(plan);

        // Create output
        return CqrsOutput.create()
                .setExitCode(ExitCode.SUCCESS);
    }
}