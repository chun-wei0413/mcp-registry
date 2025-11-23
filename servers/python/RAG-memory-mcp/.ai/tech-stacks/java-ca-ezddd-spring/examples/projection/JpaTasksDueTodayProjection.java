package tw.teddysoft.example.plan.adapter.out.projection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tw.teddysoft.example.plan.usecase.port.out.PlanData;
import tw.teddysoft.example.plan.usecase.port.out.ProjectData;
import tw.teddysoft.example.plan.usecase.port.out.TaskData;
import tw.teddysoft.example.plan.usecase.port.TaskDueTodayDto;
import tw.teddysoft.example.plan.usecase.port.out.projection.TasksDueTodayProjection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public interface JpaTasksDueTodayProjection extends JpaRepository<PlanData, String>, TasksDueTodayProjection {
    
    @Override
    default List<TaskDueTodayDto> query(TasksDueTodayProjectionInput input) {
        LocalDate today = LocalDate.now();
        return findTasksDueToday(input.userId, today);
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
        AND t.deadline = :today
        """)
    List<TaskDueTodayDto> findTasksDueToday(@Param("userId") String userId, @Param("today") LocalDate today);
}