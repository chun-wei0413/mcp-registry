package tw.teddysoft.example.plan.usecase.service;

import tw.teddysoft.example.plan.usecase.port.in.GetTasksByDateUseCase;
import tw.teddysoft.example.plan.usecase.port.out.projection.TasksByDateProjection;
import tw.teddysoft.example.plan.usecase.port.TaskDto;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;

import java.util.List;

import static tw.teddysoft.ucontract.Contract.*;

public class GetTasksByDateService implements GetTasksByDateUseCase {
    private final TasksByDateProjection projection;

    public GetTasksByDateService(TasksByDateProjection projection) {
        requireNotNull("Projection", projection);
        this.projection = projection;
    }

    @Override
    public GetTasksByDateOutput execute(GetTasksByDateInput input) {
        // Preconditions
        requireNotNull("Input", input);
        requireNotNull("User id", input.userId);
        requireNotNull("Target date", input.targetDate);
        require("User id is not empty", () -> !input.userId.trim().isEmpty());

        // Query data
        List<TaskDto> tasks = projection.findTasksByDate(input.userId, input.targetDate);

        // Create output
        GetTasksByDateOutput output = GetTasksByDateOutput.create();
        output.tasks = tasks;
        output.setExitCode(ExitCode.SUCCESS);

        // Postconditions
        ensure("Tasks list is not null", () -> output.tasks != null);
        ensure("All tasks match the target date", () -> 
            output.tasks.stream().allMatch(task -> 
                task.deadline() != null && 
                task.deadline().equals(input.targetDate.toString())
            )
        );

        return output;
    }
}