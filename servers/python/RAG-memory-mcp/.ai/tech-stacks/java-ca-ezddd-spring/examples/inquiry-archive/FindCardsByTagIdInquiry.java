package ntut.csie.sslab.ezkanban.kanban.card.usecase.port.out.repository.inquiry;

import ntut.csie.sslab.ezkanban.kanban.tag.entity.TagId;
import tw.teddysoft.ezddd.cqrs.usecase.command.Inquiry;

import java.util.List;

public interface FindCardsByTagIdInquiry extends Inquiry<TagId, List<String>> {
}
