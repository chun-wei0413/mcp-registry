package tw.teddysoft.example.plan.adapter.out.projection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tw.teddysoft.example.plan.usecase.port.TaskDueTodayDto;
import tw.teddysoft.example.plan.usecase.port.out.PlanData;
import tw.teddysoft.example.plan.usecase.port.out.projection.TasksByDateProjection;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JpaTasksByDateProjection extends JpaRepository<PlanData, String>, TasksByDateProjection {
    
    @Override
    default List<TaskDueTodayDto> query(TasksByDateProjectionInput input) {
        return findTasksByDate(input.userId, input.targetDate);
    }
    
    @Query("""
        SELECT new tw.teddysoft.example.plan.usecase.port.TaskDueTodayDto(
            t.taskId,
            t.name,
            t.done,
            CAST(t.deadline AS string),
            p.id,
            p.name,
            proj.name
        )
        FROM PlanData p
        JOIN p.projectDatas proj
        JOIN proj.taskDatas t
        WHERE p.userId = :userId
        AND p.isDeleted = false
        AND t.deadline = :targetDate
        ORDER BY p.name, proj.name, t.taskId
        """)
    List<TaskDueTodayDto> findTasksByDate(@Param("userId") String userId, 
                                          @Param("targetDate") LocalDate targetDate);
}