package tw.teddysoft.example.plan.adapter.out.projection;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tw.teddysoft.example.plan.usecase.port.PlanDto;
import tw.teddysoft.example.plan.usecase.port.PlanMapper;
import tw.teddysoft.example.plan.usecase.port.out.PlanData;
import tw.teddysoft.example.plan.usecase.port.out.projection.PlanDtosProjection;

import java.util.List;

public interface JpaPlanDtosProjection extends PlanDtosProjection, JpaRepository<PlanData, String> {

    @Override
    default List<PlanDto> query(PlanDtosProjectionInput input) {
        // Map the enum to the actual column name
        String sortColumn = input.sortBy == SortBy.NAME ? "name" : "lastUpdated";
        Sort.Direction sortDirection = input.sortOrder == SortOrder.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        // Create Sort object
        Sort sort = Sort.by(sortDirection, sortColumn);
        
        // Use the unified query method with Sort parameter
        List<PlanData> plans = findByUserIdAndIsDeletedFalse(input.userId, sort);
        
        return PlanMapper.toDto(plans);
    }


    // Do not need to write Spring Data JPA JPQL
    //    @Query("SELECT p FROM PlanData p WHERE p.userId = :userId AND p.isDeleted = false")
    //    List<PlanData> findByUserIdAndIsDeletedFalse(@Param("userId") String userId, Sort sort);

    // Query Method
    List<PlanData> findByUserIdAndIsDeletedFalse(String userId, Sort sort);
}