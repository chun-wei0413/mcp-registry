# AI Prompt for Reactor Generation

## Context
When generating reactor for performing eventual consistency in this ai-scrum project, follow the established patterns for Clean Architecture, DDD, CQRS, and Event Sourcing with the ezddd framework.

## Important: Reactor Interface Definition (ADR-018)
- **Must extend** `Reactor<DomainEvent>`, NOT `Reactor<DomainEventData>` or just `Reactor`
- **Method signature**: `execute(DomainEvent event)`, NOT `execute(Object event)`
- **Reference**: `.dev/specs/pbi/usecase/reactor/register-reactor-for-in-memory-repository-example.java`

## File Structure Pattern


For a use case named "Notify[aggregate]ToUnassignTagReactor", generate the following files:

### 1. Reactor Interface - `Notify[Aggregate]When[Event]Reactor.java`
Location: `src/main/java/tw/teddysoft/aiscrum/[aggregate]/usecase/reactor/Notify[Aggregate]When[Event]Reactor.java`

```java
package tw.teddysoft.aiscrum.[aggregate].usecase.reactor;

import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Reactor;

public interface Notify[Aggregate]When[Event]Reactor extends Reactor<DomainEvent> {
}
```
### 2. Service Implementation - `Notify[Aggregate]ToUnassignTagService.java`
Location: `src/main/java/tw/teddysoft/aikanban/[aggregate]/usecase/service/Notify[Aggregate]ToUnassignTagService.java`

```java
package tw.teddysoft.aikanban.[aggregate].usecase.service;

import ntut.csie.sslab.ezkanban.kanban.card.entity.Card;
import ntut.csie.sslab.ezkanban.kanban.card.entity.CardId;
import ntut.csie.sslab.ezkanban.kanban.card.usecase.port.in.command.unassigntag.UnassignTagInput;
import ntut.csie.sslab.ezkanban.kanban.card.usecase.port.in.command.unassigntag.UnassignTagUseCase;
import ntut.csie.sslab.ezkanban.kanban.card.usecase.port.in.reactor.notifycard.NotifyCardToUnassignTagReactor;
import ntut.csie.sslab.ezkanban.kanban.card.usecase.port.out.repository.inquiry.FindCardsByTagIdInquiry;
import ntut.csie.sslab.ezkanban.kanban.tag.entity.TagEvents;
import tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import java.util.List;

public class Notify[Aggregate]ToUnassignTagService implements Notify[Aggregate]ToUnassignTagReactor {

      private Repository<Card, CardId> cardRepository;
      private FindCardsByTagIdInquiry findCardsByTagIdInquiry;
      private final UnassignTagUseCase unassignTagUseCase;

      public NotifyCardToUnassignTagService(Repository<Card, CardId> cardRepository,
                                            FindCardsByTagIdInquiry findCardsByTagIdInquiry,
                                            UnassignTagUseCase unassignTagUseCase) {
         this.cardRepository = cardRepository;
         this.findCardsByTagIdInquiry = findCardsByTagIdInquiry;
         this.unassignTagUseCase = unassignTagUseCase;
      }

      @Override
      public void execute(DomainEvent event) {
         requireNotNull("Event", event);
         
         if (event instanceof TagEvents.TagDeleted tagDeleted) {
            whenTagDeleted(tagDeleted);
         }
         // Silently ignore other event types
      }

      private void whenTagDeleted(TagEvents.TagDeleted tagDeleted) {
         List<String> cardIds = findCardsByTagIdInquiry.query(tagDeleted.tagId());

         for (String cardId : cardIds) {
            UnassignTagInput input = new UnassignTagInput();
            input.setCardId(cardId);
            input.setUserId(tagDeleted.userId());
            input.setTagId(tagDeleted.tagId().id());
            input.setVersion(cardRepository.findById(CardId.valueOf(cardId)).get().getVersion());
            unassignTagUseCase.execute(input);
         }
      }
   }
```

### 3. Test Case - `Notify[Aggregate]ToUnassignTagReactorTest.java`
Location: `src/test/java/tw/teddysoft/aikanban/[aggregate]/usecase/Notify[Aggregate]ToUnassignTagReactorTest.java`

Use the test generation prompt at `codegen/test/test-case-generation-prompt.md`

## Important Notes

1. **Package Structure**: Follow package by feature then by layer
   - Entity layer: `[aggregate].entity`
   - Use case layer: `[aggregate].usecase.port.in.command.[action]`
   - Service layer: `[aggregate].usecase.service`

2. **Interface Mappings**:
   - ValueObject: `tw.teddysoft.ezddd.entity.ValueObject` (no generic parameter)
   - Use case: `tw.teddysoft.ezddd.cqrs.usecase.command.Command<Input, Output>`
   - Input: `tw.teddysoft.ezddd.usecase.port.in.interactor.Input`
   - Repository: `tw.teddysoft.ezddd.usecase.port.out.repository.Repository<T, ID>`

3. **Event Sourcing**:
   - Aggregate extends `EsAggregateRoot<String, [Aggregate]Events>`
   - Events implement `DomainEvent`
   - Use `apply()` method to generate events
   - Implement `when()` method to handle state changes

4. **Contract Programming**:
   - Use `requireNotNull()` for preconditions
   - Use `ensure()` for postconditions
   - Use `invariant()` in `ensureInvariant()` method
   - Use `require()` and `reject()` for business rules

5. **Common Patterns**:
   - Use Java records for Value Objects
   - Use sealed interfaces for domain events
   - Include TypeMapper for event deserialization
   - Use DateProvider.now() for timestamps
   - Generate UUID for event IDs

## Placeholder Replacements

When using this template:
- Replace `[Aggregate]` with the aggregate name (e.g., `Board`, `Workflow`)
- Replace `[aggregate]` with lowercase aggregate name (e.g., `board`, `workflow`)
- Replace `[AGGREGATE]` with uppercase aggregate name (e.g., `BOARD`, `WORKFLOW`)
- Add additional fields based on the use case specification

## Testing

Always create a corresponding test using the patterns from `test-case-generation-prompt.md`:
- Use `GenericInMemoryRepository`
- Use `BlockingMessageBus`
- Use `MessageBus<DomainEvent>` with generic type
- Verify domain events are published correctly