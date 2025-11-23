package ntut.csie.sslab.ezkanban.kanban.card.adapter.out.repository.springboot.outbox;

import ntut.csie.sslab.ezkanban.kanban.card.usecase.port.out.CardData;
import ntut.csie.sslab.ezkanban.kanban.card.usecase.port.out.repository.inquiry.FindCardsByTagIdInquiry;
import ntut.csie.sslab.ezkanban.kanban.tag.entity.TagId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RdbFindCardsByTagIdInquiry extends FindCardsByTagIdInquiry, CrudRepository<CardData, String> {

    @Override
    default List<String> query(TagId tagId) {
        return getCardsByTagId(tagId.id());
    }

    @Query(value = """
            SELECT card.id FROM card, tag_in_card 
            WHERE card.id = tag_in_card.id AND tag_in_card.tag_id = :tagId
            """, nativeQuery = true)
    List<String> getCardsByTagId(@Param("tagId") String tagId);
}
