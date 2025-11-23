package ntut.csie.sslab.ezkanban.kanban.card.adapter.out.repository.springboot.es;

import ntut.csie.sslab.ezkanban.kanban.card.entity.Card;
import ntut.csie.sslab.ezkanban.kanban.card.entity.CardEvents;
import ntut.csie.sslab.ezkanban.kanban.card.entity.CardId;
import ntut.csie.sslab.ezkanban.kanban.card.usecase.port.out.repository.inquiry.FindCardsByTagIdInquiry;
import ntut.csie.sslab.ezkanban.kanban.tag.entity.TagId;
import tw.teddysoft.ezddd.data.adapter.repository.es.EventStore;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.es.EventStoreData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EsFindCardsByTagIdInquiry implements FindCardsByTagIdInquiry {

    private final EventStore eventStore;

    public EsFindCardsByTagIdInquiry(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public List<String> query(TagId tagId) {
        var tagAssignedStream = eventStore.getEventsByType(CardEvents.TypeMapper.TAG_ASSIGNED);
        if (tagAssignedStream.isEmpty()) {
            return new ArrayList<>();
        }
        List<DomainEventData> domainEventDatas = tagAssignedStream.get().getDomainEventDatas();
        List<CardEvents.TagAssigned> tagAssigneds = DomainEventMapper.toDomain(domainEventDatas).stream().map(x -> (CardEvents.TagAssigned) x).collect(Collectors.toList());

        Set<CardId> cardIds = tagAssigneds.stream().filter(x -> x.tagId().equals(tagId)).map(x -> x.cardId()).collect(Collectors.toSet());

        List<String> result = new ArrayList<>();
        for (var cardId : cardIds) {
            Optional<EventStoreData> cardData = eventStore.getEventsByStreamName(Card.getStreamName(Card.CATEGORY, cardId.id()));
            List<CardEvents> domainEvents = DomainEventMapper.toDomain(cardData.get().getDomainEventDatas());
            Card card = new Card(domainEvents);
            if (!card.isDeleted() && card.getTagIds().contains(tagId)) {
                card.setVersion(cardData.get().getVersion());
                result.add(cardId.id());
            }
        }
        return result;
    }
}
