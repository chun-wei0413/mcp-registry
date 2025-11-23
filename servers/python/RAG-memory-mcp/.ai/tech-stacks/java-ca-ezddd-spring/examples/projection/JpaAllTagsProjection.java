package tw.teddysoft.example.tag.adapter.out.projection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tw.teddysoft.example.tag.usecase.port.TagDto;
import tw.teddysoft.example.tag.usecase.port.out.TagData;
import tw.teddysoft.example.tag.usecase.port.out.projection.AllTagsProjection;

import java.util.List;

@Repository
public interface JpaAllTagsProjection extends JpaRepository<TagData, String>, AllTagsProjection {
    
    @Override
    default List<TagDto> query(AllTagsProjectionInput input) {
        return findTagsByPlanId(input.planId);
    }
    
    @Query("""
        SELECT new tw.teddysoft.example.tag.usecase.port.TagDto(
            t.tagId,
            t.name,
            t.color
        )
        FROM TagData t
        WHERE t.planId = :planId
        AND t.isDeleted = false
        ORDER BY t.name
        """)
    List<TagDto> findTagsByPlanId(@Param("planId") String planId);
}