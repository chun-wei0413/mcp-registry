package tw.teddysoft.example.plan.usecase.service;

import tw.teddysoft.example.plan.entity.Plan;
import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.example.tag.entity.Tag;
import tw.teddysoft.example.tag.entity.TagId;
import tw.teddysoft.example.plan.usecase.port.in.AssignTagUseCase;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;

import static tw.teddysoft.ucontract.Contract.*;

public class AssignTagService implements AssignTagUseCase {
    private final Repository<Plan, PlanId> planRepository;
    private final Repository<Tag, TagId> tagRepository;

    public AssignTagService(Repository<Plan, PlanId> planRepository, 
                           Repository<Tag, TagId> tagRepository) {
        requireNotNull("Plan repository", planRepository);
        requireNotNull("Tag repository", tagRepository);
        this.planRepository = planRepository;
        this.tagRepository = tagRepository;
    }

    @Override
    public CqrsOutput execute(AssignTagInput input) {
        // Preconditions
        requireNotNull("Input", input);
        requireNotNull("Plan id", input.planId);
        requireNotNull("Project name", input.projectName);
        requireNotNull("Task id", input.taskId);
        requireNotNull("Tag id", input.tagId);

        // Load aggregates
        Plan plan = planRepository.findById(PlanId.valueOf(input.planId))
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + input.planId));
                
        Tag tag = tagRepository.findById(TagId.valueOf(input.tagId))
                .orElseThrow(() -> new IllegalArgumentException("Tag not found: " + input.tagId));

        // Business logic verification
        require("Project exists", () -> plan.hasProject(input.projectName));
        require("Task exists", () -> plan.getProject(input.projectName).hasTask(input.taskId));
        require("Tag belongs to same plan", () -> tag.getPlanId().equals(plan.getId()));
        require("Tag is not deleted", () -> !tag.isDeleted());

        // Execute business logic
        plan.assignTag(input.projectName, input.taskId, TagId.valueOf(input.tagId));

        // Save changes
        planRepository.save(plan);

        // Create output
        return CqrsOutput.create()
                .setExitCode(ExitCode.SUCCESS);
    }
}